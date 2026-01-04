package com.pulseping.waitlist.api;

import jakarta.validation.constraints.NotBlank;

public record CreateCampaignRequest(
        @NotBlank(message = "name is required")
        String name
) {}