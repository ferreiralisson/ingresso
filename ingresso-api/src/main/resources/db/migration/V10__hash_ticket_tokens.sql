ALTER TABLE issued_tickets ADD COLUMN qr_token_hash VARCHAR(64);

UPDATE issued_tickets
SET qr_token_hash = LOWER(RAWTOHEX(HASH('SHA-256', STRINGTOUTF8(qr_token))));

ALTER TABLE issued_tickets ALTER COLUMN qr_token_hash SET NOT NULL;
ALTER TABLE issued_tickets ADD CONSTRAINT uq_issued_tickets_qr_hash UNIQUE (qr_token_hash);