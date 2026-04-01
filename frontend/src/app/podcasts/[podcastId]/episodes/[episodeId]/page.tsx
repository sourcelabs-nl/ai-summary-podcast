"use client";

import { useEffect, useState, useCallback, useMemo } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { Check, RefreshCw, RotateCcw, Upload, X } from "lucide-react";
import { useUser } from "@/lib/user-context";
import { useEventStream } from "@/lib/event-context";
import type { Podcast, Episode, EpisodePublication } from "@/lib/types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { ScriptContent } from "@/components/script-viewer";
import { ArticlesTab } from "@/components/articles-tab";
import { PublicationsTab } from "@/components/publications-tab";
import { PublishWizard } from "@/components/publish-wizard";
import { useTabParam } from "@/hooks/use-tab-param";

const WORDS_PER_MINUTE = 150;
const TABS = ["script", "articles", "publications"] as const;

const STATUS_VARIANT: Record<string, "default" | "secondary" | "destructive" | "outline"> = {
  GENERATED: "outline",
  PENDING_REVIEW: "default",
  APPROVED: "default",
  GENERATING_AUDIO: "default",
  FAILED: "default",
  DISCARDED: "secondary",
};

export default function EpisodeDetailPage() {
  const params = useParams<{ podcastId: string; episodeId: string }>();
  const router = useRouter();
  const { selectedUser, loading: userLoading } = useUser();
  const [podcast, setPodcast] = useState<Podcast | null>(null);
  const [episode, setEpisode] = useState<Episode | null>(null);
  const [loading, setLoading] = useState(true);
  const [articleCount, setArticleCount] = useState<number | null>(null);
  const [published, setPublished] = useState(false);
  const [publishOpen, setPublishOpen] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);
  const [regenerating, setRegenerating] = useState(false);
  const [confirmRegenerate, setConfirmRegenerate] = useState(false);
  const [sameDayPublished, setSameDayPublished] = useState(false);
  const [currentTab, setTab] = useTabParam("script", TABS);

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

  useEventStream(params.podcastId, useCallback((_event, data) => {
    if (data.entityId === Number(params.episodeId)) {
      fetchEpisode();
    }
  }, [params.episodeId, fetchEpisode]));

  const checkSameDayPublished = useCallback((userId: string, podcastId: string, currentEpisode: Episode) => {
    const episodeDate = new Date(currentEpisode.generatedAt).toLocaleDateString();
    fetch(`/api/users/${userId}/podcasts/${podcastId}/episodes`)
      .then((res) => (res.ok ? res.json() : []))
      .then((allEpisodes: Episode[]) => {
        const sameDayEpisodes = allEpisodes.filter(
          (ep) => new Date(ep.generatedAt).toLocaleDateString() === episodeDate
        );
        return Promise.all(
          sameDayEpisodes.map((ep) =>
            fetch(`/api/users/${userId}/podcasts/${podcastId}/episodes/${ep.id}/publications`)
              .then((res) => (res.ok ? res.json() : []))
              .catch(() => [] as EpisodePublication[])
          )
        );
      })
      .then((results) => {
        setSameDayPublished(results.some((pubs: EpisodePublication[]) => pubs.some((p) => p.status === "PUBLISHED")));
      })
      .catch(() => setSameDayPublished(false));
  }, []);

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
        checkSameDayPublished(selectedUser.id, params.podcastId, episodeData);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [selectedUser, params.podcastId, params.episodeId, fetchPublished, checkSameDayPublished]);

  async function handleRegenerateConfirmed() {
    if (!selectedUser || !episode) return;
    setConfirmRegenerate(false);
    setRegenerating(true);
    try {
      const res = await fetch(
        `/api/users/${selectedUser.id}/podcasts/${params.podcastId}/episodes/${episode.id}/regenerate`,
        { method: "POST" }
      );
      if (res.ok) {
        const data = await res.json();
        router.push(`/podcasts/${params.podcastId}/episodes/${data.episodeId}`);
      }
    } finally {
      setRegenerating(false);
    }
  }

  async function handleRetry() {
    if (!selectedUser || !episode) return;
    await fetch(
      `/api/users/${selectedUser.id}/podcasts/${params.podcastId}/episodes/${episode.id}/retry`,
      { method: "POST" }
    );
    fetchEpisode();
  }

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
            {episode.recap && !episode.showNotes && (
              <> &middot; {episode.recap}</>
            )}
          </p>
          {episode.status === "FAILED" && (
            <div className="mt-1">
              {episode.pipelineStage && (
                <p className="text-sm text-muted-foreground">Failed at: {episode.pipelineStage}</p>
              )}
              {episode.errorMessage && (
                <p className="text-sm text-destructive">{episode.errorMessage}</p>
              )}
            </div>
          )}
        </div>
        <div className="flex items-center gap-2">
          {episode.status === "PENDING_REVIEW" && (
            <>
              <Button size="icon-lg" title="Approve episode" onClick={() => handleAction("approve")}>
                <Check className="size-4" />
              </Button>
              {!sameDayPublished && (
              <Button
                size="icon-lg"
                title="Regenerate episode"
                disabled={regenerating}
                onClick={() => setConfirmRegenerate(true)}
              >
                <RefreshCw className={`size-4 ${regenerating ? "animate-spin" : ""}`} />
              </Button>
              )}
              <Button
                size="icon-lg"
                variant="destructive"
                title="Discard episode"
                onClick={() => handleAction("discard")}
              >
                <X className="size-4" />
              </Button>
            </>
          )}
          {episode.status === "GENERATED" && (
            <>
            <Button size="icon-lg" title="Publish episode" onClick={() => setPublishOpen(true)}>
              <Upload className="size-4" />
            </Button>
            {!published && (
            <Button
              size="icon-lg"
              variant="destructive"
              title="Discard episode"
              onClick={() => handleAction("discard")}
            >
              <X className="size-4" />
            </Button>
            )}
            </>
          )}
          {episode.status === "FAILED" && (
            <Button size="icon-lg" title="Retry from failed stage" onClick={handleRetry}>
              <RotateCcw className="size-4" />
            </Button>
          )}
          {episode.status === "FAILED" && (
            <Button size="icon-lg" title="Retry audio generation" onClick={() => handleAction("approve")}>
              <Check className="size-4" />
            </Button>
          )}
          {(episode.status === "DISCARDED" || episode.status === "FAILED") && !sameDayPublished && (
            <Button
              size="icon-lg"
              title="Regenerate episode"
              disabled={regenerating}
              onClick={() => setConfirmRegenerate(true)}
            >
              <RefreshCw className={`size-4 ${regenerating ? "animate-spin" : ""}`} />
            </Button>
          )}
          {episode.status === "FAILED" && (
            <Button
              size="icon-lg"
              variant="destructive"
              title="Discard episode"
              onClick={() => handleAction("discard")}
            >
              <X className="size-4" />
            </Button>
          )}
        </div>
      </div>

      {episode.showNotes && (() => {
        const sourcesIdx = episode.showNotes.indexOf("\n\nSources:");
        const summary = sourcesIdx >= 0 ? episode.showNotes.slice(0, sourcesIdx) : episode.showNotes;
        const sources = sourcesIdx >= 0 ? episode.showNotes.slice(sourcesIdx + 2) : null;
        return (
          <div className="mb-6 text-sm text-muted-foreground bg-muted rounded-md p-4 overflow-hidden" style={{ wordBreak: "break-word" }}>
            <p>{summary}</p>
            {sources && (
              <details className="mt-3">
                <summary className="cursor-pointer text-sm font-medium text-foreground">Sources</summary>
                <div className="mt-2 text-xs whitespace-pre-wrap" style={{ wordBreak: "break-all" }}>{sources.replace(/^Sources:\n/, "")}</div>
              </details>
            )}
          </div>
        );
      })()}

      <Tabs value={currentTab} onValueChange={(v) => setTab(v as typeof TABS[number])}>
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

      <AlertDialog open={confirmRegenerate} onOpenChange={setConfirmRegenerate}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Regenerate episode?</AlertDialogTitle>
            <AlertDialogDescription>
              This will re-compose the script from the same articles using the current podcast settings. A new episode will be created.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={handleRegenerateConfirmed}>Regenerate</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
