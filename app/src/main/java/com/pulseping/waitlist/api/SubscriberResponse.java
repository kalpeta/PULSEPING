package com.pulseping.waitlist.api;

import java.time.Instant;

public record SubscriberResponse(
        long id,
        String email,
        Instant createdAt
) {}