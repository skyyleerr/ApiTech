-- =========================================================
-- APITECH — BASE DE DATOS CORREGIDA Y COMPLETA
-- Sistema de Información para el Monitoreo y Gestión Apícola
-- Version 2: corrige integridad empresa-sensor-colmena, umbrales
-- duplicables, monitoreo sin validación, rango 2038 en fechas y
-- amplía permisos/trazabilidad de alertas y notificaciones.
-- Base tomada del modelo original: NO se eliminó ninguna tabla
-- ni funcionalidad existente.
-- =========================================================

DROP DATABASE IF EXISTS apitech;
CREATE DATABASE apitech
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE apitech;

-- =========================================================
-- 1. EMPRESAS
-- =========================================================
CREATE TABLE empresas (
    id_empresa INT AUTO_INCREMENT PRIMARY KEY,
    nit VARCHAR(20) UNIQUE NOT NULL,
    nombre_empresa VARCHAR(150) NOT NULL,
    correo VARCHAR(150),
    telefono VARCHAR(30),
    direccion VARCHAR(200),
    estado VARCHAR(20) NOT NULL DEFAULT 'activa',
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_nit (nit)
) ENGINE=InnoDB;

-- =========================================================
-- 2. ROLES, PERMISOS Y ROLES_PERMISOS (RBAC normalizado)
-- =========================================================
CREATE TABLE roles (
    id_rol INT AUTO_INCREMENT PRIMARY KEY,
    nombre_rol VARCHAR(50) UNIQUE NOT NULL,
    descripcion VARCHAR(200)
) ENGINE=InnoDB;
-- NOTA: "Sistema" NO es una fila aquí. Es un actor automático (ver tabla
-- auditoria.actor_tipo), nunca un rol asignable a un usuario que inicia sesión.

CREATE TABLE permisos (
    id_permiso INT AUTO_INCREMENT PRIMARY KEY,
    codigo_permiso VARCHAR(60) UNIQUE NOT NULL,
    descripcion VARCHAR(200)
) ENGINE=InnoDB;

CREATE TABLE roles_permisos (
    id_rol INT NOT NULL,
    id_permiso INT NOT NULL,
    PRIMARY KEY (id_rol, id_permiso),
    FOREIGN KEY (id_rol) REFERENCES roles(id_rol) ON DELETE CASCADE,
    FOREIGN KEY (id_permiso) REFERENCES permisos(id_permiso) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 3. USUARIOS
-- =========================================================
CREATE TABLE usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    id_empresa INT NULL,              -- NULL SOLO para Admin_ApiTech / Empleado_ApiTech
    id_rol INT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100),
    correo VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,   -- hash (bcrypt) generado por la aplicación
    estado VARCHAR(20) NOT NULL DEFAULT 'activo',
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ultimo_login TIMESTAMP NULL,
    FOREIGN KEY (id_empresa) REFERENCES empresas(id_empresa) ON DELETE RESTRICT,
    FOREIGN KEY (id_rol) REFERENCES roles(id_rol),
    INDEX idx_correo (correo),
    INDEX idx_empresa (id_empresa)
) ENGINE=InnoDB;
-- Regla de coherencia rol<->empresa aplicada con triggers (ver sección de TRIGGERS).

-- =========================================================
-- 4. PLANES DE SUSCRIPCIÓN, SUSCRIPCIONES Y PAGOS
-- =========================================================
CREATE TABLE planes_suscripcion (
    id_plan INT AUTO_INCREMENT PRIMARY KEY,
    nombre_plan VARCHAR(100) NOT NULL,
    precio DECIMAL(12,2) NOT NULL,
    duracion_dias INT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'activo'
) ENGINE=InnoDB;

CREATE TABLE suscripciones (
    id_suscripcion INT AUTO_INCREMENT PRIMARY KEY,
    id_empresa INT NOT NULL,
    id_plan INT NOT NULL,
    estado ENUM('PENDIENTE','ACTIVA','SUSPENDIDA','VENCIDA','CANCELADA') NOT NULL DEFAULT 'PENDIENTE',
    activo_uniq TINYINT GENERATED ALWAYS AS (IF(estado='ACTIVA',1,NULL)) STORED,
    fecha_inicio DATE,
    fecha_vencimiento DATE,
    fecha_cancelacion DATE NULL,
    renovacion_automatica BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_empresa) REFERENCES empresas(id_empresa) ON DELETE RESTRICT,
    FOREIGN KEY (id_plan) REFERENCES planes_suscripcion(id_plan) ON DELETE RESTRICT,
    UNIQUE KEY uq_suscripcion_activa (id_empresa, activo_uniq),
    UNIQUE KEY uq_suscripcion_empresa (id_suscripcion, id_empresa),
    INDEX idx_empresa (id_empresa),
    INDEX idx_estado (estado)
) ENGINE=InnoDB;
-- El historial se conserva: cada renovación/cambio de plan es una fila nueva,
-- nunca se sobreescribe una suscripción anterior.

