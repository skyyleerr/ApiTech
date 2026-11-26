# Trazabilidad de las 43 historias MK5

| HU | Título en el documento aportado | Decisión y título MK6 |
|---|---|---|
| HU-001 | Selección del tipo de acceso | Conservada → Seleccionar el acceso. |
| HU-002 | Inicio de sesión del equipo ApiTech | Ajustada: incluye ambos roles internos → Acceso del equipo ApiTech. |
| HU-003 | Validación del NIT empresarial | Ajustada: pausa permite consulta → Identificar la empresa por NIT. |
| HU-004 | Inicio de sesión de usuarios de empresa | Conservada → Acceso de usuarios de empresa. |
| HU-005 | Cierre de sesión | Conservada y reforzada → Cerrar sesión. |
| HU-006 | Dashboard administrativo ApiTech | Conservada → Dashboard administrativo. |
| HU-007 | Registro de empresas clientes | Conservada → Registrar empresas. |
| HU-008 | Creación del administrador inicial de empresa | Conservada → Crear administrador inicial. |
| HU-009 | Consulta de empresas registradas | Ajustada: lectura del empleado interno → Consultar empresas. |
| HU-010 | Consulta y verificación de pagos | Ajustada: ambos roles internos → Verificar o rechazar pagos. |
| HU-011 | Gestión de suscripciones | Ajustada: renovación e idempotencia → Gestionar suscripciones. |
| HU-012 | Gestión del estado de empresas | Ajustada: estados separados de suscripción → Pausar y reactivar cuentas. |
| HU-013 | Gestión de usuarios del equipo ApiTech | Conservada y reforzada → Gestionar equipo ApiTech. |
| HU-014 | Gestión de empleados | Ajustada: matriz explícita → Gestionar usuarios de mi empresa. |
| HU-015 | Consulta de información de perfil | Conservada → Actualizar mi perfil. |
| HU-016 | Cambio de contraseña | Conservada → Cambiar contraseña. |
| HU-017 | Registro de sensores simulados | Sustituye registro ligado al simulador interno → Registrar sensor para la API externa. |
| HU-018 | Consulta de sensores | Ajustada: identidad y sincronización → Consultar sensores. |
| HU-019 | Actualización de sensores | Ajustada: identidad estable → Editar sensor. |
| HU-020 | Desactivación de sensores | Ajustada: detención externa → Desactivar sensor. |
| HU-021 | Registro de colmenas | Ajustada: numeración por empresa → Registrar colmena. |
| HU-022 | Consulta de colmenas | Ampliada → Consultar colmenas. |
| HU-023 | Actualización de colmenas | Corregida: sin traslado entre empresas → Editar colmena. |
| HU-024 | Desactivación de colmenas | Sustituye baja ambigua por archivo lógico → Eliminar del listado activo conservando historial. |
| HU-025 | Asociación de sensores a colmenas | Ajustada: intervalos e integridad SQL → Asociar y desasociar sensores. |
| HU-026 | Validación de sensores para monitoreo | Aclarada: monitoreo por asociación → Validar condiciones del monitoreo. |
| HU-027 | Activación del monitoreo | Sustituye autoactivación del simulador → Solicitar activación y detención. |
| HU-028 | Generación automática de datos simulados | Sustituye HU del actor Sistema y simulador Java → Recibir lecturas externas identificadas. |
| HU-029 | Consulta de mediciones actuales | Corregida: resumen con procedencia temporal → Consultar resumen de mediciones. |
| HU-030 | Historial de mediciones | Ampliada → Consultar historial. |
| HU-031 | Generación automática de alertas | Actor beneficiario cliente; automatismo interno, sin rol Sistema → Recibir alertas por umbral. |
| HU-032 | Consulta de alertas | Conservada → Consultar alertas. |
| HU-033 | Gestión de alertas | Ampliada al flujo web → Resolver alertas. |
| HU-034 | Registro de producción de miel | Conservada → Registrar producción de miel. |
| HU-035 | Consulta de producción de miel | Conservada y ampliada → Consultar producción. |
| HU-036 | Generación de reportes | Ampliada → Generar reportes multicriterio. |
| HU-037 | Exportación de datos de mediciones | Ampliada → Exportar datos. |
| HU-038 | Importación de datos | Rehecha con lotes persistentes → Importar datos validados. |
| HU-039 | Resultado de importación | Ampliada → Consultar resultado de importación. |
| HU-040 | Descarga de plantillas | Ampliada → Descargar plantillas. |
| HU-041 | Gestión de pagos y suscripciones | Conservada y reforzada → Solicitar plan y consultar pagos. |
| HU-042 | Notificaciones de alertas de monitoreo | Ampliada a bandeja completa → Consultar notificaciones. |
| HU-043 | Centro de ayuda | Conservada → Consultar ayuda. |

No se eliminan silenciosamente historias del inventario: HU028 se reemplaza, HU027 y HU024 cambian de mecanismo y las demás se conservan o ajustan como se indica. HU044–HU049 son nuevas. La especificación normativa completa es `../Requisitos_Historias_MK6.md`.
