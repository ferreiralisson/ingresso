ALTER TABLE issued_tickets ADD COLUMN used_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE issued_tickets ADD COLUMN used_by_user_id BIGINT;
ALTER TABLE issued_tickets ADD COLUMN correction_used BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE issued_tickets
    ADD CONSTRAINT fk_issued_tickets_used_by FOREIGN KEY (used_by_user_id) REFERENCES usuarios(id);

CREATE INDEX idx_issued_tickets_usage ON issued_tickets (used_at, used_by_user_id);