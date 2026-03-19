"use client";

import { useEffect, useState, useCallback, useMemo, useRef } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import cronstrue from "cronstrue";
import { CronExpressionParser } from "cron-parser";
import { Check, ChevronDown, ChevronRight, Clock, Loader2, RefreshCw, Settings, Upload, X } from "lucide-react";
import { useUser } from "@/lib/user-context";
import { useEventStream } from "@/lib/event-context";
import type { Podcast, Episode, EpisodePublication, EpisodeArticle } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
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
import { PublishWizard, TARGETS } from "@/components/publish-wizard";
import { PublicationsTab } from "@/components/publications-tab";
import { SourcesTab } from "@/components/sources-tab";
import { useTabParam } from "@/hooks/use-tab-param";

const TABS = ["episodes", "publications", "sources"] as const;

const STATUSES = [
  "GENERATED",
  "PENDING_REVIEW",
  "APPROVED",
  "FAILED",
  "DISCARDED",
] as const;

const STATUS_VARIANT: Record<string, "default" | "secondary" | "destructive" | "outline"> = {
  GENERATED: "outline",
  PENDING_REVIEW: "default",
  APPROVED: "default",
  FAILED: "default",
  DISCARDED: "secondary",
};

export default function EpisodesPage() {
  const params = useParams<{ podcastId: string }>();
  const { selectedUser, loading: userLoading } = useUser();
  const [podcast, setPodcast] = useState<Podcast | null>(null);
  const [episodes, setEpisodes] = useState<Episode[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [publishEpisode, setPublishEpisode] = useState<Episode | null>(null);
  const router = useRouter();
  const [publishedEpisodeIds, setPublishedEpisodeIds] = useState<Set<number>>(new Set());
  const [fullyPublishedEpisodeIds, setFullyPublishedEpisodeIds] = useState<Set<number>>(new Set());
  const [refreshKey, setRefreshKey] = useState(0);
  const [upcomingCount, setUpcomingCount] = useState<number>(0);
  const [upcomingPostCount, setUpcomingPostCount] = useState<number>(0);
  const [countdown, setCountdown] = useState<string | null>(null);
  const [pipelineStage, setPipelineStage] = useState<string | null>(null);
  const [regeneratingId, setRegeneratingId] = useState<number | null>(null);
  const [confirmRegenerateId, setConfirmRegenerateId] = useState<number | null>(null);
  const [currentTab, setTab] = useTabParam("episodes", TABS);

  const publishedDates = useMemo(() => {
    const dates = new Set<string>();
    for (const ep of episodes) {
      if (publishedEpisodeIds.has(ep.id)) {
        dates.add(new Date(ep.generatedAt).toLocaleDateString());
      }
    }
    return dates;
  }, [episodes, publishedEpisodeIds]);

  const fetchPublications = useCallback(
    (episodeList: Episode[]) => {
      if (!selectedUser || episodeList.length === 0) {
        setPublishedEpisodeIds(new Set());
        setFullyPublishedEpisodeIds(new Set());
        return;
      }
      Promise.all(
        episodeList.map((ep) =>
          fetch(
            `/api/users/${selectedUser.id}/podcasts/${params.podcastId}/episodes/${ep.id}/publications`
          )
            .then((res) => (res.ok ? res.json() : []))
            .catch(() => [] as EpisodePublication[])
        )
      ).then((results) => {
        const published = new Set<number>();
        const fullyPublished = new Set<number>();
        const targetValues = TARGETS.map((t) => t.value);
        results.forEach((pubs: EpisodePublication[], i) => {
          const publishedTargets = new Set(
            pubs.filter((p) => p.status === "PUBLISHED").map((p) => p.target)
          );
          if (publishedTargets.size > 0) {
            published.add(episodeList[i].id);
          }
          if (targetValues.every((t) => publishedTargets.has(t))) {
            fullyPublished.add(episodeList[i].id);
          }
        });
        setPublishedEpisodeIds(published);
        setFullyPublishedEpisodeIds(fullyPublished);
      });
    },
    [selectedUser, params.podcastId]
  );

  const fetchEpisodes = useCallback(() => {
    if (!selectedUser) return;
    const url =
      statusFilter === "all"
        ? `/api/users/${selectedUser.id}/podcasts/${params.podcastId}/episodes`
        : `/api/users/${selectedUser.id}/podcasts/${params.podcastId}/episodes?status=${statusFilter}`;
    fetch(url)
      .then((res) => res.json())
      .then((data) => {
        setEpisodes(data);
        fetchPublications(data);
      })
      .catch(() => setEpisodes([]));
  }, [selectedUser, params.podcastId, statusFilter, fetchPublications]);

  useEventStream(params.podcastId, useCallback((event: string, data: { data: Record<string, unknown> }) => {
    if (event === "pipeline.progress") {
      setPipelineStage(data.data.stage as string);
    } else {
      if (event === "episode.created" || event === "episode.generated" || event === "episode.failed") {
        setPipelineStage(null);
      }
      fetchEpisodes();
    }
  }, [fetchEpisodes]));

  useEffect(() => {
    if (!selectedUser) return;
    setLoading(true);
    Promise.all([
      fetch(`/api/users/${selectedUser.id}/podcasts/${params.podcastId}`).then(
        (res) => res.json()
      ),
      fetch(
        `/api/users/${selectedUser.id}/podcasts/${params.podcastId}/episodes${
          statusFilter !== "all" ? `?status=${statusFilter}` : ""
        }`
      ).then((res) => res.json()),
      fetch(`/api/users/${selectedUser.id}/podcasts/${params.podcastId}/upcoming-articles`)
        .then((res) => (res.ok ? res.json() : { articles: [], postCount: 0 }))
        .catch(() => ({ articles: [], postCount: 0 })),
      fetch(`/api/users/${selectedUser.id}/podcasts/${params.podcastId}/pipeline-status`)
        .then((res) => (res.ok ? res.json() : { stage: null }))
        .catch(() => ({ stage: null })),
    ])
      .then(([podcastData, episodeData, upcomingData, pipelineData]) => {
        setPodcast(podcastData);
        setEpisodes(episodeData);
        setUpcomingCount(upcomingData.articleCount ?? 0);
        setUpcomingPostCount(upcomingData.postCount ?? 0);
        setPipelineStage(pipelineData.stage ?? null);
        fetchPublications(episodeData);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [selectedUser, params.podcastId, statusFilter, fetchPublications]);

  async function handleRegenerateConfirmed() {
    if (!selectedUser || confirmRegenerateId === null) return;
    const episodeId = confirmRegenerateId;
    setConfirmRegenerateId(null);
    setRegeneratingId(episodeId);
    try {
      const res = await fetch(
        `/api/users/${selectedUser.id}/podcasts/${params.podcastId}/episodes/${episodeId}/regenerate`,
        { method: "POST" }
      );
      if (res.ok) {
        const data = await res.json();
        router.push(`/podcasts/${params.podcastId}/episodes/${data.episodeId}`);
      }
    } finally {
      setRegeneratingId(null);
    }
  }

  async function handleAction(episodeId: number, action: "approve" | "discard") {
    if (!selectedUser) return;
    await fetch(
      `/api/users/${selectedUser.id}/podcasts/${params.podcastId}/episodes/${episodeId}/${action}`,
      { method: "POST" }
    );
    fetchEpisodes();
  }

  function handlePublished() {
    fetchEpisodes();
    setRefreshKey((k) => k + 1);
  }

  const cronDescription = useMemo(() => {
    if (!podcast?.cron) return null;
    try {
      return cronstrue.toString(podcast.cron);
    } catch {
      return podcast.cron;
    }
  }, [podcast?.cron]);

  useEffect(() => {
    if (!podcast?.cron) {
      setCountdown(null);
      return;
    }
    function computeCountdown() {
      try {
        const expr = CronExpressionParser.parse(podcast!.cron, { tz: 'UTC' });
        const next = expr.next().toDate();
        const diff = next.getTime() - Date.now();
        if (diff <= 0) { setCountdown(null); return; }
        const h = Math.floor(diff / 3_600_000);
        const m = Math.floor((diff % 3_600_000) / 60_000);
        const s = Math.floor((diff % 60_000) / 1_000);
        setCountdown(
          h > 0
            ? `${h}h ${String(m).padStart(2, "0")}m ${String(s).padStart(2, "0")}s`
            : m > 0
              ? `${m}m ${String(s).padStart(2, "0")}s`
              : `${s}s`
        );
      } catch {
        setCountdown(null);
      }
    }
    computeCountdown();
    const id = setInterval(computeCountdown, 1_000);
    return () => clearInterval(id);
  }, [podcast?.cron]);

  if (userLoading || loading) {
    return <p className="text-muted-foreground">Loading...</p>;
  }

  if (!selectedUser || !podcast) {
    return <p className="text-muted-foreground">Podcast not found.</p>;
  }

  return (
    <div>
      <div className="mb-4">
        <Link href="/podcasts" className="text-sm text-muted-foreground hover:underline">
          &larr; Back to podcasts
        </Link>
      </div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <div className="flex items-center gap-3">
            <h2 className="text-2xl font-bold">{podcast.name}</h2>
            <Badge>{podcast.style}</Badge>
          </div>
          <p className="text-sm text-muted-foreground">
            {podcast.topic}
            {podcast.cron && cronDescription && (
              <span> &middot; {cronDescription.toLowerCase()}</span>
            )}
          </p>
        </div>
        <Link href={`/podcasts/${params.podcastId}/settings`}>
          <Button size="icon-lg" title="Settings">
            <Settings className="size-4" />
          </Button>
        </Link>
      </div>

      <Link
        href={`/podcasts/${params.podcastId}/upcoming`}
        className={`mb-6 flex items-center justify-between rounded-lg border px-4 py-3 transition-colors ${
          pipelineStage
            ? "border-primary/50 bg-primary/5"
            : "border-border bg-muted/50 hover:bg-muted"
        }`}
      >
        {pipelineStage ? (
          <span className="flex items-center gap-2 text-sm font-medium">
            <Loader2 className="size-4 animate-spin text-primary" />
            {pipelineStage === "aggregating" && "Aggregating posts..."}
            {pipelineStage === "scoring" && "Scoring articles..."}
            {pipelineStage === "composing" && "Composing script..."}
          </span>
        ) : (
          <span className="text-sm font-medium">
            Next Episode {upcomingCount > 0
              ? <>&middot; {upcomingCount} article{upcomingCount !== 1 ? "s" : ""}{upcomingPostCount > upcomingCount ? ` / ${upcomingPostCount} posts` : ""} ready</>
              : <>&middot; no articles yet</>}
          </span>
        )}
        <div className="flex items-center gap-2">
          {!pipelineStage && countdown && (
            <span className="flex items-center gap-1 text-xs text-muted-foreground tabular-nums">
              <Clock className="size-3" />
              {countdown}
            </span>
          )}
          <ChevronRight className="size-4 text-muted-foreground" />
        </div>
      </Link>

      <Tabs value={currentTab} onValueChange={(v) => setTab(v as typeof TABS[number])}>
        <TabsList>
          <TabsTrigger value="episodes">Episodes</TabsTrigger>
          <TabsTrigger value="publications">Publications</TabsTrigger>
          <TabsTrigger value="sources">Sources</TabsTrigger>
        </TabsList>

        <TabsContent value="episodes">
          {episodes.length === 0 ? (
            <p className="text-muted-foreground">No episodes found.</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-12">#</TableHead>
                  <TableHead className="w-24">Date</TableHead>
                  <TableHead className="w-12">Day</TableHead>
                  <TableHead className="w-32">
                    <DropdownMenu>
                      <DropdownMenuTrigger className="flex items-center gap-1 hover:text-foreground transition-colors">
                        Status
                        <ChevronDown className="size-3.5" />
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="start">
                        <DropdownMenuCheckboxItem
                          checked={statusFilter === "all"}
                          onCheckedChange={() => setStatusFilter("all")}
                        >
                          All statuses
                        </DropdownMenuCheckboxItem>
                        {STATUSES.map((status) => (
                          <DropdownMenuCheckboxItem
                            key={status}
                            checked={statusFilter === status}
                            onCheckedChange={() => setStatusFilter(status)}
                          >
                            {status.replace("_", " ").toLowerCase()}
                          </DropdownMenuCheckboxItem>
                        ))}
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableHead>
                  <TableHead className="w-0">Script Model</TableHead>
                  <TableHead className="w-0">TTS Model</TableHead>
                  <TableHead className="w-20 text-right">Cost</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {episodes.map((episode) => (
                  <TableRow
                    key={episode.id}
                    className="cursor-pointer"
                    onClick={() => router.push(`/podcasts/${params.podcastId}/episodes/${episode.id}`)}
                  >
                    <TableCell className="text-sm font-medium">{episode.id}</TableCell>
                    <TableCell className="text-sm">
                      {new Date(episode.generatedAt).toLocaleDateString()}
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {new Date(episode.generatedAt).toLocaleDateString(undefined, { weekday: "short" })}
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-1.5">
                        <Badge variant={STATUS_VARIANT[episode.status] ?? "secondary"} className="text-[11px] px-1.5 py-px">
                          {episode.status.replace("_", " ")}
                        </Badge>
                        {publishedEpisodeIds.has(episode.id) && (
                          <Badge variant="default" className="text-[11px] px-1.5 py-px">
                            Published
                          </Badge>
                        )}
                      </div>
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground whitespace-nowrap">
                      {episode.composeModel ?? "—"}
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground whitespace-nowrap">
                      {episode.ttsModel ?? "—"}
                    </TableCell>
                    <TableCell className="text-sm text-right text-muted-foreground">
                      {((episode.llmCostCents ?? 0) + (episode.ttsCostCents ?? 0)) > 0
                        ? `$${(((episode.llmCostCents ?? 0) + (episode.ttsCostCents ?? 0)) / 100).toFixed(2)}`
                        : "—"}
                    </TableCell>
                    <TableCell className="text-right h-12">
                      <div className="flex items-center justify-end gap-2" onClick={(e) => e.stopPropagation()}>
                        {episode.status === "PENDING_REVIEW" && (
                          <>
                            <Button
                              size="icon-lg"
                              title="Approve episode"
                              onClick={() => handleAction(episode.id, "approve")}
                            >
                              <Check className="size-4" />
                            </Button>
                            {!publishedDates.has(new Date(episode.generatedAt).toLocaleDateString()) && (
                            <Button
                              size="icon-lg"
                              title="Regenerate episode"
                              disabled={regeneratingId === episode.id}
                              onClick={() => setConfirmRegenerateId(episode.id)}
                            >
                              <RefreshCw className={`size-4 ${regeneratingId === episode.id ? "animate-spin" : ""}`} />
                            </Button>
                            )}
                            <Button
                              size="icon-lg"
                              variant="destructive"
                              title="Discard episode"
                              onClick={() => handleAction(episode.id, "discard")}
                            >
                              <X className="size-4" />
                            </Button>
                          </>
                        )}
                        {episode.status === "GENERATED" && (
                          <>
                          {!fullyPublishedEpisodeIds.has(episode.id) && (
                          <Button
                            size="icon-lg"
                            title="Publish episode"
                            onClick={() => setPublishEpisode(episode)}
                          >
                            <Upload className="size-4" />
                          </Button>
                          )}
                          {!publishedEpisodeIds.has(episode.id) && (
                          <Button
                            size="icon-lg"
                            variant="destructive"
                            title="Discard episode"
                            onClick={() => handleAction(episode.id, "discard")}
                          >
                            <X className="size-4" />
                          </Button>
                          )}
                          </>
                        )}
                        {episode.status === "DISCARDED" && !publishedDates.has(new Date(episode.generatedAt).toLocaleDateString()) && (
                          <Button
                            size="icon-lg"
                            title="Regenerate episode"
                            disabled={regeneratingId === episode.id}
                            onClick={() => setConfirmRegenerateId(episode.id)}
                          >
                            <RefreshCw className={`size-4 ${regeneratingId === episode.id ? "animate-spin" : ""}`} />
                          </Button>
                        )}
                        <Button
                          size="icon-lg"
                          title="View details"
                          onClick={() => router.push(`/podcasts/${params.podcastId}/episodes/${episode.id}`)}
                        >
                          <ChevronRight className="size-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </TabsContent>

        <TabsContent value="publications">
          <div className="mt-4">
            <PublicationsTab
              userId={selectedUser.id}
              podcastId={params.podcastId}
              episodes={episodes}
              refreshKey={refreshKey}
              onRepublished={handlePublished}
            />
          </div>
        </TabsContent>

        <TabsContent value="sources">
          <div className="mt-4">
            <SourcesTab userId={selectedUser.id} podcastId={params.podcastId} />
          </div>
        </TabsContent>
      </Tabs>

      {publishEpisode && (
        <PublishWizard
          open={!!publishEpisode}
          onOpenChange={(open) => {
            if (!open) setPublishEpisode(null);
          }}
          episode={publishEpisode}
          podcastName={podcast.name}
          userId={selectedUser.id}
          podcastId={params.podcastId}
          onPublished={handlePublished}
        />
      )}

      <AlertDialog open={confirmRegenerateId !== null} onOpenChange={(open) => { if (!open) setConfirmRegenerateId(null); }}>
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
