CREATE TABLE outbox_events (
    id          BIGSERIAL PRIMARY KEY,
    task_id     BIGINT       NOT NULL,
    workflow_id BIGINT       NOT NULL,
    task_type   VARCHAR(50)  NOT NULL,
    success     BOOLEAN      NOT NULL,
    error_message TEXT,
    output      JSONB,
    published   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outbox_unpublished ON outbox_events (published) WHERE published = FALSE;
