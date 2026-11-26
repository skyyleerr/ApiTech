import csv, io, json
from datetime import timedelta
from decimal import Decimal
from django import forms
from django.contrib import messages
from django.contrib.auth import update_session_auth_hash
from django.contrib.auth.decorators import login_required
from django.contrib.auth.forms import PasswordChangeForm
from django.core.exceptions import PermissionDenied, ValidationError
from django.core.paginator import Paginator
from django.db import transaction, IntegrityError
from django.db.models import Sum, Q, Count
from django.http import JsonResponse, HttpResponse
from django.shortcuts import render, redirect, get_object_or_404
from django.urls import reverse
from django.utils import timezone
from django.views.decorators.http import require_POST
from apps.apiario import services as apiario
from apps.apiario.models import (
    Colmena,
    Sensor,
    SensorColmena,
    ProduccionMiel,
    TipoMedicion,
    UNIDADES,
)
from apps.monitoreo.models import Medicion, Monitoreo, Alerta, Notificacion, RangoUmbral
from apps.monitoreo.services import resolver_alerta
from apps.usuarios.models import Rol, Usuario
from apps.usuarios import permisos as P
from apps.usuarios.decoradores import requiere_permiso
from apps.comun.acceso import exigir
from apps.comun.errors import ReglaNegocioError
from apps.empresas.services import validar_puede_operar, suscripcion_vigente, limites
from apps.empresas.models import Empresa
from apps.suscripciones.models import Plan, Pago, Suscripcion
from apps.suscripciones.services import registrar_pago
from apps.auditoria.models import Auditoria, registrar
from apps.integraciones.models import ConexionAPI
from apps.integraciones.services import encolar, sincronizar, configuracion, APIError
from apps.importaciones.models import LoteImportacion
from apps.importaciones import services as imports
from .forms import (
    ColmenaForm,
    SensorForm,
    ProduccionForm,
    UsuarioForm,
    PerfilForm,
    UmbralForm,
    EmpresaForm,
    PlanForm,
)
from .consultas import lecturas, resumen, filtrar, estadisticas


def pagina(request, qs):
    return Paginator(qs, 25).get_page(request.GET.get("page"))


def form_page(request, titulo, form, volver):
    return render(
        request, "app/form.html", {"titulo": titulo, "form": form, "volver": volver}
    )


def error_form(form, e):
    form.add_error(
        None,
        (
            str(e)
            if not isinstance(e, IntegrityError)
            else "Existe un registro con esos datos. Revise los campos únicos."
        ),
    )


def propia_colmena(request, pk):
    return get_object_or_404(Colmena, pk=pk, empresa=request.user.empresa)


def lecturas_filtradas(request, qs):
    try:
        return filtrar(qs, request.GET)
    except ReglaNegocioError as e:
        messages.error(request, str(e))
        return qs.none()


@requiere_permiso(P.COLMENAS_VER)
def dashboard(request):
    e = request.user.empresa
    cols = e.colmenas.filter(archivada=False).order_by("numero")
    maxc, maxs = limites(e)
    return render(
        request,
        "app/dashboard.html",
        {
            "empresa": e,
            "colmenas": cols[:10],
            "resumen": resumen(e),
            "total_colmenas": cols.count(),
            "total_sensores": e.sensores.count(),
            "max_colmenas": maxc,
            "max_sensores": maxs,
            "alertas_activas": e.alertas.filter(estado=Alerta.ACTIVA).count(),
            "suscripcion": suscripcion_vigente(e),
            "chart_url": reverse("telemetria"),
            "pausada": e.esta_pausada,
        },
    )


@requiere_permiso(P.COLMENAS_VER)
def colmenas(request):
    qs = request.user.empresa.colmenas.filter(
        archivada=request.GET.get("archivo") == "1"
    ).order_by("numero", "pk")
    if request.GET.get("q"):
        qs = qs.filter(
            Q(nombre__icontains=request.GET["q"])
            | Q(ubicacion__icontains=request.GET["q"])
        )
    if request.GET.get("estado"):
        qs = qs.filter(estado=request.GET["estado"])
    return render(
        request,
        "app/colmenas_lista.html",
        {"page": pagina(request, qs), "estados": Colmena.ESTADOS},
    )


