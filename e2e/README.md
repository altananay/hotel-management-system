# Uçtan Uca (E2E) Testleri (`e2e/`)

Bu dizin, `web` (`:3000`) public sitesinin ve `admin-web` (`:3001`) uygulamasının
açılışını, TR/EN dil geçişini, zorunlu ekran durumlarını ve `0003` tasarım
deneyimini Playwright ile doğrular.

## Kanıt kökü ve izolasyon

- `E2E_EVIDENCE_DIR` kanıt kökünü belirler. Varsayılan: `specs/done/evidence`.
- Ortak helper her spec'i kendi alt dizinine yazar:
  - `0002` smoke testleri → `<kök>/0002/`
  - `0003` design testleri → `<kök>/0003/`
- CI, eski spec'lerin arşivlerini korumak için tüm E2E kanıtlarını
  `$RUNNER_TEMP/e2e-evidence` altına yazar; `0001–0005` üzerinde `git diff`
  ve untracked kontrolü bu sayede anlamlı kalır.
- `0002` arşivi değiştirilmez. `0002` regresyon koşumunu **geçici bir kökle**
  çalıştırın; böylece arşivdeki PNG'ler yeniden yazılmaz:

  ```bash
  E2E_EVIDENCE_DIR=$RUNNER_TEMP/evidence-0002 npx playwright test tests/smoke.spec.ts --grep @empty
  # seed...
  E2E_EVIDENCE_DIR=$RUNNER_TEMP/evidence-0002 npx playwright test tests/smoke.spec.ts --grep @seeded
  ```

- `0003` design testleri yerelde varsayılan köke, CI'da geçici köke yazar.
  `0002` izolasyonu SHA-256 manifestiyle kanıtlanır:

  ```bash
  node scripts/sha256-manifest.mjs capture ../specs/done/evidence/0002 "$RUNNER_TEMP/0002-before.json"
  npx playwright test tests/design --grep @seeded
  node scripts/sha256-manifest.mjs verify ../specs/done/evidence/0002 "$RUNNER_TEMP/0002-before.json" ../specs/done/evidence/0003/AC-6-0002-evidence-sha256.txt
  ```

## Etiketler

- `@empty` — seed öncesi gerçek boş vitrin (0002).
- `@seeded` — seed sonrası smoke + design testleri.
- `@design` — 0003 ana sayfa, hero, scroll, a11y, metadata, credits, rooms,
  runtime, budget ve video kanıtları.

CI `--grep @seeded` ile hem `0002` smoke hem `0003` design testlerini koşar.

## Viewport ve tercihler

- Mobil: `390×844`
- Desktop: `1280×800`
- Tablet sınırları: `767`, `768`, `1023`, `1024`
- `prefers-reduced-motion: reduce`: Playwright `reducedMotion` / `emulateMedia`.
- `save-data`: `context.addInitScript` ile `navigator.connection.saveData = true`.
- JavaScript kapalı: `browser.newContext({ javaScriptEnabled: false })`.

## Design testleri

```bash
cd e2e
npx playwright test tests/design --grep @seeded
```

Kapsam:

- `home.spec.ts` — ortak chrome, copy deck, locale, JS kapalı, konsol.
- `hero.spec.ts` — poster/video, viewport, reduced-motion, save-data, fallback.
- `hero-asset.spec.ts` — ffprobe ile codec/çözünürlük/süre/fps/sessizlik.
- `scroll-story.spec.ts` — tam 2 pin, 1023/1024 sınırı, parallax, scroll kanıtı.
- `scroll-story-video.spec.ts` — `.webm` video kanıtı (≤15 sn, ≤5 MB).
- `a11y.spec.ts` — skip link, klavye, mobil menü, landmark, axe.
- `metadata.spec.ts` — title/description/canonical/hreflang/OG.
- `credits.spec.ts` — `CREDITS.md` kaynaklı sayfa.
- `rooms-design.spec.ts` — 0002 sözleşmesi, üç durum, hata matrisi, font fallback.
- `runtime.spec.ts` — üçüncü taraf istek yokluğu, motion kütüphanesi yükleme.
- `budget.spec.ts` — JS gzip-9, asset bütçeleri, scroll CLS.

## Video kanıtı

`scroll-story-video.spec.ts` Playwright `video` kaydını 1280×800'de alır ve
`ffmpeg-static` ile VP9 `.webm`'e dönüştürür. Çıktı
`specs/done/evidence/0003/AC-2-home-scroll-story.webm` olur; süre ≤15 sn,
boyut ≤5.242.880 B'dir. `ffprobe-static` süre/akış kontrolünde kullanılır.

## Lighthouse

