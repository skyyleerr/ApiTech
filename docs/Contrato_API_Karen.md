# Contrato de integración de sensores — MK6 v1

Este es el contrato **implementado por el consumidor Django**. Es la propuesta concreta para Karen; todavía no se ha probado contra su servicio real. Si cambia las rutas o formatos, ambos repositorios deben versionar el contrato y adaptar `apps/integraciones/services.py` y sus pruebas antes de integrar.

## Responsabilidades

Karen mantiene una aplicación externa, con su propia persistencia y proceso de generación. Cada sensor artificial se registra con una empresa única y, al activarse para una asociación, empieza a producir lecturas de temperatura, humedad o peso. Detener un sensor frena la generación futura y conserva sus lecturas anteriores.

Django registra las entidades locales, envía órdenes REST y consulta las lecturas; no genera datos periódicos. Un archivo de Mockaroo y un CRUD en MockAPI no bastan por sí solos para ejecutar el ciclo periódico, autenticar por empresa y garantizar idempotencia. Si se utilizan, el servicio externo debe aportar esas reglas y su programador de tareas.

No hay conexión directa de la API a la BD Django. Los intercambios usan identificadores, JSON y credenciales. Un usuario cliente nunca recibe el token del proveedor en HTML o JavaScript.

## Transporte e identidad

- Base URL configurable, por ejemplo `https://sensores.example/v1` (ejemplo, no servicio publicado).
- Cabeceras: `Authorization: Bearer <token>` y `X-Company-ID: <id interno empresa MK6>`.
- Token exclusivo por empresa recomendado y soportado. El servidor externo autentica el token y valida su empresa; **no confía únicamente en el encabezado ni en company_id del JSON**.
- HTTPS remoto obligatorio; HTTP solo localhost/127.0.0.1 con habilitación explícita para desarrollo.
- JSON UTF-8. Fechas ISO 8601 con zona; preferentemente UTC, por ejemplo `2026-11-21T15:00:00Z`.
- IDs serializados como cadenas. Máximo 100 caracteres para sensor_id, reading_id y cursor.
- `company_id`: PK inmutable de Empresa Django, no NIT.
- `hive_id`: PK interna de Colmena Django, **no el número visible reutilizable**.
- `association_id`: PK de SensorColmena; cambia al reasignar el dispositivo.
- `sensor_id`: identidad asignada por la API; `client_ref`: PK del sensor Django.
- Tipos de este contrato: `TEMPERATURA`, `HUMEDAD`, `PESO`. Unidades exactas: `°C`, `%`, `kg`.
- Origen remoto: `SIMULATED` o `REAL`; Django almacena `SIMULADO` o `REAL`.

La API puede usar UUID o enteros internos. Debe guardar explícitamente la correspondencia con company_id, client_ref, hive_id y association_id de MK6. Los números 1, 2 y 3 de una empresa nunca identifican por sí solos sus registros ante la API.

## 1. Registrar sensor

`POST /sensors`

Cabecera adicional: `Idempotency-Key: registro-<company_id>-<client_ref>`.

```json
{
  "client_ref": "8",
  "company_id": "2",
  "sensor_code": "TEMP-001",
  "type": "TEMPERATURA",
  "unit": "°C",
  "origin": "SIMULATED"
}
```

Respuesta 200 (repetición) o 201 (nuevo):

```json
{"company_id":"2","sensor_id":"sensor-72","status":"INACTIVE"}
```

El sensor se registra **inactivo**: aún no puede producir lecturas sin una asociación. La pareja empresa/client_ref y la clave idempotente deben devolver siempre el mismo sensor. Repetir la petición por un timeout nunca crea un segundo sensor. Una clave reutilizada con un cuerpo incompatible debe devolver 409. Django comprueba empresa e identidad de la respuesta.

## 2. Activar para una asociación

`POST /sensors/{sensor_id}/activate`

`Idempotency-Key`: UUID persistido de la orden Django. Cuerpo:

```json
{
  "company_id": "2",
  "association_id": "12",
  "hive_id": "9",
  "effective_at": "2026-11-21T15:00:00+00:00"
}
```

Respuesta 200 o 201:

```json
{"company_id":"2","sensor_id":"sensor-72","status":"ACTIVE"}
```

Debe validar propiedad del sensor, guardar la asociación y comenzar generación periódica **a partir de la activación efectiva**; no fabricar lecturas anteriores al registro o al inicio de la asociación. Si llega una orden atrasada, no reconstruir ficticiamente horas pasadas. La frecuencia la define Karen; 30–60 segundos es adecuada para la demostración, no una obligación de medición física.

Si la misma orden llega otra vez, devolver el mismo resultado sin crear otra tarea generadora. Un sensor no puede generar para dos asociaciones activas a la vez. Las lecturas ya generadas conservan su asociación original.

## 3. Detener

`POST /sensors/{sensor_id}/deactivate`, mismo cuerpo y cabecera que activar.

Respuesta:

```json
{"company_id":"2","sensor_id":"sensor-72","status":"INACTIVE"}
```

