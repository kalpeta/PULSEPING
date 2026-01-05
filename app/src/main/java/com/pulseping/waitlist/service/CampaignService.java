package com.pulseping.waitlist.service;

import com.pulseping.common.NotFoundException;
import com.pulseping.messaging.JsonUtil;
import com.pulseping.messaging.events.SubscriberSubscribedEvent;
import com.pulseping.messaging.outbox.OutboxRepository;
import com.pulseping.waitlist.model.Campaign;
import com.pulseping.waitlist.model.Subscriber;
import com.pulseping.waitlist.repo.CampaignRepository;
import com.pulseping.waitlist.repo.SubscriberRepository;
import com.pulseping.waitlist.repo.SubscriptionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CampaignService {

    private final CampaignRepository campaigns;
    private final SubscriberRepository subscribers;
    private final SubscriptionRepository subscriptions;
    private final OutboxRepository outbox;
    private final JdbcTemplate jdbc;

    public CampaignService(
            CampaignRepository campaigns,
            SubscriberRepository subscribers,
            SubscriptionRepository subscriptions,
            OutboxRepository outbox,
            JdbcTemplate jdbc
    ) {
        this.campaigns = campaigns;
        this.subscribers = subscribers;
        this.subscriptions = subscriptions;
        this.outbox = outbox;
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

    @Transactional
    public SubscriptionResult subscribe(long campaignId, String email, String correlationId) {
        // Ensure campaign exists
        campaigns.findById(campaignId).orElseThrow(() -> new NotFoundException("campaign not found: " + campaignId));

        // Upsert subscriber + create subscription
        Subscriber sub = subscribers.upsertByEmail(email);
        Instant subscribedAt = subscriptions.createIfMissing(campaignId, sub.id());

        // Write outbox event in SAME TX
        UUID eventId = UUID.randomUUID();
        String aggregateId = campaignId + ":" + sub.id();
        var evt = new SubscriberSubscribedEvent(eventId, campaignId, sub.id(), sub.email(), subscribedAt, correlationId);
        outbox.insertNew(eventId, "Subscription", aggregateId, "SubscriberSubscribed", JsonUtil.toJson(evt));

        return new SubscriptionResult(campaignId, sub.id(), sub.email(), subscribedAt, eventId);
    }

    public List<Subscriber> listSubscribers(long campaignId, int page, int size) {
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

    public record SubscriptionResult(
            long campaignId,
            long subscriberId,
            String email,
            Instant createdAt,
            UUID eventId
    ) {}
}