CREATE TABLE pagos (
    id_pago INT AUTO_INCREMENT PRIMARY KEY,
    id_empresa INT NOT NULL,
    id_suscripcion INT NOT NULL,
    valor DECIMAL(12,2) NOT NULL,
    fecha_pago DATE NOT NULL,
    metodo_pago VARCHAR(50),
    referencia VARCHAR(100),
    estado ENUM('PENDIENTE','VERIFICADO','RECHAZADO') NOT NULL DEFAULT 'PENDIENTE',
    fecha_verificacion TIMESTAMP NULL,
    id_usuario_verifico INT NULL,     -- debe ser Admin_ApiTech o Empleado_ApiTech (regla de aplicación)
    observaciones TEXT,
    FOREIGN KEY (id_empresa) REFERENCES empresas(id_empresa) ON DELETE RESTRICT,
    FOREIGN KEY (id_suscripcion) REFERENCES suscripciones(id_suscripcion) ON DELETE RESTRICT,
    CONSTRAINT fk_pago_suscripcion_empresa FOREIGN KEY (id_suscripcion, id_empresa) REFERENCES suscripciones(id_suscripcion, id_empresa),
    FOREIGN KEY (id_usuario_verifico) REFERENCES usuarios(id_usuario),
    INDEX idx_empresa (id_empresa),
    INDEX idx_estado (estado)
) ENGINE=InnoDB;

-- =========================================================
-- 5. COLMENAS
-- =========================================================
CREATE TABLE colmenas (
    id_colmena INT AUTO_INCREMENT PRIMARY KEY,
    id_empresa INT NOT NULL,
    id_usuario_registro INT NULL,     -- quién la registró (informativo)
    nombre VARCHAR(100) NOT NULL,
    ubicacion VARCHAR(150),
    estado VARCHAR(50) NOT NULL DEFAULT 'Estable',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_empresa) REFERENCES empresas(id_empresa) ON DELETE RESTRICT,
    FOREIGN KEY (id_usuario_registro) REFERENCES usuarios(id_usuario) ON DELETE SET NULL,
    -- clave única compuesta: soporte para la FK compuesta de sensor_colmena
    UNIQUE KEY uq_colmena_empresa (id_colmena, id_empresa),
    INDEX idx_empresa (id_empresa)
) ENGINE=InnoDB;

-- =========================================================
-- 6. SENSORES (dispositivos, simulados o reales)
-- =========================================================
CREATE TABLE sensores (
    id_sensor INT AUTO_INCREMENT PRIMARY KEY,
    id_empresa INT NOT NULL,
    codigo VARCHAR(50) UNIQUE NOT NULL,
    tipo ENUM('TEMPERATURA','HUMEDAD','PESO') NOT NULL,
    modelo VARCHAR(100),
    fabricante VARCHAR(100),
    es_simulado BOOLEAN NOT NULL DEFAULT TRUE,   -- FALSE = sensor físico IoT (ESP32, etc.)
    estado VARCHAR(20) NOT NULL DEFAULT 'activo',
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    fecha_activacion TIMESTAMP NULL,
    fecha_desactivacion TIMESTAMP NULL,
    FOREIGN KEY (id_empresa) REFERENCES empresas(id_empresa) ON DELETE RESTRICT,
    -- clave única compuesta: soporte para la FK compuesta de sensor_colmena
    UNIQUE KEY uq_sensor_empresa (id_sensor, id_empresa),
    INDEX idx_empresa (id_empresa),
    INDEX idx_tipo (tipo)
) ENGINE=InnoDB;

-- =========================================================
-- 7. ASOCIACIÓN SENSOR-COLMENA (con historial + integridad empresa)
-- =========================================================
CREATE TABLE sensor_colmena (
    id_asociacion INT AUTO_INCREMENT PRIMARY KEY,
    id_sensor INT NOT NULL,
    id_colmena INT NOT NULL,
    id_empresa INT NOT NULL,          -- denormalizado a propósito: habilita las FK compuestas de abajo
    fecha_inicio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_fin TIMESTAMP NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    -- Columna generada: solo toma valor cuando activo=1; MySQL permite múltiples
    -- NULL en un índice único, así que esto NO bloquea el historial (asociaciones
    -- inactivas repetidas), pero SÍ impide que un mismo sensor tenga dos
    -- asociaciones activas simultáneas.
    activo_uniq TINYINT GENERATED ALWAYS AS (IF(activo = 1, 1, NULL)) STORED,
    FOREIGN KEY (id_sensor) REFERENCES sensores(id_sensor) ON DELETE RESTRICT,
    FOREIGN KEY (id_colmena) REFERENCES colmenas(id_colmena) ON DELETE RESTRICT,
    -- Garantiza sensor.id_empresa = colmena.id_empresa: ambas FK compuestas
    -- deben apuntar a filas cuyo id_empresa coincida con el de esta tabla.
    CONSTRAINT fk_sc_sensor_empresa
        FOREIGN KEY (id_sensor, id_empresa) REFERENCES sensores(id_sensor, id_empresa),
    CONSTRAINT fk_sc_colmena_empresa
        FOREIGN KEY (id_colmena, id_empresa) REFERENCES colmenas(id_colmena, id_empresa),
    UNIQUE KEY uq_sensor_activo (id_sensor, activo_uniq),
    INDEX idx_sensor (id_sensor),
    INDEX idx_colmena (id_colmena),
    INDEX idx_empresa (id_empresa)
) ENGINE=InnoDB;