@requiere_permiso(P.COLMENAS_GESTIONAR)
def colmena_form(request, pk=None):
    obj = propia_colmena(request, pk) if pk else None
    if obj and obj.archivada:
        raise PermissionDenied
    form = ColmenaForm(request.POST or None, instance=obj)
    if request.method == "POST" and form.is_valid():
        try:
            d = form.cleaned_data
            if obj:
                apiario.actualizar_colmena(request.user, obj, **d)
            else:
                obj = apiario.crear_colmena(request.user, **d)
            messages.success(request, "Colmena guardada.")
            return redirect("colmena_detalle", pk=obj.pk)
        except (ReglaNegocioError, IntegrityError) as e:
            error_form(form, e)
    return form_page(
        request, "Editar colmena" if pk else "Nueva colmena", form, reverse("colmenas")
    )


@requiere_permiso(P.COLMENAS_VER)
def detalle(request, pk):
    c = propia_colmena(request, pk)
    qs = lecturas_filtradas(
        request, lecturas(request.user.empresa).filter(asociacion__colmena=c)
    )
    return render(
        request,
        "app/colmena_detalle.html",
        {
            "colmena": c,
            "page": pagina(request, qs.order_by("-fecha", "-pk")),
            "asociaciones": c.asociaciones.select_related("sensor").order_by(
                "-activo", "-pk"
            ),
            "resumen": resumen(c.empresa, c),
            "estadisticas": estadisticas(qs),
            "chart_url": reverse("telemetria_colmena", args=[c.pk]),
            "producciones": c.producciones.order_by("-fecha")[:10],
            "alertas": c.alertas.order_by("-fecha")[:10],
        },
    )


@require_POST
@requiere_permiso(P.COLMENAS_GESTIONAR)
def colmena_archivar(request, pk):
    c = propia_colmena(request, pk)
    try:
        apiario.archivar_colmena(request.user, c)
        messages.success(
            request,
            "Colmena eliminada del listado activo. Su historial se conserva y su número queda disponible.",
        )
    except ReglaNegocioError as e:
        messages.error(request, str(e))
    return redirect("colmenas")


@requiere_permiso(P.MEDICIONES_VER)
def telemetria(request, pk=None):
    c = propia_colmena(request, pk) if pk else None
    qs = lecturas(request.user.empresa)
    if c:
        qs = qs.filter(asociacion__colmena=c)
    else:
        qs = qs.filter(asociacion__colmena__archivada=False)
    qs = lecturas_filtradas(request, qs)
    series = []
    # Series por sensor; jamás promediar registros de empresas diferentes ni rellenar con ceros.
    for sid in list(qs.values_list("sensor_id", flat=True).distinct())[:60]:
        rows = list(qs.filter(sensor_id=sid).order_by("-fecha", "-pk")[:120])[::-1]
        if rows:
            series.append(
                {
                    "sensor": rows[0].sensor.codigo,
                    "origen": rows[0].origen,
                    "tipo": rows[0].tipo_medicion,
                    "unidad": rows[0].unidad,
                    "puntos": [
                        {"fecha": m.fecha.isoformat(), "valor": float(m.valor)}
                        for m in rows
                    ],
                }
            )
    return JsonResponse(
        {
            "series": series,
            "resumen": resumen(request.user.empresa, c),
            "generado": timezone.now().isoformat(),
        }
    )


