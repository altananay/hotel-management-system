# Frontend Konvansiyonları

> Bu dosya `web` ve `admin-web` frontend kurallarının **tek gerçek kaynağıdır**.
> Tasarım sistemi token'ları, bileşen envanteri, erişilebilirlik, animasyon/video,
> performans bütçesi ve kalite kapıları burada tanımlanır. Değerler
> `web/src/app/globals.css` içindeki tek `@theme` bloğundan gelir.

## Kapsam

- `web` — public site: TR/EN, Tailwind CSS v4 + shadcn/ui yaklaşımı, Radix
  primitifleri, Motion + GSAP ScrollTrigger + Lenis.
- `admin-web` — admin paneli: TR, shadcn/ui, sade ve fonksiyonel.
- **MUI kullanılmaz** (D3). Tam shadcn migrasyonu yapılmaz; mevcut `Button`,
  `Card` ve `Skeleton` API'leri korunur.
- Yeni UI veya i18n kütüphanesi eklenmez. Radix yalnızca `MobileNav` dialog'u
  için kullanılır.

## 1. Tasarım Token'ları

Token'lar `web/src/app/globals.css` içindeki tek `@theme` bloğunda tanımlanır.
Semantik adlar kullanılır; bileşenlerde token dışı sabit marka rengi kullanılmaz.
Bu dilimde yalnız açık tema vardır.

### 1.1 Renk

| Token | Değer | Kullanım |
|---|---|---|
| `--color-surface` | `#f4efe6` | Ana sayfa zemini |
| `--color-surface-raised` | `#fbf8f2` | Kart / yükseltilmiş yüzey |
| `--color-surface-muted` | `#ece4d7` | İkincil yüzey, pasif rozet |
| `--color-surface-inverse` | `#1d1916` | Koyu bölüm / footer |
| `--color-surface-inverse-raised` | `#2a241f` | Koyu yüzey üstü |
| `--color-fg` | `#241f1a` | Ana metin |
| `--color-fg-muted` | `#6d6256` | Gövde / ikincil metin |
| `--color-fg-subtle` | `#8c8072` | Yalnız büyük/yardımcı metin |
| `--color-fg-inverse` | `#f7f1e7` | Koyu zemin metni |
| `--color-fg-inverse-muted` | `#cabfae` | Koyu zemin ikincil metni |
| `--color-accent` | `#9b5a2c` | Bakır vurgu |
| `--color-accent-strong` | `#7a3f1b` | Vurgu hover / güçlü |
| `--color-accent-soft` | `#ead7c3` | Yumuşak vurgu zemini |
| `--color-accent-contrast` | `#fffaf2` | Vurgu üstü metin |
| `--color-border` | `#d8cdbd` | İnce kenarlık |
| `--color-border-strong` | `#b6a58f` | Güçlü kenarlık |
| `--color-focus` | `#8a461d` | Odak halkası |
| `--color-overlay` | `rgb(20 16 12 / 0.55)` | Modal / hero overlay |
| `--color-scrim` | `#14100c` | Hero vignette ve dekoratif maske taban rengi |
| `--color-danger-surface` | `#fdf1ef` | Hata zemini |
| `--color-danger-fg` | `#7c2d20` | Hata metni |
| `--color-danger-border` | `#e7b4a7` | Hata kenarlığı |

Kontrast: gövde metni en az **4.5:1**, büyük metin/UI en az **3:1**.
`--color-fg-subtle` yalnız büyük metin ve dekoratif etiketlerde kullanılır.

### 1.2 Tipografi

| Token | Değer | Kullanım |
|---|---|---|
| `--font-display` | Cormorant Garamond 600 + serif fallback | Başlıklar, wordmark |
| `--font-body` | Source Sans 3 400/600 + sistem fallback | Gövde, UI |
| `--text-display-xl` | `clamp(2.75rem, 7.4vw, 6.5rem)` | Hero h1 |
| `--text-display-lg` | `clamp(2.5rem, 5vw, 4.25rem)` | Bölüm başlıkları |
| `--text-display-md` | `clamp(2rem, 3.5vw, 3rem)` | Alt başlık / kart adı |
| `--text-heading` | `clamp(1.5rem, 2.4vw, 2.125rem)` | Kart başlığı |
| `--text-body` | `1.0625rem` / `1.72` | Gövde |
| `--text-small` | `0.9375rem` / `1.62` | Küçük gövde |
| `--text-eyebrow` | `0.75rem` / `0.18em` | Etiket / eyebrow |

