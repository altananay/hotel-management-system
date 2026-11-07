import assert from "node:assert/strict";
import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import { verifyManifest } from "./sha256-manifest.mjs";

function sha256(file) {
  return crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex");
}

function writeTextManifest(root, sections) {
  const lines = ["# test archive manifest", ""];
  for (const section of sections) {
    const files = fs.readdirSync(path.join(root, section)).sort();
    lines.push(`## ${section} (${files.length} files)`);
    for (const file of files) {
      lines.push(`${sha256(path.join(root, section, file))}  ${file}`);
    }
    lines.push("");
  }
  const manifestPath = path.join(root, "manifest.txt");
  fs.writeFileSync(manifestPath, lines.join("\n"), "utf8");
  return manifestPath;
}

function fixture() {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "sha256-manifest-"));
  fs.mkdirSync(path.join(root, "0001"));
  fs.mkdirSync(path.join(root, "0002"));
  fs.mkdirSync(path.join(root, "0005"));
  fs.writeFileSync(path.join(root, "0001", "a.txt"), "a");
  fs.writeFileSync(path.join(root, "0002", "b.txt"), "b");
  fs.writeFileSync(path.join(root, "0005", "current.txt"), "current");
  return root;
}

test("text manifest is UNCHANGED when only listed archive files exist", () => {
  const root = fixture();
  const manifestPath = writeTextManifest(root, ["0001", "0002"]);

  const result = verifyManifest(root, manifestPath);

  assert.equal(result.changes.length, 0);
  assert.match(result.report, /Listed files: 2/);
  assert.match(result.report, /Result: UNCHANGED/);
});

test("text manifest reports files added inside a listed archive folder", () => {
  const root = fixture();
  const manifestPath = writeTextManifest(root, ["0001", "0002"]);
  fs.writeFileSync(path.join(root, "0001", "new.png"), "new");

  const result = verifyManifest(root, manifestPath);

  assert.deepEqual(result.changes, ["added: 0001/new.png"]);
  assert.match(result.report, /Result: CHANGED/);
});

test("text manifest ignores added files in spec folders it does not list", () => {
  const root = fixture();
  const manifestPath = writeTextManifest(root, ["0001", "0002"]);
  fs.writeFileSync(path.join(root, "0005", "new.png"), "new");

  const result = verifyManifest(root, manifestPath);

  assert.equal(result.changes.length, 0);
});

test("text manifest still reports missing and changed files", () => {
  const root = fixture();
  const manifestPath = writeTextManifest(root, ["0001", "0002"]);
  fs.writeFileSync(path.join(root, "0001", "a.txt"), "changed");
  fs.rmSync(path.join(root, "0002", "b.txt"));

  const result = verifyManifest(root, manifestPath);

  assert.deepEqual(result.changes.sort(), [
    "changed: 0001/a.txt",
    "missing: 0002/b.txt",
  ]);
});
