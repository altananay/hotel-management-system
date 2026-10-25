# Arayüz Tasarım Sistemi ve Ana Sayfa Deneyimi

> Doldurmadan önce: `AGENTS.md` (altın kurallar) ve `docs/architecture.md` okunur.
> Numara: `specs/` altındaki en büyük numaranın + 1'i. Dosya: `specs/<####>-<english-name>.md` (İngilizce kebab-case; içerik Türkçe). Branch: `feature/<####>-<english-name>` (yalnızca `releasable`'dan). Örnek: `specs/0003-ui-design-system.md`.
> Spec onaylanmadan kod yazılmaz (Altın Kural 1). Onayı Takım Yöneticisi verir.

---

## 0. Künye

- **Spec No:** `0003`
- **Başlık:** Arayüz Tasarım Sistemi ve Ana Sayfa Deneyimi (`ui-design-system`)
- **Durum:** Onaylandı
- **Branch:** `feature/0003-ui-design-system`
- **İlgili doküman/kararlar:** `docs/architecture.md` (§2, §5 R6, §10 D3/D7/D8/D9/D11/D18, §11), `docs/frontend.md` (İSKELET), `docs/conventions.md` (§1, §2.3, §5), `docs/testing.md` (§1, §3, §4), `docs/domain.md` (§1, §3.14), `docs/git.md` (§1, §5), `specs/done/0002-room-catalog.md`, `specs/0003-clarification-brief.md`; Onaylı kararlar: `CLARIFY-1A`…`CLARIFY-9A` ve `B-01`…`B-17` (2026-10-25); rev.2: QA bulguları (B-13 hata matrisi test eşlemesi, 767/768/1023/1024 sınırları, tüm sayfalarda konsol hatası) işlendi
- **Tarih:** 2026-10-25 *(Son revizyon: 2026-10-25, rev.2)*
- **Sahip:** Analist (`analyst`)
- **Onaylayan (Takım Yöneticisi):** Takım Yöneticisi (Onaylandı — 2026-10-25)

## 1. Intent (Neden?)

Kapadokya'daki butik otelin potansiyel misafirleri ve otel yönetimi, siteye girişte otelin taş/mağara kimliğini ilk saniyede hissettiren ve premium algı yaratan bir web deneyimi istiyor; mevcut stone/amber görsel dil işlevsel ama jenerik kaldığı ve otelin karakterini taşımadığı için ilk izlenim hedeflenen seviyeye ulaşmıyor. Başarı; ana sayfanın hero video, iki sahnelik scroll hikâyesi ve ölçülü mikro etkileşimlerle sinematik bir giriş sunması, aynı tasarım dilinin oda vitrinine tutarlı biçimde uygulanması, TR/EN içeriğin eksiksiz olması, mobil ve tablette içeriğin bozulmadan çalışması, `prefers-reduced-motion` tercihinde hareketin tamamen durması, erişilebilirlik ile performans bütçelerinin (LCP/CLS, hero medya ağırlığı) kanıtlanması ve tasarım sistemi kararlarının `docs/frontend.md`'de gelecekteki tüm ekranlar için tek kaynak haline gelmesidir. Bu dilimde rezervasyon/fiyat/müsaitlik, gerçek oda fotoğrafları ve 360 tur, admin paneli tasarımı, CMS ve çoklu tesis bilinçli olarak yapılmayacaktır.

## 2. Requirements

- **REQ-1 (Tasarım Sistemi, Bileşen Envanteri ve `docs/frontend.md` Genişletmesi — `CLARIFY-9A`, `B-01`, `B-02`, `B-16`):** Tek bir tasarım sistemi; renk, tipografi, boşluk, köşe yarıçapı, gölge ve hareket (süre/easing) token'ları ile yeniden kullanılabilir bileşen envanterinden oluşmalıdır. `docs/frontend.md` "İSKELET" durumundan çıkarılarak token tabloları, bileşen envanteri ve kullanım kuralları, erişilebilirlik kontrol listesi, animasyon/video rehberi, performans bütçesi ve kalite kapıları ile `CLARIFY-4A`'nın öngördüğü genişletme tamamlanmalıdır.
  - **Görsel yön ve token değerleri (`B-01`):** Arayüzün görsel kalitesi ve "premium" şartı developer sorumluluğundadır. Token değerlerini, görsel yönü ve görsel seçim/düzenlemeleri developer belirler; poster, crop, renk, yerleşim ve yardımcı görseller serbestçe ayarlanabilir. Görsel kabul için önceden moodboard/token onayı aranmaz. Developer iki anlamlı seçenek arasında kaldığında veya takıldığında Takım Yöneticisi'ne iki seçenek + kendi önerisi + gerekçesiyle gelir (Altın Kural 4); karar birlikte verilir. Ek görsel tur serbesttir.
  - **Token kuralı:** Token'lar tek kaynakta (`web/src/app/globals.css` içindeki Tailwind v4 `@theme` bloğu) tanımlanır; semantik isimler kullanılır (ör. yüzey, metin, vurgu, kenarlık, odak halkası). Değerleri developer belirler; `docs/frontend.md`'deki token tablosu bu adlarla birebir eşleşir. Bileşenlerde token dışı sabit marka rengi kullanılmaz. Bu dilimde yalnız açık tema vardır; karanlık tema kapsam dışıdır.
  - **Tipografi (`B-02`):** **Cormorant Garamond 600** (display) + **Source Sans 3 400/600** (gövde); ikisi de **SIL OFL**; **Latin Extended** subset; toplam **3 `woff2`** dosya. Fontlar `web/public/fonts/` altında self-host edilir; çalışma zamanında üçüncü taraf font isteği yapılmaz; Türkçe karakterler (İ, ı, Ğ, ğ, Ş, ş, Ç, ç, Ö, ö, Ü, ü) eksiksiz desteklenir; toplam font ağırlığı **≤ 200 KB (204.800 B)**. `CREDITS.md` aile adı, lisans, kaynak URL ve ağırlıkları listeler.
  - **Bileşen envanteri ve kapsam (`B-16`):** Tam shadcn migrasyonu yapılmaz; mevcut `Button`, `Card` ve `Skeleton` API'leri korunur. Radix yalnız mobil navigasyon dialog'u için eklenir. Envanterde en az Header, MobileNav, Footer, Button, Card, Section (Container onun parçası), Badge, RoomCard, StatePanel (yükleme/boş/hata) ve Skeleton (alt bileşen) yer alır; her biri için amaç, varyant/durum ve erişilebilirlik notu `docs/frontend.md`'de belgelenir. `RoomCard` tıklanabilir değildir (detay sayfası yok); hover'da yalnız hafif gölge/kenarlık değişir, pointer cursor kullanılmaz.
  - **Stack sınırı:** Tailwind CSS v4 + shadcn/ui yaklaşımı, gerektiğinde Radix primitifleri; MUI yasaktır (D3). Yeni bir UI kütüphanesi veya i18n kütüphanesi eklenmez.
