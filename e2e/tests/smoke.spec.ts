import fs from "fs";
import path from "path";
import { expect, test } from "@playwright/test";

const WEB_BASE_URL = process.env.E2E_WEB_URL ?? "http://localhost:3000";
const ADMIN_WEB_BASE_URL = process.env.E2E_ADMIN_WEB_URL ?? "http://localhost:3001";
const EVIDENCE_DIR = path.resolve(__dirname, "../../specs/done/evidence/0002");

test.beforeAll(() => {
  fs.mkdirSync(EVIDENCE_DIR, { recursive: true });
});

test.describe("Smoke E2E — web (:3000) and admin-web (:3001) (AC-3, AC-5, AC-6, QA-2, QA-10, QA-15)", () => {
  test("@empty shouldRenderRealEmptyRoomShowcaseBeforeSeed_whenNavigatingFromHomeToRooms", async ({
    page,
  }) => {
    await page.goto(WEB_BASE_URL);
    await expect(page).toHaveURL(/\/tr$/);
    await expect(page.locator("html")).toHaveAttribute("lang", "tr");
    await expect(page.getByTestId("web-site-title")).toHaveText("Kapadokya Taş Konak Otel");

    const homeRoomsLink = page.getByTestId("home-rooms-link");
    await expect(homeRoomsLink).toBeVisible();
    await homeRoomsLink.click();

    await expect(page).toHaveURL(/\/tr\/rooms$/);
    await expect(page.getByTestId("rooms-page")).toBeVisible();
    await expect(page.getByTestId("rooms-empty")).toBeVisible();
    await expect(page.getByTestId("rooms-empty-title")).toHaveText(
      "Henüz yayınlanmış oda bulunmuyor.",
    );

    await page.screenshot({
      path: path.join(EVIDENCE_DIR, "AC-6-web-rooms-empty.png"),
      fullPage: true,
    });
  });

  test("@seeded shouldRenderLoadingAndErrorStatesOnWebRooms_whenInterceptingRoomTypesApi", async ({
    page,
  }) => {
    // 1. Loading state via Playwright route delay
    let releaseLoading!: () => void;
    const loadingGate = new Promise<void>((resolve) => {
      releaseLoading = resolve;
    });

    await page.route("**/api/room-types*", async (route) => {
      await loadingGate;
      await route.continue();
    });

    await page.goto(`${WEB_BASE_URL}/tr/rooms`);
    await expect(page.getByTestId("rooms-page")).toBeVisible();
    await expect(page.getByTestId("rooms-loading")).toBeVisible();

    await page.screenshot({
      path: path.join(EVIDENCE_DIR, "AC-6-web-rooms-loading.png"),
      fullPage: true,
    });

    releaseLoading();
    await expect(page.getByTestId("rooms-list")).toBeVisible();
    await page.unroute("**/api/room-types*");

    // 2. Error state via Playwright route 500 application/problem+json injection
    await page.route("**/api/room-types*", async (route) => {
      await route.fulfill({
        status: 500,
        contentType: "application/problem+json",
        headers: {
          "X-Request-Id": "0199f2c0-0000-7000-8000-000000000021",
        },
        body: JSON.stringify({
          type: "about:blank",
          title: "Internal Server Error",
          status: 500,
          detail: "Internal stack trace must never be shown to guest",
          instance: "/api/room-types",
          code: "system.internal_error",
          requestId: "0199f2c0-0000-7000-8000-000000000021",
        }),
      });
    });

    await page.reload();
    await expect(page.getByTestId("rooms-error")).toBeVisible();
    await expect(page.getByTestId("rooms-error-message")).toHaveText(
      "Oda bilgileri şu anda yüklenemedi. Lütfen tekrar deneyin.",
    );
    await expect(page.getByText("Internal stack trace")).toHaveCount(0);
    await expect(page.getByTestId("rooms-retry")).toHaveText("Tekrar Dene");

    await page.screenshot({
      path: path.join(EVIDENCE_DIR, "AC-6-web-rooms-error.png"),
      fullPage: true,
    });

    // Verify retry recovers once route is unrouted
    await page.unroute("**/api/room-types*");
    await page.getByTestId("rooms-retry").click();
    await expect(page.getByTestId("rooms-list")).toBeVisible();
  });

  test("@seeded shouldRenderSeededRoomShowcaseInTrAndEnAndSwitchLocaleWithoutPrices_whenCatalogSeeded", async ({
    page,
  }) => {
    await page.goto(`${WEB_BASE_URL}/tr`);
    await page.getByTestId("home-rooms-link").click();

    await expect(page).toHaveURL(/\/tr\/rooms$/);
    await expect(page.locator("html")).toHaveAttribute("lang", "tr");
    await expect(page.getByTestId("rooms-page")).toBeVisible();
    await expect(page.getByTestId("rooms-list")).toBeVisible();

    const trCards = page.getByTestId("room-card");
    await expect(trCards).toHaveCount(3);

    // Deterministic order (display_order = 10 tie-broken by UUIDv7 id ASC: 301 then 302, then 303; passive 304 excluded)
    await expect(page.getByTestId("room-card-name")).toHaveText([
      "Kemerli Mağara Süit",
      "Peri Bacası Manzaralı Deluxe Taş Oda",
      "Sultan Panoramik Konak Süiti",
    ]);
    await expect(page.getByText("Restorasyondaki Avlu Odası")).toHaveCount(0);

    const firstTrCard = trCards.nth(0);
    await expect(firstTrCard.getByTestId("room-card-description")).toContainText(
      "Tarihi tüf kayalara oyulmuş",
    );
    await expect(firstTrCard.getByTestId("room-card-capacity")).toHaveText(
      "En fazla 2 misafir",
    );
    await expect(firstTrCard.getByTestId("room-card-placeholder")).toBeVisible();
    await expect(firstTrCard.getByTestId("room-card-amenities")).toBeVisible();
    await expect(firstTrCard.getByTestId("room-card-amenity")).toHaveText([
      "Özel Taş Oyma Jakuzi",
      "Özel Seçki Kahve ve Çay Köşesi",
      "Yüksek Hızlı Kablosuz İnternet",
    ]);

    // No price testid and no currency symbols in any card (QA-10)
    await expect(page.locator('[data-testid^="price"]')).toHaveCount(0);
    for (let i = 0; i < 3; i++) {
      const cardText = await trCards.nth(i).innerText();
      expect(cardText).not.toMatch(/[₺€$]/);
    }

    await page.screenshot({
      path: path.join(EVIDENCE_DIR, "AC-6-web-rooms-tr.png"),
      fullPage: true,
    });

    // Switch locale from /tr/rooms to /en/rooms via locale-switch-en (QA-15)
    await page.getByTestId("locale-switch-en").click();
    await expect(page).toHaveURL(/\/en\/rooms$/);
    await expect(page.locator("html")).toHaveAttribute("lang", "en");
    await expect(page.getByTestId("rooms-list")).toBeVisible();

    const enCards = page.getByTestId("room-card");
    await expect(enCards).toHaveCount(3);
    await expect(page.getByTestId("room-card-name")).toHaveText([
      "Arched Cave Suite",
      "Fairy Chimney View Deluxe Stone Room",
      "Sultan Panoramic Mansion Suite",
    ]);
    await expect(page.getByText("Courtyard Room Under Restoration")).toHaveCount(0);

    const firstEnCard = enCards.nth(0);
    await expect(firstEnCard.getByTestId("room-card-capacity")).toHaveText("Up to 2 guests");
    await expect(firstEnCard.getByTestId("room-card-amenity")).toHaveText([
      "Private Carved-Stone Jacuzzi",
      "Artisan Espresso & Tea Bar",
      "High-Speed Wireless Internet",
    ]);

    await expect(page.locator('[data-testid^="price"]')).toHaveCount(0);
    for (let i = 0; i < 3; i++) {
      const cardText = await enCards.nth(i).innerText();
      expect(cardText).not.toMatch(/[₺€$]/);
    }

    await page.screenshot({
      path: path.join(EVIDENCE_DIR, "AC-6-web-rooms-en.png"),
      fullPage: true,
    });

    // Switch back to /tr/rooms via locale-switch-tr
    await page.getByTestId("locale-switch-tr").click();
    await expect(page).toHaveURL(/\/tr\/rooms$/);
  });

  test("@seeded shouldRenderLoadingEmptyAndErrorStatesOnAdminWeb_whenTestingThreeMandatoryStates", async ({
    page,
  }) => {
    // 1. Loading state via Playwright route delay
    let releaseLoading!: () => void;
    const loadingGate = new Promise<void>((resolve) => {
      releaseLoading = resolve;
    });

    await page.route("**/admin-api/system/sample-items", async (route) => {
      await loadingGate;
      await route.continue();
    });

    await page.goto(ADMIN_WEB_BASE_URL);
    await expect(page.locator("html")).toHaveAttribute("lang", "tr");
    await expect(page.getByTestId("admin-status-loading")).toBeVisible();
    await page.screenshot({
      path: path.join(EVIDENCE_DIR, "AC-6-admin-web-loading.png"),
      fullPage: true,
    });

    // 2. Release gate -> real admin-api endpoint (via Next.js rewrite) returns 200 {"items": []} -> Empty state
    releaseLoading();
    await expect(page.getByTestId("admin-status-empty")).toBeVisible();
    await expect(page.getByTestId("admin-status-empty-title")).toHaveText(
      "Henüz görüntülenecek operasyonel kayıt bulunmuyor",
    );
    await page.screenshot({
      path: path.join(EVIDENCE_DIR, "AC-6-admin-web-empty.png"),
      fullPage: true,
    });

    await page.unroute("**/admin-api/system/sample-items");

    // 3. Error state via Playwright route 500 application/problem+json injection
    await page.route("**/admin-api/system/sample-items", async (route) => {
      await route.fulfill({
        status: 500,
        contentType: "application/problem+json",
        headers: {
          "X-Request-Id": "0199f2c0-0000-7000-8000-000000000012",
        },
        body: JSON.stringify({
          type: "about:blank",
          title: "Internal Server Error",
          status: 500,
          detail: "Internal admin stack trace must never be shown to operator",
          instance: "/admin-api/system/sample-items",
          code: "system.internal_error",
          requestId: "0199f2c0-0000-7000-8000-000000000012",
        }),
      });
    });

    await page.reload();
    await expect(page.getByTestId("admin-status-error")).toBeVisible();
    await expect(page.getByTestId("admin-status-error-message")).toHaveText(
      "Yönetim servisinde geçici bir sorun oluştu. Lütfen tekrar deneyin.",
    );
    await expect(page.getByText("Internal admin stack trace")).toHaveCount(0);
    await expect(page.getByTestId("admin-status-retry")).toBeVisible();

    await page.screenshot({
      path: path.join(EVIDENCE_DIR, "AC-6-admin-web-error.png"),
      fullPage: true,
    });
  });
});
