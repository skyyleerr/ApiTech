"""Adaptadores externos opcionales: Mockaroo genera valores y MockAPI.io los persiste."""
import os
from datetime import timezone
import requests

TIMEOUT = float(os.getenv("PROVIDER_HTTP_TIMEOUT", "8"))

class MockarooProvider:
    def __init__(self):
        self.key = os.getenv("MOCKAROO_API_KEY", "")
        self.schema = os.getenv("MOCKAROO_SCHEMA", "")
        self.url = os.getenv("MOCKAROO_URL", "https://api.mockaroo.com/api/generate.json")

    @property
    def enabled(self): return bool(self.key and self.schema)

    def value(self, sensor_type):
        if not self.enabled: return None
        field = {"TEMPERATURA": "temperature", "HUMEDAD": "humidity", "PESO": "weight"}[sensor_type]
        response = requests.post(self.url, params={"key": self.key, "schema": self.schema, "count": 1, "array": "true"}, timeout=TIMEOUT)
        response.raise_for_status()
        data = response.json()
        if isinstance(data, dict): data = [data]
        if not isinstance(data, list) or not data or field not in data[0]: raise ValueError(f"Mockaroo no devolvió el campo {field}")
        return data[0][field]

class MockAPIProvider:
    def __init__(self):
        self.base = os.getenv("MOCKAPI_BASE_URL", "").rstrip("/")
        self.resource = os.getenv("MOCKAPI_READINGS_RESOURCE", "readings")
        self.token = os.getenv("MOCKAPI_TOKEN", "")

    @property
    def enabled(self): return bool(self.base)

    def persist_reading(self, reading):
        if not self.enabled: return
        headers = {"Accept": "application/json", "Content-Type": "application/json"}
        if self.token: headers["X-API-Key"] = self.token
        payload = {"reading_id": reading.reading_id, "company_id": reading.company_id, "sensor_id": reading.sensor_id, "association_id": reading.association_id, "hive_id": reading.hive_id, "type": reading.type, "value": str(reading.value), "unit": reading.unit, "origin": reading.origin, "measured_at": reading.measured_at.astimezone(timezone.utc).isoformat().replace("+00:00", "Z")}
        response = requests.post(f"{self.base}/{self.resource}", json=payload, headers=headers, timeout=TIMEOUT)
        response.raise_for_status()
