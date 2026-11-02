import { expect, test } from "@playwright/test";
import { ADMIN_WEB_BASE_URL } from "../../helpers/config";
import { capture } from "../../helpers/evidence";

const SUPERADMIN_USERNAME =
  process.env.E2E_ADMIN_SUPERADMIN_USERNAME ?? "superadmin";
const SUPERADMIN_PASSWORD =
  process.env.E2E_ADMIN_SUPERADMIN_PASSWORD ?? "LocalOnly-Cappadocia-2026!";

async function openLogin(page: import("@playwright/test").Page) {
  await page.goto(`${ADMIN_WEB_BASE_URL}/login?returnTo=/`, {
    waitUntil: "networkidle",
  });
  await expect(page.getByTestId("admin-login-form")).toBeVisible();
}

async function login(page: import("@playwright/test").Page) {
  await openLogin(page);
  await page.waitForFunction(() => document.cookie.includes("XSRF-TOKEN="));
  await page.getByTestId("admin-login-identifier").fill(SUPERADMIN_USERNAME);
  await page.getByTestId("admin-login-password").fill(SUPERADMIN_PASSWORD);
  const loginResponse = page.waitForResponse(
    (response) =>
      response.url().endsWith("/admin-api/auth/login") &&
      response.request().method() === "POST",
  );
  await page.getByTestId("admin-login-submit").click();
  const response = await loginResponse;
  expect(response.status()).toBe(204);
  await page.waitForTimeout(500);
}

test.describe("Admin auth (AC-6)", () => {
  test(
    "shouldLoginAndRenderProtectedHome_whenCredentialsAreValid",
    { tag: ["@admin"] },
    async ({ page }) => {
      await openLogin(page);
      await capture(page, "0004", "AC-6-admin-login-tr.png");
      await page.getByTestId("admin-login-identifier").fill(SUPERADMIN_USERNAME);
      await page.getByTestId("admin-login-password").fill(SUPERADMIN_PASSWORD);
      await page.getByTestId("admin-login-submit").click();

      await expect(page).toHaveURL(new RegExp(`${ADMIN_WEB_BASE_URL}/$`));
      await expect(page.getByTestId("admin-home-page")).toBeVisible();
      await expect(page.getByTestId("admin-home-page")).not.toContainText(
        "Yönetim verileri şu anda yüklenemedi.",
      );
      await capture(page, "0004", "AC-6-admin-home-protected.png");
    },
  );

  test(
    "shouldShowGenericError_whenCredentialsAreInvalid",
    { tag: ["@admin"] },
    async ({ page }) => {
      await openLogin(page);
      await page.getByTestId("admin-login-identifier").fill(SUPERADMIN_USERNAME);
      await page.getByTestId("admin-login-password").fill("Wrong-Credential-2026!");
      await page.getByTestId("admin-login-submit").click();

      await expect(page.getByTestId("admin-login-error")).toBeVisible();
      await expect(page.getByTestId("admin-login-error")).toHaveText(
        "Kullanıcı adı veya parola hatalı.",
      );
      await expect(page).toHaveURL(/\/login/);
      await capture(page, "0004", "AC-6-admin-login-error.png");
    },
  );

  test(
    "shouldLogoutAndReturnToLogin_whenSessionIsActive",
    { tag: ["@admin"] },
    async ({ page }) => {
      await login(page);
      await expect(page.getByTestId("admin-home-page")).toBeVisible({ timeout: 15_000 });
      await page.getByTestId("admin-logout").click();

      await expect(page).toHaveURL(new RegExp(`${ADMIN_WEB_BASE_URL}/login`));
      await expect(page.getByTestId("admin-login-page")).toBeVisible();
      await capture(page, "0004", "AC-6-admin-logout.png");
    },
  );
});
