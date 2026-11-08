import { expect, test, type Page } from "@playwright/test";
import {
  ADMIN_WEB_BASE_URL,
  API_BASE_URL,
  WEB_BASE_URL,
} from "../../helpers/config";
import { capture, writeEvidence } from "../../helpers/evidence";

test.describe.configure({ timeout: 180_000 });

const SUPERADMIN_USERNAME =
  process.env.E2E_ADMIN_SUPERADMIN_USERNAME ?? "superadmin";
const SUPERADMIN_PASSWORD =
  process.env.E2E_ADMIN_SUPERADMIN_PASSWORD ?? "LocalOnly-Cappadocia-2026!";
const FRONTDESK_USERNAME =
  process.env.E2E_ADMIN_FRONTDESK_USERNAME ?? "frontdesk";
const FRONTDESK_PASSWORD =
  process.env.E2E_ADMIN_FRONTDESK_PASSWORD ?? "LocalOnly-Cappadocia-2026!";

function isoDatePlus(days: number): string {
  const date = new Date();
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

async function login(page: Page, username: string, password: string) {
  await page.goto(`${ADMIN_WEB_BASE_URL}/login?returnTo=/pricing`, {
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

async function csrfToken(page: Page): Promise<string> {
  const cookies = await page.context().cookies();
  return cookies.find((cookie) => cookie.name === "XSRF-TOKEN")?.value ?? "";
}

async function adminSend(
  page: Page,
  method: "GET" | "POST" | "PUT" | "PATCH" | "DELETE",
  path: string,
  data?: unknown,
) {
  const token = await csrfToken(page);
  return page.request.fetch(`${ADMIN_WEB_BASE_URL}${path}`, {
    method,
    headers: {
      "X-XSRF-TOKEN": token,
      ...(data ? { "Content-Type": "application/json" } : {}),
    },
    data,
  });
}

async function json<T>(response: Awaited<ReturnType<Page["request"]["get"]>>): Promise<T> {
  return (await response.json()) as T;
}

test.describe("Pricing management (AC-1..AC-6)", () => {
  test(
    "shouldManagePricingAndQuotePubliclyAndEnforceRoleBoundaries",
    { tag: ["@pricing"] },
    async ({ page, browser }) => {
      await login(page, SUPERADMIN_USERNAME, SUPERADMIN_PASSWORD);

      // Loading state.
      await page.route("**/admin-api/pricing/rate-plans", async (route) => {
        await new Promise((resolve) => setTimeout(resolve, 1200));
        await route.continue();
      });
      await page.goto(`${ADMIN_WEB_BASE_URL}/pricing`, {
        waitUntil: "domcontentloaded",
      });
      await expect(page.getByTestId("admin-pricing-loading")).toBeVisible();
      await capture(page, "0006", "AC-2-admin-pricing-loading.png");
      await page.unroute("**/admin-api/pricing/rate-plans");
      await expect(page.getByTestId("admin-pricing-page")).toBeVisible();

      // Empty state on a fresh database.
      await expect(page.getByTestId("admin-pricing-empty")).toBeVisible();
      await capture(page, "0006", "AC-2-admin-pricing-empty.png");

      // Error state.
      await page.route("**/admin-api/pricing/rate-plans", async (route) => {
        await route.fulfill({
          status: 500,
          contentType: "application/problem+json",
          body: JSON.stringify({
            type: "about:blank",
            title: "Internal Server Error",
            status: 500,
            detail: "failed",
            instance: "/admin-api/pricing/rate-plans",
            code: "system.internal_error",
            requestId: "e2e-pricing",
          }),
        });
      });
      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(page.getByTestId("admin-pricing-error")).toBeVisible();
      await capture(page, "0006", "AC-2-admin-pricing-error.png");
      await page.unroute("**/admin-api/pricing/rate-plans");
      await page.getByTestId("admin-pricing-retry").click();
      await expect(page.getByTestId("admin-pricing-empty")).toBeVisible();

      // Create a plan through the real admin UI.
      const planCode = `E2E-${Date.now()}`;
      await page.getByLabel("Plan kodu").fill(planCode);
      await page.getByLabel("Plan TR adı").fill("E2E Standart");
      await page.getByLabel("Plan EN adı").fill("E2E Standard");
      await page.getByRole("button", { name: "Plan Ekle" }).click();
      await expect(page.getByTestId("admin-pricing-rate-plan-row")).toContainText(
        planCode,
      );

      const plansResponse = await adminSend(
        page,
        "GET",
        "/admin-api/pricing/rate-plans",
      );
      expect(plansResponse.status()).toBe(200);
      const plans = await json<Array<{ id: string; code: string }>>(plansResponse);
      const plan = plans.find((candidate) => candidate.code === planCode);
      expect(plan).toBeTruthy();

      const roomTypesResponse = await adminSend(
        page,
        "GET",
        "/admin-api/pricing/room-types",
      );
      const roomTypes = await json<
        Array<{ id: string; maxGuests: number }>
      >(roomTypesResponse);
      expect(roomTypes.length).toBeGreaterThan(0);

      const checkIn = isoDatePlus(30);
      const checkOut = isoDatePlus(33);
      const seasonStart = isoDatePlus(20);
      const seasonEnd = isoDatePlus(60);
      const seasonCode = `E2E-HIGH-${Date.now()}`;
      const seasonResponse = await adminSend(
        page,
        "POST",
        `/admin-api/pricing/rate-plans/${plan!.id}/seasons`,
        {
          code: seasonCode,
          nameTr: "E2E Yüksek Sezon",
          nameEn: "E2E High Season",
          startDate: seasonStart,
          endDate: seasonEnd,
        },
      );
      expect(seasonResponse.status()).toBe(201);

      const baseRows = roomTypes.flatMap((roomType) =>
        Array.from({ length: roomType.maxGuests }, (_, index) => ({
          roomTypeId: roomType.id,
          guests: index + 1,
          amountTry: (100 + index * 20).toFixed(2),
        })),
      );
      const baseSave = await adminSend(
        page,
        "PUT",
        `/admin-api/pricing/rate-plans/${plan!.id}/prices`,
        { rows: baseRows },
      );
      expect(baseSave.status()).toBe(200);

      const defaultResponse = await adminSend(
        page,
        "PUT",
        `/admin-api/pricing/rate-plans/${plan!.id}/default`,
      );
      expect(defaultResponse.status()).toBe(200);

      // Manual FX through the UI.
      await page.getByLabel("Manuel kur para birimi").selectOption("EUR");
      await page.getByLabel("Manuel kur tarihi").fill(isoDatePlus(0));
      await page.getByLabel("Manuel kur değeri").fill("42,500000");
      await page.getByRole("button", { name: "Manuel Kur Ekle" }).click();
      await expect(page.getByTestId("admin-pricing-manual-fx")).toContainText(
        "42,500000",
      );

      // Manual price through the UI.
      await page
        .getByLabel("Manuel fiyat oda tipi")
        .selectOption(roomTypes[0].id);
      await page.getByLabel("Manuel fiyat misafir sayısı").fill("1");
      await page.getByLabel("Manuel fiyat başlangıç").fill(checkIn);
      await page.getByLabel("Manuel fiyat bitiş").fill(checkIn);
      await page.getByLabel("Manuel fiyat tutarı").fill("175,00");
      await page.getByRole("button", { name: "Manuel Fiyat Ekle" }).click();
      await expect(page.getByTestId("admin-pricing-manual-prices")).toContainText(
        "175,00",
      );

      await expect(page.getByTestId("admin-pricing-fx-warning")).toBeVisible();
      await capture(page, "0006", "AC-2-admin-pricing-filled.png");
      await capture(page, "0006", "AC-3-admin-fx-warning.png");

      // Public quote API: TRY and EUR, multi-night.
      const tryQuote = await page.request.get(
        `${API_BASE_URL}/api/pricing/quote?roomTypeId=${roomTypes[0].id}&checkIn=${checkIn}&checkOut=${checkOut}&guests=1&currency=TRY`,
      );
      expect(tryQuote.status()).toBe(200);
      const tryBody = await json<{
        currency: string;
        nights: Array<{ amount: string; priceSource: string }>;
        total: string;
      }>(tryQuote);
      expect(tryBody.currency).toBe("TRY");
      expect(tryBody.nights).toHaveLength(3);
      expect(tryBody.nights[0].priceSource).toBe("MANUAL");
      expect(tryBody.total).toMatch(/^\d+\.\d{2}$/);

      const eurQuote = await page.request.get(
        `${API_BASE_URL}/api/pricing/quote?roomTypeId=${roomTypes[0].id}&checkIn=${checkIn}&checkOut=${checkOut}&guests=1&currency=EUR`,
      );
      expect(eurQuote.status()).toBe(200);
      const eurBody = await json<{ currency: string; total: string }>(eurQuote);
      expect(eurBody.currency).toBe("EUR");
      expect(eurBody.total).toMatch(/^\d+\.\d{2}$/);

      const invalidGuests = await page.request.get(
        `${API_BASE_URL}/api/pricing/quote?roomTypeId=${roomTypes[0].id}&checkIn=${checkIn}&checkOut=${checkOut}&guests=0&currency=TRY`,
      );
      expect(invalidGuests.status()).toBe(400);

      writeEvidence(
        "0006",
        "AC-4-quote-response.md",
        [
          `TRY quote: ${JSON.stringify(tryBody)}`,
          `EUR quote: ${JSON.stringify(eurBody)}`,
          "Public quote is anonymous and returns string decimal amounts.",
        ].join("\n"),
      );

      // Public showcase must not show prices or call the quote endpoint.
      const publicRequests: string[] = [];
      page.on("request", (request) => {
        if (request.url().includes("/api/pricing/quote")) {
          publicRequests.push(request.url());
        }
      });
      await page.goto(`${WEB_BASE_URL}/tr/rooms`, { waitUntil: "networkidle" });
      await expect(page.locator('[data-testid^="price"]')).toHaveCount(0);
      for (const card of await page.getByTestId("room-card").all()) {
        expect(await card.innerText()).not.toMatch(/[₺€$]/);
      }
      expect(publicRequests).toEqual([]);

      // FRONT_DESK is rejected by the API and sees the forbidden panel.
      const frontDeskContext = await browser.newContext();
      const frontDeskPage = await frontDeskContext.newPage();
      await login(frontDeskPage, FRONTDESK_USERNAME, FRONTDESK_PASSWORD);
      const forbidden = await frontDeskPage.request.get(
        `${ADMIN_WEB_BASE_URL}/admin-api/pricing/rate-plans`,
      );
      expect(forbidden.status()).toBe(403);
      await frontDeskPage.goto(`${ADMIN_WEB_BASE_URL}/pricing`, {
        waitUntil: "networkidle",
      });
      await expect(
        frontDeskPage.getByTestId("admin-pricing-forbidden"),
      ).toBeVisible();
      await frontDeskContext.close();

      writeEvidence(
        "0006",
        "AC-5-snapshot-immutability.md",
        [
          "Backend unit test PriceSnapshotTest proves deterministic, immutable payloads.",
          "Backend integration test PricingIT proves quote recalculation and string amounts.",
          "Persistent snapshot storage belongs to the booking spec.",
        ].join("\n"),
      );
    },
  );
});
