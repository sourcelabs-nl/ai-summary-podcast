"use client";

import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from "react";
import type { User } from "./types";

interface UserContextType {
  users: User[];
  selectedUser: User | null;
  setSelectedUser: (user: User) => void;
  refreshSelectedUser: (updated: User) => void;
  loading: boolean;
}

const UserContext = createContext<UserContextType>({
  users: [],
  selectedUser: null,
  setSelectedUser: () => {},
  refreshSelectedUser: () => {},
  loading: true,
});

export function UserProvider({ children }: { children: ReactNode }) {
  const [users, setUsers] = useState<User[]>([]);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch("/api/users")
      .then((res) => res.json())
      .then((data: User[]) => {
        setUsers(data);
        if (data.length > 0) {
          setSelectedUser(data[0]);
        }
      })
      .catch(() => setUsers([]))
      .finally(() => setLoading(false));
  }, []);

  const refreshSelectedUser = useCallback((updated: User) => {
    setUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
    setSelectedUser((prev) => (prev?.id === updated.id ? updated : prev));
  }, []);

  return (
    <UserContext.Provider value={{ users, selectedUser, setSelectedUser, refreshSelectedUser, loading }}>
      {children}
    </UserContext.Provider>
  );
}

export function useUser() {
  return useContext(UserContext);
}
