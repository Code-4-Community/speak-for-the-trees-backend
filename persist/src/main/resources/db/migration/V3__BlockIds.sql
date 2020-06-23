DROP TABLE block;
CREATE TABLE IF NOT EXISTS block (
    id VARCHAR(8) PRIMARY KEY,
    status INTEGER NOT NULL DEFAULT 0,
    assigned_to INTEGER REFERENCES users(id),
    updated_timestamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
