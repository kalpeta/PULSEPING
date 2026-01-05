package com.pulseping.waitlist.model;

import java.time.Instant;

public record Subscriber(
        long id,
        String email,
        Instant createdAt
) {}