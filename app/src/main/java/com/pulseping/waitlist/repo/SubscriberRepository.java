package com.pulseping.waitlist.repo;

import com.pulseping.waitlist.model.Subscriber;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class SubscriberRepository {

    private final JdbcTemplate jdbc;

    public SubscriberRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Subscriber> ROW_MAPPER = (rs, rowNum) -> new Subscriber(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getTimestamp("created_at").toInstant()
    );

    public Subscriber upsertByEmail(String email) {
        // Insert if missing, otherwise return existing row (case-insensitive)
        Long id = jdbc.queryForObject("""
                INSERT INTO subscribers(email)
                VALUES (?)
                ON CONFLICT (lower(email)) DO UPDATE SET email = EXCLUDED.email
                RETURNING id
                """, Long.class, email);

        return findById(id).orElseThrow();
    }

    public Optional<Subscriber> findById(long id) {
        var rows = jdbc.query("SELECT id, email, created_at FROM subscribers WHERE id = ?", ROW_MAPPER, id);
        return rows.stream().findFirst();
    }
}