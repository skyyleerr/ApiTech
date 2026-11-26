from apps.usuarios.permisos import permisos_de_rol
from apps.apiario.models import TipoMedicion


def permisos(request):
    q = request.GET.copy()
    q.pop("page", None)
    return {
        "permisos": (
            permisos_de_rol(request.user.rol.nombre)
            if request.user.is_authenticated
            else set()
        ),
        "notificaciones_pendientes": (
            request.user.notificaciones.filter(leida=False).count()
            if request.user.is_authenticated
            else 0
        ),
        "tipos_medicion": TipoMedicion.values,
        "filtros_sin_pagina": q.urlencode(),
    }
