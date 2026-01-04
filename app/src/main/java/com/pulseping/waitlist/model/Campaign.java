package com.pulseping.waitlist.model;

import java.time.Instant;

public record Campaign(
        long id,
        String name,
        String status,
        Instant createdAt
) {}