package com.pulseping.waitlist.service;

import com.pulseping.common.NotFoundException;
import com.pulseping.waitlist.model.Campaign;
import com.pulseping.waitlist.model.Subscriber;
import com.pulseping.waitlist.repo.CampaignRepository;
import com.pulseping.waitlist.repo.SubscriberRepository;
import com.pulseping.waitlist.repo.SubscriptionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CampaignService {

    private final CampaignRepository campaigns;
    private final SubscriberRepository subscribers;
    private final SubscriptionRepository subscriptions;
    private final JdbcTemplate jdbc;

    public CampaignService(
            CampaignRepository campaigns,
            SubscriberRepository subscribers,
            SubscriptionRepository subscriptions,
            JdbcTemplate jdbc
    ) {
        this.campaigns = campaigns;
        this.subscribers = subscribers;
        this.subscriptions = subscriptions;
        this.jdbc = jdbc;
    }

    public Campaign createCampaign(String name) {
        return campaigns.createDraft(name);
    }

    public Campaign activate(long id) {
        boolean ok = campaigns.activate(id);
        if (!ok) throw new NotFoundException("campaign not found: " + id);
        return campaigns.findById(id).orElseThrow(() -> new NotFoundException("campaign not found: " + id));
    }

    public SubscriptionResult subscribe(long campaignId, String email) {
        // Ensure campaign exists
        campaigns.findById(campaignId).orElseThrow(() -> new NotFoundException("campaign not found: " + campaignId));

        // Upsert subscriber and create subscription
        Subscriber sub = subscribers.upsertByEmail(email);
        var createdAt = subscriptions.createIfMissing(campaignId, sub.id());

        return new SubscriptionResult(campaignId, sub.id(), sub.email(), createdAt);
    }

    public List<Subscriber> listSubscribers(long campaignId, int page, int size) {
        // Ensure campaign exists
        campaigns.findById(campaignId).orElseThrow(() -> new NotFoundException("campaign not found: " + campaignId));

        int offset = page * size;

        return jdbc.query("""
                SELECT s.id, s.email, s.created_at
                FROM subscriptions ss
                JOIN subscribers s ON s.id = ss.subscriber_id
                WHERE ss.campaign_id = ?
                ORDER BY ss.created_at DESC
                LIMIT ? OFFSET ?
                """, (rs, rowNum) -> new Subscriber(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getTimestamp("created_at").toInstant()
        ), campaignId, size, offset);
    }

    public record SubscriptionResult(long campaignId, long subscriberId, String email, java.time.Instant createdAt) {}
}
