"use client";

import { useEffect, useState } from "react";
import type { Episode, EpisodePublication } from "@/lib/types";
import { Cloud } from "lucide-react";
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

export function PublicationsTab({
  userId,
  podcastId,
  episodes,
  refreshKey,
  onRepublished,
}: PublicationsTabProps) {
  const [publications, setPublications] = useState<
    (EpisodePublication & { episodeNumber: number })[]
  >([]);
  const [loading, setLoading] = useState(true);
  const [confirmPub, setConfirmPub] = useState<(EpisodePublication & { episodeNumber: number }) | null>(null);
  const [republishing, setRepublishing] = useState(false);

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
            pubs.map((p) => ({ ...p, episodeNumber: ep.id }))
          )
          .catch(() => [] as (EpisodePublication & { episodeNumber: number })[])
      )
    )
      .then((results) => setPublications(results.flat()))
      .finally(() => setLoading(false));
  }, [userId, podcastId, episodes, refreshKey]);

  async function handleRepublish() {
    if (!confirmPub) return;
    setRepublishing(true);
    try {
      await fetch(
        `/api/users/${userId}/podcasts/${podcastId}/episodes/${confirmPub.episodeId}/publish/${confirmPub.target}`,
        { method: "POST" }
      );
      onRepublished();
    } catch {
      // error handled silently, refresh will show current state
    } finally {
      setRepublishing(false);
      setConfirmPub(null);
    }
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
              <TableCell>
                {pub.publishedAt
                  ? new Date(pub.publishedAt).toLocaleDateString()
                  : "—"}
              </TableCell>
              <TableCell>
                <Badge className="text-[11px] px-1.5 py-px">{pub.status}</Badge>
              </TableCell>
              <TableCell>
                <div className="flex items-center gap-1.5">
                  {pub.target === "soundcloud" && <Cloud className="size-4 text-muted-foreground" />}
                  <span>{pub.target === "soundcloud" ? "SoundCloud" : pub.target}</span>
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
                    </>
                  ) : (
                    <span className="text-muted-foreground">—</span>
                  )}
                </div>
              </TableCell>
              <TableCell className="text-right">
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => setConfirmPub(pub)}
                >
                  Republish
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      <Dialog open={!!confirmPub} onOpenChange={(open) => { if (!open) setConfirmPub(null); }}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Republish Episode</DialogTitle>
            <DialogDescription>
              Are you sure you want to republish episode #{confirmPub?.episodeNumber} to {confirmPub?.target === "soundcloud" ? "SoundCloud" : confirmPub?.target}?
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirmPub(null)}>
              Cancel
            </Button>
            <Button onClick={handleRepublish} disabled={republishing}>
              {republishing ? "Republishing..." : "Republish"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
