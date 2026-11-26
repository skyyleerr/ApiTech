from django.contrib.auth import get_user_model

from .models import Rol


class CorreoBackend:
    """Autentica por correo + contraseña.

    - Usuarios inactivos no ingresan.
    - Usuarios de cliente solo ingresan con el NIT (empresa) de su propia empresa (`empresa_id`);
      el NIT elegido en el navegador nunca define la empresa real de la sesión.
    - Acepta hashes BCrypt de MK5 guardados como `bcrypt$<hash>` y los actualiza a PBKDF2.
    """

    def authenticate(
        self, request, correo=None, password=None, empresa_id=None, **kwargs
    ):
        correo = correo or kwargs.get("username")
        if not correo or password is None:
            return None
        Usuario = get_user_model()
        try:
            usuario = Usuario.objects.select_related("rol", "empresa").get(
                correo=correo.strip().lower()
            )
        except Usuario.DoesNotExist:
            Usuario().set_password(password)  # iguala tiempos de respuesta
            return None
        if not usuario.check_password(password) or not usuario.is_active:
            return None
        if usuario.rol.nombre in Rol.CLIENTES and (
            empresa_id is None or usuario.empresa_id != empresa_id
        ):
            return None
        return usuario

    def get_user(self, user_id):
        Usuario = get_user_model()
        try:
            usuario = Usuario.objects.select_related("rol", "empresa").get(pk=user_id)
        except Usuario.DoesNotExist:
            return None
        return usuario if usuario.is_active else None
