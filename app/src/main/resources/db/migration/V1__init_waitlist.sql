-- V1: Waitlist core schema (campaigns, subscribers, subscriptions)
-- Notes:
-- - Use BIGSERIAL for simple numeric IDs (fine for baseline)
-- - created_at default now()
-- - subscriptions enforces uniqueness (campaign_id, subscriber_id)

CREATE TABLE IF NOT EXISTS campaigns (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  status TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS subscribers (
  id BIGSERIAL PRIMARY KEY,
  email TEXT NOT NULL UNIQUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS subscriptions (
  campaign_id BIGINT NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
  subscriber_id BIGINT NOT NULL REFERENCES subscribers(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (campaign_id, subscriber_id)
);

-- Helpful indexes (small but realistic)
CREATE INDEX IF NOT EXISTS idx_subscriptions_campaign_id ON subscriptions(campaign_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_subscriber_id ON subscriptions(subscriber_id);