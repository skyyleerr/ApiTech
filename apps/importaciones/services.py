import csv, io, hashlib, json
from datetime import datetime, timezone as dt_timezone
from decimal import Decimal
from pathlib import Path
from zipfile import ZipFile, BadZipFile
from openpyxl import load_workbook
from django.db import transaction
from django.utils import timezone
from django.utils.dateparse import parse_datetime, parse_date
from apps.apiario.models import SensorColmena, ProduccionMiel, Colmena, UNIDADES
from apps.apiario.services import crear_colmena, registrar_sensor
from apps.monitoreo.models import Medicion
from apps.monitoreo.services import registrar_medicion
from apps.comun.errors import ReglaNegocioError
from apps.empresas.services import validar_puede_operar
from apps.comun.acceso import exigir
from apps.usuarios import permisos as P
from apps.auditoria.models import registrar
from .models import LoteImportacion

COLUMNAS = {
    "historico": [
        "fecha",
        "colmena",
        "asociacion",
        "tipo",
        "valor",
        "unidad",
        "origen",
        "produccion",
        "observaciones",
    ],
    "colmenas": ["nombre", "ubicacion", "estado"],
    "sensores": ["codigo", "tipo", "modelo", "fabricante"],
}


def leer(archivo, tipo):
    if tipo not in COLUMNAS:
        raise ReglaNegocioError("Tipo de importación inválido.")
    if archivo.size > 5 * 1024 * 1024:
        raise ReglaNegocioError("El archivo no puede superar 5 MB.")
    data = archivo.read()
    ext = Path(archivo.name).suffix.lower()
    try:
        if ext == ".csv":
            text = data.decode("utf-8-sig")
            reader = csv.DictReader(io.StringIO(text))
            headers = reader.fieldnames or []
            rows = list(reader)
        elif ext == ".xlsx":
            with ZipFile(io.BytesIO(data)) as z:
                if sum(i.file_size for i in z.infolist()) > 25 * 1024 * 1024:
                    raise ReglaNegocioError("El Excel descomprimido supera el límite.")
            book = load_workbook(io.BytesIO(data), read_only=True, data_only=False)
            iterator = iter(book.active.values)
            headers = [str(x or "") for x in next(iterator)]
            rows = []
            for row in iterator:
                rows.append(
                    dict(
                        zip(
                            headers,
                            [
                                (
                                    v.isoformat()
                                    if isinstance(v, datetime)
                                    else str(v) if v is not None else ""
                                )
                                for v in row
                            ],
                        )
                    )
                )
                if len(rows) > 2000:
                    break
            book.close()
        else:
            raise ReglaNegocioError("Use CSV UTF-8 o Excel XLSX.")
    except (UnicodeError, ValueError, StopIteration, BadZipFile, KeyError):
        raise ReglaNegocioError("No se pudo leer el archivo.")
    if len(rows) > 2000:
        raise ReglaNegocioError("Máximo 2000 filas por archivo.")
    if not set(COLUMNAS[tipo]).issubset(set(headers)):
        raise ReglaNegocioError("Faltan columnas. Descargue la plantilla del módulo.")
    return [
        {k: str(v or "").strip() for k, v in r.items() if k in COLUMNAS[tipo]}
        for r in rows
        if any(r.values())
    ]


