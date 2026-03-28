-- V3__Add_password_to_users.sql
-- Add password column for local authentication

ALTER TABLE users ADD COLUMN IF NOT EXISTS password VARCHAR(255);