"use client";

import { useEffect, useState, useCallback, useMemo } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { Check, Upload, X } from "lucide-react";
import { useUser } from "@/lib/user-context";
import type { Podcast, Episode, EpisodePublication } from "@/lib/types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ScriptContent } from "@/components/script-viewer";
import { ArticlesTab } from "@/components/articles-tab";
import { PublicationsTab } from "@/components/publications-tab";
import { PublishWizard } from "@/components/publish-wizard";

const WORDS_PER_MINUTE = 150;

const STATUS_VARIANT: Record<string, "default" | "secondary" | "destructive" | "outline"> = {
  GENERATED: "outline",
  PENDING_REVIEW: "default",
  APPROVED: "default",
  FAILED: "default",
  DISCARDED: "secondary",
};

export default function EpisodeDetailPage() {
  const params = useParams<{ podcastId: string; episodeId: string }>();
  const { selectedUser, loading: userLoading } = useUser();
  const [podcast, setPodcast] = useState<Podcast | null>(null);
  const [episode, setEpisode] = useState<Episode | null>(null);
  const [loading, setLoading] = useState(true);
  const [articleCount, setArticleCount] = useState<number | null>(null);
  const [published, setPublished] = useState(false);
  const [publishOpen, setPublishOpen] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  const fetchPublished = useCallback((userId: string, podcastId: string, episodeId: string) => {
    fetch(`/api/users/${userId}/podcasts/${podcastId}/episodes/${episodeId}/publications`)
      .then((res) => (res.ok ? res.json() : []))
      .then((pubs: EpisodePublication[]) => setPublished(pubs.some((p) => p.status === "PUBLISHED")))
      .catch(() => setPublished(false));
  }, []);

  const fetchEpisode = useCallback(() => {
    if (!selectedUser) return;
    fetch(`/api/users/${selectedUser.id}/podcasts/${params.podcastId}/episodes/${params.episodeId}`)
      .then((res) => (res.ok ? res.json() : null))
      .then(setEpisode)
      .catch(() => setEpisode(null));
    fetchPublished(selectedUser.id, params.podcastId, params.episodeId);
  }, [selectedUser, params.podcastId, params.episodeId, fetchPublished]);

  useEffect(() => {
    if (!selectedUser) return;
    setLoading(true);
    Promise.all([
      fetch(`/api/users/${selectedUser.id}/podcasts/${params.podcastId}`).then((res) => res.json()),
      fetch(`/api/users/${selectedUser.id}/podcasts/${params.podcastId}/episodes/${params.episodeId}`).then((res) => res.json()),
    ])
      .then(([podcastData, episodeData]) => {
        setPodcast(podcastData);
        setEpisode(episodeData);
        fetchPublished(selectedUser.id, params.podcastId, params.episodeId);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [selectedUser, params.podcastId, params.episodeId, fetchPublished]);

  async function handleAction(action: "approve" | "discard") {
    if (!selectedUser || !episode) return;
    await fetch(
      `/api/users/${selectedUser.id}/podcasts/${params.podcastId}/episodes/${episode.id}/${action}`,
      { method: "POST" }
    );
    fetchEpisode();
  }

  function handlePublished() {
    fetchEpisode();
    setRefreshKey((k) => k + 1);
  }

  const handleArticleCountLoaded = useCallback((count: number) => {
    setArticleCount(count);
  }, []);

  const scriptStats = useMemo(() => {
    if (!episode) return { wordCount: 0, estimatedMinutes: 0 };
    const plainText = episode.scriptText.replace(/<\/?[^>]+>/g, " ");
    const words = plainText.split(/\s+/).filter(Boolean).length;
    const estimatedMinutes = Math.round(words / WORDS_PER_MINUTE);
    return { wordCount: words, estimatedMinutes };
  }, [episode]);

  if (userLoading || loading) {
    return <p className="text-muted-foreground">Loading...</p>;
  }

  if (!selectedUser || !podcast || !episode) {
    return <p className="text-muted-foreground">Episode not found.</p>;
  }

  const generatedDate = new Date(episode.generatedAt);

  return (
    <div>
      <div className="mb-4">
        <Link
          href={`/podcasts/${params.podcastId}`}
          className="text-sm text-muted-foreground hover:underline"
        >
          &larr; Back to Episodes
        </Link>
      </div>

      <div className="mb-6 flex items-center justify-between">
        <div>
          <div className="flex items-center gap-3">
            <h2 className="text-2xl font-bold">
              Episode #{episode.id}
            </h2>
            <Badge variant={STATUS_VARIANT[episode.status] ?? "secondary"}>
              {episode.status.replace("_", " ")}
            </Badge>
            {published && (
              <Badge variant="default">
                Published
              </Badge>
            )}
          </div>
          <p className="text-sm text-muted-foreground">
            Generated {generatedDate.toLocaleDateString()} ({generatedDate.toLocaleDateString(undefined, { weekday: "long" })})
            {" "}&middot; {scriptStats.wordCount.toLocaleString()} words &middot; ~{scriptStats.estimatedMinutes} min estimated
            {episode.durationSeconds != null && (
              <> &middot; duration {Math.floor(episode.durationSeconds / 60)}:{String(episode.durationSeconds % 60).padStart(2, "0")}</>
            )}
            {episode.recap && (
              <> &middot; {episode.recap}</>
            )}
          </p>
        </div>
        <div className="flex items-center gap-2">
          {episode.status === "PENDING_REVIEW" && (
            <>
              <Button size="sm" onClick={() => handleAction("approve")}>
                <Check className="size-4" />
                Approve
              </Button>
              <Button
                size="sm"
                variant="destructive"
                onClick={() => handleAction("discard")}
              >
                <X className="size-4" />
                Discard
              </Button>
            </>
          )}
          {episode.status === "GENERATED" && !published && (
            <Button size="sm" onClick={() => setPublishOpen(true)}>
              <Upload className="size-4" />
              Publish
            </Button>
          )}
        </div>
      </div>

      <Tabs defaultValue="script">
        <TabsList>
          <TabsTrigger value="script">Script</TabsTrigger>
          <TabsTrigger value="articles">
            Articles{articleCount !== null ? ` (${articleCount})` : ""}
          </TabsTrigger>
          <TabsTrigger value="publications">Publications</TabsTrigger>
        </TabsList>

        <TabsContent value="script">
          <div className="mt-4">
            <ScriptContent
              scriptText={episode.scriptText}
              style={podcast.style}
              speakerNames={podcast.speakerNames}
            />
          </div>
        </TabsContent>

        <TabsContent value="articles">
          <div className="mt-4">
            <ArticlesTab
              userId={selectedUser.id}
              podcastId={params.podcastId}
              episodeId={episode.id}
              onCountLoaded={handleArticleCountLoaded}
            />
          </div>
        </TabsContent>

        <TabsContent value="publications">
          <div className="mt-4">
            <PublicationsTab
              userId={selectedUser.id}
              podcastId={params.podcastId}
              episodes={[episode]}
              refreshKey={refreshKey}
              onRepublished={handlePublished}
            />
          </div>
        </TabsContent>
      </Tabs>

      {publishOpen && (
        <PublishWizard
          open={publishOpen}
          onOpenChange={setPublishOpen}
          episode={episode}
          podcastName={podcast.name}
          userId={selectedUser.id}
          podcastId={params.podcastId}
          onPublished={handlePublished}
        />
      )}
    </div>
  );
}
