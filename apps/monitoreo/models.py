from django.conf import settings
from django.db import models
from django.db.models import Q
from django.db.models.functions import Coalesce

from apps.apiario.models import TipoMedicion
from apps.comun.campos import activo_uniq


class Monitoreo(models.Model):
    ACTIVO, INACTIVO = "activo", "inactivo"

    asociacion = models.ForeignKey(
        "apiario.SensorColmena", on_delete=models.PROTECT, related_name="monitoreos"
    )
    estado = models.CharField(max_length=20, default=ACTIVO)
    fecha_inicio = models.DateTimeField(auto_now_add=True)
    fecha_fin = models.DateTimeField(null=True, blank=True)
    activo_uniq = activo_uniq(estado="activo")

    class Meta:
        db_table = "monitoreo"
        constraints = [
            models.UniqueConstraint(
                fields=["asociacion", "activo_uniq"], name="uq_monitoreo_activo"
            )
        ]


class Medicion(models.Model):
    SIMULADO, REAL = "SIMULADO", "REAL"
    ORIGENES = [(SIMULADO, "Simulado"), (REAL, "Real")]
    FUENTE_API, FUENTE_IMPORT, FUENTE_MANUAL, FUENTE_MK5, FUENTE_DEMO = (
        "API_EXTERNA",
        "IMPORTACION",
        "MANUAL",
        "MIGRACION_MK5",
        "DEMOSTRACION",
    )

    id = models.BigAutoField(primary_key=True)
    asociacion = models.ForeignKey(
        "apiario.SensorColmena", on_delete=models.PROTECT, related_name="mediciones"
    )
    # Redundante con asociacion.sensor: permite la identidad estable (sensor, id_externo).
    sensor = models.ForeignKey(
        "apiario.Sensor", on_delete=models.PROTECT, related_name="mediciones"
    )
    tipo_medicion = models.CharField(max_length=12, choices=TipoMedicion.choices)
    valor = models.DecimalField(max_digits=10, decimal_places=3)
    unidad = models.CharField(max_length=10)
    origen = models.CharField(max_length=10, choices=ORIGENES, default=SIMULADO)
    fuente = models.CharField(max_length=20, default=FUENTE_MANUAL)
    fecha = models.DateTimeField()  # momento de la medición
    fecha_recepcion = models.DateTimeField(
        auto_now_add=True
    )  # momento en que MK6 la recibió
    id_externo = models.CharField(max_length=100, null=True, blank=True)

    class Meta:
        db_table = "mediciones"
        constraints = [
            models.UniqueConstraint(
                fields=["sensor", "fuente", "id_externo"],
                name="uq_medicion_sensor_externo",
            )
        ]
        indexes = [
            models.Index(fields=["asociacion", "fecha"]),
            models.Index(fields=["tipo_medicion", "fecha"]),
        ]


class RangoUmbral(models.Model):
    empresa = models.ForeignKey(
        "empresas.Empresa",
        null=True,
        blank=True,
        on_delete=models.PROTECT,
        related_name="umbrales",
    )
    tipo_medicion = models.CharField(max_length=12, choices=TipoMedicion.choices)
    valor_min = models.DecimalField(max_digits=10, decimal_places=3)
    valor_max = models.DecimalField(max_digits=10, decimal_places=3)
    unidad = models.CharField(max_length=10)
    estado = models.CharField(max_length=20, default="activo")
    es_demostracion = models.BooleanField(
        default=False
    )  # valores de ejemplo, NO recomendaciones técnicas
    fecha_configuracion = models.DateTimeField(auto_now_add=True)
    empresa_norm = models.GeneratedField(
        expression=Coalesce("empresa_id", 0),
        output_field=models.IntegerField(),
        db_persist=True,
    )

    class Meta:
        db_table = "rangos_umbrales"
        constraints = [
            models.CheckConstraint(
                condition=Q(valor_min__lt=models.F("valor_max")), name="ck_umbral_rango"
            ),
            models.UniqueConstraint(
                fields=["empresa_norm", "tipo_medicion"], name="uq_umbral_empresa_tipo"
            ),
        ]


