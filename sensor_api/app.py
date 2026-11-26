"""Servicio independiente de sensores artificiales ApiTech MK6 v1."""
from contextlib import asynccontextmanager
from datetime import datetime, timezone, timedelta
from decimal import Decimal, InvalidOperation
import hashlib, json, os, secrets, threading, time

from fastapi import FastAPI, Header, HTTPException, Query, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, ConfigDict, Field, field_validator
from sqlalchemy import Boolean, DateTime, Integer, Numeric, String, Text, UniqueConstraint, create_engine, select
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, Session, sessionmaker
from .providers import MockAPIProvider, MockarooProvider

DB_URL = os.getenv("SENSOR_API_DATABASE_URL", "sqlite:///./sensor_api.sqlite3")
GENERATOR_INTERVAL = max(5, int(os.getenv("SENSOR_API_GENERATOR_INTERVAL", "60")))
ALLOW_HTTP = os.getenv("SENSOR_API_ALLOW_HTTP", "false").lower() == "true"

class Base(DeclarativeBase): pass

class Company(Base):
    __tablename__ = "companies"
    id: Mapped[str] = mapped_column(String(100), primary_key=True)
    token_hash: Mapped[str] = mapped_column(String(128), nullable=False)
    active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))

class Sensor(Base):
    __tablename__ = "sensors"
    id: Mapped[str] = mapped_column(String(100), primary_key=True)
    company_id: Mapped[str] = mapped_column(String(100), nullable=False, index=True)
    client_ref: Mapped[str] = mapped_column(String(100), nullable=False)
    sensor_code: Mapped[str] = mapped_column(String(100), nullable=False)
    type: Mapped[str] = mapped_column(String(20), nullable=False)
    unit: Mapped[str] = mapped_column(String(10), nullable=False)
    origin: Mapped[str] = mapped_column(String(20), nullable=False)
    status: Mapped[str] = mapped_column(String(10), default="INACTIVE", nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    __table_args__ = (UniqueConstraint("company_id", "client_ref"), UniqueConstraint("company_id", "sensor_code"))

class Association(Base):
    __tablename__ = "associations"
    id: Mapped[str] = mapped_column(String(100), primary_key=True)
    sensor_id: Mapped[str] = mapped_column(String(100), nullable=False, index=True)
    company_id: Mapped[str] = mapped_column(String(100), nullable=False, index=True)
    hive_id: Mapped[str] = mapped_column(String(100), nullable=False)
    effective_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    ended_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)

class Reading(Base):
    __tablename__ = "readings"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    reading_id: Mapped[str] = mapped_column(String(100), unique=True, nullable=False)
    company_id: Mapped[str] = mapped_column(String(100), nullable=False, index=True)
    sensor_id: Mapped[str] = mapped_column(String(100), nullable=False, index=True)
    association_id: Mapped[str] = mapped_column(String(100), nullable=False)
    hive_id: Mapped[str] = mapped_column(String(100), nullable=False)
    type: Mapped[str] = mapped_column(String(20), nullable=False)
    value: Mapped[Decimal] = mapped_column(Numeric(10, 3), nullable=False)
    unit: Mapped[str] = mapped_column(String(10), nullable=False)
    origin: Mapped[str] = mapped_column(String(20), nullable=False)
    measured_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)

class Idempotency(Base):
    __tablename__ = "idempotency_keys"
    key: Mapped[str] = mapped_column(String(255), primary_key=True)
    company_id: Mapped[str] = mapped_column(String(100), nullable=False)
    fingerprint: Mapped[str] = mapped_column(String(64), nullable=False)
    response_json: Mapped[str] = mapped_column(Text, nullable=False)
    status_code: Mapped[int] = mapped_column(Integer, nullable=False)

engine = create_engine(DB_URL, connect_args={"check_same_thread": False} if DB_URL.startswith("sqlite") else {})
SessionLocal = sessionmaker(bind=engine, expire_on_commit=False)

UNITS = {"TEMPERATURA": "°C", "HUMEDAD": "%", "PESO": "kg"}

