import io, csv, json
from datetime import timedelta
from unittest.mock import patch
from django.test import override_settings, Client
from django.core.files.uploadedfile import SimpleUploadedFile
from django.utils import timezone
from django.db import transaction, IntegrityError
from django.urls import reverse
from apps.apiario.models import Colmena, Sensor, SensorColmena
from apps.apiario.services import crear_colmena, archivar_colmena
from apps.monitoreo.models import Medicion
from apps.monitoreo.services import registrar_medicion
from apps.integraciones.models import ConexionAPI
from apps.integraciones.services import sincronizar, APIError
from apps.importaciones.services import previsualizar, confirmar
from apps.comun.errors import ReglaNegocioError
from apps.suscripciones.services import registrar_pago, verificar_pago
from .base import BaseTest, crear_sensor_con_monitoreo


class MK6Test(BaseTest):
    def test_numeros_por_empresa_y_reutilizacion_sin_reusar_pk(self):
        a = [crear_colmena(self.admin_a, "Colmena " + str(i)) for i in range(3)]
        b = [crear_colmena(self.admin_b, "B " + str(i)) for i in range(2)]
        self.assertEqual([c.numero for c in a], [1, 2, 3])
        self.assertEqual([c.numero for c in b], [1, 2])
        old = [c.pk for c in a]
        for c in a:
            archivar_colmena(self.admin_a, c)
        nuevas = [crear_colmena(self.admin_a, "Nueva " + str(i)) for i in range(3)]
        self.assertEqual([c.numero for c in nuevas], [1, 2, 3])
        self.assertFalse(set(old) & {c.pk for c in nuevas})
        self.assertEqual(
            Colmena.objects.filter(pk__in=old, archivada=True, numero=None).count(), 3
        )

    def test_historial_no_pasa_a_colmena_nueva(self):
        c = crear_colmena(self.admin_a, "Antigua")
        s, a, _ = crear_sensor_con_monitoreo(self.emp_a, colmena=c)
        registrar_medicion(
            s, 25, timezone.now() - timedelta(hours=1), id_externo="hist"
        )
        archivar_colmena(self.admin_a, c)
        n = crear_colmena(self.admin_a, "Nueva")
        self.assertEqual(c.numero_historico, n.numero)
        self.entrar(self.admin_a)
        self.assertContains(
            self.client.get(reverse("colmena_detalle", args=[c.pk])), "25,000"
        )
        self.assertEqual(
            self.client.get(reverse("telemetria_colmena", args=[n.pk])).json()[
                "series"
            ],
            [],
        )

    def test_todas_las_pantallas_cliente(self):
        self.entrar(self.admin_a)
        for ruta in [
            "app_dashboard",
            "colmenas",
            "colmena_nueva",
            "sensores",
            "sensor_nuevo",
            "produccion",
            "produccion_nueva",
            "alertas",
            "reportes",
            "usuarios",
            "usuario_nuevo",
            "pagos",
            "importar",
            "conexion",
            "umbrales",
            "perfil",
            "notificaciones",
            "ayuda",
        ]:
            r = self.client.get(reverse(ruta))
            self.assertEqual(r.status_code, 200, ruta)

    def test_todas_las_pantallas_internas(self):
        self.entrar(self.admin_api)
        for ruta in [
            "admin_dashboard",
            "admin_empresas",
            "admin_pagos",
            "admin_solicitudes",
            "usuarios",
            "usuario_nuevo",
            "planes_admin",
            "auditoria",
            "perfil",
            "ayuda",
        ]:
            self.assertEqual(self.client.get(reverse(ruta)).status_code, 200, ruta)

    def test_detalle_telemetria_archivo_ajeno_denegado(self):
        c = crear_colmena(self.admin_b, "Secreto")
        self.entrar(self.admin_a)
        for ruta in ["colmena_detalle", "telemetria_colmena", "colmena_editar"]:
            self.assertEqual(
                self.client.get(reverse(ruta, args=[c.pk])).status_code, 404
            )
        self.assertEqual(
            self.client.post(reverse("colmena_archivar", args=[c.pk])).status_code, 404
        )

    def test_consultar_dashboard_no_genera_mediciones(self):
        self.entrar(self.admin_a)
        for _ in range(3):
            self.client.get(reverse("app_dashboard"))
            self.client.get(reverse("telemetria"))
        self.assertEqual(Medicion.objects.count(), 0)

    def test_misma_lectura_con_datos_distintos_rechazada(self):
        s, a, c = crear_sensor_con_monitoreo(self.emp_a)
        fecha = timezone.now()
        registrar_medicion(s, 25, fecha, id_externo="same")
        with self.assertRaises(ReglaNegocioError):
            registrar_medicion(s, 30, fecha, id_externo="same")
        self.assertEqual(Medicion.objects.count(), 1)

    def test_api_aislamiento_y_cursor_atomico(self):
        s, a, c = crear_sensor_con_monitoreo(self.emp_a)
        s.id_externo = "sensor-a"
        s.save()
        ConexionAPI.objects.create(empresa=self.emp_a, habilitada=True)
        fecha = timezone.now().isoformat()
        row = {
            "company_id": str(self.emp_a.pk),
            "sensor_id": "sensor-a",
            "association_id": str(a.pk),
            "hive_id": str(c.pk),
            "origin": "SIMULATED",
            "type": s.tipo,
            "unit": "°C",
            "reading_id": "a1",
            "measured_at": fecha,
            "value": 25,
        }
        with patch("apps.integraciones.services.Cliente") as cls:
            cls.return_value.request.return_value = {
                "items": [
                    row,
                    {**row, "reading_id": "a2", "company_id": str(self.emp_b.pk)},
                ],
                "next_cursor": "cursor-2",
            }
            with self.assertRaises(APIError):
                sincronizar(self.emp_a)
        s.refresh_from_db()
        self.assertEqual(s.cursor_sincronizacion, "")
        self.assertEqual(Medicion.objects.count(), 0)
        with patch("apps.integraciones.services.Cliente") as cls:
            cls.return_value.request.return_value = {
                "items": [row],
                "next_cursor": "cursor-1",
            }
            self.assertEqual(sincronizar(self.emp_a), 1)
        s.refresh_from_db()
        self.assertEqual(s.cursor_sincronizacion, "cursor-1")

    def test_importar_revalida_empresa_y_duplicados(self):
        c = crear_colmena(self.admin_a, "Importar")
        s, a, c = crear_sensor_con_monitoreo(self.emp_a, colmena=c)
        fecha = (timezone.now() - timedelta(minutes=5)).isoformat()
        csvdata = f"fecha,colmena,asociacion,tipo,valor,unidad,origen,produccion,observaciones\n{fecha},1,{a.pk},TEMPERATURA,26,°C,SIMULADO,,Ejemplo\n"
        lote = previsualizar(
            self.admin_a, SimpleUploadedFile("datos.csv", csvdata.encode()), "historico"
        )
        self.assertFalse(lote.errores)
        self.assertEqual(Medicion.objects.count(), 0)
        l = confirmar(self.admin_a, lote)
        self.assertEqual(l.resultado["creadas"], 1)
        with self.assertRaises(ReglaNegocioError):
            confirmar(self.admin_a, lote)
        l2 = previsualizar(
            self.admin_a, SimpleUploadedFile("datos.csv", csvdata.encode()), "historico"
        )
        confirmar(self.admin_a, l2)
        self.assertEqual(Medicion.objects.count(), 1)
        bad = previsualizar(
            self.admin_b, SimpleUploadedFile("datos.csv", csvdata.encode()), "historico"
        )
        self.assertEqual(len(bad.errores), 1)

    def test_exportaciones_y_filtros_no_mezclan_empresa(self):
        s, a, c = crear_sensor_con_monitoreo(self.emp_a)
        sb, ab, cb = crear_sensor_con_monitoreo(self.emp_b)
        registrar_medicion(s, 25, timezone.now(), id_externo="a")
        registrar_medicion(sb, 30, timezone.now(), id_externo="b")
        self.entrar(self.admin_a)
        for fmt in ["csv", "xlsx", "pdf"]:
            r = self.client.get(reverse("exportar", args=[fmt]))
            self.assertEqual(r.status_code, 200)
            self.assertGreater(len(r.content), 50)
        r = self.client.get(reverse("exportar", args=["csv"]))
        self.assertIn(s.codigo.encode(), r.content)
        self.assertNotIn(sb.codigo.encode(), r.content)
        r = self.client.get(reverse("exportar", args=["csv"]), {"colmena": cb.pk})
        self.assertNotIn(s.codigo.encode(), r.content)

    def test_renovacion_conserva_dias_y_historico(self):
        old = self.emp_a.suscripciones.get(estado="ACTIVA")
        fin = old.fecha_vencimiento
        p = registrar_pago(self.emp_a, old.plan_id)
        verificar_pago(self.admin_api, p.pk)
        old.refresh_from_db()
        p.suscripcion.refresh_from_db()
        self.assertEqual(old.estado, "CANCELADA")
        self.assertEqual(
            p.suscripcion.fecha_vencimiento,
            fin + timedelta(days=old.plan.duracion_dias),
        )
        self.assertEqual(self.emp_a.suscripciones.filter(estado="ACTIVA").count(), 1)

    def test_usuario_ajeno_y_elevacion_desde_formulario(self):
        self.entrar(self.admin_a)
        r = self.client.post(
            reverse("usuario_nuevo"),
            {
                "nombre": "Ataque",
                "correo": "ataque@demo.co",
                "rol": self.admin_api.rol_id,
                "estado": "activo",
                "clave": "Segura-Prueba-2026",
                "empresa": self.emp_b.pk,
            },
        )
        self.assertEqual(r.status_code, 200)
        from apps.usuarios.models import Usuario

        self.assertFalse(Usuario.objects.filter(correo="ataque@demo.co").exists())
        self.assertEqual(
            self.client.get(
                reverse("usuario_editar", args=[self.admin_b.pk])
            ).status_code,
            404,
        )
