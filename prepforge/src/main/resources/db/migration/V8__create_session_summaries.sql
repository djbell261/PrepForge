CREATE TABLE session_summaries (
    id UUID PRIMARY KEY,
    session_type VARCHAR(50) NOT NULL,
    session_id UUID NOT NULL,
    user_id UUID NOT NULL,
    summary VARCHAR(4000) NOT NULL,
    strengths_json VARCHAR(4000) NOT NULL,
    weaknesses_json VARCHAR(4000) NOT NULL,
    next_steps_json VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uq_session_summaries_type_session_id
    ON session_summaries(session_type, session_id);