-- =========================================================
-- 8. MONITOREO (activación sobre una asociación sensor-colmena)
-- =========================================================
CREATE TABLE monitoreo (
    id_monitoreo INT AUTO_INCREMENT PRIMARY KEY,
    id_asociacion INT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'activo',
    fecha_inicio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_fin TIMESTAMP NULL,
    activo_uniq TINYINT GENERATED ALWAYS AS (IF(estado='activo',1,NULL)) STORED,
    FOREIGN KEY (id_asociacion) REFERENCES sensor_colmena(id_asociacion) ON DELETE RESTRICT,
    UNIQUE KEY uq_monitoreo_activo (id_asociacion, activo_uniq),
    INDEX idx_asociacion (id_asociacion)
) ENGINE=InnoDB;
-- Regla reforzada con trigger: el monitoreo solo puede quedar 'activo' si la
-- asociación referenciada está activa=1 y el sensor está en estado='activo'.

-- =========================================================
-- 9. RANGOS Y UMBRALES
-- =========================================================
CREATE TABLE rangos_umbrales (
    id_umbral INT AUTO_INCREMENT PRIMARY KEY,
    id_empresa INT NULL,   -- NULL = umbral global por defecto de ApiTech
    tipo_medicion ENUM('TEMPERATURA','HUMEDAD','PESO') NOT NULL,
    valor_min FLOAT NOT NULL,
    valor_max FLOAT NOT NULL,
    unidad VARCHAR(10) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'activo',
    fecha_configuracion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- Normaliza NULL -> 0 solo para efectos de unicidad: evita que se
    -- inserten dos umbrales globales (id_empresa NULL) para el mismo tipo,
    -- algo que el UNIQUE original NO impedía (NULL != NULL en MySQL).
    id_empresa_norm INT GENERATED ALWAYS AS (IFNULL(id_empresa, 0)) STORED,
    FOREIGN KEY (id_empresa) REFERENCES empresas(id_empresa) ON DELETE RESTRICT,
    CONSTRAINT chk_umbral_rango CHECK (valor_min < valor_max),
    UNIQUE KEY uq_umbral_empresa_tipo (id_empresa_norm, tipo_medicion)
) ENGINE=InnoDB;
-- Prioridad de resolución (aplicada en Spring Boot, no en SQL):
--   1) buscar umbral con id_empresa = :idEmpresa
--   2) si no existe, usar el umbral con id_empresa IS NULL (global)

-- =========================================================
-- 10. MEDICIONES (historial de lecturas)
-- =========================================================
CREATE TABLE mediciones (
    id_medicion BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_asociacion INT NOT NULL,
    tipo_medicion ENUM('TEMPERATURA','HUMEDAD','PESO') NOT NULL,
    valor FLOAT NOT NULL,
    unidad VARCHAR(10) NOT NULL,
    origen ENUM('SIMULADO','REAL') NOT NULL DEFAULT 'SIMULADO',
    -- DATETIME en vez de TIMESTAMP: TIMESTAMP tiene rango máximo 2038-01-19,
    -- inviable para un histórico de sensores que se acumula por años.
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_asociacion) REFERENCES sensor_colmena(id_asociacion) ON DELETE RESTRICT,
    INDEX idx_asociacion (id_asociacion),
    INDEX idx_fecha (fecha),
    INDEX idx_tipo_fecha (tipo_medicion, fecha)
) ENGINE=InnoDB;

-- =========================================================
-- 11. ALERTAS
-- =========================================================
CREATE TABLE alertas (
    id_alerta INT AUTO_INCREMENT PRIMARY KEY,
    id_colmena INT NOT NULL,
    id_empresa INT NOT NULL,           -- debe coincidir con la empresa de la colmena
    id_medicion BIGINT NULL,
    id_sensor INT NULL,                -- redundante pero útil cuando id_medicion es NULL
    tipo VARCHAR(100) NOT NULL,
    descripcion TEXT,
    valor_detectado FLOAT NULL,        -- snapshot: qué valor disparó la alerta
    umbral_min FLOAT NULL,             -- snapshot: umbral vigente en ese momento
    umbral_max FLOAT NULL,
    severidad VARCHAR(20) NOT NULL DEFAULT 'media',
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    fecha_resolucion DATETIME NULL,
    id_usuario_resolucion INT NULL,
    FOREIGN KEY (id_colmena) REFERENCES colmenas(id_colmena) ON DELETE RESTRICT,
    FOREIGN KEY (id_empresa) REFERENCES empresas(id_empresa),
    CONSTRAINT fk_alerta_colmena_empresa FOREIGN KEY (id_colmena, id_empresa) REFERENCES colmenas(id_colmena, id_empresa),
    FOREIGN KEY (id_medicion) REFERENCES mediciones(id_medicion),
    FOREIGN KEY (id_sensor) REFERENCES sensores(id_sensor),
    FOREIGN KEY (id_usuario_resolucion) REFERENCES usuarios(id_usuario),
    INDEX idx_colmena (id_colmena),
    INDEX idx_empresa (id_empresa),
    INDEX idx_estado (estado),
    INDEX idx_severidad_estado (severidad, estado)
) ENGINE=InnoDB;