Fontlar `web/public/fonts/` altında self-host edilir; toplam **3 WOFF2**,
**2 aile**, **3 ağırlık** ve **≤ 204.800 B**'dir. Latin Extended + ASCII
kapsanır; Türkçe karakterler eksiksizdir. Çalışma zamanında üçüncü taraf font
isteği yapılmaz. Lisanslar `CREDITS.md` ve `web/public/fonts/OFL-*.txt`'dedir.

### 1.3 Boşluk, yarıçap, gölge

| Token | Değer | Kullanım |
|---|---|---|
| `--spacing-section` | `clamp(5rem, 10vw, 9rem)` | Bölüm dikey boşluğu |
| `--spacing-section-sm` | `clamp(3.5rem, 7vw, 6rem)` | Kompakt bölüm |
| `--spacing-gutter` | `clamp(1.25rem, 4vw, 2.5rem)` | Yatay gutter |
| `--spacing-stack` | `1.5rem` | Dikey yığın |
| `--spacing-stack-sm` | `0.75rem` | Küçük yığın |
| `--container-page` | `78rem` | Sayfa maksimum genişliği |
| `--radius-xs` | `0.25rem` | Küçük köşe |
| `--radius-sm` | `0.5rem` | Rozet / küçük yüzey |
| `--radius-md` | `0.75rem` | Kontrol |
| `--radius-lg` | `1rem` | Kart |
| `--radius-xl` | `1.5rem` | Büyük kart |
| `--radius-2xl` | `2rem` | Panel |
| `--radius-arch` | `999px 999px 1rem 1rem` | Kemer formu |
| `--shadow-card` | `0 18px 44px -30px rgb(36 31 26 / 0.38)` | Kart |
| `--shadow-elevated` | `0 34px 80px -42px rgb(36 31 26 / 0.48)` | Hover / öne çıkan |
| `--shadow-inset` | `inset 0 1px 0 rgb(255 250 242 / 0.08)` | Koyu yüzey |

### 1.4 Hareket

| Token | Değer | Kullanım |
|---|---|---|
| `--duration-fast` | `150ms` | Renk / odak geçişi |
| `--duration-base` | `260ms` | Standart hover |
| `--duration-slow` | `600ms` | Video görünürlüğü |
| `--duration-reveal` | `850ms` | Reveal |
| `--ease-standard` | `cubic-bezier(0.22, 1, 0.36, 1)` | Standart |
| `--ease-emphasized` | `cubic-bezier(0.16, 1, 0.3, 1)` | Vurgulu |

CSS geçişleri yalnız **renk ve opaklık** ile sınırlıdır. Layout property'si
(`width`, `height`, `top`, `left`) animasyonu yasaktır. Reveal/mikro hareketler
Motion ile, pin/parallax GSAP ile, smooth scroll Lenis ile yapılır.

## 2. Bileşen Envanteri ve Kullanım Kuralları

| Bileşen | Amaç | Varyant / durum | Erişilebilirlik notu |
|---|---|---|---|
| **Header** (`SiteHeader`) | Ortak üst chrome: wordmark, nav, dil, mobil menü | Aktif nav `aria-current="page"` | `<header>` banner; wordmark `<Link>`, h1 değil; tek `web-site-title` |
| **MobileNav** | `<1024 px` tam ekran menü | Kapalı / açık dialog | Radix Dialog; `role="dialog"`, `aria-modal="true"`, focus trap, Escape, scroll lock, route'ta kapanma |
| **Footer** (`SiteFooter`) | İletişim, credits, telif | Tek görünüm | `<footer>` contentinfo; `tel:`/`mailto:`; `footer-*` testid'leri |
| **Button** | Eylem düğmesi | `primary`, `secondary`, `ghost`; `md`, `lg` | `type="button"` varsayılanı; görünür `:focus-visible`; disabled |
| **Card** | İçerik yüzeyi | Varsayılan / özelleştirilmiş | Semantik başlık kullanan içerikle; dekoratif değil |
| **Section** | Bölüm + maksimum genişlik | `paper`, `raised`, `inverse`, `transparent`; `default`/`compact`/`none` | `Container` Section'ın parçası; landmark/heading sahibi sayfa |
| **Badge** | Etiket / durum | `accent`, `outline`, `inverse`, `muted` | Metin alternatifi; renk tek başına anlam taşımaz |
| **RoomCard** | Oda vitrini kartı | Dolu / placeholder | **Tıklanabilir değil**, pointer cursor yok; fiyat yok; başlık `h2` |
| **StatePanel** | Yükleme / boş / hata | `loading`, `empty`, `error` | Loading `role="status"`, error `role="alert"`; retry düğmesi |
| **Skeleton** | Yükleme iskeleti | Alt bileşen | `aria-hidden` tercih edilir; StatePanel içinde kullanılır |

