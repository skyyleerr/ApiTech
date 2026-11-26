from django.utils import timezone

from apps.comun.errors import ReglaNegocioError


def suscripcion_vigente(empresa):
    """Suscripción ACTIVA cuyo vencimiento no ha pasado (el día de vencimiento aún es vigente)."""
    from apps.suscripciones.models import Suscripcion

    hoy = timezone.localdate()
    return (
        Suscripcion.objects.select_related("plan")
        .filter(empresa=empresa, estado=Suscripcion.ACTIVA, fecha_vencimiento__gte=hoy)
        .first()
    )


def validar_puede_operar(empresa):
    """Empresa pausada o sin suscripción vigente: solo consulta (sin escrituras ni procesos automáticos)."""
    if empresa.esta_pausada:
        raise ReglaNegocioError(
            "La empresa está pausada por ApiTech. No se permiten cambios hasta reactivarla."
        )
    if suscripcion_vigente(empresa) is None:
        raise ReglaNegocioError(
            "La suscripción de la empresa no está vigente. No se permiten operaciones hasta renovarla."
        )


def limites(empresa):
    """(max_colmenas, max_sensores) del plan vigente; (0, 0) si no hay suscripción vigente (como MK5)."""
    s = suscripcion_vigente(empresa)
    return (s.plan.max_colmenas, s.plan.max_sensores) if s else (0, 0)


def detener_monitoreos(empresa):
    """Cierra intervalos y deja órdenes durables al pausar/vencer una empresa.

    El llamador debe estar en una transacción. La reactivación es explícita.
    """
    from apps.monitoreo.models import Monitoreo
    from apps.integraciones.services import encolar

    ahora = timezone.now()
    activos = (
        Monitoreo.objects.select_for_update()
        .filter(asociacion__empresa=empresa, estado="activo")
        .select_related("asociacion__sensor")
    )
    for monitoreo in activos:
        monitoreo.estado = "inactivo"
        monitoreo.fecha_fin = ahora
        monitoreo.save()
        encolar(monitoreo.asociacion.sensor, "DEACTIVATE", monitoreo.asociacion)
