package com.pulseping.waitlist.api;

import java.time.Instant;

public record CampaignResponse(
        long id,
        String name,
        String status,
        Instant createdAt
) {}