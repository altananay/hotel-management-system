import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const webRoot = path.resolve(__dirname, "..");
const repoRoot = path.resolve(webRoot, "..");

function fail(message) {
  console.error(`FAIL: ${message}`);
  process.exitCode = 1;
}

function walk(directory) {
  if (!fs.existsSync(directory)) {
    return [];
  }
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const full = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      return walk(full);
    }
    return [full];
  });
}

function main() {
  const cssPath = path.join(webRoot, "src", "app", "globals.css");
  const docsPath = path.join(repoRoot, "docs", "frontend.md");
  if (!fs.existsSync(cssPath) || !fs.existsSync(docsPath)) {
    fail("globals.css or docs/frontend.md is missing");
    return;
  }

  const css = fs.readFileSync(cssPath, "utf8");
  const theme = /@theme\s*\{([\s\S]*?)\n\}/.exec(css)?.[1] ?? "";
  const tokens = [...theme.matchAll(/^\s*--([a-z0-9-]+):/gm)]
    .map((match) => match[1])
    .filter((name) => !name.endsWith("--line-height") && !name.endsWith("--letter-spacing"));

  const docs = fs.readFileSync(docsPath, "utf8");
  if (docs.includes("İSKELET")) {
    fail("docs/frontend.md still contains İSKELET");
  }

  for (const token of tokens) {
    if (!docs.includes(`--${token}`)) {
      fail(`token --${token} is not documented in docs/frontend.md`);
    }
  }
  console.log(`tokens: ${tokens.length} checked`);

  for (const heading of [
    "Tasarım Token'ları",
    "Bileşen Envanteri",
    "Erişilebilirlik Kontrol Listesi",
    "Animasyon ve Video Rehberi",
    "Performans Bütçesi",
    "Kalite Kapıları",
  ]) {
    if (!docs.includes(heading)) {
      fail(`docs/frontend.md is missing the "${heading}" section`);
    }
  }

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
    if (!docs.includes(component)) {
      fail(`docs/frontend.md is missing the ${component} inventory entry`);
    }
  }

  const sourceRoots = [
    path.join(webRoot, "src", "app"),
    path.join(webRoot, "src", "components"),
    path.join(webRoot, "src", "features"),
  ];
  const files = sourceRoots.flatMap((root) => walk(root)).filter((file) => /\.(tsx|ts)$/.test(file));

  const palettePattern =
    /(?:bg|text|border|from|via|to|fill|stroke|ring|outline|decoration|shadow)-(?:stone|amber|red|zinc|slate|gray|neutral|orange|yellow|green|blue|indigo|purple|pink|rose|white|black)(?:[/\s"'\]]|$)/;

  for (const file of files) {
    if (file.includes("__tests__") || file.endsWith(".test.tsx") || file.endsWith(".test.ts")) {
      continue;
    }
    const content = fs.readFileSync(file, "utf8");
    const relative = path.relative(repoRoot, file);
    content.split(/\r?\n/).forEach((line, index) => {
      if (palettePattern.test(line)) {
        fail(`${relative}:${index + 1} uses a raw palette class: ${line.trim()}`);
      }
      if (/transition-all|hover:-translate|hover:scale|hover:rotate/.test(line)) {
        fail(`${relative}:${index + 1} uses a forbidden CSS transition/transform: ${line.trim()}`);
      }
    });
  }

  const roomCardPath = path.join(webRoot, "src", "components", "ui", "room-card.tsx");
  if (fs.existsSync(roomCardPath)) {
    const roomCard = fs.readFileSync(roomCardPath, "utf8");
    if (roomCard.includes("cursor-pointer") || roomCard.includes("onClick")) {
      fail("RoomCard must not be interactive or show a pointer cursor");
    }
  }

  if (process.exitCode) {
    console.error("Design system check failed.");
  } else {
    console.log("Design system check passed.");
  }
}

main();
