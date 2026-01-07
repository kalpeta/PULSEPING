package com.pulseping.provider.client;

import com.pulseping.provider.api.ProviderSendRequest;
import com.pulseping.provider.api.ProviderSendResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ProviderClient {

    private final RestClient restClient;

    public ProviderClient(@Value("${provider.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public ResponseEntity<ProviderSendResponse> send(ProviderSendRequest req) {
        return restClient.post()
                .uri("/provider/send")
                .body(req)
                .retrieve()
                .toEntity(ProviderSendResponse.class);
    }

    public ResponseEntity<String> sendFail(ProviderSendRequest req) {
        return restClient.post()
                .uri("/provider/send?fail=true")
                .body(req)
                .retrieve()
                .toEntity(String.class);
    }

    public ResponseEntity<ProviderSendResponse> sendSlow(ProviderSendRequest req, long slowMs) {
        return restClient.post()
                .uri("/provider/send?slowMs=" + slowMs)
                .body(req)
                .retrieve()
                .toEntity(ProviderSendResponse.class);
    }
}