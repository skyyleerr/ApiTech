# Manual de usuario — ApiTech MK6

## Acceso y roles

Abre la portada. El equipo ApiTech entra con correo y contraseña por su acceso interno. Un cliente selecciona el acceso de empresa, introduce su NIT y después sus credenciales. Los datos disponibles corresponden a la empresa de esa cuenta; cambiar el NIT no concede acceso a otra empresa.

El menú muestra las opciones de tu rol. Admin_ApiTech administra la plataforma; Empleado_ApiTech consulta y revisa pagos/solicitudes; Admin_Cliente administra su empresa; Empleado_Cliente consulta, importa históricos, exporta y resuelve alertas. Véase la matriz completa en `Permisos.md`.

## Panel del equipo ApiTech

1. **Solicitudes:** revisa el alta enviada desde Planes. Aprobar exige correo y contraseña inicial del administrador cliente. La aprobación crea empresa, administrador, suscripción y pago simulado verificado en una transacción. Rechazar conserva solicitud y motivo.
2. **Empresas:** consulta por nombre, crea o edita información y registra el administrador inicial si aún no existe. NIT es único.
3. **Pagos:** revisa pendientes; verifica o rechaza. Un pago ya procesado no se vuelve a verificar. Los importes proceden del plan, no de un campo libre del cliente.
4. **Suscripciones:** consulta historial, suspende/cancela o reactiva una vigente que tenga pago verificado. No puede haber dos activas para una empresa.
5. **Usuarios ApiTech:** crea o desactiva personal interno. No se asigna empresa a estas cuentas.
6. **Planes:** ajusta precio, duración y cupos. Cambiar el plan afecta futuras operaciones; revisa el impacto antes de modificar un plan ya utilizado.
7. **Auditoría:** consulta acciones por módulo o texto. No contiene contraseñas ni tokens.

Pausar una cuenta conserva consultas y cierra sus monitoreos, dejando órdenes de detención para la API. La vigencia de suscripción se valida aparte. Reactivar la empresa no inicia automáticamente los sensores.

## Dashboard cliente

Muestra cantidad de colmenas, sensores, alertas y plan vigente. Temperatura, humedad y peso resumen la última lectura de cada sensor con datos de los últimos cinco minutos. Se indica unidad, cantidad de sensores, origen y última fecha. Un guion significa falta de datos recientes; no equivale a cero.

Los gráficos consultan registros guardados y se actualizan cada 30 segundos. Eso no activa un generador ni sustituye el proceso que consulta la API. El peso del sensor corresponde a la colmena; la miel cosechada se registra en Producción.

Los datos de `cargar_demo` son fijos y ficticios. El resumen puede marcarlos desactualizados mientras el gráfico histórico sigue mostrando sus cinco lecturas por sensor.

## Colmenas y detalle individual

En **Colmenas**, busca por nombre/ubicación, filtra estado y abre **Ver colmena**. El detalle contiene solo sus lecturas, sensores/asociaciones, estadísticas, alertas y cosechas. Puedes filtrar fechas, horas, tipo y origen. La empresa y colmena se validan también en el servidor.

El número visible comienza desde 1 para cada empresa. Eliminar del listado activo archiva: libera el número y conserva el historial bajo una identidad interna distinta. Ejemplo: eliminas colmena 1 y creas otra; la nueva puede llamarse número 1, pero tendrá un detalle y lecturas independientes. Consulta las archivadas desde el enlace correspondiente del listado. No se renumeran las que siguen activas.

Crear y editar exige rol Admin_Cliente y una suscripción vigente con cupo disponible. El nombre y ubicación son propios de la empresa. El cambio de estado de salud no elimina información.

## Sensores, asociaciones y monitoreo

1. En **Sensores → Nuevo**, registra código, tipo, modelo y fabricante. El código es único dentro de tu empresa. Se crea una orden de registro pendiente.
2. Asócialo con una colmena propia que no esté archivada.
3. Pulsa **Iniciar monitoreo**. La colmena debe estar Estable, el sensor activo y el plan vigente.
4. En **Conectar API externa**, habilita la conexión ya configurada por el equipo técnico y pulsa sincronizar.
5. Revisa estado de sincronización y lecturas en el detalle de la colmena.

Un sensor tiene una asociación activa a la vez. No es obligatorio tener simultáneamente los tres tipos para iniciar uno. Para moverlo, detén el monitoreo, desasocia y crea una nueva asociación. Las lecturas antiguas permanecen con su colmena anterior.

La pantalla distingue el estado local deseado y la confirmación de la API. Si falla el proveedor, se conservan las órdenes para reintentar. Desactivar sensor conserva sus datos. La edición permite cambiar modelo/fabricante, no el tipo ni la empresa de un dispositivo con identidad histórica.

Para desconectar planificadamente: detener sensores, sincronizar las detenciones y luego deshabilitar la conexión. Deshabilitar solo el consumo no apaga por sí mismo el servicio externo.

