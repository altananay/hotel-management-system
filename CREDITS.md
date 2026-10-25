# Credits & Licenses

This file is the single source of truth for the visual media and typography
licences used by the public web application. The `/[locale]/credits` page is
generated from the sections below at build time.

## Visual Media

- Hero video — Pexels, video 30653318; Pexels License. Source: [Pexels video 30653318](https://www.pexels.com/video/30653318/), accessed 2026-10-25.
- Hero poster — a still frame from the hero video; 1920×1080 WebP q80.
- Open Graph image — a still frame from the hero video; 1200×630 JPEG, no text.
- Pexels License — [https://www.pexels.com/license/](https://www.pexels.com/license/).

Transcode notes: the source is downloaded from the Pexels download endpoint,
trimmed to an 8–12 second loop, scaled/cropped to 1920×1080, encoded as VP9
(WebM, CRF 32) and H.264 (MP4, CRF 23, yuv420p, faststart), 30 fps, with the
audio stream removed (`-an`). The poster and OG image are extracted from the
same source. The source file is not committed; only the transcoded derivatives
are stored in `web/public/media/hero/`.

## Typography

- Cormorant Garamond — SIL Open Font License 1.1. Source: [Google Fonts](https://fonts.google.com/specimen/Cormorant+Garamond). Weight: 600.
- Source Sans 3 — SIL Open Font License 1.1. Source: [Google Fonts](https://fonts.google.com/specimen/Source+Sans+3). Weights: 400, 600.
- Licence texts: `web/public/fonts/OFL-CormorantGaramond.txt` and `web/public/fonts/OFL-SourceSans3.txt`.

The fonts are self-hosted as three Latin Extended WOFF2 files under
`web/public/fonts/`. No third-party font request is made at runtime.

## Content Note

Contact details are placeholders and must be replaced with the real address, phone, and email.
