"""Crea una empresa y muestra su token una sola vez.
Uso: python -m sensor_api.create_company 2
"""
import sys
from .app import Base, engine, create_company

if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("Uso: python -m sensor_api.create_company <company_id>")
    Base.metadata.create_all(engine)
    print(create_company(sys.argv[1]))
