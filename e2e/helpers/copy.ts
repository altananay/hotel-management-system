import fs from "node:fs";
import path from "node:path";

const REPO_ROOT = path.resolve(__dirname, "../..");

export interface Dictionary {
  siteTitle: string;
  nav: { home: string; rooms: string; ariaLabel: string };
  skipToContent: string;
  home: {
    badge: string;
    heroTitle: string;
    heroDescription: string;
    exploreRoomsCta: string;
    scrollHint: string;
    story: { eyebrow: string; title: string; paragraph1: string; paragraph2: string; paragraph3: string };
    experience: {
      eyebrow: string;
      title: string;
      intro: string;
      cards: Record<string, { title: string; description: string }>;
    };
    roomsCta: { eyebrow: string; title: string; body: string; button: string };
    contact: {
      eyebrow: string;
      title: string;
      body: string;
      addressLabel: string;
      phoneLabel: string;
      emailLabel: string;
      address: string;
      phone: string;
      email: string;
    };
  };
  footer: { tagline: string; copyright: string; creditsLabel: string; contactLabel: string };
  credits: {
    title: string;
    intro: string;
    mediaHeading: string;
    typographyHeading: string;
    contentNoteHeading: string;
    contentNote: string;
    videoEntry: string;
    fontCormorant: string;
    fontSourceSans: string;
  };
  meta: Record<string, { title: string; description: string; ogAlt: string }>;
  rooms: Record<string, string>;
}

export function loadDictionary(locale: "tr" | "en"): Dictionary {
  const file = path.join(REPO_ROOT, "web", "src", "lib", "i18n", `${locale}.json`);
  return JSON.parse(fs.readFileSync(file, "utf8")) as Dictionary;
}