- **REQ-2 (Ana Sayfa: Hero Video, Scroll Hikâyesi, Site Chrome ve Metadata — `CLARIFY-1A`, `CLARIFY-2A`, `CLARIFY-6A`, `CLARIFY-7A`, `CLARIFY-8A`, `B-03`…`B-13`):** Ana sayfa; hero, hikâye/atmosfer, deneyim/olanaklar, oda vitrini CTA'sı, konum/iletişim ve footer bölümlerinden oluşmalıdır. Tüm metinler Ek A'daki onaylı TR/EN copy deck'inden tüketilir. Hero'da D18 asset'i (Pexels 30653318) poster eşliğinde sessiz, döngülü ve satır içi oynatılmalı; scroll hikâyesi tam iki pinli sahne ile kurulmalıdır. Ortak site chrome'u (header: tipografik wordmark, Ana Sayfa + Odalar navigasyonu, dil geçişi, skip link; footer: iletişim, `CREDITS.md` bağlantısı, telif) üç sayfada da aynı olmalıdır.
  - **Hero medya (`B-05`):** Asset'ler `web/public/media/hero/` altında WebM/VP9 (ana) + MP4/H.264 (fallback) + WebP poster olarak repoda bulunur. Video **1920×1080 (16:9)**, **8–12 sn** kusursuz döngü, **30 fps**, sessiz (ses kanalı yok); VP9 CRF ~32, H.264 CRF ~23 hedeflenir. Poster **1920×1080 WebP q80** ve **≤ 300 KB (307.200 B)**; WebM ve MP4 ayrı ayrı **≤ 8 MB (8.388.608 B)**. `preload="none"` kullanılır; poster her zaman DOM'dadır (`home-hero-poster`) ve LCP'yi taşır; video ilk boyamadan sonra posterin üstüne eklenir ve `canplay` olduğunda görünür. Video yalnız ≥ 768 px genişlikte, `prefers-reduced-motion: no-preference` iken ve `save-data` kapalıyken DOM'a eklenir. Video hatası, 404 veya autoplay reddi durumunda video gizlenir/kaldırılır, poster görünür kalır ve konsola hata basılmaz. `ffprobe` ile ses akışı olmadığı, süre, çözünürlük ve codec kanıtlanır. Kaynak, lisans, erişim tarihi ve transcode notları `CREDITS.md`'de yer alır (D18).
  - **Scroll hikâyesi (`B-04`, `B-12`):** Tam **2** pinli sahne vardır: `home-story` ve `home-experience`. `home-hero`, `home-rooms-cta` ve `home-contact` statiktir. 768–1023 px'te video açıktır; pin/parallax yoktur, yalnız hafif reveal uygulanır. ≤ 767 px'te de pin/parallax yoktur. Parallax yalnız dekoratif arka plan katmanında ve en fazla ±40 px'dir; metin parallax'ı yoktur. `data-motion` haritası: hero `static`, story `pinned`, experience `pinned`, rooms-cta `static`, contact `static`; reduced-motion altında hepsi `static`.
  - **Animasyon kütüphanesi görev dağılımı (`B-12`):** GSAP ScrollTrigger yalnız pin/parallax; Motion yalnız reveal/mikro (opacity/transform); Lenis yalnız desktop smooth scroll için kullanılır. Üçü de ilk boyamadan sonra lazy-load edilir. `< 1024 px`'te GSAP ve Lenis yoktur; yalnız hafif Motion reveal vardır. Reduced-motion'da Lenis/ScrollTrigger hiç yüklenmez, reveal yoktur ve içerik anında görünürdür. CSS geçişleri yalnız renk/opaklıkla sınırlıdır; layout property'si (width/height/top/left) animasyonu yasaktır. Tercih çalışma zamanında değişirse dinlenir. Animasyonlar JavaScript kapalıyken de tüm bölüm metinlerinin görünür kalmasını sağlar; istemci tarafında dinleyici/ScrollTrigger temizliği yapılır.
  - **Site chrome, h1 ve skip link (`B-07`, `B-09`):** Wordmark bir `<Link>`'tir, `h1` değildir; `web-site-title` testid'ini taşır. TR wordmark metni birebir `"Kapadokya Taş Konak Otel"`, EN wordmark metni Ek A'daki `"Cappadocia Stone Mansion Hotel"`dir. Sayfa `h1`'leri: ana sayfa hero başlığı, odalar sayfa başlığı, credits sayfa başlığı. Header `banner`, footer `contentinfo` rolündedir; aktif nav öğesi `aria-current="page"` taşır. `main` öğesi `id="main-content"` ve `tabindex="-1"` taşır. Skip link metni "İçeriğe geç" / "Skip to content", hedefi `#main-content`, testid'i `skip-to-content`tir; ilk odaklanabilir öğedir ve yalnız klavye odağında görünür. Telefon/e-posta `tel:`/`mailto:` bağlantısıdır.
  - **Dil geçişi ve gezinme:** Dil geçişi bulunulan sayfayı korur (ana sayfa → ana sayfa, vitrin → vitrin, credits → credits); `/` → `/tr` yönlendirmesi korunur. Desktop navigasyon `site-nav-home` ve `site-nav-rooms`; mobil navigasyon ayrı `mobile-nav-*` testid'lerini kullanır.
  - **Metadata/SEO (`B-06`):** Üç rota için de (home, rooms, credits) locale'e özel `<title>` ve `description` (70–160 karakter, dahil) bulunur; canonical self'tir; `hreflang` `tr`, `en`, `x-default` → ilgili sayfanın TR karşılığıdır (home `/tr`, rooms `/tr/rooms`, credits `/tr/credits`). Tek, metinsiz 1200×630 OG görseli (≤ 300 KB) ve locale'e özel `og:image:alt` kullanılır; `twitter:card=summary_large_image`dır. `metadataBase` `NEXT_PUBLIC_SITE_URL`'den üretilir; sondaki `/` normalize edilir; env yoksa `http://localhost:3000` kullanılır. `.env.example` `NEXT_PUBLIC_SITE_URL` ile güncellenir. Title/description/OG alt metinleri Ek A copy deck'inden gelir. Mevcut hafif JSON sözlük altyapısı korunur; yeni i18n kütüphanesi eklenmez.
  - **İçerik ve yer tutucu kuralı (`CLARIFY-8A`, `B-03`):** Tüm içerik statiktir ve TR/EN'dir (D8, D9). Logo ayrı bir marka asset'i değil, tipografik wordmark'tır. İletişim bilgileri temsilî yer tutucudur: adres TR `"Örnek Mahallesi, Taş Konak Caddesi No: 1, Nevşehir / Kapadokya"`, adres EN `"Sample District, Stone Mansion Street No: 1, Nevşehir / Cappadocia"`, telefon `"+90 000 000 00 00"`, e-posta `"ornek@example.com"`. `CREDITS.md` bu bilgilerin temsilî olduğunu açıkça belirtir.
  - **Credits sayfası (`B-10`):** `CREDITS.md` tek kaynaktır; `/[locale]/credits` sayfası build zamanında bundan üretilir (veya üretilen içerik modülü kullanılır). Sayfa ortak header/footer chrome'u, `h1` ve metadata taşır; başlıklar TR/EN, lisans adları/URL'leri değişmez. Footer etiketi "Kaynaklar ve Lisanslar" / "Credits & Licenses"tir. Sayfa axe ve metadata kapsamına dahildir. Bu dilimde açılan tek yeni sayfadır.
  - **Hata/fallback matrisi (`B-13`):** Video yüklenemez/autoplay reddedilir → video gizlenir, poster kalır, konsol hatası yok. Font yüklenemez → sistem fallback'i, `font-display: swap`, kullanıcıya hata yok. Rooms API 4xx/5xx/ağ hatası/zaman aşımı/şema hatası → mevcut `rooms-error` + sabit metin + `rooms-retry`; teknik detay gizli; **10 sn timeout**. `CREDITS.md` eksik/bozuksa → build fail-fast. Hero/poster/OG eksikse → build/bütçe kontrolü fail-fast. Statik home/credits sayfaları yükleme/boş/hata zorunluluğundan muaftır; bu istisna `docs/frontend.md`'ye yazılır.
- **REQ-3 (Oda Vitrininin Tasarım Sistemiyle Giydirilmesi — `CLARIFY-4A`, `CLARIFY-9A`, `B-06`, `B-07`, `B-16`):** `/[locale]/rooms` sayfasının işlevi ve veri sözleşmesi korunur; kart, yükleme, boş, hata ve dolu düzenleri tasarım sistemiyle yenilenir ve sayfa ortak header/footer chrome'unu kullanır. `0002`'nin `data-testid` sözleşmesi ve sabit TR/EN durum metinleri aynen korunur; `home-rooms-link` ana sayfadaki CTA'da kalır. Fotoğraf yer tutucuları `media` dilimine kadar sürer; kartlarda fiyat gösterilmez. `RoomCard` tıklanabilir değildir. Odalar sayfası locale'e özel metadata taşır (`B-06`).
- **REQ-4 (Erişilebilirlik — `CLARIFY-2A`, `B-08`, `B-09`, `B-12`, `B-13`):** Deneyim; `prefers-reduced-motion: reduce` altında video oynatmadan, pin/parallax ve mikro hareketler olmadan, içerik anında görünür biçimde çalışmalıdır. Klavye ile tam gezinme, görünür odak halkası, anlamlı başlık hiyerarşisi, WCAG AA kontrastı ve dekoratif görsellerde `aria-hidden` sağlanmalıdır.
  - **Mobil navigasyon (`B-08`):** `< 1024 px`'te mobil navigasyon devreye girer; tam ekran `role="dialog"` + `aria-modal="true"` kullanır. Focus trap vardır; Escape veya kapatma ile odak menüyü açan düğmeye döner; body scroll lock uygulanır; route değişiminde menü kapanır. Dil geçişi her genişlikte header'da görünür kalır.
  - **Klavye ve semantik (`B-09`):** İlk Tab `skip-to-content` bağlantısını görünür biçimde odaklar; tüm etkileşimli öğelerde görünür `:focus-visible` stili vardır. Her sayfada tek `h1` vardır; başlık seviyeleri atlanmaz. Dekoratif SVG/video `aria-hidden`, anlamlı görsellerde alternatif metin bulunur.
  - **Kontrast:** Metin kontrastı en az 4.5:1, büyük metin ve UI bileşenleri en az 3:1'dir.
  - **Doğrulama:** `@axe-core/playwright` ile `/tr`, `/en`, `/tr/rooms`, `/en/rooms`, `/tr/credits`, `/en/credits` taranır; kritik/ciddi ihlal sayısı sıfırdır. Klavye, mobil menü ve reduced-motion davranışı Playwright ile doğrulanır.
