// @vitest-environment node
import path from "node:path";
import { describe, expect, it } from "vitest";
import {
  getCreditsContent,
  parseCreditsMarkdown,
  readCreditsFile,
} from "@/lib/credits";
import { getDictionary } from "@/lib/i18n";

const SAMPLE = `# Credits
## Visual Media
- Hero video — Pexels, video 30653318; Pexels License. Source: [Pexels](https://www.pexels.com/video/30653318/).
## Typography
- Cormorant Garamond — SIL Open Font License 1.1.
## Content Note
Contact details are placeholders.
`;

describe("CREDITS.md single source (AC-1, AC-2, B-10, B-13)", () => {
  it("shouldParseMediaTypographyAndContentNote", () => {
    const parsed = parseCreditsMarkdown(SAMPLE);
    expect(parsed.media[0].text).toContain("Pexels, video 30653318");
    expect(parsed.media[0].segments.some((segment) => segment.type === "link")).toBe(true);
    expect(parsed.typography[0].text).toContain("SIL Open Font License 1.1");
    expect(parsed.contentNote).toEqual(["Contact details are placeholders."]);
  });

  it("shouldFailFastWhenASectionIsMissing", () => {
    expect(() => parseCreditsMarkdown("## Visual Media\n- x")).toThrow(/Typography/);
  });

  it("shouldFailFastWhenTheFileIsMissing", () => {
    expect(() => readCreditsFile(path.join(process.cwd(), "missing-credits.md"))).toThrow(
      /CREDITS.md not found/,
    );
  });

  it("shouldExposeTheApprovedCopyDeckEntriesFromTheRealFile", () => {
    const credits = getCreditsContent();
    const tr = getDictionary("tr");
    const en = getDictionary("en");

    expect(credits.media.some((entry) => entry.text.includes("30653318"))).toBe(true);
    expect(credits.typography.some((entry) => entry.text.includes("Cormorant Garamond"))).toBe(true);
    expect(credits.typography.some((entry) => entry.text.includes("Source Sans 3"))).toBe(true);
    expect(tr.credits.videoEntry).toContain("Pexels License");
    expect(en.credits.videoEntry).toContain("Pexels License");
    expect(credits.typography.some((entry) => entry.text.includes("SIL Open Font License 1.1"))).toBe(true);
  });
});
