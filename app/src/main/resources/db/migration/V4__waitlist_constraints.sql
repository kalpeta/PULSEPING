-- V4: Waitlist uniqueness constraints

-- One subscriber per email
CREATE UNIQUE INDEX IF NOT EXISTS uq_subscribers_email ON subscribers(lower(email));

-- One subscription per (campaign, subscriber)
CREATE UNIQUE INDEX IF NOT EXISTS uq_subscriptions_campaign_subscriber
  ON subscriptions(campaign_id, subscriber_id);