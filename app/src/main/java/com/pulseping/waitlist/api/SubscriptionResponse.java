package com.pulseping.waitlist.api;

import java.time.Instant;

public record SubscriptionResponse(
        long campaignId,
        long subscriberId,
        String email,
        Instant createdAt
) {}