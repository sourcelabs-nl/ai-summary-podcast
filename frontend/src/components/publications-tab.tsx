"use client";

import { useEffect, useState } from "react";
import type { Episode, EpisodePublication } from "@/lib/types";
import { Cloud, RefreshCw, Server, Trash2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

interface PublicationsTabProps {
  userId: string;
  podcastId: string;
  episodes: Episode[];
  refreshKey: number;
  onRepublished: () => void;
}

const STATUS_VARIANT: Record<string, "default" | "secondary" | "destructive" | "outline"> = {
  PUBLISHED: "default",
  PENDING: "default",
  FAILED: "default",
  UNPUBLISHED: "secondary",
};

export function PublicationsTab({
  userId,
  podcastId,
  episodes,
  refreshKey,
  onRepublished,
}: PublicationsTabProps) {
  const [publications, setPublications] = useState<
    (EpisodePublication & { episodeNumber: number; episodeDate: string })[]
  >([]);
  const [loading, setLoading] = useState(true);
  const [confirmPub, setConfirmPub] = useState<(EpisodePublication & { episodeNumber: number; episodeDate: string }) | null>(null);
  const [confirmAction, setConfirmAction] = useState<"republish" | "unpublish">("republish");
  const [actionInProgress, setActionInProgress] = useState(false);

  useEffect(() => {
    if (episodes.length === 0) {
      setPublications([]);
      setLoading(false);
      return;
    }

    setLoading(true);
    Promise.all(
      episodes.map((ep) =>
        fetch(
          `/api/users/${userId}/podcasts/${podcastId}/episodes/${ep.id}/publications`
        )
          .then((res) => (res.ok ? res.json() : []))
          .then((pubs: EpisodePublication[]) =>
            pubs.map((p) => ({ ...p, episodeNumber: ep.id, episodeDate: ep.generatedAt }))
          )
          .catch(() => [] as (EpisodePublication & { episodeNumber: number; episodeDate: string })[])
      )
    )
      .then((results) => setPublications(results.flat()))
      .finally(() => setLoading(false));
  }, [userId, podcastId, episodes, refreshKey]);

  async function handleRepublish() {
    if (!confirmPub) return;
    setActionInProgress(true);
    try {
      await fetch(
        `/api/users/${userId}/podcasts/${podcastId}/episodes/${confirmPub.episodeId}/publish/${confirmPub.target}`,
        { method: "POST" }
      );
      onRepublished();
    } catch {
      // error handled silently, refresh will show current state
    } finally {
      setActionInProgress(false);
      setConfirmPub(null);
    }
  }

  async function handleUnpublish() {
    if (!confirmPub) return;
    setActionInProgress(true);
    try {
      await fetch(
        `/api/users/${userId}/podcasts/${podcastId}/episodes/${confirmPub.episodeId}/publications/${confirmPub.target}`,
        { method: "DELETE" }
      );
      onRepublished();
    } catch {
      // error handled silently
    } finally {
      setActionInProgress(false);
      setConfirmPub(null);
    }
  }

  function openConfirm(pub: typeof publications[0], action: "republish" | "unpublish") {
    setConfirmPub(pub);
    setConfirmAction(action);
  }

  if (loading) {
    return <p className="text-muted-foreground">Loading publications...</p>;
  }

  if (publications.length === 0) {
    return <p className="text-muted-foreground">No publications found.</p>;
  }

  return (
    <>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-12">#</TableHead>
            <TableHead className="w-24">Date</TableHead>
            <TableHead className="w-12">Day</TableHead>
            <TableHead className="w-24">Published</TableHead>
            <TableHead className="w-24">Status</TableHead>
            <TableHead className="w-32">Target</TableHead>
            <TableHead>URL</TableHead>
            <TableHead className="text-right">Actions</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {publications.map((pub) => (
            <TableRow key={pub.id}>
              <TableCell className="font-medium">{pub.episodeNumber}</TableCell>
              <TableCell className="text-sm">
                {new Date(pub.episodeDate).toLocaleDateString()}
              </TableCell>
              <TableCell className="text-sm text-muted-foreground">
                {new Date(pub.episodeDate).toLocaleDateString(undefined, { weekday: "short" })}
              </TableCell>
              <TableCell className="text-sm">
                {pub.publishedAt
                  ? new Date(pub.publishedAt).toLocaleDateString()
                  : "—"}
              </TableCell>
              <TableCell>
                <Badge variant={STATUS_VARIANT[pub.status] ?? "default"} className="text-[11px] px-1.5 py-px">{pub.status}</Badge>
              </TableCell>
              <TableCell>
                <div className="flex items-center gap-1.5">
                  {pub.target === "soundcloud" && <Cloud className="size-4 text-muted-foreground" />}
                  {pub.target === "ftp" && <Server className="size-4 text-muted-foreground" />}
                  <span>{pub.target === "soundcloud" ? "SoundCloud" : pub.target.toUpperCase()}</span>
                </div>
              </TableCell>
              <TableCell>
                <div className="flex items-center gap-2">
                  {pub.externalUrl ? (
                    <>
                      <a
                        href={pub.externalUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-primary underline"
                      >
                        Track
                      </a>
                      {pub.target === "soundcloud" && (() => {
                        const match = pub.externalUrl?.match(/^https:\/\/soundcloud\.com\/([^/]+)\//);
                        if (!match) return null;
                        return (
                          <>
                            <span className="text-muted-foreground">|</span>
                            <a
                              href={`https://soundcloud.com/${match[1]}/sets`}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-primary underline"
                            >
                              Playlist
                            </a>
                          </>
                        );
                      })()}
                      {pub.target === "ftp" && (() => {
                        const episodesIdx = pub.externalUrl?.lastIndexOf("/episodes/");
                        if (episodesIdx == null || episodesIdx < 0) return null;
                        const feedUrl = pub.externalUrl!.substring(0, episodesIdx + 1) + "feed.xml";
                        return (
                          <>
                            <span className="text-muted-foreground">|</span>
                            <a
                              href={feedUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-primary underline"
                            >
                              Feed
                            </a>
                          </>
                        );
                      })()}
                    </>
                  ) : (
                    <span className="text-muted-foreground">—</span>
                  )}
                </div>
              </TableCell>
              <TableCell className="text-right">
                <div className="flex items-center justify-end gap-1">
                  <Button
                    size="icon-lg"
                    title="Republish"
                    onClick={() => openConfirm(pub, "republish")}
                  >
                    <RefreshCw className="size-4" />
                  </Button>
                  {pub.status === "PUBLISHED" && (
                    <Button
                      size="icon-lg"
                      variant="destructive"
                      title="Unpublish"
                      onClick={() => openConfirm(pub, "unpublish")}
                    >
                      <Trash2 className="size-4" />
                    </Button>
                  )}
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      <Dialog open={!!confirmPub} onOpenChange={(open) => { if (!open) setConfirmPub(null); }}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{confirmAction === "republish" ? "Republish" : "Unpublish"} Episode</DialogTitle>
            <DialogDescription>
              Are you sure you want to {confirmAction} episode #{confirmPub?.episodeNumber} {confirmAction === "republish" ? "to" : "from"} {confirmPub?.target === "soundcloud" ? "SoundCloud" : confirmPub?.target}?
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirmPub(null)}>
              Cancel
            </Button>
            {confirmAction === "republish" ? (
              <Button onClick={handleRepublish} disabled={actionInProgress}>
                {actionInProgress ? "Republishing..." : "Republish"}
              </Button>
            ) : (
              <Button variant="destructive" onClick={handleUnpublish} disabled={actionInProgress}>
                {actionInProgress ? "Unpublishing..." : "Unpublish"}
              </Button>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
