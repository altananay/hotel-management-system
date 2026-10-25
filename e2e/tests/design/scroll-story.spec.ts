import { expect, test } from "@playwright/test";
import { WEB_BASE_URL } from "../../helpers/config";
import { capture } from "../../helpers/evidence";

async function openHome(page: import("@playwright/test").Page): Promise<void> {
  await page.goto(`${WEB_BASE_URL}/tr`, { waitUntil: "networkidle" });
}

async function scrollTo(page: import("@playwright/test").Page, id: string): Promise<void> {
  await page.evaluate((sectionId) => {
    document.getElementById(sectionId)?.scrollIntoView({ block: "start" });
  }, id);
  await page.waitForTimeout(900);
}

test.describe("Design — scroll story (AC-2, AC-4, AC-6, B-04, B-12)", () => {
  test(
    "shouldPinExactlyTwoScenesAt1280And1024",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      for (const width of [1280, 1024]) {
        await page.setViewportSize({ width, height: 800 });
        await openHome(page);
        await expect(page.locator("#home-story")).toHaveAttribute("data-motion", "pinned");
        await expect(page.locator("#home-experience")).toHaveAttribute(
          "data-motion",
          "pinned",
        );
        await expect(page.locator('[data-motion="pinned"]')).toHaveCount(2);
        await expect(page.locator("#home-hero")).toHaveAttribute("data-motion", "static");
        await expect(page.locator("#home-rooms-cta")).toHaveAttribute(
          "data-motion",
          "static",
        );
        await expect(page.locator("#home-contact")).toHaveAttribute("data-motion", "static");
      }
    },
  );

  test(
    "shouldNotPinAt390_767_768And1023",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      for (const width of [390, 767, 768, 1023]) {
        await page.setViewportSize({ width, height: width >= 768 ? 1024 : 844 });
        await openHome(page);
        await page.waitForTimeout(1200);
        await expect(page.locator('[data-motion="pinned"]')).toHaveCount(0);
        if (width >= 768) {
          await expect(page.getByTestId("home-hero-video")).toHaveCount(1);
        } else {
          await expect(page.getByTestId("home-hero-video")).toHaveCount(0);
        }
      }
    },
  );

  test(
    "shouldUseOnlyDecorativeParallaxWithinFortyPixels",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      await openHome(page);
      await scrollTo(page, "home-story");
      const offsets = await page.locator("[data-parallax]").evaluateAll((elements) =>
        elements.map((element) => {
          const transform = getComputedStyle(element).transform;
          if (transform === "none") {
            return 0;
          }
          const matrix = new DOMMatrixReadOnly(transform);
          return matrix.m42;
        }),
      );
      for (const offset of offsets) {
        expect(Math.abs(offset)).toBeLessThanOrEqual(40);
      }
    },
  );

  test(
    "shouldCaptureHeroStoryExperienceAndRoomsCtaScrollSteps",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      await openHome(page);
      await capture(page, "0003", "AC-2-home-scroll-01-hero.png", { fullPage: false });
      await scrollTo(page, "home-story");
      await capture(page, "0003", "AC-2-home-scroll-02-story.png", { fullPage: false });
      await scrollTo(page, "home-experience");
      await capture(page, "0003", "AC-2-home-scroll-03-experience.png", {
        fullPage: false,
      });
      await scrollTo(page, "home-rooms-cta");
      await capture(page, "0003", "AC-2-home-scroll-04-rooms-cta.png", {
        fullPage: false,
      });
    },
  );
});
