package com.pulseping.provider.api;

public record ProviderSendResponse(
        String providerMessageId,
        String status
) {}