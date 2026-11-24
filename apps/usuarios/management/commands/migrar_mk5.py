"""Traslado transaccional de una copia MySQL MK5. No ejecuta el SQL original ni modifica el origen."""

from collections import defaultdict
from datetime import datetime
from zoneinfo import ZoneInfo
from django.apps import apps
from django.core.management.base import BaseCommand, CommandError
from django.db import connections, transaction
from django.utils import timezone
from django.core.management import call_command
from django.db.models import Q

# Orden de dependencias y correspondencias explícitas con database/schema.sql del MK5.
TABLAS = [
    ("empresas", "empresas.Empresa", "id_empresa", {}),
    ("roles", "usuarios.Rol", "id_rol", {"nombre": "nombre_rol"}),
    ("permisos", "usuarios.Permiso", "id_permiso", {"codigo": "codigo_permiso"}),
    (
        "usuarios",
        "usuarios.Usuario",
        "id_usuario",
        {"empresa_id": "id_empresa", "rol_id": "id_rol", "last_login": "ultimo_login"},
    ),
    ("planes_suscripcion", "suscripciones.Plan", "id_plan", {}),
    (
        "suscripciones",
        "suscripciones.Suscripcion",
        "id_suscripcion",
        {"empresa_id": "id_empresa", "plan_id": "id_plan"},
    ),
    (
        "pagos",
        "suscripciones.Pago",
        "id_pago",
        {
            "suscripcion_id": "id_suscripcion",
            "usuario_verifico_id": "id_usuario_verifico",
        },
    ),
    (
        "colmenas",
        "apiario.Colmena",
        "id_colmena",
        {"empresa_id": "id_empresa", "usuario_registro_id": "id_usuario_registro"},
    ),
    ("sensores", "apiario.Sensor", "id_sensor", {"empresa_id": "id_empresa"}),
    (
        "sensor_colmena",
        "apiario.SensorColmena",
        "id_asociacion",
        {
            "sensor_id": "id_sensor",
            "colmena_id": "id_colmena",
            "empresa_id": "id_empresa",
        },
    ),
    (
        "monitoreo",
        "monitoreo.Monitoreo",
        "id_monitoreo",
        {"asociacion_id": "id_asociacion"},
    ),
    (
        "rangos_umbrales",
        "monitoreo.RangoUmbral",
        "id_umbral",
        {"empresa_id": "id_empresa"},
    ),
    (
        "mediciones",
        "monitoreo.Medicion",
        "id_medicion",
        {"asociacion_id": "id_asociacion"},
    ),
    (
        "alertas",
        "monitoreo.Alerta",
        "id_alerta",
        {
            "colmena_id": "id_colmena",
            "empresa_id": "id_empresa",
            "medicion_id": "id_medicion",
            "sensor_id": "id_sensor",
            "usuario_resolucion_id": "id_usuario_resolucion",
        },
    ),
    (
        "notificaciones",
        "monitoreo.Notificacion",
        "id_notificacion",
        {
            "usuario_id": "id_usuario",
            "empresa_id": "id_empresa",
            "alerta_id": "id_alerta",
            "pago_id": "id_pago",
        },
    ),
    (
        "produccion_miel",
        "apiario.ProduccionMiel",
        "id_produccion",
        {"colmena_id": "id_colmena", "empresa_id": "id_empresa"},
    ),
    ("auditoria", "auditoria.Auditoria", "id_auditoria", {"usuario_id": "id_usuario"}),
    (
        "solicitudes_suscripcion",
        "suscripciones.SolicitudSuscripcion",
        "id_solicitud",
        {
            "plan_solicitado_id": "id_plan_solicitado",
            "usuario_revisor_id": "id_usuario_revisor",
        },
    ),
]


