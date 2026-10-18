import type { Metadata } from "next";
import { notFound } from "next/navigation";
import type { ReactNode } from "react";
import "../globals.css";
import { isValidLocale } from "@/lib/i18n";
import { QueryProvider } from "@/lib/query-client";

export const metadata: Metadata = {
  title: "Kapadokya Taş Konak Otel",
  description: "Nevşehir / Kapadokya Otel Rezervasyon Sistemi",
};

interface LocaleLayoutProps {
  children: ReactNode;
  params: Promise<{ locale: string }>;
}

export default async function LocaleLayout({ children, params }: LocaleLayoutProps) {
  const { locale } = await params;
  if (!isValidLocale(locale)) {
    notFound();
  }

  return (
    <html lang={locale}>
      <body className="min-h-screen bg-stone-100 text-stone-900 antialiased">
        <QueryProvider>{children}</QueryProvider>
      </body>
    </html>
  );
}
