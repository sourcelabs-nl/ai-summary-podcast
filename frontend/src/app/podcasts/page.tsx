"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useUser } from "@/lib/user-context";
import type { Podcast } from "@/lib/types";
import { useRouter } from "next/navigation";
import { Card, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Settings } from "lucide-react";

export default function PodcastsPage() {
  const { selectedUser, loading: userLoading } = useUser();
  const router = useRouter();
  const [podcasts, setPodcasts] = useState<Podcast[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!selectedUser) return;
    setLoading(true);
    fetch(`/api/users/${selectedUser.id}/podcasts`)
      .then((res) => res.json())
      .then((data) => setPodcasts(data))
      .catch(() => setPodcasts([]))
      .finally(() => setLoading(false));
  }, [selectedUser]);

  if (userLoading || loading) {
    return <p className="text-muted-foreground">Loading...</p>;
  }

  if (!selectedUser) {
    return <p className="text-muted-foreground">Select a user to view podcasts.</p>;
  }

  if (podcasts.length === 0) {
    return <p className="text-muted-foreground">No podcasts found for this user.</p>;
  }

  return (
    <div>
      <h2 className="mb-4 text-2xl font-bold">Podcasts</h2>
      <div className="flex flex-col gap-3">
        {podcasts.map((podcast) => (
          <Link key={podcast.id} href={`/podcasts/${podcast.id}`}>
            <Card className="cursor-pointer transition-colors hover:bg-accent">
              <CardHeader className="flex flex-row items-center gap-4 py-3">
                <CardTitle className="text-lg">{podcast.name}</CardTitle>
                <Badge>{podcast.style}</Badge>
                <CardDescription className="ml-auto text-sm">
                  {podcast.topic}
                </CardDescription>
                <Button
                  size="icon-lg"
                  title="Settings"
                  onClick={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    router.push(`/podcasts/${podcast.id}/settings`);
                  }}
                >
                  <Settings className="size-4" />
                </Button>
              </CardHeader>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  );
}
