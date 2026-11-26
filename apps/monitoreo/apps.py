from django.apps import AppConfig
from django.db.models.signals import post_migrate


def sembrar_umbrales_demo(sender, **kwargs):
    """Umbrales globales de DEMOSTRACIÓN (heredados de MK5). No son recomendaciones técnicas apícolas:
    cada empresa debe configurar los suyos. Idempotente."""
    from .models import RangoUmbral

    for tipo, vmin, vmax, unidad in (
        ("TEMPERATURA", 18, 35, "°C"),
        ("HUMEDAD", 35, 80, "%"),
        ("PESO", 0, 80, "kg"),
    ):
        if not RangoUmbral.objects.filter(
            empresa__isnull=True, tipo_medicion=tipo
        ).exists():
            RangoUmbral.objects.create(
                tipo_medicion=tipo,
                valor_min=vmin,
                valor_max=vmax,
                unidad=unidad,
                es_demostracion=True,
            )


class MonitoreoConfig(AppConfig):
    name = "apps.monitoreo"
    label = "monitoreo"

    def ready(self):
        post_migrate.connect(sembrar_umbrales_demo, sender=self)
