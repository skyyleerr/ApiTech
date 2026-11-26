# Equipo, backlog y evidencia de entrega

Este documento propone cómo revisar y cerrar el trabajo en el Project existente. No modifica issues ni presupone que su estado en GitHub sea este. El código y la documentación del ZIP sirven como base para revisión por los tres integrantes.

## Objetivos

**Product Goal:** entregar ApiTech MK6 en Django conservando la identidad y funciones del MK5, con datos aislados por empresa, monitoreo identificable por colmena, importaciones confiables, reportes y una integración externa verificable de sensores.

**Sprint Goal 1, 19–23 de noviembre:** validar alcance/documentación, disponer del núcleo Django y modelo migrable y acordar/probar el contrato de sensores con persistencia aislada.

**Sprint Goal 2, 23–25 de noviembre:** completar integración real con la API de Karen, revisar importación/exportación/pagos, probar flujos integrados y preparar la demostración y documentación final antes del 26.

El día 23 es el punto de transición: cerrar review del sprint 1 antes de iniciar tareas del sprint 2. No asignar el mismo tramo horario a ambos. Los rangos solicitados son iteraciones cortas; no se describen como semanas completas.

Todo debe quedar revisado el **miércoles 25**. Pre sustentación: **jueves 26 de noviembre**. **Sprint 3 solo si piden correcciones**, del 26 después de recibirlas al 29; sustentación oficial: **lunes 30**. Si no hay correcciones, no se inventa otro sprint de desarrollo.

## Responsabilidades

- **Juan:** integración de la migración Django, reglas comunes, frontend, aislamiento y revisión final de modelo/migraciones.
- **Karen:** servicio externo y generador artificial, autenticación empresarial, identidad/paginación/idempotencia y prueba real Django–API.
- **Anthony:** revisión funcional de importación/exportación y pagos/suscripciones, pruebas de casos límite y documentación de esos módulos. Puede implementar ajustes de BD relacionados, pero coordinados con Juan mediante migraciones revisadas; no editar dos esquemas separados.
- **Los tres:** contrastar HU/RF/RNF, casos de uso, arquitectura, UML, manuales, instalación y evidencia con lo que realmente funciona.

## Dieciséis issues revisados

| # | Título recomendado | Responsable | Sprint | Criterio de cierre |
|---|---|---|---|---|
| 1 | Validar alcance MK6 y versionar HU, RF y RNF | Juan; revisión de los tres | 1 | Las 43 HU tienen tratamiento y las nuevas tienen aceptación; documentos subidos al repositorio; sin actor Sistema ni simulador interno. |
| 2 | Revisar modelo MK5 y verificar traslado a la base MK6 | Juan; Anthony revisa | 1 | Copia del origen intacta, dry-run y traslado correctos, conteos/PK/hashes comprobados; SQL nuevo restaurable. |
| 3 | Migrar acceso, sesiones y permisos a Django | Juan | 1 | Cuatro roles; acceso por NIT/correo; pruebas de aislamiento, CSRF y elevación denegada. |
| 4 | Migrar gestión de empresas, colmenas y sensores | Juan | 1 | CRUD operativo, cupos, archivo histórico y número visible por empresa sin cruces. |
| 5 | Acordar contrato REST de sensores y credenciales por empresa | Karen; Juan revisa | 1 | Contrato versionado; IDs, unidades, fechas, errores y paginación definidos con ejemplos ejecutables. |
| 6 | Implementar registro y control de sensores en el servicio externo | Karen | 1 | Registro inactivo, activación/detención idempotentes, autenticación de empresa y generador externo persistente. |
| 7 | Generar y consultar lecturas externas con identidad e historial | Karen | 1 | Tres tipos, reading_id estable, asociación histórica y cursor por sensor; sin mezcla entre empresas. |
| 8 | Integrar Django con las órdenes y lecturas de la API | Karen; Juan apoya | 1–2 | Prueba real de punta a punta; token fuera del navegador; página atómica y datos en la colmena correcta. |
| 9 | Revisar reglas de planes, pagos y vigencias | Anthony | 1 | Estados y límites documentados; pago simulado diferenciado; casos de rechazo, renovación y doble envío acordados. |
| 10 | Validar portal de pagos y renovaciones de MK6 | Anthony | 2 | Flujo de alta/solicitud/revisión completo, importe del servidor, una activa por empresa e historial preservado. |
| 11 | Validar dashboard y detalle exclusivo por colmena | Juan | 2 | Gráficos con origen/unidad/fecha, resumen sin valores inventados, filtro por empresa y colmena demostrado. |
| 12 | Revisar importación y exportación sin duplicar datos | Anthony | 2 | Preview, confirmación, errores por fila, datos existentes, cosechas y CSV/XLSX/PDF verificados. |
| 13 | Validar reportes con filtros y estadísticas | Juan; Anthony revisa archivos | 2 | Filtros combinados iguales en consulta/exportación, estadísticas por tipo y producción separada de peso. |
| 14 | Probar caída, reintentos, reasignación e identidad de la API | Karen | 2 | Servicio real detenido y recuperado; sin duplicación; empresa ajena rechazada; archivo/reutilización mantiene histórico. |
| 15 | Completar documentación, UML y manuales de los módulos | Karen y Anthony; Juan integra | 2 | Arquitectura, casos de uso, modelo/diagramas, manuales, requisitos, contrato y limitaciones coinciden con código. |
| 16 | Ejecutar prueba integrada e instalar desde el ZIP final | Juan coordina; participan todos | 2 | Instalación en equipo de entrega, prueba con dos empresas, respaldo/restauración, evidencia y demostración preparadas el 25. |

Al cerrar un issue adjuntar: PR/commit, criterio verificado y evidencia (salida de pruebas o demostración). El consumidor HTTP con mocks no permite cerrar por sí solo los issues 6, 7, 8 y 14 como integración externa terminada. No se debe marcar una captura de datos demo como sensor externo operativo.

## Documentación a revisar/subir

`Requisitos_Historias_MK6.md`, trazabilidad, matriz de permisos, arquitectura/manual técnico, modelo de datos, casos de uso y UML, manual de usuario, contrato API, guía `Ejecutar`, pruebas, changelog y reglas de migración/respaldos. Añadir evidencia de la API real cuando exista, identificando versión y fecha de prueba. La negociación tecnológica sigue fuera de la entrega actual.
