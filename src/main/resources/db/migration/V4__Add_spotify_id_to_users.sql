-- V4__Add_spotify_id_to_users.sql
-- spotify_id already exists in V1 but making sure it's indexed

CREATE INDEX IF NOT EXISTS idx_users_spotify_id ON users(spotify_id);