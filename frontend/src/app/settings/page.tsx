"use client";

import { Suspense, useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { Lock, Pencil, Plus, Save, TestTube, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { useUser } from "@/lib/user-context";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Switch } from "@/components/ui/switch";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { ProviderConfigDialog } from "@/components/provider-config-dialog";
import { useTabParam } from "@/hooks/use-tab-param";

interface ProviderConfig {
  category: string;
  provider: string;
  baseUrl: string;
}

interface FtpForm {
  host: string;
  port: number;
  username: string;
  password: string;
  useTls: boolean;
}

interface SoundCloudForm {
  clientId: string;
  clientSecret: string;
  callbackUri: string;
}

const TABS = ["profile", "api-keys", "publishing"] as const;

const defaultFtp: FtpForm = { host: "", port: 21, username: "", password: "", useTls: true };
const defaultSoundCloud: SoundCloudForm = { clientId: "", clientSecret: "", callbackUri: "" };

const inputClass =
  "h-9 w-full rounded-md border border-input bg-background px-3 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50";

export default function SettingsPage() {
  return (
    <Suspense fallback={<p className="text-muted-foreground">Loading...</p>}>
      <SettingsContent />
    </Suspense>
  );
}

function SettingsContent() {
  const { selectedUser, refreshSelectedUser, loading: userLoading } = useUser();
  const [currentTab, setTab] = useTabParam("profile", TABS);

  // Profile state
  const [name, setName] = useState("");
  const [savingName, setSavingName] = useState(false);

  // API Keys state
  const [configs, setConfigs] = useState<ProviderConfig[]>([]);
  const [loadingConfigs, setLoadingConfigs] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editConfig, setEditConfig] = useState<ProviderConfig | null>(null);

  // Publishing state
  const [ftp, setFtp] = useState<FtpForm>(defaultFtp);
  const [soundCloud, setSoundCloud] = useState<SoundCloudForm>(defaultSoundCloud);
  const [ftpHasExisting, setFtpHasExisting] = useState(false);
  const [scHasExisting, setScHasExisting] = useState(false);
  const [savingFtp, setSavingFtp] = useState(false);
  const [savingSc, setSavingSc] = useState(false);
  const [testingFtp, setTestingFtp] = useState(false);
  const [testingSc, setTestingSc] = useState(false);

  useEffect(() => {
    if (selectedUser) setName(selectedUser.name);
  }, [selectedUser]);

  const fetchConfigs = useCallback(() => {
    if (!selectedUser) return;
    setLoadingConfigs(true);
    fetch(`/api/users/${selectedUser.id}/api-keys`)
      .then((res) => (res.ok ? res.json() : []))
      .then((data: ProviderConfig[]) => {
        setConfigs(data);
        const publishing = data.filter((c) => c.category === "PUBLISHING");
        setFtpHasExisting(publishing.some((c) => c.provider === "ftp"));
        setScHasExisting(publishing.some((c) => c.provider === "soundcloud"));
      })
      .catch(() => setConfigs([]))
      .finally(() => setLoadingConfigs(false));
  }, [selectedUser]);

  useEffect(() => {
    fetchConfigs();
  }, [fetchConfigs]);

  // Profile handlers
  async function handleSaveName() {
    if (!selectedUser || !name.trim()) return;
    setSavingName(true);
    try {
      const res = await fetch(`/api/users/${selectedUser.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: name.trim() }),
      });
      if (!res.ok) throw new Error("Failed to update name");
      const updated = await res.json();
      refreshSelectedUser(updated);
      toast.success("Name updated.");
    } catch {
      toast.error("Failed to update name.");
    } finally {
      setSavingName(false);
    }
  }

  // API Keys handlers
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

  // Publishing handlers
  async function handleSaveFtp() {
    if (!selectedUser) return;
    setSavingFtp(true);
    try {
      const res = await fetch(`/api/users/${selectedUser.id}/api-keys/PUBLISHING`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ provider: "ftp", apiKey: JSON.stringify(ftp) }),
      });
      if (!res.ok) throw new Error("Failed to save FTP credentials");
      toast.success("FTP credentials saved.");
      setFtpHasExisting(true);
    } catch {
      toast.error("Failed to save FTP credentials.");
    } finally {
      setSavingFtp(false);
    }
  }

  async function handleSaveSoundCloud() {
    if (!selectedUser) return;
    setSavingSc(true);
    try {
      const res = await fetch(`/api/users/${selectedUser.id}/api-keys/PUBLISHING`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ provider: "soundcloud", apiKey: JSON.stringify(soundCloud) }),
      });
      if (!res.ok) throw new Error("Failed to save SoundCloud credentials");
      toast.success("SoundCloud credentials saved.");
      setScHasExisting(true);
    } catch {
      toast.error("Failed to save SoundCloud credentials.");
    } finally {
      setSavingSc(false);
    }
  }

  async function handleTestFtp() {
    if (!selectedUser) return;
    setTestingFtp(true);
    try {
      const res = await fetch(`/api/users/${selectedUser.id}/publishing/test/ftp`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(ftp),
      });
      if (!res.ok) {
        const body = await res.text();
        throw new Error(body || "Connection test failed");
      }
      toast.success("FTP connection successful.");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Connection test failed.");
    } finally {
      setTestingFtp(false);
    }
  }

  async function handleTestSoundCloud() {
    if (!selectedUser) return;
    setTestingSc(true);
    try {
      const res = await fetch(`/api/users/${selectedUser.id}/publishing/test/soundcloud`, {
        method: "POST",
      });
      if (!res.ok) {
        const body = await res.text();
        throw new Error(body || "Connection test failed");
      }
      toast.success("SoundCloud connection successful.");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Connection test failed.");
    } finally {
      setTestingSc(false);
    }
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

      <Tabs value={currentTab} onValueChange={(v) => setTab(v as typeof TABS[number])}>
        <TabsList>
          <TabsTrigger value="profile">Profile</TabsTrigger>
          <TabsTrigger value="api-keys">API Keys</TabsTrigger>
          <TabsTrigger value="publishing">Publishing</TabsTrigger>
        </TabsList>

        <TabsContent value="profile">
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
                  <Save className="mr-2 size-4" />
                  {savingName ? "Saving..." : "Save"}
                </Button>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="api-keys">
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
              ) : configs.filter((c) => c.category !== "PUBLISHING").length === 0 ? (
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
                    {configs.filter((c) => c.category !== "PUBLISHING").map((config) => (
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
        </TabsContent>

        <TabsContent value="publishing">
          <div className="space-y-4">
            {/* FTP Card */}
            <Card>
              <CardHeader>
                <CardTitle>FTP</CardTitle>
                {ftpHasExisting && (
                  <p className="text-sm text-muted-foreground">
                    Existing FTP credentials are stored. Fill in the form below to update them.
                  </p>
                )}
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="ftp-host">Host</Label>
                      <input
                        id="ftp-host"
                        className={inputClass}
                        value={ftp.host}
                        onChange={(e) => setFtp({ ...ftp, host: e.target.value })}
                        placeholder="ftp.example.com"
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="ftp-port">Port</Label>
                      <input
                        id="ftp-port"
                        type="number"
                        className={inputClass}
                        value={ftp.port}
                        onChange={(e) => setFtp({ ...ftp, port: parseInt(e.target.value) || 21 })}
                      />
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="ftp-username">Username</Label>
                      <input
                        id="ftp-username"
                        className={inputClass}
                        value={ftp.username}
                        onChange={(e) => setFtp({ ...ftp, username: e.target.value })}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="ftp-password">Password</Label>
                      <input
                        id="ftp-password"
                        type="password"
                        className={inputClass}
                        value={ftp.password}
                        onChange={(e) => setFtp({ ...ftp, password: e.target.value })}
                      />
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <Switch
                      id="ftp-tls"
                      checked={ftp.useTls}
                      onCheckedChange={(checked) => setFtp({ ...ftp, useTls: checked })}
                    />
                    <Label htmlFor="ftp-tls">Use TLS</Label>
                  </div>
                  <div className="flex items-center gap-2 pt-2">
                    <Button size="sm" onClick={handleTestFtp} disabled={testingFtp || !ftp.host}>
                      <TestTube className="mr-2 size-4" />
                      {testingFtp ? "Testing..." : "Test Connection"}
                    </Button>
                    <Button size="sm" onClick={handleSaveFtp} disabled={savingFtp || !ftp.host}>
                      <Save className="mr-2 size-4" />
                      {savingFtp ? "Saving..." : "Save"}
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* SoundCloud Card */}
            <Card>
              <CardHeader>
                <CardTitle>SoundCloud</CardTitle>
                {scHasExisting && (
                  <p className="text-sm text-muted-foreground">
                    Existing SoundCloud credentials are stored. Fill in the form below to update them.
                  </p>
                )}
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="sc-client-id">Client ID</Label>
                    <input
                      id="sc-client-id"
                      className={inputClass}
                      value={soundCloud.clientId}
                      onChange={(e) => setSoundCloud({ ...soundCloud, clientId: e.target.value })}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="sc-client-secret">Client Secret</Label>
                    <input
                      id="sc-client-secret"
                      type="password"
                      className={inputClass}
                      value={soundCloud.clientSecret}
                      onChange={(e) => setSoundCloud({ ...soundCloud, clientSecret: e.target.value })}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="sc-callback-uri">Callback URI</Label>
                    <input
                      id="sc-callback-uri"
                      className={inputClass}
                      value={soundCloud.callbackUri}
                      onChange={(e) => setSoundCloud({ ...soundCloud, callbackUri: e.target.value })}
                      placeholder="https://example.com/callback"
                    />
                  </div>
                  <div className="flex items-center gap-2 pt-2">
                    <Button size="sm" onClick={handleTestSoundCloud} disabled={testingSc}>
                      <TestTube className="mr-2 size-4" />
                      {testingSc ? "Testing..." : "Test Connection"}
                    </Button>
                    <Button size="sm" onClick={handleSaveSoundCloud} disabled={savingSc || !soundCloud.clientId}>
                      <Save className="mr-2 size-4" />
                      {savingSc ? "Saving..." : "Save"}
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>
      </Tabs>

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
