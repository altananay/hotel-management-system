import { expect, test } from "@playwright/test";
import { WEB_BASE_URL } from "../../helpers/config";
import { collectConsole } from "../../helpers/console";
import { capture, writeEvidence } from "../../helpers/evidence";
import { mockRooms } from "../../helpers/rooms";
import { MOBILE } from "../../helpers/viewports";

test.describe("Design — room showcase (AC-3, AC-6, B-13, B-16)", () => {
  test(
    "shouldPreserveThe0002TestIdAndTextContractWithSharedChrome",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      await mockRooms(page);
      await page.goto(`${WEB_BASE_URL}/tr/rooms`, { waitUntil: "networkidle" });

      await expect(page.getByTestId("rooms-page")).toBeVisible();
      await expect(page.getByTestId("site-header")).toBeVisible();
      await expect(page.getByTestId("site-footer")).toBeVisible();
      await expect(page.getByTestId("rooms-list")).toBeVisible();
      await expect(page.getByTestId("room-card")).toHaveCount(3);

      for (const testId of [
        "rooms-list",
        "room-card",
        "room-card-name",
        "room-card-description",
        "room-card-capacity",
        "room-card-amenities",
        "room-card-amenity",
        "room-card-placeholder",
      ]) {
        await expect(page.getByTestId(testId).first()).toBeVisible();
      }

      await expect(page.getByTestId("room-card-name").first()).toHaveText(
        "Kemerli Mağara Süit",
      );
      await expect(page.getByTestId("room-card-capacity").first()).toHaveText(
        "En fazla 2 misafir",
      );
      await expect(page.locator('[data-testid^="price"]')).toHaveCount(0);
      for (const card of await page.getByTestId("room-card").all()) {
        expect(await card.innerText()).not.toMatch(/[₺€$]/);
      }
      const cursor = await page
        .getByTestId("room-card")
        .first()
        .evaluate((element) => getComputedStyle(element).cursor);
      expect(cursor).not.toBe("pointer");

      await capture(page, "0003", "AC-3-rooms-tr-desktop.png");

      await page.getByTestId("locale-switch-en").click();
      await expect(page).toHaveURL(/\/en\/rooms$/);
      await expect(page.getByTestId("room-card-name").first()).toHaveText(
        "Arched Cave Suite",
      );
      await capture(page, "0003", "AC-3-rooms-en-desktop.png");

      await page.setViewportSize(MOBILE);
      await page.goto(`${WEB_BASE_URL}/tr/rooms`, { waitUntil: "networkidle" });
      await expect(page.getByTestId("rooms-page")).toBeVisible();
      await capture(page, "0003", "AC-3-rooms-tr-mobile.png");
    },
  );

  test(
    "shouldRenderLoadingAndEmptyStates",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      let release!: () => void;
      const gate = new Promise<void>((resolve) => {
        release = resolve;
      });
      await page.route("**/api/room-types*", async (route) => {
        await gate;
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ items: [], page: 0, size: 100, total: 0 }),
        });
      });
      await page.goto(`${WEB_BASE_URL}/tr/rooms`, { waitUntil: "domcontentloaded" });
      await expect(page.getByTestId("rooms-loading")).toBeVisible();
      await capture(page, "0003", "AC-3-rooms-loading.png");
      release();
      await expect(page.getByTestId("rooms-empty")).toBeVisible();
      await expect(page.getByTestId("rooms-empty-title")).toHaveText(
        "Henüz yayınlanmış oda bulunmuyor.",
      );
      await capture(page, "0003", "AC-3-rooms-empty.png");
    },
  );

  test(
    "shouldShowRoomsErrorAndRetryWithoutTechnicalDetailOnNetworkAbort",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      await page.route("**/api/room-types*", (route) => route.abort());
      await page.goto(`${WEB_BASE_URL}/tr/rooms`, { waitUntil: "networkidle" });
      await expect(page.getByTestId("rooms-error")).toBeVisible();
      await expect(page.getByTestId("rooms-error-message")).toHaveText(
        "Oda bilgileri şu anda yüklenemedi. Lütfen tekrar deneyin.",
      );
      await expect(page.getByTestId("rooms-retry")).toHaveText("Tekrar Dene");
      await expect(page.getByText(/PSQLException|stack trace|system\.internal_error/i)).toHaveCount(0);
      await capture(page, "0003", "AC-3-rooms-error.png");
      await capture(page, "0003", "AC-3-rooms-error-network.png");
    },
  );

  test(
    "shouldShowRoomsErrorOnTenSecondTimeout",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      await page.route("**/api/room-types*", async (route) => {
        await new Promise((resolve) => setTimeout(resolve, 12_000));
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ items: [], page: 0, size: 100, total: 0 }),
        });
      });
      await page.goto(`${WEB_BASE_URL}/tr/rooms`, { waitUntil: "domcontentloaded" });
      await expect(page.getByTestId("rooms-error")).toBeVisible({ timeout: 15_000 });
      await expect(page.getByTestId("rooms-retry")).toBeVisible();
      await capture(page, "0003", "AC-3-rooms-error-timeout.png");
    },
  );

  test(
    "shouldShowRoomsErrorOnMalformedJson",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      await page.route("**/api/room-types*", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: "{",
        }),
      );
      await page.goto(`${WEB_BASE_URL}/tr/rooms`, { waitUntil: "networkidle" });
      await expect(page.getByTestId("rooms-error")).toBeVisible();
      await expect(page.getByTestId("rooms-error-message")).toHaveText(
        "Oda bilgileri şu anda yüklenemedi. Lütfen tekrar deneyin.",
      );
      await capture(page, "0003", "AC-3-rooms-error-malformed.png");
    },
  );

  test(
    "shouldFallbackToASystemFontWithoutApplicationConsoleErrors",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      const console = collectConsole(page);
      await mockRooms(page);
      await page.route("**/fonts/**", (route) => route.abort());
      await page.goto(`${WEB_BASE_URL}/tr/rooms`, { waitUntil: "networkidle" });
      await expect(page.getByTestId("rooms-page")).toBeVisible();
      await expect(page.getByTestId("room-card-name").first()).toBeVisible();
      expect(console.appErrors()).toEqual([]);
      writeEvidence(
        "0003",
        "AC-1-font-fallback.log",
        "Font requests aborted; the page remained readable with the system fallback and produced no application console errors.",
      );
    },
  );
});
