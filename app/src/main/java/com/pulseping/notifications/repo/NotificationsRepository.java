package com.pulseping.notifications.repo;

import com.pulseping.notifications.model.AttemptStatus;
import com.pulseping.notifications.model.MessageStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class NotificationsRepository {

    private final JdbcTemplate jdbc;

    public NotificationsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long createMessage(long campaignId, long subscriberId, String templateName) {
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO messages(campaign_id, subscriber_id, template_name, status, created_at) " +
                            "VALUES (?, ?, ?, ?, ?) RETURNING id",
                    new String[]{"id"}
            );
            ps.setLong(1, campaignId);
            ps.setLong(2, subscriberId);
            ps.setString(3, templateName);
            ps.setString(4, MessageStatus.PENDING.name());
            ps.setTimestamp(5, Timestamp.from(Instant.now()));
            return ps;
        }, keyHolder);

        // KeyHolder can contain multiple columns depending on driver; safest:
        Number id = (Number) keyHolder.getKeys().get("id");
        return id.longValue();
    }

    public void insertAttempt(long messageId, int attemptNo, AttemptStatus status, String errorCode) {
        jdbc.update(
                "INSERT INTO delivery_attempts(message_id, attempt_no, status, error_code, created_at) " +
                        "VALUES (?, ?, ?, ?, ?)",
                messageId, attemptNo, status.name(), errorCode, Timestamp.from(Instant.now())
        );
    }

    public void updateMessageStatus(long messageId, MessageStatus status) {
        jdbc.update(
                "UPDATE messages SET status=? WHERE id=?",
                status.name(), messageId
        );
    }
}
