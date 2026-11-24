from datetime import timedelta
from decimal import Decimal

from django.db import IntegrityError, transaction
from django.utils import timezone

from apps.apiario import services as ap
from apps.apiario.models import Colmena, SensorColmena
from apps.comun.errors import ReglaNegocioError
from apps.monitoreo import services as mon
from apps.monitoreo.models import Alerta, Medicion, Monitoreo, Notificacion, RangoUmbral

from .base import BaseTest, crear_sensor_con_monitoreo


def hace(**kw):
    return timezone.now() - timedelta(**kw)


class RegistroMedicionesTests(BaseTest):
    def setUp(self):
        super().setUp()
        self.sensor, self.asoc, self.colmena = crear_sensor_con_monitoreo(
            self.emp_a, "TEMPERATURA"
        )

    def test_lectura_valida_conserva_identidad_origen_y_fechas(self):
        m, creada = mon.registrar_medicion(
            self.sensor,
            "25.5",
            hace(minutes=10),
            id_externo="e-1",
            fuente=Medicion.FUENTE_API,
            unidad="°C",
        )
        self.assertTrue(creada)
        self.assertEqual(
            (m.valor, m.unidad, m.origen, m.sensor),
            (Decimal("25.500"), "°C", "SIMULADO", self.sensor),
        )
        self.assertLess(
            m.fecha, m.fecha_recepcion
        )  # fecha de medición ≠ fecha de recepción

    def test_lectura_repetida_no_duplica(self):
        f = hace(minutes=5)
        a, c1 = mon.registrar_medicion(self.sensor, 24, f, id_externo="dup")
        b, c2 = mon.registrar_medicion(self.sensor, 24, f, id_externo="dup")
        self.assertEqual((c1, c2, a.pk), (True, False, b.pk))
        self.assertEqual(Medicion.objects.count(), 1)

    def test_la_bd_impide_duplicado_sensor_id_externo(self):
        mon.registrar_medicion(self.sensor, 24, hace(minutes=5), id_externo="x")
        with self.assertRaises(IntegrityError), transaction.atomic():
            Medicion.objects.create(
                asociacion=self.asoc,
                sensor=self.sensor,
                tipo_medicion="TEMPERATURA",
                valor=1,
                unidad="°C",
                fecha=hace(minutes=4),
                id_externo="x",
            )

    def test_lecturas_invalidas_se_rechazan(self):
        casos = [
            ("abc", hace(minutes=1), {}, "numérico"),
            ("NaN", hace(minutes=1), {}, "finito"),
            (20, timezone.now() + timedelta(hours=2), {}, "futuro"),
            (20, hace(minutes=1), {"tipo": "PESO"}, "no coincide"),
            (20, hace(minutes=1), {"unidad": "kg"}, "Unidad"),
            (20, timezone.now().replace(tzinfo=None), {}, "zona horaria"),
        ]
        for valor, fecha, extra, texto in casos:
            with self.subTest(texto=texto), self.assertRaisesMessage(
                ReglaNegocioError, texto
            ):
                mon.registrar_medicion(self.sensor, valor, fecha, **extra)
        self.assertEqual(Medicion.objects.count(), 0)

    def test_sin_monitoreo_activo_se_rechaza(self):
        Monitoreo.objects.update(estado="inactivo", fecha_fin=hace(hours=1))
        with self.assertRaisesMessage(ReglaNegocioError, "sin monitoreo activo"):
            mon.registrar_medicion(self.sensor, 20, hace(minutes=1))

    def test_lectura_de_cuando_el_monitoreo_estaba_activo_se_acepta_despues(self):
        Monitoreo.objects.update(estado="inactivo", fecha_fin=hace(hours=1))
        m, creada = mon.registrar_medicion(
            self.sensor, 20, hace(hours=3), id_externo="tarde"
        )
        self.assertTrue(creada)

    def test_lecturas_fuera_de_orden_se_guardan_ordenadas_por_fecha(self):
        mon.registrar_medicion(self.sensor, 21, hace(minutes=1), id_externo="n")
        mon.registrar_medicion(self.sensor, 22, hace(minutes=30), id_externo="v")
        fechas = list(
            Medicion.objects.order_by("fecha").values_list("id_externo", flat=True)
        )
        self.assertEqual(fechas, ["v", "n"])

    def test_empresa_pausada_no_recibe_lecturas(self):
        self.emp_a.estado = "pausada"
        self.emp_a.save()
        with self.assertRaisesMessage(ReglaNegocioError, "pausada"):
            mon.registrar_medicion(self.sensor, 20, hace(minutes=1))

    def test_historial_se_conserva_al_reasignar_sensor(self):
        mon.registrar_medicion(self.sensor, 20, hace(hours=5), id_externo="antes")
        # reasignación: cerrar asociación/monitoreo antiguos (hace 2 h) y abrir otra colmena
        Monitoreo.objects.filter(asociacion=self.asoc).update(
            estado="inactivo", fecha_fin=hace(hours=2)
        )
        SensorColmena.objects.filter(pk=self.asoc.pk).update(
            activo=False, fecha_fin=hace(hours=2)
        )
        colmena2 = Colmena.objects.create(empresa=self.emp_a, nombre="Destino")
        nueva = SensorColmena.objects.create(sensor=self.sensor, colmena=colmena2)
        SensorColmena.objects.filter(pk=nueva.pk).update(fecha_inicio=hace(hours=1))
        m2 = Monitoreo.objects.create(asociacion=nueva)
        Monitoreo.objects.filter(pk=m2.pk).update(fecha_inicio=hace(hours=1))
        mon.registrar_medicion(self.sensor, 21, hace(minutes=10), id_externo="despues")
        self.assertEqual(
            Medicion.objects.get(id_externo="antes").asociacion.colmena, self.colmena
        )
        self.assertEqual(
            Medicion.objects.get(id_externo="despues").asociacion.colmena, colmena2
        )


