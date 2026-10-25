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
    "shouldKeepThePosterAndOnlyTheExpectedResourceErrorWhenTheVideoPreflightReturns404",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      const console = collectConsole(page);
      let preflight = { method: "", status: 0 };
      page.on("response", (response) => {
        if (response.url().endsWith("/media/hero/home-hero.mp4")) {
          preflight = {
            method: response.request().method(),
            status: response.status(),
          };
        }
      });
      await page.route("**/media/hero/home-hero.mp4", (route) =>
        route.fulfill({ status: 404, contentType: "video/mp4", body: "" }),
      );
      await openHome(page);
      await page.waitForTimeout(1200);

      await expect(page.getByTestId("home-hero-poster")).toBeVisible();
      await expect(page.getByTestId("home-hero-video")).toHaveCount(0);
      expect(preflight).toEqual({ method: "HEAD", status: 404 });
      expect(console.errors).toHaveLength(1);
      expect(console.errors[0]).toMatch(
        /^Failed to load resource: the server responded with a status of 404 \(Not Found\)$/,
      );
      await capture(page, "0003", "AC-2-home-video-fallback.png", { fullPage: false });
    },
  );

  test(
    "shouldRemoveTheVideoAndKeepThePosterWhenTheVideoSourcesReturn404",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      const console = collectConsole(page);
      const failedSources: string[] = [];
      let preflightStatus = 0;
      page.on("response", (response) => {
        if (response.url().endsWith("/media/hero/home-hero.mp4")) {
          if (response.request().method() === "HEAD") {
            preflightStatus = response.status();
          } else if (response.status() === 404) {
            failedSources.push(response.url());
          }
        }
        if (response.url().endsWith("/media/hero/home-hero.webm") && response.status() === 404) {
          failedSources.push(response.url());
        }
      });
      await page.addInitScript(() => {
        const mediaErrors: string[] = [];
        document.addEventListener(
          "error",
          (event) => {
            const target = event.target;
            if (target instanceof HTMLVideoElement) {
              mediaErrors.push("video");
            } else if (target instanceof HTMLSourceElement) {
              mediaErrors.push("source");
            }
          },
          true,
        );
        (window as Window & { __mediaErrors?: string[] }).__mediaErrors = mediaErrors;
      });
      await page.route("**/media/hero/home-hero.mp4", (route) => {
        if (route.request().method() === "HEAD") {
          return route.fulfill({ status: 200, contentType: "video/mp4", body: "" });
        }
        return route.fulfill({ status: 404, contentType: "video/mp4", body: "" });
      });
      await page.route("**/media/hero/home-hero.webm", (route) =>
        route.fulfill({ status: 404, contentType: "video/webm", body: "" }),
      );
      await openHome(page);
      await page.waitForTimeout(1500);

      await expect(page.getByTestId("home-hero-poster")).toBeVisible();
      await expect(page.getByTestId("home-hero-video")).toHaveCount(0);
      expect(preflightStatus).toBe(200);
      expect(failedSources).toHaveLength(2);
      const mediaErrors = await page.evaluate(
        () => (window as Window & { __mediaErrors?: string[] }).__mediaErrors ?? [],
      );
      expect(mediaErrors).toContain("source");
      expect(console.errors).toHaveLength(2);
      expect(
        console.errors.every((message) =>
          /^Failed to load resource: the server responded with a status of 404 \(Not Found\)$/.test(
            message,
          ),
        ),
      ).toBe(true);
      await capture(page, "0003", "AC-2-home-video-source-fallback.png", {
        fullPage: false,
      });
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