class RegisterIn(BaseModel):
    client_ref: str = Field(max_length=100)
    company_id: str = Field(max_length=100)
    sensor_code: str = Field(max_length=100)
    type: str
    unit: str
    origin: str

class AssociationIn(BaseModel):
    company_id: str = Field(max_length=100)
    association_id: str = Field(max_length=100)
    hive_id: str = Field(max_length=100)
    effective_at: datetime

    @field_validator("effective_at")
    @classmethod
    def aware(cls, v):
        if v.tzinfo is None: raise ValueError("effective_at requiere zona horaria")
        return v.astimezone(timezone.utc)

class ReadingOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    reading_id: str; company_id: str; sensor_id: str; association_id: str; hive_id: str
    type: str; value: str; unit: str; origin: str; measured_at: str

class ReadingsOut(BaseModel):
    items: list[ReadingOut]; next_cursor: str | None; has_more: bool


def utcnow(): return datetime.now(timezone.utc)
def digest(value): return hashlib.sha256(json.dumps(value, sort_keys=True, default=str).encode()).hexdigest()
def token_digest(token): return hashlib.sha256(token.encode()).hexdigest()
def json_response(sensor, status="INACTIVE"):
    return {"company_id": sensor.company_id, "sensor_id": sensor.id, "status": status}
def require_company(db, authorization, header_company):
    if not authorization or not authorization.startswith("Bearer ") or not header_company:
        raise HTTPException(401, "Autenticación requerida")
    company = db.get(Company, str(header_company))
    if not company or not company.active or not secrets.compare_digest(company.token_hash, token_digest(authorization[7:])):
        raise HTTPException(401, "Credenciales inválidas")
    return company

def idem(db, key, company, payload):
    if not key or len(key) > 255: raise HTTPException(400, "Idempotency-Key requerido")
    found = db.get(Idempotency, key)
    fp = digest(payload)
    if found:
        if found.company_id != company.id or found.fingerprint != fp: raise HTTPException(409, "Idempotency-Key reutilizada con cuerpo incompatible")
        return JSONResponse(json.loads(found.response_json), status_code=200)
    return fp

def save_idem(db, key, company, fp, response, status):
    db.add(Idempotency(key=key, company_id=company.id, fingerprint=fp, response_json=json.dumps(response), status_code=status))

def sensor_or_404(db, sensor_id, company):
    sensor = db.get(Sensor, sensor_id)
    if not sensor or sensor.company_id != company.id: raise HTTPException(404, "Sensor inexistente")
    return sensor

def validate_register(p):
    if p.type not in UNITS or p.unit != UNITS[p.type] or p.origin not in ("SIMULATED", "REAL"): raise HTTPException(400, "Tipo, unidad u origen inválido")

def validate_value(sensor, value):
    try: d = Decimal(str(value))
    except (InvalidOperation, ValueError): raise HTTPException(400, "Valor inválido")
    if not d.is_finite() or d.as_tuple().exponent < -3 or abs(d) >= Decimal("10000000"): raise HTTPException(400, "Valor fuera de rango")
    if sensor.type == "HUMEDAD" and not Decimal("0") <= d <= Decimal("100"): raise HTTPException(400, "Humedad fuera de rango")
    if sensor.type == "PESO" and d < 0: raise HTTPException(400, "Peso negativo")
    return d

