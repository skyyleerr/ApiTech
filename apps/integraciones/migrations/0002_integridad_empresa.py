"""Integridad entre tablas aun cuando se escribe con SQL/bulk_update.

No duplica las reglas temporales/estados de negocio de los servicios: protege las
identidades de empresa y las relaciones históricas en SQLite y MySQL/MariaDB.
"""

from django.db import migrations

REGLAS = {
    "usuarios": """EXISTS (SELECT 1 FROM roles r WHERE r.id=NEW.rol_id AND
 ((r.nombre IN ('Admin_ApiTech','Empleado_ApiTech') AND NEW.empresa_id IS NULL AND NEW.es_interno=1)
 OR (r.nombre IN ('Admin_Cliente','Empleado_Cliente') AND NEW.empresa_id IS NOT NULL AND NEW.es_interno=0)))""",
    "sensor_colmena": """EXISTS (SELECT 1 FROM sensores s JOIN colmenas c ON c.id=NEW.colmena_id
 WHERE s.id=NEW.sensor_id AND s.empresa_id=NEW.empresa_id AND c.empresa_id=NEW.empresa_id)""",
    "mediciones": """EXISTS (SELECT 1 FROM sensor_colmena a JOIN sensores s ON s.id=a.sensor_id
 WHERE a.id=NEW.asociacion_id AND s.id=NEW.sensor_id AND s.tipo=NEW.tipo_medicion
 AND NEW.origen=CASE WHEN s.es_simulado=1 THEN 'SIMULADO' ELSE 'REAL' END
 AND NEW.unidad=CASE s.tipo WHEN 'TEMPERATURA' THEN '°C' WHEN 'HUMEDAD' THEN '%' WHEN 'PESO' THEN 'kg' END)""",
    "produccion_miel": "EXISTS (SELECT 1 FROM colmenas c WHERE c.id=NEW.colmena_id AND c.empresa_id=NEW.empresa_id)",
    "alertas": """EXISTS (SELECT 1 FROM colmenas c WHERE c.id=NEW.colmena_id AND c.empresa_id=NEW.empresa_id)
 AND (NEW.sensor_id IS NULL OR EXISTS (SELECT 1 FROM sensores s WHERE s.id=NEW.sensor_id AND s.empresa_id=NEW.empresa_id))
 AND (NEW.medicion_id IS NULL OR EXISTS (SELECT 1 FROM mediciones m JOIN sensor_colmena a ON a.id=m.asociacion_id
 WHERE m.id=NEW.medicion_id AND a.colmena_id=NEW.colmena_id AND m.sensor_id=NEW.sensor_id))""",
    "notificaciones": """EXISTS (SELECT 1 FROM usuarios u WHERE u.id=NEW.usuario_id AND
 (u.empresa_id=NEW.empresa_id OR (u.empresa_id IS NULL AND NEW.empresa_id IS NULL)))
 AND (NEW.alerta_id IS NULL OR EXISTS (SELECT 1 FROM alertas a WHERE a.id=NEW.alerta_id AND a.empresa_id=NEW.empresa_id))
 AND (NEW.pago_id IS NULL OR NEW.empresa_id IS NULL OR EXISTS (SELECT 1 FROM pagos p JOIN suscripciones s ON s.id=p.suscripcion_id WHERE p.id=NEW.pago_id AND s.empresa_id=NEW.empresa_id))""",
    "pagos": """NEW.usuario_verifico_id IS NULL OR EXISTS (SELECT 1 FROM usuarios u WHERE u.id=NEW.usuario_verifico_id AND u.es_interno=1)""",
    "comandos_sensor": """NEW.asociacion_id IS NULL OR EXISTS (SELECT 1 FROM sensor_colmena a WHERE a.id=NEW.asociacion_id AND a.sensor_id=NEW.sensor_id)""",
    "importaciones": """EXISTS (SELECT 1 FROM usuarios u WHERE u.id=NEW.usuario_id AND u.empresa_id=NEW.empresa_id)""",
}


INMUTABLES = {
    "usuarios": ["empresa_id"],
    "sensores": ["empresa_id", "tipo", "es_simulado"],
    "colmenas": ["empresa_id"],
    "sensor_colmena": ["empresa_id", "sensor_id", "colmena_id"],
    "mediciones": ["sensor_id", "asociacion_id"],
    "produccion_miel": ["empresa_id"],
}


def crear(apps, editor):
    vendor = editor.connection.vendor
    if vendor not in ("sqlite", "mysql"):
        raise RuntimeError("Motor no soportado: use MySQL/MariaDB o SQLite.")
    for tabla in set(REGLAS) | set(INMUTABLES):
        regla = REGLAS.get(tabla, "1=1")
        for evento in ("INSERT", "UPDATE"):
            condicion = regla
            if evento == "UPDATE":
                for campo in INMUTABLES.get(tabla, []):
                    condicion += f" AND (NEW.{campo}=OLD.{campo} OR (NEW.{campo} IS NULL AND OLD.{campo} IS NULL))"
            nombre = f"mk6_{tabla}_{evento.lower()}"
            msg = f"Integridad de empresa/identidad: {tabla}"
            if vendor == "mysql":
                sql = f"CREATE TRIGGER {nombre} BEFORE {evento} ON {tabla} FOR EACH ROW BEGIN IF NOT ({condicion}) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='{msg}'; END IF; END"
            else:
                sql = f"CREATE TRIGGER {nombre} BEFORE {evento} ON {tabla} BEGIN SELECT RAISE(ABORT, '{msg}') WHERE NOT ({condicion}); END"
            editor.execute(f"DROP TRIGGER IF EXISTS {nombre}")
            editor.execute(sql, params=None)


def eliminar(apps, editor):
    for tabla in set(REGLAS) | set(INMUTABLES):
        for evento in ("insert", "update"):
            editor.execute(f"DROP TRIGGER IF EXISTS mk6_{tabla}_{evento}")


class Migration(migrations.Migration):
    atomic = False
    dependencies = [
        ("integraciones", "0001_initial"),
        ("importaciones", "0002_initial"),
        ("monitoreo", "0003_remove_medicion_uq_medicion_sensor_externo_and_more"),
        ("apiario", "0003_produccionmiel_referencia_importacion"),
        ("auditoria", "0003_auditoria_registro_afectado"),
        ("suscripciones", "0002_initial"),
        ("usuarios", "0001_initial"),
    ]
    operations = [migrations.RunPython(crear, eliminar, atomic=False)]
