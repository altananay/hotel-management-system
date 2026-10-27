// @vitest-environment node
import fs from "node:fs";
import path from "node:path";
import { describe, expect, it } from "vitest";

const webRoot = process.cwd();
const repoRoot = path.resolve(webRoot, "..");

function read(file: string): string {
  return fs.readFileSync(path.join(repoRoot, file), "utf8");
}

function themeTokens(): string[] {
  const css = read("web/src/app/globals.css");
  const theme = /@theme\s*\{([\s\S]*?)\n\}/.exec(css)?.[1] ?? "";
  return [...theme.matchAll(/^\s*--([a-z0-9-]+):/gm)]
    .map((match) => match[1])
    .filter((name) => !name.endsWith("--line-height") && !name.endsWith("--letter-spacing"));
}

describe("design system contract (AC-1)", () => {
  it("shouldKeepOneThemeBlockAndDocumentEverySemanticToken", () => {
    const css = read("web/src/app/globals.css");
    expect((css.match(/@theme\s*\{/g) ?? []).length).toBe(1);

    const docs = read("docs/frontend.md");
    expect(docs).not.toContain("İSKELET");
    for (const token of themeTokens()) {
      expect(docs).toContain(`--${token}`);
    }
  });

  it("shouldDocumentTheCompleteComponentInventory", () => {
    const docs = read("docs/frontend.md");
    for (const component of [
      "Header",
      "MobileNav",
      "Footer",
      "Button",
      "Card",
      "Section",
      "Badge",
      "RoomCard",
      "StatePanel",
      "Skeleton",
    ]) {
      expect(docs).toContain(component);
    }
  });

  it("shouldKeepThreeSelfHostedFontsWithinThe200KBBudget", () => {
    const fontsDir = path.join(webRoot, "public", "fonts");
    const fonts = fs.readdirSync(fontsDir).filter((file) => file.endsWith(".woff2"));
    expect(fonts).toHaveLength(3);
    const total = fonts.reduce(
      (sum, file) => sum + fs.statSync(path.join(fontsDir, file)).size,
      0,
    );
    expect(total).toBeLessThanOrEqual(204_800);
  });
});
