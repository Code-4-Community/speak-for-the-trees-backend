CREATE TABLE IF NOT EXISTS blacklisted_refreshes (
    refresh_hash VARCHAR(64) PRIMARY KEY,
    expires TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(36),
    last_name VARCHAR(36),
    email VARCHAR(36) UNIQUE NOT NULL,
    pass_hash BYTEA NOT NULL,
    privilege_level INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS team (
    id SERIAL PRIMARY KEY,
    name VARCHAR(36) NOT NULL
);

CREATE TABLE IF NOT EXISTS user_team (
    user_id INTEGER REFERENCES users(id),
    team_id INTEGER REFERENCES team(id)
);

CREATE TABLE IF NOT EXISTS block (
    fid VARCHAR(36) PRIMARY KEY,
    status INTEGER NOT NULL,
    assigned_to INTEGER REFERENCES users(id)
)

