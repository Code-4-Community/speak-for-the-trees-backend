ALTER TABLE block
    ADD COLUMN neighborhoodId int references neighborhood(id);
