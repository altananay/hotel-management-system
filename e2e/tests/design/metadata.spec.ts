import { expect, test } from "@playwright/test";
import { WEB_BASE_URL } from "../../helpers/config";
import { loadDictionary } from "../../helpers/copy";
import { writeEvidence } from "../../helpers/evidence";
import { SIX_ROUTES } from "../../helpers/viewports";

type RouteKey = "home" | "rooms" | "credits";

function routeKey(path: string): RouteKey {
  if (path.endsWith("/rooms")) {
    return "rooms";
  }
  if (path.endsWith("/credits")) {
    return "credits";
  }
  return "home";
}

test.describe("Design — locale metadata (AC-2, AC-3, B-06, B-15)", () => {
  test(
    "shouldExposeExactLocaleMetadataCanonicalHreflangAndOpenGraph",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      const rows: string[] = [
        "# Metadata kanıtı",
        "",
        "| Rota | Title | Description (code point) | Canonical |",
        "|---|---|---:|---|",
      ];

      for (const path of SIX_ROUTES) {
        const locale = path.startsWith("/en") ? "en" : "tr";
        const dict = loadDictionary(locale);
        const key = routeKey(path);
        const expected = dict.meta[key];
        await page.goto(`${WEB_BASE_URL}${path}`, { waitUntil: "networkidle" });

        const metadata = await page.evaluate(() => ({
          title: document.title,
          description:
            document.querySelector('meta[name="description"]')?.getAttribute("content") ?? "",
          canonical: document.querySelector('link[rel="canonical"]')?.getAttribute("href") ?? "",
          hreflang: Array.from(
            document.querySelectorAll<HTMLLinkElement>('link[rel="alternate"][hreflang]'),
          ).map((link) => [link.hreflang, link.href] as const),
          ogImage:
            document.querySelector('meta[property="og:image"]')?.getAttribute("content") ?? "",
          ogAlt:
            document.querySelector('meta[property="og:image:alt"]')?.getAttribute("content") ?? "",
          twitter:
            document.querySelector('meta[name="twitter:card"]')?.getAttribute("content") ?? "",
        }));

        expect(metadata.title).toBe(expected.title);
        expect(metadata.description).toBe(expected.description);
        const length = [...metadata.description.trim()].length;
        expect(length).toBeGreaterThanOrEqual(70);
        expect(length).toBeLessThanOrEqual(160);
        expect(metadata.canonical).toMatch(new RegExp(`${path}$`));
        expect(metadata.hreflang).toEqual([
          ["tr", expect.stringMatching(new RegExp(`/tr${path.replace(/^\/(tr|en)/, "")}$`))],
          ["en", expect.stringMatching(new RegExp(`/en${path.replace(/^\/(tr|en)/, "")}$`))],
          ["x-default", expect.stringMatching(new RegExp(`/tr${path.replace(/^\/(tr|en)/, "")}$`))],
        ]);
        expect(metadata.ogImage).toMatch(/\/media\/og\/home-og\.jpg$/);
        expect(metadata.ogAlt).toBe(expected.ogAlt);
        expect(metadata.twitter).toBe("summary_large_image");

        rows.push(
          `| ${path} | ${metadata.title.replaceAll("|", "\\|")} | ${length} | ${metadata.canonical} |`,
        );
      }

      writeEvidence("0003", "AC-2-metadata.md", rows.join("\n"));
    },
  );
});

