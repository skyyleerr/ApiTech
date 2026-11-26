"""Cliente del contrato MK6 v1. No genera mediciones; solo consume el servicio de Karen."""

import os, time, uuid
from urllib.parse import urlsplit, quote
import requests
from django.conf import settings
from django.db import transaction
from django.db.models import F
from django.utils import timezone
from django.utils.dateparse import parse_datetime
from apps.apiario.models import Sensor, SensorColmena, UNIDADES
from apps.monitoreo.models import Medicion, Monitoreo
from apps.monitoreo.services import registrar_medicion
from apps.comun.errors import ReglaNegocioError
from .models import ConexionAPI, ComandoSensor


class APIError(ReglaNegocioError):
    pass


def configuracion(empresa_id):
    url = os.environ.get(
        f"SENSOR_API_URL_{empresa_id}", settings.SENSOR_API_BASE_URL
    ).rstrip("/")
    token = os.environ.get(f"SENSOR_API_TOKEN_{empresa_id}", settings.SENSOR_API_TOKEN)
    parts = urlsplit(url)
    if not url or not token:
        raise APIError("La conexión externa aún no está configurada.")
    if parts.username or parts.password or parts.query or parts.fragment:
        raise APIError("URL de API no válida.")
    if parts.scheme != "https" and not (
        settings.SENSOR_API_ALLOW_LOCAL
        and parts.scheme == "http"
        and parts.hostname in ("localhost", "127.0.0.1")
    ):
        raise APIError("La conexión requiere HTTPS.")
    return url, token


class Cliente:
    def __init__(self, empresa_id):
        self.url, self.token = configuracion(empresa_id)
        self.empresa_id = str(empresa_id)

    def request(self, method, ruta, *, clave=None, data=None, params=None):
        headers = {
            "Authorization": f"Bearer {self.token}",
            "Accept": "application/json",
            "X-Company-ID": self.empresa_id,
        }
        if clave:
            headers["Idempotency-Key"] = str(clave)
        for intento in range(3):
            try:
                r = requests.request(
                    method,
                    self.url + ruta,
                    headers=headers,
                    json=data,
                    params=params,
                    timeout=settings.SENSOR_API_TIMEOUT,
                    allow_redirects=False,
                )
                if r.status_code in (429, 500, 502, 503, 504) and intento < 2:
                    time.sleep(0.25 * (2**intento))
                    continue
                if r.status_code not in (200, 201):
                    raise APIError(
                        f"El servicio externo respondió HTTP {r.status_code}."
                    )
                try:
                    body = r.json()
                except ValueError:
                    raise APIError("La API devolvió una respuesta JSON inválida.")
                if not isinstance(body, dict):
                    raise APIError("Respuesta incompatible con el contrato v1.")
                return body
            except (requests.Timeout, requests.ConnectionError):
                if intento == 2:
                    raise APIError("No fue posible conectar con el servicio externo.")
                time.sleep(0.25 * (2**intento))
            except requests.RequestException:
                raise APIError("Error en la solicitud al servicio externo.")


def encolar(sensor, accion, asociacion=None):
    cmd = ComandoSensor.objects.create(
        sensor=sensor, accion=accion, asociacion=asociacion, clave=uuid.uuid4()
    )
    sensor.sync_estado = Sensor.SYNC_PENDIENTE
    sensor.sync_error = ""
    sensor.save()
    return cmd


def enviar_comandos(empresa):
    cliente = Cliente(empresa.pk)
    for pk in (
        ComandoSensor.objects.filter(sensor__empresa=empresa, completado=False)
        .order_by("pk")
        .values_list("pk", flat=True)
    ):
        try:
            with transaction.atomic():
                cmd = (
                    ComandoSensor.objects.select_for_update()
                    .select_related("sensor", "asociacion__colmena")
                    .get(pk=pk)
                )
                if cmd.completado:
                    continue
                s = cmd.sensor
                if not s.id_externo:
                    data = cliente.request(
                        "POST",
                        "/sensors",
                        clave=f"registro-{s.empresa_id}-{s.pk}",
                        data={
                            "client_ref": str(s.pk),
                            "company_id": str(s.empresa_id),
                            "sensor_code": s.codigo,
                            "type": s.tipo,
                            "unit": UNIDADES[s.tipo],
                            "origin": "SIMULATED" if s.es_simulado else "REAL",
                        },
                    )
                    if str(data.get("company_id")) != str(s.empresa_id) or not data.get(
                        "sensor_id"
                    ):
                        raise APIError(
                            "Registro externo sin identidad válida de empresa/sensor."
                        )
                    if len(str(data["sensor_id"])) > 100:
                        raise APIError("Identificador externo demasiado largo.")
                    s.id_externo = str(data["sensor_id"])
                    s.save()
                if cmd.accion in ("ACTIVATE", "DEACTIVATE"):
                    a = cmd.asociacion
                    payload = {
                        "company_id": str(s.empresa_id),
                        "association_id": str(a.pk) if a else None,
                        "hive_id": str(a.colmena_id) if a else None,
                        "effective_at": cmd.fecha.isoformat(),
                    }
                    data = cliente.request(
                        "POST",
                        f'/sensors/{quote(s.id_externo,safe="")}/{cmd.accion.lower()}',
                        clave=cmd.clave,
                        data=payload,
                    )
                    if str(data.get("sensor_id")) != s.id_externo or str(
                        data.get("company_id")
                    ) != str(s.empresa_id):
                        raise APIError("Confirmación externa incompatible.")
                    esperado = "ACTIVE" if cmd.accion == "ACTIVATE" else "INACTIVE"
                    if data.get("status") != esperado:
                        raise APIError("Estado externo no confirmado.")
                cmd.completado = True
                cmd.error = ""
                cmd.intentos += 1
                cmd.save()
                s.sync_estado = Sensor.SYNC_CONFIRMADO
                s.sync_error = ""
                s.save()
        except APIError as e:
            ComandoSensor.objects.filter(pk=pk).update(
                error=str(e), intentos=F("intentos") + 1
            )
            Sensor.objects.filter(pk=cmd.sensor_id).update(
                sync_estado=Sensor.SYNC_ERROR, sync_error=str(e)
            )
            raise


