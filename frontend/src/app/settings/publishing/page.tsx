"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { Save, TestTube } from "lucide-react";
import { useUser } from "@/lib/user-context";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { Switch } from "@/components/ui/switch";

const inputClass =
  "h-9 w-full rounded-md border border-input bg-background px-3 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50";

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

interface ProviderConfig {
  category: string;
  provider: string;
  baseUrl: string;
}

const defaultFtp: FtpForm = { host: "", port: 21, username: "", password: "", useTls: true };
const defaultSoundCloud: SoundCloudForm = { clientId: "", clientSecret: "", callbackUri: "" };

export default function PublishingSettingsPage() {
  const { selectedUser, loading: userLoading } = useUser();

  const [ftp, setFtp] = useState<FtpForm>(defaultFtp);
  const [soundCloud, setSoundCloud] = useState<SoundCloudForm>(defaultSoundCloud);

  const [ftpHasExisting, setFtpHasExisting] = useState(false);
  const [scHasExisting, setScHasExisting] = useState(false);

  const [savingFtp, setSavingFtp] = useState(false);
  const [savingSc, setSavingSc] = useState(false);
  const [testingFtp, setTestingFtp] = useState(false);
  const [testingSc, setTestingSc] = useState(false);

  const [ftpMessage, setFtpMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);
  const [scMessage, setScMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);
  const [ftpTestMessage, setFtpTestMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);
  const [scTestMessage, setScTestMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);

  const loadExisting = useCallback(() => {
    if (!selectedUser) return;
    fetch(`/api/users/${selectedUser.id}/api-keys`)
      .then((res) => (res.ok ? res.json() : []))
      .then((data: ProviderConfig[]) => {
        const publishing = data.filter((c) => c.category === "PUBLISHING");
        setFtpHasExisting(publishing.some((c) => c.provider === "ftp"));
        setScHasExisting(publishing.some((c) => c.provider === "soundcloud"));
      })
      .catch(() => {});
  }, [selectedUser]);

  useEffect(() => {
    loadExisting();
  }, [loadExisting]);

  async function handleSaveFtp() {
    if (!selectedUser) return;
    setSavingFtp(true);
    setFtpMessage(null);
    try {
      const res = await fetch(`/api/users/${selectedUser.id}/api-keys/PUBLISHING`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          provider: "ftp",
          apiKey: JSON.stringify(ftp),
        }),
      });
      if (!res.ok) throw new Error("Failed to save FTP credentials");
      setFtpMessage({ type: "success", text: "FTP credentials saved." });
      setFtpHasExisting(true);
    } catch {
      setFtpMessage({ type: "error", text: "Failed to save FTP credentials." });
    } finally {
      setSavingFtp(false);
    }
  }

  async function handleSaveSoundCloud() {
    if (!selectedUser) return;
    setSavingSc(true);
    setScMessage(null);
    try {
      const res = await fetch(`/api/users/${selectedUser.id}/api-keys/PUBLISHING`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          provider: "soundcloud",
          apiKey: JSON.stringify(soundCloud),
        }),
      });
      if (!res.ok) throw new Error("Failed to save SoundCloud credentials");
      setScMessage({ type: "success", text: "SoundCloud credentials saved." });
      setScHasExisting(true);
    } catch {
      setScMessage({ type: "error", text: "Failed to save SoundCloud credentials." });
    } finally {
      setSavingSc(false);
    }
  }

  async function handleTestFtp() {
    if (!selectedUser) return;
    setTestingFtp(true);
    setFtpTestMessage(null);
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
      setFtpTestMessage({ type: "success", text: "FTP connection successful." });
    } catch (err) {
      setFtpTestMessage({ type: "error", text: err instanceof Error ? err.message : "Connection test failed." });
    } finally {
      setTestingFtp(false);
    }
  }

  async function handleTestSoundCloud() {
    if (!selectedUser) return;
    setTestingSc(true);
    setScTestMessage(null);
    try {
      const res = await fetch(`/api/users/${selectedUser.id}/publishing/test/soundcloud`, {
        method: "POST",
      });
      if (!res.ok) {
        const body = await res.text();
        throw new Error(body || "Connection test failed");
      }
      setScTestMessage({ type: "success", text: "SoundCloud connection successful." });
    } catch (err) {
      setScTestMessage({ type: "error", text: err instanceof Error ? err.message : "Connection test failed." });
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
        <Link href="/settings" className="text-sm text-muted-foreground hover:underline">
          &larr; Back to settings
        </Link>
      </div>
      <h2 className="mb-6 text-2xl font-bold">Publishing Credentials</h2>

      <div className="space-y-8">
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
                  <TestTube className="size-4" />
                  {testingFtp ? "Testing..." : "Test Connection"}
                </Button>
                <Button size="sm" onClick={handleSaveFtp} disabled={savingFtp || !ftp.host}>
                  <Save className="size-4" />
                  {savingFtp ? "Saving..." : "Save"}
                </Button>
              </div>
              {ftpTestMessage && (
                <p className={`text-sm ${ftpTestMessage.type === "success" ? "text-green-600" : "text-destructive"}`}>
                  {ftpTestMessage.text}
                </p>
              )}
              {ftpMessage && (
                <p className={`text-sm ${ftpMessage.type === "success" ? "text-green-600" : "text-destructive"}`}>
                  {ftpMessage.text}
                </p>
              )}
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
                  <TestTube className="size-4" />
                  {testingSc ? "Testing..." : "Test Connection"}
                </Button>
                <Button size="sm" onClick={handleSaveSoundCloud} disabled={savingSc || !soundCloud.clientId}>
                  <Save className="size-4" />
                  {savingSc ? "Saving..." : "Save"}
                </Button>
              </div>
              {scTestMessage && (
                <p className={`text-sm ${scTestMessage.type === "success" ? "text-green-600" : "text-destructive"}`}>
                  {scTestMessage.text}
                </p>
              )}
              {scMessage && (
                <p className={`text-sm ${scMessage.type === "success" ? "text-green-600" : "text-destructive"}`}>
                  {scMessage.text}
                </p>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