Kurallar:

- `RoomCard` detay sayfası olmadığı için tıklanabilir değildir; hover'da yalnız
  hafif kenarlık/gölge değişir, `cursor: pointer` kullanılmaz.
- `Section` tek primitiftir; `Container` ayrı bir bileşen değildir.
- Mevcut `Button`, `Card`, `Skeleton` API'leri korunur; yeni zorunlu prop
  eklenmez.
- Üçüncü taraf UI kütüphanesi eklenmez; Radix yalnız MobileNav'dadır.

## 3. Erişilebilirlik Kontrol Listesi

- [ ] Her sayfada tek `h1`; başlık seviyeleri atlanmaz.
- [ ] İlk Tab `skip-to-content` bağlantısını görünür biçimde odaklar; hedef
  `#main-content`, `tabindex="-1"`.
- [ ] Tüm etkileşimli öğelerde görünür `:focus-visible` stili vardır.
- [ ] Header `banner`, footer `contentinfo`; aktif nav `aria-current="page"`.
- [ ] Mobil menü `role="dialog"` + `aria-modal="true"`, focus trap, Escape ve
  kapatmada odağın düğmeye dönmesi, body scroll lock, route'ta kapanma.
- [ ] Dekoratif SVG/video/poster `aria-hidden`; anlamlı görsellerde alt metin.
- [ ] Telefon/e-posta `tel:`/`mailto:` bağlantısıdır.
- [ ] Metin kontrastı ≥ 4.5:1; büyük metin/UI ≥ 3:1.
- [ ] `prefers-reduced-motion: reduce` altında video, pin/parallax, reveal ve
  smooth scroll yoktur; içerik anında görünürdür.
- [ ] `@axe-core/playwright` taramasında altı rotada kritik/ciddi ihlal yoktur.

## 4. Animasyon ve Video Rehberi

### 4.1 Kütüphane görev dağılımı

- **GSAP ScrollTrigger** — yalnız dekoratif parallax (pin, `data-motion="pinned"` + CSS sticky ile yapılır).
- **Motion** — yalnız reveal, ikon stroke draw ve dekoratif yavaş zoom (opacity/transform).
- **Lenis** — yalnız desktop smooth scroll.
- Üçü de ilk boyamadan sonra (≈2 sn gecikmeli) dinamik import edilir.
- `< 1024 px`'te GSAP ve Lenis yüklenmez; yalnız hafif Motion reveal vardır.
- Reduced-motion'da Motion/GSAP/Lenis yüklenmez; reveal yoktur, içerik
  anında görünürdür.
- Tercih çalışma zamanında değişirse dinlenir; ScrollTrigger/Lenis ve
  pin spacer'ları temizlenir.
- JavaScript kapalıyken tüm bölüm metinleri görünürdür.

### 4.2 Scroll hikâyesi

| Bölüm | `data-motion` | Not |
|---|---|---|
| `home-hero` | `static` | LCP poster; pin yok |
| `home-story` | `pinned` (≥1024) | Tam 1 sahne; parallax yalnız dekoratif |
| `home-experience` | `pinned` (≥1024) | Tam 1 sahne |
| `home-rooms-cta` | `static` | — |
| `home-contact` | `static` | — |

Toplam pinli sahne **tam 2**'dir. Pin, `data-motion="pinned"` + CSS sticky ile
kısa bir track (`115svh`) üzerinde tutulur; böylece tek scroll jestiyle sahne
geçilebilir. Parallax yalnız dekoratif arka plan katmanındadır ve en fazla
**±40 px**'dir (dekoratif sayı 20 px, grid 24 px); metin parallax'ı yoktur.
Dekoratif story/experience görselleri ±20/24 px parallax ve 1.045'e kadar
scroll-driven scale alır; experience ikonları görünür olduklarında stroke draw
ile çizilir.

### 4.3 Hero video