-- =========================================================
-- 12. NOTIFICACIONES
-- =========================================================
CREATE TABLE notificaciones (
    id_notificacion INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    id_empresa INT NULL,               -- NULL solo para usuarios ApiTech
    tipo VARCHAR(50) NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    mensaje TEXT,
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    id_alerta INT NULL,
    id_pago INT NULL,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE RESTRICT,
    FOREIGN KEY (id_empresa) REFERENCES empresas(id_empresa),
    FOREIGN KEY (id_alerta) REFERENCES alertas(id_alerta) ON DELETE SET NULL,
    FOREIGN KEY (id_pago) REFERENCES pagos(id_pago) ON DELETE SET NULL,
    INDEX idx_usuario (id_usuario),
    INDEX idx_empresa (id_empresa),
    INDEX idx_leida (leida)
) ENGINE=InnoDB;

-- =========================================================
-- 13. PRODUCCIÓN DE MIEL
-- =========================================================
CREATE TABLE produccion_miel (
    id_produccion INT AUTO_INCREMENT PRIMARY KEY,
    id_colmena INT NOT NULL,
    id_empresa INT NOT NULL,           -- debe coincidir con la empresa de la colmena
    cantidad FLOAT NOT NULL,
    unidad VARCHAR(10) NOT NULL DEFAULT 'kg',
    fecha DATE NOT NULL,
    observaciones TEXT,
    FOREIGN KEY (id_colmena) REFERENCES colmenas(id_colmena) ON DELETE RESTRICT,
    FOREIGN KEY (id_empresa) REFERENCES empresas(id_empresa),
    CONSTRAINT fk_produccion_colmena_empresa FOREIGN KEY (id_colmena, id_empresa) REFERENCES colmenas(id_colmena, id_empresa),
    INDEX idx_colmena (id_colmena),
    INDEX idx_empresa_fecha (id_empresa, fecha),
    INDEX idx_fecha (fecha)
) ENGINE=InnoDB;

-- =========================================================
-- 14. AUDITORÍA (usuario o actor automático "Sistema")
-- =========================================================
CREATE TABLE auditoria (
    id_auditoria INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NULL,              -- NULL cuando actor_tipo = 'SISTEMA'
    actor_tipo ENUM('USUARIO','SISTEMA') NOT NULL DEFAULT 'USUARIO',
    accion VARCHAR(100) NOT NULL,
    modulo VARCHAR(50),
    entidad_tipo VARCHAR(50) NULL,     -- p.ej. 'colmena', 'medicion' (para filtrar por tipo)
    id_entidad BIGINT NULL,            -- p.ej. el id_colmena afectado, ya no como texto
    registro_afectado VARCHAR(100),    -- se conserva para no romper compatibilidad/legibilidad
    descripcion TEXT,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE SET NULL,
    CONSTRAINT chk_auditoria_actor CHECK ((actor_tipo='SISTEMA' AND id_usuario IS NULL) OR (actor_tipo='USUARIO' AND id_usuario IS NOT NULL)),
    INDEX idx_usuario (id_usuario),
    INDEX idx_fecha (fecha),
    INDEX idx_entidad (entidad_tipo, id_entidad)
) ENGINE=InnoDB;

-- =========================================================
-- 15. SOLICITUDES DE SUSCRIPCIÓN
-- =========================================================
CREATE TABLE solicitudes_suscripcion (
    id_solicitud INT AUTO_INCREMENT PRIMARY KEY,
    nombre_empresa VARCHAR(150) NOT NULL,
    nit VARCHAR(20) NOT NULL,
    correo_empresa VARCHAR(150) NOT NULL,
    telefono VARCHAR(30),
    direccion VARCHAR(255),
    nombre_contacto VARCHAR(120) NOT NULL,
    correo_contacto VARCHAR(150) NOT NULL,
    telefono_contacto VARCHAR(30),
    id_plan_solicitado INT NULL,
    tarjeta_falsa_ultimos4 VARCHAR(4) NULL,
    estado ENUM('PENDIENTE','APROBADA','RECHAZADA') NOT NULL DEFAULT 'PENDIENTE',
    observaciones TEXT,
    motivo_rechazo TEXT,
    id_usuario_revisor INT NULL,
    fecha_solicitud TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_revision DATETIME NULL,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_plan_solicitado) REFERENCES planes_suscripcion(id_plan) ON DELETE RESTRICT,
    FOREIGN KEY (id_usuario_revisor) REFERENCES usuarios(id_usuario) ON DELETE SET NULL,
    INDEX idx_solicitud_nit (nit),
    INDEX idx_solicitud_estado (estado)
) ENGINE=InnoDB;

-- =========================================================
-- TRIGGERS: reglas de negocio que cruzan varias tablas
-- =========================================================
DELIMITER $$

