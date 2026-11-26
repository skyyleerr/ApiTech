"""Solicitudes, pagos simulados y suscripciones.

AVISO: el pago es SIMULADO (tarjeta ficticia, solo 4 dígitos). No existe pasarela bancaria real.
Los importes se calculan SIEMPRE en el servidor a partir del plan.
"""

import re
from datetime import timedelta

from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ValidationError
from django.db import IntegrityError, transaction
from django.utils import timezone

from apps.auditoria.models import registrar
from apps.comun.acceso import exigir
from apps.comun.errors import ReglaNegocioError
from apps.empresas.models import Empresa
from apps.usuarios import permisos as P
from apps.usuarios.models import Rol, Usuario

from .models import Pago, Plan, SolicitudSuscripcion, Suscripcion


def planes_activos():
    return Plan.objects.filter(estado="activo").order_by("precio")


def _activar(suscripcion):
    hoy = timezone.localdate()
    suscripcion.estado, suscripcion.fecha_inicio = Suscripcion.ACTIVA, hoy
    suscripcion.fecha_vencimiento = hoy + timedelta(days=suscripcion.plan.duracion_dias)
    suscripcion.save()


@transaction.atomic
def crear_solicitud(
    *,
    nombre_empresa,
    nit,
    correo_empresa,
    telefono="",
    direccion="",
    nombre_contacto,
    correo_contacto,
    telefono_contacto="",
    tarjeta_falsa,
    plan_id,
):
    from django.core.validators import validate_email

    try:
        validate_email(correo_empresa)
        validate_email(correo_contacto)
    except ValidationError:
        raise ReglaNegocioError("Ingrese correos válidos.")
    if not nombre_empresa.strip() or not nombre_contacto.strip():
        raise ReglaNegocioError("Nombre de empresa y contacto obligatorios.")
    datos = locals()
    for campo in (
        "nombre_empresa",
        "nit",
        "correo_empresa",
        "telefono",
        "direccion",
        "nombre_contacto",
        "correo_contacto",
        "telefono_contacto",
    ):
        limite = SolicitudSuscripcion._meta.get_field(campo).max_length
        if len(datos[campo]) > limite:
            raise ReglaNegocioError(
                f"El campo {campo} admite hasta {limite} caracteres."
            )
    nit = (nit or "").strip()
    if not nit:
        raise ReglaNegocioError("El NIT es obligatorio.")
    if Empresa.objects.filter(nit=nit).exists():
        raise ReglaNegocioError(f"Ya existe una empresa registrada con el NIT {nit}.")
    plan = (
        planes_activos().filter(pk=plan_id).first()
        if str(plan_id or "").isdigit()
        else None
    )
    if plan is None:
        raise ReglaNegocioError("Debe seleccionar un plan válido.")
    tarjeta = re.sub(r"\D", "", tarjeta_falsa or "")
    if not 12 <= len(tarjeta) <= 19:
        raise ReglaNegocioError("La tarjeta ficticia debe tener entre 12 y 19 dígitos.")
    try:
        with transaction.atomic():
            solicitud = SolicitudSuscripcion.objects.create(
                nombre_empresa=nombre_empresa.strip(),
                nit=nit,
                correo_empresa=correo_empresa.strip(),
                telefono=telefono.strip(),
                direccion=direccion.strip(),
                nombre_contacto=nombre_contacto.strip(),
                correo_contacto=correo_contacto.strip(),
                telefono_contacto=telefono_contacto.strip(),
                plan_solicitado=plan,
                tarjeta_falsa_ultimos4=tarjeta[-4:],
            )  # el número completo se descarta
    except IntegrityError:
        raise ReglaNegocioError("Ya existe una solicitud pendiente para ese NIT.")
    registrar("SOLICITUD_CREADA", "suscripciones", solicitud, f"NIT {nit}")
    return solicitud


