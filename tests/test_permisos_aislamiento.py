from django.core.exceptions import ValidationError
from django.db import DatabaseError, IntegrityError, transaction

from apps.apiario import services as ap
from apps.apiario.models import Colmena, Sensor
from apps.comun.errors import PermisoDenegadoError, ReglaNegocioError
from apps.usuarios.models import Rol, Usuario

from .base import BaseTest, crear_empresa, crear_usuario


class AislamientoTests(BaseTest):
    def test_no_se_edita_colmena_de_otra_empresa_por_url(self):
        c_b = Colmena.objects.create(empresa=self.emp_b, nombre="Ajena")
        self.entrar(self.admin_a)
        self.assertEqual(
            self.client.get(f"/app/colmenas/{c_b.pk}/editar/").status_code, 404
        )
        r = self.client.post(
            f"/app/colmenas/{c_b.pk}/editar/", {"nombre": "Hack", "estado": "Estable"}
        )
        self.assertEqual(r.status_code, 404)
        c_b.refresh_from_db()
        self.assertEqual(c_b.nombre, "Ajena")

    def test_empresa_enviada_en_formulario_se_ignora(self):
        self.entrar(self.admin_a)
        self.client.post(
            "/app/colmenas/nueva/",
            {
                "nombre": "Mia",
                "estado": "Estable",
                "empresa": self.emp_b.pk,
                "empresa_id": self.emp_b.pk,
            },
        )
        self.assertTrue(
            Colmena.objects.filter(nombre="Mia", empresa=self.emp_a).exists()
        )
        self.assertFalse(Colmena.objects.filter(empresa=self.emp_b).exists())

    def test_listado_solo_muestra_mi_empresa(self):
        Colmena.objects.create(empresa=self.emp_a, nombre="DeA")
        Colmena.objects.create(empresa=self.emp_b, nombre="DeB")
        self.entrar(self.admin_a)
        r = self.client.get("/app/colmenas/")
        self.assertContains(r, "DeA")
        self.assertNotContains(r, "DeB")

    def test_asociar_sensor_de_otra_empresa_se_rechaza(self):
        c_a = Colmena.objects.create(empresa=self.emp_a, nombre="CA")
        s_b = Sensor.objects.create(empresa=self.emp_b, codigo="SB", tipo="PESO")
        with self.assertRaises(PermisoDenegadoError):
            ap.asociar_sensor(self.admin_a, s_b, c_a)

    def test_empleado_cliente_no_puede_escribir_ni_por_vista_ni_por_servicio(self):
        self.entrar(self.emp_cli_a)
        self.assertEqual(
            self.client.post(
                "/app/colmenas/nueva/", {"nombre": "X", "estado": "Estable"}
            ).status_code,
            403,
        )
        with self.assertRaises(PermisoDenegadoError):
            ap.crear_colmena(self.emp_cli_a, "X")

    def test_cliente_no_entra_al_panel_interno_y_viceversa(self):
        self.entrar(self.admin_a)
        for url in ("/admin/dashboard/", "/admin/pagos/", "/admin/solicitudes/"):
            self.assertEqual(self.client.get(url).status_code, 403, url)

    def test_empleado_apitech_no_gestiona_empresas(self):
        self.entrar(self.emp_api)
        r = self.client.post(
            f"/admin/empresas/{self.emp_a.pk}/estado/", {"estado": "pausada"}
        )
        self.assertEqual(r.status_code, 403)
        self.emp_a.refresh_from_db()
        self.assertEqual(self.emp_a.estado, "activa")

    def test_admin_apitech_pausa_empresa(self):
        self.entrar(self.admin_api)
        self.client.post(
            f"/admin/empresas/{self.emp_a.pk}/estado/", {"estado": "pausada"}
        )
        self.emp_a.refresh_from_db()
        self.assertEqual(self.emp_a.estado, "pausada")

    def test_no_hay_elevacion_cambiando_rol_o_empresa(self):
        """No existe endpoint que acepte rol/empresa del navegador; el modelo impide combinaciones incoherentes."""
        with self.assertRaises(ValidationError):
            Usuario.objects.create_user(
                "x@t.co",
                "Clave-123456",
                Rol.ADMIN_APITECH,
                empresa=self.emp_a,
                nombre="X",
            )
        with self.assertRaises(ValidationError):
            Usuario.objects.create_user(
                "y@t.co", "Clave-123456", Rol.ADMIN_CLIENTE, empresa=None, nombre="Y"
            )

    def test_la_bd_impide_usuario_incoherente_aunque_se_salte_save(self):
        with self.assertRaises(DatabaseError), transaction.atomic():
            Usuario.objects.filter(pk=self.admin_a.pk).update(
                empresa=None
            )  # cliente sin empresa