CREATE TRIGGER trg_usuarios_empresa_ins BEFORE INSERT ON usuarios
FOR EACH ROW
BEGIN
  DECLARE v_nombre_rol VARCHAR(50);
  SELECT nombre_rol INTO v_nombre_rol FROM roles WHERE id_rol = NEW.id_rol;
  IF v_nombre_rol IN ('Admin_ApiTech','Empleado_ApiTech') AND NEW.id_empresa IS NOT NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Usuarios ApiTech no deben tener id_empresa asignado';
  END IF;
  IF v_nombre_rol IN ('Admin_Cliente','Empleado_Cliente') AND NEW.id_empresa IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Usuarios cliente requieren id_empresa';
  END IF;
END$$

CREATE TRIGGER trg_usuarios_empresa_upd BEFORE UPDATE ON usuarios
FOR EACH ROW
BEGIN
  DECLARE v_nombre_rol VARCHAR(50);
  SELECT nombre_rol INTO v_nombre_rol FROM roles WHERE id_rol = NEW.id_rol;
  IF v_nombre_rol IN ('Admin_ApiTech','Empleado_ApiTech') AND NEW.id_empresa IS NOT NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Usuarios ApiTech no deben tener id_empresa asignado';
  END IF;
  IF v_nombre_rol IN ('Admin_Cliente','Empleado_Cliente') AND NEW.id_empresa IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Usuarios cliente requieren id_empresa';
  END IF;
END$$

CREATE TRIGGER trg_monitoreo_ins BEFORE INSERT ON monitoreo
FOR EACH ROW
BEGIN
  DECLARE v_asociacion_activa BOOLEAN;
  DECLARE v_sensor_estado VARCHAR(20);
  SELECT sc.activo, s.estado INTO v_asociacion_activa, v_sensor_estado
  FROM sensor_colmena sc JOIN sensores s ON s.id_sensor = sc.id_sensor
  WHERE sc.id_asociacion = NEW.id_asociacion;
  IF NEW.estado = 'activo' AND (v_asociacion_activa = FALSE OR v_sensor_estado <> 'activo') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se puede activar monitoreo: asociacion o sensor inactivos';
  END IF;
END$$

CREATE TRIGGER trg_monitoreo_upd BEFORE UPDATE ON monitoreo
FOR EACH ROW
BEGIN
  DECLARE v_asociacion_activa BOOLEAN;
  DECLARE v_sensor_estado VARCHAR(20);
  SELECT sc.activo, s.estado INTO v_asociacion_activa, v_sensor_estado
  FROM sensor_colmena sc JOIN sensores s ON s.id_sensor = sc.id_sensor
  WHERE sc.id_asociacion = NEW.id_asociacion;
  IF NEW.estado = 'activo' AND (v_asociacion_activa = FALSE OR v_sensor_estado <> 'activo') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se puede activar monitoreo: asociacion o sensor inactivos';
  END IF;
END$$

CREATE TRIGGER trg_alertas_empresa_bi BEFORE INSERT ON alertas
FOR EACH ROW
BEGIN
  DECLARE v_empresa INT;
  SELECT id_empresa INTO v_empresa FROM colmenas WHERE id_colmena = NEW.id_colmena;
  IF NEW.id_empresa IS NULL THEN SET NEW.id_empresa = v_empresa; END IF;
  IF NEW.id_empresa <> v_empresa THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La alerta debe pertenecer a la empresa de su colmena';
  END IF;
END$$

CREATE TRIGGER trg_alertas_empresa_bu BEFORE UPDATE ON alertas
FOR EACH ROW
BEGIN
  DECLARE v_empresa INT;
  SELECT id_empresa INTO v_empresa FROM colmenas WHERE id_colmena = NEW.id_colmena;
  IF NEW.id_empresa IS NULL OR NEW.id_empresa <> v_empresa THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La alerta debe pertenecer a la empresa de su colmena';
  END IF;
END$$

CREATE TRIGGER trg_prod_empresa_bi BEFORE INSERT ON produccion_miel
FOR EACH ROW
BEGIN
  DECLARE v_empresa INT;
  SELECT id_empresa INTO v_empresa FROM colmenas WHERE id_colmena = NEW.id_colmena;
  IF NEW.id_empresa IS NULL THEN SET NEW.id_empresa = v_empresa; END IF;
  IF NEW.id_empresa <> v_empresa THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La produccion debe pertenecer a la empresa de su colmena';
  END IF;
END$$

CREATE TRIGGER trg_prod_empresa_bu BEFORE UPDATE ON produccion_miel
FOR EACH ROW
BEGIN
  DECLARE v_empresa INT;
  SELECT id_empresa INTO v_empresa FROM colmenas WHERE id_colmena = NEW.id_colmena;
  IF NEW.id_empresa IS NULL OR NEW.id_empresa <> v_empresa THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La produccion debe pertenecer a la empresa de su colmena';
  END IF;
END$$

CREATE TRIGGER trg_notif_empresa_bi BEFORE INSERT ON notificaciones
FOR EACH ROW
BEGIN
  DECLARE v_empresa INT;
  SELECT id_empresa INTO v_empresa FROM usuarios WHERE id_usuario = NEW.id_usuario;
  IF v_empresa IS NULL AND NEW.id_empresa IS NOT NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Un usuario ApiTech no puede recibir notificacion de empresa';
  END IF;
  IF v_empresa IS NOT NULL AND NEW.id_empresa IS NULL THEN SET NEW.id_empresa = v_empresa; END IF;
  IF v_empresa IS NOT NULL AND NEW.id_empresa <> v_empresa THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La notificacion debe pertenecer a la empresa del usuario';
  END IF;
