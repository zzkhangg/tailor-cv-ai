CREATE TABLE resumes (
    id UUID PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    original_pdf BYTEA NOT NULL,
    raw_text TEXT NOT NULL,
    parsed_json JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE job_descriptions (
    id UUID PRIMARY KEY,
    title VARCHAR(255),
    description TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE tailoring_runs (
    id UUID PRIMARY KEY,
    resume_id UUID NOT NULL REFERENCES resumes(id),
    job_description_id UUID NOT NULL REFERENCES job_descriptions(id),
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED')),
    tailored_json JSONB,
    change_summary JSONB,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX tailoring_runs_resume_id_idx ON tailoring_runs(resume_id);
CREATE INDEX tailoring_runs_job_description_id_idx ON tailoring_runs(job_description_id);
