-- V2: Notifications persistence (messages + delivery_attempts)

CREATE TABLE IF NOT EXISTS messages (
  id BIGSERIAL PRIMARY KEY,
  campaign_id BIGINT NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
  subscriber_id BIGINT NOT NULL REFERENCES subscribers(id) ON DELETE CASCADE,
  template_name TEXT NOT NULL,
  status TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT chk_message_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_messages_campaign_id ON messages(campaign_id);
CREATE INDEX IF NOT EXISTS idx_messages_subscriber_id ON messages(subscriber_id);
CREATE INDEX IF NOT EXISTS idx_messages_status ON messages(status);

CREATE TABLE IF NOT EXISTS delivery_attempts (
  id BIGSERIAL PRIMARY KEY,
  message_id BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
  attempt_no INT NOT NULL,
  status TEXT NOT NULL,
  error_code TEXT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT chk_attempt_status CHECK (status IN ('SENT', 'FAILED')),
  CONSTRAINT uq_attempt_message_attemptno UNIQUE (message_id, attempt_no)
);

CREATE INDEX IF NOT EXISTS idx_attempts_message_id ON delivery_attempts(message_id);
CREATE INDEX IF NOT EXISTS idx_attempts_status ON delivery_attempts(status);
