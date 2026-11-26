# ApiTech MK6 — Historias y requisitos finales

Fecha: 21 de noviembre de 2026. Versión implementada del entregable.

## Base y alcance

Se revisaron las 43 historias del documento aportado y el documento de RF/RNF del MK5. Se ignoraron sus secciones de control de versiones, como solicitaste. El inventario tenía 40 identificadores RF y dos entradas idénticas RF24; se unificaron. Se conservan los números originales para trazabilidad: esta especificación contiene 49 HU, 51 RF y 19 RNF activos (RNF005 se reclasifica como regla de RF001).

Se mantiene el producto apícola y se reemplaza el backend Java/Spring Boot por Django. La negociación de software (ítems 1–4 de la lista académica) sigue fuera de este alcance. El servicio que genera sensores artificiales es externo y lo desarrolla Karen; este ZIP implementa el consumidor, las órdenes y el contrato de conexión. La validación contra su servicio definitivo queda pendiente de URL, credenciales e implementación.

No hay actor ni cuenta «Sistema». Los actores humanos son Admin_ApiTech, Empleado_ApiTech, Admin_Cliente y Empleado_Cliente; API externa es actor de integración. «PROCESO» en auditoría describe una operación técnica, no un usuario ni un simulador.

## Decisiones sobre el MK5

| Elemento | Decisión MK6 |
|---|---|
| HU028 / RF25, simulación Java cada 30 s | Sustituida por servicio externo; Django nunca genera lecturas periódicas. |
| HU027 / RF22, autoactivación | Sustituida por orden explícita e idempotente; confirmación remota separada. |
| IDs visibles globales | Número por empresa reutilizable; PK e historial permanecen inmutables. |
| Borrado de colmena | Archivo lógico y liberación de número; no se borra evidencia histórica. |
| RF23 acepta empresa del DTO | Eliminado: empresa siempre derivada del usuario y del objeto. |
| Permisos contradictorios | Matriz única en docs/Permisos.md. Empleado cliente no configura dispositivos ni crea colmenas; sí importa históricos, exporta y resuelve alertas. |
| RF24 duplicado | Una única definición RF024. |
| RNF5 unicidad del NIT | Reclasificado a RF001; la restricción UNIQUE se conserva. |
| RNF16 código Java | Reescrito para Python/Django. |
| Pago ficticio MK5 | Se conserva como pago simulado con revisión, no se presenta como pasarela bancaria real. |
| Base de datos | Nueva BD MK6, basada en las entidades de database/schema.sql; nunca reemplazar el origen sin copia. |

## Historias de usuario

### HU-001 — Seleccionar el acceso

**Actor:** Visitante. **Tratamiento:** Conservada.

Como Visitante, quiero elegir el acceso del equipo ApiTech o de una empresa para seguir el proceso de autenticación que me corresponde.

**Aceptación:** La portada ofrece acceso interno y acceso cliente; cliente solicita NIT.

**Ubicación:** `/`.

### HU-002 — Acceso del equipo ApiTech

**Actor:** Personal ApiTech. **Tratamiento:** Ajustada: incluye ambos roles internos.

Como Personal ApiTech, quiero ingresar al panel administrativo con correo y contraseña para administrar la plataforma con mis permisos.

**Aceptación:** Solo cuentas internas activas ingresan al panel; credenciales inválidas no crean sesión.

**Ubicación:** `/login/`.

### HU-003 — Identificar la empresa por NIT

**Actor:** Usuario cliente. **Tratamiento:** Ajustada: pausa permite consulta.

Como Usuario cliente, quiero seleccionar mi empresa antes de autenticarme para identificar el ámbito empresarial de mi cuenta.

**Aceptación:** Un NIT inexistente muestra error; seleccionar otra empresa no permite autenticar mi cuenta. Una cuenta pausada puede consultar sus datos.

**Ubicación:** `/login/empresa/`.

### HU-004 — Acceso de usuarios de empresa

**Actor:** Usuario cliente. **Tratamiento:** Conservada.

Como Usuario cliente, quiero autenticarme dentro de mi empresa para consultar solo la información que me pertenece.

**Aceptación:** Se exige correo, contraseña y correspondencia con el NIT; el rol se obtiene del servidor.

**Ubicación:** `/login/`.

### HU-005 — Cerrar sesión

**Actor:** Usuario autenticado. **Tratamiento:** Conservada y reforzada.

Como Usuario autenticado, quiero finalizar mi sesión para evitar que otra persona continúe usando mi sesión.

**Aceptación:** POST con CSRF cierra la sesión; un GET no cierra sesiones.

**Ubicación:** `/logout/`.

### HU-006 — Dashboard administrativo

**Actor:** Personal ApiTech. **Tratamiento:** Conservada.

Como Personal ApiTech, quiero consultar el estado administrativo de la plataforma para priorizar la atención de cuentas y solicitudes.

**Aceptación:** Muestra empresas, pausadas, solicitudes, pagos pendientes y total verificado; un cliente recibe 403.

