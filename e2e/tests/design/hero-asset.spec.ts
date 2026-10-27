import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { expect, test } from "@playwright/test";
// @ts-expect-error - ffprobe-static ships without type declarations.
import ffprobeStatic from "ffprobe-static";
import { writeEvidence } from "../../helpers/evidence";

const REPO_ROOT = path.resolve(__dirname, "../../..");
const ffprobePath = (ffprobeStatic as { path: string }).path;

interface Probe {
  streams: Array<{
    codec_type: string;
    codec_name: string;
    width?: number;
    height?: number;
    avg_frame_rate?: string;
  }>;
  format: { duration?: string; size: string };
}

function probe(file: string): Probe {
  const output = execFileSync(
    ffprobePath,
    [
      "-v",
      "error",
      "-show_streams",
      "-show_format",
      "-of",
      "json",
      file,
    ],
    { encoding: "utf8" },
  );
  return JSON.parse(output) as Probe;
}

test.describe("Design — hero asset contract (AC-2, AC-5, B-05)", () => {
  test(
    "shouldProveVideoCodecResolutionDurationFrameRateAndSilence",
    { tag: ["@design", "@seeded"] },
    async () => {
      const heroDir = path.join(REPO_ROOT, "web", "public", "media", "hero");
      const files = {
        webm: path.join(heroDir, "home-hero.webm"),
        mp4: path.join(heroDir, "home-hero.mp4"),
        poster: path.join(heroDir, "home-hero-poster.webp"),
        og: path.join(REPO_ROOT, "web", "public", "media", "og", "home-og.jpg"),
      };

      const webm = probe(files.webm);
      const mp4 = probe(files.mp4);
      const poster = probe(files.poster);
      const og = probe(files.og);

      const videoStream = (result: Probe) =>
        result.streams.find((stream) => stream.codec_type === "video");
      const audioStreams = (result: Probe) =>
        result.streams.filter((stream) => stream.codec_type === "audio");

      expect(videoStream(webm)).toMatchObject({
        codec_name: "vp9",
        width: 1920,
        height: 1080,
        avg_frame_rate: "30/1",
      });
      expect(videoStream(mp4)).toMatchObject({
        codec_name: "h264",
        width: 1920,
        height: 1080,
        avg_frame_rate: "30/1",
      });
      expect(audioStreams(webm)).toHaveLength(0);
      expect(audioStreams(mp4)).toHaveLength(0);
      expect(Number(webm.format.duration)).toBeGreaterThanOrEqual(8);
      expect(Number(webm.format.duration)).toBeLessThanOrEqual(12);
      expect(Number(mp4.format.duration)).toBeGreaterThanOrEqual(8);
      expect(Number(mp4.format.duration)).toBeLessThanOrEqual(12);

      expect(fs.statSync(files.webm).size).toBeLessThanOrEqual(8_388_608);
      expect(fs.statSync(files.mp4).size).toBeLessThanOrEqual(8_388_608);
      expect(fs.statSync(files.poster).size).toBeLessThanOrEqual(307_200);
      expect(fs.statSync(files.og).size).toBeLessThanOrEqual(307_200);

      expect(videoStream(poster)).toMatchObject({ width: 1920, height: 1080 });
      expect(videoStream(og)).toMatchObject({ width: 1200, height: 630 });

      writeEvidence(
        "0003",
        "AC-2-hero-ffprobe.log",
        JSON.stringify({ webm, mp4, poster, og }, null, 2),
      );
    },
  );
});
