from django.apps import AppConfig
from django.db.models.signals import post_migrate


def sembrar_planes(sender, **kwargs):
    """Planes oficiales de MK5 (solo dos). Idempotente: no pisa cambios hechos por el administrador."""
    from .models import Plan

    Plan.objects.get_or_create(
        nombre_plan="Plan Básico",
        defaults=dict(
            precio="49000.00", duracion_dias=30, max_colmenas=20, max_sensores=20
        ),
    )
    Plan.objects.get_or_create(
        nombre_plan="Plan Profesional",
        defaults=dict(
            precio="99000.00", duracion_dias=30, max_colmenas=60, max_sensores=60
        ),
    )


class SuscripcionesConfig(AppConfig):
    name = "apps.suscripciones"
    label = "suscripciones"

    def ready(self):
        post_migrate.connect(sembrar_planes, sender=self)
