import fs from "node:fs";
import path from "node:path";
import type { Page } from "@playwright/test";

const REPO_ROOT = path.resolve(__dirname, "../..");
const DEFAULT_ROOT = path.join(REPO_ROOT, "specs", "done", "evidence");

export function evidenceRoot(): string {
  const configured = process.env.E2E_EVIDENCE_DIR;
  return configured ? path.resolve(REPO_ROOT, configured) : DEFAULT_ROOT;
}

export function evidenceDir(spec: string): string {
  const dir = path.join(evidenceRoot(), spec);
  fs.mkdirSync(dir, { recursive: true });
  return dir;
}

export function evidencePath(spec: string, name: string): string {
  return path.join(evidenceDir(spec), name);
}

export function writeEvidence(spec: string, name: string, content: string): string {
  const target = evidencePath(spec, name);
  fs.writeFileSync(target, content, "utf8");
  return target;
}

export async function capture(
  page: Page,
  spec: string,
  name: string,
  options: { fullPage?: boolean } = {},
): Promise<string> {
  const target = evidencePath(spec, name);
  await page.screenshot({ path: target, fullPage: options.fullPage ?? true });
  return target;
}

export async function revealWholePage(page: Page): Promise<void> {
  await page.emulateMedia({ reducedMotion: "reduce" });
  await page.waitForTimeout(300);
}
