CREATE TABLE IF NOT EXISTS deleted_users
(
    id                SERIAL PRIMARY KEY,
    first_name        VARCHAR(36),
    last_name         VARCHAR(36),
    username          VARCHAR(36),
    email             VARCHAR(36) UNIQUE NOT NULL,
    pass_hash         BYTEA              NOT NULL,
    privilege_level   INTEGER            NOT NULL DEFAULT 0,
    email_verified    BOOLEAN                     DEFAULT false,
    deleted_timestamp TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS deleted_team
(
    id                   SERIAL PRIMARY KEY,
    name                 VARCHAR(36) NOT NULL,
    bio                  TEXT        NOT NULL,
    goal                 INT,
    goal_completion_date TIMESTAMP,
    created_timestamp    TIMESTAMP   NOT NULL,
    deleted_timestamp    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);