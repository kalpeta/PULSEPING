-- V3: Outbox pattern + idempotent consumer store

CREATE TABLE IF NOT EXISTS outbox_events (
  id UUID PRIMARY KEY,
  aggregate_type TEXT NOT NULL,
  aggregate_id TEXT NOT NULL,
  event_type TEXT NOT NULL,
  payload_json JSONB NOT NULL,
  status TEXT NOT NULL DEFAULT 'NEW',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  sent_at TIMESTAMPTZ NULL,
  correlation_id TEXT NULL,
  CONSTRAINT chk_outbox_status CHECK (status IN ('NEW', 'SENT'))
);

-- Efficient poller queries: "give me NEW events in creation order"
CREATE INDEX IF NOT EXISTS idx_outbox_status_created_at
  ON outbox_events(status, created_at);

CREATE TABLE IF NOT EXISTS processed_events (
  event_id UUID PRIMARY KEY,
  processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);