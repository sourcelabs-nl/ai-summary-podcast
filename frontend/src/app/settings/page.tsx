"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { Lock, Pencil, Plus, Trash2 } from "lucide-react";
import { useUser } from "@/lib/user-context";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { ProviderConfigDialog } from "@/components/provider-config-dialog";

interface ProviderConfig {
  category: string;
  provider: string;
  baseUrl: string;
}

export default function SettingsPage() {
  const { selectedUser, refreshSelectedUser, loading: userLoading } = useUser();
  const [name, setName] = useState("");
  const [savingName, setSavingName] = useState(false);
  const [nameMessage, setNameMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);

  const [configs, setConfigs] = useState<ProviderConfig[]>([]);
  const [loadingConfigs, setLoadingConfigs] = useState(true);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editConfig, setEditConfig] = useState<ProviderConfig | null>(null);

  useEffect(() => {
    if (selectedUser) {
      setName(selectedUser.name);
    }
  }, [selectedUser]);

  const fetchConfigs = useCallback(() => {
    if (!selectedUser) return;
    setLoadingConfigs(true);
    fetch(`/api/users/${selectedUser.id}/api-keys`)
      .then((res) => (res.ok ? res.json() : []))
      .then((data: ProviderConfig[]) => setConfigs(data))
      .catch(() => setConfigs([]))
      .finally(() => setLoadingConfigs(false));
  }, [selectedUser]);

  useEffect(() => {
    fetchConfigs();
  }, [fetchConfigs]);

  async function handleSaveName() {
    if (!selectedUser || !name.trim()) return;
    setSavingName(true);
    setNameMessage(null);
    try {
      const res = await fetch(`/api/users/${selectedUser.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: name.trim() }),
      });
      if (!res.ok) throw new Error("Failed to update name");
      const updated = await res.json();
      refreshSelectedUser(updated);
      setNameMessage({ type: "success", text: "Name updated." });
    } catch {
      setNameMessage({ type: "error", text: "Failed to update name." });
    } finally {
      setSavingName(false);
    }
  }

  async function handleRemove(config: ProviderConfig) {
    if (!selectedUser) return;
    await fetch(`/api/users/${selectedUser.id}/api-keys/${config.category}/${config.provider}`, {
      method: "DELETE",
    });
    fetchConfigs();
  }

  function handleEdit(config: ProviderConfig) {
    setEditConfig(config);
    setDialogOpen(true);
  }

  function handleAdd() {
    setEditConfig(null);
    setDialogOpen(true);
  }

  function handleDialogSaved() {
    setDialogOpen(false);
    setEditConfig(null);
    fetchConfigs();
  }

  if (userLoading) {
    return <p className="text-muted-foreground">Loading...</p>;
  }

  if (!selectedUser) {
    return <p className="text-muted-foreground">No user selected.</p>;
  }

  return (
    <div>
      <div className="mb-4">
        <Link href="/podcasts" className="text-sm text-muted-foreground hover:underline">
          &larr; Back to podcasts
        </Link>
      </div>
      <h2 className="mb-6 text-2xl font-bold">Settings</h2>

      <div className="space-y-8">
        <Card>
          <CardHeader>
            <CardTitle>Profile</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-end gap-4">
              <div className="flex-1 space-y-2">
                <Label htmlFor="user-name">Name</Label>
                <Input
                  id="user-name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
              </div>
              <Button size="sm" onClick={handleSaveName} disabled={savingName}>
                {savingName ? "Saving..." : "Save"}
              </Button>
            </div>
            {nameMessage && (
              <p className={`mt-2 text-sm ${nameMessage.type === "success" ? "text-green-600" : "text-destructive"}`}>
                {nameMessage.text}
              </p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle>API Keys</CardTitle>
              <Button size="icon-lg" title="Add provider" onClick={handleAdd}>
                <Plus className="size-4" />
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            <div className="mb-4 flex items-center gap-2 rounded-md border border-border bg-muted/50 px-3 py-2 text-sm text-muted-foreground">
              <Lock className="size-4 shrink-0" />
              All API keys are stored encrypted. Decryption requires the application master key.
            </div>

            {loadingConfigs ? (
              <p className="text-muted-foreground text-sm">Loading...</p>
            ) : configs.length === 0 ? (
              <p className="text-muted-foreground text-sm">No API keys configured yet.</p>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Provider</TableHead>
                    <TableHead>Category</TableHead>
                    <TableHead>Base URL</TableHead>
                    <TableHead className="text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {configs.map((config) => (
                    <TableRow key={`${config.category}-${config.provider}`}>
                      <TableCell className="font-medium">{config.provider}</TableCell>
                      <TableCell>
                        <Badge variant={config.category === "LLM" ? "default" : "outline"}>
                          {config.category}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-sm text-muted-foreground">{config.baseUrl}</TableCell>
                      <TableCell className="text-right">
                        <div className="flex items-center justify-end gap-2">
                          <Button variant="ghost" size="sm" title="Edit provider" onClick={() => handleEdit(config)}>
                            <Pencil className="size-4" />
                          </Button>
                          <Button variant="ghost" size="sm" title="Remove provider" onClick={() => handleRemove(config)}>
                            <Trash2 className="size-4" />
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </div>

      <ProviderConfigDialog
        open={dialogOpen}
        onOpenChange={(open) => {
          if (!open) {
            setDialogOpen(false);
            setEditConfig(null);
          }
        }}
        userId={selectedUser.id}
        editConfig={editConfig}
        existingConfigs={configs}
        onSaved={handleDialogSaved}
      />
    </div>
  );
}
