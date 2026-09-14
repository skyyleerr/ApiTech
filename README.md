ApiTech


Descripcion del Proyecto
ApiTech MK5 es un sistema de información web para el monitoreo y la gestión apícola, desarrollado con Spring Boot (Java), Spring Data JPA, Spring Security y Thymeleaf, sobre una base de datos relacional MySQL con integridad referencial reforzada mediante triggers.
La plataforma está diseñada bajo un modelo multiempresa (multi-tenant): distintas empresas apícolas clientes pueden registrarse, suscribirse a un plan y gestionar de forma independiente sus propias colmenas, sensores y usuarios, mientras que el equipo de ApiTech (administradores y empleados internos, sin empresa asociada) supervisa la plataforma, aprueba solicitudes de suscripción y verifica pagos.
El sistema permite asociar sensores por ahora solo simulados a colmenas, activar sesiones de monitoreo, registrar mediciones de temperatura, humedad y peso. Ademas genera alertas automáticas ante condiciones anómalas e incorpora notificaciones a los usuarios, seguimiento de producción de miel, un módulo de suscripciones y pagos (planes, solicitudes de alta, verificación de pagos), control de acceso basado en roles y permisos (RBAC), y una auditoría completa de las acciones realizadas por usuarios o por el propio sistema.


Objetivos del Proyecto
Objetivo general

Diseñar e implementar un sistema de información que permita a empresas apícolas monitorear en tiempo real las condiciones de sus colmenas mediante sensores IoT (que por ahora son simulados), generar alertas automáticas ante anomalías y gestionar de forma centralizada la información operativa, administrativa y comercial de su actividad apícola.

Objetivos específicos

- Implementar un modelo de datos multiempresa que garantice el aislamiento y la integridad de la información entre empresas clientes (colmenas, sensores, mediciones, alertas, producción).
- Desarrollar un módulo de gestión de sensores y colmenas, con asociación histórica sensor–colmena y control de estados (activo/inactivo).
- Implementar un módulo de monitoreo y mediciones que registre lecturas de temperatura, humedad y peso, validando su coherencia con el sensor y el monitoreo activo correspondiente.
- Configurar rangos y umbrales de referencia que disparen alertas automáticas cuando una medición se salga de los límites establecidos.
- Implementar un sistema de notificaciones para mantener informados a los usuarios sobre alertas, pagos y eventos relevantes de su empresa.
- Diseñar un módulo de planes, suscripciones, solicitudes y pagos que permita a empresas solicitar el alta, elegir un plan y que el equipo de ApiTech verifique y gestione los pagos.
- Establecer un sistema de roles y permisos (RBAC) que diferencie entre usuarios internos de ApiTech (Admin/Empleado) y usuarios de empresas cliente (Admin/Empleado), restringiendo el acceso según el rol.
- Registrar y consultar la producción de miel por colmena como indicador de desempeño apícola.
- Mantener una auditoría trazable de las acciones (de usuarios o del sistema) sobre las entidades del sistema, con fines de control y trazabilidad.
- Ofrecer una interfaz web (Thymeleaf) diferenciada por rol (dashboards de administrador ApiTech, empleado ApiTech, administrador de empresa, empleado de empresa) para facilitar el uso del sistema según el perfil del usuario.

Explicación de los diagramas
Caso de Uso
Se representan las funcionalidades principales del sistema ApiTech MK5 y la interacción de los diferentes actores externos con la aplicación. Se identifican los usuarios Admin_ApiTech, Empleado_ApiTech, Admin_Cliente y Empleado_Cliente, además de los sensores simulados. Cada actor se relaciona con los casos de uso que tiene autorizados según sus responsabilidades.
Las funcionalidades se organizan en paquetes como autenticación, gestión de empresas, usuarios, colmenas, sensores, monitoreo, alertas, producción, reportes y auditoría. También se utilizan relaciones «include», que representan comportamientos obligatorios reutilizados por otros casos de uso, y «extend», que representan comportamientos adicionales ejecutados bajo determinadas condiciones. Este diagrama permite definir el alcance funcional del sistema y las responsabilidades de cada tipo de usuario.


