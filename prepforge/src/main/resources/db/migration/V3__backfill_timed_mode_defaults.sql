UPDATE coding_sessions
SET timed_mode = FALSE
WHERE timed_mode IS NULL;

ALTER TABLE coding_sessions
    ALTER COLUMN timed_mode SET DEFAULT FALSE,
    ALTER COLUMN timed_mode SET NOT NULL;