@transaction.atomic
def rechazar_solicitud(revisor, solicitud_id, motivo=""):
    exigir(revisor, P.SOLICITUDES_REVISAR)
    s = SolicitudSuscripcion.objects.select_for_update().get(pk=solicitud_id)
    if s.estado != SolicitudSuscripcion.PENDIENTE:
        raise ReglaNegocioError("La solicitud ya fue procesada.")
    s.estado, s.usuario_revisor, s.fecha_revision = (
        SolicitudSuscripcion.RECHAZADA,
        revisor,
        timezone.now(),
    )
    s.motivo_rechazo = (motivo or "Solicitud rechazada por ApiTech").strip()
    s.save()
    registrar("SOLICITUD_RECHAZADA", "suscripciones", s, usuario=revisor)
    return s


@transaction.atomic
def aprobar_solicitud(
    revisor, solicitud_id, *, correo_admin, password_inicial, plan_id=None
):
    """Crea empresa + Admin_Cliente + suscripción activa con pago verificado, todo o nada."""
    exigir(revisor, P.SOLICITUDES_REVISAR)
    s = (
        SolicitudSuscripcion.objects.select_for_update()
        .select_related("plan_solicitado")
        .get(pk=solicitud_id)
    )
    if s.estado != SolicitudSuscripcion.PENDIENTE:
        raise ReglaNegocioError("La solicitud ya fue procesada.")
    correo_admin = (correo_admin or "").strip().lower()
    if Usuario.objects.filter(correo=correo_admin).exists():
        raise ReglaNegocioError("El correo del administrador ya está registrado.")
    if Empresa.objects.filter(nit=s.nit).exists():
        raise ReglaNegocioError(f"Ya existe una empresa registrada con el NIT {s.nit}.")
    try:
        from django.core.validators import validate_email

        validate_email(correo_admin)
        validate_password(password_inicial)
    except ValidationError as e:
        raise ReglaNegocioError(" ".join(e.messages))
    plan = Plan.objects.get(pk=plan_id) if plan_id else s.plan_solicitado
    empresa = Empresa.objects.create(
        nit=s.nit,
        nombre_empresa=s.nombre_empresa,
        correo=s.correo_empresa,
        telefono=s.telefono,
        direccion=s.direccion,
    )
    Usuario.objects.create_user(
        correo_admin,
        password_inicial,
        Rol.ADMIN_CLIENTE,
        empresa=empresa,
        nombre=s.nombre_contacto,
    )
    suscripcion = Suscripcion.objects.create(empresa=empresa, plan=plan)
    pago = Pago.objects.create(
        suscripcion=suscripcion,
        valor=plan.precio,
        fecha_pago=timezone.localdate(),
        metodo_pago="SIMULADO",
        referencia=f"ALTA-{s.pk}",
    )
    _verificar(pago, revisor)
    s.estado, s.usuario_revisor, s.fecha_revision, s.plan_solicitado = (
        SolicitudSuscripcion.APROBADA,
        revisor,
        timezone.now(),
        plan,
    )
    s.save()
    registrar(
        "SOLICITUD_APROBADA", "suscripciones", s, f"Empresa {empresa.nit}", revisor
    )
    return empresa


@transaction.atomic
def registrar_pago(empresa, plan_id, metodo="SIMULADO", referencia=""):
    """Crea suscripción PENDIENTE y pago PENDIENTE. El valor sale del plan, nunca del navegador."""
    plan = planes_activos().filter(pk=plan_id).first()
    if plan is None:
        raise ReglaNegocioError("Plan no disponible.")
    suscripcion = Suscripcion.objects.create(empresa=empresa, plan=plan)
    return Pago.objects.create(
        suscripcion=suscripcion,
        valor=plan.precio,
        fecha_pago=timezone.localdate(),
        metodo_pago=metodo,
        referencia=referencia,
    )