def procesar(usuario, tipo, r):
    validar_puede_operar(usuario.empresa)
    if tipo == "colmenas":
        existing = Colmena.objects.filter(
            empresa=usuario.empresa, nombre=r["nombre"], archivada=False
        ).first()
        if existing:
            if existing.ubicacion != r["ubicacion"] or existing.estado != r["estado"]:
                raise ReglaNegocioError(
                    "Colmena existente con información diferente; edítela desde el módulo."
                )
            return False
        crear_colmena(usuario, r["nombre"], r["ubicacion"], r["estado"] or "Estable")
        return True
    if tipo == "sensores":
        existing = usuario.empresa.sensores.filter(codigo=r["codigo"]).first()
        if existing:
            if existing.tipo != r["tipo"]:
                raise ReglaNegocioError("Código de sensor existente con otro tipo.")
            return False
        from apps.integraciones.services import encolar

        sensor = registrar_sensor(usuario, **r)
        encolar(sensor, "REGISTER")
        return True
    a = (
        SensorColmena.objects.select_related("sensor", "colmena")
        .filter(pk=r["asociacion"], empresa=usuario.empresa)
        .first()
        if r["asociacion"].isdigit()
        else None
    )
    if not a:
        raise ReglaNegocioError("Asociación inexistente o de otra empresa.")
    if str(a.colmena.numero or a.colmena.numero_historico) != r["colmena"]:
        raise ReglaNegocioError("El número de colmena no corresponde a la asociación.")
    fecha = parse_datetime(r["fecha"])
    if not fecha or timezone.is_naive(fecha):
        raise ReglaNegocioError(
            "Fecha requerida en ISO 8601 con zona horaria, ej. 2026-11-21T10:00:00-05:00."
        )
    if r["origen"] != ("SIMULADO" if a.sensor.es_simulado else "REAL"):
        raise ReglaNegocioError("Origen incompatible con el sensor.")
    from apps.monitoreo.services import _a_decimal
    from apps.apiario.models import Sensor

    # Serializa importaciones del mismo dispositivo y normaliza fecha/decimal.
    Sensor.objects.select_for_update().get(pk=a.sensor_id)
    valor = _a_decimal(r["valor"])
    if r["tipo"] != a.sensor.tipo or r["unidad"] != UNIDADES[a.sensor.tipo]:
        raise ReglaNegocioError("Tipo o unidad incompatible con el sensor.")
    evento = f"{a.sensor_id}|{fecha.astimezone(dt_timezone.utc).isoformat()}"
    identity = hashlib.sha256(evento.encode()).hexdigest()
    m = Medicion.objects.filter(sensor=a.sensor, fecha=fecha).first()
    new = False
    if m:
        if (
            m.valor != valor
            or m.asociacion_id != a.pk
            or m.tipo_medicion != r["tipo"]
            or m.unidad != r["unidad"]
        ):
            raise ReglaNegocioError(
                "Ya existe una lectura en ese instante con datos diferentes; no se sobrescribe."
            )
    else:
        m, new = registrar_medicion(
            a.sensor,
            valor,
            fecha,
            tipo=r["tipo"],
            unidad=r["unidad"],
            id_externo="imp-" + identity,
            fuente=Medicion.FUENTE_IMPORT,
        )
    if m.asociacion_id != a.pk:
        raise ReglaNegocioError("La fecha está fuera de la asociación histórica.")
    nueva_produccion = False
    if r["produccion"]:
        cantidad = Decimal(r["produccion"])
        if not cantidad.is_finite() or cantidad <= 0 or cantidad > 9999999:
            raise ReglaNegocioError("Producción inválida.")
        # Una cosecha importada por colmena/instante, aunque la fila se repita en
        # varios sensores. La lectura ya existente no impide incorporar cosecha.
        ref = hashlib.sha256(
            f"{a.colmena_id}|{fecha.astimezone(dt_timezone.utc).isoformat()}".encode()
        ).hexdigest()
        produccion = ProduccionMiel.objects.filter(referencia_importacion=ref).first()
        if produccion:
            if (
                produccion.cantidad != cantidad
                or produccion.observaciones != r["observaciones"]
            ):
                raise ReglaNegocioError(
                    "Cosecha ya importada con datos diferentes; no se sobrescribe."
                )
        else:
            ProduccionMiel.objects.create(
                empresa=usuario.empresa,
                colmena=a.colmena,
                cantidad=cantidad,
                fecha=timezone.localtime(fecha).date(),
                observaciones=r["observaciones"],
                referencia_importacion=ref,
            )
            nueva_produccion = True
    return new or nueva_produccion


