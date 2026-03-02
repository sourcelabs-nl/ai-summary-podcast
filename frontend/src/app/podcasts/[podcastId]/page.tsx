"use client";

import { useEffect, useState, useCallback } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { useUser } from "@/lib/user-context";
import type { Podcast, Episode } from "@/lib/types";
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
import { ScriptViewer } from "@/components/script-viewer";

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
  DISCARDED: "default",
};

export default function EpisodesPage() {
  const params = useParams<{ podcastId: string }>();
  const { selectedUser, loading: userLoading } = useUser();
  const [podcast, setPodcast] = useState<Podcast | null>(null);
  const [episodes, setEpisodes] = useState<Episode[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [scriptEpisode, setScriptEpisode] = useState<Episode | null>(null);

  const fetchEpisodes = useCallback(() => {
    if (!selectedUser) return;
    const url =
      statusFilter === "all"
        ? `/api/users/${selectedUser.id}/podcasts/${params.podcastId}/episodes`
        : `/api/users/${selectedUser.id}/podcasts/${params.podcastId}/episodes?status=${statusFilter}`;
    fetch(url)
      .then((res) => res.json())
      .then((data) => setEpisodes(data))
      .catch(() => setEpisodes([]));
  }, [selectedUser, params.podcastId, statusFilter]);

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
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [selectedUser, params.podcastId, statusFilter]);

  async function handleAction(episodeId: number, action: "approve" | "discard") {
    if (!selectedUser) return;
    await fetch(
      `/api/users/${selectedUser.id}/podcasts/${params.podcastId}/episodes/${episodeId}/${action}`,
      { method: "POST" }
    );
    fetchEpisodes();
  }

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
          <h2 className="text-2xl font-bold">{podcast.name}</h2>
          <p className="text-muted-foreground">{podcast.topic}</p>
        </div>
        <Badge>{podcast.style}</Badge>
      </div>

      <div className="mb-4">
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
              <TableHead className="w-20">#</TableHead>
              <TableHead>Date</TableHead>
              <TableHead>Status</TableHead>
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
                <TableCell>
                  <Badge variant={STATUS_VARIANT[episode.status] ?? "secondary"}>
                    {episode.status.replace("_", " ")}
                  </Badge>
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
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => setScriptEpisode(episode)}
                    >
                      View Script
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

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
    </div>
  );
}