class Alerta(models.Model):
    ACTIVA, RESUELTA = "ACTIVA", "RESUELTA"

    colmena = models.ForeignKey(
        "apiario.Colmena", on_delete=models.PROTECT, related_name="alertas"
    )
    empresa = models.ForeignKey(
        "empresas.Empresa", on_delete=models.PROTECT, related_name="alertas"
    )
    medicion = models.OneToOneField(
        Medicion, null=True, blank=True, on_delete=models.PROTECT, related_name="alerta"
    )
    sensor = models.ForeignKey(
        "apiario.Sensor",
        null=True,
        blank=True,
        on_delete=models.PROTECT,
        related_name="+",
    )
    tipo = models.CharField(max_length=100)
    descripcion = models.TextField(blank=True)
    valor_detectado = models.DecimalField(
        max_digits=10, decimal_places=3, null=True, blank=True
    )
    umbral_min = models.DecimalField(
        max_digits=10, decimal_places=3, null=True, blank=True
    )
    umbral_max = models.DecimalField(
        max_digits=10, decimal_places=3, null=True, blank=True
    )
    severidad = models.CharField(max_length=20, default="media")
    estado = models.CharField(max_length=20, default=ACTIVA)
    # Una sola alerta ACTIVA por colmena y tipo, aun con procesamiento concurrente.
    activa_uniq = activo_uniq(estado="ACTIVA")
    fecha = models.DateTimeField(auto_now_add=True)
    fecha_resolucion = models.DateTimeField(null=True, blank=True)
    usuario_resolucion = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        null=True,
        blank=True,
        on_delete=models.PROTECT,
        related_name="+",
    )

    class Meta:
        db_table = "alertas"
        constraints = [
            models.UniqueConstraint(
                fields=["colmena", "tipo", "activa_uniq"], name="uq_alerta_activa"
            )
        ]
        indexes = [models.Index(fields=["empresa", "estado"])]

    def save(self, *args, **kwargs):
        if self.empresa_id is None:
            self.empresa_id = self.colmena.empresa_id
        if self.empresa_id != self.colmena.empresa_id:
            raise ValueError("La alerta debe pertenecer a la empresa de su colmena.")
        if self.medicion_id and (
            self.medicion.asociacion.colmena_id != self.colmena_id
            or self.medicion.sensor_id != self.sensor_id
        ):
            raise ValueError(
                "La medición de la alerta no corresponde a su sensor y colmena."
            )
        super().save(*args, **kwargs)


class Notificacion(models.Model):
    usuario = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.PROTECT,
        related_name="notificaciones",
    )
    empresa = models.ForeignKey(
        "empresas.Empresa",
        null=True,
        blank=True,
        on_delete=models.PROTECT,
        related_name="+",
    )
    tipo = models.CharField(max_length=50)
    titulo = models.CharField(max_length=150)
    mensaje = models.TextField(blank=True)
    leida = models.BooleanField(default=False)
    fecha = models.DateTimeField(auto_now_add=True)
    alerta = models.ForeignKey(
        Alerta,
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
        related_name="notificaciones",
    )
    pago = models.ForeignKey(
        "suscripciones.Pago",
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
        related_name="+",
    )

    class Meta:
        db_table = "notificaciones"
        indexes = [models.Index(fields=["usuario", "leida"])]

    def save(self, *args, **kwargs):
        """Réplica de trg_notif_empresa_*: la empresa es la del usuario destinatario."""
        if self.empresa_id != self.usuario.empresa_id:
            if self.empresa_id is None or self.usuario.empresa_id is None:
                self.empresa_id = self.usuario.empresa_id
            else:
                raise ValueError(
                    "La notificación debe pertenecer a la empresa del usuario."
                )
        if self.alerta_id and self.alerta.empresa_id != self.empresa_id:
            raise ValueError("Alerta de otra empresa.")
        if (
            self.pago_id
            and self.empresa_id is not None
            and self.pago.suscripcion.empresa_id != self.empresa_id
        ):
            raise ValueError("Pago de otra empresa.")
        super().save(*args, **kwargs)
