import { expect, test } from "@playwright/test";
import { WEB_BASE_URL } from "../../helpers/config";
import { collectConsole } from "../../helpers/console";
import { capture } from "../../helpers/evidence";
import { mockRooms } from "../../helpers/rooms";
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
    "shouldOnlyRequestTheResponsivePosterOnTheHomeRoute",
    { tag: ["@design", "@seeded"] },
    async ({ browser }) => {
      for (const viewport of [DESKTOP, MOBILE]) {
        const context = await browser.newContext({ viewport });
        const page = await context.newPage();
        const homeRequests: string[] = [];
        page.on("request", (request) => {
          if (request.url().includes("/media/hero/home-hero-poster")) {
            homeRequests.push(request.url());
          }
        });
        await page.goto(`${WEB_BASE_URL}/tr`, { waitUntil: "networkidle" });
        await page.waitForTimeout(500);
        expect(homeRequests).toHaveLength(1);
        const currentSrc = await page
          .getByTestId("home-hero-poster")
          .evaluate((image) => (image as HTMLImageElement).currentSrc);
        expect(currentSrc).toContain(
          viewport.width < 768 ? "home-hero-poster-960.webp" : "home-hero-poster-1280.webp",
        );

        for (const route of ["/tr/rooms", "/tr/credits"]) {
          const other = await context.newPage();
          if (route.endsWith("/rooms")) {
            await mockRooms(other);
          }
          const otherRequests: string[] = [];
          other.on("request", (request) => {
            if (request.url().includes("/media/hero/home-hero-poster")) {
              otherRequests.push(request.url());
            }
          });
          await other.goto(`${WEB_BASE_URL}${route}`, { waitUntil: "networkidle" });
          await other.waitForTimeout(1000);
          const imagePreloads = await other.evaluate(
            () => document.querySelectorAll('link[rel="preload"][as="image"]').length,
          );
          expect(imagePreloads, `image preloads on ${route}`).toBe(0);
          expect(otherRequests, `poster requests on ${route}`).toEqual([]);
          await other.close();
        }
        await context.close();
      }
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
    "shouldReactToSaveDataChangesAtRuntime",
    { tag: ["@design", "@seeded"] },
    async ({ browser }) => {
      const context = await browser.newContext({ viewport: DESKTOP });
      await context.addInitScript(() => {
        const target = new EventTarget();
        let saveData = false;
        const connection = {
          get saveData() {
            return saveData;
          },
          addEventListener: target.addEventListener.bind(target),
          removeEventListener: target.removeEventListener.bind(target),
        };
        Object.defineProperty(navigator, "connection", {
          configurable: true,
          value: connection,
        });
        (window as Window & { __setSaveData?: (value: boolean) => void }).__setSaveData = (
          value,
        ) => {
          saveData = value;
          target.dispatchEvent(new Event("change"));
        };
      });
      const page = await context.newPage();
      await openHome(page);
      await expect(page.getByTestId("home-hero-video")).toHaveCount(1);

      await page.evaluate(() =>
        (window as Window & { __setSaveData?: (value: boolean) => void }).__setSaveData?.(true),
      );
      await expect(page.getByTestId("home-hero-video")).toHaveCount(0);
      await expect(page.getByTestId("home-hero-poster")).toBeVisible();

      await page.evaluate(() =>
        (window as Window & { __setSaveData?: (value: boolean) => void }).__setSaveData?.(false),
      );
      await expect(page.getByTestId("home-hero-video")).toHaveCount(1);
      await context.close();
    },
  );

  test(
    "shouldKeepThePosterAndNoConsoleErrorsWhenTheVideoPreflightReturns404",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      const console = collectConsole(page);
      // A real 404 makes Chromium log a resource error that application code cannot suppress.
      // The fetch shim exercises the same `response.ok === false` branch without network noise.
      await page.addInitScript(() => {
        const originalFetch = window.fetch.bind(window);
        window.fetch = (input, init) => {
          const url =
            typeof input === "string"
              ? input
              : input instanceof URL
                ? input.href
                : input.url;
          if (url.endsWith("/media/hero/home-hero.mp4") && init?.method === "HEAD") {
            return Promise.resolve(new Response(null, { status: 404 }));
          }
          return originalFetch(input, init);
        };
      });
      await openHome(page);
      await page.waitForTimeout(1200);

      await expect(page.getByTestId("home-hero-poster")).toBeVisible();
      await expect(page.getByTestId("home-hero-video")).toHaveCount(0);
      expect(console.errors).toEqual([]);
      await capture(page, "0003", "AC-2-home-video-fallback.png", { fullPage: false });
    },
  );

  test(
    "shouldKeepThePosterAndNoConsoleErrorsWhenAutoplayIsRejected",
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
      expect(console.errors).toEqual([]);
    },
  );
});