def generate_value(sensor):
    if False and mockaroo.enabled:
        return validate_value(sensor, mockaroo.value(sensor.type))
    # Fallback local demostrativo cuando aún no se han configurado proveedores.
    n = int(time.time() // GENERATOR_INTERVAL)
    if sensor.type == "TEMPERATURA": return Decimal("30") + Decimal((n + len(sensor.id)) % 40) / 10
    if sensor.type == "HUMEDAD": return Decimal("45") + Decimal((n + len(sensor.id)) % 300) / 10
    return Decimal("10") + Decimal((n + len(sensor.id)) % 500) / 10


def generator_loop(stop):
    while not stop.wait(GENERATOR_INTERVAL):
        db = SessionLocal()
        try:
            sensors = db.scalars(
                select(Sensor).where(Sensor.status == "ACTIVE")
            ).all()

            for sensor in sensors:
                try:
                    assoc = db.scalar(
                        select(Association).where(
                            Association.sensor_id == sensor.id,
                            Association.ended_at.is_(None)
                        )
                    )

                    if not assoc:
                        continue

                    measured = utcnow()
                    effective_at = assoc.effective_at

                    if effective_at.tzinfo is None:
                        effective_at = effective_at.replace(
                            tzinfo=timezone.utc
                        )

                    if measured < effective_at:
                        continue

                    reading = Reading(
                        reading_id=f"reading-{secrets.token_urlsafe(12)}",
                        company_id=sensor.company_id,
                        sensor_id=sensor.id,
                        association_id=assoc.id,
                        hive_id=assoc.hive_id,
                        type=sensor.type,
                        value=validate_value(
                            sensor, generate_value(sensor)
                        ),
                        unit=sensor.unit,
                        origin=sensor.origin,
                        measured_at=measured
                    )

                    db.add(reading)
                    db.commit()
                    db.refresh(reading)

                    if mockapi.enabled:
                        try:
                            mockapi.persist_reading(reading)
                        except Exception as exc:
                            print(
                                f"ERROR MOCKAPI {sensor.id}: {exc}",
                                flush=True
                            )

                    print(
                        f"LECTURA GENERADA {sensor.id}: {reading.value}",
                        flush=True
                    )

                except Exception as exc:
                    db.rollback()
                    print(
                        f"ERROR GENERADOR {sensor.id}: {exc}",
                        flush=True
                    )

        except Exception as exc:
            db.rollback()
            print(f"ERROR CICLO GENERADOR: {exc}", flush=True)

        finally:
            db.close()

stop_event = threading.Event(); generator_thread = None
mockaroo = MockarooProvider(); mockapi = MockAPIProvider()
@asynccontextmanager
async def lifespan(app):
    global generator_thread
    Base.metadata.create_all(engine)
    generator_thread = threading.Thread(target=generator_loop, args=(stop_event,), daemon=True, name="sensor-generator"); generator_thread.start()
    yield
    stop_event.set()

app = FastAPI(title="ApiTech Sensores API", version="1.0.0", lifespan=lifespan)

@app.exception_handler(HTTPException)
async def http_error(request: Request, exc: HTTPException): return JSONResponse({"error": exc.detail}, status_code=exc.status_code)

@app.post("/sensors")
def register(p: RegisterIn, authorization: str | None = Header(None), x_company_id: str | None = Header(None), idempotency_key: str | None = Header(None)):
    validate_register(p); db = SessionLocal()
    try:
        company = require_company(db, authorization, x_company_id)
        if p.company_id != company.id: raise HTTPException(403, "Empresa no autorizada")
        payload = p.model_dump(); fp_or_response = idem(db, idempotency_key, company, payload)
        if isinstance(fp_or_response, JSONResponse): return fp_or_response
        sensor = db.scalar(select(Sensor).where(Sensor.company_id == company.id, Sensor.client_ref == p.client_ref))
        if sensor:
            if sensor.sensor_code != p.sensor_code or sensor.type != p.type or sensor.unit != p.unit or sensor.origin != p.origin: raise HTTPException(409, "Identidad de sensor incompatible")
            response, status = json_response(sensor), 200
        else:
            sensor = Sensor(id=f"sensor-{secrets.token_urlsafe(10)}", company_id=company.id, client_ref=p.client_ref, sensor_code=p.sensor_code, type=p.type, unit=p.unit, origin=p.origin)
            db.add(sensor); db.flush(); response, status = json_response(sensor), 201
        save_idem(db, idempotency_key, company, fp_or_response, response, status); db.commit(); return JSONResponse(response, status_code=status)
    finally: db.close()

@app.post("/sensors/{sensor_id}/activate")
def activate(sensor_id: str, p: AssociationIn, authorization: str | None = Header(None), x_company_id: str | None = Header(None), idempotency_key: str | None = Header(None)):
    return change_state(sensor_id, p, True, authorization, x_company_id, idempotency_key)

@app.post("/sensors/{sensor_id}/deactivate")
def deactivate(sensor_id: str, p: AssociationIn, authorization: str | None = Header(None), x_company_id: str | None = Header(None), idempotency_key: str | None = Header(None)):
    return change_state(sensor_id, p, False, authorization, x_company_id, idempotency_key)

def change_state(sensor_id, p, activate_flag, authorization, x_company_id, idempotency_key):
    db = SessionLocal()
    try:
        company = require_company(db, authorization, x_company_id); sensor = sensor_or_404(db, sensor_id, company)
        if p.company_id != company.id: raise HTTPException(403, "Empresa no autorizada")
        payload = p.model_dump(); payload["action"] = "activate" if activate_flag else "deactivate"
        fp_or_response = idem(db, idempotency_key, company, payload)
        if isinstance(fp_or_response, JSONResponse): return fp_or_response
        current = db.scalar(select(Association).where(Association.sensor_id == sensor.id, Association.ended_at.is_(None)))
        if activate_flag:
            if current and current.id != p.association_id: raise HTTPException(409, "El sensor ya tiene una asociación activa")
            if current and current.id == p.association_id: response, status = json_response(sensor, "ACTIVE"), 200
            else:
                assoc = Association(id=p.association_id, sensor_id=sensor.id, company_id=company.id, hive_id=p.hive_id, effective_at=p.effective_at)
                db.add(assoc); sensor.status = "ACTIVE"; db.flush(); response, status = json_response(sensor, "ACTIVE"), 201
        else:
            if current and current.id != p.association_id: raise HTTPException(409, "La asociación activa no coincide")
            if current: current.ended_at = utcnow()
            sensor.status = "INACTIVE"; db.flush(); response, status = json_response(sensor, "INACTIVE"), 200
        save_idem(db, idempotency_key, company, fp_or_response, response, status); db.commit(); return JSONResponse(response, status_code=status)
    finally: db.close()

@app.get("/readings", response_model=ReadingsOut)
def readings(sensor_id: str = Query(...), after: str | None = None, limit: int = Query(100, ge=1, le=100), authorization: str | None = Header(None), x_company_id: str | None = Header(None)):
    db = SessionLocal()
    try:
        company = require_company(db, authorization, x_company_id); sensor = sensor_or_404(db, sensor_id, company)
        try: cursor = int(after) if after else 0
        except ValueError: raise HTTPException(400, "Cursor inválido")
        if cursor < 0: raise HTTPException(400, "Cursor inválido")
        rows = db.scalars(select(Reading).where(Reading.sensor_id == sensor.id, Reading.id > cursor).order_by(Reading.id).limit(limit + 1)).all()
        has_more = len(rows) > limit; rows = rows[:limit]
        items = [ReadingOut(reading_id=r.reading_id, company_id=r.company_id, sensor_id=r.sensor_id, association_id=r.association_id, hive_id=r.hive_id, type=r.type, value=f"{r.value:.3f}", unit=r.unit, origin=r.origin, measured_at=(r.measured_at.replace(tzinfo=timezone.utc) if r.measured_at.tzinfo is None else r.measured_at).astimezone(timezone.utc).isoformat().replace("+00:00", "Z")) for r in rows]
        return ReadingsOut(items=items, next_cursor=str(rows[-1].id) if rows else None, has_more=has_more)
    finally: db.close()

@app.get("/health")
def health(): return {"status": "ok", "service": "apitech-sensors", "version": "1.0.0"}

def create_company(company_id: str):
    """CLI helper: returns a new token once; only its hash is persisted."""
    token = secrets.token_urlsafe(32); db = SessionLocal()
    try:
        if db.get(Company, company_id): raise ValueError("La empresa ya existe")
        db.add(Company(id=str(company_id), token_hash=token_digest(token))); db.commit(); return token
    finally: db.close()

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("sensor_api.app:app", host=os.getenv("SENSOR_API_HOST", "127.0.0.1"), port=int(os.getenv("SENSOR_API_PORT", "8080")), reload=False)
