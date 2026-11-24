from django.conf import settings
from django.core.exceptions import ValidationError
from django.db import models

from apps.comun.campos import activo_uniq


class TipoMedicion(models.TextChoices):
    TEMPERATURA = "TEMPERATURA", "Temperatura"
    HUMEDAD = "HUMEDAD", "Humedad"
    PESO = "PESO", "Peso"


UNIDADES = {
    TipoMedicion.TEMPERATURA: "°C",
    TipoMedicion.HUMEDAD: "%",
    TipoMedicion.PESO: "kg",
}


class Colmena(models.Model):
    ESTABLE, ADVERTENCIA, CRITICO = "Estable", "Advertencia", "Crítico"
    ESTADOS = [(ESTABLE, "Estable"), (ADVERTENCIA, "Advertencia"), (CRITICO, "Crítico")]

    empresa = models.ForeignKey(
        "empresas.Empresa", on_delete=models.PROTECT, related_name="colmenas"
    )
    usuario_registro = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
        related_name="+",
    )
    numero = models.PositiveIntegerField(null=True, blank=True)
    numero_historico = models.PositiveIntegerField(null=True, blank=True)
    archivada = models.BooleanField(default=False)
    nombre = models.CharField(max_length=100)
    ubicacion = models.CharField(max_length=150, blank=True)
    estado = models.CharField(max_length=50, choices=ESTADOS, default=ESTABLE)
    fecha_creacion = models.DateTimeField(auto_now_add=True)
    fecha_actualizacion = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "colmenas"
        constraints = [
            models.UniqueConstraint(
                fields=["empresa", "numero"], name="uq_colmena_numero_empresa"
            )
        ]
        indexes = [models.Index(fields=["empresa", "estado"])]

    def __str__(self):
        return self.nombre


class Sensor(models.Model):
    ACTIVO, INACTIVO = "activo", "inactivo"
    # Estado de la sincronización con el servicio externo (separado del estado deseado `estado`).
    SYNC_NO_APLICA, SYNC_PENDIENTE, SYNC_CONFIRMADO, SYNC_ERROR = (
        "NO_APLICA",
        "PENDIENTE",
        "CONFIRMADO",
        "ERROR",
    )
    SYNC = [
        (SYNC_NO_APLICA, "No aplica"),
        (SYNC_PENDIENTE, "Pendiente"),
        (SYNC_CONFIRMADO, "Confirmado"),
        (SYNC_ERROR, "Error"),
    ]

    empresa = models.ForeignKey(
        "empresas.Empresa", on_delete=models.PROTECT, related_name="sensores"
    )
    codigo = models.CharField(max_length=50)
    tipo = models.CharField(max_length=12, choices=TipoMedicion.choices)
    modelo = models.CharField(max_length=100, blank=True)
    fabricante = models.CharField(max_length=100, blank=True)
    es_simulado = models.BooleanField(default=True)
    estado = models.CharField(max_length=20, default=ACTIVO)  # estado DESEADO
    fecha_registro = models.DateTimeField(auto_now_add=True)
    fecha_actualizacion = models.DateTimeField(auto_now=True)
    fecha_activacion = models.DateTimeField(null=True, blank=True)
    fecha_desactivacion = models.DateTimeField(null=True, blank=True)
    # Integración externa (contrato provisional)
    id_externo = models.CharField(max_length=100, null=True, blank=True)
    sync_estado = models.CharField(max_length=10, choices=SYNC, default=SYNC_NO_APLICA)
    sync_error = models.CharField(max_length=255, blank=True)
    ultima_sincronizacion = models.DateTimeField(null=True, blank=True)
    cursor_sincronizacion = models.CharField(max_length=100, blank=True)

    class Meta:
        db_table = "sensores"
        constraints = [
            models.UniqueConstraint(
                fields=["empresa", "codigo"], name="uq_sensor_codigo_empresa"
            ),
            models.UniqueConstraint(
                fields=["empresa", "id_externo"], name="uq_sensor_id_externo"
            ),
        ]
        indexes = [models.Index(fields=["empresa", "tipo"])]

    def __str__(self):
        return self.codigo


class SensorColmena(models.Model):
    sensor = models.ForeignKey(
        Sensor, on_delete=models.PROTECT, related_name="asociaciones"
    )
    colmena = models.ForeignKey(
        Colmena, on_delete=models.PROTECT, related_name="asociaciones"
    )
    # Denormalizada a propósito (como en MK5) para consultas por empresa; se valida en save().
    empresa = models.ForeignKey(
        "empresas.Empresa", on_delete=models.PROTECT, related_name="+"
    )
    fecha_inicio = models.DateTimeField(auto_now_add=True)
    fecha_fin = models.DateTimeField(null=True, blank=True)
    activo = models.BooleanField(default=True)
    activo_uniq = activo_uniq(activo=True)

    class Meta:
        db_table = "sensor_colmena"
        constraints = [
            models.UniqueConstraint(
                fields=["sensor", "activo_uniq"], name="uq_sensor_activo"
            )
        ]
        indexes = [models.Index(fields=["colmena", "activo"])]

    def clean(self):
        """Réplica de las FK compuestas de MK5: sensor, colmena y asociación, misma empresa."""
        if (
            self.sensor.empresa_id != self.colmena.empresa_id
            or self.empresa_id != self.sensor.empresa_id
        ):
            raise ValidationError(
                "El sensor y la colmena deben pertenecer a la misma empresa."
            )

    def save(self, *args, **kwargs):
        if self.empresa_id is None:
            self.empresa_id = self.sensor.empresa_id
        self.clean()
        super().save(*args, **kwargs)


class ProduccionMiel(models.Model):
    """Miel cosechada. Es independiente del peso medido por el sensor."""

    colmena = models.ForeignKey(
        Colmena, on_delete=models.PROTECT, related_name="producciones"
    )
    empresa = models.ForeignKey(
        "empresas.Empresa", on_delete=models.PROTECT, related_name="+"
    )
    referencia_importacion = models.CharField(
        max_length=64, null=True, blank=True, unique=True
    )
    cantidad = models.DecimalField(max_digits=10, decimal_places=3)
    unidad = models.CharField(max_length=10, default="kg")
    fecha = models.DateField()
    observaciones = models.TextField(blank=True)

    class Meta:
        db_table = "produccion_miel"
        indexes = [models.Index(fields=["empresa", "fecha"])]
        constraints = [
            models.CheckConstraint(
                condition=models.Q(cantidad__gte=0), name="ck_produccion_no_negativa"
            )
        ]

    def save(self, *args, **kwargs):
        if self.empresa_id is None:
            self.empresa_id = self.colmena.empresa_id
        if self.empresa_id != self.colmena.empresa_id:
            raise ValidationError(
                "La producción debe pertenecer a la empresa de su colmena."
            )
        super().save(*args, **kwargs)
