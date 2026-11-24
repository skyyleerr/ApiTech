from datetime import timedelta

from django.db import IntegrityError, transaction
from django.utils import timezone

from apps.comun.errors import PermisoDenegadoError, ReglaNegocioError
from apps.empresas.models import Empresa
from apps.suscripciones import services as s
from apps.suscripciones.models import Pago, Plan, SolicitudSuscripcion, Suscripcion
from apps.usuarios.models import Rol, Usuario

from .base import CLAVE, BaseTest, crear_empresa

DATOS = dict(
    nombre_empresa="Miel Sur",
    nit="800111222-3",
    correo_empresa="a@miel.co",
    nombre_contacto="Ana",
    correo_contacto="ana@miel.co",
    tarjeta_falsa="4111 1111 1111 1234",
)


class SolicitudesTests(BaseTest):
    def plan(self):
        return Plan.objects.get(nombre_plan="Plan Profesional").pk

    def test_seeds_de_instalacion_limpia(self):
        self.assertEqual(
            set(Rol.objects.values_list("nombre", flat=True)),
            set(Rol.INTERNOS + Rol.CLIENTES),
        )
        self.assertEqual(
            list(
                Plan.objects.order_by("precio").values_list(
                    "max_colmenas", "max_sensores"
                )
            ),
            [(20, 20), (60, 60)],
        )
        self.assertFalse(
            Rol.objects.filter(nombre="Sistema").exists()
        )  # «Sistema» no es un rol con sesión

    def test_solicitud_solo_guarda_ultimos_4_digitos(self):
        sol = s.crear_solicitud(plan_id=self.plan(), **DATOS)
        self.assertEqual(sol.tarjeta_falsa_ultimos4, "1234")
        self.assertNotIn("4111", str(sol.__dict__))

    def test_validaciones_de_solicitud(self):
        with self.assertRaisesMessage(ReglaNegocioError, "tarjeta"):
            s.crear_solicitud(plan_id=self.plan(), **{**DATOS, "tarjeta_falsa": "123"})
        with self.assertRaisesMessage(ReglaNegocioError, "plan"):
            s.crear_solicitud(plan_id=9999, **DATOS)
        with self.assertRaisesMessage(ReglaNegocioError, "Ya existe una empresa"):
            s.crear_solicitud(plan_id=self.plan(), **{**DATOS, "nit": self.emp_a.nit})

    def test_no_dos_solicitudes_pendientes_para_el_mismo_nit(self):
        s.crear_solicitud(plan_id=self.plan(), **DATOS)
        with self.assertRaisesMessage(ReglaNegocioError, "pendiente"):
            s.crear_solicitud(plan_id=self.plan(), **DATOS)

    def test_aprobar_crea_empresa_admin_y_suscripcion_activa(self):
        sol = s.crear_solicitud(plan_id=self.plan(), **DATOS)
        s.aprobar_solicitud(
            self.admin_api,
            sol.pk,
            correo_admin="ana@miel.co",
            password_inicial="Segura-2026-ok",
        )
        emp = Empresa.objects.get(nit="800111222-3")
        sub = Suscripcion.objects.get(empresa=emp)
        self.assertEqual((sub.estado, sub.plan.max_colmenas), ("ACTIVA", 60))
        self.assertEqual(
            sub.fecha_vencimiento, timezone.localdate() + timedelta(days=30)
        )
        self.assertEqual(Pago.objects.get(suscripcion=sub).estado, "VERIFICADO")
        self.assertEqual(
            Usuario.objects.get(correo="ana@miel.co").rol.nombre, Rol.ADMIN_CLIENTE
        )
        with self.assertRaisesMessage(ReglaNegocioError, "ya fue procesada"):
            s.aprobar_solicitud(
                self.admin_api,
                sol.pk,
                correo_admin="otro@miel.co",
                password_inicial="Segura-2026-ok",
            )
        self.assertEqual(Empresa.objects.filter(nit="800111222-3").count(), 1)

    def test_aprobacion_es_atomica_si_falla_el_correo(self):
        sol = s.crear_solicitud(plan_id=self.plan(), **DATOS)
        with self.assertRaises(ReglaNegocioError):
            s.aprobar_solicitud(
                self.admin_api,
                sol.pk,
                correo_admin=self.admin_a.correo,
                password_inicial="Segura-2026-ok",
            )
        self.assertFalse(Empresa.objects.filter(nit="800111222-3").exists())
        sol.refresh_from_db()
        self.assertEqual(sol.estado, SolicitudSuscripcion.PENDIENTE)

    def test_contrasena_inicial_debil_se_rechaza(self):
        sol = s.crear_solicitud(plan_id=self.plan(), **DATOS)
        with self.assertRaises(ReglaNegocioError):
            s.aprobar_solicitud(
                self.admin_api,
                sol.pk,
                correo_admin="ana@miel.co",
                password_inicial="123456",
            )

    def test_empleado_apitech_revisa_y_cliente_no_revisa(self):
        sol = s.crear_solicitud(plan_id=self.plan(), **DATOS)
        s.aprobar_solicitud(
            self.emp_api,
            sol.pk,
            correo_admin="ana@miel.co",
            password_inicial="Segura-2026-ok",
        )
        sol.refresh_from_db()
        self.assertEqual(sol.estado, "APROBADA")
        with self.assertRaises(PermisoDenegadoError):
            s.rechazar_solicitud(self.admin_a, sol.pk)

    def test_rechazar_solicitud(self):
        sol = s.crear_solicitud(plan_id=self.plan(), **DATOS)
        s.rechazar_solicitud(self.admin_api, sol.pk, "Datos incompletos")
        sol.refresh_from_db()
        self.assertEqual(
            (sol.estado, sol.motivo_rechazo), ("RECHAZADA", "Datos incompletos")
        )


