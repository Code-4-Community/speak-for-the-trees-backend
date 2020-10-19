CREATE TABLE IF NOT EXISTS trees (
    id SERIAL PRIMARY KEY NOT NULL,
    street_ad VARCHAR(36) NOT NULL,
    zip_code VARCHAR(5) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(36),
    tree_present BOOLEAN NOT NULL DEFAULT false,
    genus VARCHAR(36) NOT NULL,
    species VARCHAR(36) NOT NULL,
    common_name VARCHAR(36) NOT NULL,
    diameter FLOAT,
    latitude FLOAT NOT NULL,
    longitude FLOAT NOT NULL
);