**Ubicación:** `/admin/dashboard/`.

### HU-007 — Registrar empresas

**Actor:** Admin_ApiTech. **Tratamiento:** Conservada.

Como Admin_ApiTech, quiero crear empresas con NIT único para incorporar clientes sin duplicar empresas.

**Aceptación:** Se validan campos y NIT duplicado; la empresa tiene identidad independiente de los números de colmena.

**Ubicación:** `/admin/empresas/nueva/`.

### HU-008 — Crear administrador inicial

**Actor:** Admin_ApiTech. **Tratamiento:** Conservada.

Como Admin_ApiTech, quiero habilitar el primer administrador cliente para permitir que la empresa empiece a gestionar sus usuarios.

**Aceptación:** Se restringe a Admin_Cliente de la empresa indicada; no se repite el alta inicial si ya existe administrador.

**Ubicación:** `/admin/empresas/<id>/administrador/`.

### HU-009 — Consultar empresas

**Actor:** Personal ApiTech. **Tratamiento:** Ajustada: lectura del empleado interno.

Como Personal ApiTech, quiero consultar las cuentas y suscripciones para conocer el estado de las cuentas atendidas.

**Aceptación:** El listado muestra NIT, estado y suscripciones; solo Admin_ApiTech modifica cuentas.

**Ubicación:** `/admin/empresas/`.

### HU-010 — Verificar o rechazar pagos

**Actor:** Personal ApiTech. **Tratamiento:** Ajustada: ambos roles internos.

Como Personal ApiTech, quiero revisar pagos simulados pendientes para habilitar suscripciones solo después de una revisión.

**Aceptación:** El servidor calcula el valor; una segunda confirmación no activa ni cobra otra vez; se registra revisor y fecha.

**Ubicación:** `/admin/pagos/`.

### HU-011 — Gestionar suscripciones

**Actor:** Admin_ApiTech. **Tratamiento:** Ajustada: renovación e idempotencia.

Como Admin_ApiTech, quiero controlar la vigencia de los planes para mantener vigencias y pagos trazables.

**Aceptación:** Una única suscripción activa por empresa; renovar el mismo plan conserva días restantes y el historial. Cambiar plan se verifica al terminar la vigencia anterior.

**Ubicación:** `/admin/empresas/`.

### HU-012 — Pausar y reactivar cuentas

**Actor:** Admin_ApiTech. **Tratamiento:** Ajustada: estados separados de suscripción.

Como Admin_ApiTech, quiero controlar la operación de una empresa para controlar operaciones sin perder los registros anteriores.

**Aceptación:** Al pausar se cierran monitoreos y encolan detenciones externas. Consultas siguen disponibles; operar exige empresa activa y suscripción vigente. Reactivar monitoreo requiere acción explícita.

**Ubicación:** `/admin/empresas/<id>/estado/`.

### HU-013 — Gestionar equipo ApiTech

**Actor:** Admin_ApiTech. **Tratamiento:** Conservada y reforzada.

Como Admin_ApiTech, quiero crear y administrar usuarios internos para distribuir las funciones del equipo de plataforma.

**Aceptación:** Solo roles internos sin empresa; no se permite desactivar la propia cuenta ni cambiar su propio rol.

**Ubicación:** `/usuarios/`.

### HU-014 — Gestionar usuarios de mi empresa

**Actor:** Admin_Cliente. **Tratamiento:** Ajustada: matriz explícita.

Como Admin_Cliente, quiero crear y administrar usuarios de mi empresa para dar acceso al personal autorizado de mi empresa.

**Aceptación:** Solo roles cliente y empresa de la sesión; IDs ajenos devuelven 404; no se acepta elevación a rol interno.

**Ubicación:** `/usuarios/`.

### HU-015 — Actualizar mi perfil

**Actor:** Usuario autenticado. **Tratamiento:** Conservada.

Como Usuario autenticado, quiero editar mi información personal para mantener actualizados mis datos de contacto.

**Aceptación:** Nombre, apellido y correo validado; el formulario no cambia empresa, rol ni estado.

**Ubicación:** `/perfil/`.

### HU-016 — Cambiar contraseña

**Actor:** Usuario autenticado. **Tratamiento:** Conservada.

Como Usuario autenticado, quiero actualizar mi contraseña para proteger el acceso a mi cuenta.

**Aceptación:** Solicita contraseña actual, nueva y confirmación; valida fortaleza, guarda hash y conserva la sesión propia.

**Ubicación:** `/perfil/`.

### HU-017 — Registrar sensor para la API externa

**Actor:** Admin_Cliente. **Tratamiento:** Sustituye registro ligado al simulador interno.

Como Admin_Cliente, quiero registrar sensores de temperatura, humedad o peso para preparar los dispositivos que consumirá el servicio externo.

**Aceptación:** Código único por empresa; respeta límite del plan; crea orden externa pendiente. No genera lecturas dentro de Django.

**Ubicación:** `/app/sensores/nuevo/`.

