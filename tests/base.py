from datetime import timedelta
from itertools import count

from django.test import TestCase
from django.utils import timezone

from apps.apiario.models import Colmena, Sensor, SensorColmena
from apps.empresas.models import Empresa
from apps.monitoreo.models import Monitoreo
from apps.suscripciones.models import Plan, Suscripcion
from apps.usuarios.models import Rol, Usuario

_n = count(1)
CLAVE = "Cl4ve-de-prueba-2026"


def crear_empresa(
    nombre="Empresa", estado=Empresa.ACTIVA, plan="Plan Básico", vence_en=30
):
    n = next(_n)
    empresa = Empresa.objects.create(
        nit=f"900{n:06d}-1", nombre_empresa=f"{nombre} {n}", estado=estado
    )
    if plan:
        Suscripcion.objects.create(
            empresa=empresa,
            plan=Plan.objects.get(nombre_plan=plan),
            estado=Suscripcion.ACTIVA,
            fecha_inicio=timezone.localdate(),
            fecha_vencimiento=timezone.localdate() + timedelta(days=vence_en),
        )
    return empresa


def crear_usuario(rol, empresa=None, correo=None, estado=Usuario.ACTIVO):
    n = next(_n)
    return Usuario.objects.create_user(
        correo or f"u{n}@test.co",
        CLAVE,
        rol,
        empresa=empresa,
        nombre=f"U{n}",
        estado=estado,
    )


def crear_sensor_con_monitoreo(
    empresa, tipo="TEMPERATURA", colmena=None, es_simulado=True, inicio_hace_horas=48
):
    """Sensor + colmena Estable + asociación + monitoreo, con inicio en el pasado (para lecturas históricas)."""
    n = next(_n)
    colmena = colmena or Colmena.objects.create(empresa=empresa, nombre=f"Colmena {n}")
    sensor = Sensor.objects.create(
        empresa=empresa, codigo=f"S-{n}", tipo=tipo, es_simulado=es_simulado
    )
    asoc = SensorColmena.objects.create(sensor=sensor, colmena=colmena)
    inicio = timezone.now() - timedelta(hours=inicio_hace_horas)
    SensorColmena.objects.filter(pk=asoc.pk).update(fecha_inicio=inicio)
    mon = Monitoreo.objects.create(asociacion=asoc)
    Monitoreo.objects.filter(pk=mon.pk).update(fecha_inicio=inicio)
    asoc.refresh_from_db()
    return sensor, asoc, colmena


class BaseTest(TestCase):
    def setUp(self):
        self.emp_a = crear_empresa("A")
        self.emp_b = crear_empresa("B")
        self.admin_a = crear_usuario(Rol.ADMIN_CLIENTE, self.emp_a)
        self.emp_cli_a = crear_usuario(Rol.EMPLEADO_CLIENTE, self.emp_a)
        self.admin_b = crear_usuario(Rol.ADMIN_CLIENTE, self.emp_b)
        self.admin_api = crear_usuario(Rol.ADMIN_APITECH)
        self.emp_api = crear_usuario(Rol.EMPLEADO_APITECH)

    def entrar(self, usuario):
        """Inicia sesión por el flujo real de la aplicación (NIT + credenciales)."""
        if not usuario.es_interno:
            self.client.post("/login/empresa/", {"nit": usuario.empresa.nit})
        r = self.client.post("/login/", {"correo": usuario.correo, "password": CLAVE})
        self.assertEqual(r.status_code, 302, "el inicio de sesión debía redirigir")
        return r
