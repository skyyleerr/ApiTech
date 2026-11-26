-- ApiTech MK6 · estructura completa y semillas. Generado desde migraciones Django.
-- Probado con MariaDB 11.8.6. Destino: MySQL 8.0.16+ / MariaDB 10.5+.
-- IMPORTAR SOLO EN UNA BASE NUEVA Y VACIA. No modifica ni borra la base MK5.
-- Incluye django_migrations: después de importar ejecutar python manage.py migrate.
-- No contiene empresas, cuentas, contraseñas, tokens ni lecturas de clientes.
SET NAMES utf8mb4;
SET time_zone = '+00:00';
SET FOREIGN_KEY_CHECKS=0;

CREATE TABLE `alertas` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `tipo` varchar(100) NOT NULL,
  `descripcion` longtext NOT NULL,
  `valor_detectado` decimal(10,3) DEFAULT NULL,
  `umbral_min` decimal(10,3) DEFAULT NULL,
  `umbral_max` decimal(10,3) DEFAULT NULL,
  `severidad` varchar(20) NOT NULL,
  `estado` varchar(20) NOT NULL,
  `activa_uniq` smallint(6) GENERATED ALWAYS AS (case when `estado` = 'ACTIVA' then 1 else NULL end) STORED,
  `fecha` datetime(6) NOT NULL,
  `fecha_resolucion` datetime(6) DEFAULT NULL,
  `colmena_id` int(11) NOT NULL,
  `empresa_id` int(11) NOT NULL,
  `sensor_id` int(11) DEFAULT NULL,
  `usuario_resolucion_id` int(11) DEFAULT NULL,
  `medicion_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `medicion_id` (`medicion_id`),
  UNIQUE KEY `uq_alerta_activa` (`colmena_id`,`tipo`,`activa_uniq`),
  KEY `alertas_sensor_id_b5bba864_fk_sensores_id` (`sensor_id`),
  KEY `alertas_usuario_resolucion_id_09c96510_fk_usuarios_id` (`usuario_resolucion_id`),
  KEY `alertas_empresa_2f80d8_idx` (`empresa_id`,`estado`),
  CONSTRAINT `alertas_colmena_id_1451f47f_fk_colmenas_id` FOREIGN KEY (`colmena_id`) REFERENCES `colmenas` (`id`),
  CONSTRAINT `alertas_empresa_id_2329a232_fk_empresas_id` FOREIGN KEY (`empresa_id`) REFERENCES `empresas` (`id`),
  CONSTRAINT `alertas_medicion_id_70656b07_fk_mediciones_id` FOREIGN KEY (`medicion_id`) REFERENCES `mediciones` (`id`),
  CONSTRAINT `alertas_sensor_id_b5bba864_fk_sensores_id` FOREIGN KEY (`sensor_id`) REFERENCES `sensores` (`id`),
  CONSTRAINT `alertas_usuario_resolucion_id_09c96510_fk_usuarios_id` FOREIGN KEY (`usuario_resolucion_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `auditoria` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `actor_tipo` varchar(10) NOT NULL,
  `accion` varchar(100) NOT NULL,
  `modulo` varchar(50) NOT NULL,
  `entidad_tipo` varchar(50) NOT NULL,
  `id_entidad` bigint(20) DEFAULT NULL,
  `descripcion` longtext NOT NULL,
  `fecha` datetime(6) NOT NULL,
  `usuario_id` int(11) DEFAULT NULL,
  `registro_afectado` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `auditoria_usuario_id_5e766294_fk_usuarios_id` (`usuario_id`),
  KEY `auditoria_entidad_cc7a6e_idx` (`entidad_tipo`,`id_entidad`),
  KEY `auditoria_fecha_b71d64_idx` (`fecha`),
  CONSTRAINT `auditoria_usuario_id_5e766294_fk_usuarios_id` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `ck_auditoria_actor` CHECK (`actor_tipo` = 'PROCESO' and `usuario_id` is null or `actor_tipo` = 'USUARIO' and `usuario_id` is not null)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `auth_group` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(150) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `auth_group_permissions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `group_id` int(11) NOT NULL,
  `permission_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `auth_group_permissions_group_id_permission_id_0cd325b0_uniq` (`group_id`,`permission_id`),
  KEY `auth_group_permissio_permission_id_84c5c92e_fk_auth_perm` (`permission_id`),
  CONSTRAINT `auth_group_permissio_permission_id_84c5c92e_fk_auth_perm` FOREIGN KEY (`permission_id`) REFERENCES `auth_permission` (`id`),
  CONSTRAINT `auth_group_permissions_group_id_b120cbf9_fk_auth_group_id` FOREIGN KEY (`group_id`) REFERENCES `auth_group` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `auth_permission` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `content_type_id` int(11) NOT NULL,
  `codename` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `auth_permission_content_type_id_codename_01ab375a_uniq` (`content_type_id`,`codename`),
  CONSTRAINT `auth_permission_content_type_id_2f476e4b_fk_django_co` FOREIGN KEY (`content_type_id`) REFERENCES `django_content_type` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=105 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `colmenas` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `numero` int(10) unsigned DEFAULT NULL CHECK (`numero` >= 0),
  `numero_historico` int(10) unsigned DEFAULT NULL CHECK (`numero_historico` >= 0),
  `archivada` tinyint(1) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `ubicacion` varchar(150) NOT NULL,
  `estado` varchar(50) NOT NULL,
  `fecha_creacion` datetime(6) NOT NULL,
  `fecha_actualizacion` datetime(6) NOT NULL,
  `empresa_id` int(11) NOT NULL,
  `usuario_registro_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_colmena_numero_empresa` (`empresa_id`,`numero`),
  KEY `colmenas_usuario_registro_id_32e7e0cc_fk_usuarios_id` (`usuario_registro_id`),
  KEY `colmenas_empresa_2eb13b_idx` (`empresa_id`,`estado`),
  CONSTRAINT `colmenas_empresa_id_07eb58db_fk_empresas_id` FOREIGN KEY (`empresa_id`) REFERENCES `empresas` (`id`),
  CONSTRAINT `colmenas_usuario_registro_id_32e7e0cc_fk_usuarios_id` FOREIGN KEY (`usuario_registro_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `comandos_sensor` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `accion` varchar(20) NOT NULL,
  `clave` uuid NOT NULL,
  `completado` tinyint(1) NOT NULL,
  `intentos` int(10) unsigned NOT NULL CHECK (`intentos` >= 0),
  `error` varchar(250) NOT NULL,
  `fecha` datetime(6) NOT NULL,
  `asociacion_id` int(11) DEFAULT NULL,
  `sensor_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `clave` (`clave`),
  KEY `comandos_sensor_asociacion_id_21e448dd_fk_sensor_colmena_id` (`asociacion_id`),
  KEY `comandos_sensor_sensor_id_225ea6fc_fk_sensores_id` (`sensor_id`),
  CONSTRAINT `comandos_sensor_asociacion_id_21e448dd_fk_sensor_colmena_id` FOREIGN KEY (`asociacion_id`) REFERENCES `sensor_colmena` (`id`),
  CONSTRAINT `comandos_sensor_sensor_id_225ea6fc_fk_sensores_id` FOREIGN KEY (`sensor_id`) REFERENCES `sensores` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `conexiones_api` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `habilitada` tinyint(1) NOT NULL,
  `ultimo_exito` datetime(6) DEFAULT NULL,
  `ultimo_error` varchar(250) NOT NULL,
  `empresa_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `empresa_id` (`empresa_id`),
  CONSTRAINT `conexiones_api_empresa_id_3f519b4e_fk_empresas_id` FOREIGN KEY (`empresa_id`) REFERENCES `empresas` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `django_admin_log` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `action_time` datetime(6) NOT NULL,
  `object_id` longtext DEFAULT NULL,
  `object_repr` varchar(200) NOT NULL,
  `action_flag` smallint(5) unsigned NOT NULL CHECK (`action_flag` >= 0),
  `change_message` longtext NOT NULL,
  `content_type_id` int(11) DEFAULT NULL,
  `user_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `django_admin_log_content_type_id_c4bce8eb_fk_django_co` (`content_type_id`),
  KEY `django_admin_log_user_id_c564eba6_fk_usuarios_id` (`user_id`),
  CONSTRAINT `django_admin_log_content_type_id_c4bce8eb_fk_django_co` FOREIGN KEY (`content_type_id`) REFERENCES `django_content_type` (`id`),
  CONSTRAINT `django_admin_log_user_id_c564eba6_fk_usuarios_id` FOREIGN KEY (`user_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `django_content_type` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `app_label` varchar(100) NOT NULL,
  `model` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `django_content_type_app_label_model_76bd3d3b_uniq` (`app_label`,`model`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `django_migrations` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `app` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `applied` datetime(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `django_session` (
  `session_key` varchar(40) NOT NULL,
  `session_data` longtext NOT NULL,
  `expire_date` datetime(6) NOT NULL,
  PRIMARY KEY (`session_key`),
  KEY `django_session_expire_date_a5c62663` (`expire_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `empresas` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nit` varchar(20) NOT NULL,
  `nombre_empresa` varchar(150) NOT NULL,
  `correo` varchar(150) NOT NULL,
  `telefono` varchar(30) NOT NULL,
  `direccion` varchar(200) NOT NULL,
  `estado` varchar(20) NOT NULL,
  `fecha_registro` datetime(6) NOT NULL,
  `fecha_actualizacion` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `nit` (`nit`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `importaciones` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(200) NOT NULL,
  `tipo` varchar(20) NOT NULL,
  `filas` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`filas`)),
  `errores` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`errores`)),
  `resultado` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`resultado`)),
  `confirmado` tinyint(1) NOT NULL,
  `fecha` datetime(6) NOT NULL,
  `empresa_id` int(11) NOT NULL,
  `usuario_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `importaciones_empresa_id_1c70db1e_fk_empresas_id` (`empresa_id`),
  KEY `importaciones_usuario_id_dc15351a_fk_usuarios_id` (`usuario_id`),
  CONSTRAINT `importaciones_empresa_id_1c70db1e_fk_empresas_id` FOREIGN KEY (`empresa_id`) REFERENCES `empresas` (`id`),
  CONSTRAINT `importaciones_usuario_id_dc15351a_fk_usuarios_id` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `mediciones` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tipo_medicion` varchar(12) NOT NULL,
  `valor` decimal(10,3) NOT NULL,
  `unidad` varchar(10) NOT NULL,
  `origen` varchar(10) NOT NULL,
  `fuente` varchar(20) NOT NULL,
  `fecha` datetime(6) NOT NULL,
  `fecha_recepcion` datetime(6) NOT NULL,
  `id_externo` varchar(100) DEFAULT NULL,
  `asociacion_id` int(11) NOT NULL,
  `sensor_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_medicion_sensor_externo` (`sensor_id`,`fuente`,`id_externo`),
  KEY `mediciones_asociac_87a531_idx` (`asociacion_id`,`fecha`),
  KEY `mediciones_tipo_me_fcd214_idx` (`tipo_medicion`,`fecha`),
  KEY `mediciones_sensor_id_7dae7b18` (`sensor_id`),
  CONSTRAINT `mediciones_asociacion_id_c3692972_fk_sensor_colmena_id` FOREIGN KEY (`asociacion_id`) REFERENCES `sensor_colmena` (`id`),
  CONSTRAINT `mediciones_sensor_id_7dae7b18_fk_sensores_id` FOREIGN KEY (`sensor_id`) REFERENCES `sensores` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `monitoreo` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `estado` varchar(20) NOT NULL,
  `fecha_inicio` datetime(6) NOT NULL,
  `fecha_fin` datetime(6) DEFAULT NULL,
  `activo_uniq` smallint(6) GENERATED ALWAYS AS (case when `estado` = 'activo' then 1 else NULL end) STORED,
  `asociacion_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_monitoreo_activo` (`asociacion_id`,`activo_uniq`),
  CONSTRAINT `monitoreo_asociacion_id_1e05398b_fk_sensor_colmena_id` FOREIGN KEY (`asociacion_id`) REFERENCES `sensor_colmena` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `notificaciones` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `tipo` varchar(50) NOT NULL,
  `titulo` varchar(150) NOT NULL,
  `mensaje` longtext NOT NULL,
  `leida` tinyint(1) NOT NULL,
  `fecha` datetime(6) NOT NULL,
  `alerta_id` int(11) DEFAULT NULL,
  `empresa_id` int(11) DEFAULT NULL,
  `pago_id` int(11) DEFAULT NULL,
  `usuario_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `notificaciones_alerta_id_99818edd_fk_alertas_id` (`alerta_id`),
  KEY `notificaciones_empresa_id_7eef7576_fk_empresas_id` (`empresa_id`),
  KEY `notificaciones_pago_id_b74e0a19_fk_pagos_id` (`pago_id`),
  KEY `notificacio_usuario_ef49b7_idx` (`usuario_id`,`leida`),
  CONSTRAINT `notificaciones_alerta_id_99818edd_fk_alertas_id` FOREIGN KEY (`alerta_id`) REFERENCES `alertas` (`id`),
  CONSTRAINT `notificaciones_empresa_id_7eef7576_fk_empresas_id` FOREIGN KEY (`empresa_id`) REFERENCES `empresas` (`id`),
  CONSTRAINT `notificaciones_pago_id_b74e0a19_fk_pagos_id` FOREIGN KEY (`pago_id`) REFERENCES `pagos` (`id`),
  CONSTRAINT `notificaciones_usuario_id_a75f5971_fk_usuarios_id` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `pagos` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `valor` decimal(12,2) NOT NULL,
  `fecha_pago` date NOT NULL,
  `metodo_pago` varchar(50) NOT NULL,
  `referencia` varchar(100) NOT NULL,
  `estado` varchar(10) NOT NULL,
  `fecha_verificacion` datetime(6) DEFAULT NULL,
  `observaciones` longtext NOT NULL,
  `usuario_verifico_id` int(11) DEFAULT NULL,
  `suscripcion_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `pagos_usuario_verifico_id_5897039e_fk_usuarios_id` (`usuario_verifico_id`),
  KEY `pagos_suscripcion_id_99dc8463_fk_suscripciones_id` (`suscripcion_id`),
  KEY `pagos_estado_79b102_idx` (`estado`),
  CONSTRAINT `pagos_suscripcion_id_99dc8463_fk_suscripciones_id` FOREIGN KEY (`suscripcion_id`) REFERENCES `suscripciones` (`id`),
  CONSTRAINT `pagos_usuario_verifico_id_5897039e_fk_usuarios_id` FOREIGN KEY (`usuario_verifico_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `permisos` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `codigo` varchar(60) NOT NULL,
  `descripcion` varchar(200) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `codigo` (`codigo`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `planes_suscripcion` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nombre_plan` varchar(100) NOT NULL,
  `precio` decimal(12,2) NOT NULL,
  `duracion_dias` int(10) unsigned NOT NULL CHECK (`duracion_dias` >= 0),
  `estado` varchar(20) NOT NULL,
  `max_colmenas` int(10) unsigned NOT NULL CHECK (`max_colmenas` >= 0),
  `max_sensores` int(10) unsigned NOT NULL CHECK (`max_sensores` >= 0),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `produccion_miel` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `cantidad` decimal(10,3) NOT NULL,
  `unidad` varchar(10) NOT NULL,
  `fecha` date NOT NULL,
  `observaciones` longtext NOT NULL,
  `colmena_id` int(11) NOT NULL,
  `empresa_id` int(11) NOT NULL,
  `referencia_importacion` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `referencia_importacion` (`referencia_importacion`),
  KEY `produccion_miel_colmena_id_87603b63_fk_colmenas_id` (`colmena_id`),
  KEY `produccion__empresa_8f2024_idx` (`empresa_id`,`fecha`),
  CONSTRAINT `produccion_miel_colmena_id_87603b63_fk_colmenas_id` FOREIGN KEY (`colmena_id`) REFERENCES `colmenas` (`id`),
  CONSTRAINT `produccion_miel_empresa_id_1389b45b_fk_empresas_id` FOREIGN KEY (`empresa_id`) REFERENCES `empresas` (`id`),
  CONSTRAINT `ck_produccion_no_negativa` CHECK (`cantidad` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `rangos_umbrales` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `tipo_medicion` varchar(12) NOT NULL,
  `valor_min` decimal(10,3) NOT NULL,
  `valor_max` decimal(10,3) NOT NULL,
  `unidad` varchar(10) NOT NULL,
  `estado` varchar(20) NOT NULL,
  `es_demostracion` tinyint(1) NOT NULL,
  `fecha_configuracion` datetime(6) NOT NULL,
  `empresa_id` int(11) DEFAULT NULL,
  `empresa_norm` int(11) GENERATED ALWAYS AS (coalesce(`empresa_id`,0)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_umbral_empresa_tipo` (`empresa_norm`,`tipo_medicion`),
  KEY `rangos_umbrales_empresa_id_a4089ba6_fk_empresas_id` (`empresa_id`),
  CONSTRAINT `rangos_umbrales_empresa_id_a4089ba6_fk_empresas_id` FOREIGN KEY (`empresa_id`) REFERENCES `empresas` (`id`),
  CONSTRAINT `ck_umbral_rango` CHECK (`valor_min` < `valor_max`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `roles` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(50) NOT NULL,
  `descripcion` varchar(200) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `nombre` (`nombre`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `roles_permisos` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `rol_id` int(11) NOT NULL,
  `permiso_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `roles_permisos_rol_id_permiso_id_daf91080_uniq` (`rol_id`,`permiso_id`),
  KEY `roles_permisos_permiso_id_ff70f233_fk_permisos_id` (`permiso_id`),
  CONSTRAINT `roles_permisos_permiso_id_ff70f233_fk_permisos_id` FOREIGN KEY (`permiso_id`) REFERENCES `permisos` (`id`),
  CONSTRAINT `roles_permisos_rol_id_5daf3c70_fk_roles_id` FOREIGN KEY (`rol_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `sensor_colmena` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `fecha_inicio` datetime(6) NOT NULL,
  `fecha_fin` datetime(6) DEFAULT NULL,
  `activo` tinyint(1) NOT NULL,
  `activo_uniq` smallint(6) GENERATED ALWAYS AS (case when `activo` = 1 then 1 else NULL end) STORED,
  `colmena_id` int(11) NOT NULL,
  `empresa_id` int(11) NOT NULL,
  `sensor_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_sensor_activo` (`sensor_id`,`activo_uniq`),
  KEY `sensor_colmena_empresa_id_5c344e09_fk_empresas_id` (`empresa_id`),
  KEY `sensor_colm_colmena_19c21c_idx` (`colmena_id`,`activo`),
  CONSTRAINT `sensor_colmena_colmena_id_25a8a17a_fk_colmenas_id` FOREIGN KEY (`colmena_id`) REFERENCES `colmenas` (`id`),
  CONSTRAINT `sensor_colmena_empresa_id_5c344e09_fk_empresas_id` FOREIGN KEY (`empresa_id`) REFERENCES `empresas` (`id`),
  CONSTRAINT `sensor_colmena_sensor_id_5eaf3d77_fk_sensores_id` FOREIGN KEY (`sensor_id`) REFERENCES `sensores` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `sensores` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `codigo` varchar(50) NOT NULL,
  `tipo` varchar(12) NOT NULL,
  `modelo` varchar(100) NOT NULL,
  `fabricante` varchar(100) NOT NULL,
  `es_simulado` tinyint(1) NOT NULL,
  `estado` varchar(20) NOT NULL,
  `fecha_registro` datetime(6) NOT NULL,
  `fecha_actualizacion` datetime(6) NOT NULL,
  `fecha_activacion` datetime(6) DEFAULT NULL,
  `fecha_desactivacion` datetime(6) DEFAULT NULL,
  `id_externo` varchar(100) DEFAULT NULL,
  `sync_estado` varchar(10) NOT NULL,
  `sync_error` varchar(255) NOT NULL,
  `ultima_sincronizacion` datetime(6) DEFAULT NULL,
  `cursor_sincronizacion` varchar(100) NOT NULL,
  `empresa_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_sensor_codigo_empresa` (`empresa_id`,`codigo`),
  UNIQUE KEY `uq_sensor_id_externo` (`empresa_id`,`id_externo`),
  KEY `sensores_empresa_470060_idx` (`empresa_id`,`tipo`),
  CONSTRAINT `sensores_empresa_id_250851ee_fk_empresas_id` FOREIGN KEY (`empresa_id`) REFERENCES `empresas` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `solicitudes_suscripcion` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nombre_empresa` varchar(150) NOT NULL,
  `nit` varchar(20) NOT NULL,
  `correo_empresa` varchar(150) NOT NULL,
  `telefono` varchar(30) NOT NULL,
  `direccion` varchar(255) NOT NULL,
  `nombre_contacto` varchar(120) NOT NULL,
  `correo_contacto` varchar(150) NOT NULL,
  `telefono_contacto` varchar(30) NOT NULL,
  `tarjeta_falsa_ultimos4` varchar(4) NOT NULL,
  `estado` varchar(10) NOT NULL,
  `pendiente_uniq` smallint(6) GENERATED ALWAYS AS (case when `estado` = 'PENDIENTE' then 1 else NULL end) STORED,
  `observaciones` longtext NOT NULL,
  `motivo_rechazo` longtext NOT NULL,
  `fecha_solicitud` datetime(6) NOT NULL,
  `fecha_revision` datetime(6) DEFAULT NULL,
  `fecha_actualizacion` datetime(6) NOT NULL,
  `plan_solicitado_id` int(11) DEFAULT NULL,
  `usuario_revisor_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_solicitud_pendiente_nit` (`nit`,`pendiente_uniq`),
  KEY `solicitudes_suscripc_plan_solicitado_id_1171b6dc_fk_planes_su` (`plan_solicitado_id`),
  KEY `solicitudes_suscripc_usuario_revisor_id_ca853c77_fk_usuarios_` (`usuario_revisor_id`),
  CONSTRAINT `solicitudes_suscripc_plan_solicitado_id_1171b6dc_fk_planes_su` FOREIGN KEY (`plan_solicitado_id`) REFERENCES `planes_suscripcion` (`id`),
  CONSTRAINT `solicitudes_suscripc_usuario_revisor_id_ca853c77_fk_usuarios_` FOREIGN KEY (`usuario_revisor_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `suscripciones` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `estado` varchar(12) NOT NULL,
  `activa_uniq` smallint(6) GENERATED ALWAYS AS (case when `estado` = 'ACTIVA' then 1 else NULL end) STORED,
  `fecha_inicio` date DEFAULT NULL,
  `fecha_vencimiento` date DEFAULT NULL,
  `fecha_cancelacion` date DEFAULT NULL,
  `renovacion_automatica` tinyint(1) NOT NULL,
  `fecha_registro` datetime(6) NOT NULL,
  `fecha_actualizacion` datetime(6) NOT NULL,
  `empresa_id` int(11) NOT NULL,
  `plan_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_suscripcion_activa` (`empresa_id`,`activa_uniq`),
  KEY `suscripciones_plan_id_9fe5be51_fk_planes_suscripcion_id` (`plan_id`),
  KEY `suscripcion_estado_6cdbc1_idx` (`estado`),
  CONSTRAINT `suscripciones_empresa_id_189e6166_fk_empresas_id` FOREIGN KEY (`empresa_id`) REFERENCES `empresas` (`id`),
  CONSTRAINT `suscripciones_plan_id_9fe5be51_fk_planes_suscripcion_id` FOREIGN KEY (`plan_id`) REFERENCES `planes_suscripcion` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `usuarios` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `password` varchar(128) NOT NULL,
  `last_login` datetime(6) DEFAULT NULL,
  `is_superuser` tinyint(1) NOT NULL,
  `es_interno` tinyint(1) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `apellido` varchar(100) NOT NULL,
  `correo` varchar(150) NOT NULL,
  `estado` varchar(20) NOT NULL,
  `is_staff` tinyint(1) NOT NULL,
  `fecha_registro` datetime(6) NOT NULL,
  `fecha_actualizacion` datetime(6) NOT NULL,
  `empresa_id` int(11) DEFAULT NULL,
  `rol_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `correo` (`correo`),
  KEY `usuarios_empresa_id_5461dd8a_fk_empresas_id` (`empresa_id`),
  KEY `usuarios_rol_id_fa223853_fk_roles_id` (`rol_id`),
  CONSTRAINT `usuarios_empresa_id_5461dd8a_fk_empresas_id` FOREIGN KEY (`empresa_id`) REFERENCES `empresas` (`id`),
  CONSTRAINT `usuarios_rol_id_fa223853_fk_roles_id` FOREIGN KEY (`rol_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `ck_usuario_rol_empresa` CHECK (`empresa_id` is null and `es_interno` = 0x01 or `empresa_id` is not null and `es_interno` = 0x00)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `usuarios_groups` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `usuario_id` int(11) NOT NULL,
  `group_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `usuarios_groups_usuario_id_group_id_a66c5ef3_uniq` (`usuario_id`,`group_id`),
  KEY `usuarios_groups_group_id_18c61092_fk_auth_group_id` (`group_id`),
  CONSTRAINT `usuarios_groups_group_id_18c61092_fk_auth_group_id` FOREIGN KEY (`group_id`) REFERENCES `auth_group` (`id`),
  CONSTRAINT `usuarios_groups_usuario_id_1132ca50_fk_usuarios_id` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `usuarios_user_permissions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `usuario_id` int(11) NOT NULL,
  `permission_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `usuarios_user_permissions_usuario_id_permission_id_474b33a5_uniq` (`usuario_id`,`permission_id`),
  KEY `usuarios_user_permis_permission_id_af615ca1_fk_auth_perm` (`permission_id`),
  CONSTRAINT `usuarios_user_permis_permission_id_af615ca1_fk_auth_perm` FOREIGN KEY (`permission_id`) REFERENCES `auth_permission` (`id`),
  CONSTRAINT `usuarios_user_permissions_usuario_id_232fd58d_fk_usuarios_id` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (1,'Can add log entry',1,'add_logentry');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (2,'Can change log entry',1,'change_logentry');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (3,'Can delete log entry',1,'delete_logentry');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (4,'Can view log entry',1,'view_logentry');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (5,'Can add permission',2,'add_permission');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (6,'Can change permission',2,'change_permission');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (7,'Can delete permission',2,'delete_permission');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (8,'Can view permission',2,'view_permission');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (9,'Can add group',3,'add_group');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (10,'Can change group',3,'change_group');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (11,'Can delete group',3,'delete_group');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (12,'Can view group',3,'view_group');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (13,'Can add content type',4,'add_contenttype');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (14,'Can change content type',4,'change_contenttype');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (15,'Can delete content type',4,'delete_contenttype');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (16,'Can view content type',4,'view_contenttype');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (17,'Can add session',5,'add_session');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (18,'Can change session',5,'change_session');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (19,'Can delete session',5,'delete_session');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (20,'Can view session',5,'view_session');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (21,'Can add permiso',6,'add_permiso');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (22,'Can change permiso',6,'change_permiso');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (23,'Can delete permiso',6,'delete_permiso');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (24,'Can view permiso',6,'view_permiso');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (25,'Can add rol',7,'add_rol');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (26,'Can change rol',7,'change_rol');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (27,'Can delete rol',7,'delete_rol');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (28,'Can view rol',7,'view_rol');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (29,'Can add usuario',8,'add_usuario');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (30,'Can change usuario',8,'change_usuario');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (31,'Can delete usuario',8,'delete_usuario');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (32,'Can view usuario',8,'view_usuario');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (33,'Can add empresa',9,'add_empresa');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (34,'Can change empresa',9,'change_empresa');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (35,'Can delete empresa',9,'delete_empresa');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (36,'Can view empresa',9,'view_empresa');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (37,'Can add pago',10,'add_pago');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (38,'Can change pago',10,'change_pago');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (39,'Can delete pago',10,'delete_pago');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (40,'Can view pago',10,'view_pago');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (41,'Can add plan',11,'add_plan');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (42,'Can change plan',11,'change_plan');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (43,'Can delete plan',11,'delete_plan');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (44,'Can view plan',11,'view_plan');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (45,'Can add solicitud suscripcion',12,'add_solicitudsuscripcion');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (46,'Can change solicitud suscripcion',12,'change_solicitudsuscripcion');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (47,'Can delete solicitud suscripcion',12,'delete_solicitudsuscripcion');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (48,'Can view solicitud suscripcion',12,'view_solicitudsuscripcion');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (49,'Can add suscripcion',13,'add_suscripcion');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (50,'Can change suscripcion',13,'change_suscripcion');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (51,'Can delete suscripcion',13,'delete_suscripcion');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (52,'Can view suscripcion',13,'view_suscripcion');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (53,'Can add produccion miel',14,'add_produccionmiel');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (54,'Can change produccion miel',14,'change_produccionmiel');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (55,'Can delete produccion miel',14,'delete_produccionmiel');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (56,'Can view produccion miel',14,'view_produccionmiel');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (57,'Can add sensor',15,'add_sensor');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (58,'Can change sensor',15,'change_sensor');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (59,'Can delete sensor',15,'delete_sensor');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (60,'Can view sensor',15,'view_sensor');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (61,'Can add sensor colmena',16,'add_sensorcolmena');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (62,'Can change sensor colmena',16,'change_sensorcolmena');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (63,'Can delete sensor colmena',16,'delete_sensorcolmena');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (64,'Can view sensor colmena',16,'view_sensorcolmena');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (65,'Can add colmena',17,'add_colmena');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (66,'Can change colmena',17,'change_colmena');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (67,'Can delete colmena',17,'delete_colmena');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (68,'Can view colmena',17,'view_colmena');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (69,'Can add medicion',18,'add_medicion');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (70,'Can change medicion',18,'change_medicion');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (71,'Can delete medicion',18,'delete_medicion');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (72,'Can view medicion',18,'view_medicion');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (73,'Can add monitoreo',19,'add_monitoreo');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (74,'Can change monitoreo',19,'change_monitoreo');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (75,'Can delete monitoreo',19,'delete_monitoreo');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (76,'Can view monitoreo',19,'view_monitoreo');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (77,'Can add notificacion',20,'add_notificacion');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (78,'Can change notificacion',20,'change_notificacion');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (79,'Can delete notificacion',20,'delete_notificacion');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (80,'Can view notificacion',20,'view_notificacion');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (81,'Can add rango umbral',21,'add_rangoumbral');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (82,'Can change rango umbral',21,'change_rangoumbral');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (83,'Can delete rango umbral',21,'delete_rangoumbral');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (84,'Can view rango umbral',21,'view_rangoumbral');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (85,'Can add alerta',22,'add_alerta');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (86,'Can change alerta',22,'change_alerta');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (87,'Can delete alerta',22,'delete_alerta');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (88,'Can view alerta',22,'view_alerta');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (89,'Can add auditoria',23,'add_auditoria');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (90,'Can change auditoria',23,'change_auditoria');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (91,'Can delete auditoria',23,'delete_auditoria');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (92,'Can view auditoria',23,'view_auditoria');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (93,'Can add comando sensor',24,'add_comandosensor');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (94,'Can change comando sensor',24,'change_comandosensor');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (95,'Can delete comando sensor',24,'delete_comandosensor');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (96,'Can view comando sensor',24,'view_comandosensor');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (97,'Can add conexion api',25,'add_conexionapi');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (98,'Can change conexion api',25,'change_conexionapi');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (99,'Can delete conexion api',25,'delete_conexionapi');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (100,'Can view conexion api',25,'view_conexionapi');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (101,'Can add lote importacion',26,'add_loteimportacion');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (102,'Can change lote importacion',26,'change_loteimportacion');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (103,'Can delete lote importacion',26,'delete_loteimportacion');
INSERT INTO `auth_permission` (`id`,`name`,`content_type_id`,`codename`) VALUES (104,'Can view lote importacion',26,'view_loteimportacion');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (1,'admin','logentry');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (17,'apiario','colmena');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (14,'apiario','produccionmiel');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (15,'apiario','sensor');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (16,'apiario','sensorcolmena');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (23,'auditoria','auditoria');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (3,'auth','group');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (2,'auth','permission');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (4,'contenttypes','contenttype');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (9,'empresas','empresa');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (26,'importaciones','loteimportacion');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (24,'integraciones','comandosensor');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (25,'integraciones','conexionapi');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (22,'monitoreo','alerta');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (18,'monitoreo','medicion');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (19,'monitoreo','monitoreo');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (20,'monitoreo','notificacion');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (21,'monitoreo','rangoumbral');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (5,'sessions','session');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (10,'suscripciones','pago');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (11,'suscripciones','plan');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (12,'suscripciones','solicitudsuscripcion');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (13,'suscripciones','suscripcion');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (6,'usuarios','permiso');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (7,'usuarios','rol');
INSERT INTO `django_content_type` (`id`,`app_label`,`model`) VALUES (8,'usuarios','usuario');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (1,'empresas','0001_initial','2026-11-22 04:54:53.772872');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (2,'contenttypes','0001_initial','2026-11-22 04:54:53.787524');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (3,'contenttypes','0002_remove_content_type_name','2026-11-22 04:54:53.800103');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (4,'auth','0001_initial','2026-11-22 04:54:53.848121');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (5,'auth','0002_alter_permission_name_max_length','2026-11-22 04:54:53.858135');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (6,'auth','0003_alter_user_email_max_length','2026-11-22 04:54:53.861668');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (7,'auth','0004_alter_user_username_opts','2026-11-22 04:54:53.864612');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (8,'auth','0005_alter_user_last_login_null','2026-11-22 04:54:53.866826');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (9,'auth','0006_require_contenttypes_0002','2026-11-22 04:54:53.867312');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (10,'auth','0007_alter_validators_add_error_messages','2026-11-22 04:54:53.869212');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (11,'auth','0008_alter_user_username_max_length','2026-11-22 04:54:53.871336');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (12,'auth','0009_alter_user_last_name_max_length','2026-11-22 04:54:53.873417');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (13,'auth','0010_alter_group_name_max_length','2026-11-22 04:54:53.878920');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (14,'auth','0011_update_proxy_permissions','2026-11-22 04:54:53.882739');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (15,'auth','0012_alter_user_first_name_max_length','2026-11-22 04:54:53.884985');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (16,'usuarios','0001_initial','2026-11-22 04:54:53.998470');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (17,'admin','0001_initial','2026-11-22 04:54:54.024891');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (18,'admin','0002_logentry_remove_auto_add','2026-11-22 04:54:54.029121');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (19,'admin','0003_logentry_add_action_flag_choices','2026-11-22 04:54:54.032935');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (20,'apiario','0001_initial','2026-11-22 04:54:54.060601');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (21,'apiario','0002_initial','2026-11-22 04:54:54.253585');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (22,'apiario','0003_produccionmiel_referencia_importacion','2026-11-22 04:54:54.266670');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (23,'auditoria','0001_initial','2026-11-22 04:54:54.270880');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (24,'auditoria','0002_initial','2026-11-22 04:54:54.327400');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (25,'auditoria','0003_auditoria_registro_afectado','2026-11-22 04:54:54.339709');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (26,'importaciones','0001_initial','2026-11-22 04:54:54.358516');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (27,'importaciones','0002_initial','2026-11-22 04:54:54.377138');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (28,'suscripciones','0001_initial','2026-11-22 04:54:54.392721');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (29,'suscripciones','0002_initial','2026-11-22 04:54:54.579789');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (30,'monitoreo','0001_initial','2026-11-22 04:54:54.637228');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (31,'monitoreo','0002_initial','2026-11-22 04:54:54.978351');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (32,'monitoreo','0003_remove_medicion_uq_medicion_sensor_externo_and_more','2026-11-22 04:54:55.063616');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (33,'integraciones','0001_initial','2026-11-22 04:54:55.176821');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (34,'integraciones','0002_integridad_empresa','2026-11-22 04:54:55.257649');
INSERT INTO `django_migrations` (`id`,`app`,`name`,`applied`) VALUES (35,'sessions','0001_initial','2026-11-22 04:54:55.266440');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (1,'alertas.resolver','');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (2,'alertas.ver','');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (3,'auditoria.ver','');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (4,'colmenas.gestionar','');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (5,'colmenas.ver','');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (6,'empresas.gestionar','');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (7,'empresas.ver','');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (8,'importaciones.ejecutar','');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (9,'mediciones.ver','');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (10,'monitoreo.gestionar','');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (11,'pagos.ver_propios','');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (12,'pagos.ver_todos','');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (13,'pagos.verificar','');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (14,'produccion.registrar','');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (15,'produccion.ver','');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (16,'reportes.exportar','');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (17,'reportes.ver','');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (18,'sensores.gestionar','');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (19,'sensores.ver','');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (20,'solicitudes.revisar','');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (21,'solicitudes.ver','');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (22,'umbrales.gestionar','');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (23,'usuarios_empresa.gestionar','');
INSERT INTO `permisos` (`id`,`codigo`,`descripcion`) VALUES (24,'usuarios_internos.gestionar','');
INSERT INTO `planes_suscripcion` (`id`,`nombre_plan`,`precio`,`duracion_dias`,`estado`,`max_colmenas`,`max_sensores`) VALUES (1,'Plan Básico',49000.00,30,'activo',20,20);
INSERT INTO `planes_suscripcion` (`id`,`nombre_plan`,`precio`,`duracion_dias`,`estado`,`max_colmenas`,`max_sensores`) VALUES (2,'Plan Profesional',99000.00,30,'activo',60,60);
INSERT INTO `rangos_umbrales` (`id`,`tipo_medicion`,`valor_min`,`valor_max`,`unidad`,`estado`,`es_demostracion`,`fecha_configuracion`,`empresa_id`) VALUES (1,'TEMPERATURA',18.000,35.000,'°C','activo',1,'2026-11-22 04:54:55.339441',NULL);
INSERT INTO `rangos_umbrales` (`id`,`tipo_medicion`,`valor_min`,`valor_max`,`unidad`,`estado`,`es_demostracion`,`fecha_configuracion`,`empresa_id`) VALUES (2,'HUMEDAD',35.000,80.000,'%','activo',1,'2026-11-22 04:54:55.341414',NULL);
INSERT INTO `rangos_umbrales` (`id`,`tipo_medicion`,`valor_min`,`valor_max`,`unidad`,`estado`,`es_demostracion`,`fecha_configuracion`,`empresa_id`) VALUES (3,'PESO',0.000,80.000,'kg','activo',1,'2026-11-22 04:54:55.342356',NULL);
INSERT INTO `roles` (`id`,`nombre`,`descripcion`) VALUES (1,'Admin_ApiTech','Administrador de la plataforma ApiTech (sin empresa)');
INSERT INTO `roles` (`id`,`nombre`,`descripcion`) VALUES (2,'Empleado_ApiTech','Empleado operativo de ApiTech (sin empresa)');
INSERT INTO `roles` (`id`,`nombre`,`descripcion`) VALUES (3,'Admin_Cliente','Administrador de una empresa cliente');
INSERT INTO `roles` (`id`,`nombre`,`descripcion`) VALUES (4,'Empleado_Cliente','Empleado operativo de una empresa cliente');
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (1,1,3);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (2,1,6);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (3,1,7);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (4,1,12);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (5,1,13);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (6,1,20);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (7,1,21);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (8,1,24);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (9,2,3);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (10,2,7);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (11,2,12);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (12,2,13);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (13,2,20);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (14,2,21);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (15,3,1);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (16,3,2);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (17,3,4);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (18,3,5);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (19,3,8);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (20,3,9);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (21,3,10);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (22,3,11);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (23,3,14);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (24,3,15);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (25,3,16);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (26,3,17);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (27,3,18);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (28,3,19);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (29,3,22);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (30,3,23);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (31,4,1);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (32,4,2);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (33,4,5);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (34,4,8);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (35,4,9);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (36,4,11);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (37,4,15);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (38,4,16);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (39,4,17);
INSERT INTO `roles_permisos` (`id`,`rol_id`,`permiso_id`) VALUES (40,4,19);

SET FOREIGN_KEY_CHECKS=1;

DELIMITER $$
CREATE TRIGGER `mk6_alertas_insert` BEFORE INSERT ON `alertas` FOR EACH ROW BEGIN IF NOT (EXISTS (SELECT 1 FROM colmenas c WHERE c.id=NEW.colmena_id AND c.empresa_id=NEW.empresa_id)
 AND (NEW.sensor_id IS NULL OR EXISTS (SELECT 1 FROM sensores s WHERE s.id=NEW.sensor_id AND s.empresa_id=NEW.empresa_id))
 AND (NEW.medicion_id IS NULL OR EXISTS (SELECT 1 FROM mediciones m JOIN sensor_colmena a ON a.id=m.asociacion_id
 WHERE m.id=NEW.medicion_id AND a.colmena_id=NEW.colmena_id AND m.sensor_id=NEW.sensor_id))) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Integridad de empresa/identidad: alertas'; END IF; END$$
CREATE TRIGGER `mk6_alertas_update` BEFORE UPDATE ON `alertas` FOR EACH ROW BEGIN IF NOT (EXISTS (SELECT 1 FROM colmenas c WHERE c.id=NEW.colmena_id AND c.empresa_id=NEW.empresa_id)
 AND (NEW.sensor_id IS NULL OR EXISTS (SELECT 1 FROM sensores s WHERE s.id=NEW.sensor_id AND s.empresa_id=NEW.empresa_id))
 AND (NEW.medicion_id IS NULL OR EXISTS (SELECT 1 FROM mediciones m JOIN sensor_colmena a ON a.id=m.asociacion_id
 WHERE m.id=NEW.medicion_id AND a.colmena_id=NEW.colmena_id AND m.sensor_id=NEW.sensor_id))) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Integridad de empresa/identidad: alertas'; END IF; END$$
CREATE TRIGGER `mk6_colmenas_insert` BEFORE INSERT ON `colmenas` FOR EACH ROW BEGIN IF NOT (1=1) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Integridad de empresa/identidad: colmenas'; END IF; END$$
CREATE TRIGGER `mk6_colmenas_update` BEFORE UPDATE ON `colmenas` FOR EACH ROW BEGIN IF NOT (1=1 AND (NEW.empresa_id=OLD.empresa_id OR (NEW.empresa_id IS NULL AND OLD.empresa_id IS NULL))) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Integridad de empresa/identidad: colmenas'; END IF; END$$
CREATE TRIGGER `mk6_comandos_sensor_insert` BEFORE INSERT ON `comandos_sensor` FOR EACH ROW BEGIN IF NOT (NEW.asociacion_id IS NULL OR EXISTS (SELECT 1 FROM sensor_colmena a WHERE a.id=NEW.asociacion_id AND a.sensor_id=NEW.sensor_id)) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Integridad de empresa/identidad: comandos_sensor'; END IF; END$$
CREATE TRIGGER `mk6_comandos_sensor_update` BEFORE UPDATE ON `comandos_sensor` FOR EACH ROW BEGIN IF NOT (NEW.asociacion_id IS NULL OR EXISTS (SELECT 1 FROM sensor_colmena a WHERE a.id=NEW.asociacion_id AND a.sensor_id=NEW.sensor_id)) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Integridad de empresa/identidad: comandos_sensor'; END IF; END$$
CREATE TRIGGER `mk6_importaciones_insert` BEFORE INSERT ON `importaciones` FOR EACH ROW BEGIN IF NOT (EXISTS (SELECT 1 FROM usuarios u WHERE u.id=NEW.usuario_id AND u.empresa_id=NEW.empresa_id)) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Integridad de empresa/identidad: importaciones'; END IF; END$$
CREATE TRIGGER `mk6_importaciones_update` BEFORE UPDATE ON `importaciones` FOR EACH ROW BEGIN IF NOT (EXISTS (SELECT 1 FROM usuarios u WHERE u.id=NEW.usuario_id AND u.empresa_id=NEW.empresa_id)) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Integridad de empresa/identidad: importaciones'; END IF; END$$
CREATE TRIGGER `mk6_mediciones_insert` BEFORE INSERT ON `mediciones` FOR EACH ROW BEGIN IF NOT (EXISTS (SELECT 1 FROM sensor_colmena a JOIN sensores s ON s.id=a.sensor_id
 WHERE a.id=NEW.asociacion_id AND s.id=NEW.sensor_id AND s.tipo=NEW.tipo_medicion
 AND NEW.origen=CASE WHEN s.es_simulado=1 THEN 'SIMULADO' ELSE 'REAL' END
 AND NEW.unidad=CASE s.tipo WHEN 'TEMPERATURA' THEN '°C' WHEN 'HUMEDAD' THEN '%' WHEN 'PESO' THEN 'kg' END)) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Integridad de empresa/identidad: mediciones'; END IF; END$$
CREATE TRIGGER `mk6_mediciones_update` BEFORE UPDATE ON `mediciones` FOR EACH ROW BEGIN IF NOT (EXISTS (SELECT 1 FROM sensor_colmena a JOIN sensores s ON s.id=a.sensor_id
 WHERE a.id=NEW.asociacion_id AND s.id=NEW.sensor_id AND s.tipo=NEW.tipo_medicion
 AND NEW.origen=CASE WHEN s.es_simulado=1 THEN 'SIMULADO' ELSE 'REAL' END
 AND NEW.unidad=CASE s.tipo WHEN 'TEMPERATURA' THEN '°C' WHEN 'HUMEDAD' THEN '%' WHEN 'PESO' THEN 'kg' END) AND (NEW.sensor_id=OLD.sensor_id OR (NEW.sensor_id IS NULL AND OLD.sensor_id IS NULL)) AND (NEW.asociacion_id=OLD.asociacion_id OR (NEW.asociacion_id IS NULL AND OLD.asociacion_id IS NULL))) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Integridad de empresa/identidad: mediciones'; END IF; END$$
CREATE TRIGGER `mk6_notificaciones_insert` BEFORE INSERT ON `notificaciones` FOR EACH ROW BEGIN IF NOT (EXISTS (SELECT 1 FROM usuarios u WHERE u.id=NEW.usuario_id AND
 (u.empresa_id=NEW.empresa_id OR (u.empresa_id IS NULL AND NEW.empresa_id IS NULL)))
 AND (NEW.alerta_id IS NULL OR EXISTS (SELECT 1 FROM alertas a WHERE a.id=NEW.alerta_id AND a.empresa_id=NEW.empresa_id))
 AND (NEW.pago_id IS NULL OR NEW.empresa_id IS NULL OR EXISTS (SELECT 1 FROM pagos p JOIN suscripciones s ON s.id=p.suscripcion_id WHERE p.id=NEW.pago_id AND s.empresa_id=NEW.empresa_id))) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Integridad de empresa/identidad: notificaciones'; END IF; END$$
CREATE TRIGGER `mk6_notificaciones_update` BEFORE UPDATE ON `notificaciones` FOR EACH ROW BEGIN IF NOT (EXISTS (SELECT 1 FROM usuarios u WHERE u.id=NEW.usuario_id AND
 (u.empresa_id=NEW.empresa_id OR (u.empresa_id IS NULL AND NEW.empresa_id IS NULL)))
 AND (NEW.alerta_id IS NULL OR EXISTS (SELECT 1 FROM alertas a WHERE a.id=NEW.alerta_id AND a.empresa_id=NEW.empresa_id))
 AND (NEW.pago_id IS NULL OR NEW.empresa_id IS NULL OR EXISTS (SELECT 1 FROM pagos p JOIN suscripciones s ON s.id=p.suscripcion_id WHERE p.id=NEW.pago_id AND s.empresa_id=NEW.empresa_id))) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Integridad de empresa/identidad: notificaciones'; END IF; END$$
CREATE TRIGGER `mk6_pagos_insert` BEFORE INSERT ON `pagos` FOR EACH ROW BEGIN IF NOT (NEW.usuario_verifico_id IS NULL OR EXISTS (SELECT 1 FROM usuarios u WHERE u.id=NEW.usuario_verifico_id AND u.es_interno=1)) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Integridad de empresa/identidad: pagos'; END IF; END$$
CREATE TRIGGER `mk6_pagos_update` BEFORE UPDATE ON `pagos` FOR EACH ROW BEGIN IF NOT (NEW.usuario_verifico_id IS NULL OR EXISTS (SELECT 1 FROM usuarios u WHERE u.id=NEW.usuario_verifico_id AND u.es_interno=1)) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Integridad de empresa/identidad: pagos'; END IF; END$$
CREATE TRIGGER `mk6_produccion_miel_insert` BEFORE INSERT ON `produccion_miel` FOR EACH ROW BEGIN IF NOT (EXISTS (SELECT 1 FROM colmenas c WHERE c.id=NEW.colmena_id AND c.empresa_id=NEW.empresa_id)) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Integridad de empresa/identidad: produccion_miel'; END IF; END$$
CREATE TRIGGER `mk6_produccion_miel_update` BEFORE UPDATE ON `produccion_miel` FOR EACH ROW BEGIN IF NOT (EXISTS (SELECT 1 FROM colmenas c WHERE c.id=NEW.colmena_id AND c.empresa_id=NEW.empresa_id) AND (NEW.empresa_id=OLD.empresa_id OR (NEW.empresa_id IS NULL AND OLD.empresa_id IS NULL))) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Integridad de empresa/identidad: produccion_miel'; END IF; END$$
CREATE TRIGGER `mk6_sensor_colmena_insert` BEFORE INSERT ON `sensor_colmena` FOR EACH ROW BEGIN IF NOT (EXISTS (SELECT 1 FROM sensores s JOIN colmenas c ON c.id=NEW.colmena_id
 WHERE s.id=NEW.sensor_id AND s.empresa_id=NEW.empresa_id AND c.empresa_id=NEW.empresa_id)) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Integridad de empresa/identidad: sensor_colmena'; END IF; END$$
CREATE TRIGGER `mk6_sensor_colmena_update` BEFORE UPDATE ON `sensor_colmena` FOR EACH ROW BEGIN IF NOT (EXISTS (SELECT 1 FROM sensores s JOIN colmenas c ON c.id=NEW.colmena_id
 WHERE s.id=NEW.sensor_id AND s.empresa_id=NEW.empresa_id AND c.empresa_id=NEW.empresa_id) AND (NEW.empresa_id=OLD.empresa_id OR (NEW.empresa_id IS NULL AND OLD.empresa_id IS NULL)) AND (NEW.sensor_id=OLD.sensor_id OR (NEW.sensor_id IS NULL AND OLD.sensor_id IS NULL)) AND (NEW.colmena_id=OLD.colmena_id OR (NEW.colmena_id IS NULL AND OLD.colmena_id IS NULL))) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Integridad de empresa/identidad: sensor_colmena'; END IF; END$$
CREATE TRIGGER `mk6_sensores_insert` BEFORE INSERT ON `sensores` FOR EACH ROW BEGIN IF NOT (1=1) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Integridad de empresa/identidad: sensores'; END IF; END$$
CREATE TRIGGER `mk6_sensores_update` BEFORE UPDATE ON `sensores` FOR EACH ROW BEGIN IF NOT (1=1 AND (NEW.empresa_id=OLD.empresa_id OR (NEW.empresa_id IS NULL AND OLD.empresa_id IS NULL)) AND (NEW.tipo=OLD.tipo OR (NEW.tipo IS NULL AND OLD.tipo IS NULL)) AND (NEW.es_simulado=OLD.es_simulado OR (NEW.es_simulado IS NULL AND OLD.es_simulado IS NULL))) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Integridad de empresa/identidad: sensores'; END IF; END$$
CREATE TRIGGER `mk6_usuarios_insert` BEFORE INSERT ON `usuarios` FOR EACH ROW BEGIN IF NOT (EXISTS (SELECT 1 FROM roles r WHERE r.id=NEW.rol_id AND
 ((r.nombre IN ('Admin_ApiTech','Empleado_ApiTech') AND NEW.empresa_id IS NULL AND NEW.es_interno=1)
 OR (r.nombre IN ('Admin_Cliente','Empleado_Cliente') AND NEW.empresa_id IS NOT NULL AND NEW.es_interno=0)))) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Integridad de empresa/identidad: usuarios'; END IF; END$$
CREATE TRIGGER `mk6_usuarios_update` BEFORE UPDATE ON `usuarios` FOR EACH ROW BEGIN IF NOT (EXISTS (SELECT 1 FROM roles r WHERE r.id=NEW.rol_id AND
 ((r.nombre IN ('Admin_ApiTech','Empleado_ApiTech') AND NEW.empresa_id IS NULL AND NEW.es_interno=1)
 OR (r.nombre IN ('Admin_Cliente','Empleado_Cliente') AND NEW.empresa_id IS NOT NULL AND NEW.es_interno=0))) AND (NEW.empresa_id=OLD.empresa_id OR (NEW.empresa_id IS NULL AND OLD.empresa_id IS NULL))) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Integridad de empresa/identidad: usuarios'; END IF; END$$
DELIMITER ;
