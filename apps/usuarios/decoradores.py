from functools import wraps

from django.contrib.auth.views import redirect_to_login
from django.core.exceptions import PermissionDenied
from django.shortcuts import get_object_or_404

from .permisos import tiene_permiso


def requiere_permiso(codigo):
    """Autoriza en el SERVIDOR cada vista (no basta con ocultar botones)."""

    def decorador(vista):
        @wraps(vista)
        def envoltura(request, *args, **kwargs):
            if not request.user.is_authenticated:
                return redirect_to_login(request.get_full_path())
            if not tiene_permiso(request.user, codigo):
                raise PermissionDenied
            return vista(request, *args, **kwargs)

        return envoltura

    return decorador


def objeto_de_mi_empresa(modelo, request, pk, campo_empresa="empresa_id"):
    """Devuelve el objeto SOLO si es de la empresa del usuario; si no, 404 (no revela existencia)."""
    return get_object_or_404(modelo, pk=pk, **{campo_empresa: request.user.empresa_id})