- **REQ-5 (Performans Bütçesi ve Ölçüm — `CLARIFY-5A`, `B-11`, `B-12`, `B-15`):** Deneyim; hero medya ve animasyonlara rağmen hızlı açılmalı ve animasyonlar layout shift üretmemelidir. Bütçeler: **LCP ≤ 2.5 s**, **CLS ≤ 0.1** (production build, Lighthouse mobil preset), `/tr` rotası için ilk yük JavaScript'i **≤ 250 KB gzip (256.000 B)**, font toplamı **≤ 200 KB (204.800 B)**, WebM/MP4 ayrı ayrı **≤ 8 MB (8.388.608 B)**, poster ve OG görseli **≤ 300 KB (307.200 B)**.
  - **Ölçüm tanımları (`B-11`):** **1 KB = 1024 B; 1 MB = 1.048.576 B**; üst sınırlar dahildir. JS bütçesi: `/tr` ilk yükünde yüklenen tüm script chunk'larının (lazy chunk'lar dahil, ağ idle'a kadar) `gzip -9` toplamı. Font bütçesi: `web/public/fonts` altındaki tüm `woff2` dosyaları. Lighthouse: production build, Chrome headless, mobil preset, **3 koşu median**. CLS: Lighthouse değeri + Playwright scroll sırasında `layout-shift` toplamı. Kanıt logunda Lighthouse/Chrome sürümü, URL, build commit'i ve ölçüm komutu yer alır. Lighthouse raporundaki LCP öğesi hero video dosyası olamaz; poster/metin LCP'yi taşır.
  - **CI kapıları:** Dosya boyutu ve bundle boyutu gibi deterministik bütçeler CI'da otomatik doğrulanır ve aşımda build kırmızı olur. Lighthouse CI gate'i bu dilimde yoktur.
  - **Üçüncü taraf:** Çalışma zamanında font, medya veya script için üçüncü taraf isteği yapılmaz; ana sayfa yalnız aynı origin ve `http://localhost:8081` API çağrıları üretir.
- **REQ-6 (E2E, Kanıtlar ve Doküman Etkisi — `CLARIFY-4A`, `CLARIFY-6A`, `B-14`):** Tasarım deneyimi Playwright ile doğrulanmalı; ana sayfa TR/EN, hero görünürlüğü, scroll adımları, mobil, tablet, reduced-motion, save-data, JavaScript kapalı ve klavye senaryoları kanıtlanmalıdır. Görsel kanıtlar `specs/done/evidence/0003/` altına yazılmalı; `0002` arşivi değişmemelidir.
  - **Kanıt ve test altyapısı (`B-14`):** `E2E_EVIDENCE_DIR` kanıt kökünü belirler; varsayılan `specs/done/evidence`'dır; ortak helper her spec'i `.../0002` veya `.../0003` altına yazar. Yeni testler `test(..., { tag: ["@design", "@seeded"] })` ile etiketlenir; CI `--grep @seeded` design testlerini de koşar; `0002` başlık etiketleri değişmez. `save-data` `context.addInitScript` ile `navigator.connection.saveData = true` yapılarak simüle edilir. axe `@axe-core/playwright` ile altı rotada koşar ve Markdown rapor üretir. Video `recordVideo` (1280×800, ~10 sn scripted scroll) ile kaydedilir, `ffmpeg` ile **≤ 5 MB (5.242.880 B)** ve **≤ 15 sn**'ye getirilir. `0002` izolasyonu önce/sonra **SHA-256 manifest** karşılaştırmasıyla kanıtlanır. Sayfa kanıtları `fullPage`, scroll/focus/menu kanıtları viewport olarak alınır.
  - **Video kanıtı:** `AC-2-home-scroll-story.webm` zorunludur. `docs/testing.md` §3 ve `specs/done/README.md` video kanıtı satırıyla güncellenir.
  - **Mevcut testler:** `0002`'nin `@empty` ve `@seeded` smoke testleri davranış olarak değişmeden 4/4 yeşil kalır.

## 3. Constraints (Kapsam Dışı Dahil)

- **Teknik/İş kısıtları:**
  - **Kapsam:** Değişiklik yalnız `web`, `e2e`, `docs` ve kök `CREDITS.md` ile sınırlıdır. `hotel-api`, `admin-api`, `admin-web` ve veritabanı değişmez; mevcut `GET /api/room-types` sözleşmesi olduğu gibi tüketilir.
  - **Stack ve kütüphane sınırı (`B-16`):** Tailwind CSS v4 + shadcn/ui yaklaşımı, gerektiğinde Radix primitifleri; animasyon Motion + GSAP ScrollTrigger + Lenis. MUI yasaktır. Tam shadcn migrasyonu yoktur; mevcut `Button`, `Card`, `Skeleton` API'leri korunur; Radix yalnız MobileNav dialog'u için eklenir. Yeni UI veya i18n kütüphanesi eklenmez.
  - **Varlıklar ve D18 sınırı (`B-01`, `B-05`):** Hero video kaynağı **Pexels 30653318**, Pexels License, lisans kaydı ve dosya boyutu kuralları korunur. Developer poster, crop, renk, yerleşim ve yardımcı görselleri serbestçe ayarlayabilir; **hero video kaynağını değiştirmek isterse ayrı bir D18 kararı olarak Takım Yöneticisi'ne sunar.** Video/poster/fontlar repoda tutulur; Git LFS kullanılmaz (git geçmişinde ~16 MB kalıcı binary maliyeti kabul edilir).
  - **İçerik ve yer tutucu:** Statik TR/EN (D9); CMS yok. Metinler Ek A copy deck'inden gelir; `0002`'nin sabit boş/hata/retry metinleri aynen korunur. İletişim bilgileri temsilî yer tutucudur ve `CREDITS.md`'de işaretlenir.
  - **Tema ve gizlilik:** Yalnız açık tema; karanlık tema kapsam dışıdır. Analytics, çerez bandı, izleme veya pazarlama pikseli eklenmez.
  - **Sınır değerleri (`B-15`):**

    | Alan | Kural | Test noktaları |
    |---|---|---|
    | Viewport | 390×844 mobil; 768'de video açık; 1023'e kadar pin yok; 1024'te pin | 390, 767, 768, 1023, 1024, 1280 |
    | Description | 70–160 karakter **dahil**; trim sonrası Unicode code point | 69, 70, 160, 161 |
    | Byte birimi | 1 KB = 1024 B; 1 MB = 1.048.576 B; üst sınırlar dahil | 204.800 B font; 256.000 B JS gzip; 8.388.608 B video; 307.200 B poster/OG |
    | Pinli sahne | Tam 2 (`B-04/A`) | `data-motion="pinned"` sayısı = 2 |
    | Font | Tam 2 aile; 3 ağırlık | aile sayısı = 2; toplam woff2 ≤ 200 KB |
    | Video kanıtı | ≤ 15 sn, ≤ 5 MB | ffprobe süresi + dosya boyutu |
- **Kapsam dışı (bilinçli olarak yapılmayacaklar):**
  - Rezervasyon, fiyat/quote, müsaitlik ve ödeme akışları (`booking`, `pricing`, `payment`).
  - Gerçek oda fotoğrafları, galeri ve 360 tur (`media`); vitrinde yer tutucular korunur.
  - `admin-web` tasarımı; admin arayüzü sade işlevsel hâlinde kalır.
  - CMS veya panelden içerik düzenleme (D9); çoklu tesis/tenant (D7); ek diller.
  - Oda tipi detay sayfası; `media` ve `pricing` hazır olduğunda ayrı spec'te açılacaktır (`0002` planlama kaydı).
  - Ayrı logo/brand kimliği tasarımı, kurumsal kimlik kiti veya favicon seti üretimi; yalnız tipografik wordmark kullanılır.
  - Form akışları, arama/filtreleme, harita gömme ve canlı destek.
  - Lighthouse CI gate'i, görsel regresyon (snapshot) altyapısı ve A/B test altyapısı.