@requiere_permiso(P.SENSORES_VER)
def sensores(request):
    qs = Sensor.objects.filter(empresa=request.user.empresa).order_by("codigo")
    if request.GET.get("q"):
        qs = qs.filter(codigo__icontains=request.GET["q"])
    if request.GET.get("tipo"):
        qs = qs.filter(tipo=request.GET["tipo"])
    return render(
        request,
        "app/sensores.html",
        {
            "page": pagina(request, qs),
            "colmenas": request.user.empresa.colmenas.filter(archivada=False),
            "asociaciones": SensorColmena.objects.filter(empresa=request.user.empresa)
            .select_related("sensor", "colmena")
            .order_by("-activo", "-pk")[:200],
        },
    )


@requiere_permiso(P.SENSORES_GESTIONAR)
def sensor_form(request, pk=None):
    obj = get_object_or_404(Sensor, pk=pk, empresa=request.user.empresa) if pk else None
    form = SensorForm(request.POST or None, instance=obj)
    if obj:
        form.fields["tipo"].disabled = True
        form.fields["codigo"].disabled = True
    if request.method == "POST" and form.is_valid():
        try:
            validar_puede_operar(request.user.empresa)
            with transaction.atomic():
                if obj:
                    obj = form.save()
                    registrar("SENSOR_EDITADO", "sensores", obj, usuario=request.user)
                else:
                    obj = apiario.registrar_sensor(request.user, **form.cleaned_data)
                    encolar(obj, "REGISTER")
            messages.success(request, "Sensor guardado.")
            return redirect("sensores")
        except (ReglaNegocioError, IntegrityError) as e:
            error_form(form, e)
    return form_page(
        request, "Editar sensor" if pk else "Nuevo sensor", form, reverse("sensores")
    )


@require_POST
@requiere_permiso(P.SENSORES_GESTIONAR)
def sensor_accion(request, pk):
    s = get_object_or_404(Sensor, pk=pk, empresa=request.user.empresa)
    try:
        with transaction.atomic():
            accion = request.POST.get("accion")
            if accion == "asociar":
                c = propia_colmena(request, request.POST.get("colmena"))
                apiario.asociar_sensor(request.user, s, c)
            elif accion in ("activar", "detener", "desasociar"):
                a = get_object_or_404(
                    SensorColmena, sensor=s, empresa=request.user.empresa, activo=True
                )
                if accion == "activar":
                    apiario.activar_monitoreo(request.user, a)
                    encolar(s, "ACTIVATE", a)
                else:
                    for m in a.monitoreos.filter(estado="activo"):
                        apiario.desactivar_monitoreo(request.user, m)
                    encolar(s, "DEACTIVATE", a)
                    if accion == "desasociar":
                        apiario.desasociar_sensor(request.user, a)
            elif accion == "estado":
                nuevo = "inactivo" if s.estado == "activo" else "activo"
                if nuevo == "inactivo":
                    for a in s.asociaciones.filter(activo=True):
                        for m in a.monitoreos.filter(estado="activo"):
                            apiario.desactivar_monitoreo(request.user, m)
                        encolar(s, "DEACTIVATE", a)
                apiario.cambiar_estado_sensor(request.user, s, nuevo)
            else:
                raise ReglaNegocioError("Acción desconocida.")
        messages.success(
            request,
            "Cambio registrado. Las órdenes externas se enviarán al sincronizar la API.",
        )
    except (ReglaNegocioError, ValueError) as e:
        messages.error(request, str(e))
    return redirect("sensores")


@requiere_permiso(P.PRODUCCION_VER)
def produccion(request):
    qs = (
        ProduccionMiel.objects.filter(empresa=request.user.empresa)
        .select_related("colmena")
        .order_by("-fecha", "-pk")
    )
    if request.GET.get("colmena", "").isdigit():
        qs = qs.filter(colmena_id=request.GET["colmena"])
    for k, op in [("desde", "gte"), ("hasta", "lte")]:
        if request.GET.get(k):
            try:
                fecha = forms.DateField().clean(request.GET[k])
                qs = qs.filter(**{"fecha__" + op: fecha})
            except ValidationError:
                messages.error(request, "Fecha inválida.")
                qs = qs.none()
    total = qs.aggregate(t=Sum("cantidad"))["t"] or 0
    n = qs.values("colmena_id").distinct().count()
    return render(
        request,
        "app/produccion.html",
        {
            "page": pagina(request, qs),
            "total": total,
            "promedio": total / n if n else 0,
            "ultima": qs.first(),
            "colmenas": request.user.empresa.colmenas.all(),
        },
    )


