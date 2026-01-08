package com.pulseping.provider.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulseping.provider.api.ProviderSendRequest;
import com.pulseping.provider.api.ProviderSendResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class ProviderClient {

    private final String mode;
    private final long slowMs;

    private final RestClient restClient;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public ProviderClient(
            @Value("${provider.base-url}") String baseUrl,
            @Value("${provider.mode:ok}") String mode,
            @Value("${provider.slow-ms:0}") long slowMs
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.mode = mode == null ? "ok" : mode.trim().toLowerCase();
        this.slowMs = slowMs;
    }

    /**
     * Sends a message via the provider stub.
     * - mode=ok   => /provider/send
     * - mode=fail => /provider/send?fail=true (returns 500)
     * - mode=slow => /provider/send?slowMs=<provider.slow-ms>
     */
    public ProviderSendResponse send(ProviderSendRequest req) {
        final String uri = switch (mode) {
            case "fail" -> "/provider/send?fail=true";
            case "slow" -> "/provider/send?slowMs=" + slowMs;
            default -> "/provider/send";
        };

        return restClient.post()
                .uri(uri)
                .body(req)
                .exchange((request, response) -> {
                    HttpStatusCode status = response.getStatusCode();

                    String body = readBodyAsString(response.getBody());
                    if (status.is2xxSuccessful()) {
                        // body is JSON for success
                        return mapper.readValue(body, ProviderSendResponse.class);
                    }

                    // body is a plain string for failure: provider_failed_at=...
                    throw new ProviderCallException(status, body);
                });
    }

    private static String readBodyAsString(InputStream is) {
        if (is == null) return "";
        try {
            byte[] bytes = is.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    public static class ProviderCallException extends RuntimeException {
        private final HttpStatusCode status;
        private final String body;

        public ProviderCallException(HttpStatusCode status, String body) {
            super("Provider call failed: status=" + status + " body=" + body);
            this.status = status;
            this.body = body;
        }

        public HttpStatusCode getStatus() {
            return status;
        }

        public String getBody() {
            return body;
        }
    }
}
