"use client";

import Link from "next/link";
import { Podcast, Settings } from "lucide-react";
import { useUser } from "@/lib/user-context";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

export function Header() {
  const { users, selectedUser, setSelectedUser, loading } = useUser();

  return (
    <header className="bg-primary text-primary-foreground">
      <div className="container mx-auto flex h-14 items-center justify-between px-4">
        <Link href="/podcasts" className="flex items-center gap-2 text-lg font-semibold hover:opacity-80">
          <Podcast className="size-5" />
          AI Podcast Studio
        </Link>
        <div className="ml-auto flex items-center gap-2">
          <div className="w-48">
            {loading ? (
              <span className="text-sm opacity-80">Loading...</span>
            ) : users.length === 0 ? (
              <span className="text-sm opacity-80">No users available</span>
            ) : (
              <Select
                value={selectedUser?.id ?? ""}
                onValueChange={(id) => {
                  const user = users.find((u) => u.id === id);
                  if (user) setSelectedUser(user);
                }}
              >
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="Select user" />
                </SelectTrigger>
                <SelectContent position="popper" align="end">
                  {users.map((user) => (
                    <SelectItem key={user.id} value={user.id}>
                      {user.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          </div>
          {selectedUser && (
            <Link href="/settings">
              <Button variant="ghost" size="sm" title="User settings" className="h-9 rounded-md border border-input text-primary-foreground hover:text-primary-foreground/80 hover:bg-primary-foreground/10">
                <Settings className="size-4" />
              </Button>
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}
