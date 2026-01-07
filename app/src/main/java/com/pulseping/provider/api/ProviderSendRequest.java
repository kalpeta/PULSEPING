package com.pulseping.provider.api;

public record ProviderSendRequest(
        long campaignId,
        long subscriberId,
        String email,
        String templateName,
        String correlationId,
        String eventId
) {}