## Alertas, umbrales y avisos

Configura en **Umbrales** el mínimo/máximo por tipo. Debe cumplirse mínimo menor que máximo. Los umbrales globales incluidos son ejemplos de demostración y deben revisarse para la operación real de cada apiario; no son asesoría apícola.

Una lectura válida fuera del rango produce alerta y notificaciones para usuarios activos de la misma empresa. No se duplica otra alerta activa del mismo tipo en esa colmena. **Resolver** registra usuario de sesión, fecha y estado; después otra lectura fuera de rango puede generar una nueva alerta.

La campana abre tus notificaciones. Puedes marcarlas individualmente o todas como leídas. No muestra notificaciones de otro usuario, aunque sea de la misma empresa.

## Producción y reportes

En **Producción**, el administrador registra kg cosechados, fecha, colmena y observaciones. Filtra por período/colmena para ver total, promedio por colmena con cosechas, último registro y tabla. Exporta CSV, Excel o PDF.

En **Reportes**, combina fechas/horas, colmena, sensor, tipo y origen. Las estadísticas cuentan registros y calculan mínimo, máximo y promedio dentro del filtro, separados por tipo/unidad. El promedio estadístico del histórico es diferente del promedio de últimas lecturas recientes del dashboard.

Las exportaciones conservan el alcance empresarial y filtros. Máximo 10.000 filas por exportación: divide períodos mayores. Los gráficos muestran hasta 120 puntos por sensor, no todos los registros de una exportación grande. Excel conserva valores numéricos para el análisis; los textos que podrían interpretarse como fórmulas se neutralizan.

## Importar sin mezclar lo existente

Descarga la plantilla de **Histórico**, **Colmenas** o **Sensores**. Usa CSV UTF-8 separado por comas o XLSX. Máximo 5 MB, 2000 filas y 25 MB descomprimidos para XLSX.

| Plantilla | Columnas obligatorias |
|---|---|
| Histórico | fecha, colmena, asociacion, tipo, valor, unidad, origen, produccion, observaciones |
| Colmenas | nombre, ubicacion, estado |
| Sensores | codigo, tipo, modelo, fabricante |

Todas las columnas deben existir aunque producción/observaciones estén vacías. Ejemplo de fecha: `2026-11-21T10:00:00-05:00`. Temperatura usa `°C`, humedad `%`, peso `kg`; los decimales utilizan punto. El número de colmena es el visible; la asociación es la identidad estable indicada en el detalle. Ambas deben corresponder a la misma relación de tu empresa y al instante de lectura.

1. Selecciona tipo y archivo.
2. Revisa vista previa y errores. Esta fase no agrega colmenas, sensores, mediciones ni cosechas.
3. Confirma. Las reglas se comprueban otra vez por si cambió el plan, una asociación u otro dato.
4. Revisa conteos: filas con datos incorporados, repetidas y rechazadas; descarga los errores.
5. Corrige el archivo y presenta un lote nuevo. Un lote ya confirmado no se vuelve a ejecutar.

Una lectura del mismo sensor e instante ya existente se conserva si coincide; si difiere se rechaza, nunca se sobrescribe silenciosamente. Las fechas equivalentes en zonas distintas y los decimales `26`, `26.0` y `26.000` se normalizan. Puedes incorporar una cosecha junto a una lectura existente; la cosecha importada se identifica por colmena/instante y no se vuelve a añadir al repetir filas. Una cosecha repetida con cantidad u observaciones distintas se rechaza para revisión.

No se crean automáticamente asociaciones al importar históricos. Regístralas y valida sus intervalos primero. Para históricos reales anteriores al inicio de MK6 utiliza el traslado MK5; no cambies fechas para fingir que el sensor ya estaba asociado.

## Pagos y suscripción cliente

Admin_Cliente solicita un pago de un plan y consulta pendientes/verificados/rechazados. Empleado_Cliente solo consulta. El portal es **simulado**, como el MK5: no cobra dinero ni integra una entidad bancaria. La solicitud pública conserva únicamente cuatro dígitos de una tarjeta ficticia; no uses una tarjeta real.

Verificar una renovación del mismo plan conserva el tiempo restante y crea una nueva suscripción, manteniendo el historial de la anterior. Un cambio de plan mientras el actual está vigente no se verifica hasta terminar esa vigencia. Los límites se obtienen del plan vigente.

## Perfil, contraseña y salida

Desde el avatar abre Perfil. Cambia nombre/apellido/correo o usa el formulario de contraseña con tu clave actual. El perfil no cambia rol ni empresa. Usa **Cerrar sesión** para terminar; es una operación protegida por CSRF.

Ante problemas de instalación o conexión consulta `../Ejecutar`. Ante errores de una fila revisa primero asociación, fecha y unidad; no intentes corregirlos cambiando la empresa del registro.
