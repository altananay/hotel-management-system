import type { Metadata } from "next";
import { notFound } from "next/navigation";
import ReactDOM from "react-dom";
import type { ReactNode } from "react";
import "../globals.css";
import { SiteFooter } from "@/components/layout/site-footer";
import { SiteHeader } from "@/components/layout/site-header";
import { SkipLink } from "@/components/layout/skip-link";
import { appConfig } from "@/lib/config";
import {
  getDictionary,
  isValidLocale,
  SUPPORTED_LOCALES,
  type Locale,
} from "@/lib/i18n";
import { QueryProvider } from "@/lib/query-client";

interface LocaleLayoutProps {
  children: ReactNode;
  params: Promise<{ locale: string }>;
}

export function generateStaticParams() {
  return SUPPORTED_LOCALES.map((locale) => ({ locale }));
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ locale: string }>;
}): Promise<Metadata> {
  const { locale } = await params;
  if (!isValidLocale(locale)) {
    return {};
  }

  return {
    metadataBase: new URL(appConfig.siteUrl),
    applicationName:
      locale === "tr" ? "Kapadokya Taş Konak Otel" : "Cappadocia Stone Mansion Hotel",
  };
}

export default async function LocaleLayout({
  children,
  params,
}: LocaleLayoutProps) {
  const { locale: rawLocale } = await params;
  if (!isValidLocale(rawLocale)) {
    notFound();
  }
  const locale = rawLocale as Locale;
  const dict = getDictionary(locale);

  ReactDOM.preload("/fonts/source-sans-3-400.woff2", {
    as: "font",
    type: "font/woff2",
    crossOrigin: "anonymous",
  });
  ReactDOM.preload("/fonts/source-sans-3-600.woff2", {
    as: "font",
    type: "font/woff2",
    crossOrigin: "anonymous",
  });
  ReactDOM.preload("/fonts/cormorant-garamond-600.woff2", {
    as: "font",
    type: "font/woff2",
    crossOrigin: "anonymous",
  });

  return (
    <html lang={locale}>
      <body className="paper-grain min-h-screen bg-surface text-fg antialiased">
        <SkipLink label={dict.skipToContent} />
        <SiteHeader locale={locale} dict={dict} />
        <QueryProvider>
          <main
            id="main-content"
            tabIndex={-1}
            data-testid="site-main"
            className="outline-none"
          >
            {children}
          </main>
        </QueryProvider>
        <SiteFooter locale={locale} dict={dict} />
      </body>
    </html>
  );
}
