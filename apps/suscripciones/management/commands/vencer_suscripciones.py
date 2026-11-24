from django.core.management.base import BaseCommand

from apps.suscripciones.services import vencer_suscripciones


class Command(BaseCommand):
    help = "Marca como VENCIDA toda suscripción ACTIVA cuya fecha de vencimiento ya pasó (ejecutar a diario)."

    def handle(self, *args, **opciones):
        self.stdout.write(f"Suscripciones vencidas: {vencer_suscripciones()}")
