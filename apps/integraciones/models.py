from django.db import models


class ConexionAPI(models.Model):
    empresa = models.OneToOneField(
        "empresas.Empresa", on_delete=models.PROTECT, related_name="conexion_api"
    )
    habilitada = models.BooleanField(default=False)
    # La URL y token se leen del entorno por empresa; nunca se guardan credenciales en HTML/BD.
    ultimo_exito = models.DateTimeField(null=True, blank=True)
    ultimo_error = models.CharField(max_length=250, blank=True)

    class Meta:
        db_table = "conexiones_api"


class ComandoSensor(models.Model):
    sensor = models.ForeignKey(
        "apiario.Sensor", on_delete=models.PROTECT, related_name="comandos"
    )
    accion = models.CharField(max_length=20)
    asociacion = models.ForeignKey(
        "apiario.SensorColmena", null=True, on_delete=models.PROTECT
    )
    clave = models.UUIDField(unique=True, editable=False)
    completado = models.BooleanField(default=False)
    intentos = models.PositiveIntegerField(default=0)
    error = models.CharField(max_length=250, blank=True)
    fecha = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "comandos_sensor"
