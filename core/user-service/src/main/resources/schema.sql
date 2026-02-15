CREATE SCHEMA IF NOT EXISTS user_service;

CREATE TABLE IF NOT EXISTS users
(
    id    BIGINT PRIMARY KEY,
    name  VARCHAR(255)        NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_name ON users (name);
CREATE INDEX IF NOT EXISTS idx_email ON users (email);