import fs from "fs";
import path from "path";
import { expect, test } from "@playwright/test";

const WEB_BASE_URL = process.env.E2E_WEB_URL ?? "http://localhost:3000";
const ADMIN_WEB_BASE_URL = process.env.E2E_ADMIN_WEB_URL ?? "http://localhost:3001";
const EVIDENCE_DIR = path.resolve(__dirname, "../../specs/done/evidence/0001");

test.beforeAll(() => {
  fs.mkdirSync(EVIDENCE_DIR, { recursive: true });
});

test.describe("Smoke E2E — web (:3000) and admin-web (:3001) (AC-7, AC-8, AC-10, AC-11)", () => {
  test("shouldRedirectRootToTrAndSwitchLocaleBetweenTrAndEn_whenNavigatingWeb", async ({
    page,
  }) => {
    await page.goto(WEB_BASE_URL);
    await expect(page).toHaveURL(/\/tr$/);
    await expect(page.locator("html")).toHaveAttribute("lang", "tr");
    await expect(page.getByTestId("web-site-title")).toHaveText("Kapadokya Taş Konak Otel");
    await expect(page.getByTestId("web-status-empty")).toBeVisible();

    await page.screenshot({
      path: path.join(EVIDENCE_DIR, "AC-7-web-tr.png"),
      fullPage: true,
    });
    await page.screenshot({
      path: path.join(EVIDENCE_DIR, "AC-10-smoke-web.png"),
      fullPage: true,
    });

    await page.getByTestId("locale-switch-en").click();
    await expect(page).toHaveURL(/\/en$/);
    await expect(page.locator("html")).toHaveAttribute("lang", "en");
    await expect(page.getByTestId("web-site-title")).toHaveText(
      "Cappadocia Stone Mansion Hotel",
    );
    await expect(page.getByTestId("web-status-empty")).toBeVisible();

    await page.screenshot({
      path: path.join(EVIDENCE_DIR, "AC-7-web-en.png"),
      fullPage: true,
    });
  });

  test("shouldRenderLoadingEmptyAndErrorStatesOnWeb_whenTestingThreeMandatoryStates", async ({
    page,
  }) => {
    // 1. Loading state via Playwright route delay
    let releaseLoading!: () => void;
    const loadingGate = new Promise<void>((resolve) => {
      releaseLoading = resolve;
    });

    await page.route("**/api/system/sample-items", async (route) => {
      await loadingGate;
      await route.continue();
    });

    await page.goto(`${WEB_BASE_URL}/tr`);
    await expect(page.getByTestId("web-status-loading")).toBeVisible();
    await page.screenshot({
      path: path.join(EVIDENCE_DIR, "AC-11-web-loading.png"),
      fullPage: true,
    });

    // 2. Release gate -> real hotel-api endpoint returns 200 {"items": []} -> Empty state
    releaseLoading();
    await expect(page.getByTestId("web-status-empty")).toBeVisible();
    await expect(page.getByTestId("web-status-empty-title")).toHaveText(
      "Henüz yayınlanmış oda bulunmuyor",
    );
    await page.screenshot({
      path: path.join(EVIDENCE_DIR, "AC-11-web-empty.png"),
      fullPage: true,
    });

    await page.unroute("**/api/system/sample-items");

    // 3. Error state via Playwright route 500 application/problem+json injection
    await page.route("**/api/system/sample-items", async (route) => {
      await route.fulfill({
        status: 500,
        contentType: "application/problem+json",
        headers: {
          "X-Request-Id": "0199f2c0-0000-7000-8000-000000000011",
        },
        body: JSON.stringify({
          type: "about:blank",
          title: "Internal Server Error",
          status: 500,
          detail: "Internal stack trace must never be shown to guest",
          instance: "/api/system/sample-items",
          code: "system.internal_error",
          requestId: "0199f2c0-0000-7000-8000-000000000011",
        }),
      });
    });

    await page.reload();
    await expect(page.getByTestId("web-status-error")).toBeVisible();
    await expect(page.getByTestId("web-status-error-message")).toHaveText(
      "Sistemde geçici bir sorun oluştu. Lütfen kısa süre sonra tekrar deneyin.",
    );
    await expect(page.getByText("Internal stack trace")).toHaveCount(0);
    await expect(page.getByTestId("web-status-retry")).toBeVisible();

    await page.screenshot({
      path: path.join(EVIDENCE_DIR, "AC-11-web-error.png"),
      fullPage: true,
    });
  });

  test("shouldRenderLoadingEmptyAndErrorStatesOnAdminWeb_whenTestingThreeMandatoryStates", async ({
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
      path: path.join(EVIDENCE_DIR, "AC-11-admin-web-loading.png"),
      fullPage: true,
    });

    // 2. Release gate -> real admin-api endpoint (via Next.js rewrite) returns 200 {"items": []} -> Empty state
    releaseLoading();
    await expect(page.getByTestId("admin-status-empty")).toBeVisible();
    await expect(page.getByTestId("admin-status-empty-title")).toHaveText(
      "Henüz görüntülenecek operasyonel kayıt bulunmuyor",
    );
    await page.screenshot({
      path: path.join(EVIDENCE_DIR, "AC-11-admin-web-empty.png"),
      fullPage: true,
    });
    await page.screenshot({
      path: path.join(EVIDENCE_DIR, "AC-8-admin-web-tr.png"),
      fullPage: true,
    });
    await page.screenshot({
      path: path.join(EVIDENCE_DIR, "AC-10-smoke-admin-web.png"),
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
      path: path.join(EVIDENCE_DIR, "AC-11-admin-web-error.png"),
      fullPage: true,
    });
  });
});
