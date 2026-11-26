import os
os.environ["SENSOR_API_DATABASE_URL"] = "sqlite:///./test_sensor_api.sqlite3"
os.environ["SENSOR_API_GENERATOR_INTERVAL"] = "5"

from fastapi.testclient import TestClient
from sensor_api.app import app, Base, engine, create_company

Base.metadata.drop_all(engine); Base.metadata.create_all(engine)
TOKEN_1 = create_company("1")
TOKEN_2 = create_company("2")
client = TestClient(app)

def headers(token, company, key=None):
    h = {"Authorization": f"Bearer {token}", "X-Company-ID": company}
    if key: h["Idempotency-Key"] = key
    return h

def test_register_is_idempotent_and_isolated():
    body = {"client_ref":"8", "company_id":"1", "sensor_code":"TEMP-001", "type":"TEMPERATURA", "unit":"°C", "origin":"SIMULATED"}
    r = client.post("/sensors", json=body, headers=headers(TOKEN_1, "1", "reg-1")); assert r.status_code == 201
    r2 = client.post("/sensors", json=body, headers=headers(TOKEN_1, "1", "reg-1")); assert r2.status_code == 200
    assert r.status_code == 201 and r2.status_code == 200
    assert r.json()["sensor_id"] == r2.json()["sensor_id"]
    body_2 = {**body, "company_id": "2"}
    assert client.post("/sensors", json=body_2, headers=headers(TOKEN_2, "2", "reg-2")).status_code == 201

def test_activate_read_and_deactivate():
    body = {"client_ref":"9", "company_id":"1", "sensor_code":"HUM-001", "type":"HUMEDAD", "unit":"%", "origin":"SIMULATED"}
    sensor = client.post("/sensors", json=body, headers=headers(TOKEN_1, "1", "reg-9")).json()["sensor_id"]
    assoc = {"company_id":"1", "association_id":"a-9", "hive_id":"h-9", "effective_at":"2026-11-21T15:00:00Z"}
    r = client.post(f"/sensors/{sensor}/activate", json=assoc, headers=headers(TOKEN_1, "1", "act-9")); assert r.status_code == 201
    assert client.get(f"/readings?sensor_id={sensor}", headers=headers(TOKEN_2, "2")).status_code == 404
    r = client.post(f"/sensors/{sensor}/deactivate", json=assoc, headers=headers(TOKEN_1, "1", "deact-9")); assert r.status_code == 200

def test_bad_token_and_foreign_company_are_rejected():
    assert client.get("/readings?sensor_id=missing", headers=headers("bad", "1")).status_code == 401
    body = {"client_ref":"x", "company_id":"2", "sensor_code":"X", "type":"PESO", "unit":"kg", "origin":"SIMULATED"}
    assert client.post("/sensors", json=body, headers=headers(TOKEN_1, "1", "foreign")).status_code == 403
