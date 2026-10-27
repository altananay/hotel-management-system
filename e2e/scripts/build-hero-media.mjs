import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import ffmpegPath from "ffmpeg-static";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, "../..");
const source = process.argv[2];
if (!source || !fs.existsSync(source)) {
  console.error("Usage: node e2e/scripts/build-hero-media.mjs <source-video>");
  process.exit(1);
}

const heroDir = path.join(repoRoot, "web", "public", "media", "hero");
const ogDir = path.join(repoRoot, "web", "public", "media", "og");
fs.mkdirSync(heroDir, { recursive: true });
fs.mkdirSync(ogDir, { recursive: true });

const CROP = "crop=2160:1215:0:1500,scale=1920:1080";
const GRADE =
  "eq=contrast=1.06:saturation=0.9:brightness=-0.025:gamma=1.02,colorbalance=rs=0.04:gs=0.015:bs=-0.05:rm=0.03:gm=0.01:bm=-0.04:rh=0.02:bh=-0.03";

function run(args) {
  execFileSync(ffmpegPath, ["-y", "-hide_banner", "-loglevel", "error", ...args], {
    stdio: "inherit",
  });
}

const poster = path.join(heroDir, "home-hero-poster.webp");
run([
  "-ss", "22",
  "-i", source,
  "-frames:v", "1",
  "-vf", `${CROP},${GRADE}`,
  "-c:v", "libwebp",
  "-quality", "80",
  poster,
]);

const webm = path.join(heroDir, "home-hero.webm");
run([
  "-ss", "14",
  "-t", "11",
  "-i", source,
  "-filter_complex",
  `${CROP},${GRADE},fps=30,split=3[body][tail][head];` +
    `[body]trim=start=1:end=10,setpts=PTS-STARTPTS[main];` +
    `[tail]trim=start=10:end=11,setpts=PTS-STARTPTS[tailpart];` +
    `[head]trim=start=0:end=1,setpts=PTS-STARTPTS[headpart];` +
    `[tailpart][headpart]blend=all_expr='A*(1-T)+B*T'[blend];` +
    `[main][blend]concat=n=2:v=1:a=0[out]`,
  "-map", "[out]",
  "-an",
  "-c:v", "libvpx-vp9",
  "-crf", "32",
  "-b:v", "0",
  "-row-mt", "1",
  "-cpu-used", "2",
  "-pix_fmt", "yuv420p",
  "-r", "30",
  webm,
]);

const mp4 = path.join(heroDir, "home-hero.mp4");
run([
  "-i", webm,
  "-an",
  "-c:v", "libx264",
  "-crf", "23",
  "-preset", "slow",
  "-pix_fmt", "yuv420p",
  "-movflags", "+faststart",
  "-r", "30",
  mp4,
]);

const og = path.join(ogDir, "home-og.jpg");
run([
  "-ss", "22",
  "-i", source,
  "-frames:v", "1",
  "-vf",
  `crop=2160:1134:0:1500,scale=1200:630,${GRADE}`,
  "-q:v", "2",
  og,
]);

for (const file of [poster, webm, mp4, og]) {
  console.log(`${path.relative(repoRoot, file)}: ${fs.statSync(file).size.toLocaleString()} B`);
}
