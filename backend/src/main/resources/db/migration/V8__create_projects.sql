BEGIN;

CREATE SCHEMA IF NOT EXISTS projects;

CREATE TABLE projects.projects (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users.app_users(id),
    title VARCHAR(150) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    technologies TEXT[] NOT NULL,
    status VARCHAR(30) NOT NULL,
    repository_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_projects_user_id ON projects.projects(user_id);

COMMIT;