- **Kabul edilen risk/ödünç:**
  - **Görsel kalite (`B-01`):** "Premium" şartı developer sorumluluğundadır; belirsizlikte Takım Yöneticisi ile birlikte karar verilir. Ek görsel tur serbesttir; önceden moodboard/token onayı aranmaz.
  - **Repo ağırlığı:** Hero video ve MP4 fallback'i repoda kalıcı olarak ~16 MB yer kaplar; Git LFS kullanılmaz.
  - **Mobil ve tablet hareketi:** 390×844'te yalnız poster; 768–1023 px'te video açık ama pin/parallax yoktur. Sinematik etki mobilde azalır, buna karşılık veri ve performans korunur.
  - **Performans doğrulaması:** LCP/CLS Lighthouse median ve Playwright scroll CLS ile kanıtlanır; CI'da Lighthouse gate yoktur.
  - **Temsilî içerik:** Adres/telefon/e-posta gerçek değildir; yayına almadan önce gerçek bilgilerle değiştirilmelidir.
- **Doküman ve CI Etkisi (Developer):**
  1. **`docs/frontend.md`:** "İSKELET" ibaresi kaldırılır; token tabloları (değerler developer tarafından), bileşen envanteri ve kullanım kuralları, erişilebilirlik kontrol listesi, animasyon/video rehberi, performans bütçesi, kalite kapıları, statik sayfa üç-durum istisnası ve yer tutucu içerik kuralı eklenir.
  2. **`CREDITS.md` (yeni):** Hero video kaynağı (Pexels 30653318), lisans, erişim tarihi, transcode notları, poster ve font lisansları (Cormorant Garamond, Source Sans 3 — SIL OFL); iletişim bilgilerinin temsilî olduğu notu.
  3. **`e2e/README.md`:** `E2E_EVIDENCE_DIR` ve ortak helper, `@design`/`@seeded` etiketleri, iki viewport (390×844, 1280×800), tablet kontrol noktaları, reduced-motion, save-data init script, JavaScript kapalı ve video kaydı/ffmpeg adımları.
  4. **`docs/testing.md` §3 ve `specs/done/README.md`:** Video kanıtı satırı ve `AC-n-<english-name>.webm` isim deseni eklenir.
  5. **`.github/workflows/ci.yml` ve `.env.example`:** Deterministik asset/bundle bütçe kontrolü eklenir; `NEXT_PUBLIC_SITE_URL` değişken envanteri güncellenir; `@design` testleri `@seeded` aşamasında koşacak şekilde bağlanır.

## 4. Context

- **Mevcut durum:** `specs/done/0002-room-catalog.md` tamamlanmıştır. `hotel-api` üzerinde `GET /api/room-types` sözleşmesi ve `web` üzerinde `/[locale]/rooms` vitrin sayfası çalışır durumdadır. `0002` kapanışında backend 142, frontend 15, E2E 4 test yeşildir; 8 görsel kanıt `specs/done/evidence/0002/` altındadır.
- **Mevcut arayüz:** `web/src/app/[locale]/page.tsx` ana sayfası hero + üç olanak kartından oluşan işlevsel bir iskelettir; mevcut `h1` site adıdır ve hero başlığı `h2`'dir. `/[locale]/rooms` sayfası kendi header'ını taşır. Görsel dil ad-hoc stone/amber sınıflarıyla kurulmuştur; `web/src/app/globals.css` yalnız `@import "tailwindcss";` içerir. `web/src/components/ui` altında el yazımı `Button`, `Card`, `Skeleton` bulunur; Radix kurulu değildir.
- **Mevcut bağımlılıklar:** `web/package.json` içinde Motion, GSAP, Lenis ve Radix yoktur; bu dilimde eklenecektir. `web/public/` boştur; hero asset'i, `web/public/fonts/` ve `CREDITS.md` henüz yoktur.
- **E2E bağımlılıkları ve bulgular:**
  - `e2e/tests/smoke.spec.ts` kanıt dizinini `specs/done/evidence/0002` olarak sabitler; ortak helper ve `E2E_EVIDENCE_DIR` gerekir (`B-14`).
  - Aynı dosya ana sayfada `/tr` üzerinde `web-site-title` metnini birebir `"Kapadokya Taş Konak Otel"` olarak doğrular; TR wordmark bu metni korumalıdır. EN wordmark Ek A'daki `"Cappadocia Stone Mansion Hotel"` metnini kullanır (`B-07`, `B-09`).
  - Dil geçişi vitrinde sayfa bağlamını korur (`/tr/rooms` ↔ `/en/rooms`); ortak header bu davranışı sürdürmelidir.
  - `0002` görsel kanıtları arşivdir ve değiştirilmez; `0003` kanıtları ayrı dizine yazılır; izolasyon SHA-256 manifest ile kanıtlanır.
- **Doküman durumu:** `docs/frontend.md` hâlâ "İSKELET" olarak işaretlidir ve genişletmeyi bu dilime bırakır. `docs/testing.md` §3 yalnız PNG kanıtı ve isim desenini tanımlar; video kanıtı için satır yoktur.
- **İlgili uygulamalar:**
  - `web` (`:3000`): Tasarım sistemi, ana sayfa, vitrin giydirmesi, credits sayfası, metadata.
  - `e2e`: `@empty`/`@seeded` (0002) + `@design`/`@seeded` (0003) smoke ve kanıt üretimi.
  - `docs` ve kök `CREDITS.md`: Tasarım sistemi sözleşmesi ve lisans kaydı.
- **Sonraki dilimler:** `0003` Arayüz/Tasarım + Ana Sayfa → Admin Kimlik ve Oturum → Medya → Fiyatlandırma → Ödeme → Rezervasyon → Bildirim. **Planlama kaydı (`0002`):** Oda Tipi Detay Sayfası üründe kesinlikle yer alacaktır; `media` ve `pricing` hazır olduğunda ilgili spec'te açılacak ve URL stratejisi (`id` veya `slug`) o spec'te kararlaştırılacaktır.
- **Karar kaydı:** `CLARIFY-*` ve `B-*` kararlarının özeti ve etkilediği REQ/AC'ler Ek B'dedir.

## 5. Acceptance Criteria

