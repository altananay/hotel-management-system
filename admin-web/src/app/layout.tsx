import type { Metadata } from "next";
import type { ReactNode } from "react";
import "./globals.css";
import { AdminQueryProvider } from "@/lib/query-client";

export const metadata: Metadata = {
  title: "Otel Yönetim Paneli",
  description: "Nevşehir / Kapadokya Otel Operasyon ve Yönetim Paneli",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="tr">
      <body className="min-h-screen bg-slate-100 text-slate-900 antialiased">
        <AdminQueryProvider>{children}</AdminQueryProvider>
      </body>
    </html>
  );
}
