# Code Review Fix Report — Spec 0003

- **Tarih:** 2026-10-25
- **İncelenen head:** `8bbe705`
- **Fix head:** `8cec9fc` (kod), ardından gelen kanıt commit'i
- **PR:** [#11](https://github.com/altananay/hotel-management-system/pull/11)
- **Kapsam:** `code-review-2026-10-25.md` içindeki CR-1, CR-3, CR-4, CR-5, CR-6, CR-7.
  CR-2 Takım Yöneticisi kararıyla kapalıdır; değiştirilmedi.

## Bulgu bazında çözüm

### CR-1 — PR base/CI (Blocker)

- `origin/integration` (`61fcf49`) feature branch'e merge edildi (`0305043`).
- Conflict'lerin tamamı feature içeriği korunarak çözüldü; merge sonrası ağaç
  merge öncesi feature ağacıyla birebir aynıdır.
- Böylece PR diff'i yalnız 0003 kapsamına iner ve GitHub merge ref'i
  oluşturabilir; `pull_request` workflow'u tetiklenebilir.
- Push sonrası PR `mergeable` durumu ve CI koşuları ayrıca doğrulanacaktır.

### CR-3 — Console filtreleri

- `e2e/helpers/console.ts` içindeki `appErrors()` ve `RESOURCE_ERROR` filtresi
  tamamen kaldırıldı. Testler artık ham `console.errors` dizisini doğrular.
- Video preflight 404 testi, Chromium'un gerçek 404 kaynak logunu üretmeyecek
  şekilde `fetch` shim'i ile aynı `response.ok === false` dalını çalıştırır;
  poster kalır ve konsol temiz kalır.
- Font fallback testi artık istek abort etmez. Latin glyph'i olmayan geçerli
  WOFF2 fixture'ı (`e2e/fixtures/fonts/empty-latin.woff2`) tüm font
  isteklerine servis edilir. Ölçülen metin genişliği, aynı font yığınının
  `Source Sans 3` olmadan genişliğiyle eşleşir; bu sistem fallback'ini kanıtlar
  ve konsol hatası üretmez.
- Test adları "application console error" yerine "console error" olarak
  güncellendi.

### CR-4 — Parallax testi

- Test 1280×800'e sabitlendi.
- `window.__hotelMotionLoaded?.gsap === true` ve `#home-story`
  `data-motion="pinned"` olana kadar beklenir.
- En az bir `[data-parallax]` offsetinin sıfırdan büyük ve en fazla 40 px
  olduğu doğrulanır; GSAP yüklenmezse test timeout ile düşer.

### CR-5 — Token dışı dekoratif renkler ve guard

- `paper-grain`, `hero-vignette` ve `fine-grid` ham `rgb()` değerleri
  `color-mix(in srgb, var(--token) N%, transparent)` türevlerine çevrildi.
- Vignette/mask için `--color-scrim: #14100c` token'ı eklendi ve
  `docs/frontend.md` token tablosuna yazıldı.
- `web/scripts/check-design-system.mjs` artık `globals.css` içinde `@theme`
  dışında kalan `rgb()/rgba()/hsl()/hsla()` ve hex renkleri tarar.
- Guard self-test: dosyaya geçici `.guard-self-test { color: rgb(1 2 3); }`
  eklendiğinde guard FAIL verdi; geri alındığında PASS verdi.

### CR-6 — Hero poster preload

- Tüm rotalarda çalışan `ReactDOM.preload("/media/hero/home-hero-poster-960.webp")`
  locale layout'tan kaldırıldı.
- Poster tek `<img>` olarak `srcSet` (`960w`, `1280w`, `1920w`) ve
  `sizes="100vw"` ile responsive seçilir; `fetchPriority="high"` korunur.
- Ayrı preload kaldırıldı çünkü Next'in rota prefetch'i ipucunu rooms/credits
  sayfalarına taşıyor ve gereksiz poster indiriyordu. Ana sayfa HTML'indeki
  `img`, preload scanner tarafından zaten erken keşfedilir.
- Yeni E2E: 1280'de yalnız `-1280.webp`, 390'da yalnız `-960.webp` isteği;
  rooms ve credits sayfalarında poster isteği ve image preload sayısı sıfır.

### CR-7 — Runtime save-data değişimi

- `HomeHeroVideo` artık `navigator.connection` üzerindeki `change` olayını
  dinler; `canUseVideo` yeniden değerlendirilir.
- Save-data açılınca video DOM'dan kaldırılır; kapanınca preflight yeniden
  çalışır ve video geri gelir.
- Cleanup'ta connection listener'ı kaldırılır.
- Runtime E2E her iki yönü (açma/kapama) doğrular.

## Yerel doğrulama

| Kontrol | Sonuç |
|---|---|
| `web` lint + tsc + guard | PASS |
| Vitest | **39/39** |
| 0003 design E2E | **38/38** |
| 0002 smoke `@seeded` | **3/3** |
| 0002 kanıt SHA-256 | **UNCHANGED (12 dosya)** |
| Axe kritik/ciddi | **0** |
| JS gzip-9 | **206.819 B / 256.000 B** |
| Lighthouse mobil LCP (3 koşu median) | **2316,31 ms** |
| Lighthouse CLS | **0.042** |
| Guard raw-color self-test | FAIL (beklenen) → PASS |

## CI

- PR #11 push sonrası `mergeable` ve `CI Quality Gate` sonucu bu rapora
  eklenecektir.
