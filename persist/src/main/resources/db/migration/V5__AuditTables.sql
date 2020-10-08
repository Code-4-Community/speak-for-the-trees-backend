CREATE TABLE IF NOT EXISTS audit (
    transaction_type VARCHAR(1000) NOT NULL,
    table_name       VARCHAR(1000) NOT NULL,
    user_id          INTEGER NOT NULL,
    result           VARCHAR (1000) NOT NULL,
    timestamp        TIMESTAMP NOT NULL
);