END$$

CREATE TRIGGER trg_notif_empresa_bu BEFORE UPDATE ON notificaciones
FOR EACH ROW
BEGIN
  DECLARE v_empresa INT;
  SELECT id_empresa INTO v_empresa FROM usuarios WHERE id_usuario = NEW.id_usuario;
  IF v_empresa IS NULL AND NEW.id_empresa IS NOT NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Un usuario ApiTech no puede recibir notificacion de empresa';
  END IF;
  IF v_empresa IS NOT NULL AND (NEW.id_empresa IS NULL OR NEW.id_empresa <> v_empresa) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La notificacion debe pertenecer a la empresa del usuario';
  END IF;
END$$

CREATE TRIGGER trg_umbral_bi BEFORE INSERT ON rangos_umbrales
FOR EACH ROW
BEGIN
  IF NEW.valor_min >= NEW.valor_max THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'valor_min debe ser menor que valor_max';
  END IF;
END$$

CREATE TRIGGER trg_umbral_bu BEFORE UPDATE ON rangos_umbrales
FOR EACH ROW
BEGIN
  IF NEW.valor_min >= NEW.valor_max THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'valor_min debe ser menor que valor_max';
  END IF;
END$$

CREATE TRIGGER trg_mediciones_coherencia_bi BEFORE INSERT ON mediciones
FOR EACH ROW
BEGIN
  DECLARE v_tipo ENUM('TEMPERATURA','HUMEDAD','PESO');
  DECLARE v_simulado BOOLEAN;
  DECLARE v_monitoreo INT;
  SELECT COUNT(*) INTO v_monitoreo FROM monitoreo WHERE id_asociacion=NEW.id_asociacion AND estado='activo';
  IF v_monitoreo=0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='No se puede registrar medicion sin monitoreo activo'; END IF;
  SELECT s.tipo, s.es_simulado INTO v_tipo, v_simulado
  FROM sensor_colmena sc JOIN sensores s ON s.id_sensor=sc.id_sensor
  WHERE sc.id_asociacion=NEW.id_asociacion;
  IF v_tipo IS NULL OR v_tipo <> NEW.tipo_medicion THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='El tipo de medicion no coincide con el sensor asociado';
  END IF;
  IF (v_simulado=TRUE AND NEW.origen<>'SIMULADO') OR (v_simulado=FALSE AND NEW.origen<>'REAL') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='El origen de la medicion no coincide con el sensor';
  END IF;
END$$

CREATE TRIGGER trg_mediciones_coherencia_bu BEFORE UPDATE ON mediciones
FOR EACH ROW
BEGIN
  DECLARE v_tipo ENUM('TEMPERATURA','HUMEDAD','PESO');
  DECLARE v_simulado BOOLEAN;
  DECLARE v_monitoreo INT;
  SELECT COUNT(*) INTO v_monitoreo FROM monitoreo WHERE id_asociacion=NEW.id_asociacion AND estado='activo';
  IF v_monitoreo=0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='No se puede registrar medicion sin monitoreo activo'; END IF;
  SELECT s.tipo, s.es_simulado INTO v_tipo, v_simulado
  FROM sensor_colmena sc JOIN sensores s ON s.id_sensor=sc.id_sensor
  WHERE sc.id_asociacion=NEW.id_asociacion;
  IF v_tipo IS NULL OR v_tipo <> NEW.tipo_medicion THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='El tipo de medicion no coincide con el sensor asociado';
  END IF;
  IF (v_simulado=TRUE AND NEW.origen<>'SIMULADO') OR (v_simulado=FALSE AND NEW.origen<>'REAL') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='El origen de la medicion no coincide con el sensor';
  END IF;
END$$

CREATE TRIGGER trg_alertas_integridad_bi BEFORE INSERT ON alertas
FOR EACH ROW
BEGIN
  DECLARE v_empresa INT; DECLARE v_sensor INT; DECLARE v_colmena INT;
  SELECT id_empresa INTO v_empresa FROM colmenas WHERE id_colmena=NEW.id_colmena;
  IF NEW.id_empresa<>v_empresa THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Empresa de alerta incompatible con colmena'; END IF;
  IF NEW.id_sensor IS NOT NULL THEN
    SELECT id_sensor, id_colmena INTO v_sensor, v_colmena FROM sensor_colmena WHERE id_sensor=NEW.id_sensor AND id_colmena=NEW.id_colmena AND activo=TRUE LIMIT 1;
    IF v_sensor IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Sensor de alerta incompatible con colmena'; END IF;
  END IF;
  IF NEW.id_medicion IS NOT NULL THEN
    SELECT sc.id_sensor, sc.id_colmena INTO v_sensor, v_colmena
    FROM mediciones m JOIN sensor_colmena sc ON sc.id_asociacion=m.id_asociacion
    WHERE m.id_medicion=NEW.id_medicion;
    IF v_colmena<>NEW.id_colmena OR (NEW.id_sensor IS NOT NULL AND v_sensor<>NEW.id_sensor) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Medicion de alerta incompatible con sensor o colmena';
    END IF;
  END IF;
