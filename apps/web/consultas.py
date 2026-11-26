from datetime import datetime, time, timedelta
from django.utils import timezone
from django.db.models import Avg, Min, Max, Count, Sum
from apps.apiario.models import Colmena, Sensor
from apps.monitoreo.models import Medicion
from apps.comun.errors import ReglaNegocioError


def filtrar(qs, params, colmena_field="asociacion__colmena_id", fecha_field="fecha"):
    for key, field in [("colmena", colmena_field), ("sensor", "sensor_id")]:
        if params.get(key) and field:
            try:
                valor = int(params[key])
            except (ValueError, TypeError):
                raise ReglaNegocioError("Filtro de identificación inválido.")
            qs = qs.filter(**{field: valor})
    for key in ["desde", "hasta"]:
        if params.get(key):
            try:
                d = datetime.strptime(params[key], "%Y-%m-%d").date()
            except ValueError:
                raise ReglaNegocioError("Fecha de filtro inválida.")
            hora = params.get(key + "_hora")
            try:
                h = datetime.strptime(hora, "%H:%M").time() if hora else time.min
            except ValueError:
                raise ReglaNegocioError("Hora de filtro inválida.")
            dt = timezone.make_aware(datetime.combine(d, h))
            limite = (
                dt
                if key == "desde"
                else dt + (timedelta(minutes=1) if hora else timedelta(days=1))
            )
            qs = qs.filter(
                **{fecha_field + ("__gte" if key == "desde" else "__lt"): limite}
            )
    if (
        params.get("desde")
        and params.get("hasta")
        and params["desde"] > params["hasta"]
    ):
        raise ReglaNegocioError("El inicio del período debe ser anterior al final.")
    if params.get("tipo"):
        qs = qs.filter(tipo_medicion=params["tipo"])
    if params.get("origen"):
        qs = qs.filter(origen=params["origen"])
    return qs


def lecturas(empresa):
    return Medicion.objects.filter(asociacion__empresa=empresa).select_related(
        "asociacion__colmena", "sensor"
    )


def resumen(empresa, colmena=None):
    sensores = Sensor.objects.filter(empresa=empresa)
    if colmena:
        sensores = sensores.filter(
            asociaciones__colmena=colmena, asociaciones__activo=True
        ).distinct()
    else:
        sensores = sensores.filter(
            asociaciones__activo=True, asociaciones__colmena__archivada=False
        ).distinct()
    resultado = []
    for tipo, unidad in [("TEMPERATURA", "°C"), ("HUMEDAD", "%"), ("PESO", "kg")]:
        ultimas = []
        for s in sensores.filter(tipo=tipo):
            q = s.mediciones.filter(asociacion__activo=True)
            if colmena:
                q = q.filter(asociacion__colmena=colmena)
            m = q.order_by("-fecha", "-pk").first()
            if m:
                ultimas.append(m)
        recientes = [
            m for m in ultimas if m.fecha >= timezone.now() - timedelta(minutes=5)
        ]
        # Solo datos recientes para el resumen actual. No rellenar faltantes con cero.
        valor = sum(m.valor for m in recientes) / len(recientes) if recientes else None
        resultado.append(
            {
                "tipo": tipo,
                "unidad": unidad,
                "valor": valor,
                "muestras": len(recientes),
                "total": len(ultimas),
                "origenes": " / ".join(sorted({m.origen for m in ultimas})),
                "fecha": max((m.fecha for m in ultimas), default=None),
            }
        )
    return resultado


def estadisticas(qs):
    return list(
        qs.values("tipo_medicion", "unidad")
        .annotate(
            cantidad=Count("id"),
            minimo=Min("valor"),
            maximo=Max("valor"),
            promedio=Avg("valor"),
        )
        .order_by("tipo_medicion")
    )
