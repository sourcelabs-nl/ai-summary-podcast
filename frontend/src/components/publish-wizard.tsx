"use client";

import { useState } from "react";
import type { Episode, EpisodePublication } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";

const TARGETS = [
  { value: "soundcloud", label: "SoundCloud" },
] as const;

type Step = "select" | "confirm" | "result";

interface PublishWizardProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  episode: Episode;
  podcastName: string;
  userId: string;
  podcastId: string;
  onPublished: () => void;
}

export function PublishWizard({
  open,
  onOpenChange,
  episode,
  podcastName,
  userId,
  podcastId,
  onPublished,
}: PublishWizardProps) {
  const [step, setStep] = useState<Step>("select");
  const [target, setTarget] = useState<string>(TARGETS[0].value);
  const [publishing, setPublishing] = useState(false);
  const [result, setResult] = useState<EpisodePublication | null>(null);
  const [error, setError] = useState<string | null>(null);

  function reset() {
    setStep("select");
    setTarget(TARGETS[0].value);
    setPublishing(false);
    setResult(null);
    setError(null);
  }

  function handleClose(isOpen: boolean) {
    if (!isOpen) {
      if (result) onPublished();
      reset();
    }
    onOpenChange(isOpen);
  }

  async function handlePublish() {
    setPublishing(true);
    setError(null);
    try {
      const res = await fetch(
        `/api/users/${userId}/podcasts/${podcastId}/episodes/${episode.id}/publish/${target}`,
        { method: "POST" }
      );
      if (res.ok) {
        const data: EpisodePublication = await res.json();
        setResult(data);
        setStep("result");
      } else if (res.status === 409) {
        const label = TARGETS.find((t) => t.value === target)?.label ?? target;
        setError(`This episode has already been published to ${label}.`);
        setStep("result");
      } else {
        const body = await res.json().catch(() => ({ error: "Publishing failed" }));
        setError(body.error || "Publishing failed");
        setStep("result");
      }
    } catch {
      setError("Network error — could not reach the server.");
      setStep("result");
    } finally {
      setPublishing(false);
    }
  }

  const targetLabel = TARGETS.find((t) => t.value === target)?.label ?? target;

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-md">
        {step === "select" && (
          <>
            <DialogHeader>
              <DialogTitle>Publish Episode #{episode.id}</DialogTitle>
              <DialogDescription>Select a provider to publish to.</DialogDescription>
            </DialogHeader>
            <div className="flex flex-col gap-2 py-2">
              {TARGETS.map((t) => (
                <button
                  key={t.value}
                  onClick={() => setTarget(t.value)}
                  className={`flex items-center justify-between rounded-md border p-3 text-left transition-colors ${
                    target === t.value
                      ? "border-primary bg-primary/5"
                      : "border-border hover:bg-accent"
                  }`}
                >
                  <span className="font-medium">{t.label}</span>
                  {target === t.value && (
                    <Badge>Selected</Badge>
                  )}
                </button>
              ))}
            </div>
            <DialogFooter>
              <Button onClick={() => setStep("confirm")}>Next</Button>
            </DialogFooter>
          </>
        )}

        {step === "confirm" && (
          <>
            <DialogHeader>
              <DialogTitle>Confirm Publication</DialogTitle>
              <DialogDescription>
                Review the details before publishing.
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-2 py-2 text-sm">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Podcast</span>
                <span className="font-medium">{podcastName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Episode</span>
                <span className="font-medium">#{episode.id}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Date</span>
                <span>{new Date(episode.generatedAt).toLocaleDateString()}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Target</span>
                <Badge>{targetLabel}</Badge>
              </div>
              {episode.recap && (
                <div className="pt-2">
                  <span className="text-muted-foreground">Summary</span>
                  <p className="mt-1 text-foreground">{episode.recap}</p>
                </div>
              )}
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setStep("select")}>
                Back
              </Button>
              <Button onClick={handlePublish} disabled={publishing}>
                {publishing ? "Publishing..." : "Publish"}
              </Button>
            </DialogFooter>
          </>
        )}

        {step === "result" && (
          <>
            <DialogHeader>
              <DialogTitle>
                {error ? "Publication Failed" : "Published Successfully"}
              </DialogTitle>
            </DialogHeader>
            <div className="py-2">
              {error ? (
                <p className="text-sm text-destructive">{error}</p>
              ) : result ? (
                <div className="space-y-2 text-sm">
                  <p>Episode #{episode.id} has been published to {targetLabel}.</p>
                  {result.externalUrl && (
                    <div className="flex items-center gap-2">
                      <a
                        href={result.externalUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-primary underline"
                      >
                        Link
                      </a>
                      {result.target === "soundcloud" && (() => {
                        const match = result.externalUrl?.match(/^https:\/\/soundcloud\.com\/([^/]+)\//);
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
                    </div>
                  )}
                </div>
              ) : null}
            </div>
            <DialogFooter>
              <Button onClick={() => handleClose(false)}>Done</Button>
            </DialogFooter>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