@requiere_permiso(P.PRODUCCION_REGISTRAR)
def produccion_nueva(request):
    form = ProduccionForm(request.POST or None, empresa=request.user.empresa)
    if request.method == "POST" and form.is_valid():
        try:
            apiario.registrar_produccion(request.user, **form.cleaned_data)
            messages.success(request, "Producción registrada.")
            return redirect("produccion")
        except ReglaNegocioError as e:
            error_form(form, e)
    return form_page(request, "Registrar cosecha de miel", form, reverse("produccion"))


@requiere_permiso(P.ALERTAS_VER)
def alertas(request):
    qs = request.user.empresa.alertas.select_related("colmena", "sensor").order_by(
        "-fecha"
    )
    if request.GET.get("estado"):
        qs = qs.filter(estado=request.GET["estado"])
    if request.GET.get("colmena", "").isdigit():
        qs = qs.filter(colmena_id=request.GET["colmena"])
    return render(request, "app/alertas.html", {"page": pagina(request, qs)})


@require_POST
@requiere_permiso(P.ALERTAS_RESOLVER)
def alerta_resolver(request, pk):
    a = get_object_or_404(Alerta, pk=pk, empresa=request.user.empresa)
    try:
        resolver_alerta(request.user, a)
        messages.success(request, "Alerta resuelta.")
    except ReglaNegocioError as e:
        messages.error(request, str(e))
    return redirect("alertas")


@login_required
def notificaciones(request):
    if request.method == "POST":
        qs = request.user.notificaciones.all()
        if request.POST.get("id"):
            if not request.POST["id"].isdigit():
                return HttpResponse("Identificador inválido", status=400)
            qs = qs.filter(pk=request.POST["id"])
        qs.update(leida=True)
        return redirect("notificaciones")
    return render(
        request,
        "app/notificaciones.html",
        {"page": pagina(request, request.user.notificaciones.order_by("-fecha"))},
    )


@login_required
def perfil(request):
    form = PerfilForm(
        (
            request.POST
            if request.method == "POST" and request.POST.get("accion") == "perfil"
            else None
        ),
        instance=request.user,
    )
    password = PasswordChangeForm(
        request.user,
        (
            request.POST
            if request.method == "POST" and request.POST.get("accion") == "clave"
            else None
        ),
    )
    if request.method == "POST":
        if request.POST.get("accion") == "perfil" and form.is_valid():
            form.save()
            registrar(
                "PERFIL_ACTUALIZADO", "usuarios", request.user, usuario=request.user
            )
            messages.success(request, "Perfil actualizado.")
            return redirect("perfil")
        if request.POST.get("accion") == "clave" and password.is_valid():
            u = password.save()
            update_session_auth_hash(request, u)
            messages.success(request, "Contraseña actualizada.")
            return redirect("perfil")
    return render(request, "app/perfil.html", {"form": form, "password_form": password})


@login_required
def usuarios(request):
    permiso = (
        P.USUARIOS_INTERNOS_GESTIONAR
        if request.user.es_interno
        else P.USUARIOS_EMPRESA_GESTIONAR
    )
    exigir(request.user, permiso)
    qs = (
        Usuario.objects.filter(empresa=request.user.empresa)
        .select_related("rol")
        .order_by("nombre")
    )
    return render(request, "app/usuarios.html", {"page": pagina(request, qs)})


