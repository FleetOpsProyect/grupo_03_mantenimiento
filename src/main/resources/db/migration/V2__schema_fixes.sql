-- Migración V2: Correcciones de columnas identificadas en Code Review
-- Añade columnas para persistir cancel_reason, assigned_workshop, routine_description y metadatos de mensajería outbox

ALTER TABLE maintenance_order 
    ADD COLUMN IF NOT EXISTS cancel_reason TEXT,
    ADD COLUMN IF NOT EXISTS assigned_workshop VARCHAR(255);

ALTER TABLE preventive_rule 
    ADD COLUMN IF NOT EXISTS routine_description TEXT;

ALTER TABLE outbox_message 
    ADD COLUMN IF NOT EXISTS aggregate_type VARCHAR(100),
    ADD COLUMN IF NOT EXISTS exchange VARCHAR(255),
    ADD COLUMN IF NOT EXISTS routing_key VARCHAR(255);
