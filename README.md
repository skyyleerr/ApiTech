# ApiTech MK6 · Python y Django

Migración del sistema apícola MK5 de Java/Spring Boot. Conserva su identidad visual, módulos empresariales, gestión de dispositivos, monitoreo, producción, suscripciones y reportes. Añade detalle por colmena, numeración independiente por empresa, importación persistente y conexión configurable al servicio externo de sensores.

**Empieza por [Ejecutar](Ejecutar)**: instrucciones completas para Windows CMD, instalación rápida, MariaDB/MySQL, traslado MK5, respaldos y solución de problemas.

## Inicio local

```bat
py -3.13 -m venv .venv
.venv\Scripts\activate
python -m pip install -r requirements.txt
copy .env.example .env
```

Genera tu clave con `python -c "from django.core.management.utils import get_random_secret_key; print(get_random_secret_key())"` y ponla en `.env`. Deja `DB_ENGINE=sqlite` para explorar sin servidor SQL.

```bat
python manage.py migrate
python manage.py cargar_demo --password "Demo-Apitech-2026!"
python manage.py runserver
```

Abre http://127.0.0.1:8000/. Cliente demo: NIT `900123456-7`, correo `admin@demo-cliente.invalid`; interno: `admin.apitech@demo.invalid`. Contraseña del ejemplo: `Demo-Apitech-2026!`. Son cuentas ficticias de uso local. Para una base real, omite la carga demo y sigue la instalación/migración de `Ejecutar`.

## Cambios principales

- Dashboard general y detalle con sensores, gráficos, lecturas, producción y alertas de **cada colmena**.
- Empresa obtenida de la sesión; validaciones de relaciones y restricciones SQL.
- Número de colmena por empresa reutilizable al archivar; identidad interna e historial permanentes.
- CSV/XLSX con vista previa, revalidación, lotes, detección de repetidos y errores descargables.
- Exportación CSV/XLSX/PDF, filtros multicriterio y estadísticas.
- Pagos **simulados**, revisión, renovación sin duplicaciones, estados y vigencias.
- Integración externa con órdenes persistentes, idempotencia, reintentos y cursor por sensor.

Django **no genera sensores artificiales periódicamente**. La API de Karen debe implementar el contrato y operar externamente. La opción de conexión está implementada; la prueba con su servicio real requiere su URL y credenciales. Los datos demo se cargan una sola vez y están marcados como tales.

## Documentación

| Archivo | Contenido |
|---|---|
| [Requisitos_Historias_MK6.md](Requisitos_Historias_MK6.md) | 49 HU, 51 RF y 19 RNF activos; cambios respecto a MK5. |
| [docs/Trazabilidad_MK5_MK6.md](docs/Trazabilidad_MK5_MK6.md) | Correspondencia de las 43 historias originales. |
| [docs/Contrato_API_Karen.md](docs/Contrato_API_Karen.md) | Protocolo implementado, ejemplos y pruebas que faltan con el proveedor. |
| [docs/Mensaje_para_Karen.md](docs/Mensaje_para_Karen.md) | Texto para trabajar con su propio ChatGPT. |
| [docs/Manual_Usuario.md](docs/Manual_Usuario.md) | Flujo de cada rol y módulo. |
| [docs/Arquitectura_Manual_Tecnico.md](docs/Arquitectura_Manual_Tecnico.md) | Estructura, servicios, despliegue y diagramas. |
| [docs/Modelo_Datos.md](docs/Modelo_Datos.md) | Cambios SQL, identidades, origen y migración. |
| [docs/Casos_Uso_MK6.md](docs/Casos_Uso_MK6.md) | Casos de uso y diagramas de secuencia/estados. |
| [docs/Permisos.md](docs/Permisos.md) | Matriz única de autorización. |
| [docs/Pruebas.md](docs/Pruebas.md) | Validaciones ejecutadas y límites. |
| [docs/Equipo_y_Backlog.md](docs/Equipo_y_Backlog.md) | Responsables, entregables y aceptación de tareas. |
| [database/schema_mk6.sql](database/schema_mk6.sql) | Estructura completa y semillas para base vacía. |

## Verificación

```bat
python manage.py check
python manage.py makemigrations --check --dry-run
python manage.py test tests
```

Python 3.12/3.13; Django 5.2.19. Destino SQL: MySQL 8.0.16+ o MariaDB 10.5+; comprobado con MariaDB 11.8.6. SQLite sirve para la demostración local. No se certifica XAMPP con MariaDB 10.4. No se necesita Java ni Node para ejecutar.