def sincronizar(empresa):
    conexion, _ = ConexionAPI.objects.get_or_create(empresa=empresa)
    if not conexion.habilitada:
        raise APIError("La conexión externa está deshabilitada.")
    from apps.empresas.services import validar_puede_operar

    try:
        # Enviar desactivaciones incluso si la empresa ya está pausada.
        enviar_comandos(empresa)
        validar_puede_operar(empresa)
        cliente = Cliente(empresa.pk)
        total = 0
        for sensor in empresa.sensores.exclude(id_externo=None).order_by("pk"):
            for _ in range(50):
                cursor_anterior = sensor.cursor_sincronizacion
                body = cliente.request(
                    "GET",
                    "/readings",
                    params={
                        "sensor_id": sensor.id_externo,
                        "after": cursor_anterior,
                        "limit": 100,
                    },
                )
                rows = body.get("items")
                cursor = body.get("next_cursor")
                if not isinstance(body.get("has_more", False), bool):
                    raise APIError("El indicador has_more debe ser booleano.")
                if not isinstance(rows, list) or len(rows) > 100:
                    raise APIError("Página de lecturas inválida.")
                with transaction.atomic():
                    locked = Sensor.objects.select_for_update().get(pk=sensor.pk)
                    if locked.cursor_sincronizacion != cursor_anterior:
                        raise APIError(
                            "Otra sincronización avanzó el cursor. Vuelva a intentar."
                        )
                    for row in rows:
                        requeridos = {
                            "company_id",
                            "sensor_id",
                            "association_id",
                            "hive_id",
                            "origin",
                            "type",
                            "unit",
                            "reading_id",
                            "measured_at",
                            "value",
                        }
                        if (
                            not isinstance(row, dict)
                            or not requeridos.issubset(row)
                            or row.get("type") not in UNIDADES
                            or row.get("unit") != UNIDADES[sensor.tipo]
                        ):
                            raise APIError("Lectura inválida o incompleta.")
                        if (
                            str(row.get("company_id")) != str(empresa.pk)
                            or str(row.get("sensor_id")) != sensor.id_externo
                        ):
                            raise APIError(
                                "La lectura no pertenece a la empresa/sensor solicitados."
                            )
                        a = SensorColmena.objects.filter(
                            pk=row.get("association_id"), sensor=sensor, empresa=empresa
                        ).first()
                        if not a or str(row.get("hive_id")) != str(a.colmena_id):
                            raise APIError("Asociación histórica incompatible.")
                        fecha = parse_datetime(str(row.get("measured_at", "")))
                        if not fecha or timezone.is_naive(fecha):
                            raise APIError("Fecha externa sin zona horaria.")
                        if row.get("origin") != (
                            "SIMULATED" if sensor.es_simulado else "REAL"
                        ):
                            raise APIError("Origen externo incompatible.")
                        if (
                            not row.get("reading_id")
                            or len(str(row["reading_id"])) > 100
                        ):
                            raise APIError("Identidad de lectura inválida.")
                        m, created = registrar_medicion(
                            sensor,
                            row.get("value"),
                            fecha,
                            tipo=row.get("type"),
                            unidad=row.get("unit"),
                            id_externo=str(row["reading_id"]),
                            fuente=Medicion.FUENTE_API,
                        )
                        if m.asociacion_id != a.pk:
                            raise APIError(
                                "La fecha no corresponde a la asociación informada."
                            )
                        total += int(created)
                    if cursor is not None:
                        if (
                            not isinstance(cursor, str)
                            or len(cursor) > 100
                            or cursor == cursor_anterior
                        ):
                            raise APIError("Cursor inválido o repetido.")
                        locked.cursor_sincronizacion = cursor
                    locked.ultima_sincronizacion = timezone.now()
                    locked.save()
                    sensor = locked
                if not body.get("has_more", False):
                    break
                if not rows or cursor is None:
                    raise APIError("Paginación incompleta.")
        conexion.ultimo_exito = timezone.now()
        conexion.ultimo_error = ""
        conexion.save()
        return total
    except (APIError, ReglaNegocioError, ValueError, TypeError) as e:
        conexion.ultimo_error = str(e)[:250]
        conexion.save()
        raise APIError(str(e))
