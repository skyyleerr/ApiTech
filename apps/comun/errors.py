"""Errores de dominio compartidos por todas las aplicaciones."""


class ReglaNegocioError(Exception):
    """Se intentó una operación que viola una regla de negocio (mensaje apto para el usuario)."""


class PermisoDenegadoError(ReglaNegocioError):
    """El actor no tiene permiso sobre el recurso (incluye acceso a datos de otra empresa)."""
