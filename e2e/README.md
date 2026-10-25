# UÃ§tan Uca (E2E) Testleri (`e2e/`)

Bu dizin, `web` (`:3000`) public sitesinin ve `admin-web` (`:3001`) uygulamasÄ±nÄ±n
aÃ§Ä±lÄ±ÅŸÄ±nÄ±, TR/EN dil geÃ§iÅŸini, zorunlu ekran durumlarÄ±nÄ± ve `0003` tasarÄ±m
deneyimini Playwright ile doÄŸrular.

## KanÄ±t kÃ¶kÃ¼ ve izolasyon

- `E2E_EVIDENCE_DIR` kanÄ±t kÃ¶kÃ¼nÃ¼ belirler. VarsayÄ±lan: `specs/done/evidence`.
- Ortak helper her spec'i kendi alt dizinine yazar:
  - `0002` smoke testleri â†’ `<kÃ¶k>/0002/`
  - `0003` design testleri â†’ `<kÃ¶k>/0003/`
- `0002` arÅŸivi deÄŸiÅŸtirilmez. `0002` regresyon koÅŸumunu **geÃ§ici bir kÃ¶kle**
  Ã§alÄ±ÅŸtÄ±rÄ±n; bÃ¶ylece arÅŸivdeki PNG'ler yeniden yazÄ±lmaz:

  ```bash
  E2E_EVIDENCE_DIR=$RUNNER_TEMP/evidence-0002 npx playwright test tests/smoke.spec.ts --grep @empty
  # seed...
  E2E_EVIDENCE_DIR=$RUNNER_TEMP/evidence-0002 npx playwright test tests/smoke.spec.ts --grep @seeded
  ```

- `0003` design testleri varsayÄ±lan kÃ¶ke yazar. `0002` izolasyonu SHA-256
  manifestiyle kanÄ±tlanÄ±r:

  ```bash
  node scripts/sha256-manifest.mjs capture ../specs/done/evidence/0002 "$RUNNER_TEMP/0002-before.json"
  npx playwright test tests/design --grep @seeded
  node scripts/sha256-manifest.mjs verify ../specs/done/evidence/0002 "$RUNNER_TEMP/0002-before.json" ../specs/done/evidence/0003/AC-6-0002-evidence-sha256.txt
  ```

## Etiketler

- `@empty` â€” seed Ã¶ncesi gerÃ§ek boÅŸ vitrin (0002).
- `@seeded` â€” seed sonrasÄ± smoke + design testleri.
- `@design` â€” 0003 ana sayfa, hero, scroll, a11y, metadata, credits, rooms,
  runtime, budget ve video kanÄ±tlarÄ±.

CI `--grep @seeded` ile hem `0002` smoke hem `0003` design testlerini koÅŸar.

## Viewport ve tercihler

- Mobil: `390Ã—844`
- Desktop: `1280Ã—800`
- Tablet sÄ±nÄ±rlarÄ±: `767`, `768`, `1023`, `1024`
- `prefers-reduced-motion: reduce`: Playwright `reducedMotion` / `emulateMedia`.
- `save-data`: `context.addInitScript` ile `navigator.connection.saveData = true`.
- JavaScript kapalÄ±: `browser.newContext({ javaScriptEnabled: false })`.

## Design testleri

```bash
cd e2e
npx playwright test tests/design --grep @seeded
```

Kapsam:

- `home.spec.ts` â€” ortak chrome, copy deck, locale, JS kapalÄ±, konsol.
- `hero.spec.ts` â€” poster/video, viewport, reduced-motion, save-data, fallback.
- `hero-asset.spec.ts` â€” ffprobe ile codec/Ã§Ã¶zÃ¼nÃ¼rlÃ¼k/sÃ¼re/fps/sessizlik.
- `scroll-story.spec.ts` â€” tam 2 pin, 1023/1024 sÄ±nÄ±rÄ±, parallax, scroll kanÄ±tÄ±.
- `scroll-story-video.spec.ts` â€” `.webm` video kanÄ±tÄ± (â‰¤15 sn, â‰¤5 MB).
- `a11y.spec.ts` â€” skip link, klavye, mobil menÃ¼, landmark, axe.
- `metadata.spec.ts` â€” title/description/canonical/hreflang/OG.
- `credits.spec.ts` â€” `CREDITS.md` kaynaklÄ± sayfa.
- `rooms-design.spec.ts` â€” 0002 sÃ¶zleÅŸmesi, Ã¼Ã§ durum, hata matrisi, font fallback.
- `runtime.spec.ts` â€” Ã¼Ã§Ã¼ncÃ¼ taraf istek yokluÄŸu, motion kÃ¼tÃ¼phanesi yÃ¼kleme.
- `budget.spec.ts` â€” JS gzip-9, asset bÃ¼tÃ§eleri, scroll CLS.

## Video kanÄ±tÄ±

`scroll-story-video.spec.ts` Playwright `video` kaydÄ±nÄ± 1280Ã—800'de alÄ±r ve
`ffmpeg-static` ile VP9 `.webm`'e dÃ¶nÃ¼ÅŸtÃ¼rÃ¼r. Ã‡Ä±ktÄ±
`specs/done/evidence/0003/AC-2-home-scroll-story.webm` olur; sÃ¼re â‰¤15 sn,
boyut â‰¤5.242.880 B'dir. `ffprobe-static` sÃ¼re/akÄ±ÅŸ kontrolÃ¼nde kullanÄ±lÄ±r.

## Lighthouse

`0003` iÃ§in Lighthouse CI gate'i yoktur. KanÄ±t Ã¼retimi:

```bash
cd e2e
CHROME_PATH="$(node -e "console.log(require('playwright').chromium.executablePath())")"
npx lighthouse@10.4.0 http://localhost:3000/tr \
  --only-categories=performance --form-factor=mobile --screenEmulation.mobile \
  --throttling-method=devtools --output=json --output=html \
  --chrome-path="$CHROME_PATH" --chrome-flags="--headless --no-sandbox"
```

3 koÅŸunun median LCP/CLS deÄŸeri `AC-5-lighthouse-mobile.md` ve
`AC-5-lighthouse-mobile.html` olarak saklanÄ±r.

## Ä°ki aÅŸamalÄ± 0002 akÄ±ÅŸÄ±

AyrÄ±ntÄ±lÄ± altyapÄ±, migration, seed ve backend/frontend baÅŸlatma adÄ±mlarÄ± iÃ§in
Ã¶nceki bÃ¶lÃ¼mdeki komutlar geÃ§erlidir:

1. AltyapÄ± + migration (seed'siz)
2. Backend/frontend build + start
3. `@empty` (geÃ§ici kanÄ±t kÃ¶kÃ¼)
4. Seed (`-Pseed -Dseed.env=local|ci`)
5. `@seeded` smoke (geÃ§ici kanÄ±t kÃ¶kÃ¼)
6. `@seeded` design (varsayÄ±lan kanÄ±t kÃ¶kÃ¼)
7. SHA-256 manifest doÄŸrulamasÄ±
