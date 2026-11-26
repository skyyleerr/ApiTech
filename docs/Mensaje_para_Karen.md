# Mensaje para Karen y su ChatGPT

Karen, ya hay un consumidor Django en ApiTech MK6. Tu responsabilidad es desarrollar el servicio REST externo de sensores artificiales y hacer la prueba conjunta de conexión. Usa este texto con tu ChatGPT y adjunta `docs/Contrato_API_Karen.md` y `apps/integraciones/services.py`:

> Necesito desarrollar la API externa de sensores de ApiTech MK6. No debes modificar Django para generar datos aleatorios dentro del sistema. Tu aplicación debe ejecutarse como servicio independiente, con persistencia propia y un proceso de generación periódico.
>
> Cada empresa tiene sus sensores de TEMPERATURA, HUMEDAD y PESO. Al registrar un sensor por REST, lo guardas inicialmente inactivo. Al recibir una activación con company_id, hive_id y association_id, comienza a producir lecturas artificiales con fecha, unidad, valor, origen SIMULATED e identidad permanente. Al detenerlo, deja de generar, pero conserva los datos anteriores.
>
> Implementa exactamente el contrato adjunto MK6 v1: POST /sensors, POST /sensors/{id}/activate, POST /sensors/{id}/deactivate y GET /readings con cursor. Sigue sus nombres de campos, valores y códigos HTTP. Django ya consume ese contrato; no lo cambies unilateralmente.
>
> Valida el token Bearer y su empresa en cada operación. No confíes únicamente en company_id enviado por el cliente. No permitas consultar ni activar sensores ajenos. Código de sensor puede repetirse entre empresas, pero no dentro de la misma. Usa PK internas/UUID estables: hive_id es la PK de Django, no el número visible de colmena, que puede reutilizarse.
>
> Implementa Idempotency-Key para registro/activación/detención y reading_id estable por lectura. Una petición repetida no crea otro sensor ni otra tarea generadora. Las páginas de lecturas usan un cursor estable por sensor y conservan la asociación histórica. No mezcles datos de empresas ni reasignes lecturas antiguas al mover un sensor.
>
> Antes de escribir código, lee todo el contrato y explica la estructura del servicio, persistencia, autenticación y generador. Después entrega el proyecto completo, migraciones/SQL, .env.example sin secretos, comandos CMD, pruebas y OpenAPI. Usa Python con un framework adecuado y una tarea periódica supervisada. Si empleas Mockaroo o MockAPI, explica y desarrolla los componentes adicionales: esas herramientas por sí solas no resuelven autenticación empresarial, generación continua y activación dinámica.
>
> Genera escenarios de prueba con dos empresas, tres tipos por empresa, caída y recuperación, lecturas repetidas, paginación, reasignación y eliminación lógica de colmenas. Los valores son SIMULADOS, no mediciones reales ni recomendaciones apícolas. No uses los IDs de ejemplo como constantes globales.
>
> Finalmente indícame la URL base, cómo crear un token por empresa y cómo mantener el proceso externo en marcha. Realizaremos con Juan la prueba desde la pantalla “Conectar API externa” de Django y el comando sincronizar_sensores. Solo entonces marcaremos como terminada la integración real.

Para conectarlo, entrega a Juan la URL real y las credenciales por un canal privado; en el repositorio sube ejemplos sin tokens. Actualiza el documento de arquitectura, despliegue, casos de uso, HU028/HU046, RF025/RF043/RF044 y pruebas. Anthony revisa importación/exportación/pagos y su documentación; coordinen las migraciones de BD antes de modificar modelos compartidos.
