export interface User {
  id: string;
  name: string;
}

export interface Podcast {
  id: string;
  userId: string;
  name: string;
  topic: string;
  language: string;
  llmModels?: Record<string, string>;
  ttsProvider: string;
  ttsVoices?: Record<string, string>;
  ttsSettings?: Record<string, string>;
  style: string;
  targetWords?: number;
  cron: string;
  customInstructions?: string;
  relevanceThreshold: number;
  requireReview: boolean;
  maxLlmCostCents?: number;
  maxArticleAgeDays?: number;
  speakerNames?: Record<string, string>;
  fullBodyThreshold?: number;
  sponsor?: Record<string, string>;
  pronunciations?: Record<string, string>;
  lastGeneratedAt?: string;
}

export interface Episode {
  id: number;
  podcastId: string;
  generatedAt: string;
  scriptText: string;
  status: string;
  audioFilePath?: string;
  durationSeconds?: number;
  llmCostCents?: number;
  ttsCostCents?: number;
  recap?: string;
}

export interface ArticleSource {
  id: string;
  type: string;
  url: string;
  label: string | null;
}

export interface EpisodeArticle {
  id: number;
  title: string;
  url: string;
  author: string | null;
  publishedAt: string | null;
  relevanceScore: number | null;
  summary: string | null;
  body: string | null;
  source: ArticleSource;
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
