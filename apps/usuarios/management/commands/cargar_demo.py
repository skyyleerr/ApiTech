"""Carga datos FICTICIOS de demostración (no son datos reales de MK5). Repetible: no duplica."""

import secrets
from datetime import timedelta

from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from django.utils import timezone

from apps.apiario.models import Colmena, Sensor, SensorColmena
from apps.empresas.models import Empresa
from apps.monitoreo.models import Medicion, Monitoreo
from apps.monitoreo import services as mon
from apps.suscripciones.models import Plan, Suscripcion
from apps.usuarios.models import Rol, Usuario


class Command(BaseCommand):
    help = "Crea empresa, usuarios, colmenas y sensores de DEMOSTRACIÓN. Use --password o se genera una aleatoria."

    def add_arguments(self, parser):
        parser.add_argument(
            "--password", help="Contraseña común para los usuarios demo (mín. 8)."
        )

    @transaction.atomic
    def handle(self, *args, **o):
        clave = o["password"] or secrets.token_urlsafe(10)
        if len(clave) < 8:
            raise CommandError("La contraseña debe tener al menos 8 caracteres.")
        empresa, creada = Empresa.objects.get_or_create(
            nit="900123456-7",
            defaults=dict(
                nombre_empresa="Apícola El Panal S.A.S. (DEMO)",
                correo="contacto@demo.invalid",
            ),
        )
        usuarios = [
            ("admin.apitech@demo.invalid", Rol.ADMIN_APITECH, None),
            ("operador.apitech@demo.invalid", Rol.EMPLEADO_APITECH, None),
            ("admin@demo-cliente.invalid", Rol.ADMIN_CLIENTE, empresa),
            ("empleado@demo-cliente.invalid", Rol.EMPLEADO_CLIENTE, empresa),
        ]
        for correo, rol, emp in usuarios:
            if not Usuario.objects.filter(correo=correo).exists():
                Usuario.objects.create_user(
                    correo, clave, rol, empresa=emp, nombre="Demo"
                )
        hoy = timezone.localdate()
        if not Suscripcion.objects.filter(empresa=empresa, estado="ACTIVA").exists():
            Suscripcion.objects.create(
                empresa=empresa,
                plan=Plan.objects.get(nombre_plan="Plan Básico"),
                estado="ACTIVA",
                fecha_inicio=hoy,
                fecha_vencimiento=hoy + timedelta(days=30),
            )
        if not empresa.colmenas.exists():
            norte = Colmena.objects.create(
                empresa=empresa,
                numero=1,
                numero_historico=1,
                nombre="Panal Norte (demo)",
                ubicacion="Sector demo A",
            )
            Colmena.objects.create(
                empresa=empresa,
                numero=2,
                numero_historico=2,
                nombre="Panal Sur (demo)",
                ubicacion="Sector demo B",
                estado="Advertencia",
            )
            inicio = timezone.now() - timedelta(hours=6)
            for codigo, tipo, base in (
                ("DEMO-TEMP-001", "TEMPERATURA", 28),
                ("DEMO-HUM-001", "HUMEDAD", 60),
                ("DEMO-PESO-001", "PESO", 18),
            ):
                sensor = Sensor.objects.create(
                    empresa=empresa,
                    codigo=codigo,
                    tipo=tipo,
                    modelo="Demo",
                    fabricante="ApiTech",
                )
                asoc = SensorColmena.objects.create(sensor=sensor, colmena=norte)
                SensorColmena.objects.filter(pk=asoc.pk).update(fecha_inicio=inicio)
                m = Monitoreo.objects.create(asociacion=asoc)
                Monitoreo.objects.filter(pk=m.pk).update(fecha_inicio=inicio)
                for i in range(5):  # lecturas ficticias con identidad y origen SIMULADO
                    mon.registrar_medicion(
                        sensor,
                        base + i * 0.5,
                        inicio + timedelta(hours=i + 1),
                        id_externo=f"demo-{codigo}-{i}",
                        fuente=Medicion.FUENTE_DEMO,
                    )
        self.stdout.write(self.style.SUCCESS("Datos de demostración listos."))
        if o["password"] is None:
            self.stdout.write(
                f"Contraseña generada para los usuarios demo (guárdela, no se vuelve a mostrar): {clave}"
            )
        self.stdout.write("NIT de la empresa demo: 900123456-7")
