-- Update audioFilePath: ./data/episodes/{podcastId}/{file} → ./data/{podcastId}/episodes/{file}
UPDATE episodes
SET audio_file_path = REPLACE(audio_file_path, '/data/episodes/' || podcast_id || '/', '/data/' || podcast_id || '/episodes/')
WHERE audio_file_path IS NOT NULL
  AND audio_file_path LIKE '%/data/episodes/%';
