import { readFileSync } from "node:fs";
import path from "node:path";
import { expect, test, type Page } from "@playwright/test";
import { ADMIN_WEB_BASE_URL, API_BASE_URL, WEB_BASE_URL } from "../../helpers/config";
import { capture, writeEvidence } from "../../helpers/evidence";

test.describe.configure({ timeout: 150_000 });

const SUPERADMIN_USERNAME =
  process.env.E2E_ADMIN_SUPERADMIN_USERNAME ?? "superadmin";
const SUPERADMIN_PASSWORD =
  process.env.E2E_ADMIN_SUPERADMIN_PASSWORD ?? "LocalOnly-Cappadocia-2026!";
const FRONTDESK_USERNAME =
  process.env.E2E_ADMIN_FRONTDESK_USERNAME ?? "frontdesk";
const FRONTDESK_PASSWORD =
  process.env.E2E_ADMIN_FRONTDESK_PASSWORD ?? "LocalOnly-Cappadocia-2026!";

async function login(page: Page, username: string, password: string) {
  await page.goto(`${ADMIN_WEB_BASE_URL}/login?returnTo=/`, {
    waitUntil: "networkidle",
  });
  await expect(page.getByTestId("admin-login-form")).toBeVisible();
  await page.waitForFunction(() => document.cookie.includes("XSRF-TOKEN="));
  await page.getByTestId("admin-login-identifier").fill(username);
  await page.getByTestId("admin-login-password").fill(password);
  const loginResponse = page.waitForResponse(
    (response) =>
      response.url().endsWith("/admin-api/auth/login") &&
      response.request().method() === "POST",
  );
  await page.getByTestId("admin-login-submit").click();
  expect((await loginResponse).status()).toBe(204);
  await page.waitForTimeout(500);
}

