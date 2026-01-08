package com.pulseping.notifications.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulseping.notifications.model.AttemptStatus;
import com.pulseping.notifications.model.MessageStatus;
import com.pulseping.notifications.repo.NotificationsRepository;
import com.pulseping.provider.client.ProviderClient;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class NotificationsConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationsConsumer.class);

    private final NotificationsRepository repo;
    private final ProviderClient providerClient;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public NotificationsConsumer(NotificationsRepository repo, ProviderClient providerClient) {
        this.repo = repo;
        this.providerClient = providerClient;
    }

    @KafkaListener(topics = "pulseping.events")
    public void onEvent(String payload, org.apache.kafka.clients.consumer.ConsumerRecord<String, String> record) throws Exception {
        String eventType = headerAsString(record.headers().lastHeader("eventType"));
        String correlationId = headerAsString(record.headers().lastHeader("correlationId"));
        String eventId = headerAsString(record.headers().lastHeader("eventId"));

        if (!"SubscriberSubscribed".equals(eventType)) {
            return; // ignore others for now
        }

        JsonNode n = mapper.readTree(payload);
        long campaignId = n.get("campaignId").asLong();
        long subscriberId = n.get("subscriberId").asLong();
        String email = n.get("email").asText();

        long messageId = repo.createMessage(campaignId, subscriberId, "WELCOME_V1");

        var req = new com.pulseping.provider.api.ProviderSendRequest(
                campaignId,
                subscriberId,
                email,
                "WELCOME_V1",
                correlationId,
                eventId
        );

        try {
            var resp = providerClient.send(req);

            repo.insertAttempt(messageId, 1, AttemptStatus.SENT, null);
            repo.updateMessageStatus(messageId, MessageStatus.SENT);

            log.info("[consumer] delivered OK eventId={} correlationId={} messageId={} providerMessageId={}",
                    eventId, correlationId, messageId, resp.providerMessageId());

        } catch (ProviderClient.ProviderCallException e) {
            // Non-2xx from provider (ex: mode=fail)
            String code = e.getStatus().is5xxServerError() ? "PROVIDER_5XX" : "PROVIDER_NON_2XX";

            // store a concise hint in error_code (keep it small)
            String errorCode = code;

            repo.insertAttempt(messageId, 1, AttemptStatus.FAILED, errorCode);
            repo.updateMessageStatus(messageId, MessageStatus.FAILED);

            log.warn("[consumer] provider FAILED eventId={} correlationId={} messageId={} status={} body={}",
                    eventId, correlationId, messageId, e.getStatus().value(), e.getBody());

            // IMPORTANT: do NOT rethrow -> avoids duplicate messages on Kafka retries

        } catch (Exception e) {
            repo.insertAttempt(messageId, 1, AttemptStatus.FAILED, "PROVIDER_EXCEPTION");
            repo.updateMessageStatus(messageId, MessageStatus.FAILED);

            log.error("[consumer] provider EXCEPTION eventId={} correlationId={} messageId={}",
                    eventId, correlationId, messageId, e);

            // IMPORTANT: do NOT rethrow -> avoids duplicates
        }
    }

    private String headerAsString(Header h) {
        if (h == null) return null;
        return new String(h.value(), StandardCharsets.UTF_8);
    }
}
