-- V2__Add_audit_columns.sql
-- Add audit columns for JPA auditing

ALTER TABLE users ADD COLUMN created_by VARCHAR(255);
ALTER TABLE users ADD COLUMN updated_by VARCHAR(255);

ALTER TABLE oauth_tokens ADD COLUMN created_by VARCHAR(255);
ALTER TABLE oauth_tokens ADD COLUMN updated_by VARCHAR(255);

ALTER TABLE playlists ADD COLUMN created_by VARCHAR(255);
ALTER TABLE playlists ADD COLUMN updated_by VARCHAR(255);

ALTER TABLE tracks ADD COLUMN created_by VARCHAR(255);
ALTER TABLE tracks ADD COLUMN updated_by VARCHAR(255);

