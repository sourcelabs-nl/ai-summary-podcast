"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { useUser } from "@/lib/user-context";
import type { Podcast, PodcastDefaults } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useTabParam } from "@/hooks/use-tab-param";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { KeyValueEditor } from "@/components/key-value-editor";

const STYLES = [
  "news-briefing",
  "casual",
  "deep-dive",
  "executive-summary",
  "dialogue",
  "interview",
];

const TTS_PROVIDERS = ["openai", "elevenlabs", "inworld"];

const TABS = ["general", "llm", "tts", "content"] as const;

function FieldLabel({ children }: { children: React.ReactNode }) {
  return (
    <label className="text-sm font-medium leading-none">{children}</label>
  );
}

function FieldGroup({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="space-y-2">
      <FieldLabel>{label}</FieldLabel>
      {children}
    </div>
  );
}

const inputClass =
  "h-9 w-full rounded-md border border-input bg-background px-3 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50";

const textareaClass =
  "w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 min-h-[80px]";

export default function PodcastSettingsPage() {
  const params = useParams<{ podcastId: string }>();
  const { selectedUser, loading: userLoading } = useUser();
  const [podcast, setPodcast] = useState<Podcast | null>(null);
  const [form, setForm] = useState<Podcast | null>(null);
  const [loading, setLoading] = useState(true);
  const [defaults, setDefaults] = useState<PodcastDefaults | null>(null);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);
  const [currentTab, setTab] = useTabParam("general", TABS);

  useEffect(() => {
    if (!selectedUser) return;
    setLoading(true);
    Promise.all([
      fetch(`/api/users/${selectedUser.id}/podcasts/${params.podcastId}`)
        .then((res) => { if (!res.ok) throw new Error("Not found"); return res.json(); }),
      fetch("/api/config/defaults")
        .then((res) => (res.ok ? res.json() : null))
        .catch(() => null),
    ])
      .then(([podcastData, defaultsData]: [Podcast, PodcastDefaults | null]) => {
        setPodcast(podcastData);
        setForm(podcastData);
        setDefaults(defaultsData);
      })
      .catch(() => setPodcast(null))
      .finally(() => setLoading(false));
  }, [selectedUser, params.podcastId]);

  async function handleSave() {
    if (!selectedUser || !form) return;
    setSaving(true);
    setMessage(null);
    try {
      const res = await fetch(
        `/api/users/${selectedUser.id}/podcasts/${params.podcastId}`,
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            name: form.name,
            topic: form.topic,
            language: form.language,
            llmModels: form.llmModels,
            ttsProvider: form.ttsProvider,
            ttsVoices: form.ttsVoices,
            ttsSettings: form.ttsSettings,
            style: form.style,
            targetWords: form.targetWords,
            cron: form.cron,
            customInstructions: form.customInstructions,
            relevanceThreshold: form.relevanceThreshold,
            requireReview: form.requireReview,
            maxLlmCostCents: form.maxLlmCostCents,
            maxArticleAgeDays: form.maxArticleAgeDays,
            speakerNames: form.speakerNames,
            fullBodyThreshold: form.fullBodyThreshold,
            sponsor: form.sponsor,
            pronunciations: form.pronunciations,
          }),
        }
      );
      if (!res.ok) {
        const body = await res.json().catch(() => null);
        throw new Error(body?.error || `Save failed (${res.status})`);
      }
      const updated: Podcast = await res.json();
      setPodcast(updated);
      setForm(updated);
      setMessage({ type: "success", text: "Settings saved." });
    } catch (err) {
      setMessage({ type: "error", text: err instanceof Error ? err.message : "Save failed" });
    } finally {
      setSaving(false);
    }
  }

  function update<K extends keyof Podcast>(key: K, value: Podcast[K]) {
    setForm((prev) => (prev ? { ...prev, [key]: value } : prev));
  }

  function updateNumber(key: keyof Podcast, raw: string) {
    const parsed = raw === "" ? undefined : Number(raw);
    update(key, (parsed !== undefined && isNaN(parsed) ? undefined : parsed) as never);
  }

  if (userLoading || loading) {
    return <p className="text-muted-foreground">Loading...</p>;
  }

  if (!selectedUser || !podcast || !form) {
    return <p className="text-muted-foreground">Podcast not found.</p>;
  }

  return (
    <div>
      <div className="mb-4">
        <Link
          href={`/podcasts/${params.podcastId}`}
          className="text-sm text-muted-foreground hover:underline"
        >
          &larr; Back to podcast
        </Link>
      </div>
      <h2 className="mb-6 text-2xl font-bold">Settings: {podcast.name}</h2>

      <Tabs value={currentTab} onValueChange={(v) => setTab(v as typeof TABS[number])}>
        <TabsList>
          <TabsTrigger value="general">General</TabsTrigger>
          <TabsTrigger value="llm">LLM</TabsTrigger>
          <TabsTrigger value="tts">TTS</TabsTrigger>
          <TabsTrigger value="content">Content</TabsTrigger>
        </TabsList>

        <TabsContent value="general">
          <Card>
            <CardHeader>
              <CardTitle>General Settings</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <FieldGroup label="Name">
                <input
                  type="text"
                  value={form.name}
                  onChange={(e) => update("name", e.target.value)}
                  className={inputClass}
                />
              </FieldGroup>
              <FieldGroup label="Topic">
                <input
                  type="text"
                  value={form.topic}
                  onChange={(e) => update("topic", e.target.value)}
                  className={inputClass}
                />
              </FieldGroup>
              <FieldGroup label="Language">
                <input
                  type="text"
                  value={form.language}
                  onChange={(e) => update("language", e.target.value)}
                  className={inputClass}
                />
              </FieldGroup>
              <FieldGroup label="Style">
                <Select value={form.style} onValueChange={(v) => update("style", v)}>
                  <SelectTrigger className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {STYLES.map((s) => (
                      <SelectItem key={s} value={s}>
                        {s}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </FieldGroup>
              <FieldGroup label="Cron Schedule">
                <input
                  type="text"
                  value={form.cron}
                  onChange={(e) => update("cron", e.target.value)}
                  className={inputClass}
                />
              </FieldGroup>
              <FieldGroup label="Custom Instructions">
                <textarea
                  value={form.customInstructions ?? ""}
                  onChange={(e) => update("customInstructions", e.target.value || undefined)}
                  className={textareaClass}
                />
              </FieldGroup>
              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="requireReview"
                  checked={form.requireReview}
                  onChange={(e) => update("requireReview", e.target.checked)}
                  className="size-4 rounded border border-input"
                />
                <label htmlFor="requireReview" className="text-sm font-medium">
                  Require review before publishing
                </label>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="llm">
          <Card>
            <CardHeader>
              <CardTitle>LLM Settings</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <FieldGroup label="LLM Models">
                <KeyValueEditor
                  value={form.llmModels}
                  onChange={(v) => update("llmModels", v ?? undefined)}
                  keyPlaceholder="Stage (e.g. filter, compose)"
                  valuePlaceholder="Model name"
                />
                {defaults?.llmModels && (
                  <p className="text-xs text-muted-foreground">
                    System defaults: {Object.entries(defaults.llmModels).map(([k, v]) => `${k} = ${v}`).join(", ")}
                  </p>
                )}
              </FieldGroup>
              <FieldGroup label="Relevance Threshold">
                <input
                  type="number"
                  value={form.relevanceThreshold ?? ""}
                  onChange={(e) => updateNumber("relevanceThreshold", e.target.value)}
                  className={inputClass}
                />
              </FieldGroup>
              <FieldGroup label="Max LLM Cost (cents)">
                <input
                  type="number"
                  value={form.maxLlmCostCents ?? ""}
                  onChange={(e) => updateNumber("maxLlmCostCents", e.target.value)}
                  placeholder={defaults ? `${defaults.maxLlmCostCents} (system default)` : ""}
                  className={inputClass}
                />
              </FieldGroup>
              <FieldGroup label="Full Body Threshold">
                <input
                  type="number"
                  value={form.fullBodyThreshold ?? ""}
                  onChange={(e) => updateNumber("fullBodyThreshold", e.target.value)}
                  placeholder={defaults ? `${defaults.fullBodyThreshold} (system default)` : ""}
                  className={inputClass}
                />
              </FieldGroup>
              <FieldGroup label="Max Article Age (days)">
                <input
                  type="number"
                  value={form.maxArticleAgeDays ?? ""}
                  onChange={(e) => updateNumber("maxArticleAgeDays", e.target.value)}
                  placeholder={defaults ? `${defaults.maxArticleAgeDays} (system default)` : ""}
                  className={inputClass}
                />
              </FieldGroup>
              <FieldGroup label="Target Words">
                <input
                  type="number"
                  value={form.targetWords ?? ""}
                  onChange={(e) => updateNumber("targetWords", e.target.value)}
                  placeholder={defaults ? `${defaults.targetWords} (system default)` : ""}
                  className={inputClass}
                />
              </FieldGroup>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="tts">
          <Card>
            <CardHeader>
              <CardTitle>TTS Settings</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <FieldGroup label="TTS Provider">
                <Select
                  value={form.ttsProvider}
                  onValueChange={(v) => update("ttsProvider", v)}
                >
                  <SelectTrigger className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {TTS_PROVIDERS.map((p) => (
                      <SelectItem key={p} value={p}>
                        {p}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </FieldGroup>
              <FieldGroup label="TTS Voices">
                <KeyValueEditor
                  value={form.ttsVoices}
                  onChange={(v) => update("ttsVoices", v ?? undefined)}
                  keyPlaceholder="Role (e.g. default, host)"
                  valuePlaceholder="Voice ID"
                />
              </FieldGroup>
              <FieldGroup label="TTS Settings">
                <KeyValueEditor
                  value={form.ttsSettings}
                  onChange={(v) => update("ttsSettings", v ?? undefined)}
                  keyPlaceholder="Setting (e.g. speed)"
                  valuePlaceholder="Value"
                />
              </FieldGroup>
              <FieldGroup label="Speaker Names">
                <KeyValueEditor
                  value={form.speakerNames}
                  onChange={(v) => update("speakerNames", v ?? undefined)}
                  keyPlaceholder="Role (e.g. host, cohost)"
                  valuePlaceholder="Display name"
                />
              </FieldGroup>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="content">
          <Card>
            <CardHeader>
              <CardTitle>Content Settings</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <FieldGroup label="Sponsor">
                <KeyValueEditor
                  value={form.sponsor}
                  onChange={(v) => update("sponsor", v ?? undefined)}
                  keyPlaceholder="Field (e.g. name, message)"
                  valuePlaceholder="Value"
                />
              </FieldGroup>
              <FieldGroup label="Pronunciations">
                <KeyValueEditor
                  value={form.pronunciations}
                  onChange={(v) => update("pronunciations", v ?? undefined)}
                  keyPlaceholder="Word"
                  valuePlaceholder="IPA pronunciation"
                />
              </FieldGroup>
            </CardContent>
          </Card>
        </TabsContent>

      </Tabs>

      <div className="mt-6 flex items-center gap-4">
        <Button onClick={handleSave} disabled={saving}>
          {saving ? "Saving..." : "Save"}
        </Button>
        {message && (
          <p
            className={
              message.type === "success"
                ? "text-sm text-green-600"
                : "text-sm text-destructive"
            }
          >
            {message.text}
          </p>
        )}
      </div>
    </div>
  );
}