class ReglasDeOperacionTests(BaseTest):
    def test_limite_del_plan_basico_20_colmenas(self):
        for i in range(20):
            ap.crear_colmena(self.admin_a, f"C{i}")
        with self.assertRaisesMessage(ReglaNegocioError, "límite de 20 colmenas"):
            ap.crear_colmena(self.admin_a, "C21")

    def test_limite_del_plan_profesional_60(self):
        emp = crear_empresa("Pro", plan="Plan Profesional")
        admin = crear_usuario(Rol.ADMIN_CLIENTE, emp)
        for i in range(60):
            Colmena.objects.create(empresa=emp, nombre=f"C{i}")
        with self.assertRaisesMessage(ReglaNegocioError, "límite de 60 colmenas"):
            ap.crear_colmena(admin, "extra")

    def test_limite_de_sensores(self):
        for i in range(20):
            ap.registrar_sensor(self.admin_a, f"SEN-{i}", "PESO")
        with self.assertRaisesMessage(ReglaNegocioError, "límite de 20 sensores"):
            ap.registrar_sensor(self.admin_a, "SEN-X", "PESO")

    def test_empresa_pausada_solo_consulta(self):
        self.emp_a.estado = "pausada"
        self.emp_a.save()
        with self.assertRaisesMessage(ReglaNegocioError, "pausada"):
            ap.crear_colmena(self.admin_a, "No")
        self.entrar(self.admin_a)
        self.assertEqual(
            self.client.get("/app/colmenas/").status_code, 200
        )  # consulta permitida
        self.assertEqual(self.client.get("/app/dashboard/").status_code, 200)

    def test_suscripcion_vencida_bloquea_escrituras(self):
        emp = crear_empresa("Vencida", vence_en=-1)
        admin = crear_usuario(Rol.ADMIN_CLIENTE, emp)
        with self.assertRaisesMessage(ReglaNegocioError, "no está vigente"):
            ap.crear_colmena(admin, "No")

    def test_sin_suscripcion_limite_cero(self):
        emp = crear_empresa("Sin", plan=None)
        admin = crear_usuario(Rol.ADMIN_CLIENTE, emp)
        with self.assertRaises(ReglaNegocioError):
            ap.crear_colmena(admin, "No")

    def test_nombre_vacio_y_estado_invalido(self):
        with self.assertRaises(ReglaNegocioError):
            ap.crear_colmena(self.admin_a, "  ")
        with self.assertRaises(ReglaNegocioError):
            ap.crear_colmena(self.admin_a, "ok", estado="Inventado")

    def test_sensor_con_una_sola_asociacion_activa(self):
        c1, c2 = (ap.crear_colmena(self.admin_a, n) for n in ("c1", "c2"))
        s = ap.registrar_sensor(self.admin_a, "UNICO", "HUMEDAD")
        ap.asociar_sensor(self.admin_a, s, c1)
        with self.assertRaisesMessage(
            ReglaNegocioError, "ya tiene una asociación activa"
        ):
            ap.asociar_sensor(self.admin_a, s, c2)

    def test_reglas_de_monitoreo_heredadas_de_triggers(self):
        c = ap.crear_colmena(self.admin_a, "c", estado="Advertencia")
        s = ap.registrar_sensor(self.admin_a, "S1", "PESO")
        a = ap.asociar_sensor(self.admin_a, s, c)
        with self.assertRaisesMessage(ReglaNegocioError, "Estable"):
            ap.activar_monitoreo(self.admin_a, a)
        ap.actualizar_colmena(self.admin_a, c, "c", "", "Estable")
        m = ap.activar_monitoreo(self.admin_a, a)
        with self.assertRaisesMessage(
            ReglaNegocioError, "ya tiene un monitoreo activo"
        ):
            ap.activar_monitoreo(self.admin_a, a)
        with self.assertRaisesMessage(ReglaNegocioError, "monitoreo activo"):
            ap.desasociar_sensor(self.admin_a, a)
        with self.assertRaisesMessage(ReglaNegocioError, "monitoreo activo"):
            ap.cambiar_estado_sensor(self.admin_a, s, "inactivo")
        ap.desactivar_monitoreo(self.admin_a, m)
        ap.desasociar_sensor(self.admin_a, a)
        a.refresh_from_db()
        self.assertFalse(a.activo)
        self.assertIsNotNone(a.fecha_fin)  # historial conservado

    def test_bd_impide_dos_asociaciones_activas_del_mismo_sensor(self):
        from apps.apiario.models import SensorColmena

        c1 = Colmena.objects.create(empresa=self.emp_a, nombre="a")
        c2 = Colmena.objects.create(empresa=self.emp_a, nombre="b")
        s = Sensor.objects.create(empresa=self.emp_a, codigo="DUP", tipo="PESO")
        SensorColmena.objects.create(sensor=s, colmena=c1)
        with self.assertRaises(IntegrityError), transaction.atomic():
            SensorColmena.objects.create(sensor=s, colmena=c2)

    def test_modelo_impide_asociar_entre_empresas(self):
        from apps.apiario.models import SensorColmena

        c_a = Colmena.objects.create(empresa=self.emp_a, nombre="a")
        s_b = Sensor.objects.create(empresa=self.emp_b, codigo="X-B", tipo="PESO")
        with self.assertRaises(ValidationError):
            SensorColmena.objects.create(sensor=s_b, colmena=c_a)
