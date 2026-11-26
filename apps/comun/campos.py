"""Campos generados portables (MySQL, PostgreSQL, SQLite) para invariantes de unicidad.

Reproducen el patrón de MK5 (`activo_uniq`): la columna vale 1 solo cuando la fila está
"activa" y NULL en caso contrario; un índice único sobre (clave, columna) permite muchas
filas inactivas pero solo UNA activa. Funciona porque los índices únicos aceptan varios NULL.
"""

from django.db import models
from django.db.models import Case, IntegerField, Value, When


def activo_uniq(**condicion):
    return models.GeneratedField(
        expression=Case(
            When(**condicion, then=Value(1)),
            default=Value(None, output_field=IntegerField()),
            output_field=IntegerField(),
        ),
        output_field=models.SmallIntegerField(null=True),
        db_persist=True,
    )
