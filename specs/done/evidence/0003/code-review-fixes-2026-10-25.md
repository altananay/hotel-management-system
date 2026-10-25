# Code Review Fix Report — Spec 0003

- **Tarih:** 2026-10-25
- **İncelenen head:** `8bbe705`
- **Fix head:** `8537442` (kod + kanıt commit'leri)
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
- Video preflight 404 için gerçek 404 ağ testi CR-8'de eklendi; bu bölümde
  yalnız geniş kaynak-hatası filtresinin kaldırılması ele alındı.
- Font glyph fallback testi geçerli, Latin glyph'i olmayan WOFF2 fixture'ı
  (`e2e/fixtures/fonts/empty-latin.woff2`) ile ayrı tutuldu. Gerçek font
  yükleme hatası testi CR-8'de `route.abort("failed")` ile eklendi.
- Test adları "application console error" yerine "console error" olarak
  güncellendi.

### CR-8 — Gerçek failure-path kanıtları

- **Font:** `shouldFallbackToASystemFontWhenFontRequestsFail` testi üç font
  isteğini gerçekten `route.abort("failed")` ile düşürür. Font face'ler
  `error` durumuna geçer, metin genişliği `Source Sans 3`'süz yığınla
  eşleşir ve ham konsolda yalnız üç beklenen `net::ERR_FAILED` kaynak logu
  bulunur; `pageerror`/uygulama hatası yoktur.
- **Glyph fallback:** Geçerli fixture testi
  `shouldFallbackToASystemFontWhenTheCustomFontLacksLatinGlyphs` olarak
  yeniden adlandırıldı; `AC-1-font-glyph-fallback.log` yazar.
- **Video preflight 404:** Gerçek `HEAD` isteği 404 döner; yanıt gerçekten
  `HEAD/404` olarak doğrulanır, poster kalır, video mount edilmez ve yalnız
  beklenen tek 404 kaynak logu kabul edilir.
- **Video kaynak hatası:** `HEAD` 200 ile preflight geçer; WebM ve MP4
  kaynakları gerçek 404 döner. `<source>` error olayı yakalanır, video
  DOM'dan kaldırılır, poster kalır; iki beklenen 404 kaynak logu dışında
  uygulama hatası yoktur. Son `<source>` üzerindeki `onError` fallback'i
  deterministik kılar.
- Tarayıcının kaçınılmaz kaynak logu spec `B-13` ve `docs/frontend.md` hata
  matrisinde açık istisna olarak tanımlandı.

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
| 0003 design E2E | **40/40** |
| 0002 smoke `@seeded` | **3/3** |
| 0002 kanıt SHA-256 | **UNCHANGED (12 dosya)** |
| Axe kritik/ciddi | **0** |
| JS gzip-9 | **206.826 B / 256.000 B** |
| Lighthouse mobil LCP (3 koşu median) | **2289,97 ms** |
| Lighthouse CLS | **0.042** |
| Guard raw-color self-test | FAIL (beklenen) → PASS |

## CI

- **CI run:** [40224276319](https://github.com/altananay/hotel-management-system/actions/runs/40224276319)
  — head `8537442`, **success** (Backend, Frontend, E2E, CI Quality Gate;
  GitGuardian ayrıca success).
- PR #11: `mergeable=true`, `mergeable_state=clean`, **140 dosya**;
  `backend/`, `admin-web/` ve `specs/done/evidence/0002/` kapsam dışı.
- İlk PR'da CI'nın çalışmama nedeni: `releasable` tabanlı branch ile
  `integration` base'i arasındaki conflict, PR'ı `dirty` yapıp GitHub merge
  ref'ini engelledi. Merge sonrası run tetiklendi.
