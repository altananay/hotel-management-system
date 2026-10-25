import { expect, test } from "@playwright/test";
import { WEB_BASE_URL } from "../../helpers/config";
import { loadDictionary } from "../../helpers/copy";
import { capture } from "../../helpers/evidence";

test.describe("Design — credits (AC-2, AC-4, B-10)", () => {
  test(
    "shouldRenderCreditsFromTheSingleMarkdownSourceWithSharedChrome",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      for (const locale of ["tr", "en"] as const) {
        const dict = loadDictionary(locale);
        await page.emulateMedia({ reducedMotion: "reduce" });
        await page.goto(`${WEB_BASE_URL}/${locale}/credits`, {
          waitUntil: "networkidle",
        });

        await expect(page.getByTestId("credits-page")).toBeVisible();
        await expect(page.getByTestId("site-header")).toBeVisible();
        await expect(page.getByTestId("site-footer")).toBeVisible();
        await expect(page.locator("h1")).toHaveCount(1);
        await expect(page.locator("h1")).toHaveText(dict.credits.title);
        await expect(page.getByText(dict.credits.intro)).toBeVisible();
        await expect(
          page.getByRole("heading", { name: dict.credits.mediaHeading }),
        ).toBeVisible();
        await expect(
          page.getByRole("heading", { name: dict.credits.typographyHeading }),
        ).toBeVisible();
        await expect(
          page.getByRole("heading", { name: dict.credits.contentNoteHeading }),
        ).toBeVisible();
        await expect(page.getByText(dict.credits.contentNote)).toBeVisible();
        await expect(page.getByText(/Pexels, video 30653318/)).toBeVisible();
        await expect(page.getByText(/Cormorant Garamond/)).toBeVisible();
        await expect(page.getByText(/Source Sans 3/)).toBeVisible();
        await expect(page.getByTestId("footer-credits-link")).toHaveAttribute(
          "href",
          `/${locale}/credits`,
        );

        await capture(page, "0003", `AC-2-credits-${locale}-desktop.png`);
      }
    },
  );

  test(
    "shouldNavigateFromTheHomeFooterToCreditsAndPreserveLocale",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      await page.goto(`${WEB_BASE_URL}/tr`, { waitUntil: "networkidle" });
      await page.getByTestId("footer-credits-link").click();
      await expect(page).toHaveURL(/\/tr\/credits$/);
      await page.getByTestId("locale-switch-en").click();
      await expect(page).toHaveURL(/\/en\/credits$/);
    },
  );
});
