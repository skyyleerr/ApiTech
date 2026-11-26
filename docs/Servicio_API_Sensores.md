# Servicio externo de sensores ApiTech MK6 v1

`sensor_api` es un servicio independiente de Django. Usa FastAPI, SQLAlchemy y una base propia (SQLite por defecto; PostgreSQL/MySQL mediante `SENSOR_API_DATABASE_URL`). Django nunca comparte su base de datos ni genera lecturas periódicas.

## Inicio local

Desde la carpeta que contiene `manage.py`:

```bat
python -m venv .venv-api
.venv-api\Scripts\activate
pip install -r requirements-api.txt
copy sensor_api\.env.example .env
python -m sensor_api.create_company 2
python -m sensor_api.app
```

En Linux/macOS sustituir `copy` por `cp`. El comando de empresa imprime el token una sola vez: guardarlo en un gestor de secretos y nunca subirlo al repositorio. La URL local es `http://127.0.0.1:8080`; para Django se usa esa URL con `SENSOR_API_ALLOW_LOCAL=true` únicamente en desarrollo. En un entorno remoto se publica mediante HTTPS.

## Configurar Mockaroo y MockAPI.io

La API integra ambos servicios cuando sus variables están completas. Mockaroo genera el valor mediante `POST https://api.mockaroo.com/api/generate.json`; el schema debe tener los campos `temperature`, `humidity` y `weight`. En MockAPI.io crea un proyecto con un recurso REST `readings` y las propiedades `reading_id`, `company_id`, `sensor_id`, `association_id`, `hive_id`, `type`, `value`, `unit`, `origin` y `measured_at`.

Añade estos valores al entorno, sin subir claves al repositorio:

```dotenv
MOCKAROO_API_KEY=<clave-privada>
MOCKAROO_SCHEMA=<nombre-del-schema>
MOCKAROO_URL=https://api.mockaroo.com/api/generate.json
MOCKAPI_BASE_URL=https://<proyecto>.mockapi.io/api/v1
MOCKAPI_READINGS_RESOURCE=readings
MOCKAPI_TOKEN=
```

Cuando están completas, cada ciclo obtiene el valor de Mockaroo y persiste la lectura en MockAPI.io. Si todavía están vacías, se usa un fallback local para poder probar la aplicación antes de crear las cuentas.

## Diseño

- `companies` almacena el identificador de empresa y el hash SHA-256 del token; nunca el token en claro.
- `sensors` impone unicidad por `(company_id, client_ref)` y `(company_id, sensor_code)`.
- `associations` conserva el historial de asignaciones; `ended_at` cierra una asociación sin reasignar lecturas antiguas.
- `readings.id` es el orden de incorporación estable para cursores; `reading_id` es la identidad pública permanente.
- `idempotency_keys` guarda huella, respuesta y código para repetir registro, activación o detención sin crear entidades/tareas duplicadas.
- Un hilo supervisado por el ciclo de vida de FastAPI ejecuta un ciclo cada `SENSOR_API_GENERATOR_INTERVAL` segundos. Solo genera para sensores activos con asociación vigente y nunca reconstruye el pasado.

## Contrato

Implementa `POST /sensors`, `POST /sensors/{id}/activate`, `POST /sensors/{id}/deactivate` y `GET /readings`. Todas las operaciones exigen `Authorization: Bearer ...` y `X-Company-ID`. La documentación interactiva queda en `/docs` y el esquema OpenAPI en `/openapi.json`.

Para conectar Django:

```dotenv
SENSOR_API_URL_2=http://127.0.0.1:8080
SENSOR_API_TOKEN_2=<token-obtenido-privadamente>
SENSOR_API_ALLOW_LOCAL=true
```

Luego ejecutar en el proyecto Django `python manage.py sincronizar_sensores` o `python manage.py sincronizar_sensores --continuo --intervalo 60`.
