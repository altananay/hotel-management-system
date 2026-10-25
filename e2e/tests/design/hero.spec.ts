import { expect, test } from "@playwright/test";
import { WEB_BASE_URL } from "../../helpers/config";
import { collectConsole } from "../../helpers/console";
import { capture } from "../../helpers/evidence";
import { DESKTOP, MOBILE } from "../../helpers/viewports";

async function openHome(page: import("@playwright/test").Page) {
  await page.goto(`${WEB_BASE_URL}/tr`, { waitUntil: "networkidle" });
}

test.describe("Design — hero media (AC-2, AC-4, B-05, B-13)", () => {
  test(
    "shouldMountMutedLoopingInlineVideoWithBothSourcesAtDesktop",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      await openHome(page);
      await expect(page.getByTestId("home-hero-poster")).toBeVisible();
      const video = page.getByTestId("home-hero-video");
      await expect(video).toHaveCount(1);
      await expect(video).toHaveCSS("opacity", "1");

      const properties = await video.evaluate((element) => {
        const media = element as HTMLVideoElement;
        return {
          muted: media.muted,
          loop: media.loop,
          playsInline: media.playsInline,
          preload: media.preload,
          ariaHidden: media.getAttribute("aria-hidden"),
          sources: Array.from(media.querySelectorAll("source")).map((source) =>
            source.getAttribute("src"),
          ),
        };
      });

      expect(properties).toMatchObject({
        muted: true,
        loop: true,
        playsInline: true,
        preload: "none",
        ariaHidden: "true",
      });
      expect(properties.sources).toEqual([
        "/media/hero/home-hero.webm",
        "/media/hero/home-hero.mp4",
      ]);
    },
  );

  test(
    "shouldShowPosterOnlyAt390And767AndMountVideoAt768",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      for (const width of [390, 767]) {
        await page.setViewportSize({ width, height: 844 });
        await openHome(page);
        await page.waitForTimeout(1200);
        await expect(page.getByTestId("home-hero-poster")).toBeVisible();
        await expect(page.getByTestId("home-hero-video")).toHaveCount(0);
      }

      await page.setViewportSize({ width: 768, height: 1024 });
      await openHome(page);
      await expect(page.getByTestId("home-hero-video")).toHaveCount(1);
      await expect(page.getByTestId("home-hero-video")).toHaveCSS("opacity", "1");
    },
  );

  test(
    "shouldNotMountVideoUnderReducedMotion",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      await page.emulateMedia({ reducedMotion: "reduce" });
      await openHome(page);
      await page.waitForTimeout(1000);
      await expect(page.getByTestId("home-hero-poster")).toBeVisible();
      await expect(page.getByTestId("home-hero-video")).toHaveCount(0);
    },
  );

  test(
    "shouldNotMountVideoWhenSaveDataIsEnabledAt1280And390",
    { tag: ["@design", "@seeded"] },
    async ({ browser }) => {
      for (const viewport of [DESKTOP, MOBILE]) {
        const context = await browser.newContext({ viewport });
        await context.addInitScript(() => {
          Object.defineProperty(navigator, "connection", {
            configurable: true,
            value: { saveData: true },
          });
        });
        const page = await context.newPage();
        await openHome(page);
        await page.waitForTimeout(1200);
        await expect(page.getByTestId("home-hero-poster")).toBeVisible();
        await expect(page.getByTestId("home-hero-video")).toHaveCount(0);
        await context.close();
      }
    },
  );

  test(
    "shouldKeepThePosterAndNoApplicationConsoleErrorWhenVideoReturns404",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      const console = collectConsole(page);
      await page.route("**/media/hero/home-hero.mp4", (route) =>
        route.fulfill({ status: 404, body: "" }),
      );
      await openHome(page);
      await page.waitForTimeout(1200);

      await expect(page.getByTestId("home-hero-poster")).toBeVisible();
      await expect(page.getByTestId("home-hero-video")).toHaveCount(0);
      expect(console.appErrors()).toEqual([]);
      await capture(page, "0003", "AC-2-home-video-fallback.png", { fullPage: false });
    },
  );

  test(
    "shouldKeepThePosterAndNoApplicationConsoleErrorWhenAutoplayIsRejected",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      const console = collectConsole(page);
      await page.addInitScript(() => {
        HTMLMediaElement.prototype.play = () =>
          Promise.reject(new DOMException("autoplay blocked", "NotAllowedError"));
      });
      await openHome(page);
      await expect(page.getByTestId("home-hero-poster")).toBeVisible();
      await expect(page.getByTestId("home-hero-video")).toHaveCount(0);
      expect(console.appErrors()).toEqual([]);
    },
  );
});