END$$

CREATE TRIGGER trg_alertas_integridad_bu BEFORE UPDATE ON alertas
FOR EACH ROW
BEGIN
  DECLARE v_empresa INT; DECLARE v_sensor INT; DECLARE v_colmena INT;
  SELECT id_empresa INTO v_empresa FROM colmenas WHERE id_colmena=NEW.id_colmena;
  IF NEW.id_empresa<>v_empresa THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Empresa de alerta incompatible con colmena'; END IF;
  IF NEW.id_sensor IS NOT NULL THEN
    SELECT id_sensor, id_colmena INTO v_sensor, v_colmena FROM sensor_colmena WHERE id_sensor=NEW.id_sensor AND id_colmena=NEW.id_colmena AND activo=TRUE LIMIT 1;
    IF v_sensor IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Sensor de alerta incompatible con colmena'; END IF;
  END IF;
  IF NEW.id_medicion IS NOT NULL THEN
    SELECT sc.id_sensor, sc.id_colmena INTO v_sensor, v_colmena FROM mediciones m JOIN sensor_colmena sc ON sc.id_asociacion=m.id_asociacion WHERE m.id_medicion=NEW.id_medicion;
    IF v_colmena<>NEW.id_colmena OR (NEW.id_sensor IS NOT NULL AND v_sensor<>NEW.id_sensor) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Medicion de alerta incompatible con sensor o colmena'; END IF;
  END IF;
END$$