### HU-018 — Consultar sensores

**Actor:** Usuario cliente. **Tratamiento:** Ajustada: identidad y sincronización.

Como Usuario cliente, quiero ver mis sensores y sus asociaciones para conocer disponibilidad y destino de cada dispositivo.

**Aceptación:** Muestra tipo, estado deseado, confirmación/error externo y asociación; nunca muestra sensores ajenos.

**Ubicación:** `/app/sensores/`.

### HU-019 — Editar sensor

**Actor:** Admin_Cliente. **Tratamiento:** Ajustada: identidad estable.

Como Admin_Cliente, quiero actualizar modelo y fabricante para mantener identificable el dispositivo sin alterar su historial.

**Aceptación:** No se modifica tipo, empresa ni identidad de un sensor con historial; otro dispositivo se registra por separado.

**Ubicación:** `/app/sensores/<id>/editar/`.

### HU-020 — Desactivar sensor

**Actor:** Admin_Cliente. **Tratamiento:** Ajustada: detención externa.

Como Admin_Cliente, quiero detener un dispositivo sin perder sus datos para retirar un dispositivo de la operación conservando evidencia.

**Aceptación:** Cierra monitoreos y deja orden DEACTIVATE; mantiene sensor, asociaciones y lecturas; no elimina físicamente el historial.

**Ubicación:** `/app/sensores/<id>/accion/`.

### HU-021 — Registrar colmena

**Actor:** Admin_Cliente. **Tratamiento:** Ajustada: numeración por empresa.

Como Admin_Cliente, quiero crear una colmena para mi empresa para incorporar unidades de monitoreo dentro de mi plan.

**Aceptación:** Nombre obligatorio, estado válido y cupo del plan; empresa tomada de sesión; asigna menor número visible disponible.

**Ubicación:** `/app/colmenas/nueva/`.

### HU-022 — Consultar colmenas

**Actor:** Usuario cliente. **Tratamiento:** Ampliada.

Como Usuario cliente, quiero buscar y revisar mis colmenas para encontrar la colmena que necesito revisar.

**Aceptación:** Filtro por nombre/ubicación y estado, paginación, enlace al detalle y consulta de archivadas.

**Ubicación:** `/app/colmenas/`.

### HU-023 — Editar colmena

**Actor:** Admin_Cliente. **Tratamiento:** Corregida: sin traslado entre empresas.

Como Admin_Cliente, quiero actualizar nombre, ubicación y estado para mantener actualizada la información del apiario.

**Aceptación:** Empresa e identidad no se pueden cambiar desde el formulario; colmenas archivadas no se editan.

**Ubicación:** `/app/colmenas/<id>/editar/`.

### HU-024 — Eliminar del listado activo conservando historial

**Actor:** Admin_Cliente. **Tratamiento:** Sustituye baja ambigua por archivo lógico.

Como Admin_Cliente, quiero retirar una colmena y liberar su número para retirar colmenas sin atribuir sus datos a otras nuevas.

**Aceptación:** Archiva el registro, conserva número histórico y PK, cierra intervalos y deja detenciones pendientes; no mueve lecturas a la siguiente colmena con el mismo número.

**Ubicación:** `/app/colmenas/<id>/eliminar/`.

### HU-025 — Asociar y desasociar sensores

**Actor:** Admin_Cliente. **Tratamiento:** Ajustada: intervalos e integridad SQL.

Como Admin_Cliente, quiero asignar cada sensor a una colmena para mantener la relación correcta entre dispositivo y colmena.

**Aceptación:** Ambos pertenecen a mi empresa; una asociación activa por sensor; al cerrar se registra fecha final y se conserva historial.

**Ubicación:** `/app/sensores/`.

### HU-026 — Validar condiciones del monitoreo

**Actor:** Admin_Cliente. **Tratamiento:** Aclarada: monitoreo por asociación.

Como Admin_Cliente, quiero iniciar monitoreo solo con relaciones válidas para evitar iniciar operaciones sobre relaciones inválidas.

**Aceptación:** Exige asociación y sensor activos, colmena Estable no archivada y empresa habilitada; no exige los tres tipos para iniciar un sensor.

**Ubicación:** `/app/sensores/`.

### HU-027 — Solicitar activación y detención

**Actor:** Admin_Cliente. **Tratamiento:** Sustituye autoactivación del simulador.

Como Admin_Cliente, quiero controlar el monitoreo desde ApiTech para conocer qué monitoreo solicité y qué confirmó el proveedor.

**Aceptación:** Crea intervalo local y orden durable; el estado externo solo se confirma tras respuesta de la API. Detener cierra el intervalo.

**Ubicación:** `/app/sensores/`.

### HU-028 — Recibir lecturas externas identificadas

**Actor:** Admin_Cliente / API externa. **Tratamiento:** Sustituye HU del actor Sistema y simulador Java.

Como Admin_Cliente / API externa, quiero incorporar las lecturas generadas por el servicio externo para analizar lecturas identificables recibidas del proveedor.

