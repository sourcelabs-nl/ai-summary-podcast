import type { Metadata } from "next";
import { UserProvider } from "@/lib/user-context";
import { Header } from "@/components/header";
import "./globals.css";

export const metadata: Metadata = {
  title: "AI Summary Podcast",
  description: "Dashboard for AI Summary Podcast",
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
          <Header />
          <main className="container mx-auto px-4 py-6">{children}</main>
        </UserProvider>
      </body>
    </html>
  );
}
