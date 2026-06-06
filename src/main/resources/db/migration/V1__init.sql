CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE workflows (
                           id BIGSERIAL PRIMARY KEY,

                           external_id UUID NOT NULL UNIQUE,

                           status VARCHAR(30) NOT NULL,

                           audio_file_key VARCHAR(500),

                           metadata JSONB,

                           created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                           updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE tasks (
                       id BIGSERIAL PRIMARY KEY,

                       workflow_id BIGINT NOT NULL,

                       task_type VARCHAR(50) NOT NULL,

                       status VARCHAR(30) NOT NULL,

                       retry_count INT NOT NULL DEFAULT 0,

                       max_retries INT NOT NULL DEFAULT 3,

                       locked_by VARCHAR(100),

                       heartbeat_at TIMESTAMPTZ,

                       next_run_at TIMESTAMPTZ,

                       error_message TEXT,

                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                       CONSTRAINT fk_tasks_workflow
                           FOREIGN KEY (workflow_id)
                               REFERENCES workflows(id)
                               ON DELETE CASCADE
);

CREATE TABLE task_history (
                              id BIGSERIAL PRIMARY KEY,

                              task_id BIGINT NOT NULL,

                              workflow_id BIGINT NOT NULL,

                              task_type VARCHAR(50),

                              old_status VARCHAR(30),

                              new_status VARCHAR(30) NOT NULL,

                              worker_id VARCHAR(100),

                              message TEXT,

                              occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                              CONSTRAINT fk_task_history_task
                                  FOREIGN KEY (task_id)
                                      REFERENCES tasks(id)
                                      ON DELETE CASCADE
);

CREATE INDEX idx_tasks_workflow_id
    ON tasks(workflow_id);

CREATE INDEX idx_tasks_status_next_run
    ON tasks(status, next_run_at);

CREATE INDEX idx_tasks_heartbeat
    ON tasks(status, heartbeat_at);

CREATE INDEX idx_task_history_workflow
    ON task_history(workflow_id, occurred_at);

CREATE INDEX idx_workflows_external_id
    ON workflows(external_id);