**Aceptación:** Exige empresa, sensor, asociación, colmena, lectura, tipo, unidad, valor, origen y fecha con zona; las lecturas se almacenan tras validarse. No existe generación periódica en Django.

**Ubicación:** `Comando sincronizar_sensores`.

### HU-029 — Consultar resumen de mediciones

**Actor:** Usuario cliente. **Tratamiento:** Corregida: resumen con procedencia temporal.

Como Usuario cliente, quiero ver las condiciones generales de mi empresa para identificar condiciones generales y falta de datos recientes.

**Aceptación:** Promedia la última lectura reciente por sensor y tipo; indica fecha y número de sensores; sin datos muestra —, no cero ni números aleatorios.

**Ubicación:** `/app/dashboard/`.

### HU-030 — Consultar historial

**Actor:** Usuario cliente. **Tratamiento:** Ampliada.

Como Usuario cliente, quiero consultar mediciones anteriores para comparar el comportamiento del apiario en el tiempo.

**Aceptación:** Tabla paginada, filtros de fechas/horas, tipo, origen, colmena y sensor; distingue fecha de medición y recepción en el modelo.

**Ubicación:** `/app/reportes/`.

### HU-031 — Recibir alertas por umbral

**Actor:** Usuario cliente. **Tratamiento:** Actor beneficiario cliente; automatismo interno, sin rol Sistema.

Como Usuario cliente, quiero conocer mediciones fuera del rango configurado para detectar condiciones que requieren atención.

**Aceptación:** Al ingresar lecturas válidas se evalúa umbral empresarial o global; evita alerta activa duplicada por colmena/tipo. Una lectura antigua posterior a otra más reciente no dispara alerta atrasada.

**Ubicación:** `Ingreso de mediciones / /app/alertas/`.

### HU-032 — Consultar alertas

**Actor:** Usuario cliente. **Tratamiento:** Conservada.

Como Usuario cliente, quiero revisar alertas de mi empresa para priorizar las incidencias de mis colmenas.

**Aceptación:** Filtra por estado/colmena y muestra lectura, severidad, fecha y estado; excluye otras empresas.

**Ubicación:** `/app/alertas/`.

### HU-033 — Resolver alertas

**Actor:** Usuario cliente. **Tratamiento:** Ampliada al flujo web.

Como Usuario cliente, quiero registrar que una alerta fue atendida para dejar constancia de las incidencias atendidas.

**Aceptación:** Resolución web por POST, empresa y permiso validados; usuario tomado de la sesión; fecha y notificaciones actualizadas.

**Ubicación:** `/app/alertas/<id>/resolver/`.

### HU-034 — Registrar producción de miel

**Actor:** Admin_Cliente. **Tratamiento:** Conservada.

Como Admin_Cliente, quiero registrar cosechas por colmena para llevar un historial de cosechas separado del peso total.

**Aceptación:** Cantidad positiva en kg, fecha válida y colmena propia activa; cosecha es distinta del peso del sensor.

**Ubicación:** `/app/produccion/nueva/`.

### HU-035 — Consultar producción

**Actor:** Usuario cliente. **Tratamiento:** Conservada y ampliada.

Como Usuario cliente, quiero analizar mis cosechas para analizar los resultados de producción por período.

**Aceptación:** Filtro por período y colmena; total, promedio por colmena con registros, último registro y listado.

**Ubicación:** `/app/produccion/`.

### HU-036 — Generar reportes multicriterio

**Actor:** Usuario cliente. **Tratamiento:** Ampliada.

Como Usuario cliente, quiero analizar mediciones con estadísticas para tomar decisiones basadas en datos filtrados y comparables.

**Aceptación:** Filtros combinables; conteo, mínimo, máximo y promedio por tipo/unidad; gráficos por sensor y datos de la propia empresa.

**Ubicación:** `/app/reportes/`.

### HU-037 — Exportar datos

**Actor:** Usuario cliente. **Tratamiento:** Ampliada.

Como Usuario cliente, quiero obtener archivos para análisis para trabajar con la información autorizada fuera de la aplicación.

**Aceptación:** CSV, XLSX y PDF; respeta filtros y empresa. Exporta mediciones, producción, colmenas o sensores. Neutraliza fórmulas en CSV/Excel.

**Ubicación:** `/app/exportar/<formato>/`.

### HU-038 — Importar datos validados

**Actor:** Usuario cliente. **Tratamiento:** Rehecha con lotes persistentes.

Como Usuario cliente, quiero incorporar históricos sin corromper lo existente para incorporar datos históricos sin duplicar ni mezclar registros.

**Aceptación:** CSV/XLSX con plantilla, vista previa sin escrituras de negocio, confirmación y revalidación; solo asociaciones propias. Alta masiva de colmenas/sensores reservada al administrador.

**Ubicación:** `/app/importar/`.

### HU-039 — Consultar resultado de importación

**Actor:** Usuario cliente. **Tratamiento:** Ampliada.

