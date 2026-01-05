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
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public NotificationsConsumer(NotificationsRepository repo) {
        this.repo = repo;
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

        // For B5.1, simulate “provider success”
        long messageId = repo.createMessage(campaignId, subscriberId, "WELCOME_V1");
        repo.insertAttempt(messageId, 1, AttemptStatus.SENT, null);
        repo.updateMessageStatus(messageId, MessageStatus.SENT);

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