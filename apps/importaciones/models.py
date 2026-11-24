from django.db import models
from django.conf import settings


class LoteImportacion(models.Model):
    empresa = models.ForeignKey("empresas.Empresa", on_delete=models.PROTECT)
    usuario = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT)
    nombre = models.CharField(max_length=200)
    tipo = models.CharField(max_length=20)
    filas = models.JSONField(default=list)
    errores = models.JSONField(default=list)
    resultado = models.JSONField(default=dict)
    confirmado = models.BooleanField(default=False)
    fecha = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "importaciones"
