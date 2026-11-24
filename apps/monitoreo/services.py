import logging
from decimal import Decimal, InvalidOperation

from django.db import IntegrityError, transaction
from django.db.models import Q
from django.utils import timezone

from apps.apiario.models import UNIDADES, SensorColmena
from apps.auditoria.models import registrar
from apps.comun.acceso import exigir
from apps.comun.errors import ReglaNegocioError
from apps.empresas.services import validar_puede_operar
from apps.usuarios import permisos as P
from apps.usuarios.models import Usuario

from .models import Alerta, Medicion, Notificacion, RangoUmbral

log = logging.getLogger(__name__)
TOLERANCIA_FUTURO = timezone.timedelta(minutes=5)


def umbral_aplicable(empresa_id, tipo):
    """Prioridad MK5: umbral activo de la empresa; si no existe, el global (empresa NULL)."""
    base = RangoUmbral.objects.filter(tipo_medicion=tipo, estado="activo")
    return (
        base.filter(empresa_id=empresa_id).first()
        or base.filter(empresa__isnull=True).first()
    )


def asociacion_para(sensor, fecha):
    """Asociación que estaba vigente en `fecha` (conserva el historial al reasignar sensores)."""
    return (
        SensorColmena.objects.select_related("colmena", "sensor", "empresa")
        .filter(sensor=sensor, fecha_inicio__lte=fecha)
        .filter(Q(fecha_fin__isnull=True) | Q(fecha_fin__gt=fecha))
        .order_by("-fecha_inicio")
        .first()
    )


def _a_decimal(valor):
    try:
        d = Decimal(str(valor))
    except (InvalidOperation, ValueError):
        raise ReglaNegocioError("El valor de la medición debe ser numérico.")
    if not d.is_finite():
        raise ReglaNegocioError("El valor de la medición debe ser un número finito.")
    if abs(d) > Decimal("9999999.999"):
        raise ReglaNegocioError("Valor fuera del tamaño admitido.")
    return d.quantize(Decimal("0.001"))


@transaction.atomic
def registrar_medicion(
    sensor,
    valor,
    fecha,
    *,
    tipo=None,
    unidad=None,
    id_externo=None,
    fuente=Medicion.FUENTE_MANUAL,
    evaluar=True,
):
    """Punto ÚNICO de ingreso de lecturas (API externa, importación, carga manual).

    Réplica de trg_mediciones_coherencia_*: monitoreo activo en `fecha`, tipo coherente con el sensor y
    origen derivado del sensor (el llamador no lo elige). Además:
      * idempotente por (sensor, id_externo): repetir una lectura no crea duplicados;
      * rechaza fechas futuras y valores no numéricos; acepta lecturas fuera de orden;
      * empresa pausada o sin suscripción vigente: no se almacenan lecturas (como el simulador de MK5).
    Devuelve (medicion, creada).
    """
    if timezone.is_naive(fecha):
        raise ReglaNegocioError("La fecha de la medición debe incluir zona horaria.")
    if fecha > timezone.now() + TOLERANCIA_FUTURO:
        raise ReglaNegocioError("La fecha de la medición está en el futuro.")
    if tipo is not None and tipo != sensor.tipo:
        raise ReglaNegocioError(
            f"El tipo de medición ({tipo}) no coincide con el sensor ({sensor.tipo})."
        )
    esperada = UNIDADES[sensor.tipo]
    if unidad is not None and unidad != esperada:
        raise ReglaNegocioError(
            f"Unidad no válida para {sensor.tipo}: se esperaba {esperada}."
        )
    valor = _a_decimal(valor)
    if sensor.tipo == "HUMEDAD" and not 0 <= valor <= 100:
        raise ReglaNegocioError("La humedad debe estar entre 0 y 100 %.")
    if sensor.tipo == "PESO" and valor < 0:
        raise ReglaNegocioError("El peso no puede ser negativo.")

    if id_externo:
        existente = Medicion.objects.filter(
            sensor=sensor, fuente=fuente, id_externo=id_externo
        ).first()
        if existente:
            if existente.valor != valor or existente.fecha != fecha:
                raise ReglaNegocioError(
                    "Identificador de lectura repetido con datos diferentes."
                )
            return existente, False
    validar_puede_operar(sensor.empresa)
    asociacion = asociacion_para(sensor, fecha)
    if asociacion is None:
        raise ReglaNegocioError(
            "El sensor no estaba asociado a una colmena en esa fecha."
        )
    if (
        not asociacion.monitoreos.filter(fecha_inicio__lte=fecha)
        .filter(Q(fecha_fin__isnull=True) | Q(fecha_fin__gt=fecha))
        .exists()
    ):
        raise ReglaNegocioError(
            "No se puede registrar una medición sin monitoreo activo en esa fecha."
        )

    try:
        with transaction.atomic():
            medicion = Medicion.objects.create(
                asociacion=asociacion,
                sensor=sensor,
                tipo_medicion=sensor.tipo,
                valor=valor,
                unidad=esperada,
                origen=Medicion.SIMULADO if sensor.es_simulado else Medicion.REAL,
                fuente=fuente,
                fecha=fecha,
                id_externo=id_externo or None,
            )
    except IntegrityError:  # carrera: otra sincronización guardó la misma lectura
        existente = Medicion.objects.filter(
            sensor=sensor, fuente=fuente, id_externo=id_externo
        ).first()
        if existente:
            if existente.valor != valor or existente.fecha != fecha:
                raise ReglaNegocioError(
                    "Identificador de lectura repetido con datos diferentes."
                )
            return existente, False
        raise
    if evaluar:
        evaluar_umbral(medicion)
    return medicion, True


