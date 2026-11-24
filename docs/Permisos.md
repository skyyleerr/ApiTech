# Matriz de permisos MK6

Fuente ejecutable: `apps/usuarios/permisos.py`. Las tablas se sincronizan al ejecutar `migrate`. No se obtienen permisos de campos enviados por el navegador. No hay un rol Sistema ni acceso por ese nombre.

| Operación | Admin_ApiTech | Empleado_ApiTech | Admin_Cliente | Empleado_Cliente |
|---|---|---|---|---|
| Panel administrativo, empresas, pagos globales, solicitudes, auditoría | Sí | Sí | No | No |
| Crear/editar empresas, pausa/reactivación, planes y estados de suscripción | Sí | No | No | No |
| Revisar solicitudes y verificar/rechazar pagos | Sí | Sí | No | No |
| Gestionar usuarios internos | Sí | No | No | No |
| Ver dashboard, colmenas, sensores e históricos propios | No | No | Sí | Sí |
| Crear/editar/archivar colmenas | No | No | Sí | No |
| Configurar sensores, asociaciones, monitoreo, umbrales y conexión | No | No | Sí | No |
| Gestionar usuarios de la propia empresa | No | No | Sí | No |
| Consultar producción | No | No | Sí | Sí |
| Registrar cosecha desde formulario | No | No | Sí | No |
| Importar histórico de mediciones/producción | No | No | Sí | Sí |
| Carga inicial de colmenas/sensores | No | No | Sí | No |
| Reportes, exportación y resolución de alertas | No | No | Sí | Sí |
| Consultar pagos de la empresa | No | No | Sí | Sí |
| Solicitar pago/renovación de la empresa | No | No | Sí | No |
| Perfil, contraseña, notificaciones propias y ayuda | Sí | Sí | Sí | Sí |

Los RF/HU originales tenían contradicciones sobre «Empleado», edición de sensores y revisión de pagos. Aquí se resuelven de forma explícita: el empleado interno conserva la revisión operativa del MK5; el empleado cliente conserva importación histórica, exportación y resolución, pero no administra identidades, dispositivos o suscripciones. No existe un permiso general de edición derivado del acceso al listado.

La empresa pausada o sin suscripción vigente conserva consulta y acceso al portal de pagos. Las modificaciones de negocio/importaciones/lecturas se bloquean. El perfil personal y la lectura de notificaciones siguen disponibles. Reactivar la cuenta y renovar suscripción son operaciones distintas.

Los usuarios internos no navegan datos de monitoreo de empresas desde rutas cliente. Su acceso global está limitado a los módulos administrativos. Las URL con PK internas no son secretos ni reemplazan los controles de autorización.
