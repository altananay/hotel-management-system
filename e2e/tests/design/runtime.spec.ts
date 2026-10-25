import { expect, test } from "@playwright/test";
import { WEB_BASE_URL } from "../../helpers/config";
import { writeEvidence } from "../../helpers/evidence";
import { MOBILE } from "../../helpers/viewports";

interface MotionFlags {
  motion: boolean;
  gsap: boolean;
  lenis: boolean;
}

declare global {
  interface Window {
    __hotelMotionLoaded?: MotionFlags;
  }
}

test.describe("Design — runtime and motion loading (AC-5, B-11, B-12)", () => {
  test(
    "shouldMakeNoThirdPartyRuntimeRequestsOnHome",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      const requests: string[] = [];
      page.on("request", (request) => {
        const url = request.url();
        if (url.startsWith("http")) {
          requests.push(url);
        }
      });
      await page.goto(`${WEB_BASE_URL}/tr`, { waitUntil: "networkidle" });
      await page.waitForTimeout(1500);

      const allowed = new Set([
        new URL(WEB_BASE_URL).origin,
        "http://localhost:8081",
      ]);
      const thirdParty = requests.filter((url) => !allowed.has(new URL(url).origin));
      const log = [
        "# Runtime request log",
        "",
        `Allowed origins: ${[...allowed].join(", ")}`,
        `Total requests: ${requests.length}`,
        `Third-party requests: ${thirdParty.length}`,
        "",
        ...thirdParty,
      ].join("\n");
      writeEvidence("0003", "AC-5-runtime-requests.log", log);
      writeEvidence("0003", "AC-1-runtime-requests.log", log);
      expect(thirdParty, thirdParty.join("\n")).toEqual([]);
    },
  );

  test(
    "shouldLoadGsapAndLenisOnlyOnDesktopAndNeverUnderReducedMotion",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      await page.setViewportSize(MOBILE);
      await page.goto(`${WEB_BASE_URL}/tr`, { waitUntil: "networkidle" });
      await page.waitForFunction(() => window.__hotelMotionLoaded?.motion === true);
      let flags = await page.evaluate(() => window.__hotelMotionLoaded);
      expect(flags).toMatchObject({ motion: true, gsap: false, lenis: false });

      await page.setViewportSize({ width: 1280, height: 800 });
      await page.goto(`${WEB_BASE_URL}/tr`, { waitUntil: "networkidle" });
      await page.waitForFunction(() => window.__hotelMotionLoaded?.gsap === true);
      flags = await page.evaluate(() => window.__hotelMotionLoaded);
      expect(flags).toEqual({ motion: true, gsap: true, lenis: true });

      await page.emulateMedia({ reducedMotion: "reduce" });
      await page.goto(`${WEB_BASE_URL}/tr`, { waitUntil: "networkidle" });
      await page.waitForTimeout(800);
      flags = await page.evaluate(() => window.__hotelMotionLoaded);
      expect(flags?.gsap ?? false).toBe(false);
      expect(flags?.lenis ?? false).toBe(false);
    },
  );
});