MER
Se representa la estructura de los datos del sistema, incluyendo empresas, usuarios, roles, suscripciones, pagos, colmenas, sensores, monitoreo, mediciones, alertas, notificaciones, producción de miel y auditoría. Utiliza la notación de Chen, donde los rectángulos representan entidades, las elipses atributos y los rombos relaciones. Empresa es la entidad central, ya que se relaciona con usuarios, suscripciones, colmenas, sensores y rangos de medición. La entidad ASOCIACION_SC conecta sensores y colmenas, permitiendo registrar su relación y vigencia. El monitoreo genera mediciones y alertas cuando se detectan valores fuera de los rangos establecidos. Las alertas pueden producir notificaciones y las acciones de los usuarios quedan registradas en auditoría. Las cardinalidades 1:N y N:M indican cómo se relacionan las entidades y posteriormente se implementan mediante claves foráneas y tablas intermedias.


Diagrama de Clases 
Se representa la estructura estática del sistema, traduciendo las tablas principales de la base de datos SQL en clases UML organizadas en bloques temáticos. Uno de los bloques contiene las clases relacionadas con el núcleo del sistema y el control de acceso basado en roles, como Empresa, Rol, Permiso y Usuario; otro de los bloques agrupa las clases de suscripciones y pagos; otro representa la operación apícola, incluyendo colmenas, sensores y monitoreo; y otro bloque contiene las clases relacionadas con mediciones, alertas, notificaciones, producción y auditoría. Los atributos identificados con [PK] corresponden a llaves primarias, mientras que los marcados con [FK] representan llaves foráneas. Las asociaciones muestran las relaciones entre las clases mediante cardinalidades como 1:N y 0..1:N, según si la relación es obligatoria u opcional. Empresa funciona como entidad central debido a que el sistema utiliza una arquitectura multiempresa. Además, RolPermiso resuelve la relación muchos a muchos entre roles y permisos, mientras que SensorColmena permite mantener el historial de asociaciones entre sensores y colmenas mediante fechas y estados. Esta última clase es fundamental para la trazabilidad, ya que conecta el sensor con las mediciones, alertas y notificaciones generadas durante un monitoreo. Finalmente, la clase Auditoria registra las acciones realizadas por usuarios o procesos automáticos del sistema, por lo que su relación con Usuario es opcional.


Diagrama de Componentes
Se representa la estructura modular del sistema ApiTech MK5 y las dependencias entre sus principales componentes de software. Se muestran módulos relacionados con la autenticación y seguridad, gestión de usuarios y empresas, suscripciones y pagos, administración de colmenas y sensores, monitoreo de mediciones, alertas, producción de miel, reportes y auditoría.
Cada componente agrupa responsabilidades específicas y expone servicios o interfaces que pueden ser utilizados por otros módulos. Por ejemplo, el componente de monitoreo recibe y procesa las mediciones de los sensores, el componente de alertas analiza los valores frente a los umbrales configurados y el componente de reportes utiliza la información almacenada para generar archivos PDF o Excel. Este diagrama facilita la comprensión de la arquitectura interna, la separación de responsabilidades y las relaciones de dependencia entre los módulos del sistema.


Diagrama de Paquetes 
Se organiza el código fuente bajo el paquete com.apitech.mk5, siguiendo una arquitectura por capas. controller recibe las solicitudes HTTP y utiliza los servicios definidos en service, cuyas implementaciones se encuentran en service.impl. Los paquetes dto.request y dto.response manejan los datos de entrada y salida, mientras que mapper convierte entre DTO y entidades. repository y entity conforman la capa de persistencia y se organizan por módulos como usuarios, apiario, empresas, suscripciones y auditoría. Además, security gestiona la autenticación y autorización; exception, el manejo de errores; config, las configuraciones; y validation y util, las funciones auxiliares. Esta distribución separa responsabilidades, reduce el acoplamiento y facilita el mantenimiento y crecimiento de la aplicación.


Diagrama de Despliegue 
Se describe la distribución física de los componentes de ApiTech MK5 y la comunicación entre los diferentes nodos tecnológicos. El sistema se ejecuta como una aplicación monolítica desarrollada con Spring Boot, empaquetada en un archivo .jar y ejecutada mediante Apache Tomcat embebido en un servidor de aplicaciones.
Los usuarios acceden al sistema mediante un navegador web utilizando el protocolo HTTPS. Los sensores simulados envían mediciones al backend a través de servicios REST. El servidor de aplicaciones se comunica con el servidor de base de datos MySQL 8 mediante JDBC y el conector MySQL. El diagrama también muestra los artefactos desplegados, los protocolos de comunicación y las relaciones entre los nodos, permitiendo comprender la infraestructura física y lógica necesaria para ejecutar la solución.

