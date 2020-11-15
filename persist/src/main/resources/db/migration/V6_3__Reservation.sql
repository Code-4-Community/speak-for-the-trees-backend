CREATE TABLE IF NOT EXISTS reservation (
        id SERIAL PRIMARY KEY,
        user_id INTEGER REFERENCES users(id),
        block_id VARCHAR(8) REFERENCES block(id),
        completed BOOLEAN DEFAULT FALSE,
        date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