- **AC-1 (Tasarım Sistemi, Token'lar, Bileşen Envanteri ve Doküman — `REQ-1`, `CLARIFY-9A`, `B-01`, `B-02`, `B-16`):**
  - `web/src/app/globals.css` içinde tek bir `@theme` bloğu; renk, tipografi, boşluk, yarıçap, gölge ve hareket token'larını semantik adlarla tanımlar. Token değerlerini developer belirler; `docs/frontend.md`'deki token tablosu bu adlarla birebir eşleşir; bileşenlerde token dışı sabit marka rengi kullanılmaz (kod incelemesi + `git grep` kanıtı).
  - `docs/frontend.md` artık "İSKELET" ibaresi taşımaz ve şu altı bölümü içerir: token'lar, bileşen envanteri ve kullanım kuralları, erişilebilirlik kontrol listesi, animasyon/video rehberi, performans bütçesi, kalite kapıları. Envanterde Header, MobileNav, Footer, Button, Card, Section (Container parçası), Badge, RoomCard, StatePanel ve Skeleton yer alır; her biri için amaç, varyant/durum ve erişilebilirlik notu yazılıdır. Tam shadcn migrasyonu yoktur; Radix yalnız MobileNav dialog'undadır.
  - **Cormorant Garamond 600** + **Source Sans 3 400/600** self-host edilir; tam 2 aile, 3 ağırlık, 3 `woff2`; Latin Extended subset; toplam ≤ 200 KB (204.800 B); Türkçe karakterler eksiksizdir; çalışma zamanında üçüncü taraf font isteği yoktur (Playwright request log kanıtı). `CREDITS.md` aile, lisans, kaynak URL ve ağırlıkları listeler.
  - Görsel kalite ve premium şartı developer sorumluluğundadır; görsel kabul için önceden moodboard/token onayı aranmaz; ek görsel tur serbesttir. Takıldığında Takım Yöneticisi ile birlikte karar verilir.
  - `admin-web` ve backend dizinlerinde değişiklik yoktur (`git diff --stat` kanıtı).
  - **Kanıt:** `docs/frontend.md`, token/envanter karşılaştırma notu, `AC-1-asset-and-font-budget.log`, `AC-1-runtime-requests.log`, `AC-1-font-fallback.log`.
- **AC-2 (Ana Sayfa, Hero Video, Scroll Hikâyesi, Chrome ve Metadata — `REQ-2`, `CLARIFY-1A`, `CLARIFY-2A`, `CLARIFY-7A`, `CLARIFY-8A`, `B-03`…`B-13`):**
  - `/tr` ve `/en` ana sayfalarında `home-hero`, `home-story`, `home-experience`, `home-rooms-cta`, `home-contact` ve `site-footer` bölümleri bulunur. Tüm metinler Ek A copy deck'iyle birebir eşleşir. Header `site-header` testid'ini, wordmark `web-site-title` testid'ini taşır; TR wordmark metni birebir `"Kapadokya Taş Konak Otel"`, EN wordmark metni `"Cappadocia Stone Mansion Hotel"`dir. Wordmark bir linktir ve `h1` değildir. Sayfa `h1`'i hero başlığıdır.
  - **Testid sözleşmesi (`B-07`):**

    | Alan | Testid |
    |---|---|
    | Sayfa iskeleti | `skip-to-content`, `site-main`, `site-header`, `site-footer` |
    | Wordmark | `web-site-title` (sayfada tek) |
    | Desktop navigasyon | `site-nav-home`, `site-nav-rooms` |
    | Mobil navigasyon | `mobile-nav-toggle`, `mobile-nav`, `mobile-nav-home`, `mobile-nav-rooms` |
    | Dil | `locale-switcher`, `locale-switch-tr`, `locale-switch-en` |
    | Ana sayfa | `home-hero`, `home-hero-poster`, `home-hero-video`, `home-story`, `home-experience`, `home-rooms-cta`, `home-contact`, `home-rooms-link`, `home-contact-address`, `home-contact-phone`, `home-contact-email` |
    | Footer | `footer-address`, `footer-phone`, `footer-email`, `footer-copyright`, `footer-credits-link` |
    | Credits | `credits-page` |

  - **Hero (`B-05`):** 1280×800'de `home-hero-video` DOM'da bulunur; `muted`, `loop`, `playsinline`, ses kanalı yok ve `aria-hidden="true"`dır. 390×844'te video DOM'da yoktur, `home-hero-poster` görünürdür. `save-data` açıkken ve `prefers-reduced-motion: reduce` altında da video DOM'da yoktur. Poster her zaman DOM'dadır; video ilk boyamadan sonra eklenir ve `canplay` olduğunda görünür. Video hatası/404/autoplay reddinde video gizlenir, poster kalır, konsol hatası oluşmaz.
  - **Hero asset kanıtı:** WebM/VP9 (≤ 8 MB) + MP4/H.264 (≤ 8 MB) + WebP poster (1920×1080 q80, ≤ 300 KB) `web/public/media/hero/` altındadır; `ffprobe` çıktısı 1920×1080, 8–12 sn, 30 fps ve ses akışının olmadığını doğrular; `CREDITS.md` kaynak, lisans, erişim tarihi ve transcode notlarını içerir.
  - **Scroll (`B-04`, `B-12`):** 1280×800'de tam 2 bölüm `data-motion="pinned"` taşır (`home-story`, `home-experience`); `home-hero`, `home-rooms-cta` ve `home-contact` `static`tir. Parallax yalnız dekoratif arka plan katmanında ±40 px'dir. 390×844 ve 768×1024'te `data-motion="pinned"` sayısı sıfırdır; 768'de video açıktır. Reduced-motion'da tüm bölümler `static`tir. JavaScript kapalıyken tüm bölüm metinleri görünürdür.
  - **Metadata (`B-06`):** Home, rooms ve credits için TR/EN `<title>` ve `description` (70–160 karakter, dahil) Ek A'daki metinlerle birebir eşleşir; canonical self; `hreflang` `tr`, `en`, `x-default` → ilgili sayfanın TR karşılığı; tek 1200×630 OG görseli (≤ 300 KB) ve locale `og:image:alt`; `twitter:card=summary_large_image`. Mutlak URL'ler `NEXT_PUBLIC_SITE_URL` ile üretilir.
  - **Credits ve footer (`B-10`):** `/[locale]/credits` sayfası `credits-page` testid'iyle açılır; `CREDITS.md`'den üretilir; ortak chrome, `h1`, metadata ve dil geçişi taşır; lisans adları/URL'leri sabittir. Footer'daki `footer-credits-link` "Kaynaklar ve Lisanslar" / "Credits & Licenses" etiketiyle bu sayfaya gider. Footer `footer-address`, `footer-phone`, `footer-email` alanları `CLARIFY-8A`'daki temsilî değerleri taşır; `footer-copyright` Ek A'daki metni taşır; `CREDITS.md` bu değerlerin temsilî olduğunu belirtir.
  - **Hata/fallback (`B-13`):** Video/font/rooms/CREDITS davranışları `REQ-2` matrisine uyar; statik home/credits yükleme/boş/hata zorunluluğundan muaftır.
  - Tüm sayfalarda (home, rooms, credits; TR/EN) gezinmede konsol hatası oluşmaz.
  - **Kanıt:** `AC-2-home-tr-desktop.png`, `AC-2-home-en-desktop.png`, `AC-2-home-tr-mobile.png`, `AC-2-home-scroll-01-hero.png`, `AC-2-home-scroll-02-story.png`, `AC-2-home-scroll-03-experience.png`, `AC-2-home-scroll-04-rooms-cta.png`, `AC-2-home-nojs.png`, `AC-2-home-video-fallback.png`, `AC-2-credits-tr-desktop.png`, `AC-2-credits-en-desktop.png`, `AC-2-metadata.md`, `AC-2-home-scroll-story.webm`.
- **AC-3 (Oda Vitrininin Tasarım Sistemiyle Giydirilmesi ve `0002` Sözleşmesinin Korunması — `REQ-3`, `CLARIFY-4A`, `CLARIFY-9A`, `B-06`, `B-07`, `B-16`):**
  - `0002`'nin tüm testid'leri korunur: `home-rooms-link`, `locale-switch-tr`, `locale-switch-en`, `rooms-page`, `rooms-loading`, `rooms-empty`, `rooms-empty-title`, `rooms-error`, `rooms-error-message`, `rooms-retry`, `rooms-list`, `room-card`, `room-card-name`, `room-card-description`, `room-card-capacity`, `room-card-amenities`, `room-card-amenity`, `room-card-placeholder`. `0002`'deki sabit TR/EN boş, hata ve tekrar dene metinleri değişmez; `rooms.*` metinleri Ek A'da korunan metinler olarak listelenir.
  - `/[locale]/rooms` sayfası ortak `site-header` ve `site-footer` chrome'unu kullanır; kart, yükleme, boş, hata ve dolu durumları tasarım token'larıyla yeniden giydirilir. Kartlarda `data-testid^="price"` bulunmaz ve kart metninde `₺`, `€`, `$` geçmez; gerçek fotoğraf yerine yer tutucu kullanılır. `RoomCard` tıklanabilir değildir ve pointer cursor taşımaz.
  - Odalar sayfası locale'e özel title/description/canonical/hreflang/OG metadata'sı taşır (`B-06`).
  - `0002` Vitest testleri ve `@empty`/`@seeded` Playwright smoke testleri davranış değişikliği olmadan yeşildir.
  - **Hata matrisi testleri (`B-13`):** Mevcut 5xx testine ek olarak ağ abort, 10 sn timeout ve bozuk JSON → `rooms-error` + `rooms-retry` (teknik detay sızdırmadan); font isteği başarısız → sistem fallback'i (konsol hatası yok); `CREDITS.md`/hero asset eksik → build guard fail-fast. Kanıt: `AC-3-rooms-error-network.png`, `AC-3-rooms-error-timeout.png`, `AC-3-rooms-error-malformed.png`, `AC-1-font-fallback.log`, `AC-5-build-guard.log`.
  - **Kanıt:** `AC-3-rooms-tr-desktop.png`, `AC-3-rooms-en-desktop.png`, `AC-3-rooms-tr-mobile.png`, `AC-3-rooms-loading.png`, `AC-3-rooms-empty.png`, `AC-3-rooms-error.png`.
- **AC-4 (Erişilebilirlik: Reduced Motion, Klavye, Mobil Menü, Kontrast ve Semantik — `REQ-4`, `CLARIFY-2A`, `B-08`, `B-09`, `B-12`, `B-13`):**
  - `prefers-reduced-motion: reduce` altında `home-hero-video` DOM'da yoktur; tüm bölümler `data-motion="static"` taşır; Lenis/ScrollTrigger yüklenmez; pin/parallax ve mikro animasyon çalışmaz; tüm metinler ilk boyamada görünürdür.
  - Klavyeyle ilk Tab `skip-to-content` bağlantısını görünür biçimde odaklar; hedef `#main-content` ve `tabindex="-1"`dir. Tüm etkileşimli öğelerde görünür `:focus-visible` stili vardır. `< 1024 px` mobil menü `role="dialog"` + `aria-modal="true"` taşır, focus trap uygular, Escape/kapatma ile odak `mobile-nav-toggle` düğmesine döner, body scroll lock uygular ve route değişiminde kapanır.
  - `@axe-core/playwright` taramasında `/tr`, `/en`, `/tr/rooms`, `/en/rooms`, `/tr/credits`, `/en/credits` için kritik/ciddi ihlal sayısı sıfırdır; metin kontrastı 4.5:1, büyük metin/UI 3:1'dir.
  - Her sayfada tam bir `h1` vardır; başlık seviyeleri atlanmaz. Header `banner`, footer `contentinfo`, aktif nav `aria-current="page"` taşır. Dekoratif SVG/video `aria-hidden`, anlamlı görsellerde alternatif metin bulunur. Telefon/e-posta `tel:`/`mailto:` bağlantısıdır.
  - **Kanıt:** `AC-4-reduced-motion-home.png`, `AC-4-keyboard-focus-header.png`, `AC-4-mobile-menu-keyboard.png`, `AC-4-axe-report.md`.
- **AC-5 (Performans Bütçesi ve Ölçüm — `REQ-5`, `CLARIFY-5A`, `B-11`, `B-12`, `B-15`):**
  - Production build üzerinde Lighthouse mobil preset, Chrome headless ve 3 koşu median ile **LCP ≤ 2.5 s** ve **CLS ≤ 0.1** ölçülür; rapor `specs/done/evidence/0003/` altına konur. Rapordaki LCP öğesi hero video dosyası değildir; poster/metin LCP'yi taşır.
  - `/tr` rotasının ilk yük JavaScript'i, ağ idle'a kadar yüklenen tüm script chunk'larının `gzip -9` toplamı olarak **≤ 250 KB (256.000 B)**; `web/public/fonts` altındaki tüm `woff2` dosyaları **≤ 200 KB (204.800 B)**; WebM/MP4 ayrı ayrı **≤ 8 MB (8.388.608 B)**; poster ve OG **≤ 300 KB (307.200 B)**. CI'daki deterministik bütçe kontrolü aşımda kırmızı build üretir.
  - Animasyonlar scroll sırasında layout shift üretmez: Lighthouse CLS değeri bütçeyi aşmaz ve Playwright scroll sırasındaki `layout-shift` toplamı 0.1'i geçmez.
  - GSAP/Lenis `< 1024 px`'te ve reduced-motion'da yüklenmez; Motion/GSAP/Lenis ilk boyamadan sonra lazy yüklenir.
  - Ana sayfa çalışma zamanında üçüncü taraf font/medya/script isteği üretmez; yalnız aynı origin ve `http://localhost:8081` API çağrıları görülür.
  - **Kanıt:** `AC-5-lighthouse-mobile.html`, `AC-5-asset-and-bundle-budget.log`, `AC-5-runtime-requests.log`, `AC-5-scroll-cls.log`, `AC-5-build-guard.log`.
- **AC-6 (E2E, Video Kanıtı, Arşiv İzolasyonu ve Doküman Etkisi — `REQ-6`, `CLARIFY-4A`, `CLARIFY-6A`, `B-14`):**
  - `E2E_EVIDENCE_DIR` kanıt kökünü belirler; varsayılan `specs/done/evidence`'dır; ortak helper `0002` testlerini `.../0002/`, `@design` testlerini `.../0003/` altına yazar. `0003` koşumu sonrası `specs/done/evidence/0002/` altındaki dosyalar **SHA-256 manifest** ile bayt düzeyinde değişmemiştir.
  - Yeni `@design` testleri `@seeded` aşamasında koşar ve şunları doğrular: ana sayfa TR/EN, `home-hero`, `home-hero-video`, en az üç scroll adımı, 390×844 mobil poster davranışı, 767, 768, 1023 ve 1024 sınır davranışları, `reducedMotion: 'reduce'`, `save-data` init script simülasyonu (1280 ve 390), JavaScript kapalı içerik görünürlüğü, klavye ve mobil menü.
  - `AC-2-home-scroll-story.webm` zorunlu video kanıtıdır; `.webm` formatında, **≤ 15 sn** ve **≤ 5 MB (5.242.880 B)**dır; desktop scroll hikâyesini gösterir.
  - `0002`'nin `@empty` ve `@seeded` smoke testleri 4/4 yeşil kalır; CI pipeline yeşildir.
  - `docs/frontend.md`, `CREDITS.md`, `e2e/README.md`, `docs/testing.md` §3, `specs/done/README.md`, `.github/workflows/ci.yml` ve `.env.example` güncellenmiştir.
  - **Kanıt:** `AC-6-e2e-ci-summary.md` (test sayıları + CI run linki), `AC-2-home-scroll-story.webm`, `AC-6-0002-evidence-sha256.txt`.

## 6. Definition of Done

- [ ] Tüm AC'ler doğrulandı (test ve/veya görsel kanıt)
- [ ] Pipeline yeşil (lint + build + test + kritik akış smoke)
- [ ] Kod incelemesi onaylandı (Takım Yöneticisi)
- [ ] Davranış değiştiyse dokümanlar güncellendi
- [ ] Spec `specs/done/` klasörüne taşındı, görsel kanıtlar eklendi
- [ ] SCORECARD dolduruldu

## 7. SCORECARD

- **Spec revizyon sayısı:** 2
- **Düzeltme turu sayısı:**
- **Bulgu gerçek/gürültü oranı:**
- **Regresyon sayısı:**
- **Kaçan hata (escaped bug):**

## 8. Ek A — TR/EN Copy Deck (`B-03`)

> Bu deck, `0003` kapsamındaki tüm yeni metinlerin tek kaynağıdır. Developer yalnız bu metinleri tüketir; yeni pazarlama metni uydurmaz. Metin değişikliği gerekiyorsa Takım Yöneticisi onayı alınır.
> `0002`'nin sabit boş/hata/retry metinleri ve mevcut `rooms.*` metinleri aynen korunur (aşağıda listelenmiştir).
> Copy deck onaylanmadan görsel kanıtların "premium" kabulü yapılmaz (`B-03`).

### 8.1 Header, navigasyon ve skip link

| Anahtar | TR | EN |
|---|---|---|
| `siteTitle` (korunur) | Kapadokya Taş Konak Otel | Cappadocia Stone Mansion Hotel |
| `siteSubtitle` (korunur) | Nevşehir / Kapadokya Rezervasyon ve Misafir Deneyimi | Nevşehir / Cappadocia Booking and Guest Experience |
| `nav.home` | Ana Sayfa | Home |
| `nav.rooms` | Odalar | Rooms |
| `nav.ariaLabel` | Ana menü | Main menu |
| `skipToContent` | İçeriğe geç | Skip to content |
| `mobileMenu.open` | Menüyü aç | Open menu |
| `mobileMenu.close` | Menüyü kapat | Close menu |
| `languageLabel` (korunur) | Dil | Language |
| `switchTr` (korunur) | TR | TR |
| `switchEn` (korunur) | EN | EN |

### 8.2 Hero

| Anahtar | TR | EN |
|---|---|---|
| `home.badge` (korunur) | NEVŞEHİR · KAPADOKYA MİRAS KONAKLAMA | NEVŞEHİR · CAPPADOCIA HERITAGE STAY |
| `home.heroTitle` (korunur) | Tarihi Tüf Kayalara Oyulmuş Zamansız Bir Kapadokya Deneyimi | A Timeless Cappadocia Sanctuary Carved Into Ancient Tuff Stone |
| `home.heroDescription` (korunur) | Binlerce yıllık vadi manzarasına uyanacağınız kemerli mağara süitlerimiz ve doğal taş odalarımızla Kapadokya'nın büyüleyici ruhunu keşfedin. | Discover the enchanting spirit of Cappadocia in our vaulted cave suites and handcrafted stone rooms overlooking millennia-old valleys at sunrise. |
| `home.exploreRoomsCta` (korunur) | Oda Koleksiyonunu Keşfet | Explore Room Collection |
| `home.scrollHint` | Keşfetmek için kaydırın | Scroll to discover |

### 8.3 Story

| Anahtar | TR | EN |
|---|---|---|
| `home.story.eyebrow` | HİKÂYEMİZ | OUR STORY |
| `home.story.title` | Taşın Hafızası, Konuğun Huzuru | The Memory of Stone, the Calm of the Guest |
| `home.story.paragraph1` | Kapadokya'nın binlerce yıllık tüf kayaları, bu kez bir konak olarak nefes alıyor. Her kemer, her niş ve her taş yüzey, bölgenin ustaları tarafından elde işlenerek bugünkü sakin formuna kavuştu. | Cappadocia's millennia-old tuff rock breathes anew as a mansion. Every arch, niche, and stone surface was shaped by local craftsmen into the calm form it holds today. |
| `home.story.paragraph2` | Odalarımızda doğal taşın serinliği, el dokuması tekstillerin sıcaklığıyla buluşuyor. Sabah, vadiden yükselen ışıkla; akşam, taş şöminenin çıtırtısıyla ritmini değiştiren bir gün sizi bekliyor. | In our rooms, the coolness of natural stone meets the warmth of hand-woven textiles. Days here change rhythm with the light rising over the valley and the crackle of a stone fireplace at dusk. |
| `home.story.paragraph3` | Burada konaklamak, Kapadokya'nın ritmine karışmak demek: yavaş, derin ve hatırlanan. | To stay here is to join Cappadocia's rhythm: slow, deep, and remembered. |

### 8.4 Experience (4 kart)

> Mevcut üç `home.highlights.*` metni bu dört deneyim kartıyla değiştirilir; eski anahtarlar kaldırılır.

| Anahtar | TR | EN |
|---|---|---|
| `home.experience.eyebrow` | DENEYİM | EXPERIENCE |
| `home.experience.title` | Kapadokya'yı Hissetmenin Dört Yolu | Four Ways to Feel Cappadocia |
| `home.experience.intro` | Konaklamanızı bir kaçıştan çok, bölgenin dokusuna dokunan bir deneyime dönüştüren ayrıntılar. | Details that turn your stay into an experience touching the very texture of the region. |
| `home.experience.cards.shelter.title` | Taşın İçinde Bir Sığınak | A Sanctuary Within Stone |
| `home.experience.cards.shelter.description` | Geleneksel Kapadokya taş işçiliğiyle restore edilmiş, her biri kendine özgü dokuya sahip yaşam alanları. | Living spaces restored with traditional Cappadocian masonry, each with its own distinct texture. |
| `home.experience.cards.terrace.title` | Vadi Terası ve Balon Manzarası | Valley Terrace & Balloon Views |
| `home.experience.cards.terrace.description` | Gün doğumunda gökyüzünü dolduran balonları, vadinin sessizliğine eşlik eden bir fincan kahveyle izleyin. | Watch the sunrise balloons fill the sky with a cup of coffee beside the silence of the valley. |
| `home.experience.cards.comfort.title` | Taş Oyma Jakuzi ve Şömine | Carved Jacuzzi & Stone Fireplace |
| `home.experience.cards.comfort.description` | Tüf kayanın içine oyulmuş özel jakuzinizde günün yorgunluğunu bırakın; akşamı taş şöminenin başında tamamlayın. | Leave the day's tiredness in your private jacuzzi carved into tuff rock, then complete the evening by the stone fireplace. |
| `home.experience.cards.breakfast.title` | Kapadokya Kahvaltısı | Cappadocia Breakfast |
| `home.experience.cards.breakfast.description` | Bölgenin köy ürünleri, ev yapımı reçeller ve taze ekmekle başlayan, uzun ve telaşsız bir sabah. | A long, unhurried morning that begins with village produce, homemade preserves, and fresh bread. |

### 8.5 Rooms CTA

| Anahtar | TR | EN |
|---|---|---|
| `home.roomsCta.eyebrow` | ODALAR | ROOMS |
| `home.roomsCta.title` | Her Biri Kendi Hikâyesini Taşıyan Odalar | Rooms That Carry Their Own Story |
| `home.roomsCta.body` | Kemerli mağara süitlerinden vadi manzaralı taş odalara uzanan koleksiyonumuzu keşfedin. | Discover our collection, from arched cave suites to stone rooms overlooking the valley. |
| `home.roomsCta.button` | Oda Koleksiyonunu Keşfet | Explore Room Collection |

### 8.6 Contact

| Anahtar | TR | EN |
|---|---|---|
| `home.contact.eyebrow` | KONUM | LOCATION |
| `home.contact.title` | Kapadokya'nın Kalbinde | In the Heart of Cappadocia |
| `home.contact.body` | Nevşehir'in tarihi dokusunda, vadiye bakan sakin bir konum. Yol tarifi ve konaklama sorularınız için resepsiyonumuzla iletişime geçebilirsiniz. | A quiet location overlooking the valley within Nevşehir's historic texture. Contact our front desk for directions and stay enquiries. |
| `home.contact.addressLabel` | Adres | Address |
| `home.contact.phoneLabel` | Telefon | Phone |
| `home.contact.emailLabel` | E-posta | Email |
| `home.contact.address` | Örnek Mahallesi, Taş Konak Caddesi No: 1, Nevşehir / Kapadokya | Sample District, Stone Mansion Street No: 1, Nevşehir / Cappadocia |
| `home.contact.phone` | +90 000 000 00 00 | +90 000 000 00 00 |
| `home.contact.email` | ornek@example.com | ornek@example.com |

### 8.7 Footer

| Anahtar | TR | EN |
|---|---|---|
| `footer.tagline` | Kapadokya'nın taş mirasında, sade ve hatırlanan bir konaklama. | A quiet, memorable stay within Cappadocia's stone heritage. |
| `footer.copyright` | © 2026 Kapadokya Taş Konak Otel. Tüm hakları saklıdır. | © 2026 Cappadocia Stone Mansion Hotel. All rights reserved. |
| `footer.creditsLabel` | Kaynaklar ve Lisanslar | Credits & Licenses |
| `footer.contactLabel` | İletişim | Contact |

### 8.8 Credits

| Anahtar | TR | EN |
|---|---|---|
| `credits.title` | Kaynaklar ve Lisanslar | Credits & Licenses |
| `credits.intro` | Bu sitede kullanılan görsel ve tipografik kaynakların lisans bilgileri aşağıdadır. | License information for the visual and typographic assets used on this site is listed below. |
| `credits.mediaHeading` | Görsel Medya | Visual Media |
| `credits.typographyHeading` | Tipografi | Typography |
| `credits.contentNoteHeading` | İçerik Notu | Content Note |
| `credits.contentNote` | İletişim bilgileri temsilîdir; gerçek adres, telefon ve e-posta ile değiştirilmelidir. | Contact details are placeholders and must be replaced with the real address, phone, and email. |
| `credits.videoEntry` | Hero video — Pexels, video 30653318; Pexels License. | Hero video — Pexels, video 30653318; Pexels License. |
| `credits.fontCormorant` | Cormorant Garamond — SIL Open Font License 1.1. | Cormorant Garamond — SIL Open Font License 1.1. |
| `credits.fontSourceSans` | Source Sans 3 — SIL Open Font License 1.1. | Source Sans 3 — SIL Open Font License 1.1. |

### 8.9 Metadata (title / description / OG alt)

| Rota | TR title | EN title |
|---|---|---|
| Home | Kapadokya Taş Konak Otel \| Butik Mağara ve Taş Odalar | Cappadocia Stone Mansion Hotel \| Boutique Cave & Stone Rooms |
| Rooms | Oda ve Süit Koleksiyonu \| Kapadokya Taş Konak Otel | Rooms & Suites \| Cappadocia Stone Mansion Hotel |
| Credits | Kaynaklar ve Lisanslar \| Kapadokya Taş Konak Otel | Credits & Licenses \| Cappadocia Stone Mansion Hotel |

| Rota | TR description | EN description |
|---|---|---|
| Home | Nevşehir Kapadokya'da tarihi tüf kayalara oyulmuş butik taş konak. Kemerli mağara süitleri, vadi terası ve sakin bir konaklama deneyimi. | A boutique stone mansion carved into ancient tuff in Nevşehir, Cappadocia. Arched cave suites, valley terraces, and a quietly refined stay. |
| Rooms | Kemerli mağara süitleri ve vadi manzaralı taş odalar. Kapadokya Taş Konak Otel'in oda koleksiyonunu ve olanaklarını keşfedin. | Arched cave suites and valley-view stone rooms. Explore the room collection and amenities of Cappadocia Stone Mansion Hotel. |
| Credits | Kapadokya Taş Konak Otel web sitesinde kullanılan görsel medya ve tipografi kaynaklarının lisans bilgileri. | License information for the visual media and typography used on the Cappadocia Stone Mansion Hotel website. |

| Rota | `og:image:alt` TR | `og:image:alt` EN |
|---|---|---|
| Home | Kapadokya Taş Konak Otel — taş kemerler ve vadi manzarası | Cappadocia Stone Mansion Hotel — stone arches and valley views |
| Rooms | Kapadokya Taş Konak Otel oda koleksiyonu | Cappadocia Stone Mansion Hotel room collection |
| Credits | Kapadokya Taş Konak Otel kaynaklar ve lisanslar | Cappadocia Stone Mansion Hotel credits and licenses |

> **Not 1:** Title tablosundaki `\|` yalnız Markdown tablo kaçışıdır; uygulamada düz `|` karakteri kullanılır.
> **Not 2:** Description uzunlukları trim sonrası Unicode code point olarak doğrulanmıştır: Home TR 136 / EN 139; Rooms TR 125 / EN 124; Credits TR 107 / EN 107. Tümü 70–160 aralığındadır.
> **Not 3:** `og:image:alt` metinleri tek OG görseli için locale'e özel alternatif metindir; görsel üzerinde metin yer almaz.

### 8.10 Rooms sayfası — korunan metinler (`0002`)

| Anahtar | TR | EN |
|---|---|---|
| `rooms.backToHome` | Ana Sayfaya Dön | Back to Home |
| `rooms.collectionBadge` | ODA KATEGORİLERİ VE OLANAKLAR | ROOM CATEGORIES & AMENITIES |
| `rooms.pageTitle` | Oda ve Süit Koleksiyonumuz | Our Room & Suite Collection |
| `rooms.pageSubtitle` | Kapadokya'nın doğal kaya oluşumları ve tarihi taş mimarisiyle bütünleşen konaklama seçeneklerimizi inceleyin. | Explore our accommodation categories blending seamlessly with Cappadocia's natural rock formations and historic stone architecture. |
| `rooms.loadingText` | Oda koleksiyonumuz hazırlanıyor... | Preparing our room collection... |
| `rooms.emptyTitle` | Henüz yayınlanmış oda bulunmuyor. | No published rooms are available yet. |
| `rooms.emptySubtitle` | Yeni oda ve süit kategorilerimiz yayına alındığında bu vitrinde yer alacaktır. | Our room and suite categories will appear here as soon as they are published. |
| `rooms.errorMessage` | Oda bilgileri şu anda yüklenemedi. Lütfen tekrar deneyin. | Room information could not be loaded right now. Please try again. |
| `rooms.retryButton` | Tekrar Dene | Try Again |
| `rooms.capacityBadge` | En fazla {count} misafir | Up to {count} guests |
| `rooms.amenitiesHeading` | Oda Olanakları | Room Amenities |
| `rooms.placeholderBadge` | Kapadokya Taş Mimari Koleksiyonu | Cappadocia Stone Architecture Collection |
| `rooms.placeholderCaption` | Fotoğraf galerisi ve 360° sanal tur yakında eklenecektir | Photo gallery and 360° virtual tour coming soon |

## 9. Ek B — Karar Kaydı (`CLARIFY-*` ve `B-*`)

> Tarih: 2026-10-25. Bu tablo, spec metnine işlenen kararların izlenebilirliğini sağlar; ayrıntılı gerekçeler `specs/0003-clarification-brief.md` ve ilgili Takım Yöneticisi karar kayıtlarındadır.

| Kod | Karar özeti | Etkilenen REQ/AC |
|---|---|---|
| `CLARIFY-1A` | Desktop WebM+MP4, mobil/save-data poster; WebM/MP4 ≤ 8 MB, poster ≤ 300 KB | REQ-2, AC-2 |
| `CLARIFY-2A` | Pin/parallax ≥ 1024 px; mobil/tablet statik; reduced-motion'da tüm hareket durur | REQ-2, REQ-4, AC-2, AC-4 |
| `CLARIFY-3A` | Ana sayfada canlı oda teaser'ı yok; yalnız CTA ve statik atmosfer metni | REQ-2, AC-2 |
| `CLARIFY-4A` | `0002` testid/metin sözleşmesi korunur; kanıt dizini parametrik; dil geçişi sayfa bağlamını korur | REQ-3, REQ-6, AC-3, AC-6 |
| `CLARIFY-5A` | LCP/CLS Lighthouse mobil median ile; CI'da deterministik dosya/bundle kapıları; Lighthouse gate yok | REQ-5, AC-5 |
| `CLARIFY-6A` | Desktop scroll hikâyesi için zorunlu `.webm` kanıtı | REQ-6, AC-6 |
| `CLARIFY-7A` | Locale metadata + canonical/hreflang/OG; mevcut hafif i18n altyapısı korunur | REQ-2, AC-2 |
| `CLARIFY-8A` | Tipografik wordmark; temsilî iletişim bilgileri; copy deck onayı | REQ-2, AC-2 |
| `CLARIFY-9A` | Mevcut bileşen API'leri korunur; Radix yalnız gerektiğinde; 390×844 + 1280×800 kanıt | REQ-1, AC-1 |
| `B-01` | Görsel kalite/premium developer sorumluluğunda; takılınca yöneticiyle karar; ek tur serbest; D18 sınırları korunur | REQ-1, AC-1; §3 |
| `B-02` | Cormorant Garamond 600 + Source Sans 3 400/600; OFL; Latin Extended; 3 woff2; ≤ 200 KB | REQ-1, AC-1 |
| `B-03` | TR/EN copy deck analist tarafından hazırlanır ve onaylanır | REQ-2, AC-2, Ek A |
| `B-04` | Tam 2 pinli sahne (`home-story`, `home-experience`); 768–1023 pinsiz; ±40 px yalnız dekoratif parallax | REQ-2, AC-2 |
| `B-05` | Hero 1920×1080, 8–12 sn, 30 fps, sessiz; poster her zaman DOM'da; `preload="none"`; ffprobe kanıtı | REQ-2, AC-2, AC-5 |
| `B-06` | Tüm rotalarda metadata; x-default ilgili TR karşılığı; tek OG; `metadataBase` | REQ-2, REQ-3, AC-2, AC-3 |
| `B-07` | Ayrı desktop/mobile testid'leri; tek `web-site-title`; footer/home contact testid'leri | AC-2, AC-3, AC-4, AC-6 |
| `B-08` | `< 1024 px` modal mobil menü; focus trap; scroll lock; route değişiminde kapanma | REQ-4, AC-4 |
| `B-09` | Wordmark link; h1 sayfa başlığı; skip link `#main-content`; landmark ve `aria-current` | REQ-2, REQ-4, AC-2, AC-4 |
| `B-10` | `CREDITS.md` tek kaynak; credits sayfası bundan üretilir; ortak chrome + metadata + axe | REQ-2, AC-2, AC-4 |
| `B-11` | Byte birimleri, gzip-9 JS toplamı, 3 koşu Lighthouse median, scroll CLS ölçümü | REQ-5, AC-5 |
| `B-12` | GSAP pin/parallax, Motion reveal/mikro, Lenis desktop; lazy; reduced-motion'da yüklenmez | REQ-2, REQ-4, REQ-5, AC-2, AC-4, AC-5 |
| `B-13` | Video/font/API/CREDITS hata ve fallback matrisi; 10 sn timeout; statik sayfa istisnası | REQ-2, REQ-3, REQ-4, AC-2, AC-3 |
| `B-14` | Kanıt kökü + helper, tag'ler, save-data init script, axe, video/ffmpeg, SHA-256 manifest | REQ-6, AC-6 |
| `B-15` | Viewport/description/byte/pin/font/video sınır tablosu | AC-2, AC-5, AC-6 |
| `B-16` | Tam shadcn migrasyonu yok; Radix yalnız MobileNav; `Section` tek primitif; `RoomCard` tıklanamaz | REQ-1, REQ-3, AC-1, AC-3 |
| `B-17` | Bu karar kaydı tablosu | Künye/Context |
