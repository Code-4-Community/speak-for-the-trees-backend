CREATE TABLE IF NOT EXISTS verification_keys (
    id      VARCHAR(50) PRIMARY KEY,
    user_id INT NOT NULL,
    used    BOOLEAN DEFAULT false,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT verification_keys_user_id_fk FOREIGN KEY (user_id) REFERENCES users(id)
);

ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN DEFAULT false
