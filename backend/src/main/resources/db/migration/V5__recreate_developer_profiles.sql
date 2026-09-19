CREATE TABLE developer_profiles (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL UNIQUE REFERENCES app_users(id),
    biography varchar(500) NOT NULL,
    programming_languages text[] NOT NULL,
    technologies text[] NOT NULL,
    experience_level varchar(20) NOT NULL CHECK (experience_level IN ('JUNIOR', 'SEMI_SENIOR', 'SENIOR')),
    github_url varchar(500),
    linkedin_url varchar(500),
    portfolio_url varchar(500),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT ck_developer_profiles_dates CHECK (updated_at >= created_at)
);

CREATE INDEX ix_developer_profiles_user_id ON developer_profiles (user_id);
