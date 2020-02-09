CREATE TABLE IF NOT EXISTS blacklisted_refreshes (
    refresh_hash VARCHAR(64) PRIMARY KEY,
    expires TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS note_user (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(36),
    last_name VARCHAR(36),
    user_name VARCHAR(36) UNIQUE NOT NULL,
    email VARCHAR(36) UNIQUE NOT NULL,
    pass_hash VARCHAR(126) NOT NULL
);