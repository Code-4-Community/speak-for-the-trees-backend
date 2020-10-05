ALTER TABLE users
ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users
    ADD COLUMN deleted_timestamp timestamp;
ALTER TABLE team
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE team
    ADD COLUMN deleted_timestamp timestamp;

