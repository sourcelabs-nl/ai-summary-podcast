"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Plus, Trash2 } from "lucide-react";

interface KeyValueRow {
  key: string;
  value: string;
}

interface KeyValueEditorProps {
  value: Record<string, string> | null | undefined;
  onChange: (value: Record<string, string>) => void;
  keyPlaceholder?: string;
  valuePlaceholder?: string;
}

function toRows(value: Record<string, string> | null | undefined): KeyValueRow[] {
  if (!value || Object.keys(value).length === 0) return [];
  return Object.entries(value).map(([key, val]) => ({ key, value: val }));
}

function toRecord(rows: KeyValueRow[]): Record<string, string> {
  const filtered = rows.filter((r) => r.key.trim() !== "");
  if (filtered.length === 0) return {};
  return Object.fromEntries(filtered.map((r) => [r.key, r.value]));
}

export function KeyValueEditor({
  value,
  onChange,
  keyPlaceholder = "Key",
  valuePlaceholder = "Value",
}: KeyValueEditorProps) {
  const [rows, setRows] = useState<KeyValueRow[]>(() => toRows(value));

  useEffect(() => {
    setRows(toRows(value));
  }, [value]);

  function updateRow(index: number, field: "key" | "value", newValue: string) {
    const updated = [...rows];
    updated[index] = { ...updated[index], [field]: newValue };
    setRows(updated);
    onChange(toRecord(updated));
  }

  function addRow() {
    setRows([...rows, { key: "", value: "" }]);
  }

  function removeRow(index: number) {
    const updated = rows.filter((_, i) => i !== index);
    setRows(updated);
    onChange(toRecord(updated));
  }

  return (
    <div className="space-y-2">
      {rows.map((row, index) => (
        <div key={index} className="flex items-center gap-2">
          <input
            type="text"
            value={row.key}
            onChange={(e) => updateRow(index, "key", e.target.value)}
            placeholder={keyPlaceholder}
            className="h-9 flex-1 rounded-md border border-input bg-background px-3 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
          />
          <input
            type="text"
            value={row.value}
            onChange={(e) => updateRow(index, "value", e.target.value)}
            placeholder={valuePlaceholder}
            className="h-9 flex-1 rounded-md border border-input bg-background px-3 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
          />
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            onClick={() => removeRow(index)}
          >
            <Trash2 className="size-4 text-muted-foreground" />
          </Button>
        </div>
      ))}
      <Button type="button" size="icon-lg" title="Add row" onClick={addRow}>
        <Plus className="size-4" />
      </Button>
    </div>
  );
}
