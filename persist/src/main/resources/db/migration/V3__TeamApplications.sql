CREATE TABLE IF NOT EXISTS team_applicants (
    id SERIAL PRIMARY KEY,
    team_id INTEGER REFERENCES team(id),
    user_id INTEGER REFERENCES users(id)
)
