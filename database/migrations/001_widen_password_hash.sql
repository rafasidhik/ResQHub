-- Migration 001 (REQUIRED for databases created by the desktop version)
-- Widens users.password_hash from CHAR(64) to VARCHAR(255) so the web
-- application can store salted PBKDF2 hashes.
--
-- Existing accounts are NOT modified and keep working: their old SHA-256
-- digests are still accepted and are automatically upgraded to the salted
-- format the next time each user logs in successfully.
-- Safe to re-run.
USE resqhub_web;
ALTER TABLE users
    MODIFY COLUMN password_hash VARCHAR(255) NOT NULL
    COMMENT 'PBKDF2 salted hash (legacy: SHA-256 hex)';
