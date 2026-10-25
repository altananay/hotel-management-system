import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { expect, test } from "@playwright/test";
import ffmpegPath from "ffmpeg-static";
// @ts-expect-error - ffprobe-static ships without type declarations.
import ffprobeStatic from "ffprobe-static";
import { WEB_BASE_URL } from "../../helpers/config";
import { evidencePath } from "../../helpers/evidence";

const ffprobePath = (ffprobeStatic as { path: string }).path;

test.use({ video: { mode: "on", size: { width: 1280, height: 800 } } });

test.describe("Design — scroll story video evidence (AC-6)", () => {
  test(
    "shouldRecordTheDesktopScrollStoryWithin15SecondsAnd5MB",
    { tag: ["@design", "@seeded"] },
    async ({ page }) => {
      await page.setViewportSize({ width: 1280, height: 800 });
      await page.goto(`${WEB_BASE_URL}/tr`, { waitUntil: "networkidle" });
      await page.waitForTimeout(1500);
      for (let index = 0; index < 12; index += 1) {
        await page.mouse.wheel(0, 400);
        await page.waitForTimeout(700);
      }

      const video = page.video();
      expect(video).not.toBeNull();
      await page.close();
      const rawPath = await video!.path();

      const output = evidencePath("0003", "AC-2-home-scroll-story.webm");
      execFileSync(
        ffmpegPath as string,
        [
          "-y",
          "-hide_banner",
          "-loglevel",
          "error",
          "-i",
          rawPath,
          "-t",
          "15",
          "-an",
          "-c:v",
          "libvpx-vp9",
          "-crf",
          "35",
          "-b:v",
          "0",
          "-row-mt",
          "1",
          "-cpu-used",
          "2",
          "-pix_fmt",
          "yuv420p",
          output,
        ],
        { stdio: "inherit" },
      );

      const bytes = fs.statSync(output).size;
      expect(bytes).toBeLessThanOrEqual(5_242_880);
      const probe = execFileSync(
        ffprobePath,
        [
          "-v",
          "error",
          "-show_entries",
          "format=duration,size:stream=codec_type",
          "-of",
          "json",
          output,
        ],
        { encoding: "utf8" },
      );
      const parsed = JSON.parse(probe) as {
        format: { duration: string; size: string };
        streams: Array<{ codec_type: string }>;
      };
      expect(Number(parsed.format.duration)).toBeLessThanOrEqual(15);
      expect(parsed.streams.every((stream) => stream.codec_type === "video")).toBe(true);
    },
  );
});