class UmbralesAlertasTests(BaseTest):
    def setUp(self):
        super().setUp()
        self.sensor, self.asoc, self.colmena = crear_sensor_con_monitoreo(
            self.emp_a, "TEMPERATURA"
        )

    def test_umbrales_demo_globales_existen_y_estan_marcados(self):
        g = RangoUmbral.objects.filter(empresa__isnull=True)
        self.assertEqual(g.count(), 3)
        self.assertTrue(all(u.es_demostracion for u in g))

    def test_prioridad_umbral_de_empresa_sobre_global(self):
        RangoUmbral.objects.create(
            empresa=self.emp_a,
            tipo_medicion="TEMPERATURA",
            valor_min=10,
            valor_max=20,
            unidad="°C",
        )
        self.assertEqual(
            mon.umbral_aplicable(self.emp_a.pk, "TEMPERATURA").valor_max, 20
        )
        self.assertEqual(
            mon.umbral_aplicable(self.emp_b.pk, "TEMPERATURA").valor_max, 35
        )  # global

    def test_lectura_dentro_del_rango_no_alerta(self):
        mon.registrar_medicion(self.sensor, 25, hace(minutes=5))
        self.assertEqual(Alerta.objects.count(), 0)

    def test_fuera_de_rango_crea_alerta_y_notifica_solo_a_su_empresa(self):
        mon.registrar_medicion(self.sensor, 50, hace(minutes=5), id_externo="a1")
        a = Alerta.objects.get()
        self.assertEqual(
            (a.severidad, a.empresa, a.colmena, a.estado),
            ("critica", self.emp_a, self.colmena, "ACTIVA"),
        )
        self.assertEqual((a.umbral_min, a.umbral_max), (Decimal("18"), Decimal("35")))
        destinatarios = set(Notificacion.objects.values_list("usuario", flat=True))
        self.assertEqual(
            destinatarios, {self.admin_a.pk, self.emp_cli_a.pk}
        )  # nadie de B ni de ApiTech

    def test_no_duplica_alertas_activas_del_mismo_tipo(self):
        mon.registrar_medicion(self.sensor, 50, hace(minutes=10), id_externo="a1")
        mon.registrar_medicion(self.sensor, 52, hace(minutes=5), id_externo="a2")
        self.assertEqual(Alerta.objects.count(), 1)
        self.assertEqual(Notificacion.objects.count(), 2)

    def test_lectura_antigua_tardia_no_dispara_alerta(self):
        mon.registrar_medicion(self.sensor, 25, hace(minutes=1), id_externo="reciente")
        mon.registrar_medicion(self.sensor, 60, hace(hours=2), id_externo="vieja")
        self.assertEqual(Alerta.objects.count(), 0)

    def test_resolver_alerta_actualiza_notificaciones_y_no_se_repite(self):
        mon.registrar_medicion(self.sensor, 50, hace(minutes=5))
        alerta = Alerta.objects.get()
        mon.resolver_alerta(self.admin_a, alerta)
        alerta.refresh_from_db()
        self.assertEqual(
            (alerta.estado, alerta.usuario_resolucion), ("RESUELTA", self.admin_a)
        )
        self.assertFalse(Notificacion.objects.filter(leida=False).exists())
        with self.assertRaisesMessage(ReglaNegocioError, "ya fue resuelta"):
            mon.resolver_alerta(self.admin_a, alerta)
        # tras resolver puede generarse una alerta nueva
        mon.registrar_medicion(self.sensor, 55, hace(minutes=1), id_externo="otra")
        self.assertEqual(Alerta.objects.filter(estado="ACTIVA").count(), 1)

    def test_otra_empresa_no_puede_resolver_mi_alerta(self):
        from apps.comun.errors import PermisoDenegadoError

        mon.registrar_medicion(self.sensor, 50, hace(minutes=5))
        with self.assertRaises(PermisoDenegadoError):
            mon.resolver_alerta(self.admin_b, Alerta.objects.get())

    def test_bd_impide_umbral_invertido_y_umbral_global_duplicado(self):
        with self.assertRaises(IntegrityError), transaction.atomic():
            RangoUmbral.objects.create(
                empresa=self.emp_a,
                tipo_medicion="PESO",
                valor_min=10,
                valor_max=5,
                unidad="kg",
            )
        with self.assertRaises(IntegrityError), transaction.atomic():
            RangoUmbral.objects.create(
                tipo_medicion="PESO", valor_min=0, valor_max=9, unidad="kg"
            )  # ya hay global
