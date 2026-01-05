package com.pulseping.messaging.outbox;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class OutboxDao {

    private final JdbcTemplate jdbc;

    public OutboxDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<OutboxRow> fetchNextBatch(int batchSize) {
        return jdbc.query("""
                SELECT id, aggregate_id, event_type, payload_json, created_at
                FROM outbox_events
                WHERE status = 'NEW'
                ORDER BY created_at ASC
                LIMIT ?
                """,
                (rs, rowNum) -> new OutboxRow(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("aggregate_id"),
                        rs.getString("event_type"),
                        rs.getString("payload_json"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                batchSize
        );
    }

    public int markSent(UUID id) {
        return jdbc.update("""
                UPDATE outbox_events
                SET status = 'SENT'
                WHERE id = ? AND status = 'NEW'
                """, id);
    }

    public record OutboxRow(
            UUID id,
            String aggregateId,
            String eventType,
            String payloadJson,
            Instant createdAt
    ) {}
}