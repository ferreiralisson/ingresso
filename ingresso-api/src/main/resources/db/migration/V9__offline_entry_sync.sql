ALTER TABLE entry_check_ins ADD COLUMN device_id VARCHAR(64);
ALTER TABLE entry_check_ins ADD COLUMN offline_manifest_id VARCHAR(36);

ALTER TABLE entry_check_ins
    ADD CONSTRAINT fk_entry_check_ins_manifest FOREIGN KEY (offline_manifest_id) REFERENCES offline_entry_manifests(id);

CREATE INDEX idx_entry_check_ins_manifest ON entry_check_ins (offline_manifest_id, synchronized_at);