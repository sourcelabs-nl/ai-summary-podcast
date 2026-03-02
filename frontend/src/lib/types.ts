export interface User {
  id: string;
  name: string;
}

export interface Podcast {
  id: string;
  userId: string;
  name: string;
  topic: string;
  style: string;
  speakerNames?: Record<string, string>;
}

export interface Episode {
  id: number;
  podcastId: string;
  generatedAt: string;
  scriptText: string;
  status: string;
  audioFilePath?: string;
  durationSeconds?: number;
  recap?: string;
}

export interface EpisodePublication {
  id: number;
  episodeId: number;
  target: string;
  status: string;
  externalId: string | null;
  externalUrl: string | null;
  errorMessage: string | null;
  publishedAt: string | null;
  createdAt: string;
}
