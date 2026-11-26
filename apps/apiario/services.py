from django.db import IntegrityError, transaction
from django.utils import timezone

from apps.auditoria.models import registrar
from apps.comun.acceso import exigir
from apps.comun.errors import ReglaNegocioError
from apps.empresas.models import Empresa
from apps.empresas.services import limites, validar_puede_operar
from apps.monitoreo.models import Monitoreo
from apps.usuarios import permisos as P

from .models import Colmena, ProduccionMiel, Sensor, SensorColmena


def _empresa_bloqueada(usuario):
    """Bloquea la fila de la empresa: serializa operaciones que comprueban límites del plan."""
    empresa = Empresa.objects.select_for_update().get(pk=usuario.empresa_id)
    validar_puede_operar(empresa)
    return empresa


@transaction.atomic
def crear_colmena(usuario, nombre, ubicacion="", estado=Colmena.ESTABLE):
    exigir(usuario, P.COLMENAS_GESTIONAR, usuario.empresa_id)
    empresa = _empresa_bloqueada(usuario)
    max_colmenas, _ = limites(empresa)
    if empresa.colmenas.filter(archivada=False).count() >= max_colmenas:
        raise ReglaNegocioError(
            f"Has alcanzado el límite de {max_colmenas} colmenas de tu plan."
        )
    nombre = (nombre or "").strip()
    if not nombre:
        raise ReglaNegocioError("El nombre de la colmena es obligatorio.")
    if estado not in dict(Colmena.ESTADOS):
        raise ReglaNegocioError("Estado de colmena no válido.")
    usados = set(empresa.colmenas.exclude(numero=None).values_list("numero", flat=True))
    numero = 1
    while numero in usados:
        numero += 1
    colmena = Colmena.objects.create(
        empresa=empresa,
        numero=numero,
        numero_historico=numero,
        usuario_registro=usuario,
        nombre=nombre,
        ubicacion=(ubicacion or "").strip(),
        estado=estado,
    )
    registrar("COLMENA_CREADA", "apiario", colmena, nombre, usuario)
    return colmena


@transaction.atomic
def actualizar_colmena(usuario, colmena, nombre, ubicacion, estado):
    exigir(usuario, P.COLMENAS_GESTIONAR, colmena.empresa_id)
    validar_puede_operar(colmena.empresa)
    if not (nombre or "").strip():
        raise ReglaNegocioError("El nombre de la colmena es obligatorio.")
    if estado not in dict(Colmena.ESTADOS):
        raise ReglaNegocioError("Estado de colmena no válido.")
    colmena.nombre, colmena.ubicacion, colmena.estado = (
        nombre.strip(),
        (ubicacion or "").strip(),
        estado,
    )
    colmena.save()
    registrar("COLMENA_ACTUALIZADA", "apiario", colmena, usuario=usuario)
    return colmena


@transaction.atomic
def registrar_sensor(usuario, codigo, tipo, modelo="", fabricante="", es_simulado=True):
    """Crea el sensor LOCAL. El registro externo (si aplica) se hace después, fuera de la transacción."""
    exigir(usuario, P.SENSORES_GESTIONAR, usuario.empresa_id)
    empresa = _empresa_bloqueada(usuario)
    _, max_sensores = limites(empresa)
    if empresa.sensores.count() >= max_sensores:
        raise ReglaNegocioError(
            f"Has alcanzado el límite de {max_sensores} sensores de tu plan."
        )
    from .models import TipoMedicion

    if tipo not in TipoMedicion.values:
        raise ReglaNegocioError("Tipo de sensor no válido.")
    codigo = (codigo or "").strip()
    if not codigo:
        raise ReglaNegocioError("El código del sensor es obligatorio.")
    try:
        with transaction.atomic():
            sensor = Sensor.objects.create(
                empresa=empresa,
                codigo=codigo,
                tipo=tipo,
                modelo=modelo,
                fabricante=fabricante,
                es_simulado=es_simulado,
                fecha_activacion=timezone.now(),
            )
    except IntegrityError:
        raise ReglaNegocioError(f"Ya existe un sensor con el código {codigo}.")
    registrar("SENSOR_REGISTRADO", "apiario", sensor, codigo, usuario)
    return sensor


@transaction.atomic
def asociar_sensor(usuario, sensor, colmena):
    exigir(usuario, P.SENSORES_GESTIONAR, sensor.empresa_id)
    exigir(
        usuario, P.SENSORES_GESTIONAR, colmena.empresa_id
    )  # valida TAMBIÉN el objeto relacionado
    if sensor.empresa_id != colmena.empresa_id:
        raise ReglaNegocioError(
            "El sensor y la colmena deben pertenecer a la misma empresa."
        )
    validar_puede_operar(sensor.empresa)
    sensor = Sensor.objects.select_for_update().get(pk=sensor.pk)
    if colmena.archivada:
        raise ReglaNegocioError("La colmena está archivada.")
    if sensor.estado != Sensor.ACTIVO:
        raise ReglaNegocioError("El sensor está inactivo.")
    if sensor.asociaciones.filter(activo=True).exists():
        raise ReglaNegocioError(
            f"El sensor {sensor.codigo} ya tiene una asociación activa; desactívela primero."
        )
    try:
        with transaction.atomic():
            asociacion = SensorColmena.objects.create(
                sensor=sensor, colmena=colmena, empresa=sensor.empresa
            )
    except IntegrityError:
        raise ReglaNegocioError(
            f"El sensor {sensor.codigo} ya tiene una asociación activa."
        )
    registrar(
        "SENSOR_ASOCIADO",
        "apiario",
        asociacion,
        f"{sensor.codigo} → {colmena.nombre}",
        usuario,
    )
    return asociacion


