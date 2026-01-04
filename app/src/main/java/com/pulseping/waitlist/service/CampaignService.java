package com.pulseping.waitlist.service;

import com.pulseping.common.NotFoundException;
import com.pulseping.waitlist.model.Campaign;
import com.pulseping.waitlist.repo.CampaignRepository;
import org.springframework.stereotype.Service;

@Service
public class CampaignService {

    private final CampaignRepository repo;

    public CampaignService(CampaignRepository repo) {
        this.repo = repo;
    }

    public Campaign createCampaign(String name) {
        return repo.createDraft(name);
    }

    public Campaign activate(long id) {
        boolean ok = repo.activate(id);
        if (!ok) {
            throw new NotFoundException("campaign not found: " + id);
        }
        return repo.findById(id).orElseThrow(() -> new NotFoundException("campaign not found: " + id));
    }
}
