package com.pulseping.messaging.events;

import java.time.Instant;
import java.util.UUID;

public record SubscriberSubscribedEvent(
        UUID eventId,
        long campaignId,
        long subscriberId,
        String email,
        Instant subscribedAt,
        String correlationId
) {}