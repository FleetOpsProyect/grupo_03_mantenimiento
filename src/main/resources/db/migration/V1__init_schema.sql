-- ====================================================================
-- Script de Migración Inicial Flyway V1
-- Módulo: Microservicio de Mantenimiento (FleetOps)
-- Basado en el Modelo ER y Requerimientos del DAS (Grupo 03)
-- ====================================================================

-- Habilitar extensión para generación de UUIDs si no existe
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Tabla de Técnicos de Mantenimiento
CREATE TABLE IF NOT EXISTS technician (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabla de Reglas Preventivas Multicriterio (HU-01)
CREATE TABLE IF NOT EXISTS preventive_rule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scope_type VARCHAR(50) NOT NULL, -- 'GLOBAL', 'CATEGORY', 'VEHICLE'
    vehicle_category VARCHAR(50),   -- 'CAMIONETA', 'AUTOMOVIL', 'CARGA_PESADA'
    vehicle_id UUID,
    mileage_interval_km INT NOT NULL,
    time_interval_days INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices únicos condicionales para evitar duplicidad de reglas activas (HU-01)
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_global_rule 
    ON preventive_rule (scope_type) 
    WHERE scope_type = 'GLOBAL' AND is_active = true;

CREATE UNIQUE INDEX IF NOT EXISTS uq_active_category_rule 
    ON preventive_rule (scope_type, vehicle_category) 
    WHERE scope_type = 'CATEGORY' AND is_active = true;

CREATE UNIQUE INDEX IF NOT EXISTS uq_active_vehicle_rule 
    ON preventive_rule (scope_type, vehicle_id) 
    WHERE scope_type = 'VEHICLE' AND is_active = true;

-- 3. Tabla de Órdenes de Mantenimiento (HU-02, HU-03, HU-04, HU-05, HU-06, HU-08)
CREATE TABLE IF NOT EXISTS maintenance_order (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID NOT NULL,
    technician_id UUID REFERENCES technician(id) ON DELETE SET NULL,
    incident_id UUID,
    maintenance_type VARCHAR(50) NOT NULL, -- 'PREVENTIVE', 'CORRECTIVE'
    status VARCHAR(50) NOT NULL,           -- 'SCHEDULED_PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'
    origin VARCHAR(50) NOT NULL,           -- 'SCHEDULER', 'INCIDENT', 'MANUAL'
    current_mileage INT,
    description TEXT,
    completed_by_type VARCHAR(50),
    completed_by_id VARCHAR(100),
    total_cost NUMERIC(12, 2) DEFAULT 0.00,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_order_vehicle_id ON maintenance_order (vehicle_id);
CREATE INDEX IF NOT EXISTS idx_order_technician_id ON maintenance_order (technician_id);
CREATE INDEX IF NOT EXISTS idx_order_status ON maintenance_order (status);
CREATE INDEX IF NOT EXISTS idx_order_created_at ON maintenance_order (created_at DESC);

-- Índice único condicional: Idempotencia en órdenes preventivas activas (DAS y HU-02)
CREATE UNIQUE INDEX IF NOT EXISTS uq_preventive_active_order 
    ON maintenance_order (vehicle_id) 
    WHERE status IN ('SCHEDULED_PENDING', 'IN_PROGRESS') AND maintenance_type = 'PREVENTIVE';

-- Índice único condicional: Idempotencia en órdenes correctivas por incidente (HU-04)
CREATE UNIQUE INDEX IF NOT EXISTS uq_corrective_incident_order 
    ON maintenance_order (incident_id) 
    WHERE incident_id IS NOT NULL;

-- 4. Tabla de Bitácora / Avances de Mantenimiento (HU-09)
CREATE TABLE IF NOT EXISTS maintenance_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    maintenance_order_id UUID NOT NULL REFERENCES maintenance_order(id) ON DELETE CASCADE,
    author_type VARCHAR(50) NOT NULL,
    author_id VARCHAR(100) NOT NULL,
    author_name VARCHAR(255) NOT NULL,
    log_type VARCHAR(50) NOT NULL, -- 'REVISION', 'PROGRESS', 'INCIDENT_UPDATE', 'TECHNICIAN_ASSIGNMENT'
    notes TEXT NOT NULL,
    cost_delta NUMERIC(12, 2) DEFAULT 0.00,
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_log_order_id ON maintenance_log (maintenance_order_id);

-- 5. Tabla de Adjuntos a la Bitácora
CREATE TABLE IF NOT EXISTS maintenance_log_attachment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    maintenance_log_id UUID NOT NULL REFERENCES maintenance_log(id) ON DELETE CASCADE,
    file_url VARCHAR(1000) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100),
    file_size_bytes BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_attachment_log_id ON maintenance_log_attachment (maintenance_log_id);

-- 6. Tabla de Transactional Outbox (Confiabilidad y Resiliencia en Eventos y Llamadas)
CREATE TABLE IF NOT EXISTS outbox_message (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    maintenance_order_id UUID REFERENCES maintenance_order(id) ON DELETE SET NULL,
    message_type VARCHAR(50) NOT NULL, -- 'EVENT', 'VEHICLE_CALL'
    destination VARCHAR(255) NOT NULL, -- routing key o URI del recurso
    payload JSONB NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'SENT', 'FAILED'
    attempts INT NOT NULL DEFAULT 0,
    last_error TEXT,
    next_attempt_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_outbox_pending ON outbox_message (status, next_attempt_at) 
    WHERE status = 'PENDING';

-- 7. Tabla de Auditoría de Acceso y Operaciones Críticas (DAS ISO 25010 Trazabilidad)
CREATE TABLE IF NOT EXISTS access_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_type VARCHAR(50),
    actor_id VARCHAR(100),
    actor_email VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    resource_accessed VARCHAR(255) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    ip_address VARCHAR(100),
    user_agent VARCHAR(500),
    status_result VARCHAR(50) NOT NULL,
    details JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_created_at ON access_audit_log (created_at DESC);