Como Usuario cliente, quiero identificar filas aceptadas, repetidas y rechazadas para corregir errores de carga con evidencia por fila.

**Aceptación:** Lote por usuario/empresa, conteos y errores descargables; confirmar de nuevo no repite el proceso; las filas inválidas no se guardan.

**Ubicación:** `/app/importar/<id>/`.

### HU-040 — Descargar plantillas

**Actor:** Usuario cliente. **Tratamiento:** Ampliada.

Como Usuario cliente, quiero preparar archivos compatibles para preparar datos con la estructura que acepta ApiTech.

**Aceptación:** Plantillas CSV/XLSX para histórico, colmenas y sensores; encabezados definidos en el código y manual.

**Ubicación:** `/app/plantilla/<formato>/`.

### HU-041 — Solicitar plan y consultar pagos

**Actor:** Visitante / Admin_Cliente. **Tratamiento:** Conservada y reforzada.

Como Visitante / Admin_Cliente, quiero solicitar alta o renovación y consultar su estado para conocer el estado de la solicitud y de la vigencia.

**Aceptación:** Alta pública con tarjeta ficticia (solo últimos cuatro dígitos); cliente administrador solicita renovación simulada; empleado consulta; no hay cobro bancario real.

**Ubicación:** `/planes/ · /solicitud-suscripcion/ · /app/pagos/`.

### HU-042 — Consultar notificaciones

**Actor:** Usuario autenticado. **Tratamiento:** Ampliada a bandeja completa.

Como Usuario autenticado, quiero revisar avisos que me corresponden para atender avisos relevantes para mi cuenta.

**Aceptación:** Bandeja individual, contador y marcado como leída; una alerta notifica solo usuarios activos de su empresa.

**Ubicación:** `/notificaciones/`.

### HU-043 — Consultar ayuda

**Actor:** Usuario autenticado. **Tratamiento:** Conservada.

Como Usuario autenticado, quiero entender el uso de los módulos para utilizar correctamente las funcionalidades disponibles.

**Aceptación:** Ayuda accesible desde menú y perfil, con orientación de conexión, datos, importación y pagos.

**Ubicación:** `/ayuda/`.

### HU-044 — Detalle exclusivo de una colmena

**Actor:** Usuario cliente. **Tratamiento:** Nueva.

Como Usuario cliente, quiero consultar sensores y resultados de una sola colmena para evaluar una colmena sin confundirla con el resto del apiario.

**Aceptación:** Desde el listado se accede al detalle; gráficos, lecturas, asociaciones, producción y alertas se limitan al PK de esa colmena y a la empresa de sesión.

**Ubicación:** `/app/colmenas/<id>/`.

### HU-045 — Numeración independiente y reutilizable

**Actor:** Admin_Cliente. **Tratamiento:** Nueva.

Como Admin_Cliente, quiero usar números comprensibles sin afectar el historial para organizar mis colmenas con números propios sin perder trazabilidad.

**Aceptación:** A y B pueden tener colmena 1; al archivar 1/2/3 y crear tres nuevas se asignan 1/2/3 con nuevas PK. No se renumeran colmenas que siguen activas.

**Ubicación:** `Servicio crear_colmena / archivar_colmena`.

### HU-046 — Conectar y supervisar la API externa

**Actor:** Admin_Cliente / Equipo técnico. **Tratamiento:** Nueva; prueba con proveedor real pendiente de Karen.

Como Admin_Cliente / Equipo técnico, quiero habilitar la conexión de mi empresa y conocer fallos para recuperar el consumo de datos ante interrupciones del proveedor.

**Aceptación:** Credenciales en entorno, sin mostrarlas al navegador; reintentos acotados, órdenes idempotentes, cursores por sensor y rechazo de páginas con datos ajenos.

**Ubicación:** `/app/conexion-api/`.

### HU-047 — Configurar umbrales propios

**Actor:** Admin_Cliente. **Tratamiento:** Nueva documentación de funcionalidad explícita.

Como Admin_Cliente, quiero definir los rangos de alerta de mi empresa para evaluar mediciones con rangos definidos para mi empresa.

**Aceptación:** Mínimo menor que máximo, unidad según tipo, único umbral por empresa/tipo; valores globales marcados como demostración.

**Ubicación:** `/app/umbrales/`.

### HU-048 — Consultar auditoría

**Actor:** Personal ApiTech. **Tratamiento:** Nueva.

Como Personal ApiTech, quiero revisar operaciones relevantes para revisar quién realizó cada operación relevante.

**Aceptación:** Registra acción, módulo, entidad, fecha y usuario o proceso técnico; no guarda contraseñas ni tokens. No existe un usuario o rol Sistema.

**Ubicación:** `/admin/auditoria/`.

### HU-049 — Trasladar datos MK5 y respaldar

**Actor:** Equipo técnico. **Tratamiento:** Nueva.

Como Equipo técnico, quiero migrar una copia de MK5 de forma verificable para continuar operando en Django con un traslado comprobable.