@login_required
def usuario_form(request, pk=None):
    exigir(
        request.user,
        (
            P.USUARIOS_INTERNOS_GESTIONAR
            if request.user.es_interno
            else P.USUARIOS_EMPRESA_GESTIONAR
        ),
    )
    obj = (
        get_object_or_404(Usuario, pk=pk, empresa=request.user.empresa) if pk else None
    )
    form = UsuarioForm(
        request.POST or None, instance=obj, internos=request.user.es_interno
    )
    if obj:
        form.instance.empresa = request.user.empresa
    if request.method == "POST" and form.is_valid():
        try:
            if not request.user.es_interno:
                validar_puede_operar(request.user.empresa)
            if (
                obj
                and obj.pk == request.user.pk
                and (
                    form.cleaned_data["estado"] != "activo"
                    or form.cleaned_data["rol"] != request.user.rol
                )
            ):
                raise ReglaNegocioError(
                    "No puede desactivar su propia cuenta ni cambiar su propio rol."
                )
            with transaction.atomic():
                u = form.save(commit=False)
                u.empresa = request.user.empresa
                if not obj:
                    u.set_password(form.cleaned_data["clave"])
                u.save()
                registrar("USUARIO_GUARDADO", "usuarios", u, usuario=request.user)
            messages.success(request, "Usuario guardado.")
            return redirect("usuarios")
        except (ReglaNegocioError, IntegrityError, ValidationError) as e:
            error_form(form, e)
    return form_page(
        request, "Editar usuario" if pk else "Nuevo usuario", form, reverse("usuarios")
    )


@requiere_permiso(P.UMBRALES_GESTIONAR)
def umbrales(request):
    form = UmbralForm(request.POST or None)
    if request.method == "POST" and form.is_valid():
        try:
            validar_puede_operar(request.user.empresa)
            d = form.cleaned_data
            obj, _ = RangoUmbral.objects.update_or_create(
                empresa=request.user.empresa,
                tipo_medicion=d["tipo_medicion"],
                defaults={
                    **d,
                    "unidad": UNIDADES[d["tipo_medicion"]],
                    "es_demostracion": False,
                },
            )
            registrar("UMBRAL_GUARDADO", "monitoreo", obj, usuario=request.user)
            messages.success(request, "Umbral guardado.")
            return redirect("umbrales")
        except (ReglaNegocioError, IntegrityError) as e:
            error_form(form, e)
    return render(
        request,
        "app/umbrales.html",
        {
            "form": form,
            "umbrales": RangoUmbral.objects.filter(
                Q(empresa=request.user.empresa) | Q(empresa=None)
            ).order_by("tipo_medicion"),
        },
    )


@requiere_permiso(P.SENSORES_GESTIONAR)
def conexion(request):
    c, _ = ConexionAPI.objects.get_or_create(empresa=request.user.empresa)
    if request.method == "POST":
        try:
            validar_puede_operar(request.user.empresa)
            if request.POST.get("accion") == "sincronizar":
                n = sincronizar(request.user.empresa)
                messages.success(request, f"{n} lecturas nuevas recibidas.")
            else:
                if not c.habilitada:
                    configuracion(request.user.empresa_id)
                c.habilitada = not c.habilitada
                c.save()
                messages.success(
                    request,
                    (
                        "Conexión habilitada."
                        if c.habilitada
                        else "Sincronización local deshabilitada. Los sensores externos deben detenerse desde su servicio si siguen activos."
                    ),
                )
            return redirect("conexion")
        except ReglaNegocioError as e:
            messages.error(request, str(e))
    try:
        configuracion(request.user.empresa_id)
        configured = True
    except APIError:
        configured = False
    return render(
        request,
        "app/conexion.html",
        {
            "conexion": c,
            "configurada": configured,
            "pendientes": request.user.empresa.sensores.filter(
                sync_estado__in=["PENDIENTE", "ERROR"]
            ).count(),
        },
    )