CREATE TRIGGER trg_auditoria_actor_bi BEFORE INSERT ON auditoria
FOR EACH ROW
BEGIN
  IF (NEW.actor_tipo='SISTEMA' AND NEW.id_usuario IS NOT NULL) OR (NEW.actor_tipo='USUARIO' AND NEW.id_usuario IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Actor de auditoria incompatible con id_usuario';
  END IF;
END$$

CREATE TRIGGER trg_notificaciones_referencias_bi BEFORE INSERT ON notificaciones
FOR EACH ROW
BEGIN
  DECLARE v_empresa INT;
  IF NEW.id_alerta IS NOT NULL THEN
    SELECT id_empresa INTO v_empresa FROM alertas WHERE id_alerta=NEW.id_alerta;
    IF NEW.id_empresa IS NULL OR NEW.id_empresa<>v_empresa THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Alerta de notificacion incompatible con su empresa'; END IF;
  END IF;
  IF NEW.id_pago IS NOT NULL AND NEW.id_empresa IS NOT NULL THEN
    SELECT id_empresa INTO v_empresa FROM pagos WHERE id_pago=NEW.id_pago;
    IF v_empresa<>NEW.id_empresa THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Pago de notificacion incompatible con su empresa'; END IF;
  END IF;
END$$

CREATE TRIGGER trg_notificaciones_referencias_bu BEFORE UPDATE ON notificaciones
FOR EACH ROW
BEGIN
  DECLARE v_empresa INT;
  IF NEW.id_alerta IS NOT NULL THEN
    SELECT id_empresa INTO v_empresa FROM alertas WHERE id_alerta=NEW.id_alerta;
    IF NEW.id_empresa IS NULL OR NEW.id_empresa<>v_empresa THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Alerta de notificacion incompatible con su empresa'; END IF;
  END IF;
  IF NEW.id_pago IS NOT NULL AND NEW.id_empresa IS NOT NULL THEN
    SELECT id_empresa INTO v_empresa FROM pagos WHERE id_pago=NEW.id_pago;
    IF v_empresa<>NEW.id_empresa THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Pago de notificacion incompatible con su empresa'; END IF;
  END IF;
END$$

CREATE TRIGGER trg_sensores_estado_bu BEFORE UPDATE ON sensores
FOR EACH ROW
BEGIN
  IF NEW.estado<>'activo' AND EXISTS (SELECT 1 FROM sensor_colmena sc JOIN monitoreo m ON m.id_asociacion=sc.id_asociacion WHERE sc.id_sensor=NEW.id_sensor AND m.estado='activo') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='No se puede desactivar un sensor con monitoreo activo';
  END IF;
END$$

CREATE TRIGGER trg_sensor_colmena_estado_bu BEFORE UPDATE ON sensor_colmena
FOR EACH ROW
BEGIN
  IF NEW.activo=FALSE AND EXISTS (SELECT 1 FROM monitoreo WHERE id_asociacion=NEW.id_asociacion AND estado='activo') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='No se puede desactivar una asociacion con monitoreo activo';
  END IF;
END$$

CREATE TRIGGER trg_pagos_verificador_bi BEFORE INSERT ON pagos
FOR EACH ROW
BEGIN
  DECLARE v_rol VARCHAR(50);
  IF NEW.id_usuario_verifico IS NOT NULL THEN
    SELECT r.nombre_rol INTO v_rol FROM usuarios u JOIN roles r ON r.id_rol=u.id_rol WHERE u.id_usuario=NEW.id_usuario_verifico;
    IF v_rol NOT IN ('Admin_ApiTech','Empleado_ApiTech') THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Solo ApiTech puede verificar pagos'; END IF;
  END IF;
END$$

CREATE TRIGGER trg_pagos_verificador_bu BEFORE UPDATE ON pagos
FOR EACH ROW
BEGIN
  DECLARE v_rol VARCHAR(50);
  IF NEW.id_usuario_verifico IS NOT NULL THEN
    SELECT r.nombre_rol INTO v_rol FROM usuarios u JOIN roles r ON r.id_rol=u.id_rol WHERE u.id_usuario=NEW.id_usuario_verifico;
    IF v_rol NOT IN ('Admin_ApiTech','Empleado_ApiTech') THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Solo ApiTech puede verificar pagos'; END IF;
  END IF;
END$$

CREATE TRIGGER trg_monitoreo_empresa_bi BEFORE INSERT ON monitoreo
FOR EACH ROW
BEGIN
  DECLARE v_activo BOOLEAN; DECLARE v_sensor_estado VARCHAR(20); DECLARE v_colmena_estado VARCHAR(50);
  SELECT sc.activo,s.estado,c.estado INTO v_activo,v_sensor_estado,v_colmena_estado
  FROM sensor_colmena sc JOIN sensores s ON s.id_sensor=sc.id_sensor JOIN colmenas c ON c.id_colmena=sc.id_colmena
  WHERE sc.id_asociacion=NEW.id_asociacion;
  IF NEW.estado='activo' AND (v_activo IS NULL OR v_activo=FALSE OR v_sensor_estado<>'activo' OR v_colmena_estado<>'Estable') THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Monitoreo requiere asociacion, sensor y colmena activos'; END IF;
END$$

CREATE TRIGGER trg_monitoreo_empresa_bu BEFORE UPDATE ON monitoreo
FOR EACH ROW
BEGIN
  DECLARE v_activo BOOLEAN; DECLARE v_sensor_estado VARCHAR(20); DECLARE v_colmena_estado VARCHAR(50);
  SELECT sc.activo,s.estado,c.estado INTO v_activo,v_sensor_estado,v_colmena_estado
  FROM sensor_colmena sc JOIN sensores s ON s.id_sensor=sc.id_sensor JOIN colmenas c ON c.id_colmena=sc.id_colmena
  WHERE sc.id_asociacion=NEW.id_asociacion;
  IF NEW.estado='activo' AND (v_activo IS NULL OR v_activo=FALSE OR v_sensor_estado<>'activo' OR v_colmena_estado<>'Estable') THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Monitoreo requiere asociacion, sensor y colmena activos'; END IF;
END$$

DELIMITER ;


CREATE USER 'apitech'@'localhost' IDENTIFIED BY 'ApiTech2026*';
GRANT ALL PRIVILEGES ON apitech.* TO 'apitech'@'localhost';
FLUSH PRIVILEGES;
CHECK TABLE mysql.global_priv;
REPAIR TABLE mysql.global_priv;
SELECT VERSION();

REPAIR TABLE mysql.user;
REPAIR TABLE mysql.db;
REPAIR TABLE mysql.tables_priv;
REPAIR TABLE mysql.columns_priv;
REPAIR TABLE mysql.procs_priv;
REPAIR TABLE mysql.proxies_priv;
REPAIR TABLE mysql.global_priv;
REPAIR TABLE mysql.roles_mapping;
FLUSH PRIVILEGES;

CREATE USER 'apitech'@'localhost' IDENTIFIED BY 'ApiTech2026*';
GRANT ALL PRIVILEGES ON apitech.* TO 'apitech'@'localhost';
FLUSH PRIVILEGES;
SELECT User, Host FROM mysql.user WHERE User = 'apitech';
DROP USER 'apitech'@'localhost';
FLUSH PRIVILEGES;

CREATE USER 'apitech'@'localhost' IDENTIFIED BY 'ApiTech2026*';
GRANT ALL PRIVILEGES ON apitech.* TO 'apitech'@'localhost';
FLUSH PRIVILEGES;




USE apitech;

INSERT INTO roles (nombre_rol, descripcion) VALUES
  ('Admin_ApiTech', 'Administrador de la plataforma ApiTech (sin empresa asociada)'),
  ('Empleado_ApiTech', 'Empleado operativo de ApiTech (sin empresa asociada)'),
  ('Admin_Cliente', 'Administrador de una empresa cliente'),
  ('Empleado_Cliente', 'Empleado operativo de una empresa cliente');

INSERT INTO permisos (codigo_permiso, descripcion) VALUES
  ('GESTIONAR_USUARIOS', 'Permite crear, editar y eliminar usuarios de la propia empresa');

INSERT INTO empresas (nit, nombre_empresa, correo, telefono, direccion, estado) VALUES
  ('900123456-7', 'Apicola El Panal S.A.S', 'contacto@elpanal.com', '3001234567', 'Cra 10 # 20-30, Bogota', 'activa');

-- =========================================================
-- PLANES COMERCIALES OFICIALES — SOLO DOS
-- =========================================================
INSERT INTO planes_suscripcion (nombre_plan, precio, duracion_dias, estado) VALUES
('Plan Básico', 49000.00, 30, 'activo'),
('Plan Profesional', 99000.00, 30, 'activo');