**Aceptación:** Destino vacío; validación con rollback por defecto, --aplicar explícito, conteos por tabla, PK conservadas y hashes compatibles; instrucciones de respaldo y restauración.

**Ubicación:** `manage.py migrar_mk5 · Ejecutar`.

## Requisitos funcionales

| ID MK6 | Nombre y comportamiento obligatorio | Origen MK5 | HU |
|---|---|---|---|
| RF-001 | **Registro de empresas.** NIT único y datos empresariales validados. | RF1 | HU-007 |
| RF-002 | **Administrador inicial.** Crear Admin_Cliente vinculado a una sola empresa. | RF2 | HU-008 |
| RF-003 | **Acceso interno.** Autenticar Admin_ApiTech y Empleado_ApiTech, sin empresa. | RF3 | HU-002 |
| RF-004 | **Identificación por NIT.** Validar existencia; cuenta pausada conserva acceso de consulta. | RF4 | HU-003 |
| RF-005 | **Acceso cliente.** Autenticar cuenta activa dentro del NIT seleccionado y derivar empresa/rol de la sesión. | RF5 | HU-004 |
| RF-006 | **Consulta de empresas.** Listado y búsqueda administrativa de cuentas y suscripciones. | RF6 | HU-009 |
| RF-007 | **Revisión de pagos.** Verificar o rechazar pendientes, con revisor y fecha, sin doble activación. | RF7 | HU-010 |
| RF-008 | **Suscripciones.** Vigencias, límites, suspensión, cancelación y renovaciones históricas; una activa por empresa. | RF8 | HU-011 |
| RF-009 | **Activación de cuenta.** Admin_ApiTech activa la cuenta; operar también requiere suscripción vigente. | RF9 | HU-012 |
| RF-010 | **Pausa empresarial.** Bloquear operaciones y detener intervalos; mantener consulta e historial. | RF10 | HU-012 |
| RF-011 | **Reactivación empresarial.** Reactivar cuenta; monitoreos se reinician explícitamente. | RF11 | HU-012 |
| RF-012 | **Usuarios cliente.** Crear, consultar, editar y desactivar usuarios propios por Admin_Cliente. | RF12 | HU-014 |
| RF-013 | **Usuarios internos.** Gestión de roles internos sin empresa por Admin_ApiTech. | RF13 | HU-013 |
| RF-014 | **Roles.** Cuatro roles humanos, sin rol Sistema; asignaciones validadas del lado servidor. | RF14 | HU-013, HU-014 |
| RF-015 | **Registro de sensores externos.** Crear sensor local y orden REGISTER; código único por empresa y cupo del plan. | RF15 | HU-017 |
| RF-016 | **Consulta de sensores.** Listado con tipo, asociación, estado y sincronización de la empresa. | RF16 | HU-018 |
| RF-017 | **Edición de sensores.** Admin_Cliente edita descripción de hardware; identidad/tipo/empresa permanecen estables. | RF17 | HU-019 |
| RF-018 | **Desactivación de sensores.** Cierra monitoreo y solicita detención externa, sin borrar históricos. | RF18 | HU-020 |
| RF-019 | **Registro de colmenas.** Admin_Cliente registra nombre/ubicación/estado con cupo y empresa de sesión. | RF19 | HU-021 |
| RF-020 | **Asociación de sensores.** Admin_Cliente asocia dispositivos de la misma empresa; uno activo por sensor. | RF20 | HU-025 |
| RF-021 | **Validación de monitoreo.** Asociación/sensor activos, colmena Estable y empresa/suscripción operativas; sin mínimo de tres tipos. | RF21 | HU-026 |
| RF-022 | **Control de monitoreo.** Activación/detención explícita y confirmación externa; no autoactiva un simulador interno. | RF22 | HU-027 |
| RF-023 | **Edición de colmenas.** Editar únicamente campos permitidos, sin aceptar empresa desde formulario. | RF23 | HU-023 |
| RF-024 | **Estado y archivo de colmenas.** Cambiar estado o archivar conservando PK, intervalos y datos; fusiona las dos entradas RF24 originales. | RF24 | HU-024 |
| RF-025 | **Consumo de API externa.** Sustituir generador Java por consumo REST autenticado; el servicio externo genera los datos artificiales. | RF25 | HU-028 |
| RF-026 | **Persistencia de lecturas.** Identidad completa, valor finito, unidad/tipo/origen coherentes, zona horaria y asociación histórica válida. | RF26 | HU-028 |
| RF-027 | **Visualización de mediciones.** Mostrar registros almacenados; sin datos no se inventan valores. | RF27 | HU-029 |
| RF-028 | **Historial filtrado.** Consultar períodos, hora, sensor, colmena, tipo y origen con paginación. | RF28 | HU-030 |
| RF-029 | **Alertas por rango.** Comparar lecturas con umbral aplicable, registrar snapshot y evitar alertas activas equivalentes. | RF29 | HU-031 |
| RF-030 | **Consulta de alertas.** Listar alertas propias por estado/colmena y severidad visible. | RF30 | HU-032 |
| RF-031 | **Resolución de alertas.** POST validado registra usuario de sesión, fecha y estado RESUELTA. | RF31 | HU-033 |
| RF-032 | **Producción de miel.** Guardar cosecha positiva en kg, colmena, fecha y observaciones; separada del peso del sensor. | RF32 | HU-034 |
| RF-033 | **Análisis de producción.** Filtro de período/colmena, total, promedio por colmena con cosechas, último registro y exportación. | RF33 | HU-035 |
| RF-034 | **Exportación.** CSV/XLSX/PDF de mediciones, cosechas, colmenas o sensores, con alcance de empresa. | RF34 | HU-037 |
| RF-035 | **Importación.** CSV/XLSX, estructura validada, vista previa, confirmación, idempotencia y errores por fila. | RF35 | HU-038, HU-039, HU-040 |
| RF-036 | **Portal de pagos.** Solicitud pública de alta, pagos/renovaciones simulados y consulta histórica; no es pasarela bancaria. | RF36 | HU-041 |
| RF-037 | **Notificaciones.** Crear avisos por alerta para usuarios activos de la empresa y permitir lectura individual. | RF37 | HU-042 |
| RF-038 | **Aislamiento empresarial.** Todas las lecturas/escrituras de cliente derivan empresa de sesión; API exige empresa de credencial y referencias coherentes. | RF38 | HU-044, HU-046 |
| RF-039 | **Panel de plataforma.** Resumen administrativo para personal ApiTech, separado del monitoreo de clientes. | RF39 | HU-006 |
| RF-040 | **Estado administrativo.** Estado de cuenta manual separado de vigencia; el comando de vencimiento cierra operación y encola detenciones. | RF40 | HU-012 |
| RF-041 | **Detalle de colmena.** Vista específica con datos exclusivamente de la colmena elegida, incluso su historial archivado. | Nuevo | HU-044 |
| RF-042 | **Números visibles por empresa.** Menor entero positivo libre, reutilizable al archivar, con PK interna inmutable. | Nuevo | HU-045 |
| RF-043 | **Configuración externa.** Conexión habilitable por empresa; URL/token fuera de HTML; estado y último error visibles. | Nuevo | HU-046 |
| RF-044 | **Sincronización resiliente.** Órdenes con clave estable, tres intentos por petición, timeout, cursor por sensor y página atómica; no duplicar lecturas por reading_id. | Nuevo | HU-046 |
| RF-045 | **Umbrales por empresa.** Crear/actualizar umbral único por tipo, validando mínimo/máximo y unidad. | Nuevo | HU-047 |
| RF-046 | **Perfil y contraseña.** Edición personal y cambio de clave con validación de contraseña actual. | Nuevo | HU-015, HU-016 |
| RF-047 | **Auditoría de operaciones.** Trazar acciones relevantes, sin credenciales, diferenciando usuario y proceso técnico. | Nuevo | HU-048 |
| RF-048 | **Ayuda.** Orientar uso, alcance de simulación externa, importación y pagos. | Nuevo | HU-043 |
| RF-049 | **Migración verificable.** Trasladar las tablas MK5 a destino vacío, preservar PK/hash/fechas y comparar conteos; abortar ante incoherencias. | Nuevo | HU-049 |
| RF-050 | **Reportes estadísticos.** Filtros combinados, mínimo, máximo, promedio, conteo y series separadas por sensor/tipo. | Nuevo | HU-036 |
| RF-051 | **Cerrar sesión.** Cerrar sesión mediante POST protegido por CSRF. | Nuevo | HU-005 |

