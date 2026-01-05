package com.pulseping.waitlist.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public class SubscriptionRepository {

    private final JdbcTemplate jdbc;

    public SubscriptionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Instant createIfMissing(long campaignId, long subscriberId) {
        // Insert subscription if missing; otherwise return existing created_at
        return jdbc.queryForObject("""
                INSERT INTO subscriptions(campaign_id, subscriber_id)
                VALUES (?, ?)
                ON CONFLICT (campaign_id, subscriber_id) DO UPDATE
                  SET campaign_id = EXCLUDED.campaign_id
                RETURNING created_at
                """, Instant.class, campaignId, subscriberId);
    }
}