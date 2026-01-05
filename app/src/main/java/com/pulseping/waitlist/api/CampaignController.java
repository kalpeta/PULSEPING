package com.pulseping.waitlist.api;

import com.pulseping.waitlist.model.Campaign;
import com.pulseping.waitlist.model.Subscriber;
import com.pulseping.waitlist.service.CampaignService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/{id}/subscribe")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse subscribe(
            @PathVariable long id,
            @Valid @RequestBody SubscribeRequest req,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        String cid = (correlationId == null || correlationId.isBlank())
                ? java.util.UUID.randomUUID().toString()
                : correlationId;

        var res = service.subscribe(id, req.email(), cid);
        return new SubscriptionResponse(res.campaignId(), res.subscriberId(), res.email(), res.createdAt(), res.eventId());
    }

    @GetMapping("/{id}/subscribers")
    public List<SubscriberResponse> listSubscribers(
            @PathVariable long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<Subscriber> subs = service.listSubscribers(id, page, size);
        return subs.stream().map(s -> new SubscriberResponse(s.id(), s.email(), s.createdAt())).toList();
    }
}
