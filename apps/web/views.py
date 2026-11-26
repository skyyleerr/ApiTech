from django.contrib import messages
from django.contrib.auth import authenticate, login, logout
from django.contrib.auth.decorators import login_required
from django.core.exceptions import PermissionDenied
from django.db.models import Sum
from django.db import transaction
from apps.auditoria.models import registrar
from apps.empresas.services import detener_monitoreos
from django.http import HttpResponseNotAllowed
from django.shortcuts import get_object_or_404, redirect, render
from django.views.decorators.cache import never_cache
from django.views.decorators.http import require_POST

from apps.apiario import services as apiario
from apps.apiario.models import Colmena
from apps.comun.errors import ReglaNegocioError
from apps.empresas.models import Empresa
from apps.empresas.services import limites, suscripcion_vigente
from apps.monitoreo.models import Alerta
from apps.suscripciones import services as subs
from apps.suscripciones.models import Pago, SolicitudSuscripcion, Suscripcion
from apps.usuarios import permisos as P
from apps.usuarios.decoradores import objeto_de_mi_empresa, requiere_permiso
from apps.usuarios.models import Rol

SESION_EMPRESA = "login_empresa_id"


def _destino(usuario):
    return "admin_dashboard" if usuario.es_interno else "app_dashboard"


# ── Público ───────────────────────────────────────────────────
def inicio(request):
    if request.user.is_authenticated:
        return redirect(_destino(request.user))
    return render(request, "publico/landing.html")


def planes(request):
    return render(request, "publico/planes.html", {"planes": subs.planes_activos()})


def solicitud(request):
    ctx = {
        "planes": subs.planes_activos(),
        "v": request.POST or {"plan": request.GET.get("plan", "")},
    }
    if request.method == "POST":
        p = request.POST
        try:
            subs.crear_solicitud(
                nombre_empresa=p.get("nombre_empresa", ""),
                nit=p.get("nit", ""),
                correo_empresa=p.get("correo_empresa", ""),
                telefono=p.get("telefono", ""),
                direccion=p.get("direccion", ""),
                nombre_contacto=p.get("nombre_contacto", ""),
                correo_contacto=p.get("correo_contacto", ""),
                telefono_contacto=p.get("telefono_contacto", ""),
                tarjeta_falsa=p.get("tarjeta_falsa", ""),
                plan_id=p.get("plan"),
            )
            return redirect("/solicitud-suscripcion/?enviada=1")
        except ReglaNegocioError as e:
            ctx["error"] = str(e)
    ctx["enviada"] = request.GET.get("enviada") == "1" and request.method == "GET"
    return render(request, "auth/solicitud.html", ctx)


# ── Acceso ────────────────────────────────────────────────────
@never_cache
def login_empresa(request):
    request.session.pop(SESION_EMPRESA, None)
    if request.method == "POST":
        empresa = Empresa.objects.filter(
            nit=request.POST.get("nit", "").strip()
        ).first()
        if empresa:
            request.session[SESION_EMPRESA] = empresa.pk
            return redirect("login")
        return render(
            request,
            "auth/empresa.html",
            {"error": "No encontramos una empresa con ese NIT."},
        )
    return render(request, "auth/empresa.html")


@never_cache
def login_vista(request):
    empresa_id = request.session.get(SESION_EMPRESA)
    empresa = Empresa.objects.filter(pk=empresa_id).first() if empresa_id else None
    ctx = {"empresa": empresa, "logout": request.GET.get("logout")}
    if request.method == "POST":
        usuario = authenticate(
            request,
            correo=request.POST.get("correo", ""),
            password=request.POST.get("password", ""),
            empresa_id=empresa_id,
        )
        if usuario is None:
            ctx["error"] = (
                "Correo o contraseña incorrectos, cuenta inactiva, o empresa no coincide."
            )
            return render(request, "auth/login.html", ctx)
        login(request, usuario)
        request.session.pop(
            SESION_EMPRESA, None
        )  # el tenant real sale SIEMPRE del usuario
        return redirect(_destino(usuario))
    return render(request, "auth/login.html", ctx)


def logout_vista(request):
    if (
        request.method != "POST"
    ):  # evita cierre de sesión por enlaces/imagenes de terceros (CSRF)
        return HttpResponseNotAllowed(["POST"])
    logout(request)
    return redirect("/login/?logout=1")


