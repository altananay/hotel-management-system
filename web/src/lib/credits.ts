import fs from "node:fs";
import path from "node:path";

export interface CreditsSegment {
  type: "text" | "link";
  value: string;
  href?: string;
}

export interface CreditsEntry {
  text: string;
  segments: CreditsSegment[];
}

export interface CreditsContent {
  media: CreditsEntry[];
  typography: CreditsEntry[];
  contentNote: string[];
}

const REQUIRED_SECTIONS = ["Visual Media", "Typography", "Content Note"] as const;

function parseInline(text: string): CreditsSegment[] {
  const segments: CreditsSegment[] = [];
  const pattern = /\[([^\]]+)\]\((https?:\/\/[^)\s]+)\)/g;
  let cursor = 0;
  let match: RegExpExecArray | null;

  while ((match = pattern.exec(text)) !== null) {
    if (match.index > cursor) {
      segments.push({ type: "text", value: text.slice(cursor, match.index) });
    }
    segments.push({ type: "link", value: match[1], href: match[2] });
    cursor = match.index + match[0].length;
  }

  if (cursor < text.length) {
    segments.push({ type: "text", value: text.slice(cursor) });
  }

  return segments.length > 0 ? segments : [{ type: "text", value: text }];
}

function stripInlineMarkdown(text: string): string {
  return text.replace(/\[([^\]]+)\]\((https?:\/\/[^)\s]+)\)/g, "$1");
}

export function parseCreditsMarkdown(markdown: string): CreditsContent {
  const lines = markdown.split(/\r?\n/);
  const sections = new Map<string, string[]>();
  let current: string | null = null;

  for (const line of lines) {
    const heading = /^##\s+(.+?)\s*$/.exec(line);
    if (heading) {
      current = heading[1].trim();
      sections.set(current, []);
      continue;
    }
    if (current) {
      sections.get(current)?.push(line);
    }
  }

  for (const required of REQUIRED_SECTIONS) {
    if (!sections.has(required)) {
      throw new Error(`CREDITS.md is missing the "${required}" section`);
    }
  }

  const media = (sections.get("Visual Media") ?? [])
    .filter((line) => /^\s*-\s+/.test(line))
    .map((line) => {
      const raw = line.replace(/^\s*-\s+/, "").trim();
      const text = stripInlineMarkdown(raw);
      return { text, segments: parseInline(raw) };
    });

  const typography = (sections.get("Typography") ?? [])
    .filter((line) => /^\s*-\s+/.test(line))
    .map((line) => {
      const raw = line.replace(/^\s*-\s+/, "").trim();
      const text = stripInlineMarkdown(raw);
      return { text, segments: parseInline(raw) };
    });

  const contentNote = (sections.get("Content Note") ?? [])
    .map((line) => line.trim())
    .filter((line) => line.length > 0 && !line.startsWith("-"));

  if (media.length === 0 || typography.length === 0 || contentNote.length === 0) {
    throw new Error("CREDITS.md must contain media, typography and content note entries");
  }

  return { media, typography, contentNote };
}

export function readCreditsFile(filePath?: string): string {
  const resolved =
    filePath ?? path.resolve(process.cwd(), "..", "CREDITS.md");
  if (!fs.existsSync(resolved)) {
    throw new Error(`CREDITS.md not found at ${resolved}`);
  }
  return fs.readFileSync(resolved, "utf8");
}

export function getCreditsContent(filePath?: string): CreditsContent {
  return parseCreditsMarkdown(readCreditsFile(filePath));
}