## Requisitos no funcionales

RNF-005 no está activo como requisito no funcional: su unicidad se exige en RF-001. El salto de numeración es deliberado para conservar referencias del MK5.

| ID | Criterio verificable | Evidencia / límite |
|---|---|---|
| RNF-001 | **Seguridad de acceso.** Sesiones, CSRF en mutaciones, ORM parametrizado, autoescape y autorización por rol en servidor. | Pruebas de autenticación, CSRF, acceso denegado y roles. |
| RNF-002 | **Protección de credenciales.** Contraseñas PBKDF2; BCrypt MK5 actualizado tras autenticación válida. Secretos y tokens solo por entorno. | Pruebas de hashes, revisión del ZIP sin .env real. |
| RNF-003 | **Autorización de cuatro roles.** Aplicar la matriz docs/Permisos.md en interfaz y servicios. | Tests de permisos; controles no dependen de ocultar botones. |
| RNF-004 | **Aislamiento empresarial.** No exponer ni modificar datos de otro cliente por URL, formulario, importación o respuesta externa. | Tests con dos empresas y triggers SQL de integridad. |
| RNF-006 | **Integridad referencial e histórica.** FK protegidas, índices únicos, restricciones, transacciones y triggers; historial no reutiliza identidades. | Tests de datos inválidos, rollback, PK y números visibles. |
| RNF-007 | **Disponibilidad operativa.** La caída de la API conserva la información ya guardada y permite consultarla. No se promete SLA de alojamiento. | Pruebas de timeout/errores; disponibilidad 24/7 depende del despliegue. |
| RNF-008 | **Rendimiento medible.** Paginación de 25 filas; gráficos hasta 60 sensores × 120 puntos; importación 2000 filas; exportación 10000 filas. Objetivo a medir: p95 < 3 s para consultas paginadas con 10 usuarios en equipo/BD de entrega. | Límites implementados; ensayo de carga en infraestructura final PENDIENTE, no certificado. |
| RNF-009 | **Crecimiento.** Nuevas empresas y dispositivos mediante datos, sin cambiar código; índices por empresa/fecha/tipo y límites de plan configurables. | Pruebas funcionales multiempresa; capacidad masiva requiere medición adicional. |
| RNF-010 | **Mantenibilidad.** Apps por dominio, servicios centrales, migraciones versionadas, dependencias y pruebas reproducibles. | Estructura del ZIP, check y test. |
| RNF-011 | **Usabilidad y continuidad visual.** Mantener logo, paleta amarillo/oscuro, accesos diferenciados y navegación del MK5; formularios con errores y confirmaciones. | Revisión visual de escritorio y móvil; nuevas vistas consistentes. |
| RNF-012 | **Compatibilidad web.** HTML/CSS/JS adaptable; funcionalidades principales utilizables en navegador de escritorio y ancho móvil de 390 px. | Chromium revisado. Verificación adicional en Firefox/Safari y dispositivos reales PENDIENTE. |
| RNF-013 | **Trazabilidad.** Auditoría de altas, cambios, importaciones, exportaciones y pagos; usuario/fecha/entidad. | Auditoría consultable y tests de operaciones. |
| RNF-014 | **Respaldo y recuperación.** Instrucciones de dump/restauración y archivo SQLite; restaurar primero en una base nueva. | SQL generado/restaurado en prueba técnica; respaldos de la instalación real a cargo del operador. |
| RNF-015 | **Separación de responsabilidades.** Templates y consultas, servicios de negocio, modelos/BD, integración externa y permisos desacoplados. | Arquitectura y dependencias documentadas. |
| RNF-016 | **Implementación Python/Django.** Sustituir paquetes Java/Spring por apps Python y Django 5.2.19; archivos de ejecución y dependencias reproducibles. | Instalación limpia y pruebas. No incluye backend Java en ejecución. |
| RNF-017 | **Resiliencia de integración.** HTTPS en entorno remoto, timeout de 8 s, hasta 3 intentos, comandos durables y cursores transaccionales; sin redirecciones. | Pruebas de cliente HTTP y contrato simulado; conexión con API final de Karen PENDIENTE. |
| RNF-018 | **Procedencia de datos.** Distinguir SIMULADO/REAL y API_EXTERNA/IMPORTACION/MIGRACION_MK5/DEMOSTRACION; no presentar demo como medición real. | Metadatos del modelo, tabla de reportes y revisión de semillas. |
| RNF-019 | **Carga de archivos controlada.** CSV UTF-8/XLSX hasta 5 MB, expansión XLSX 25 MB, máximo 2000 filas; no ejecutar fórmulas ni código. | Validación de archivos y lotes; exportación neutraliza fórmulas. |
| RNF-020 | **Portabilidad documentada.** SQLite para demostración; MySQL 8.0.16+ o MariaDB 10.5+ para destino. Entorno ensayado: Python 3.12 y MariaDB 11.8.6. | No se certifica PostgreSQL ni MariaDB 10.4/XAMPP antiguo en esta entrega. |

