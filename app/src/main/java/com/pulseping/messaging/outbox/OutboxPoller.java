package com.pulseping.messaging.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulseping.messaging.kafka.KafkaProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class OutboxPoller {

    private final OutboxDao dao;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Value("${pulseping.outbox.batch-size:25}")
    private int batchSize;

    public OutboxPoller(OutboxDao dao, KafkaTemplate<String, String> kafka) {
        this.dao = dao;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${pulseping.outbox.poll-delay-ms:1000}")
    public void tick() {
        List<OutboxDao.OutboxRow> rows = dao.fetchNextBatch(batchSize);
        for (var row : rows) {
            publishOne(row);
        }
    }

    private void publishOne(OutboxDao.OutboxRow row) {
        try {
            String correlationId = extractCorrelationId(row.payloadJson());

            // Build a real Kafka ProducerRecord so we control key + headers.
            ProducerRecord<String, String> record =
                    new ProducerRecord<>(KafkaProducerConfig.TOPIC_EVENTS, row.aggregateId(), row.payloadJson());

            // Headers (bytes). This is our tracing mindset.
            record.headers().add("eventId", row.id().toString().getBytes(StandardCharsets.UTF_8));
            if (correlationId != null && !correlationId.isBlank()) {
                record.headers().add("correlationId", correlationId.getBytes(StandardCharsets.UTF_8));
            }
            record.headers().add("eventType", row.eventType().getBytes(StandardCharsets.UTF_8));

            // Send async. Mark SENT only after broker ack succeeds.
            kafka.send(record).whenComplete((result, ex) -> {
                if (ex == null) {
                    dao.markSent(row.id());
                }
                // if ex != null, leave status NEW so it retries next tick
            });
        } catch (Exception ignored) {
            // leave as NEW, retry later
        }
    }

    private String extractCorrelationId(String payloadJson) {
        try {
            JsonNode n = mapper.readTree(payloadJson);
            JsonNode cid = n.get("correlationId");
            return (cid == null) ? null : cid.asText();
        } catch (Exception e) {
            return null;
        }
    }
}
