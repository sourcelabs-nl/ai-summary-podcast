"use client";

import { createContext, useContext, useEffect, useRef, useCallback, type ReactNode } from "react";
import { toast } from "sonner";
import { useUser } from "./user-context";

interface PodcastEventData {
  podcastId: string;
  entityType: string;
  entityId: number;
  data: Record<string, unknown>;
}

type EventCallback = (event: string, data: PodcastEventData) => void;

interface EventContextType {
  subscribe: (callback: EventCallback) => () => void;
}

const EventContext = createContext<EventContextType>({
  subscribe: () => () => {},
});

const TOAST_EVENTS: Record<string, { message: (data: PodcastEventData) => string; type: "success" | "error" | "info" }> = {
  "episode.created": {
    message: () => "New episode pending review",
    type: "info",
  },
  "episode.approved": {
    message: (d) => `Episode #${d.data.episodeNumber ?? d.entityId} approved`,
    type: "success",
  },
  "episode.audio.started": {
    message: (d) => `Episode #${d.data.episodeNumber ?? d.entityId} audio generating...`,
    type: "info",
  },
  "episode.generated": {
    message: (d) => `Episode #${d.data.episodeNumber ?? d.entityId} audio ready`,
    type: "success",
  },
  "episode.failed": {
    message: (d) => `Episode #${d.data.episodeNumber ?? d.entityId} audio generation failed`,
    type: "error",
  },
  "episode.published": {
    message: (d) => `Episode #${d.data.episodeNumber ?? d.entityId} published to ${d.data.target ?? "target"}`,
    type: "success",
  },
  "episode.publish.failed": {
    message: (d) => `Episode #${d.data.episodeNumber ?? d.entityId} publish failed`,
    type: "error",
  },
  "episode.discarded": {
    message: (d) => `Episode #${d.data.episodeNumber ?? d.entityId} discarded`,
    type: "info",
  },
  "episode.unpublished": {
    message: (d) => `Episode #${d.data.episodeNumber ?? d.entityId} unpublished from ${d.data.target ?? "target"}`,
    type: "info",
  },
  "episode.retrying": {
    message: (d) => `Retrying episode #${d.data.episodeNumber ?? d.entityId} from ${(d.data.resumePoint as string ?? "pipeline").toLowerCase().replace("_", " ")}...`,
    type: "info",
  },
  "episode.stage": {
    message: (d) => {
      const stage = d.data.stage as string;
      if (stage === "dedup_saved") return `Saved ${d.data.articleCount ?? ""} article topics`;
      if (stage === "script_saved") return "Script saved";
      if (stage === "marking_processed") return "Marking articles as processed...";
      if (stage === "generating_recap") return "Generating recap...";
      return null as unknown as string;
    },
    type: "info",
  },
  "pipeline.progress": {
    message: (d) => {
      const stage = d.data.stage as string;
      if (stage === "aggregating") return `Aggregating ${d.data.postCount ?? ""} posts...`;
      if (stage === "scoring") return `Scoring ${d.data.articleCount ?? ""} articles...`;
      if (stage === "deduplicating") return `Deduplicating ${d.data.articleCount ?? ""} articles...`;
      if (stage === "composing") return `Composing episode script...`;
      return "Generating episode...";
    },
    type: "info",
  },
};

export function EventProvider({ children }: { children: ReactNode }) {
  const { selectedUser } = useUser();
  const subscribersRef = useRef<Set<EventCallback>>(new Set());

  const subscribe = useCallback((callback: EventCallback) => {
    subscribersRef.current.add(callback);
    return () => {
      subscribersRef.current.delete(callback);
    };
  }, []);

  useEffect(() => {
    if (!selectedUser) return;

    const eventSource = new EventSource(`http://localhost:8085/users/${selectedUser.id}/events`);

    eventSource.onmessage = (e: MessageEvent) => {
      try {
        const parsed = JSON.parse(e.data) as PodcastEventData & { event: string };
        const eventName = parsed.event;

        // Show toast for toast-worthy events
        const toastConfig = TOAST_EVENTS[eventName];
        if (toastConfig) {
          const message = toastConfig.message(parsed);
          if (message) {
            if (toastConfig.type === "error") {
              toast.error(message);
            } else if (toastConfig.type === "success") {
              toast.success(message);
            } else {
              toast.info(message);
            }
          }
        }

        // Notify all subscribers
        for (const callback of subscribersRef.current) {
          callback(eventName, parsed);
        }
      } catch {
        // Ignore unparseable events
      }
    };

    return () => {
      eventSource.close();
    };
  }, [selectedUser]);

  return (
    <EventContext.Provider value={{ subscribe }}>
      {children}
    </EventContext.Provider>
  );
}

export function useEventStream(
  podcastId: string | null | undefined,
  callback: (event: string, data: PodcastEventData) => void
) {
  const { subscribe } = useContext(EventContext);
  const callbackRef = useRef(callback);
  callbackRef.current = callback;

  useEffect(() => {
    return subscribe((event, data) => {
      if (!podcastId || data.podcastId === podcastId) {
        callbackRef.current(event, data);
      }
    });
  }, [subscribe, podcastId]);
}
