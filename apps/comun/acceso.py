from apps.comun.errors import PermisoDenegadoError
from apps.usuarios.permisos import tiene_permiso


def exigir(usuario, permiso, empresa_id=None):
    """Defensa en profundidad: los servicios re-validan permiso y empresa del actor.

    Sirve igual para vistas, API, importaciones y procesos: el aislamiento no depende de la vista.
    """
    if not tiene_permiso(usuario, permiso):
        raise PermisoDenegadoError("No tienes permiso para realizar esta operación.")
    if (
        empresa_id is not None
        and not usuario.es_interno
        and usuario.empresa_id != empresa_id
    ):
        raise PermisoDenegadoError("No puedes operar con datos de otra empresa.")
