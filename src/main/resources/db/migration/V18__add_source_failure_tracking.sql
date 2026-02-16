ALTER TABLE sources ADD COLUMN consecutive_failures INTEGER NOT NULL DEFAULT 0;
ALTER TABLE sources ADD COLUMN last_failure_type TEXT;
ALTER TABLE sources ADD COLUMN disabled_reason TEXT;
