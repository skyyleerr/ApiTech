# Verificación de ApiTech MK6

Entrega del 21 de noviembre de 2026. Resultados obtenidos sobre el código de este paquete. Las pruebas usan datos ficticios y bases independientes; no se ejecutaron contra una base productiva del equipo.

## Resultados ejecutados

| Comprobación | Resultado observado |
|---|---|
| Instalación desde base SQLite vacía | Todas las migraciones aplicadas; `check` sin incidencias. |
| Coherencia entre modelos y migraciones | `makemigrations --check --dry-run`: sin cambios pendientes. |
| Suite automatizada, SQLite | **94 pruebas aprobadas**, 72,428 segundos. |
| Misma suite, MariaDB 11.8.6 | **94 pruebas aprobadas**, 72,832 segundos. |
| SQL entregado | Restaurado en otra base vacía: **31 tablas y 22 triggers**; semillas coincidentes, `migrate` sin cambios y `check` sin incidencias. |
| Traslado MK5 | Validación con reversión y aplicación definitiva sobre datos sintéticos: conteos, identidades, fechas y registros comprobados; origen intacto. |
| Contraseñas heredadas | Inicio de sesión con BCrypt del formato MK5 y actualización a PBKDF2 comprobados. |
| Navegación cliente | Dashboard y 14 rutas de módulos respondieron correctamente. Sin errores JavaScript capturados. |
| Formularios en navegador | Alta de colmena; alta de sensor y asociación con orden externa pendiente; guardado de umbral; registro de producción. |
| API aún sin configurar | Mensaje controlado; el sistema conserva datos y órdenes pendientes. |
| Presentación | Inspección del dashboard y detalle por colmena; comprobación a 1440 px y 390 px en Chromium. |

Entorno utilizado: Linux, Python 3.12.14, Django 5.2.19, SQLite y MariaDB 11.8.6. La guía de Windows CMD se proporciona para la instalación del equipo; no se afirma haber ejecutado estas pruebas en Windows. MySQL 8 tiene soporte configurado, pero el servidor SQL efectivamente probado fue MariaDB.

Los registros de ejecución están en [evidencias/](evidencias/): `sqlite.txt`, `mariadb_y_traslado.txt` y `navegador.txt`. La línea `Forbidden (CSRF cookie not set.): /login/` corresponde a una prueba negativa que verifica el rechazo de una solicitud sin CSRF; la suite finaliza con `OK`.

## Qué comprueba la suite

| Área | Casos relevantes | Archivo |
|---|---|---|
| Autenticación | Cuatro roles, NIT correcto, cuentas inactivas, contraseña incorrecta, logout POST, rutas protegidas y BCrypt. | `tests/test_auth.py` |
| Separación empresarial y permisos | URLs ajenas, empresa manipulada en formularios, asociación cruzada, escalamiento de rol, límites de plan, empresa pausada y suscripción vencida. | `tests/test_permisos_aislamiento.py` |
| Lecturas y alertas | Identidad y unidades, duplicados, valores inválidos, intervalo de monitoreo, reasignación histórica, umbrales, alertas y destinatarios de la empresa correcta. | `tests/test_mediciones.py` |
| Suscripciones y pagos | Solicitudes válidas e inválidas, aprobaciones atómicas, precio calculado en servidor, doble confirmación, rechazo y única suscripción activa. | `tests/test_suscripciones.py` |
| Funciones MK6 | Numeración por empresa, archivo y conservación de historial, detalle de colmena, importación, filtros/exportaciones, renovación y formularios. | `tests/test_mk6.py` |
| Resistencia de integración e importación | Restricciones SQL, timeout/reintentos, idempotencia, redirects rechazados, lectura incompleta, órdenes pendientes, suspensión, importación sin duplicar lecturas/cosechas y números seguros en Excel. | `tests/test_integracion_resistencia.py` |

Las respuestas del proveedor se sustituyen por respuestas controladas en las pruebas de integración. Esto verifica el consumidor Django y sus fallos previstos; no certifica un servicio externo que Karen todavía no ha entregado.

## Prueba del traslado antiguo

Se usaron las definiciones de tablas del `database/schema.sql` aportado con MK5 y se insertaron datos sintéticos de dos empresas, usuarios, suscripciones, dispositivos, lecturas y entidades relacionadas. Se verificaron los conteos de las 18 tablas de entidades trasladadas. La relación de roles y permisos se reconstruye intencionalmente con la matriz MK6.

Primero se ejecutó el importador sin `--aplicar`, comprobando la reversión; luego se aplicó, se cotejaron identidades y datos, y se comprobó el acceso con una contraseña BCrypt conocida. La copia del origen permaneció intacta. Se verificó también la conservación de `registro_afectado` en auditoría.

La copia de ensayo requirió adaptar el `ON DELETE SET NULL` de auditoría a `RESTRICT` por su combinación con un CHECK del DDL antiguo en MariaDB 11.8. Esto se documenta en [Modelo_Datos.md](Modelo_Datos.md). No se ejecutaron las instrucciones destructivas de administración presentes en el SQL antiguo.

Esta prueba no reemplaza la validación de un respaldo real de tu instalación. `Ejecutar` explica cómo hacer la copia, validar sin guardar y aplicar únicamente sobre un destino vacío.

## Repetir las comprobaciones

Con el entorno activado y `.env` configurado:

```bat
python manage.py check
python manage.py makemigrations --check --dry-run
python manage.py test tests
```

Las pruebas crean una base temporal. Para MariaDB/MySQL, concede al usuario de pruebas permisos sobre su base `test_apitech_mk6`, siguiendo `Ejecutar`. No reutilices una base operativa como base de pruebas.

Para verificar manualmente la interfaz, carga los datos demo en una instalación independiente y entra con cada rol. Prueba además dos empresas: un objeto de la segunda nunca debe ser visible o editable desde la primera. Archiva una colmena y crea otra: puede reutilizar su número visible, pero debe tener una identidad interna distinta y el histórico anterior debe seguir perteneciendo a la colmena archivada.

## Pendientes que requieren el entorno definitivo

- **API de Karen:** servicio publicado, credenciales por empresa y prueba conjunta de registro, activación, generación externa, sincronización, detención, caída y recuperación. Contrato y criterios en [Contrato_API_Karen.md](Contrato_API_Karen.md).
- **Pagos:** el flujo implementado es simulado con revisión administrativa. No se efectuaron cobros bancarios ni existe una pasarela real certificada en esta entrega.
- **Datos reales:** ejecutar la validación sobre una copia íntegra del MK5 operativo y cotejar una muestra antes de adoptar MK6.
- **Despliegue:** configurar y probar el servidor elegido, HTTPS, respaldos, procesos programados y credenciales. `runserver` solo es para desarrollo.
- **No certificados en este ensayo:** pruebas de carga prolongadas, concurrencia masiva, disponibilidad productiva, accesibilidad exhaustiva, Safari/Firefox y ejecución nativa en Windows. Los RNF asociados siguen siendo objetivos que deben medirse en el entorno final.

No se presenta el resultado de las 94 pruebas como garantía de ausencia de errores. Constituye evidencia concreta de los escenarios anteriores sobre la versión incluida en el ZIP.