Debe detener la generación futura, conservar el historial y ser idempotente. Django cierra su intervalo al registrar la acción y encola esta orden. Si la API está caída, la interfaz muestra el estado pendiente/error; el siguiente ciclo vuelve a enviarla. Una lectura generada después del cierre local no se incorpora como si el monitoreo siguiera activo.

Deshabilitar la conexión en la pantalla detiene el consumo local; no equivale a detener físicamente el servicio externo. Para una desconexión planificada, detener sensores, sincronizar las órdenes y después deshabilitar la conexión.

## 4. Consultar lecturas

`GET /readings?sensor_id=sensor-72&after=<cursor>&limit=100`

El cursor vacío inicia la consulta. La API devuelve solo lecturas del sensor autenticado y de su empresa, en orden estable de incorporación al servicio externo (no solamente por measured_at, para admitir lecturas recibidas fuera de orden).

```json
{
  "items": [
    {
      "reading_id": "lectura-00081",
      "company_id": "2",
      "sensor_id": "sensor-72",
      "association_id": "12",
      "hive_id": "9",
      "type": "TEMPERATURA",
      "value": "31.250",
      "unit": "°C",
      "origin": "SIMULATED",
      "measured_at": "2026-11-21T15:01:00Z"
    }
  ],
  "next_cursor": "pagina-81",
  "has_more": false
}
```

Todos los campos del ejemplo de lectura son obligatorios. El valor puede ser número JSON o cadena decimal. No se admite NaN, infinito, humedad fuera de 0–100 %, peso negativo ni valores de más de 7 dígitos enteros y 3 decimales. Las fechas futuras tienen una tolerancia de cinco minutos por diferencia de relojes.

Reglas de paginación:

- Hasta 100 lecturas por respuesta. `has_more` es booleano.
- Si hay avance, next_cursor es una cadena nueva de máximo 100 caracteres; no repite el cursor recibido.
- Sin lecturas nuevas: `{"items":[],"next_cursor":null,"has_more":false}`.
- `has_more=true` exige items no vacíos y next_cursor nuevo.
- Reading_id identifica permanentemente una lectura dentro del sensor. Repetir el mismo ID, fecha y valor es idempotente. Repetirlo con otro contenido es error, no actualización silenciosa.
- No se deben borrar páginas aún no consumidas ni invalidar cursores sin coordinar una estrategia de retención.
- Django procesa hasta 50 páginas por sensor y ciclo; el siguiente ciclo continúa desde el cursor guardado.

Cada página se valida en una transacción: si una fila es ajena o incompatible, se revierte **esa página** y su cursor no avanza. Las páginas anteriores ya confirmadas permanecen guardadas. Corregir la fila o el contrato y reintentar; no borrar el cursor para ocultar el problema.

## Códigos HTTP y errores

200/201 para respuestas exitosas con objeto JSON. 400 para formato inválido; 401/403 para autenticación o empresa; 404 para dispositivo inexistente/no autorizado; 409 para identidad/idempotencia incompatibles; 429 para límite temporal; 5xx para fallo del servicio. No redirigir solicitudes autenticadas a otro dominio. La API debe responder JSON aun en errores, sin exponer secretos.

Django utiliza timeout de 8 segundos, hasta tres intentos para timeout/conexión y 429/500/502/503/504, y esperas cortas de 0,25 y 0,5 s. Conserva la misma clave entre reintentos. Una interrupción tras un POST exitoso pero antes del commit se recupera mediante idempotencia del proveedor.

## Configuración y ejecución

```dotenv
SENSOR_API_URL_2=https://sensores.example/v1
SENSOR_API_TOKEN_2=token-de-empresa-2
```

```bat
python manage.py sincronizar_sensores
python manage.py sincronizar_sensores --continuo --intervalo 60
```

Hay que habilitar la conexión de esa empresa desde su sesión de Admin_Cliente. Se pueden compartir la URL base entre empresas; sus credenciales y verificaciones deben mantener el aislamiento. La configuración global SENSOR_API_BASE_URL/TOKEN es una alternativa para proveedores con una credencial de aplicación y autorización empresarial propia.

## Prueba conjunta obligatoria antes de marcar el issue como terminado

1. Preparar dos empresas y un sensor de cada tipo por empresa; pueden repetir sensor_code.
2. Registrar y activar desde Django; comprobar una sola entidad/tarea por sensor.
3. Consultar y mostrar lectura en dashboard y exclusivamente en la colmena correcta.
4. Repetir una página y una orden: sin duplicados ni nueva generación paralela.
5. Introducir empresa/sensor/asociación ajenos: rechazo y cursor sin avance de esa página.
6. Detener, reasignar, archivar y reutilizar número visible: histórico separado.
7. Interrumpir servicio, reiniciarlo y comprobar recuperación sin datos ficticios locales.
8. Comprobar ISO 8601, unidades, origen simulado, paginación y lectura fuera de orden.
9. Revisar que los tokens no aparecen en consola, capturas, navegador ni repositorio.
10. Registrar URL de versión, commit de ambos repositorios y evidencia de la prueba real.

Los tests del consumidor están en `tests/test_mk6.py` y `tests/test_integracion_resistencia.py`. Usan respuestas controladas; la API final de Karen no fue llamada en esta entrega.