@transaction.atomic
def desasociar_sensor(usuario, asociacion):
    exigir(usuario, P.SENSORES_GESTIONAR, asociacion.empresa_id)
    validar_puede_operar(asociacion.empresa)
    if asociacion.monitoreos.filter(estado=Monitoreo.ACTIVO).exists():
        raise ReglaNegocioError(
            "No se puede desactivar la asociación porque tiene un monitoreo activo; desactívelo primero."
        )
    asociacion.activo, asociacion.fecha_fin = False, timezone.now()
    asociacion.save()
    registrar(
        "SENSOR_DESASOCIADO", "apiario", asociacion, usuario=usuario
    )  # el historial se conserva
    return asociacion


@transaction.atomic
def activar_monitoreo(usuario, asociacion):
    exigir(usuario, P.MONITOREO_GESTIONAR, asociacion.empresa_id)
    validar_puede_operar(asociacion.empresa)
    if not asociacion.activo or asociacion.sensor.estado != Sensor.ACTIVO:
        raise ReglaNegocioError("Monitoreo requiere asociación y sensor activos.")
    if asociacion.colmena.estado != Colmena.ESTABLE:
        raise ReglaNegocioError(
            "La colmena debe estar en estado Estable para activar el monitoreo."
        )
    try:
        with transaction.atomic():
            monitoreo = Monitoreo.objects.create(asociacion=asociacion)
    except IntegrityError:
        raise ReglaNegocioError("La asociación ya tiene un monitoreo activo.")
    registrar("MONITOREO_ACTIVADO", "apiario", monitoreo, usuario=usuario)
    return monitoreo


@transaction.atomic
def desactivar_monitoreo(usuario, monitoreo):
    exigir(usuario, P.MONITOREO_GESTIONAR, monitoreo.asociacion.empresa_id)
    validar_puede_operar(monitoreo.asociacion.empresa)
    if monitoreo.estado != Monitoreo.ACTIVO:
        raise ReglaNegocioError("El monitoreo ya está inactivo.")
    monitoreo.estado, monitoreo.fecha_fin = Monitoreo.INACTIVO, timezone.now()
    monitoreo.save()
    registrar("MONITOREO_DESACTIVADO", "apiario", monitoreo, usuario=usuario)
    return monitoreo


@transaction.atomic
def cambiar_estado_sensor(usuario, sensor, estado):
    exigir(usuario, P.SENSORES_GESTIONAR, sensor.empresa_id)
    validar_puede_operar(sensor.empresa)
    if estado not in (Sensor.ACTIVO, Sensor.INACTIVO):
        raise ReglaNegocioError("Estado de sensor no válido.")
    if (
        estado == Sensor.INACTIVO
        and Monitoreo.objects.filter(
            asociacion__sensor=sensor, estado=Monitoreo.ACTIVO
        ).exists()
    ):
        raise ReglaNegocioError(
            "No se puede desactivar un sensor con monitoreo activo."
        )
    sensor.estado = estado
    sensor.fecha_activacion = (
        timezone.now() if estado == Sensor.ACTIVO else sensor.fecha_activacion
    )
    sensor.fecha_desactivacion = timezone.now() if estado == Sensor.INACTIVO else None
    sensor.save()
    registrar("SENSOR_ESTADO", "apiario", sensor, estado, usuario)
    return sensor


@transaction.atomic
def registrar_produccion(usuario, colmena, cantidad, fecha, observaciones=""):
    exigir(usuario, P.PRODUCCION_REGISTRAR, colmena.empresa_id)
    validar_puede_operar(colmena.empresa)
    if cantidad is None or cantidad < 0:
        raise ReglaNegocioError("La producción no puede ser negativa.")
    produccion = ProduccionMiel.objects.create(
        colmena=colmena, cantidad=cantidad, fecha=fecha, observaciones=observaciones
    )
    registrar("PRODUCCION_REGISTRADA", "apiario", produccion, usuario=usuario)
    return produccion


@transaction.atomic
def archivar_colmena(usuario, colmena):
    exigir(usuario, P.COLMENAS_GESTIONAR, colmena.empresa_id)
    validar_puede_operar(colmena.empresa)
    Empresa.objects.select_for_update().get(pk=colmena.empresa_id)
    colmena = Colmena.objects.select_for_update().get(pk=colmena.pk)
    if colmena.archivada:
        raise ReglaNegocioError("La colmena ya está archivada.")
    from apps.integraciones.services import encolar

    ahora = timezone.now()
    for a in colmena.asociaciones.filter(activo=True):
        encolar(a.sensor, "DEACTIVATE", a)
        a.monitoreos.filter(estado=Monitoreo.ACTIVO).update(
            estado=Monitoreo.INACTIVO, fecha_fin=ahora
        )
        a.activo = False
        a.fecha_fin = ahora
        a.save()
    colmena.numero_historico = colmena.numero
    colmena.numero = None
    colmena.archivada = True
    colmena.save()
    registrar("COLMENA_ARCHIVADA", "apiario", colmena, usuario=usuario)
    return colmena
