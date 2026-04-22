-- =========================
-- V5: Behavioral Module
-- PostgreSQL-safe Flyway migration
-- =========================

-- 1) behavioral_questions
CREATE TABLE IF NOT EXISTS behavioral_questions (
                                                    id UUID PRIMARY KEY,
                                                    question_text VARCHAR(4000),
    category VARCHAR(255),
    difficulty VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW()
    );

-- Rename old column if needed
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'behavioral_questions'
          AND column_name = 'prompt'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'behavioral_questions'
          AND column_name = 'question_text'
    ) THEN
ALTER TABLE behavioral_questions
    RENAME COLUMN prompt TO question_text;
END IF;
END $$;

ALTER TABLE behavioral_questions
    ADD COLUMN IF NOT EXISTS question_text VARCHAR(4000),
    ADD COLUMN IF NOT EXISTS category VARCHAR(255),
    ADD COLUMN IF NOT EXISTS difficulty VARCHAR(50),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW();

ALTER TABLE behavioral_questions
ALTER COLUMN question_text TYPE VARCHAR(4000);

-- 2) behavioral_sessions
CREATE TABLE IF NOT EXISTS behavioral_sessions (
                                                   id UUID PRIMARY KEY,
                                                   user_id UUID,
                                                   question_id UUID,
                                                   started_at TIMESTAMP DEFAULT NOW(),
    status VARCHAR(50)
    );

ALTER TABLE behavioral_sessions
    ADD COLUMN IF NOT EXISTS user_id UUID,
    ADD COLUMN IF NOT EXISTS question_id UUID,
    ADD COLUMN IF NOT EXISTS started_at TIMESTAMP DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS status VARCHAR(50);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'behavioral_sessions'
          AND constraint_name = 'fk_behavioral_sessions_user'
    ) THEN
ALTER TABLE behavioral_sessions
    ADD CONSTRAINT fk_behavioral_sessions_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'behavioral_sessions'
          AND constraint_name = 'fk_behavioral_sessions_question'
    ) THEN
ALTER TABLE behavioral_sessions
    ADD CONSTRAINT fk_behavioral_sessions_question
        FOREIGN KEY (question_id) REFERENCES behavioral_questions(id) ON DELETE CASCADE;
END IF;
END $$;

-- 3) behavioral_submissions
CREATE TABLE IF NOT EXISTS behavioral_submissions (
                                                      id UUID PRIMARY KEY,
                                                      session_id UUID,
                                                      user_id UUID,
                                                      response_text TEXT,
                                                      ai_feedback TEXT,
                                                      submitted_at TIMESTAMP DEFAULT NOW()
    );

ALTER TABLE behavioral_submissions
    ADD COLUMN IF NOT EXISTS session_id UUID,
    ADD COLUMN IF NOT EXISTS user_id UUID,
    ADD COLUMN IF NOT EXISTS response_text TEXT,
    ADD COLUMN IF NOT EXISTS ai_feedback TEXT,
    ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP DEFAULT NOW();

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'behavioral_submissions'
          AND constraint_name = 'fk_behavioral_submissions_session'
    ) THEN
ALTER TABLE behavioral_submissions
    ADD CONSTRAINT fk_behavioral_submissions_session
        FOREIGN KEY (session_id) REFERENCES behavioral_sessions(id) ON DELETE CASCADE;
END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'behavioral_submissions'
          AND constraint_name = 'fk_behavioral_submissions_user'
    ) THEN
ALTER TABLE behavioral_submissions
    ADD CONSTRAINT fk_behavioral_submissions_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
END IF;
END $$;