- Dosyalar: `web/public/media/hero/home-hero.webm` (VP9) ve `.mp4` (H.264).
- 1920×1080, 8–12 sn, 30 fps, sessiz; WebM/MP4 ayrı ayrı ≤ 8.388.608 B.
- Poster: 1920×1080 WebP q80, ≤ 307.200 B; `home-hero-poster` her zaman DOM'da. Mobil/tablet için sırasıyla 960×540 ve 1280×720 responsive türevleri kullanılır; 1920×1080 ana dosya korunur. Poster `srcSet`/`sizes` ile seçilir; ana sayfa HTML'inde `loading="eager"` + `fetchPriority="high"` taşır. Ayrı `<link rel="preload">` kullanılmaz: Next rota prefetch'i ipucunu rooms/credits sayfalarına taşıyıp gereksiz poster indiriyordu; preload scanner `<img>` etiketini zaten erken keşfeder.
- Video `preload="none"`; ilk boyamadan sonra eklenir ve `canplay` olunca görünür.
- Video yalnız ≥768 px, `prefers-reduced-motion: no-preference` ve save-data
  kapalıyken DOM'a eklenir.
- Hata/404/autoplay reddinde video kaldırılır, poster kalır; uygulama konsol
  hatası üretilmez.
- `aria-hidden="true"`, `muted`, `loop`, `playsInline`, ses kanalı yok.

## 5. Performans Bütçesi

Ölçüm tanımları: **1 KB = 1024 B**, **1 MB = 1.048.576 B**; üst sınırlar dahil.

| Alan | Bütçe | Doğrulama |
|---|---|---|
| LCP | ≤ 2.5 s | Lighthouse mobil, production, 3 koşu median |
| CLS | ≤ 0.1 | Lighthouse + Playwright scroll `layout-shift` toplamı |
| `/tr` ilk yük JS | ≤ 256.000 B gzip-9 | Tüm script chunk'ları, lazy dahil, ağ idle'a kadar |
| Font toplamı | ≤ 204.800 B | `web/public/fonts/**/*.woff2` |
| Hero WebM / MP4 | her biri ≤ 8.388.608 B | dosya boyutu |
| Poster / OG | her biri ≤ 307.200 B | dosya boyutu |
| Story / experience stilleri | her biri ≤ 204.800 B | dosya boyutu |

- Hero video LCP öğesi olamaz; poster/metin LCP'yi taşır.
- Story ve experience dekoratif stilleri aynı D18 kaynağından türetilir; her
  biri **≤ 204.800 B**'dir. Below-fold yüklenir; poster/video bütçesini etkilemez.
- Çalışma zamanında üçüncü taraf font/medya/script isteği yapılmaz; ana sayfa
  yalnız aynı origin ve `http://localhost:8081` API çağrıları üretir.
- CI'da deterministik dosya ve bundle bütçesi kapısı vardır; Lighthouse CI gate'i
  bu dilimde yoktur.

## 6. Kalite Kapıları

- `npm run lint` — ESLint + `tsc --noEmit` + `check-design-system`.
- `npm test` — Vitest/Testing Library.
- `npm run build` — `prebuild` içinde `check-assets` (eksik/limit aşımı fail-fast).
- Playwright: `@empty` → seed → `@seeded` (0002 regression) → `@design`
  (0003 design) adımları.
- `@axe-core/playwright`: `/tr`, `/en`, `/tr/rooms`, `/en/rooms`,
  `/tr/credits`, `/en/credits`; kritik/ciddi = 0.
- Asset/bundle bütçesi ve scroll CLS testi CI'da kırmızı build üretir.
- Lighthouse raporu kanıt olarak üretilir; CI gate değildir.

### 6.1 Hata / fallback matrisi

| Durum | Davranış |
|---|---|
| Video yüklenemez / autoplay reddi | Video kaldırılır, poster kalır, uygulama konsol hatası yok |
| Font yüklenemez | `font-display: swap` + sistem fallback; kullanıcıya hata yok |
| Rooms API 4xx/5xx/ağ/timeout/şema | `rooms-error` + sabit metin + `rooms-retry`; 10 sn timeout; teknik detay gizli |
| `CREDITS.md` eksik/bozuk | Build fail-fast |
| Hero/poster/OG eksik veya bütçe aşımı | Build/bütçe kontrolü fail-fast |
| Statik home/credits | Yükleme/boş/hata zorunluluğundan muaf |

### 6.2 İçerik kuralları

- Tüm public metinler onaylı TR/EN copy deck'inden gelir; geliştirici yeni
  pazarlama metni uydurmaz.
- İletişim bilgileri temsilîdir; `CREDITS.md` bunu açıkça belirtir.
- `CREDITS.md` tek kaynaktır; `/[locale]/credits` sayfası build zamanında bu
  dosyadan üretilir.
- `0002`'nin `data-testid` sözleşmesi ve sabit boş/hata/retry metinleri
  değişmez. `RoomCard` tıklanabilir değildir; kartlarda fiyat gösterilmez.
