// @vitest-environment node
import { describe, expect, it } from "vitest";
import en from "@/lib/i18n/en.json";
import tr from "@/lib/i18n/tr.json";
import {
  absoluteSiteUrl,
  buildPageMetadata,
  countCodePoints,
  isValidDescription,
  languageAlternates,
  routePath,
  type SiteRoute,
} from "@/lib/seo";

const ROUTES: SiteRoute[] = ["home", "rooms", "credits"];
const LOCALES = ["tr", "en"] as const;

interface OpenGraphLike {
  images?: Array<{ url: string; alt?: string }>;
}

describe("SEO metadata (AC-2, AC-3, B-15)", () => {
  it("shouldAcceptDescriptionAt70And160AndReject69And161CodePoints", () => {
    expect(countCodePoints(` ${"a".repeat(70)} `)).toBe(70);
    expect(isValidDescription("a".repeat(69))).toBe(false);
    expect(isValidDescription("a".repeat(70))).toBe(true);
    expect(isValidDescription("a".repeat(160))).toBe(true);
    expect(isValidDescription("a".repeat(161))).toBe(false);
  });

  it("shouldKeepTheApprovedDescriptionLengths", () => {
    expect(countCodePoints(tr.meta.home.description)).toBe(136);
    expect(countCodePoints(en.meta.home.description)).toBe(139);
    expect(countCodePoints(tr.meta.rooms.description)).toBe(125);
    expect(countCodePoints(en.meta.rooms.description)).toBe(124);
    expect(countCodePoints(tr.meta.credits.description)).toBe(107);
    expect(countCodePoints(en.meta.credits.description)).toBe(107);
  });

  it("shouldBuildExactLocaleMetadataForEveryRoute", () => {
    for (const locale of LOCALES) {
      const dict = locale === "tr" ? tr : en;
      for (const route of ROUTES) {
        const metadata = buildPageMetadata({ locale, route, dict });
        expect(metadata.title).toBe(dict.meta[route].title);
        expect(metadata.description).toBe(dict.meta[route].description);
        expect(metadata.alternates?.canonical).toBe(routePath(locale, route));
        expect(metadata.alternates?.languages).toEqual(languageAlternates(route));

        const images = (metadata.openGraph as OpenGraphLike | undefined)?.images;
        expect(images?.[0]).toMatchObject({
          url: "/media/og/home-og.jpg",
          alt: dict.meta[route].ogAlt,
        });
        expect(metadata.twitter).toMatchObject({ card: "summary_large_image" });
      }
    }
  });

  it("shouldPointXDefaultToTheTurkishRoute", () => {
    for (const route of ROUTES) {
      expect(languageAlternates(route)["x-default"]).toBe(routePath("tr", route));
    }
  });

  it("shouldBuildAbsoluteUrlsFromTheConfiguredSiteUrl", () => {
    expect(absoluteSiteUrl("/tr/rooms")).toMatch(/^https?:\/\/.+\/tr\/rooms$/);
  });
});
