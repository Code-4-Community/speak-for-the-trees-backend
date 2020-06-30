ALTER TABLE block
    ADD COLUMN last_reserved timestamp;

ALTER TABLE block
    ADD COLUMN last_completed timestamp;