@requiere_permiso(P.PAGOS_VER_PROPIOS)
def pagos(request):
    if request.method == "POST":
        if request.user.rol.nombre != Rol.ADMIN_CLIENTE:
            raise PermissionDenied
        try:
            # Bloqueo impide dos solicitudes pendientes del mismo plan por doble clic.
            with transaction.atomic():
                Empresa.objects.select_for_update().get(pk=request.user.empresa_id)
                if not request.POST.get("plan", "").isdigit():
                    raise ReglaNegocioError("Seleccione un plan válido.")
                plan = get_object_or_404(Plan, pk=request.POST["plan"], estado="activo")
                if Pago.objects.filter(
                    suscripcion__empresa=request.user.empresa,
                    suscripcion__plan=plan,
                    estado=Pago.PENDIENTE,
                ).exists():
                    raise ReglaNegocioError("Ya tiene un pago pendiente para ese plan.")
                pago = registrar_pago(
                    request.user.empresa,
                    plan.pk,
                    referencia=request.POST.get("referencia", "")[:100],
                )
                registrar("PAGO_SOLICITADO", "pagos", pago, usuario=request.user)
            messages.success(
                request, "Pago simulado registrado y pendiente de revisión."
            )
            return redirect("pagos")
        except ReglaNegocioError as e:
            messages.error(request, str(e))
    return render(
        request,
        "app/pagos.html",
        {
            "planes": Plan.objects.filter(estado="activo"),
            "suscripcion": suscripcion_vigente(request.user.empresa),
            "suscripciones": request.user.empresa.suscripciones.select_related(
                "plan"
            ).order_by("-pk"),
            "pagos": Pago.objects.filter(suscripcion__empresa=request.user.empresa)
            .select_related("suscripcion__plan")
            .order_by("-pk"),
        },
    )


@requiere_permiso(P.EMPRESAS_GESTIONAR)
def empresa_form(request, pk=None):
    obj = get_object_or_404(Empresa, pk=pk) if pk else None
    form = EmpresaForm(request.POST or None, instance=obj)
    if request.method == "POST" and form.is_valid():
        from apps.empresas.services import detener_monitoreos

        with transaction.atomic():
            if obj:
                Empresa.objects.select_for_update().get(pk=obj.pk)
            obj = form.save()
            if obj.esta_pausada:
                detener_monitoreos(obj)
            registrar("EMPRESA_GUARDADA", "empresas", obj, usuario=request.user)
        messages.success(request, "Empresa guardada.")
        return redirect("admin_empresas")
    return form_page(
        request,
        "Editar empresa" if pk else "Nueva empresa",
        form,
        reverse("admin_empresas"),
    )


@requiere_permiso(P.EMPRESAS_GESTIONAR)
def empresa_admin(request, pk):
    empresa = get_object_or_404(Empresa, pk=pk)
    form = UsuarioForm(request.POST or None, internos=False)
    form.fields["rol"].queryset = Rol.objects.filter(nombre=Rol.ADMIN_CLIENTE)
    if request.method == "POST" and form.is_valid():
        try:
            with transaction.atomic():
                Empresa.objects.select_for_update().get(pk=empresa.pk)
                if empresa.usuarios.filter(rol__nombre=Rol.ADMIN_CLIENTE).exists():
                    raise ReglaNegocioError(
                        "La empresa ya tiene un administrador inicial."
                    )
                u = form.save(commit=False)
                u.empresa = empresa
                u.set_password(form.cleaned_data["clave"])
                u.save()
            messages.success(request, "Administrador inicial creado.")
            return redirect("admin_empresas")
        except (ReglaNegocioError, IntegrityError) as e:
            error_form(form, e)
    return form_page(
        request,
        "Administrador inicial · " + empresa.nombre_empresa,
        form,
        reverse("admin_empresas"),
    )