class Command(BaseCommand):
    help = "Valida el traslado MK5→MK6. Sin --aplicar revierte todo al final. Solo destino vacío (sin usuarios/empresas)."

    def add_arguments(self, p):
        p.add_argument("--aplicar", action="store_true")
        p.add_argument("--zona-origen", default="America/Bogota")
        p.add_argument("--offset-timestamp", default="-05:00")

    def handle(self, *args, **o):
        src = connections["legacy"]
        dst = connections["default"]
        if src.settings_dict["NAME"] == dst.settings_dict["NAME"]:
            raise CommandError("Origen y destino deben ser bases diferentes.")
        if (
            apps.get_model("empresas", "Empresa").objects.exists()
            or apps.get_model("usuarios", "Usuario").objects.exists()
        ):
            raise CommandError(
                "El destino ya contiene empresas/usuarios. Use una base MK6 nueva; no se sobrescriben datos."
            )
        try:
            zona = ZoneInfo(o["zona_origen"])
        except Exception:
            raise CommandError("Zona horaria inválida.")
        with src.cursor() as cursor:
            if src.vendor == "mysql":
                cursor.execute("SET SESSION time_zone = %s", [o["offset_timestamp"]])
            disponibles = set(src.introspection.table_names(cursor))
        requeridas = {t for t, _, _, _ in TABLAS}
        if requeridas - disponibles:
            raise CommandError(
                "Faltan tablas de origen: "
                + ", ".join(sorted(requeridas - disponibles))
            )
        contadores = defaultdict(int)
        try:
            with transaction.atomic():
                # Retirar únicamente semillas automáticas de la BD nueva, dentro de la transacción.
                apps.get_model(
                    "usuarios", "Rol"
                ).permisos.through.objects.all().delete()
                for label in [
                    "monitoreo.RangoUmbral",
                    "suscripciones.Plan",
                    "usuarios.Rol",
                    "usuarios.Permiso",
                ]:
                    apps.get_model(label).objects.all().delete()
                for tabla, label, pk, mapping in TABLAS:
                    model = apps.get_model(label)
                    with src.cursor() as cursor:
                        cursor.execute(
                            "SELECT * FROM "
                            + src.ops.quote_name(tabla)
                            + " ORDER BY "
                            + src.ops.quote_name(pk)
                        )
                        columns = [d[0] for d in cursor.description]
                        rows = [dict(zip(columns, r)) for r in cursor.fetchall()]
                    for row in rows:
                        if label == "usuarios.Rol" and row.get("nombre_rol") not in (
                            "Admin_ApiTech",
                            "Empleado_ApiTech",
                            "Admin_Cliente",
                            "Empleado_Cliente",
                        ):
                            raise CommandError(
                                "Rol no admitido en MK6: " + str(row.get("nombre_rol"))
                            )
                        data = {"id": row[pk]}
                        dates = {}
                        for f in model._meta.concrete_fields:
                            if f.primary_key or f.generated:
                                continue
                            source = mapping.get(f.attname, f.attname)
                            if source in row:
                                v = row[source]
                                if (
                                    v is None
                                    and not f.null
                                    and f.get_internal_type()
                                    in ("CharField", "TextField", "EmailField")
                                ):
                                    v = ""
                                if isinstance(v, datetime) and timezone.is_naive(v):
                                    v = timezone.make_aware(v, zona)
                                data[f.attname] = v
                                if getattr(f, "auto_now", False) or getattr(
                                    f, "auto_now_add", False
                                ):
                                    dates[f.attname] = v
                        if label == "usuarios.Usuario":
                            pwd = data.get("password", "")
                            if pwd.startswith(("$2a$", "$2b$", "$2y$")):
                                data["password"] = "bcrypt$" + pwd
                            elif not pwd.startswith(("pbkdf2_sha256$", "bcrypt$", "!")):
                                raise CommandError(
                                    f"Hash de usuario {row[pk]} no reconocido."
                                )
                        if label == "suscripciones.Plan":
                            limite = (
                                60
                                if "profesional" in data["nombre_plan"].lower()
                                else 20
                            )
                            data.update(max_colmenas=limite, max_sensores=limite)
                        if label == "apiario.Colmena":
                            contadores["numero-" + str(data["empresa_id"])] += 1
                            n = contadores["numero-" + str(data["empresa_id"])]
                            data.update(numero=n, numero_historico=n)
                        if label == "monitoreo.Medicion":
                            a = apps.get_model("apiario", "SensorColmena").objects.get(
                                pk=data["asociacion_id"]
                            )
                            data.update(
                                sensor_id=a.sensor_id,
                                fuente="MIGRACION_MK5",
                                id_externo="mk5-" + str(row[pk]),
                            )
                        if (
                            label == "monitoreo.RangoUmbral"
                            and data.get("empresa_id") is None
                        ):
                            data["es_demostracion"] = True
                        if label == "auditoria.Auditoria":
                            data["actor_tipo"] = (
                                "USUARIO" if data.get("usuario_id") else "PROCESO"
                            )
                        obj = model(**data)
                        # Fechas históricas se conservan antes de evaluar relaciones temporales.
                        for f in model._meta.concrete_fields:
                            if f.name in dates:
                                setattr(obj, f.name, dates[f.name])
                        obj.save(force_insert=True)
                        if dates:
                            model.objects.filter(pk=obj.pk).update(**dates)
                    actual = model.objects.count()
                    if actual != len(rows):
                        raise CommandError(
                            f"Conteo incompatible para {tabla}: {len(rows)}→{actual}"
                        )
                    self.stdout.write(f"{tabla}: {len(rows)} registros verificados")
                # Verificar que las lecturas caen dentro del intervalo de su asociación.
                Medicion = apps.get_model("monitoreo", "Medicion")
                from django.db.models import F

                if Medicion.objects.filter(
                    Q(fecha__lt=F("asociacion__fecha_inicio"))
                    | Q(
                        asociacion__fecha_fin__isnull=False,
                        fecha__gte=F("asociacion__fecha_fin"),
                    )
                ).exists():
                    raise CommandError(
                        "Hay mediciones fuera del intervalo de asociación. Corrija la copia MK5 antes de continuar."
                    )
                from apps.usuarios.permisos import sincronizar_roles_y_permisos

                sincronizar_roles_y_permisos()
                # Los dispositivos migrados quedan preparados para el servicio externo.
                from apps.integraciones.services import encolar
                from apps.empresas.services import suscripcion_vigente

                for sensor in apps.get_model(
                    "apiario", "Sensor"
                ).objects.select_related("empresa"):
                    encolar(sensor, "REGISTER")
                    for a in sensor.asociaciones.filter(activo=True).select_related(
                        "colmena"
                    ):
                        if a.monitoreos.filter(estado="activo").exists():
                            if (
                                sensor.estado == "activo"
                                and a.colmena.estado == "Estable"
                                and not sensor.empresa.esta_pausada
                                and suscripcion_vigente(sensor.empresa)
                            ):
                                encolar(sensor, "ACTIVATE", a)
                            else:
                                a.monitoreos.filter(estado="activo").update(
                                    estado="inactivo", fecha_fin=timezone.now()
                                )
                # Los permisos se normalizan a la matriz MK6; no se arrastran permisos Java obsoletos.
                if not o["aplicar"]:
                    transaction.set_rollback(True)
        except Exception as e:
            raise CommandError(f"Traslado revertido. {type(e).__name__}: {e}")
        self.stdout.write(
            self.style.SUCCESS(
                "Traslado aplicado."
                if o["aplicar"]
                else "Validación completada; no se guardaron cambios. Repita con --aplicar."
            )
        )