# ── Equipo ApiTech ────────────────────────────────────────────
@requiere_permiso(P.EMPRESAS_VER)
def admin_dashboard(request):
    return render(
        request,
        "admin/dashboard.html",
        {
            "empresas": Empresa.objects.count(),
            "pausadas": Empresa.objects.exclude(estado=Empresa.ACTIVA).count(),
            "solicitudes_pendientes": SolicitudSuscripcion.objects.filter(
                estado=SolicitudSuscripcion.PENDIENTE
            ).count(),
            "pagos_pendientes": Pago.objects.filter(estado=Pago.PENDIENTE).count(),
            "total_verificado": Pago.objects.filter(estado=Pago.VERIFICADO).aggregate(
                t=Sum("valor")
            )["t"]
            or 0,
        },
    )


@requiere_permiso(P.EMPRESAS_VER)
def admin_empresas(request):
    q = request.GET.get("q", "").strip()
    qs = Empresa.objects.order_by("nombre_empresa")
    if q:
        qs = qs.filter(nombre_empresa__icontains=q)
    return render(request, "admin/empresas.html", {"empresas": qs, "q": q})


@require_POST
@requiere_permiso(P.EMPRESAS_GESTIONAR)
def admin_empresa_estado(request, pk):
    empresa = get_object_or_404(Empresa, pk=pk)
    nuevo = request.POST.get("estado")
    if nuevo not in dict(Empresa.ESTADOS):
        raise PermissionDenied
    with transaction.atomic():
        empresa = Empresa.objects.select_for_update().get(pk=empresa.pk)
        empresa.estado = nuevo
        empresa.save()
        if nuevo == Empresa.PAUSADA:
            detener_monitoreos(empresa)
        registrar("EMPRESA_ESTADO", "empresas", empresa, nuevo, request.user)
    messages.success(request, f"Empresa {empresa.nombre_empresa}: {nuevo}.")
    return redirect("admin_empresas")


@requiere_permiso(P.SOLICITUDES_VER)
def admin_solicitudes(request):
    return render(
        request,
        "admin/solicitudes.html",
        {
            "solicitudes": SolicitudSuscripcion.objects.select_related(
                "plan_solicitado"
            ).order_by("-pk")
        },
    )


@requiere_permiso(P.SOLICITUDES_VER)
def admin_solicitud_detalle(request, pk):
    s = get_object_or_404(
        SolicitudSuscripcion.objects.select_related("plan_solicitado"), pk=pk
    )
    if request.method == "POST":
        accion = request.POST.get("accion")
        try:
            if accion == "aprobar":
                subs.aprobar_solicitud(
                    request.user,
                    pk,
                    correo_admin=request.POST.get("correo_admin", s.correo_contacto),
                    password_inicial=request.POST.get("password_inicial", ""),
                )
                messages.success(
                    request, "Solicitud aprobada: empresa y administrador creados."
                )
            elif accion == "rechazar":
                subs.rechazar_solicitud(
                    request.user, pk, request.POST.get("motivo", "")
                )
                messages.success(request, "Solicitud rechazada.")
            else:
                raise ReglaNegocioError("Acción no válida.")
            return redirect("admin_solicitudes")
        except ReglaNegocioError as e:
            messages.error(request, str(e))
    return render(
        request,
        "admin/solicitud_detalle.html",
        {"s": s, "puede_revisar": P.tiene_permiso(request.user, P.SOLICITUDES_REVISAR)},
    )


@requiere_permiso(P.PAGOS_VER_TODOS)
def admin_pagos(request):
    if request.method == "POST":
        try:
            pago_id, accion = int(request.POST.get("pago", 0)), request.POST.get(
                "accion"
            )
            if accion == "verificar":
                subs.verificar_pago(request.user, pago_id)
            elif accion == "rechazar":
                subs.rechazar_pago(
                    request.user, pago_id, request.POST.get("observaciones", "")
                )
            else:
                raise ReglaNegocioError("Acción no válida.")
            messages.success(request, "Pago actualizado.")
        except (ReglaNegocioError, Pago.DoesNotExist, ValueError) as e:
            messages.error(
                request,
                str(e) if isinstance(e, ReglaNegocioError) else "Pago no encontrado.",
            )
        return redirect("admin_pagos")
    return render(
        request,
        "admin/pagos.html",
        {
            "pagos": Pago.objects.select_related(
                "suscripcion__empresa", "suscripcion__plan"
            ).order_by("-pk"),
            "puede_verificar": P.tiene_permiso(request.user, P.PAGOS_VERIFICAR),
        },
    )
