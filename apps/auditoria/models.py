from django.conf import settings
from django.db import models
from django.db.models import Q


class Auditoria(models.Model):
    USUARIO, PROCESO = "USUARIO", "PROCESO"

    usuario = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
        related_name="+",
    )
    actor_tipo = models.CharField(
        max_length=10,
        choices=[(USUARIO, "Usuario"), (PROCESO, "Proceso")],
        default=USUARIO,
    )
    accion = models.CharField(max_length=100)
    modulo = models.CharField(max_length=50, blank=True)
    entidad_tipo = models.CharField(max_length=50, blank=True)
    id_entidad = models.BigIntegerField(null=True, blank=True)
    registro_afectado = models.CharField(max_length=100, blank=True)
    descripcion = models.TextField(blank=True)
    fecha = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "auditoria"
        constraints = [
            models.CheckConstraint(
                condition=(
                    Q(actor_tipo="PROCESO", usuario__isnull=True)
                    | Q(actor_tipo="USUARIO", usuario__isnull=False)
                ),
                name="ck_auditoria_actor",
            )
        ]
        indexes = [
            models.Index(fields=["entidad_tipo", "id_entidad"]),
            models.Index(fields=["fecha"]),
        ]


def registrar(accion, modulo="", entidad=None, descripcion="", usuario=None):
    """Registra una acción. Sin usuario => actor automático de integración o mantenimiento (nunca inicia sesión)."""
    return Auditoria.objects.create(
        usuario=usuario,
        actor_tipo=Auditoria.USUARIO if usuario else Auditoria.PROCESO,
        accion=accion,
        modulo=modulo,
        descripcion=descripcion[:2000],
        entidad_tipo=type(entidad).__name__ if entidad is not None else "",
        id_entidad=getattr(entidad, "pk", None),
    )
