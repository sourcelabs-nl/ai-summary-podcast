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
}