@requiere_permiso(P.EMPRESAS_GESTIONAR)
def planes_admin(request, pk=None):
    obj = get_object_or_404(Plan, pk=pk) if pk else None
    form = PlanForm(request.POST or None, instance=obj)
    if request.method == "POST" and form.is_valid():
        obj = form.save()
        registrar("PLAN_GUARDADO", "suscripciones", obj, usuario=request.user)
        messages.success(request, "Plan guardado.")
        return redirect("planes_admin")
    return render(
        request,
        "admin/planes.html",
        {"form": form, "planes": Plan.objects.order_by("pk")},
    )


@requiere_permiso(P.AUDITORIA_VER)
def auditoria(request):
    qs = Auditoria.objects.select_related("usuario").order_by("-fecha")
    if request.GET.get("q"):
        qs = qs.filter(
            Q(accion__icontains=request.GET["q"])
            | Q(modulo__icontains=request.GET["q"])
        )
    return render(request, "admin/auditoria.html", {"page": pagina(request, qs)})


@requiere_permiso(P.IMPORTACIONES_EJECUTAR)
def importar(request):
    if request.method == "POST":
        try:
            archivo = request.FILES.get("archivo")
            if not archivo:
                raise ReglaNegocioError("Seleccione un archivo.")
            lote = imports.previsualizar(
                request.user, archivo, request.POST.get("tipo", "historico")
            )
            return redirect("importacion_detalle", pk=lote.pk)
        except ReglaNegocioError as e:
            messages.error(request, str(e))
    return render(
        request,
        "app/importar.html",
        {
            "lotes": LoteImportacion.objects.filter(
                empresa=request.user.empresa, usuario=request.user
            ).order_by("-fecha")[:50]
        },
    )


@requiere_permiso(P.IMPORTACIONES_EJECUTAR)
def importacion_detalle(request, pk):
    lote = get_object_or_404(
        LoteImportacion, pk=pk, empresa=request.user.empresa, usuario=request.user
    )
    if request.method == "POST":
        try:
            imports.confirmar(request.user, lote)
            messages.success(request, "Importación procesada; revise el resultado.")
            return redirect("importacion_detalle", pk=pk)
        except ReglaNegocioError as e:
            messages.error(request, str(e))
    return render(
        request,
        "app/importacion_detalle.html",
        {"lote": lote, "filas": lote.filas[:30]},
    )


@requiere_permiso(P.IMPORTACIONES_EJECUTAR)
def plantilla(request, formato):
    from .exportar import generar_archivo

    tipo = request.GET.get("tipo", "historico")
    if tipo not in imports.COLUMNAS:
        raise PermissionDenied
    return generar_archivo("plantilla_" + tipo, imports.COLUMNAS[tipo], [], formato)


@login_required
def ayuda(request):
    return render(request, "app/ayuda.html")


@requiere_permiso(P.REPORTES_VER)
def reportes(request):
    qs = lecturas_filtradas(request, lecturas(request.user.empresa)).order_by(
        "-fecha", "-pk"
    )
    return render(
        request,
        "app/reportes.html",
        {
            "page": pagina(request, qs),
            "estadisticas": estadisticas(qs),
            "colmenas": request.user.empresa.colmenas.order_by("numero"),
            "sensores": request.user.empresa.sensores.order_by("codigo"),
            "query": request.GET.urlencode(),
            "chart_url": reverse("telemetria"),
        },
    )


