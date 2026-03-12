package com.aisummarypodcast.store

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

@Repository
class PodcastPublicationTargetRepository(private val jdbcTemplate: JdbcTemplate) {

    private val rowMapper = RowMapper<PodcastPublicationTarget> { rs, _ ->
        PodcastPublicationTarget(
            id = rs.getLong("id"),
            podcastId = rs.getString("podcast_id"),
            target = rs.getString("target"),
            config = rs.getString("config"),
            enabled = rs.getInt("enabled") == 1
        )
    }

    fun findByPodcastId(podcastId: String): List<PodcastPublicationTarget> =
        jdbcTemplate.query(
            "SELECT * FROM podcast_publication_targets WHERE podcast_id = ?",
            rowMapper, podcastId
        )

    fun findByPodcastIdAndTarget(podcastId: String, target: String): PodcastPublicationTarget? =
        jdbcTemplate.query(
            "SELECT * FROM podcast_publication_targets WHERE podcast_id = ? AND target = ?",
            rowMapper, podcastId, target
        ).firstOrNull()

    fun save(target: PodcastPublicationTarget): PodcastPublicationTarget {
        if (target.id == null) {
            jdbcTemplate.update(
                """
                INSERT INTO podcast_publication_targets (podcast_id, target, config, enabled)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (podcast_id, target) DO UPDATE SET config = ?, enabled = ?
                """.trimIndent(),
                target.podcastId, target.target, target.config, if (target.enabled) 1 else 0,
                target.config, if (target.enabled) 1 else 0
            )
        } else {
            jdbcTemplate.update(
                "UPDATE podcast_publication_targets SET config = ?, enabled = ? WHERE id = ?",
                target.config, if (target.enabled) 1 else 0, target.id
            )
        }
        return findByPodcastIdAndTarget(target.podcastId, target.target)!!
    }

    fun deleteByPodcastIdAndTarget(podcastId: String, target: String): Int =
        jdbcTemplate.update(
            "DELETE FROM podcast_publication_targets WHERE podcast_id = ? AND target = ?",
            podcastId, target
        )
}
