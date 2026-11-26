from django.apps import AppConfig
from django.db.models.signals import post_migrate


class UsuariosConfig(AppConfig):
    name = "apps.usuarios"
    label = "usuarios"

    def ready(self):
        from .permisos import sincronizar_roles_y_permisos

        post_migrate.connect(sincronizar_roles_y_permisos, sender=self)
