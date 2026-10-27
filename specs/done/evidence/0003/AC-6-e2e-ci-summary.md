# 0003 E2E ve CI özeti

- Web unit testleri (Vitest): **39/39 passed**
- 0002 smoke (`@empty` 1 + `@seeded` 3): **4/4 passed**
  - 0002 kanıt kökü geçici dizin kullanıldı; arşiv değişmedi.
  - Bu turda `@seeded` **3/3** yeniden koşuldu; `@empty` CI'da temiz DB ile koşar.
- 0003 design (`@design @seeded`): **40/40 passed**
- Axe: altı rotada kritik/ciddi ihlal **0**
- JS bütçesi: **206.826 B / 256.000 B** (gzip-9)
- CLS (scroll): **0.0000**; Lighthouse CLS: **0.042**
- Lighthouse mobil (devtools throttling, 3 koşu median): **LCP 2289,97 ms**,
  performans **0.96**
- Video kanıtı: **4,00 sn / 546.651 B** (VP9, 1280×800)
- `0002` kanıt SHA-256: **UNCHANGED (12 dosya)**

## 2026-10-25 code review fixleri

- **CR-1:** `origin/integration` feature branch'e merge edildi (`0305043`); conflict'ler
  feature içeriği korunarak çözüldü. Push sonrası PR #11 **mergeable=true /
  clean** oldu; CI run [40227343692](https://github.com/altananay/hotel-management-system/actions/runs/40227343692)
  **success**.
- **CR-3:** `appErrors()` kaynak-hatası filtresi kaldırıldı; tüm senaryolar ham
  `console.errors` ile doğrulanıyor. Font glyph fallback testi Latin glyph'i
  olmayan geçerli bir WOFF2 fixture'ı (`e2e/fixtures/fonts/empty-latin.woff2`)
  sunar; gerçek font/video failure-path testleri CR-8'de ayrıca ele alınır.
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
- **CR-8:** Font failure-path testi gerçek `route.abort("failed")` ile üç font
  isteğini düşürür; font face'ler `error` durumuna geçer, metin sistem
  fallback'iyle ölçülür ve yalnız beklenen `net::ERR_FAILED` kaynak logları
  kabul edilir. Video için gerçek preflight 404 ve gerçek kaynak 404'leri
  (WebM + MP4) ayrı test edilir; `<source>` error olayı, posterin kalması ve
  videonun kaldırılması doğrulanır. Glyph fallback testi ayrı adlandırıldı.
  Tarayıcının kaçınılmaz kaynak logu spec `B-13`'te açık istisna olarak
  tanımlandı.

## 2026-10-25 QA bulguları (QA-0003-1…3)

- **QA-0003-1 (kanıt hijyeni):** Kanıt seti kod head `5f8b4b3` üzerinde
  yeniden üretildi. `AC-1-asset-and-font-budget.log` ve
  `AC-5-build-guard.log` LF checkout'ta `OFL-SourceSans3.txt` için **4.486 B**
  yazar; `AC-1-scope.txt` güncel test satırları ve görsel boyutlarıyla
  yenilendi. Kapanış CI run'ları #39 (`ae0f92d`) ve #40 (`ad4603f`) ayrı
  ayrı success olarak aşağıda listelendi; QA fix run'ı
  [40232356949](https://github.com/altananay/hotel-management-system/actions/runs/40232356949)
  (`45e6fc8`) success.
- **QA-0003-2 (yerel ortam):** `e2e/README.md` içine Windows'ta çalışan
  backend jar'ı nedeniyle `spring-boot:repackage` kilidi notu ve
  `-Dspring-boot.repackage.skip=true` önerisi eklendi.
- **QA-0003-3 (mock veri):** `SAMPLE_ROOMS` gerçek seed ile hizalandı; her
  odada **3 olanak** ve `code` ASC sırası var. Design testi kart başına
  olanak metin sırasını birebir doğrular; `AC-3-rooms-*` ekran görüntüleri
  yeniden alındı.
- **QA-0003-3 sıra fix:** 2026-10-27'de mock sırası düzeltildi; kod head
  `65fda1f`; CI run
  [40594795547](https://github.com/altananay/hotel-management-system/actions/runs/40594795547)
  (`8353ee0`) success.

## Kanıt revizyonu

- **Kod head:** `65fda1f` (QA sıra fix commit'i; QA-1 ilk kanıt turu `5f8b4b3`)
- Bu evidence seti `65fda1f` ağacında yeniden üretildi; sonraki commit yalnız
  kanıt/rapor dosyalarını değiştirir.
- `AC-1-asset-and-font-budget.log` / `AC-5-build-guard.log`: LF checkout,
  `OFL-SourceSans3.txt` **4.486 B**.
- `AC-1-scope.txt`: final koda göre güncellendi; `backend/` ve `admin-web/`
  kapsam dışı.
- CI: kapanış #39 ve #40 success; QA fix run'ı
  [40232356949](https://github.com/altananay/hotel-management-system/actions/runs/40232356949)
  (`45e6fc8`) ve sıra fix run'ı
  [40594795547](https://github.com/altananay/hotel-management-system/actions/runs/40594795547)
  (`8353ee0`) success.

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

- **Kapanış CI run'ları:**
  - [#39 — 40227343692](https://github.com/altananay/hotel-management-system/actions/runs/40227343692)
    — head `ae0f92d`, sonuç **success**.
  - [#40 — 40227913825](https://github.com/altananay/hotel-management-system/actions/runs/40227913825)
    — head `ad4603f`, sonuç **success** (spec DoD'deki final run).
  - Her iki run'da Backend, Frontend, E2E ve CI Quality Gate success;
    GitGuardian ayrıca success.
- **QA fix CI run:** [40232356949](https://github.com/altananay/hotel-management-system/actions/runs/40232356949)
  — head `45e6fc8`, sonuç **success**.
- **QA amenity-order fix CI run:** [40594795547](https://github.com/altananay/hotel-management-system/actions/runs/40594795547)
  — head `8353ee0`, sonuç **success**.
- **Neden ilk PR'da run oluşmadı:** Branch `releasable` tabanlıydı, PR base'i
  `integration` idi. İki geçmiş ayrıştığı ve conflict oluştuğu için PR
  `mergeable=false` / `dirty` kaldı; GitHub merge ref'i üretemedi ve
  `pull_request` workflow'u tetiklenmedi. `origin/integration` merge edilip
  conflict'ler feature içeriğiyle çözülünce PR `mergeable=true` / `clean`
  oldu ve CI run başladı.
- **PR kapsamı:** 143 dosya; `backend/`, `admin-web/` ve
  `specs/done/evidence/0002/` değişmedi.
- CI akışı: `@empty` → seed → `@seeded` smoke (geçici kanıt kökü) →
  SHA manifest → `@seeded` design → SHA doğrulama → 0003 kanıt artefaktı.
