ALTER TABLE coding_sessions
    ADD COLUMN IF NOT EXISTS clarification_questions VARCHAR(4000),
    ADD COLUMN IF NOT EXISTS planned_approach VARCHAR(4000),
    ADD COLUMN IF NOT EXISTS expected_time_complexity VARCHAR(255),
    ADD COLUMN IF NOT EXISTS expected_space_complexity VARCHAR(255);
