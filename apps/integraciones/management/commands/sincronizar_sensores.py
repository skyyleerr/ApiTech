import time
from django.core.management.base import BaseCommand
from apps.integraciones.models import ConexionAPI
from apps.integraciones.services import sincronizar, APIError


class Command(BaseCommand):
    help = "Consume lecturas externas. No genera datos artificiales."

    def add_arguments(self, p):
        p.add_argument("--continuo", action="store_true")
        p.add_argument("--intervalo", type=int, default=60)

    def handle(self, *args, **o):
        try:
            while True:
                for c in ConexionAPI.objects.filter(habilitada=True).select_related(
                    "empresa"
                ):
                    try:
                        self.stdout.write(
                            f"Empresa {c.empresa_id}: {sincronizar(c.empresa)} lecturas nuevas"
                        )
                    except APIError as e:
                        self.stderr.write(f"Empresa {c.empresa_id}: {e}")
                if not o["continuo"]:
                    break
                time.sleep(max(10, o["intervalo"]))
        except KeyboardInterrupt:
            self.stdout.write("Sincronización detenida.")
