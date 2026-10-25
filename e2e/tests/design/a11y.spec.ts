import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "@playwright/test";
import { WEB_BASE_URL } from "../../helpers/config";
import { capture, writeEvidence } from "../../helpers/evidence";
import { mockRooms } from "../../helpers/rooms";
import { MOBILE, SIX_ROUTES } from "../../helpers/viewports";

test.describe("Design — accessibility (AC-4, B-08, B-09, B-12)", () => {
  test(
    "shouldFocusTheSkipLinkFirstAndMoveFocusToMainContent",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      await page.goto(`${WEB_BASE_URL}/tr`, { waitUntil: "networkidle" });
      await page.keyboard.press("Tab");
      const skip = page.getByTestId("skip-to-content");
      await expect(skip).toBeFocused();
      await expect(skip).toBeVisible();
      await capture(page, "0003", "AC-4-keyboard-focus-header.png", {
        fullPage: false,
      });

      await page.keyboard.press("Enter");
      await expect(page.locator("#main-content")).toBeFocused();
    },
  );

  test(
    "shouldExposeSingleH1LandmarksCurrentNavAndAccessibleContactLinks",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      await mockRooms(page);
      for (const route of SIX_ROUTES) {
        await page.goto(`${WEB_BASE_URL}${route}`, { waitUntil: "networkidle" });
        await expect(page.locator("h1")).toHaveCount(1);
        await expect(page.getByRole("banner")).toBeVisible();
        await expect(page.getByRole("contentinfo")).toBeVisible();
      }

      await page.goto(`${WEB_BASE_URL}/tr`, { waitUntil: "networkidle" });
      await expect(page.getByTestId("site-nav-home")).toHaveAttribute("aria-current", "page");
      await expect(page.getByTestId("home-contact-phone")).toHaveAttribute("href", /^tel:/);
      await expect(page.getByTestId("home-contact-email")).toHaveAttribute("href", /^mailto:/);
      await expect(page.getByTestId("home-hero-poster")).toHaveAttribute(
        "aria-hidden",
        "true",
      );

      await page.goto(`${WEB_BASE_URL}/tr/rooms`, { waitUntil: "networkidle" });
      await expect(page.getByTestId("site-nav-rooms")).toHaveAttribute("aria-current", "page");
    },
  );

  test(
    "shouldTrapFocusLockScrollAndReturnFocusInTheMobileMenu",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      await page.setViewportSize(MOBILE);
      await page.goto(`${WEB_BASE_URL}/tr`, { waitUntil: "networkidle" });
      const toggle = page.getByTestId("mobile-nav-toggle");
      await toggle.click();
      await expect(page.getByTestId("mobile-nav")).toBeVisible();
      await expect(page.getByTestId("mobile-nav")).toHaveAttribute("role", "dialog");
      await expect(page.getByTestId("mobile-nav")).toHaveAttribute("aria-modal", "true");
      expect(await page.evaluate(() => getComputedStyle(document.body).overflow)).toBe(
        "hidden",
      );

      for (let index = 0; index < 8; index += 1) {
        await page.keyboard.press("Tab");
        const inside = await page.evaluate(
          () => !!document.activeElement?.closest('[data-testid="mobile-nav"]'),
        );
        expect(inside).toBe(true);
      }

      await capture(page, "0003", "AC-4-mobile-menu-keyboard.png", {
        fullPage: false,
      });
      await page.keyboard.press("Escape");
      await expect(page.getByTestId("mobile-nav")).toHaveCount(0);
      await expect(toggle).toBeFocused();
    },
  );

  test(
    "shouldCloseTheMobileMenuOnRouteChange",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      await mockRooms(page);
      await page.setViewportSize(MOBILE);
      await page.goto(`${WEB_BASE_URL}/tr`, { waitUntil: "networkidle" });
      await page.getByTestId("mobile-nav-toggle").click();
      await expect(page.getByTestId("mobile-nav")).toBeVisible();
      await page.getByTestId("mobile-nav-rooms").click();
      await expect(page).toHaveURL(/\/tr\/rooms$/);
      await expect(page.getByTestId("mobile-nav")).toHaveCount(0);
    },
  );

  test(
    "shouldDisableVideoPinsAndMotionUnderReducedMotion",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      await page.emulateMedia({ reducedMotion: "reduce" });
      await page.goto(`${WEB_BASE_URL}/tr`, { waitUntil: "networkidle" });
      await expect(page.getByTestId("home-hero-video")).toHaveCount(0);
      await expect(page.locator('[data-motion="pinned"]')).toHaveCount(0);
      await expect(page.getByText("Taşın Hafızası, Konuğun Huzuru")).toBeVisible();
      const flags = await page.evaluate(() => window.__hotelMotionLoaded);
      expect(flags?.gsap ?? false).toBe(false);
      expect(flags?.lenis ?? false).toBe(false);
      await capture(page, "0003", "AC-4-reduced-motion-home.png");
    },
  );

  test(
    "shouldHaveZeroCriticalOrSeriousAxeViolationsOnAllSixRoutes",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      await mockRooms(page);
      const rows: string[] = [
        "# Axe erişilebilirlik raporu",
        "",
        "| Rota | Kritik | Ciddi | Orta | Küçük |",
        "|---|---:|---:|---:|---:|",
      ];
      let blocking = 0;

      for (const route of SIX_ROUTES) {
        await page.goto(`${WEB_BASE_URL}${route}`, { waitUntil: "networkidle" });
        const results = await new AxeBuilder({ page }).analyze();
        const counts = { critical: 0, serious: 0, moderate: 0, minor: 0 };
        for (const violation of results.violations) {
          const impact = violation.impact ?? "minor";
          if (impact in counts) {
            counts[impact as keyof typeof counts] += 1;
          }
          if (impact === "critical" || impact === "serious") {
            blocking += 1;
            rows.push(
              "",
              `- **${route} / ${violation.id}** (${impact}): ${violation.help} — ${violation.nodes.length} düğüm`,
            );
          }
        }
        rows.push(
          `| ${route} | ${counts.critical} | ${counts.serious} | ${counts.moderate} | ${counts.minor} |`,
        );
      }

      rows.push("", `**Kritik + ciddi toplam:** ${blocking}`);
      writeEvidence("0003", "AC-4-axe-report.md", rows.join("\n"));
      expect(blocking, rows.join("\n")).toBe(0);
    },
  );
});
