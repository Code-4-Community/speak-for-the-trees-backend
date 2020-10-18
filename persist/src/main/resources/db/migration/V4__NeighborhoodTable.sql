CREATE TABLE IF NOT EXISTS neighborhood (
    id INT PRIMARY KEY,
    Name VARCHAR(36) NOT NULL,
    SqMiles DOUBLE PRECISION
);

-- CREATE TABLE IF NOT EXISTS neighborhood_polygon_point (
--     point_id SERIAL PRIMARY KEY,
--     latitude INT NOT NULL,
--     longitude INT NOT NULL,
--     neighborhood_id INTEGER REFERENCES neighborhood(id)
-- );

