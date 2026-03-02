"use client";

import { useEffect, useState, useCallback, useMemo } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import cronstrue from "cronstrue";
import { Settings } from "lucide-react";
import { useUser } from "@/lib/user-context";
import type { Podcast, Episode, EpisodePublication } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ScriptViewer } from "@/components/script-viewer";
import { PublishWizard } from "@/components/publish-wizard";
import { PublicationsTab } from "@/components/publications-tab";

const STATUSES = [
  "GENERATED",
  "PENDING_REVIEW",
  "APPROVED",
  "FAILED",
  "DISCARDED",
] as const;

const STATUS_VARIANT: Record<string, "default" | "secondary" | "destructive" | "outline"> = {
  GENERATED: "default",
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
  const [scriptEpisode, setScriptEpisode] = useState<Episode | null>(null);
  const [publishEpisode, setPublishEpisode] = useState<Episode | null>(null);
  const [publishedEpisodeIds, setPublishedEpisodeIds] = useState<Set<number>>(new Set());
  const [refreshKey, setRefreshKey] = useState(0);

  const fetchPublications = useCallback(
    (episodeList: Episode[]) => {
      if (!selectedUser || episodeList.length === 0) {
        setPublishedEpisodeIds(new Set());
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
        results.forEach((pubs: EpisodePublication[], i) => {
          if (pubs.some((p) => p.status === "PUBLISHED")) {
            published.add(episodeList[i].id);
          }
        });
        setPublishedEpisodeIds(published);
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
    ])
      .then(([podcastData, episodeData]) => {
        setPodcast(podcastData);
        setEpisodes(episodeData);
        fetchPublications(episodeData);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [selectedUser, params.podcastId, statusFilter, fetchPublications]);

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
          <p className="text-muted-foreground">{podcast.topic}</p>
          {podcast.cron && cronDescription && (
            <p className="text-muted-foreground">
              Generates {cronDescription.toLowerCase()}
            </p>
          )}
        </div>
        <Link href={`/podcasts/${params.podcastId}/settings`}>
          <Button variant="outline" size="xs">
            <Settings className="size-3" />
            Settings
          </Button>
        </Link>
      </div>

      <Tabs defaultValue="episodes">
        <TabsList>
          <TabsTrigger value="episodes">Episodes</TabsTrigger>
          <TabsTrigger value="publications">Publications</TabsTrigger>
        </TabsList>

        <TabsContent value="episodes">
          <div className="mb-4 mt-4">
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="w-48">
                <SelectValue placeholder="Filter by status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All statuses</SelectItem>
                {STATUSES.map((status) => (
                  <SelectItem key={status} value={status}>
                    {status.replace("_", " ")}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {episodes.length === 0 ? (
            <p className="text-muted-foreground">No episodes found.</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-12">#</TableHead>
                  <TableHead className="w-24">Date</TableHead>
                  <TableHead className="w-12">Day</TableHead>
                  <TableHead className="w-32">Status</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {episodes.map((episode) => (
                  <TableRow key={episode.id}>
                    <TableCell className="font-medium">{episode.id}</TableCell>
                    <TableCell>
                      {new Date(episode.generatedAt).toLocaleDateString()}
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {new Date(episode.generatedAt).toLocaleDateString(undefined, { weekday: "short" })}
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-1.5">
                        <Badge variant={STATUS_VARIANT[episode.status] ?? "secondary"} className="text-[11px] px-1.5 py-px">
                          {episode.status.replace("_", " ")}
                        </Badge>
                        {publishedEpisodeIds.has(episode.id) && (
                          <Badge variant="outline" className="text-[11px] px-1.5 py-px">
                            Published
                          </Badge>
                        )}
                      </div>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex items-center justify-end gap-2">
                        {episode.status === "PENDING_REVIEW" && (
                          <>
                            <Button
                              size="sm"
                              onClick={() => handleAction(episode.id, "approve")}
                            >
                              Approve
                            </Button>
                            <Button
                              size="sm"
                              variant="destructive"
                              onClick={() => handleAction(episode.id, "discard")}
                            >
                              Discard
                            </Button>
                          </>
                        )}
                        {episode.status === "GENERATED" && !publishedEpisodeIds.has(episode.id) && (
                          <Button
                            size="sm"
                            className="w-20"
                            onClick={() => setPublishEpisode(episode)}
                          >
                            Publish
                          </Button>
                        )}
                        <Button
                          size="sm"
                          className="w-20"
                          variant="outline"
                          onClick={() => setScriptEpisode(episode)}
                        >
                          Script
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
      </Tabs>

      {scriptEpisode && (
        <ScriptViewer
          open={!!scriptEpisode}
          onOpenChange={(open) => {
            if (!open) setScriptEpisode(null);
          }}
          scriptText={scriptEpisode.scriptText}
          style={podcast.style}
          speakerNames={podcast.speakerNames}
        />
      )}

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
    </div>
  );
}
