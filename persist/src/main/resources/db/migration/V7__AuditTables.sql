CREATE TABLE IF NOT EXISTS audit
(
    transaction_type VARCHAR(10)   NOT NULL,
    table_name       VARCHAR(10)   NOT NULL,
    user_id          INTEGER       NOT NULL,
    old_value        VARCHAR(1000),
    result           VARCHAR(1000) NOT NULL,
    timestamp        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
