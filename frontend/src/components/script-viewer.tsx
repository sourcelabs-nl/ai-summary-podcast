"use client";

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

interface ScriptViewerProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  scriptText: string;
  style: string;
  speakerNames?: Record<string, string>;
}

interface SpeakerBlock {
  speaker: string;
  text: string;
}

const SPEAKER_STYLES: Record<string, { bg: string; name: string }> = {
  host: { bg: "bg-primary/10 border-primary/30", name: "text-primary" },
  cohost: { bg: "bg-emerald-500/10 border-emerald-500/30", name: "text-emerald-600" },
  interviewer: { bg: "bg-primary/10 border-primary/30", name: "text-primary" },
  expert: { bg: "bg-emerald-500/10 border-emerald-500/30", name: "text-emerald-600" },
};

const DEFAULT_SPEAKER_STYLE = { bg: "bg-violet-500/10 border-violet-500/30", name: "text-violet-600" };

function parseMultiSpeakerScript(scriptText: string): SpeakerBlock[] | null {
  const tagPattern = /<(\w+)>([\s\S]*?)<\/\1>/g;
  const blocks: SpeakerBlock[] = [];
  let match;

  while ((match = tagPattern.exec(scriptText)) !== null) {
    blocks.push({
      speaker: match[1],
      text: match[2].trim(),
    });
  }

  return blocks.length > 0 ? blocks : null;
}

function MonologueScript({ scriptText }: { scriptText: string }) {
  const paragraphs = scriptText.split(/\n\n+/).filter(Boolean);
  return (
    <div className="space-y-3">
      {paragraphs.map((paragraph, i) => (
        <div
          key={i}
          className="rounded-2xl border border-primary/20 bg-primary/5 px-4 py-3"
        >
          <p className="leading-relaxed">{paragraph}</p>
        </div>
      ))}
    </div>
  );
}

function MultiSpeakerScript({
  blocks,
  speakerNames,
}: {
  blocks: SpeakerBlock[];
  speakerNames?: Record<string, string>;
}) {
  const speakers = [...new Set(blocks.map((b) => b.speaker))];
  const isFirst = speakers[0];

  return (
    <div className="space-y-3">
      {blocks.map((block, i) => {
        const displayName =
          speakerNames?.[block.speaker] ?? block.speaker.toUpperCase();
        const styles = SPEAKER_STYLES[block.speaker] ?? DEFAULT_SPEAKER_STYLE;
        const alignRight = block.speaker !== isFirst;

        return (
          <div
            key={i}
            className={`flex ${alignRight ? "justify-end" : "justify-start"}`}
          >
            <div className={`max-w-[75%] space-y-1`}>
              <span
                className={`text-xs font-semibold ${styles.name} ${
                  alignRight ? "block text-right" : ""
                }`}
              >
                {displayName}
              </span>
              <div
                className={`rounded-2xl border px-4 py-3 ${styles.bg} ${
                  alignRight ? "rounded-tr-sm" : "rounded-tl-sm"
                }`}
              >
                <p className="leading-relaxed">{block.text}</p>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}

export function ScriptViewer({
  open,
  onOpenChange,
  scriptText,
  style,
  speakerNames,
}: ScriptViewerProps) {
  const isMultiSpeaker = style === "dialogue" || style === "interview";
  const blocks = isMultiSpeaker ? parseMultiSpeakerScript(scriptText) : null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[80vh] w-[90vw] !max-w-7xl overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Episode Script</DialogTitle>
        </DialogHeader>
        <div className="mt-4">
          {blocks ? (
            <MultiSpeakerScript blocks={blocks} speakerNames={speakerNames} />
          ) : (
            <MonologueScript scriptText={scriptText} />
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
