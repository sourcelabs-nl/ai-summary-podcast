-- Migrate soundcloud_playlist_id data to podcast_publication_targets
INSERT INTO podcast_publication_targets (podcast_id, target, config, enabled)
SELECT id, 'soundcloud', '{"playlistId":"' || soundcloud_playlist_id || '"}', 1
FROM podcasts
WHERE soundcloud_playlist_id IS NOT NULL;

-- Note: soundcloud_playlist_id column is intentionally kept on the podcasts table
-- to avoid foreign key constraint issues during SQLite table recreation.
-- The Podcast entity no longer maps this field, so it is effectively ignored.