## Cobertura de la lista de chequeo académica

| Ítem | Entregable |
|---|---|
| 5 · Django/Flask | Proyecto Django, migraciones, vistas y plantillas. La puesta en marcha en el equipo de entrega sigue los pasos de Ejecutar. |
| 6 · Validación, errores y seguridad | Sesiones, permisos, CSRF, validaciones, aislamiento, restricciones SQL y pruebas. |
| 8 · Carga inicial/masiva | Migración MK5, cargar_demo opcional, importación CSV/XLSX de históricos/colmenas/sensores. |
| 9 · Consumo externo | Cliente REST, órdenes, reintentos y contrato implementados. La evidencia con la API real se obtiene cuando Karen la publique; no se debe marcar completada aún en la sustentación. |
| 10 · Reportes | Filtros multicriterio, estadísticas, gráficos, exportaciones y análisis de producción. |

## Validación y alcance de las evidencias

Las pruebas incluidas se ejecutan con `python manage.py test tests`. Véase `docs/Pruebas.md` para resultados reales y límites. Pruebas con respuestas controladas demuestran el comportamiento del cliente; no equivalen a validar un servicio de Karen aún no entregado. Los objetivos de carga, compatibilidad adicional y disponibilidad de producción no se declaran cumplidos sin mediciones.