@requiere_permiso(P.REPORTES_EXPORTAR)
def exportar(request, formato):
    from .exportar import generar_archivo

    entidad = request.GET.get("entidad", "mediciones")
    e = request.user.empresa
    if entidad == "mediciones":
        qs = lecturas_filtradas(request, lecturas(e)).order_by("fecha", "pk")
        headers = [
            "Colmena",
            "Sensor",
            "Tipo",
            "Fecha",
            "Valor",
            "Unidad",
            "Origen",
            "Fuente",
        ]
        rows = [
            [
                f"{m.asociacion.colmena.numero or m.asociacion.colmena.numero_historico} · {m.asociacion.colmena.nombre}",
                m.sensor.codigo,
                m.tipo_medicion,
                timezone.localtime(m.fecha).isoformat(),
                m.valor,
                m.unidad,
                m.origen,
                m.fuente,
            ]
            for m in qs[:10000]
        ]
    elif entidad == "colmenas":
        headers = ["Número", "Nombre", "Ubicación", "Estado", "Archivada"]
        rows = list(
            e.colmenas.order_by("pk").values_list(
                "numero_historico", "nombre", "ubicacion", "estado", "archivada"
            )
        )
    elif entidad == "sensores":
        headers = ["Código", "Tipo", "Estado", "Sincronización"]
        rows = list(
            e.sensores.order_by("pk").values_list(
                "codigo", "tipo", "estado", "sync_estado"
            )
        )
    elif entidad == "produccion":
        qs = (
            ProduccionMiel.objects.filter(empresa=e)
            .select_related("colmena")
            .order_by("fecha")
        )
        if request.GET.get("colmena", "").isdigit():
            qs = qs.filter(colmena_id=request.GET["colmena"])
        for k, op in [("desde", "gte"), ("hasta", "lte")]:
            if request.GET.get(k):
                try:
                    d = forms.DateField().clean(request.GET[k])
                    qs = qs.filter(**{"fecha__" + op: d})
                except ValidationError:
                    return HttpResponse("Fecha inválida", status=400)
        headers = ["Colmena", "Fecha", "Cantidad", "Unidad", "Observaciones"]
        rows = [
            [str(x.colmena), x.fecha, x.cantidad, x.unidad, x.observaciones]
            for x in qs[:10000]
        ]
    else:
        return HttpResponse("Conjunto de datos no admitido", status=400)
    registrar(
        "EXPORTACION",
        "reportes",
        descripcion=f"{entidad} {formato}: {len(rows)} filas",
        usuario=request.user,
    )
    return generar_archivo(
        "apitech_" + entidad,
        headers,
        rows,
        formato,
        descripcion=f"Empresa: {e.nombre_empresa}. Filtros: {request.GET.urlencode()}. Máximo 10.000 filas; divida períodos mayores.",
    )


@require_POST
@requiere_permiso(P.EMPRESAS_GESTIONAR)
def suscripcion_estado(request, pk):
    s = get_object_or_404(Suscripcion, pk=pk)
    nuevo = request.POST.get("estado")
    if nuevo not in ("SUSPENDIDA", "CANCELADA", "ACTIVA"):
        raise PermissionDenied
    try:
        with transaction.atomic():
            Empresa.objects.select_for_update().get(pk=s.empresa_id)
            if nuevo == "ACTIVA" and (
                not s.fecha_vencimiento
                or s.fecha_vencimiento < timezone.localdate()
                or not s.pagos.filter(estado=Pago.VERIFICADO).exists()
            ):
                raise ReglaNegocioError(
                    "Se requiere pago verificado y vigencia para reactivar."
                )
            s.estado = nuevo
            s.save()
            if nuevo != "ACTIVA":
                from apps.empresas.services import detener_monitoreos

                detener_monitoreos(s.empresa)
            registrar("SUSCRIPCION_ESTADO", "suscripciones", s, nuevo, request.user)
        messages.success(request, "Suscripción actualizada.")
    except (ReglaNegocioError, IntegrityError) as e:
        messages.error(
            request,
            (
                str(e)
                if isinstance(e, ReglaNegocioError)
                else "Ya existe otra suscripción activa."
            ),
        )
    return redirect("admin_empresas")


@requiere_permiso(P.IMPORTACIONES_EJECUTAR)
def importacion_errores(request, pk):
    from .exportar import generar_archivo

    l = get_object_or_404(
        LoteImportacion, pk=pk, empresa=request.user.empresa, usuario=request.user
    )
    return generar_archivo(
        "errores_importacion",
        ["Fila", "Error"],
        [[e["fila"], e["error"]] for e in l.errores],
        "csv",
    )
