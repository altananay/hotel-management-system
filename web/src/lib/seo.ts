import type { Metadata } from "next";
import { appConfig } from "./config";
import type { Dictionary, Locale } from "./i18n";

export type SiteRoute = "home" | "rooms" | "credits";

const ROUTE_PATHS: Record<SiteRoute, string> = {
  home: "",
  rooms: "/rooms",
  credits: "/credits",
};

const OG_IMAGE = "/media/og/home-og.jpg";

export function countCodePoints(value: string): number {
  return [...value.trim()].length;
}

export function isValidDescription(value: string, min = 70, max = 160): boolean {
  const length = countCodePoints(value);
  return length >= min && length <= max;
}

export function assertValidDescription(value: string): void {
  const length = countCodePoints(value);
  if (length < 70 || length > 160) {
    throw new Error(`Metadata description must be 70-160 code points, got ${length}`);
  }
}

export function routePath(locale: Locale, route: SiteRoute): string {
  return `/${locale}${ROUTE_PATHS[route]}`;
}

export function languageAlternates(route: SiteRoute): Record<string, string> {
  return {
    tr: routePath("tr", route),
    en: routePath("en", route),
    "x-default": routePath("tr", route),
  };
}

export function buildPageMetadata({
  locale,
  route,
  dict,
}: {
  locale: Locale;
  route: SiteRoute;
  dict: Dictionary;
}): Metadata {
  const meta = dict.meta[route];
  assertValidDescription(meta.description);

  return {
    title: meta.title,
    description: meta.description,
    alternates: {
      canonical: routePath(locale, route),
      languages: languageAlternates(route),
    },
    openGraph: {
      type: "website",
      siteName: dict.siteTitle,
      locale: locale === "tr" ? "tr_TR" : "en_US",
      title: meta.title,
      description: meta.description,
      url: routePath(locale, route),
      images: [
        {
          url: OG_IMAGE,
          width: 1200,
          height: 630,
          alt: meta.ogAlt,
        },
      ],
    },
    twitter: {
      card: "summary_large_image",
      title: meta.title,
      description: meta.description,
      images: [OG_IMAGE],
    },
  };
}

export function absoluteSiteUrl(path: string): string {
  return `${appConfig.siteUrl}${path.startsWith("/") ? path : `/${path}`}`;
}