`0003` için Lighthouse CI gate'i yoktur. Kanıt üretimi:

```bash
cd e2e
CHROME_PATH="$(node -e "console.log(require('playwright').chromium.executablePath())")"
npx lighthouse@10.4.0 http://localhost:3000/tr \
  --only-categories=performance --form-factor=mobile --screenEmulation.mobile \
  --throttling-method=devtools --output=json --output=html \
  --chrome-path="$CHROME_PATH" --chrome-flags="--headless --no-sandbox"
```

3 koşunun median LCP/CLS değeri `AC-5-lighthouse-mobile.md` ve
`AC-5-lighthouse-mobile.html` olarak saklanır.

## İki aşamalı 0002 akışı

> **Windows notu:** Yerelde `mvn verify` çalıştırırken backend `java -jar` ile
> açıksa Spring Boot `repackage` aşaması jar dosyasını kilitleyebilir. Ya
> backend'i durdurun ya da `-Dspring-boot.repackage.skip=true` kullanın; CI'da
> uygulama çalışmadığı için bu durum oluşmaz.

Ayrıntılı altyapı, migration, seed ve backend/frontend başlatma adımları için
önceki bölümdeki komutlar geçerlidir:

1. Altyapı + migration (seed'siz)
2. Backend/frontend build + start
3. `@empty` (geçici kanıt kökü)
4. Seed (`-Pseed -Dseed.env=local|ci`)
5. `@seeded` smoke (geçici kanıt kökü)
6. `@seeded` design (varsayılan kanıt kökü)
7. SHA-256 manifest doğrulaması

## 0004 admin kimlik E2E

- Admin seed (migrator): `(cd backend/admin-api && ./mvnw flyway:migrate -Pseed -Dseed.env=local)`
- Admin-api local JWT anahtarı: `ADMIN_JWT_ACTIVE_KEY_PATH=.local/admin-jwt-active.pem` ve
  `ADMIN_JWT_ALLOW_GENERATED_DEV_KEY=true`.
- Testler: `npx playwright test tests/admin --grep @admin` — başarılı login + korumalı sayfa,
  hatalı login generic hata, logout + login'e dönüş (3/3).
- 0002 canlı admin-web smoke testi kaldırıldı; 0002 smoke artık 3/3'tür (1 `@empty` + 2 `@seeded`).
- 0003 design 40/40 korunur; 0004 kanıtları `specs/done/evidence/0004/` altına yazılır.

## 0006 fiyatlandırma E2E

- Testler: `npx playwright test tests/pricing --grep @pricing` (`@media` sonrası).
- Sıra: SUPER_ADMIN login → `/pricing` yükleme/boş/hata/dolu → plan → sezon →
  baz/sezon fiyat matrisi → varsayılan plan → eskimiş `expectedDefaultPlanId`
  için 409 → manuel kur → manuel fiyat → public teklif API (TRY/EUR/USD, çok
  gece) → 30 gece kabul / 31 gece 400 → sezon fiyatı silme (baz fallback) →
  baz fiyat silme (409 + eksik uyarısı) → sıfır/negatif tutar
  (`pricing.invalid_amount`) ve sıfır misafir (`pricing.invalid_guests`) →
  oda vitrininde fiyat yok → FRONT_DESK okuma/yazma/silme 403.
- Local/CI'da TCMB scheduler kapalıdır (`HOTEL_PRICING_TCMB_ENABLED=false`);
  bayat kur uyarısı `db/seed/pricing` içindeki dünkü TCMB kuruyla gösterilir.
- Kanıtlar: `specs/done/evidence/0006/`; `E2E_EVIDENCE_DIR` verilmezse bu dizine
  yazılır. CI tüm E2E kanıtlarını `$RUNNER_TEMP/e2e-evidence` altına yazar;
  0001–0005 arşivleri `git diff` + untracked kontrolüyle korunur.

## 0005 medya E2E

- Testler: `npx playwright test tests/media --grep @media` (`@admin` sonrası).
- Gerçek yükleme fixture'ı: `web/public/media/hero/home-hero-poster-1280.webp`
  (mevcut Pexels lisanslı hero posteri; yeni lisans/asset eklenmez).
- Akış: SUPER_ADMIN login → SVG/boyut reddi → yükleme → READY → oda atama →
  yayın → kapak → public `room-card-image` (WebP) → FRONT_DESK 403 →
  unpublish/placeholder → archive onayı.
- Kanıtlar: `specs/done/evidence/0005/`; `E2E_EVIDENCE_DIR` verilmezse bu dizine
  yazılır. CI'da geçici köke yazılır. 0001–0004 arşivleri SHA-256 manifestiyle
  korunur.
