"""Matriz de permisos de MK6 (fuente de verdad en código; se sincroniza a las tablas roles/permisos).

Derivada de los documentos y de los controles de MK5. Véase docs/Permisos.md.
"""

from .models import Rol

# Permisos internos (equipo ApiTech)
EMPRESAS_VER = "empresas.ver"
EMPRESAS_GESTIONAR = "empresas.gestionar"
USUARIOS_INTERNOS_GESTIONAR = "usuarios_internos.gestionar"
SOLICITUDES_VER = "solicitudes.ver"
SOLICITUDES_REVISAR = "solicitudes.revisar"
PAGOS_VER_TODOS = "pagos.ver_todos"
PAGOS_VERIFICAR = "pagos.verificar"
AUDITORIA_VER = "auditoria.ver"
# Permisos de cliente (siempre acotados a la propia empresa)
COLMENAS_VER = "colmenas.ver"
COLMENAS_GESTIONAR = "colmenas.gestionar"
SENSORES_VER = "sensores.ver"
SENSORES_GESTIONAR = "sensores.gestionar"
MONITOREO_GESTIONAR = "monitoreo.gestionar"
MEDICIONES_VER = "mediciones.ver"
ALERTAS_VER = "alertas.ver"
ALERTAS_RESOLVER = "alertas.resolver"
PRODUCCION_VER = "produccion.ver"
PRODUCCION_REGISTRAR = "produccion.registrar"
UMBRALES_GESTIONAR = "umbrales.gestionar"
USUARIOS_EMPRESA_GESTIONAR = "usuarios_empresa.gestionar"
REPORTES_VER = "reportes.ver"
REPORTES_EXPORTAR = "reportes.exportar"
IMPORTACIONES_EJECUTAR = "importaciones.ejecutar"
PAGOS_VER_PROPIOS = "pagos.ver_propios"

# Equipo interno operativo: revisa solicitudes y pagos; no administra empresas/usuarios.
EMPLEADO_APITECH_PUEDE_REVISAR = True
# Empleado cliente: reportes, importación histórica y resolución; no configura dispositivos.
EMPLEADO_CLIENTE_PUEDE_OPERAR = True

_CLIENTE_LECTURA = {
    COLMENAS_VER,
    SENSORES_VER,
    MEDICIONES_VER,
    ALERTAS_VER,
    PRODUCCION_VER,
    REPORTES_VER,
    PAGOS_VER_PROPIOS,
}
_CLIENTE_ADMIN = _CLIENTE_LECTURA | {
    COLMENAS_GESTIONAR,
    SENSORES_GESTIONAR,
    MONITOREO_GESTIONAR,
    ALERTAS_RESOLVER,
    PRODUCCION_REGISTRAR,
    UMBRALES_GESTIONAR,
    USUARIOS_EMPRESA_GESTIONAR,
    REPORTES_EXPORTAR,
    IMPORTACIONES_EJECUTAR,
    PAGOS_VER_PROPIOS,
}
_INTERNO_LECTURA = {EMPRESAS_VER, SOLICITUDES_VER, PAGOS_VER_TODOS, AUDITORIA_VER}
_INTERNO_ADMIN = _INTERNO_LECTURA | {
    EMPRESAS_GESTIONAR,
    USUARIOS_INTERNOS_GESTIONAR,
    SOLICITUDES_REVISAR,
    PAGOS_VERIFICAR,
}


def permisos_de_rol(nombre_rol):
    if nombre_rol == Rol.ADMIN_APITECH:
        return set(_INTERNO_ADMIN)
    if nombre_rol == Rol.EMPLEADO_APITECH:
        extra = (
            {SOLICITUDES_REVISAR, PAGOS_VERIFICAR}
            if EMPLEADO_APITECH_PUEDE_REVISAR
            else set()
        )
        return _INTERNO_LECTURA | extra
    if nombre_rol == Rol.ADMIN_CLIENTE:
        return set(_CLIENTE_ADMIN)
    if nombre_rol == Rol.EMPLEADO_CLIENTE:
        extra = (
            {ALERTAS_RESOLVER, IMPORTACIONES_EJECUTAR, REPORTES_EXPORTAR}
            if EMPLEADO_CLIENTE_PUEDE_OPERAR
            else set()
        )
        return _CLIENTE_LECTURA | extra
    return set()


def sincronizar_roles_y_permisos(**kwargs):
    """Idempotente. Se ejecuta tras `migrate` para que roles y permisos existan siempre."""
    from .models import Permiso

    descripciones = {
        Rol.ADMIN_APITECH: "Administrador de la plataforma ApiTech (sin empresa)",
        Rol.EMPLEADO_APITECH: "Empleado operativo de ApiTech (sin empresa)",
        Rol.ADMIN_CLIENTE: "Administrador de una empresa cliente",
        Rol.EMPLEADO_CLIENTE: "Empleado operativo de una empresa cliente",
    }
    todos = set().union(*(permisos_de_rol(n) for n in descripciones))
    objetos = {c: Permiso.objects.get_or_create(codigo=c)[0] for c in sorted(todos)}
    for nombre, desc in descripciones.items():
        rol, _ = Rol.objects.get_or_create(
            nombre=nombre, defaults={"descripcion": desc}
        )
        rol.permisos.set([objetos[c] for c in permisos_de_rol(nombre)])


def tiene_permiso(usuario, codigo):
    return bool(
        usuario
        and usuario.is_authenticated
        and usuario.is_active
        and codigo in permisos_de_rol(usuario.rol.nombre)
    )
