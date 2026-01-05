package com.pulseping.messaging.outbox;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class OutboxRepository {

    private final JdbcTemplate jdbc;

    public OutboxRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertNew(
            UUID id,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payloadJson
    ) {
        jdbc.update("""
                INSERT INTO outbox_events(id, aggregate_type, aggregate_id, event_type, payload_json, status)
                VALUES (?, ?, ?, ?, ?::jsonb, 'NEW')
                """,
                id, aggregateType, aggregateId, eventType, payloadJson
        );
    }
}