def evaluar_umbral(medicion):
    """Crea alerta + notificaciones si el valor sale del rango. Sin duplicados ni siquiera en concurrencia."""
    asociacion = medicion.asociacion
    # Una lectura antigua llegada tarde no debe disparar alertas por un estado ya superado.
    if Medicion.objects.filter(
        asociacion=asociacion,
        tipo_medicion=medicion.tipo_medicion,
        fecha__gt=medicion.fecha,
    ).exists():
        return None
    umbral = umbral_aplicable(asociacion.empresa_id, medicion.tipo_medicion)
    if umbral is None:
        log.warning(
            "Sin umbral configurado para %s; no se evalúa alerta.",
            medicion.tipo_medicion,
        )
        return None
    if umbral.valor_min <= medicion.valor <= umbral.valor_max:
        return None
    distancia = (
        (umbral.valor_min - medicion.valor)
        if medicion.valor < umbral.valor_min
        else (medicion.valor - umbral.valor_max)
    )
    rango = max(Decimal("1"), umbral.valor_max - umbral.valor_min)
    severidad = "critica" if distancia > rango * Decimal("0.20") else "advertencia"
    tipo = f"{medicion.tipo_medicion}_FUERA_RANGO"
    try:
        with transaction.atomic():
            alerta = Alerta.objects.create(
                colmena=asociacion.colmena,
                empresa=asociacion.empresa,
                medicion=medicion,
                sensor=medicion.sensor,
                tipo=tipo,
                severidad=severidad,
                valor_detectado=medicion.valor,
                umbral_min=umbral.valor_min,
                umbral_max=umbral.valor_max,
                descripcion=f"El valor de {medicion.tipo_medicion.lower()} ({medicion.valor} {medicion.unidad}) "
                f"está fuera del rango permitido ({umbral.valor_min}–{umbral.valor_max}).",
            )
            for u in Usuario.objects.filter(
                empresa=asociacion.empresa, estado=Usuario.ACTIVO
            ):
                Notificacion.objects.create(
                    usuario=u,
                    tipo="ALERTA",
                    alerta=alerta,
                    titulo=f"Alerta en {asociacion.colmena.nombre}",
                    mensaje=alerta.descripcion,
                )
    except IntegrityError:
        return None  # ya existe una alerta ACTIVA del mismo tipo para la colmena (o para la misma medición)
    registrar("ALERTA_GENERADA", "monitoreo", alerta, alerta.descripcion)
    return alerta


@transaction.atomic
def resolver_alerta(usuario, alerta):
    exigir(usuario, P.ALERTAS_RESOLVER, alerta.empresa_id)
    alerta = Alerta.objects.select_for_update().get(pk=alerta.pk)
    if alerta.estado == Alerta.RESUELTA:
        raise ReglaNegocioError("La alerta ya fue resuelta.")
    validar_puede_operar(alerta.empresa)
    alerta.estado, alerta.fecha_resolucion, alerta.usuario_resolucion = (
        Alerta.RESUELTA,
        timezone.now(),
        usuario,
    )
    alerta.save()
    alerta.notificaciones.update(leida=True)
    registrar("ALERTA_RESUELTA", "monitoreo", alerta, usuario=usuario)
    return alerta
