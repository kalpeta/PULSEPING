package com.pulseping.notifications.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulseping.notifications.model.AttemptStatus;
import com.pulseping.notifications.model.MessageStatus;
import com.pulseping.notifications.repo.NotificationsRepository;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class NotificationsConsumer {

    private final NotificationsRepository repo;
    private final com.pulseping.provider.client.ProviderClient providerClient;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public NotificationsConsumer(NotificationsRepository repo, com.pulseping.provider.client.ProviderClient providerClient) {
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

        long messageId = repo.createMessage(campaignId, subscriberId, "WELCOME_V1");

        try {
            var req = new com.pulseping.provider.api.ProviderSendRequest(
                    campaignId,
                    subscriberId,
                    n.get("email").asText(),
                    "WELCOME_V1",
                    correlationId,
                    eventId
            );

            // SUCCESS path by default
            var resp = providerClient.send(req);

            if (resp.getStatusCode().is2xxSuccessful()) {
                repo.insertAttempt(messageId, 1, AttemptStatus.SENT, null);
                repo.updateMessageStatus(messageId, MessageStatus.SENT);
            } else {
                repo.insertAttempt(messageId, 1, AttemptStatus.FAILED, "PROVIDER_NON_2XX");
                repo.updateMessageStatus(messageId, MessageStatus.FAILED);
            }

        } catch (Exception e) {
            repo.insertAttempt(messageId, 1, AttemptStatus.FAILED, "PROVIDER_EXCEPTION");
            repo.updateMessageStatus(messageId, MessageStatus.FAILED);
            throw e;
        }

        System.out.println("[consumer] eventType=" + eventType +
                " eventId=" + eventId +
                " correlationId=" + correlationId +
                " messageId=" + messageId);
    }

    private String headerAsString(Header h) {
        if (h == null) return null;
        return new String(h.value(), StandardCharsets.UTF_8);
    }
}