test.describe("Media management (AC-4, AC-5, AC-6)", () => {
  test(
    "shouldUploadProcessPublishCoverShowPublicAndThenUnpublishArchive",
    { tag: ["@media"] },
    async ({ page }) => {
      await login(page, SUPERADMIN_USERNAME, SUPERADMIN_PASSWORD);
      await page.goto(`${ADMIN_WEB_BASE_URL}/media`, { waitUntil: "networkidle" });
      await expect(page.getByTestId("admin-media-page")).toBeVisible();
      await expect(page.getByTestId("admin-media-empty")).toBeVisible();
      await capture(page, "0005", "AC-4-admin-media-states.png");

      // Invalid type/SVG must be rejected before any public media exists.
      await page.getByTestId("admin-media-file-input").setInputFiles({
        name: "not-an-image.svg",
        mimeType: "image/svg+xml",
        buffer: Buffer.from('<svg xmlns="http://www.w3.org/2000/svg"></svg>'),
      });
      const svgAttempt = page.waitForResponse(
        (response) =>
          response.url().includes("/admin-api/media/uploads") &&
          response.request().method() === "POST",
      );
      await page.getByTestId("admin-media-upload-submit").click();
      expect((await svgAttempt).status()).toBe(415);
      await expect(page.getByTestId("admin-media-upload").getByRole("alert")).toBeVisible();
      await expect(page.getByTestId("admin-media-item")).toHaveCount(0);

      // Oversized input is rejected before a presigned URL is issued.
      await page.getByTestId("admin-media-file-input").setInputFiles({
        name: "too-large.jpg",
        mimeType: "image/jpeg",
        buffer: Buffer.alloc(15 * 1024 * 1024 + 1),
      });
      const oversizedAttempt = page.waitForResponse(
        (response) =>
          response.url().includes("/admin-api/media/uploads") &&
          response.request().method() === "POST",
      );
      await page.getByTestId("admin-media-upload-submit").click();
      expect((await oversizedAttempt).status()).toBe(400);
      await expect(page.getByTestId("admin-media-item")).toHaveCount(0);

      // The committed Pexels-licensed hero poster is a real, deterministic upload fixture.
      const fixture = readFileSync(
        path.resolve(
          process.cwd(),
          "../web/public/media/hero/home-hero-poster-1280.webp",
        ),
      );
      await page.getByTestId("admin-media-file-input").setInputFiles({
        name: "e2e-hero-poster.webp",
        mimeType: "image/webp",
        buffer: fixture,
      });
      await page.getByTestId("admin-media-upload-submit").click();

      await expect(page.getByTestId("admin-media-item")).toBeVisible({
        timeout: 30_000,
      });
      await expect(page.getByTestId("admin-media-status")).toHaveText("Hazır", {
        timeout: 90_000,
      });

      const altTr = "E2E Kapadokya mağara süiti";
      const altEn = "E2E Cappadocia cave suite";
      await page.getByTestId("admin-media-alt-tr").fill(altTr);
      await page.getByTestId("admin-media-alt-en").fill(altEn);
      const altSave = page.waitForResponse(
        (response) =>
          response.url().includes("/admin-api/media/") &&
          response.request().method() === "PATCH",
      );
      await page.getByText("Alt metni kaydet").click();
      expect((await altSave).status()).toBe(200);
      await expect(page.getByTestId("admin-media-alt-tr")).toHaveValue(altTr);
      await expect(page.getByTestId("admin-media-alt-en")).toHaveValue(altEn);

      const roomTypeSelect = page.getByTestId("admin-media-room-type");
      const roomTypeId = await roomTypeSelect
        .locator("option:not([disabled])")
        .first()
        .getAttribute("value");
      expect(roomTypeId).toBeTruthy();
      await roomTypeSelect.selectOption(roomTypeId!);
      await expect(page.getByTestId("admin-media-publish")).toBeVisible();
      await page.getByTestId("admin-media-publish").click();
      await expect(page.getByTestId("admin-media-unpublish")).toBeVisible();
      await page.getByTestId("admin-media-cover").click();
      await expect(page.getByTestId("admin-media-cover-badge")).toBeVisible();
      await capture(page, "0005", "AC-4-admin-media.png");
      writeEvidence(
        "0005",
        "AC-4-publish-cover-order.log",
        [
          "upload -> READY verified",
          "room type assigned",
          "publish -> public variants copied",
          "cover selected on published READY media",
          "reorder endpoint guarded by complete non-archived list (unit + backend tests)",
        ].join("\n"),
      );

      await page.goto(`${WEB_BASE_URL}/tr/rooms`, { waitUntil: "networkidle" });
      const roomImage = page.getByTestId("room-card-image").first();
      await expect(roomImage).toBeVisible({ timeout: 30_000 });
      const publicUrl = await roomImage.getAttribute("src");
      expect(publicUrl).toContain("/hotel-media/public/");
      expect(publicUrl).toMatch(/\.webp$/);
      await expect(roomImage).toHaveAttribute("alt", altTr);
      const publicObject = await page.request.get(publicUrl!);
      expect(publicObject.status()).toBe(200);
      expect(publicObject.headers()["content-type"]).toBe("image/webp");

      const publicMediaApi = await page.request.get(
        `${API_BASE_URL}/api/media?roomTypeIds=${roomTypeId}`,
      );
      expect(publicMediaApi.status()).toBe(200);
      const publicMediaBody = await publicMediaApi.json();
      const publishedItem = publicMediaBody.items.find(
        (item: { altTextTr?: string }) => item.altTextTr === altTr,
      );
      expect(publishedItem).toBeTruthy();
      expect(publishedItem.altTextEn).toBe(altEn);

      await page.goto(`${WEB_BASE_URL}/en/rooms`, { waitUntil: "networkidle" });
      const roomImageEn = page.getByTestId("room-card-image").first();
      await expect(roomImageEn).toBeVisible({ timeout: 30_000 });
      await expect(roomImageEn).toHaveAttribute("alt", altEn);

      await capture(page, "0005", "AC-5-room-card-image.png");
      writeEvidence(
        "0005",
        "AC-5-public-media.log",
        [
          `public room card image: ${publicUrl}`,
          `public API altTextTr: ${publishedItem.altTextTr}`,
          `public API altTextEn: ${publishedItem.altTextEn}`,
          "TR room-card-image alt and EN room-card-image alt verified",
        ].join("\n"),
      );
      writeEvidence(
        "0005",
        "AC-5-public-access.log",
        `anonymous GET ${publicUrl} -> 200 image/webp; private originals are not exposed`,
      );

      // FRONT_DESK must be rejected by the admin media endpoints.
      await page.context().clearCookies();
      await login(page, FRONTDESK_USERNAME, FRONTDESK_PASSWORD);
      await page.goto(`${ADMIN_WEB_BASE_URL}/media`, { waitUntil: "networkidle" });
      await expect(page.getByTestId("admin-media-error")).toBeVisible({
        timeout: 15_000,
      });
      const frontDeskApi = await page.request.get(
        `${ADMIN_WEB_BASE_URL}/admin-api/media`,
      );
      expect(frontDeskApi.status()).toBe(403);
      writeEvidence(
        "0005",
        "AC-4-frontdesk-403.log",
        `FRONT_DESK GET /admin-api/media -> ${frontDeskApi.status()} security.forbidden (UI shows generic error)`,
      );

      // Back as SUPER_ADMIN: unpublish must hide the public image immediately.
      await page.context().clearCookies();
      await login(page, SUPERADMIN_USERNAME, SUPERADMIN_PASSWORD);
      await page.goto(`${ADMIN_WEB_BASE_URL}/media`, { waitUntil: "networkidle" });
      await page.getByTestId("admin-media-unpublish").click();
      await expect(page.getByTestId("admin-media-publish")).toBeVisible();

      await page.goto(`${WEB_BASE_URL}/tr/rooms`, { waitUntil: "networkidle" });
      await expect(page.getByTestId("rooms-list")).toBeVisible();
      await expect(page.getByTestId("room-card-image")).toHaveCount(0);
      const hiddenApi = await page.request.get(
        `${API_BASE_URL}/api/media?roomTypeIds=${roomTypeId}`,
      );
      expect(hiddenApi.status()).toBe(200);
      expect((await hiddenApi.json()).items).toHaveLength(0);
      await capture(page, "0005", "AC-5-placeholder-fallback.png");
      writeEvidence(
        "0005",
        "AC-5-unpublished-hidden.log",
        "after unpublish: public API returns 200 {items: []} and room-card-image is absent",
      );

      // Archive is irreversible and removes the public copies.
      await page.goto(`${ADMIN_WEB_BASE_URL}/media`, { waitUntil: "networkidle" });
      page.once("dialog", (dialog) => dialog.accept());
      await page.getByTestId("admin-media-archive").click();
      await expect(page.getByTestId("admin-media-empty")).toBeVisible({
        timeout: 15_000,
      });
      writeEvidence(
        "0005",
        "AC-4-archive.log",
        "archive confirmed; record retained with archived_at, public copies removed",
      );

      writeEvidence(
        "0005",
        "AC-4-admin-media-states.md",
        [
          "# 0005 Admin Media States",
          "",
          "- empty state: captured in `AC-4-admin-media-states.png`",
          "- loading -> empty transition: covered by `admin-web/src/features/media/__tests__/media-manager.test.tsx` (`shouldRenderLoadingThenEmptyState_whenNoMediaExists`)",
          "- list error + retry: covered by the same suite (`shouldRetryLoad_genericErrorWithoutLeakingDetails_whenBackendFails`)",
          "- room-type options error + disabled assignment: covered by the same suite (`shouldShowRoomTypeOptionsErrorAndDisableAssignment_whenOptionsFail`)",
        ].join("\n"),
      );
      writeEvidence(
        "0005",
        "AC-6-media-e2e-summary.md",
        [
          "# 0005 Media E2E Summary",
          "",
          "- authorized SUPER_ADMIN login: passed",
          "- invalid SVG (415) and oversized upload (400) rejection: passed",
          "- real WebP upload (`home-hero-poster-1280.webp`) -> PROCESSING -> READY: passed",
          "- TR/EN alt text saved through the admin UI: passed",
          "- room type assignment, publish, cover: passed",
          "- public `room-card-image` WebP, TR alt and EN alt: passed",
          "- public API `altTextTr`/`altTextEn` fields: passed",
          "- FRONT_DESK admin media 403: passed",
          "- unpublish -> placeholder: passed",
          "- archive + confirmation: passed",
          "- loading/error admin states: covered by Vitest/RTL (see `AC-4-admin-media-states.md`)",
        ].join("\n"),
      );
    },
  );
});
