import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";

function walk(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const full = path.join(directory, entry.name);
    return entry.isDirectory() ? walk(full) : [full];
  });
}

function manifest(directory) {
  const files = walk(directory)
    .map((file) => ({
      path: path.relative(directory, file).replaceAll("\\", "/"),
      sha256: crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex"),
      bytes: fs.statSync(file).size,
    }))
    .sort((a, b) => a.path.localeCompare(b.path));
  return { directory: path.resolve(directory), files };
}

function main() {
  const [command, directory, manifestPath, outputPath] = process.argv.slice(2);
  if (!command || !directory || !manifestPath) {
    console.error(
      "Usage: node scripts/sha256-manifest.mjs capture <dir> <manifest> | verify <dir> <manifest> <output>",
    );
    process.exit(1);
  }
  if (!fs.existsSync(directory)) {
    console.error(`Evidence directory not found: ${directory}`);
    process.exit(1);
  }

  if (command === "capture") {
    fs.writeFileSync(manifestPath, JSON.stringify(manifest(directory), null, 2), "utf8");
    console.log(`Captured ${manifest(directory).files.length} files to ${manifestPath}`);
    return;
  }

  if (command === "verify") {
    const expected = JSON.parse(fs.readFileSync(manifestPath, "utf8"));
    const actual = manifest(directory);
    const expectedMap = new Map(expected.files.map((file) => [file.path, file]));
    const actualMap = new Map(actual.files.map((file) => [file.path, file]));
    const changes = [];
    for (const [file, entry] of expectedMap) {
      const current = actualMap.get(file);
      if (!current) {
        changes.push(`missing: ${file}`);
      } else if (current.sha256 !== entry.sha256) {
        changes.push(`changed: ${file}`);
      }
    }
    for (const file of actualMap.keys()) {
      if (!expectedMap.has(file)) {
        changes.push(`added: ${file}`);
      }
    }

    const report = [
      "# 0002 evidence SHA-256 manifest",
      "",
      `Directory: ${path.resolve(directory)}`,
      `Files: ${actual.files.length}`,
      `Result: ${changes.length === 0 ? "UNCHANGED" : "CHANGED"}`,
      "",
      ...(changes.length === 0 ? ["All 0002 evidence files are byte-identical."] : changes),
    ].join("\n");
    if (outputPath) {
      fs.writeFileSync(outputPath, report, "utf8");
    }
    console.log(report);
    if (changes.length > 0) {
      process.exit(1);
    }
  }
}

main();
