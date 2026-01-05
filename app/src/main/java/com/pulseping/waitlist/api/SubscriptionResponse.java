package com.pulseping.waitlist.api;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(
        long campaignId,
        long subscriberId,
        String email,
        Instant createdAt,
        UUID eventId
) {}
