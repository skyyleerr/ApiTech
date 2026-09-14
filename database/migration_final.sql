-- ApiTech MK5 - migración final para una base existente
-- Ejecutar después de schema.sql si la base ya fue creada.
USE apitech;

-- Solicitudes (el schema definitivo ya la crea; este bloque es para instalaciones anteriores).
CREATE TABLE IF NOT EXISTS solicitudes_suscripcion (
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
    FOREIGN KEY (id_usuario_revisor) REFERENCES usuarios(id_usuario) ON DELETE SET NULL
) ENGINE=InnoDB;

-- Si la tabla ya existía, añade únicamente la columna nueva.
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'solicitudes_suscripcion'
      AND COLUMN_NAME = 'tarjeta_falsa_ultimos4'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE solicitudes_suscripcion ADD COLUMN tarjeta_falsa_ultimos4 VARCHAR(4) NULL AFTER id_plan_solicitado',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Solo dos planes visibles/comerciales.
UPDATE planes_suscripcion
SET estado = 'inactivo'
WHERE LOWER(nombre_plan) NOT IN ('plan básico','plan profesional','plan basico','plan profesional');

INSERT INTO planes_suscripcion (nombre_plan, precio, duracion_dias, estado)
SELECT 'Plan Básico', 49000.00, 30, 'activo'
WHERE NOT EXISTS (SELECT 1 FROM planes_suscripcion WHERE LOWER(nombre_plan) IN ('plan básico','plan basico'));

INSERT INTO planes_suscripcion (nombre_plan, precio, duracion_dias, estado)
SELECT 'Plan Profesional', 99000.00, 30, 'activo'
WHERE NOT EXISTS (SELECT 1 FROM planes_suscripcion WHERE LOWER(nombre_plan) = 'plan profesional');