def _validar_limite_colmenas(usuario, filas):
    """
    Verifica que la importación no exceda el límite de colmenas del plan.
    Lanza ReglaNegocioError con mensaje descriptivo si se excede.
    Solo considera filas que crearían colmenas nuevas (no duplicados).
    """
    from apps.empresas.services import limites
    max_col, _ = limites(usuario.empresa)
    if max_col <= 0:
        return  # Plan sin límite explícito (0 = ilimitado)
    actuales = usuario.empresa.colmenas.filter(archivada=False).count()
    nombres_en_archivo = {r["nombre"] for r in filas if r.get("nombre")}
    existentes_en_bd = set(
        usuario.empresa.colmenas.filter(
            archivada=False, nombre__in=nombres_en_archivo
        ).values_list("nombre", flat=True)
    )
    nuevas_estimadas = len(nombres_en_archivo - existentes_en_bd)
    if actuales + nuevas_estimadas > max_col:
        exceso = actuales + nuevas_estimadas - max_col
        disponibles = max(0, max_col - actuales)
        raise ReglaNegocioError(
            f"Importación cancelada. Tu suscripción permite un máximo de {max_col} colmenas. "
            f"Actualmente tienes {actuales} registradas e intentas importar {nuevas_estimadas} "
            f"nuevas, lo que excedería tu límite en {exceso} "
            f"{'colmena' if exceso == 1 else 'colmenas'}. "
            f"Puedes importar un máximo de {disponibles} colmenas adicionales."
        )


def previsualizar(usuario, archivo, tipo):
    exigir(usuario, P.IMPORTACIONES_EJECUTAR, usuario.empresa_id)
    if tipo in ("colmenas", "sensores"):
        exigir(
            usuario,
            P.COLMENAS_GESTIONAR if tipo == "colmenas" else P.SENSORES_GESTIONAR,
            usuario.empresa_id,
        )
    filas = leer(archivo, tipo)
    # Validar límite de suscripción ANTES del dry-run (sólo para colmenas)
    if tipo == "colmenas":
        _validar_limite_colmenas(usuario, filas)
    errores = []
    # Dry run completo: verifica las mismas reglas sin guardar datos.
    with transaction.atomic():
        for i, r in enumerate(filas, 2):
            try:
                with transaction.atomic():
                    procesar(usuario, tipo, r)
            except Exception as e:
                if isinstance(e, ReglaNegocioError):
                    msg = str(e)
                else:
                    msg = (
                        "Formato o relación inválida. Revise fechas, valores y campos."
                    )
                errores.append({"fila": i, "error": msg})
        transaction.set_rollback(True)
    return LoteImportacion.objects.create(
        empresa=usuario.empresa,
        usuario=usuario,
        nombre=Path(archivo.name).name[:200],
        tipo=tipo,
        filas=filas,
        errores=errores,
    )


@transaction.atomic
def confirmar(usuario, lote):
    exigir(usuario, P.IMPORTACIONES_EJECUTAR, usuario.empresa_id)
    lote = LoteImportacion.objects.select_for_update().get(
        pk=lote.pk, empresa=usuario.empresa, usuario=usuario
    )
    if lote.confirmado:
        raise ReglaNegocioError("Esta importación ya fue confirmada.")
    if lote.tipo in ("colmenas", "sensores"):
        exigir(
            usuario,
            P.COLMENAS_GESTIONAR if lote.tipo == "colmenas" else P.SENSORES_GESTIONAR,
            usuario.empresa_id,
        )
    validar_puede_operar(usuario.empresa)
    # Re-validar límite en el momento de confirmar (defensa en profundidad)
    if lote.tipo == "colmenas":
        _validar_limite_colmenas(usuario, lote.filas)
    created = duplicados = 0
    errores = []
    for i, r in enumerate(lote.filas, 2):
        try:
            with transaction.atomic():
                new = procesar(usuario, lote.tipo, r)
            created += int(new)
            duplicados += int(not new)
        except Exception as e:
            errores.append(
                {
                    "fila": i,
                    "error": (
                        str(e)
                        if isinstance(e, ReglaNegocioError)
                        else "Datos inválidos o relación modificada."
                    ),
                }
            )
    lote.confirmado = True
    lote.errores = errores
    lote.resultado = {
        "creadas": created,
        "duplicadas": duplicados,
        "rechazadas": len(errores),
    }
    lote.save()
    registrar(
        "IMPORTACION_CONFIRMADA", "importaciones", lote, str(lote.resultado), usuario
    )
    return lote
