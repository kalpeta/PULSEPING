package com.pulseping.provider.stub;

import com.pulseping.provider.api.ProviderSendRequest;
import com.pulseping.provider.api.ProviderSendResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/provider")
public class ProviderStubController {

    /**
     * Controls:
     *  - ?fail=true        -> returns 500
     *  - ?slowMs=1500      -> sleeps 1500ms then returns 200
     */
    @PostMapping("/send")
    public ResponseEntity<?> send(
            @RequestBody ProviderSendRequest req,
            @RequestParam(name = "fail", required = false, defaultValue = "false") boolean fail,
            @RequestParam(name = "slowMs", required = false, defaultValue = "0") long slowMs
    ) throws InterruptedException {

        if (slowMs > 0) {
            Thread.sleep(slowMs);
        }

        if (fail) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("provider_failed_at=" + Instant.now());
        }

        var resp = new ProviderSendResponse(UUID.randomUUID().toString(), "SENT");
        return ResponseEntity.ok(resp);
    }
}