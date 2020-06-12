
ALTER TABLE user_team
    ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE user_team
    ALTER COLUMN team_id SET NOT NULL;


ALTER TABLE user_team
    ADD PRIMARY KEY (user_id, team_id);
