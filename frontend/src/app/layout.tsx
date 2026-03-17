import type { Metadata } from "next";
import { UserProvider } from "@/lib/user-context";
import { EventProvider } from "@/lib/event-context";
import { Header } from "@/components/header";
import { Toaster } from "@/components/ui/sonner";
import "./globals.css";

export const metadata: Metadata = {
  title: "AI Podcast Studio",
  description: "Dashboard for AI Podcast Studio",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="antialiased">
        <UserProvider>
          <EventProvider>
            <Header />
            <main className="container mx-auto px-4 py-6 overflow-x-hidden">{children}</main>
            <Toaster position="bottom-right" expand visibleToasts={5} />
          </EventProvider>
        </UserProvider>
      </body>
    </html>
  );
}