def _verificar(pago, usuario):
    Empresa.objects.select_for_update().get(pk=pago.suscripcion.empresa_id)
    otra = (
        Suscripcion.objects.filter(
            empresa=pago.suscripcion.empresa, estado=Suscripcion.ACTIVA
        )
        .exclude(pk=pago.suscripcion_id)
        .first()
    )
    dias_restantes = 0
    if otra:
        if (
            otra.plan_id != pago.suscripcion.plan_id
            and otra.fecha_vencimiento
            and otra.fecha_vencimiento >= timezone.localdate()
        ):
            raise ReglaNegocioError(
                "El cambio de plan se puede verificar cuando finalice el plan vigente."
            )
        if otra.fecha_vencimiento:
            dias_restantes = max(
                0, (otra.fecha_vencimiento - timezone.localdate()).days
            )
        otra.estado = (
            Suscripcion.VENCIDA if not dias_restantes else Suscripcion.CANCELADA
        )
        otra.fecha_cancelacion = timezone.localdate()
        otra.save()
    pago.estado, pago.fecha_verificacion, pago.usuario_verifico = (
        Pago.VERIFICADO,
        timezone.now(),
        usuario,
    )
    pago.save()
    try:
        with transaction.atomic():
            _activar(pago.suscripcion)
            if dias_restantes:
                pago.suscripcion.fecha_vencimiento += timedelta(days=dias_restantes)
                pago.suscripcion.save()
    except IntegrityError:  # respaldo del índice único: nunca dos suscripciones activas
        raise ReglaNegocioError("La empresa ya tiene una suscripción activa.")


@transaction.atomic
def verificar_pago(usuario, pago_id):
    """Idempotente por estado: confirmar dos veces el mismo pago falla y no activa nada otra vez."""
    exigir(usuario, P.PAGOS_VERIFICAR)
    pago = (
        Pago.objects.select_for_update()
        .select_related("suscripcion__plan", "suscripcion__empresa")
        .get(pk=pago_id)
    )
    if pago.estado != Pago.PENDIENTE:
        raise ReglaNegocioError(f"El pago ya fue procesado ({pago.estado.lower()}).")
    _verificar(pago, usuario)
    registrar("PAGO_VERIFICADO", "suscripciones", pago, usuario=usuario)
    return pago


@transaction.atomic
def rechazar_pago(usuario, pago_id, observaciones=""):
    exigir(usuario, P.PAGOS_VERIFICAR)
    pago = (
        Pago.objects.select_for_update().select_related("suscripcion").get(pk=pago_id)
    )
    if pago.estado != Pago.PENDIENTE:
        raise ReglaNegocioError(f"El pago ya fue procesado ({pago.estado.lower()}).")
    pago.estado, pago.usuario_verifico, pago.fecha_verificacion = (
        Pago.RECHAZADO,
        usuario,
        timezone.now(),
    )
    pago.observaciones = observaciones
    pago.save()
    if pago.suscripcion.estado == Suscripcion.PENDIENTE:
        pago.suscripcion.estado = Suscripcion.CANCELADA
        pago.suscripcion.fecha_cancelacion = timezone.localdate()
        pago.suscripcion.save()
    registrar("PAGO_RECHAZADO", "suscripciones", pago, usuario=usuario)
    return pago


@transaction.atomic
def vencer_suscripciones():
    """Cierra vigencias vencidas y encola la detención de sensores externos."""
    from apps.empresas.services import detener_monitoreos

    ids = list(
        Suscripcion.objects.filter(
            estado=Suscripcion.ACTIVA, fecha_vencimiento__lt=timezone.localdate()
        ).values_list("id", flat=True)
    )
    n = 0
    for pk in ids:
        sub = Suscripcion.objects.get(pk=pk)
        Empresa.objects.select_for_update().get(pk=sub.empresa_id)
        sub = Suscripcion.objects.select_for_update().get(pk=pk)
        if sub.estado != Suscripcion.ACTIVA:
            continue
        sub.estado = Suscripcion.VENCIDA
        sub.save()
        detener_monitoreos(sub.empresa)
        n += 1
    if n:
        registrar(
            "SUSCRIPCIONES_VENCIDAS", "suscripciones", descripcion=f"{n} suscripciones"
        )
    return n
