package com.pulseping.waitlist.repo;

import com.pulseping.waitlist.model.Campaign;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public class CampaignRepository {

    private final JdbcTemplate jdbc;

    public CampaignRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Campaign> ROW_MAPPER = (rs, rowNum) -> new Campaign(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("status"),
            rs.getTimestamp("created_at").toInstant()
    );

    public Campaign createDraft(String name) {
        Long id = jdbc.queryForObject(
                "INSERT INTO campaigns(name, status) VALUES (?, 'DRAFT') RETURNING id",
                Long.class,
                name
        );

        if (id == null) {
            throw new IllegalStateException("Failed to create campaign: no id returned");
        }

        return findById(id).orElseThrow(); // should exist
    }

    public Optional<Campaign> findById(long id) {
        var rows = jdbc.query("SELECT id, name, status, created_at FROM campaigns WHERE id = ?", ROW_MAPPER, id);
        return rows.stream().findFirst();
    }

    public boolean activate(long id) {
        int updated = jdbc.update("UPDATE campaigns SET status = 'ACTIVE' WHERE id = ?", id);
        return updated == 1;
    }
}