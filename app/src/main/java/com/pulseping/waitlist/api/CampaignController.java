package com.pulseping.waitlist.api;

import com.pulseping.waitlist.model.Campaign;
import com.pulseping.waitlist.service.CampaignService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/campaigns")
public class CampaignController {

    private final CampaignService service;

    public CampaignController(CampaignService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CampaignResponse create(@Valid @RequestBody CreateCampaignRequest req) {
        Campaign c = service.createCampaign(req.name());
        return new CampaignResponse(c.id(), c.name(), c.status(), c.createdAt());
    }

    @PostMapping("/{id}/activate")
    public CampaignResponse activate(@PathVariable long id) {
        Campaign c = service.activate(id);
        return new CampaignResponse(c.id(), c.name(), c.status(), c.createdAt());
    }
}