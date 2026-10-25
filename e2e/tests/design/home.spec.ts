import { expect, test } from "@playwright/test";
import { WEB_BASE_URL } from "../../helpers/config";
import { collectConsole } from "../../helpers/console";
import { loadDictionary } from "../../helpers/copy";
import { capture } from "../../helpers/evidence";
import { mockRooms } from "../../helpers/rooms";
import { MOBILE, SIX_ROUTES } from "../../helpers/viewports";

test.describe("Design — home, chrome and language (AC-2, AC-4, AC-6)", () => {
  test(
    "shouldRenderApprovedChromeAndCopyInTurkishAndEnglish",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      for (const locale of ["tr", "en"] as const) {
        const dict = loadDictionary(locale);
        await page.emulateMedia({ reducedMotion: "reduce" });
        await page.goto(`${WEB_BASE_URL}/${locale}`, { waitUntil: "networkidle" });

        await expect(page.getByTestId("site-header")).toBeVisible();
        await expect(page.getByRole("banner")).toBeVisible();
        await expect(page.getByRole("contentinfo")).toBeVisible();
        await expect(page.getByTestId("web-site-title")).toHaveCount(1);
        await expect(page.getByTestId("web-site-title")).toHaveText(dict.siteTitle);
        await expect(page.locator("h1")).toHaveCount(1);
        await expect(page.locator("h1")).toHaveText(dict.home.heroTitle);

        for (const testId of [
          "home-hero",
          "home-story",
          "home-experience",
          "home-rooms-cta",
          "home-contact",
          "site-footer",
        ]) {
          await expect(page.getByTestId(testId)).toBeVisible();
        }

        await expect(page.getByTestId("site-nav-home")).toHaveAttribute("aria-current", "page");
        await expect(page.getByText(dict.home.story.title)).toBeVisible();
        await expect(page.getByText(dict.home.experience.title)).toBeVisible();
        for (const card of Object.values(dict.home.experience.cards)) {
          await expect(page.getByText(card.title)).toBeVisible();
        }
        await expect(page.getByText(dict.home.roomsCta.title)).toBeVisible();
        await expect(page.getByText(dict.home.contact.title)).toBeVisible();
        await expect(page.getByTestId("home-rooms-link")).toHaveAttribute(
          "href",
          `/${locale}/rooms`,
        );
        await expect(page.getByTestId("home-story-image")).toHaveAttribute(
          "aria-hidden",
          "true",
        );
        await expect(page.getByTestId("home-story-image")).toHaveAttribute(
          "src",
          /\/media\/story\/home-story-portrait\.webp$/,
        );
        await expect(page.getByTestId("home-experience-image")).toHaveAttribute(
          "aria-hidden",
          "true",
        );
        await expect(page.getByTestId("home-experience-image")).toHaveAttribute(
          "src",
          /\/media\/experience\/home-experience-panorama\.webp$/,
        );
        await expect(page.getByTestId("home-story-image")).toHaveJSProperty(
          "complete",
          true,
        );
        await expect(page.getByTestId("home-experience-image")).toHaveJSProperty(
          "complete",
          true,
        );
        await expect(page.getByTestId("home-contact-address")).toHaveText(
          dict.home.contact.address,
        );
        await expect(page.getByTestId("home-contact-phone")).toHaveAttribute(
          "href",
          `tel:${dict.home.contact.phone.replace(/\s+/g, "")}`,
        );
        await expect(page.getByTestId("home-contact-email")).toHaveAttribute(
          "href",
          `mailto:${dict.home.contact.email}`,
        );
        await expect(page.getByTestId("footer-address")).toHaveText(
          dict.home.contact.address,
        );
        await expect(page.getByTestId("footer-copyright")).toHaveText(
          dict.footer.copyright,
        );
        await expect(page.getByTestId("footer-credits-link")).toHaveAttribute(
          "href",
          `/${locale}/credits`,
        );
        await page.evaluate(async () => {
          await Promise.all(
            Array.from(document.images).map((image) =>
              image.decode().catch(() => undefined),
            ),
          );
        });

        await capture(page, "0003", `AC-2-home-${locale}-desktop.png`);
      }
    },
  );

  test(
    "shouldRenderTheMobileHomeWithTheFullWordmark",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      await page.setViewportSize(MOBILE);
      await page.emulateMedia({ reducedMotion: "reduce" });
      await page.goto(`${WEB_BASE_URL}/tr`, { waitUntil: "networkidle" });
      await expect(page.getByTestId("web-site-title")).toHaveText("Kapadokya Taş Konak Otel");
      await expect(page.getByTestId("mobile-nav-toggle")).toBeVisible();
      await expect(page.getByTestId("home-story-image")).toHaveJSProperty(
        "complete",
        true,
      );
      await expect(page.getByTestId("home-experience-image")).toHaveJSProperty(
        "complete",
        true,
      );
      await page.evaluate(async () => {
        await Promise.all(
          Array.from(document.images).map((image) =>
            image.decode().catch(() => undefined),
          ),
        );
      });
      await capture(page, "0003", "AC-2-home-tr-mobile.png");
    },
  );

  test(
    "shouldKeepLocaleSwitchInPageContext",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      await mockRooms(page);
      await page.goto(`${WEB_BASE_URL}/tr/rooms`, { waitUntil: "networkidle" });
      await page.getByTestId("locale-switch-en").click();
      await expect(page).toHaveURL(/\/en\/rooms$/);

      await page.goto(`${WEB_BASE_URL}/tr/credits`, { waitUntil: "networkidle" });
      await page.getByTestId("locale-switch-en").click();
      await expect(page).toHaveURL(/\/en\/credits$/);
    },
  );

  test(
    "shouldKeepAllSectionTextVisibleWhenJavaScriptIsDisabled",
    { tag: ["@design", "@seeded"] },
    async ({ browser }) => {
      const context = await browser.newContext({
        javaScriptEnabled: false,
        viewport: { width: 1280, height: 800 },
      });
      const page = await context.newPage();
      await page.goto(`${WEB_BASE_URL}/tr`, { waitUntil: "networkidle" });

      await expect(page.getByTestId("home-story")).toBeVisible();
      await expect(page.getByText("Taşın Hafızası, Konuğun Huzuru")).toBeVisible();
      await expect(page.getByText("Kapadokya'yı Hissetmenin Dört Yolu")).toBeVisible();
      await expect(page.locator('[data-motion="pinned"]')).toHaveCount(0);
      await expect(page.getByTestId("home-hero-video")).toHaveCount(0);
      await page.mouse.wheel(0, 4200);
      await page.waitForTimeout(400);
      await page.mouse.wheel(0, -4200);
      await page.waitForTimeout(400);
      await capture(page, "0003", "AC-2-home-nojs.png");
      await context.close();
    },
  );

  test(
    "shouldNavigateAllDesignPagesWithoutApplicationConsoleErrors",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      await mockRooms(page);
      const console = collectConsole(page);
      for (const route of SIX_ROUTES) {
        await page.goto(`${WEB_BASE_URL}${route}`, { waitUntil: "networkidle" });
        expect(console.appErrors(), `console errors on ${route}`).toEqual([]);
        console.clear();
      }
    },
  );
});
