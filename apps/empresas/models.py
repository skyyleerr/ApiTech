from django.db import models


class Empresa(models.Model):
    ACTIVA, PAUSADA = "activa", "pausada"
    ESTADOS = [(ACTIVA, "Activa"), (PAUSADA, "Pausada")]

    nit = models.CharField(max_length=20, unique=True)
    nombre_empresa = models.CharField(max_length=150)
    correo = models.EmailField(max_length=150, blank=True)
    telefono = models.CharField(max_length=30, blank=True)
    direccion = models.CharField(max_length=200, blank=True)
    estado = models.CharField(max_length=20, choices=ESTADOS, default=ACTIVA)
    fecha_registro = models.DateTimeField(auto_now_add=True)
    fecha_actualizacion = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "empresas"

    def __str__(self):
        return f"{self.nombre_empresa} ({self.nit})"

    @property
    def esta_pausada(self):
        return self.estado != self.ACTIVA
