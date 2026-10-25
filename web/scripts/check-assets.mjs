import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const webRoot = path.resolve(__dirname, "..");
const repoRoot = path.resolve(webRoot, "..");

const FONT_BUDGET = 204_800;
const VIDEO_BUDGET = 8_388_608;
const POSTER_BUDGET = 307_200;
const OG_BUDGET = 307_200;

const FONT_FILES = [
  "cormorant-garamond-600.woff2",
  "source-sans-3-400.woff2",
  "source-sans-3-600.woff2",
];

function fail(message) {
  console.error(`FAIL: ${message}`);
  process.exitCode = 1;
}

function assertWithinBudget(size, limit, label) {
  if (size > limit) {
    throw new Error(`${label} exceeds its budget by ${size - limit} B`);
  }
}

function checkFile(filePath, maxBytes, label) {
  if (!fs.existsSync(filePath)) {
    fail(`${label} is missing: ${path.relative(repoRoot, filePath)}`);
    return;
  }
  const size = fs.statSync(filePath).size;
  const suffix = maxBytes ? ` / limit ${maxBytes.toLocaleString()} B` : "";
  console.log(`${label}: ${size.toLocaleString()} B${suffix}`);
  if (maxBytes) {
    try {
      assertWithinBudget(size, maxBytes, label);
    } catch (error) {
      fail(error.message);
    }
  }
}

function main() {
  const fontsDir = path.join(webRoot, "public", "fonts");
  const heroDir = path.join(webRoot, "public", "media", "hero");
  const ogDir = path.join(webRoot, "public", "media", "og");

  const woff2 = fs.existsSync(fontsDir)
    ? fs.readdirSync(fontsDir).filter((file) => file.endsWith(".woff2")).sort()
    : [];
  if (woff2.length !== 3 || !FONT_FILES.every((file) => woff2.includes(file))) {
    fail(`expected exactly 3 font files (${FONT_FILES.join(", ")}), found ${woff2.join(", ") || "none"}`);
  }

  let fontTotal = 0;
  for (const file of FONT_FILES) {
    const filePath = path.join(fontsDir, file);
    if (!fs.existsSync(filePath)) {
      fail(`font is missing: ${file}`);
      continue;
    }
    const size = fs.statSync(filePath).size;
    fontTotal += size;
    console.log(`font ${file}: ${size.toLocaleString()} B`);
  }
  console.log(`font total: ${fontTotal.toLocaleString()} B / limit ${FONT_BUDGET.toLocaleString()} B`);
  try {
    assertWithinBudget(fontTotal, FONT_BUDGET, "font total");
  } catch (error) {
    fail(error.message);
  }

  checkFile(path.join(fontsDir, "OFL-CormorantGaramond.txt"), 0, "licence Cormorant Garamond");
  checkFile(path.join(fontsDir, "OFL-SourceSans3.txt"), 0, "licence Source Sans 3");

  checkFile(path.join(heroDir, "home-hero.webm"), VIDEO_BUDGET, "hero WebM");
  checkFile(path.join(heroDir, "home-hero.mp4"), VIDEO_BUDGET, "hero MP4");
  checkFile(path.join(heroDir, "home-hero-poster.webp"), POSTER_BUDGET, "hero poster");
  checkFile(
    path.join(heroDir, "home-hero-poster-1280.webp"),
    POSTER_BUDGET,
    "hero poster 1280",
  );
  checkFile(
    path.join(heroDir, "home-hero-poster-960.webp"),
    POSTER_BUDGET,
    "hero poster 960",
  );
  checkFile(path.join(ogDir, "home-og.jpg"), OG_BUDGET, "OG image");

  const creditsPath = path.join(repoRoot, "CREDITS.md");
  if (!fs.existsSync(creditsPath)) {
    fail("CREDITS.md is missing");
  } else {
    const credits = fs.readFileSync(creditsPath, "utf8");
    for (const heading of ["## Visual Media", "## Typography", "## Content Note"]) {
      if (!credits.includes(heading)) {
        fail(`CREDITS.md is missing ${heading}`);
      }
    }
    console.log("CREDITS.md: present");
  }

  if (process.argv.includes("--self-test")) {
    try {
      assertWithinBudget(FONT_BUDGET + 1, FONT_BUDGET, "synthetic oversized font");
      fail("self-test did not reject an oversized asset");
    } catch (error) {
      console.log(`SELF-TEST PASS: ${error.message}`);
    }
  }

  if (process.exitCode) {
    console.error("Asset budget check failed.");
  } else {
    console.log("Asset budget check passed.");
  }
}

main();