class PagosTests(BaseTest):
    def nuevo_pago(self, empresa=None):
        empresa = empresa or crear_empresa("Nueva", plan=None)
        return s.registrar_pago(empresa, Plan.objects.get(nombre_plan="Plan Básico").pk)

    def test_valor_lo_calcula_el_servidor_desde_el_plan(self):
        self.assertEqual(
            self.nuevo_pago().valor, Plan.objects.get(nombre_plan="Plan Básico").precio
        )

    def test_confirmacion_repetida_no_reactiva_ni_duplica(self):
        pago = self.nuevo_pago()
        s.verificar_pago(self.admin_api, pago.pk)
        pago.refresh_from_db()
        inicio, vence = (
            pago.suscripcion.fecha_inicio,
            pago.suscripcion.fecha_vencimiento,
        )
        with self.assertRaisesMessage(ReglaNegocioError, "ya fue procesado"):
            s.verificar_pago(self.admin_api, pago.pk)
        pago.suscripcion.refresh_from_db()
        self.assertEqual(
            (
                pago.suscripcion.estado,
                pago.suscripcion.fecha_inicio,
                pago.suscripcion.fecha_vencimiento,
            ),
            ("ACTIVA", inicio, vence),
        )

    def test_no_se_activa_segunda_suscripcion_para_la_misma_empresa(self):
        pago = self.nuevo_pago(self.emp_a)  # emp_a ya tiene una ACTIVA
        s.verificar_pago(self.admin_api, pago.pk)
        pago.refresh_from_db()
        self.assertEqual(pago.estado, "VERIFICADO")
        self.assertEqual(
            Suscripcion.objects.filter(empresa=self.emp_a, estado="ACTIVA").count(), 1
        )

    def test_la_bd_impide_dos_suscripciones_activas(self):
        plan = Plan.objects.first()
        with self.assertRaises(IntegrityError), transaction.atomic():
            Suscripcion.objects.create(empresa=self.emp_a, plan=plan, estado="ACTIVA")

    def test_rechazar_pago_cancela_suscripcion_pendiente_y_no_se_repite(self):
        pago = self.nuevo_pago()
        s.rechazar_pago(self.admin_api, pago.pk, "Sin fondos")
        pago.refresh_from_db()
        self.assertEqual(
            (pago.estado, pago.suscripcion.estado), ("RECHAZADO", "CANCELADA")
        )
        with self.assertRaises(ReglaNegocioError):
            s.verificar_pago(self.admin_api, pago.pk)

    def test_clientes_no_verifican(self):
        pago = self.nuevo_pago()
        for u in (self.admin_a, self.emp_cli_a):
            with self.assertRaises(PermisoDenegadoError):
                s.verificar_pago(u, pago.pk)

    def test_vencimiento_programado(self):
        sub = Suscripcion.objects.get(empresa=self.emp_a)
        Suscripcion.objects.filter(pk=sub.pk).update(
            fecha_vencimiento=timezone.localdate() - timedelta(days=1)
        )
        self.assertEqual(s.vencer_suscripciones(), 1)
        sub.refresh_from_db()
        self.assertEqual(sub.estado, "VENCIDA")

    def test_vistas_admin_de_pagos_con_permisos(self):
        pago = self.nuevo_pago()
        self.entrar(self.admin_api)
        r = self.client.post("/admin/pagos/", {"pago": pago.pk, "accion": "verificar"})
        self.assertEqual(r.status_code, 302)
        pago.refresh_from_db()
        self.assertEqual(pago.estado, "VERIFICADO")
        pago2 = self.nuevo_pago()
        self.client.logout()
        self.entrar(self.emp_api)
        self.client.post("/admin/pagos/", {"pago": pago2.pk, "accion": "verificar"})
        pago2.refresh_from_db()
        self.assertEqual(pago2.estado, "VERIFICADO")


class NavegacionPublicaTests(BaseTest):
    def test_paginas_publicas_y_formulario(self):
        for url in (
            "/",
            "/planes/",
            "/solicitud-suscripcion/",
            "/login/",
            "/login/empresa/",
        ):
            self.assertEqual(self.client.get(url).status_code, 200, url)
        r = self.client.get("/planes/")
        self.assertContains(r, "Plan Básico")
        self.assertContains(r, "Hasta 60 colmenas")
        r = self.client.post(
            "/solicitud-suscripcion/",
            {
                "nombre_empresa": "X",
                "nit": self.emp_a.nit,
                "correo_empresa": "a@a.co",
                "nombre_contacto": "N",
                "correo_contacto": "n@a.co",
                "tarjeta_falsa": "4111111111111111",
                "plan": Plan.objects.first().pk,
            },
        )
        self.assertContains(r, "Ya existe una empresa")

    def test_csrf_activo_en_formularios(self):
        from django.test import Client

        c = Client(enforce_csrf_checks=True)
        self.assertEqual(
            c.post("/login/", {"correo": "a@b.co", "password": "x"}).status_code, 403
        )


class DemoTests(BaseTest):
    def test_cargar_demo_es_repetible_y_marca_datos_simulados(self):
        from django.core.management import call_command
        from apps.monitoreo.models import Medicion

        call_command("cargar_demo", "--password", "Demo-Clave-2026", verbosity=0)
        call_command("cargar_demo", "--password", "Demo-Clave-2026", verbosity=0)
        self.assertEqual(Medicion.objects.count(), 15)
        self.assertTrue(
            all(
                m.origen == "SIMULADO" and m.fuente == "DEMOSTRACION"
                for m in Medicion.objects.all()
            )
        )
        self.assertEqual(Empresa.objects.filter(nit="900123456-7").count(), 1)
