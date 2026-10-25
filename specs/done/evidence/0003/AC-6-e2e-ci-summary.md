# 0003 E2E ve CI özeti

- Web unit testleri (Vitest): **39/39 passed**
- 0002 smoke (`@empty` 1 + `@seeded` 3): **4/4 passed**
  - 0002 kanıt kökü geçici dizin kullanıldı; arşiv değişmedi.
  - Bu turda `@seeded` **3/3** yeniden koşuldu; `@empty` CI'da temiz DB ile koşar.
- 0003 design (`@design @seeded`): **38/38 passed**
- Axe: altı rotada kritik/ciddi ihlal **0**
- JS bütçesi: **206.819 B / 256.000 B** (gzip-9)
- CLS (scroll): **0.0000**; Lighthouse CLS: **0.042**
- Lighthouse mobil (devtools throttling, 3 koşu median): **LCP 2316,31 ms**,
  performans **0.96**
- Video kanıtı: **3,9 sn / 468.529 B** (VP9, 1280×800)
- `0002` kanıt SHA-256: **UNCHANGED (12 dosya)**

## 2026-10-25 code review fixleri

- **CR-1:** `origin/integration` feature branch'e merge edildi (`0305043`); conflict'ler
  feature içeriği korunarak çözüldü. Push sonrası PR #11 `mergeable` doğrulanacak.
- **CR-3:** `appErrors()` kaynak-hatası filtresi kaldırıldı; tüm senaryolar ham
  `console.errors` ile doğrulanıyor. Font fallback testi Latin glyph'i olmayan
  geçerli bir WOFF2 fixture'ı (`e2e/fixtures/fonts/empty-latin.woff2`) sunar;
  video preflight 404 dalı ağ gürültüsü olmadan aynı `response.ok === false`
  koşulunu çalıştırır.
- **CR-4:** Parallax testi `window.__hotelMotionLoaded.gsap === true` ve
  `data-motion="pinned"` bekler; en az bir offsetin **0 < |offset| ≤ 40 px**
  olduğunu doğrular.
- **CR-5:** Dekoratif `paper-grain`, `hero-vignette`, `fine-grid` renkleri
  `color-mix()` + token'lara çevrildi (`--color-scrim` eklendi). Guard artık
  `@theme` dışındaki ham `rgb/hsl/hex` renkleri de tarar; kasıtlı ham renk
  enjeksiyonuyla self-test yapıldı ve guard FAIL verdi.
- **CR-6:** Tüm rotalara sızan `ReactDOM.preload` kaldırıldı; poster ana sayfa
  HTML'inde `srcSet`/`sizes` + `fetchPriority="high"` ile seçilir. E2E, 1280'de
  1280w, 390'da 960w isteğini ve rooms/credits'te poster isteği olmamasını
  doğrular.
- **CR-7:** `navigator.connection` `change` olayı dinlenir; save-data açılınca
  video sökülür, kapanınca yeniden değerlendirilir. Runtime E2E her iki yönü
  doğrular.

## Komutlar

```bash
# web
cd web && npm run lint && npm test && npm run build

# 0002 regression (geçici kanıt kökü)
cd e2e
E2E_EVIDENCE_DIR="$RUNNER_TEMP/evidence-0002" npx playwright test tests/smoke.spec.ts --grep @empty
# seed...
E2E_EVIDENCE_DIR="$RUNNER_TEMP/evidence-0002" npx playwright test tests/smoke.spec.ts --grep @seeded

# 0003 design
node scripts/sha256-manifest.mjs capture ../specs/done/evidence/0002 "$RUNNER_TEMP/0002-before.json"
npx playwright test tests/design --grep @seeded
node scripts/sha256-manifest.mjs verify ../specs/done/evidence/0002 "$RUNNER_TEMP/0002-before.json" ../specs/done/evidence/0003/AC-6-0002-evidence-sha256.txt
```

## CI

- CI run linki: PR açıldığında `CI Quality Gate` run'ı ile eklenecek.
- CI akışı: `@empty` → seed → `@seeded` smoke (geçici kanıt kökü) →
  SHA manifest → `@seeded` design → SHA doğrulama → 0003 kanıt artefaktı.
