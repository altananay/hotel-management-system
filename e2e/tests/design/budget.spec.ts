import fs from "node:fs";
import path from "node:path";
import { gzipSync } from "node:zlib";
import { expect, test } from "@playwright/test";
import { WEB_BASE_URL } from "../../helpers/config";
import { writeEvidence } from "../../helpers/evidence";

const REPO_ROOT = path.resolve(__dirname, "../../..");
const JS_BUDGET = 256_000;
const FONT_BUDGET = 204_800;
const VIDEO_BUDGET = 8_388_608;
const IMAGE_BUDGET = 307_200;

interface LayoutShiftEntry extends PerformanceEntry {
  value: number;
  hadRecentInput: boolean;
}

declare global {
  interface Window {
    __cls: number;
  }
}

test.describe("Design — performance budget (AC-5, B-11, B-15)", () => {
  test(
    "shouldKeepTheInitialHomeJavaScriptWithin250KBGzip9",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      const scripts = new Map<string, Buffer>();
      page.on("response", async (response) => {
        if (response.request().resourceType() !== "script") {
          return;
        }
        try {
          const body = await response.body();
          scripts.set(response.url(), body);
        } catch {
          // Redirects and service-worker responses have no readable body.
        }
      });

      await page.setViewportSize({ width: 1280, height: 800 });
      await page.goto(`${WEB_BASE_URL}/tr`, { waitUntil: "networkidle" });
      await page.waitForFunction(() => window.__hotelMotionLoaded?.gsap === true);
      await page.waitForLoadState("networkidle");
      await page.waitForTimeout(500);

      const rows = [...scripts.entries()]
        .map(([url, body]) => ({
          url,
          gzip: gzipSync(body, { level: 9 }).length,
        }))
        .sort((a, b) => b.gzip - a.gzip);
      const total = rows.reduce((sum, row) => sum + row.gzip, 0);

      const assetRows = [
        ["font total", path.join(REPO_ROOT, "web", "public", "fonts"), FONT_BUDGET, true],
        ["hero WebM", path.join(REPO_ROOT, "web", "public", "media", "hero", "home-hero.webm"), VIDEO_BUDGET, false],
        ["hero MP4", path.join(REPO_ROOT, "web", "public", "media", "hero", "home-hero.mp4"), VIDEO_BUDGET, false],
        ["hero poster", path.join(REPO_ROOT, "web", "public", "media", "hero", "home-hero-poster.webp"), IMAGE_BUDGET, false],
        ["OG image", path.join(REPO_ROOT, "web", "public", "media", "og", "home-og.jpg"), IMAGE_BUDGET, false],
      ] as const;

      const log: string[] = [
        "# Asset and bundle budget",
        "",
        `JS gzip-9 total: ${total.toLocaleString()} B / limit ${JS_BUDGET.toLocaleString()} B`,
        "",
        "| Chunk | gzip-9 |",
        "|---|---:|",
        ...rows.map((row) => `| ${row.url} | ${row.gzip.toLocaleString()} B |`),
        "",
        "| Asset | Bytes | Limit |",
        "|---|---:|---:|",
      ];

      for (const [label, target, limit, isDirectory] of assetRows) {
        const size = isDirectory
          ? fs
              .readdirSync(target)
              .filter((file) => file.endsWith(".woff2"))
              .reduce((sum, file) => sum + fs.statSync(path.join(target, file)).size, 0)
          : fs.statSync(target).size;
        log.push(`| ${label} | ${size.toLocaleString()} B | ${limit.toLocaleString()} B |`);
        expect(size, label).toBeLessThanOrEqual(limit);
      }

      writeEvidence("0003", "AC-5-asset-and-bundle-budget.log", log.join("\n"));
      expect(total, log.join("\n")).toBeLessThanOrEqual(JS_BUDGET);
    },
  );

  test(
    "shouldKeepScrollingLayoutShiftBelowTenth",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      await page.addInitScript(() => {
        window.__cls = 0;
        new PerformanceObserver((list) => {
          for (const entry of list.getEntries() as LayoutShiftEntry[]) {
            if (!entry.hadRecentInput) {
              window.__cls += entry.value;
            }
          }
        }).observe({ type: "layout-shift", buffered: true });
      });
      await page.setViewportSize({ width: 1280, height: 800 });
      await page.goto(`${WEB_BASE_URL}/tr`, { waitUntil: "networkidle" });
      for (let index = 0; index < 12; index += 1) {
        await page.mouse.wheel(0, 700);
        await page.waitForTimeout(350);
      }
      const cls = await page.evaluate(() => window.__cls);
      writeEvidence(
        "0003",
        "AC-5-scroll-cls.log",
        `Scroll layout-shift total: ${cls.toFixed(4)} / limit 0.1`,
      );
      expect(cls).toBeLessThanOrEqual(0.1);
    },
  );
});
