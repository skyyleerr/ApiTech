# ApiTech MK5 — Cambios finales

Incluye:
- Registro público con tarjeta de débito FALSA; solo se conservan los últimos 4 dígitos.
- Dos planes: Básico (20 colmenas/20 sensores) y Profesional (60/60).
- Límites de colmenas y sensores validados en backend.
- Gestión de empresas con búsqueda por nombre y activar/desactivar.
- Gestión de pagos basada en suscripciones, con pagos pendientes y total recibido verificado.
- Gestión de usuarios internos ApiTech.
- Menú lateral completo para el equipo ApiTech.
- Eliminada la opción visual "Vista cliente".
- Empresas bloqueadas pueden iniciar sesión, pero quedan pausadas y no pueden modificar datos ni generar simulaciones.
- Pagos del cliente en modo consulta: plan, beneficios, inicio y fin.
- Alertas con "Marcar completado"; dejan de aparecer entre las alertas activas y en la campana.
- Botones de regreso añadidos en las vistas principales.
- `database/schema.sql` actualizado y `database/migration_final.sql` para instalaciones existentes.

## Base de datos

Para una instalación nueva: ejecutar `database/schema.sql`.

Para una instalación existente: ejecutar `database/migration_final.sql` y verificar que el esquema de la aplicación corresponda con `schema.sql`.

## Ejecución

Desde la raíz:
`bash mvnw spring-boot:run`

En Windows:
`mvnw.cmd spring-boot:run`
