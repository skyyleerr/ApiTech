from django.conf import settings
from django.db import models

from apps.comun.campos import activo_uniq


class Plan(models.Model):
    nombre_plan = models.CharField(max_length=100)
    precio = models.DecimalField(max_digits=12, decimal_places=2)
    duracion_dias = models.PositiveIntegerField()
    estado = models.CharField(max_length=20, default="activo")
    # MK5 deducía el límite por el nombre del plan; MK6 lo declara explícitamente.
    max_colmenas = models.PositiveIntegerField()
    max_sensores = models.PositiveIntegerField()

    class Meta:
        db_table = "planes_suscripcion"

    def __str__(self):
        return self.nombre_plan


class Suscripcion(models.Model):
    PENDIENTE, ACTIVA, SUSPENDIDA, VENCIDA, CANCELADA = (
        "PENDIENTE",
        "ACTIVA",
        "SUSPENDIDA",
        "VENCIDA",
        "CANCELADA",
    )
    ESTADOS = [
        (e, e.title()) for e in (PENDIENTE, ACTIVA, SUSPENDIDA, VENCIDA, CANCELADA)
    ]

    empresa = models.ForeignKey(
        "empresas.Empresa", on_delete=models.PROTECT, related_name="suscripciones"
    )
    plan = models.ForeignKey(
        Plan, on_delete=models.PROTECT, related_name="suscripciones"
    )
    estado = models.CharField(max_length=12, choices=ESTADOS, default=PENDIENTE)
    activa_uniq = activo_uniq(estado="ACTIVA")
    fecha_inicio = models.DateField(null=True, blank=True)
    fecha_vencimiento = models.DateField(null=True, blank=True)
    fecha_cancelacion = models.DateField(null=True, blank=True)
    renovacion_automatica = models.BooleanField(default=False)
    fecha_registro = models.DateTimeField(auto_now_add=True)
    fecha_actualizacion = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "suscripciones"
        constraints = [
            models.UniqueConstraint(
                fields=["empresa", "activa_uniq"], name="uq_suscripcion_activa"
            )
        ]
        indexes = [models.Index(fields=["estado"])]


class Pago(models.Model):
    PENDIENTE, VERIFICADO, RECHAZADO = "PENDIENTE", "VERIFICADO", "RECHAZADO"
    ESTADOS = [
        (PENDIENTE, "Pendiente"),
        (VERIFICADO, "Verificado"),
        (RECHAZADO, "Rechazado"),
    ]

    suscripcion = models.ForeignKey(
        Suscripcion, on_delete=models.PROTECT, related_name="pagos"
    )
    valor = models.DecimalField(max_digits=12, decimal_places=2)
    fecha_pago = models.DateField()
    metodo_pago = models.CharField(max_length=50, blank=True)
    referencia = models.CharField(max_length=100, blank=True)
    estado = models.CharField(max_length=10, choices=ESTADOS, default=PENDIENTE)
    fecha_verificacion = models.DateTimeField(null=True, blank=True)
    usuario_verifico = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        null=True,
        blank=True,
        on_delete=models.PROTECT,
        related_name="pagos_verificados",
    )
    observaciones = models.TextField(blank=True)

    class Meta:
        db_table = "pagos"
        indexes = [models.Index(fields=["estado"])]

    @property
    def empresa(self):
        """La empresa se deriva de la suscripción (en MK5 era una columna redundante)."""
        return self.suscripcion.empresa


class SolicitudSuscripcion(models.Model):
    PENDIENTE, APROBADA, RECHAZADA = "PENDIENTE", "APROBADA", "RECHAZADA"
    ESTADOS = [
        (PENDIENTE, "Pendiente"),
        (APROBADA, "Aprobada"),
        (RECHAZADA, "Rechazada"),
    ]

    nombre_empresa = models.CharField(max_length=150)
    nit = models.CharField(max_length=20)
    correo_empresa = models.EmailField(max_length=150)
    telefono = models.CharField(max_length=30, blank=True)
    direccion = models.CharField(max_length=255, blank=True)
    nombre_contacto = models.CharField(max_length=120)
    correo_contacto = models.EmailField(max_length=150)
    telefono_contacto = models.CharField(max_length=30, blank=True)
    plan_solicitado = models.ForeignKey(Plan, null=True, on_delete=models.PROTECT)
    # Solo se conservan 4 dígitos de una tarjeta FICTICIA. No es una pasarela de pagos.
    tarjeta_falsa_ultimos4 = models.CharField(max_length=4, blank=True)
    estado = models.CharField(max_length=10, choices=ESTADOS, default=PENDIENTE)
    pendiente_uniq = activo_uniq(estado="PENDIENTE")
    observaciones = models.TextField(blank=True)
    motivo_rechazo = models.TextField(blank=True)
    usuario_revisor = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
        related_name="solicitudes_revisadas",
    )
    fecha_solicitud = models.DateTimeField(auto_now_add=True)
    fecha_revision = models.DateTimeField(null=True, blank=True)
    fecha_actualizacion = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "solicitudes_suscripcion"
        constraints = [
            models.UniqueConstraint(
                fields=["nit", "pendiente_uniq"], name="uq_solicitud_pendiente_nit"
            )
        ]
