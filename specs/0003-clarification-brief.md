# Spec 0003 — Karar ve Analist Brief'i

> **Amaç:** `specs/0003-ui-design-system.md` uygulanmaya başlamadan önce geliştiricinin varsayım yapmak zorunda kalacağı boşlukları tek yerde toplamak; her biri için karar + gerekçe sunmak.
>
> **Kapsam:** Yalnızca spec 0003'tür. Bu dosya bir spec, plan veya kod değildir; onaylanan maddeler analist tarafından spec rev.1'e metin olarak işlenir. Tüm maddeler karara bağlanmıştır; geliştirici onaylanmış kararlar dışında varsayım yapmamalıdır.
>
> **Durum:** Takım Yöneticisi kararları tamamlandı (2026-10-25). Analistin bu kararlara göre spec rev.1'i hazırlaması bekleniyor.
>
> **Analist için:** B-01 developer delegasyonunu ve B-02–B-17 A kararlarını spec rev.1'e işle. B-03 kapsamındaki TR/EN copy deck'ini sen hazırla. Ayrıntılı iş listesi §4'tedir.
> **Tarih:** 2026-10-25
> **İnceleme girdisi:** Spec 0003 (Taslak), `docs/frontend.md` (İSKELET), `docs/architecture.md` §10 D3/D11/D18, `docs/testing.md` §3–§4, mevcut `web`/`e2e` kodu.
>
> **Satır referansları:** Aksi belirtilmedikçe `specs/0003-ui-design-system.md` satır numaralarıdır.

## 0. Karar özeti ve kullanım

- **B-01:** Takım Yöneticisi görsel kaliteyi ve "premium" şartını **developer'a devretti**. Developer görsel yönü belirler, görselleri ayarlar; takıldığı noktalarda Takım Yöneticisi'ne sorar ve birlikte karar verilir. Ek görsel tur serbesttir.
- **B-02–B-17:** A seçenekleri **onaylandı**.
- Bu dosyadaki maddeler artık tartışma sorusu değil, **karar kaydıdır**; her maddenin "Karar" satırı bağlayıcıdır.
- Analist, §4'teki kontrol listesini uygulayarak spec rev.1'i hazırlar. Spec onaylanmadan kod yazılmaz (Altın Kural 1).
- Onaylanan maddelerden biri spec metnine işlenmezse geliştirici o konuda varsayım yapmamalı, analiste geri dönmelidir.

## 1. Özet tablo

| ID | Konu | Kategori | Bloklayıcı? | Karar |
|---|---|---|---|---|
| B-01 | "Premium" görsel yön ve token değerleri | Belirsiz kabul kriteri | **Evet** | **Developer'a devredildi** — görsel kalite developer'da; takılınca yöneticiye sorulacak; ek tur serbest |
| B-02 | Font ailesi, ağırlıklar ve lisans | Eksik sınır / içerik | **Evet** | **A onaylandı** — Cormorant Garamond + Source Sans 3 (OFL) |
| B-03 | Yeni bölümlerin TR/EN metinleri | Eksik içerik | **Evet** | **A onaylandı** — analist copy deck'i hazırlayacak |
| B-04 | Pinli sahne sayısı ve tablet davranışı | Belirsiz AC / sınır | **Evet** | **A onaylandı** — 2 sahne; 768–1023 pin yok |
| B-05 | Hero asset boyut/süre/kodlayıcı ve poster katmanı | Eksik sınır | **Evet** | **A onaylandı** — 1920×1080, 8–12 sn, 30 fps, sessiz |
| B-06 | Metadata kapsamı, x-default ve OG görseli | Belirsiz AC | **Evet** | **A onaylandı** — tüm rotalar; x-default TR karşılığı; tek OG |
| B-07 | Yeni bileşenlerin testid sözleşmesi | Belirsiz AC / test | **Evet** | **A onaylandı** — ayrı mobile testid'leri; tek `web-site-title` |
| B-08 | MobileNav kırılımı ve odak modeli | Belirsiz AC / a11y | **Evet** | **A onaylandı** — `<1024px` modal menü + focus trap |
| B-09 | h1 hiyerarşisi, wordmark ve skip link | Belirsiz AC / a11y | **Evet** | **A onaylandı** — wordmark link; h1 sayfa başlığı; `#main-content` |
| B-10 | Credits sayfasının kaynağı ve chrome'u | Belirsiz AC | **Evet** | **A onaylandı** — `CREDITS.md` tek kaynak; ortak chrome |
| B-11 | Performans bütçelerinin ölçüm yöntemi | Eksik sınır / ölçüm | **Evet** | **A onaylandı** — deterministik gzip/font/Lighthouse tanımları |
| B-12 | Animasyon kütüphanesi görev dağılımı | Belirsiz AC / performans | **Evet** | **A onaylandı** — GSAP pin, Motion mikro, Lenis desktop |
| B-13 | Tanımsız hata ve fallback durumları | Tanımsız hata | **Evet** | **A onaylandı** — video/font/API/credits hata matrisi |
| B-14 | E2E kanıt ve test altyapısı | Belirsiz AC / kanıt | **Evet** | **A onaylandı** — tag, save-data, axe, video, checksum |
| B-15 | Sınır değerleri ve birimler | Eksik sınır | **Evet** | **A onaylandı** — boundary tablosu |
| B-16 | shadcn/Radix kapsamı ve bileşen yönetimi | Belirsiz kapsam | Hayır | **A onaylandı** — tam migrasyon yok; Radix yalnız gerektiğinde |
| B-17 | `CLARIFY-*` kararlarının izlenebilirliği | Doküman boşluğu | Hayır | **A onaylandı** — spec'e karar kaydı tablosu |

> **Toplam:** 15 bloklayıcı, 2 bloklayıcı olmayan madde — tümü karara bağlandı.

---

## 2. Bloklayıcı maddeler

> Aşağıdaki başlıklar ilk incelemedeki soruyu tanımlar; her maddenin bağlayıcı sonucu **"Karar (2026-10-25)"** satırındadır. B-01 artık developer'a devredilmiş bir karardır; diğerleri A seçeneğiyle onaylanmıştır.

### B-01 — Görsel kalite ve "premium" yönü developer'a devredildi

- **Kategori:** Takım Yöneticisi kararı (artık soru değil).
- **Spec yeri:** §1 Intent (`:22`), §3 kabul edilen risk (`:75`), AC-1 (`:105-106`).
- **Karar (2026-10-25):** Developer, arayüzün görsel kalitesinden ve "premium" şartından sorumludur; arayüzün güzel görünmesi için yüksek özen gösterir. Görsel yönü, token değerlerini ve görsel seçim/düzenlemelerini kendisi belirler; görselleri ayarlayabilir. Geliştirme sırasında takıldığı veya iki anlamlı seçenek arasında kaldığı noktalarda Takım Yöneticisi'ne sorar; tasarıma birlikte karar verilir. Ek görsel tur serbesttir.
- **Analistin spec'e işleyeceği:** §3'teki **"en fazla 2 revizyon turu"** ifadesi kaldırılır veya bu kararla geçersiz kılınır; yerine "görsel kalite developer sorumluluğunda; belirsizlikte Takım Yöneticisi ile birlikte karar; ek tur serbest" yazılır. AC-1'deki token tablosu zorunluluğu korunur; token değerlerini developer belirler ve `docs/frontend.md`'ye yazar. Görsel kabul için önceden moodboard/token onayı aranmaz.
- **Sınır:** B-02'deki font eşleşmesi ve B-05'teki hero asset ölçüleri/kodlayıcı kuralları onaylı A kararlarıdır; B-01 delegasyonu bu teknik sınırların dışına taşmaz. D18'deki Pexels 30653318 hero video kaynağı, lisans ve dosya boyutu kuralları varsayılan olarak korunur; developer poster, crop, renk, yerleşim ve yardımcı görselleri serbestçe ayarlayabilir. Hero video kaynağını değiştirmek isterse bunu ayrı bir D18 kararı olarak Takım Yöneticisi'ne sunar.
- **Çalışma biçimi:** Developer takıldığında 2 seçenek + kendi önerisini ve gerekçesini getirir (Altın Kural 4); yönetici karar verir.
- **Gerekçe:** "Premium" görsel kalite, önceden dondurulmuş bir moodboard'dan çok iteratif değerlendirmeyle ortaya çıkar. Developer uygulama bağlamında en hızlı geri bildirimi üretir; takıldığı noktada yöneticiyle birlikte karar vermek hem hızı hem kaliteyi korur. Ek tur serbestliği, 2 tur sınırının yaratacağı yapay baskıyı ortadan kaldırır.
- **Etki:** AC-1, AC-2, AC-3; spec §3 risk bölümü; görsel kabul süreci.
- **Nihai durum:** **DELEGATED — developer (2026-10-25).**

### B-02 — Font ailesi, ağırlıklar ve lisans belirsiz

- **Kategori:** Eksik sınır / eksik içerik.
- **Spec yeri:** REQ-1 tipografi (`:28`), AC-1 (`:107`), `CREDITS.md` gereksinimi (`:78`, `:116`).
- **Soru:** Hangi iki font ailesi, hangi ağırlıklar ve hangi lisansla kullanılacak? "Toplam font ağırlığı ≤ 200 KB" tam olarak hangi dosyaların toplamı?
- **Neden açık:** Spec yalnızca "en fazla iki aile (display + gövde)" diyor; font adı, ağırlık sayısı, subset ve lisans yok. `CREDITS.md`'nin font lisanslarını listelemesi isteniyor, ancak font seçilmeden bu madde tamamlanamaz.
- **Seçenekler:**
  - **A)** **Cormorant Garamond 600** (display) + **Source Sans 3 400/600** (gövde); ikisi de SIL OFL; Latin Extended subset; toplam 3 `woff2`.
  - **B)** Fraunces (display) + Manrope (gövde); benzer lisans profili.
  - **C)** Geliştirici iki alternatif sunar; yönetici seçer.
- **Önerim:** **A.** Fontlar `web/public/fonts/` altında self-host edilir; yalnız kullanılan ağırlıklar yüklenir; toplam dosya boyutu 200 KB'ı aşarsa ağırlık veya subset azaltılır. `CREDITS.md`'de aile adı, lisans, kaynak URL ve ağırlıklar yer alır.
- **Gerekçe:** Cormorant'ın yüksek kontrastlı serif yapısı taş/mağara kimliğine uygun, Source Sans 3 Türkçe karakterleri ve UI okunabilirliğini karşılıyor; ikisi de OFL olduğu için lisans kaydı nettir. Ayrıca font seçimi B-01'deki tipografi kararının girdisidir.
- **Etki:** AC-1, AC-2; `CREDITS.md`; görsel kimlik.
- **Karar (2026-10-25):** **ONAYLANDI — A.**

### B-03 — Yeni bölümlerin TR/EN metinleri yok

- **Kategori:** Eksik içerik.
- **Spec yeri:** REQ-2 bölüm listesi (`:30`), içerik kuralı (`:35-36`), AC-2 (`:111-116`), AC-3 (`:120-121`), metadata (`:115`).
- **Soru:** Hero, hikâye, deneyim, CTA, konum/iletişim, footer, credits ve metadata için nihai TR/EN metinler nerede? Hangi metinler mevcut `tr.json`/`en.json`'dan aynen korunacak, hangileri yeni yazılacak?
- **Neden açık:** Spec bölümleri ve iletişim yer tutucularını veriyor; ancak hikâye/deneyim bölümlerinin başlık ve gövde metinleri, nav/footer/credits etiketleri, copyright ve metadata title/description metinleri yok. Mevcut sözlükte yalnızca hero ve üç highlight var.
- **Seçenekler:**
  - **A)** Analist, spec ekinde TR/EN copy deck hazırlar; Takım Yöneticisi onaylar. Geliştirici yalnızca onaylı metni tüketir.
  - **B)** Geliştirici taslak metin yazar; yönetici düzeltir. İçerik kararı koda taşınır, tur sayısı artar.
  - **C)** Yalnızca mevcut hero/highlight metinleri kullanılır; yeni bölümler eksik kalır.
- **Önerim:** **A.** Copy deck en az şu blokları içermeli: header nav ve skip link; hero badge/title/description/CTA; story başlık + 2–3 paragraf; experience başlık + 4 kart; CTA başlık/gövde/düğme; contact başlık/gövde; footer tagline/copyright/credits etiketi; credits başlık/giriş; home/rooms/credits title + description + OG alt.
- **Gerekçe:** Metin ürün ve hukuki içeriktir; geliştiricinin Türkçe/İngilizce pazarlama metni uydurması doğru değil. Ayrıca E2E testleri sabit metinleri doğrulayacağı için metinlerin önceden kesinleşmesi gerekir. `0002`'nin sabit boş/hata/tekrar dene metinleri **aynen** korunmalıdır.
- **Etki:** AC-2, AC-3, AC-4 (skip link/menu etiketleri), metadata; tüm görsel kanıtlar.
- **Karar (2026-10-25):** **ONAYLANDI — A.**

### B-04 — Pinli sahne sayısı ve tablet davranışı belirsiz

- **Kategori:** Belirsiz kabul kriteri / eksik sınır.
- **Spec yeri:** REQ-2 scroll (`:32`), AC-2 (`:114`), AC-4 (`:125`).
- **Soru:** "2–3 pinli sahne" tam olarak kaç sahne ve hangi bölümler? 768–1023 px aralığında pin/parallax var mı, yok mu?
- **Neden açık:** Spec ≥1024 px için 2–3 sahne, ≤767 px için statik diyor; 768–1023 px tanımsız. Hangi bölümlerin `data-motion="pinned"` olacağı da yazmıyor. AC-2 testi 2–3 aralığını tek başına doğrulayamaz.
- **Seçenekler:**
  - **A)** Tam **2** pinli sahne: `home-story` ve `home-experience`. Hero, `home-rooms-cta` ve `home-contact` statik. 768–1023 px: video açık, pin/parallax yok, yalnız hafif reveal.
  - **B)** 3 pinli sahne: `home-story`, `home-experience`, `home-rooms-cta`; tablet yine pinsiz.
  - **C)** 2–3 aralığı geliştiriciye bırakılır; test yalnızca en az 2 sahne arar.
- **Önerim:** **A.** `data-motion` haritası spec'e yazılmalı: hero `static`, story `pinned`, experience `pinned`, rooms-cta `static`, contact `static`; reduced-motion altında hepsi `static`. Parallax yalnız dekoratif arka plan katmanında, en fazla ±40 px; metin parallax'ı yok.
- **Gerekçe:** Hero'yu pinlemek LCP'yi ve ilk ekranı riske atar; 2 sahne "sinematik" hedefi karşılar ve scroll süresini kısa tutar. Tablet için açık bir kural, 768/1024 sınırında beklenmeyen davranışı ve test belirsizliğini önler.
- **Etki:** AC-2, AC-4; scroll kanıtı ve performans.
- **Karar (2026-10-25):** **ONAYLANDI — A.**

### B-05 — Hero asset boyutu, süresi ve poster katmanı belirsiz

- **Kategori:** Eksik sınır / belirsiz AC.
- **Spec yeri:** REQ-2 hero (`:31`), AC-2 (`:112-113`), AC-5 (`:131-132`), `CREDITS.md` (`:78`).
- **Soru:** Video çözünürlüğü, en-boy oranı, süresi, fps'i, bitrate/kalite hedefi ve poster boyutu ne? Poster her zaman DOM'da mı, video üstüne mi biniyor? Video yüklenemezse/autoplay engellenirse ne olur?
- **Neden açık:** Yalnızca "WebM/VP9 + MP4/H.264 + WebP poster" ve dosya üst sınırları var; 8 MB bütçesinin içini dolduracak teknik hedef yok. Poster ile videonun katman ilişkisi ve hata durumu tanımsız.
- **Seçenekler:**
  - **A)** 1920×1080 (16:9), 8–12 sn kusursuz döngü, 30 fps, ses kanalı yok; VP9 CRF ~32, H.264 CRF ~23; poster 1920×1080 WebP q80. Poster her zaman DOM'da (`home-hero-poster`), video ilk boyamadan sonra üstüne eklenir; `preload="none"`.
  - **B)** 1280×720; daha küçük dosya, 1280 px ekranda daha yumuşak görüntü.
  - **C)** Geliştirici asset'i indirip uygun değeri seçer; sınırlar yalnız dosya boyutu olur.
- **Önerim:** **A.** Video `canplay` olduğunda görünür; poster LCP'yi taşır. Video yüklenemez, autoplay reddedilir veya kaynak 404 olursa video DOM'dan kaldırılır/gizlenir, poster görünür kalır ve konsola hata basılmaz. Poster dekoratiftir (`alt=""`, `aria-hidden="true"`). `ffprobe` ile ses akışı olmadığı, süre/kodlayıcı ve çözünürlük kanıtlanır.
- **Gerekçe:** 8 MB ve LCP bütçesi ancak net çözünürlük/süre/kalite hedefiyle planlanabilir; posterin her zaman DOM'da olması hem mobil/save-data hem de video hatası için tek ve basit fallback sağlar. 1080p, 1280×800 masaüstünde premium algıyı korur.
- **Etki:** AC-2, AC-5; `CREDITS.md`; video kanıtı.
- **Karar (2026-10-25):** **ONAYLANDI — A.**

### B-06 — Metadata kapsamı, x-default ve OG görseli belirsiz

- **Kategori:** Belirsiz kabul kriteri.
- **Spec yeri:** REQ-2 metadata (`:34`), AC-2 (`:115`).
- **Soru:** Metadata yalnız ana sayfa için mi, yoksa `/[locale]/rooms` ve `/[locale]/credits` için de mi? `x-default` her sayfada `/tr`'ye mi, yoksa sayfanın TR karşılığına mı gider? OG görseli tek mi, locale başına mı? Title/description metinleri nerede?
- **Neden açık:** AC-2 ana sayfa başlığı altında konuşuyor; odalar/credits için metadata şartı yok. `x-default → /tr` ifadesi alt sayfalarda yanlış sonuç verir (ör. `/en/rooms` için `/tr`). OG görselinin sayısı ve alt metni belirsiz.
- **Seçenekler:**
  - **A)** Üç rota için de (home, rooms, credits) locale'e özel title/description; canonical self; `hreflang` `tr`, `en`, `x-default` → ilgili sayfanın TR karşılığı; tek, metinsiz 1200×630 OG görseli + locale'e özel `og:image:alt`; `twitter:card=summary_large_image`.
  - **B)** Yalnız ana sayfa metadata'sı; alt sayfalar Next.js varsayılanında kalır.
  - **C)** Her sayfa/locale için ayrı OG görseli (6 görsel); daha fazla asset ve bakım.
- **Önerim:** **A.** `metadataBase` `NEXT_PUBLIC_SITE_URL`'den üretilir; sondaki `/` normalize edilir; env yoksa `http://localhost:3000`; `.env.example`'a `NEXT_PUBLIC_SITE_URL` eklenir. Title/description metinleri B-03 copy deck'inden gelir. `x-default` örnekleri: home `/tr`, rooms `/tr/rooms`, credits `/tr/credits`.
- **Gerekçe:** SEO tutarlılığı ve doğru dil hedeflemesi için alt sayfalar da metadata taşımalı. Tek metinsiz OG görseli 300 KB bütçesini ve bakımı korur; metin görselde olmadığı için TR/EN ayrımı gerekmez.
- **Etki:** AC-2 (ve önerilen kapsamla AC-3/AC-6); SEO.
- **Karar (2026-10-25):** **ONAYLANDI — A.**

### B-07 — Yeni bileşenlerin testid sözleşmesi eksik

- **Kategori:** Belirsiz kabul kriteri / test sözleşmesi.
- **Spec yeri:** AC-2 (`:111-116`), AC-3 (`:120`), AC-4 (`:126`), AC-6 (`:138`).
- **Soru:** MobileNav, skip link, footer copyright ve home contact için testid'ler ne? Desktop ve mobile navigasyon aynı `site-nav-*` testid'lerini mi kullanacak? `web-site-title` sayfada kaç kez geçecek?
- **Neden açık:** AC-2 yalnız `site-header`, `web-site-title`, `site-nav-home`, `site-nav-rooms`, locale switch'leri ve footer alanlarını sayıyor. AC-4 klavye/mobil menü kanıtı istiyor ama menü düğmesi/paneli için testid yok. Aynı testid'in desktop ve mobile DOM'da iki kez bulunması Playwright strict mode'da kırılır.
- **Seçenekler:**
  - **A)** Testid tablosu spec'e eklenir; desktop `site-nav-*`, mobile `mobile-nav-*`; tek `web-site-title`; dil switch'i her genişlikte header'da kalır; home contact alanları `home-contact-*`, footer alanları `footer-*`.
  - **B)** Aynı testid'ler iki navigasyonda kullanılır; testler `:visible` ile ayıklanır (kırılgan).
- **Önerim:** **A.** Önerilen ek testid'ler: `skip-to-content`, `site-main`, `mobile-nav-toggle`, `mobile-nav`, `mobile-nav-home`, `mobile-nav-rooms`, `footer-copyright`; home contact için `home-contact-address`, `home-contact-phone`, `home-contact-email`. `web-site-title` yalnız ortak header'daki wordmark linkinde bulunur.
- **Gerekçe:** AC-4 klavye testi ve AC-6 E2E kanıtı sabit seçicilere dayanır; duplicate testid flaky/kırık test üretir. Dil switch'inin header'da kalması `0002`'nin `locale-switch-*` sözleşmesini de korur.
- **Etki:** AC-2, AC-3, AC-4, AC-6; E2E bakımı.
- **Karar (2026-10-25):** **ONAYLANDI — A.**

### B-08 — MobileNav kırılımı ve odak modeli belirsiz

- **Kategori:** Belirsiz kabul kriteri / erişilebilirlik.
- **Spec yeri:** REQ-4 klavye (`:39`), AC-4 (`:126`), AC-6 (`:138`).
- **Soru:** MobileNav hangi genişlikte devreye giriyor? Tam ekran modal mı, yoksa açılır disclosure mı? Focus trap, body scroll lock, dışarı tıklama ve route değişiminde kapanma var mı?
- **Neden açık:** Spec yalnız `aria-expanded`, Escape ve odağın geri dönmesini söylüyor; kırılım noktası ve odak tuzağı yok. Tam ekran menüde focus trap yoksa klavye kullanıcısı arka plana kaçabilir.
- **Seçenekler:**
  - **A)** `<1024 px` mobile nav; tam ekran `role="dialog"` + `aria-modal="true"`, focus trap, Escape/close ile odak menüyü açan düğmeye döner, body scroll lock, route değişiminde kapanır. Dil switch'i header'da görünür kalır.
  - **B)** `<768 px` mobile nav; aynı modal davranışı.
  - **C)** Basit disclosure; focus trap yok, yalnız Escape ve dışarı tıklama.
- **Önerim:** **A.** 1024 px kırılımı scroll pin eşiğiyle hizalanır; uzun Türkçe wordmark + 4 öğe 768–1023 aralığında sıkışmaz. Tam ekran menü için focus trap beklenen davranıştır.
- **Gerekçe:** Erişilebilirlik kabulü ve E2E klavye testi ancak tek bir modelle yazılabilir. 1024 kırılımı ayrıca tablette video açıkken sade navigasyonu korur.
- **Etki:** AC-4, AC-6; header/mobile bileşenleri.
- **Karar (2026-10-25):** **ONAYLANDI — A.**

### B-09 — h1 hiyerarşisi, wordmark ve skip link tanımsız

- **Kategori:** Belirsiz kabul kriteri / erişilebilirlik.
- **Spec yeri:** REQ-4 anlam (`:40`), AC-2 wordmark (`:111`), AC-4 (`:126`, `:128`).
- **Soru:** Ortak header'daki wordmark h1 mi, link mi? Ana sayfada h1 hero başlığı mı, site adı mı? "İçeriğe geç" bağlantısının hedefi, metni ve testid'i ne?
- **Neden açık:** Mevcut ana sayfada h1 site adı, hero başlığı h2; odalar sayfasında h1 sayfa başlığı. Ortak header gelince `web-site-title` h1 olarak kalırsa her sayfada iki h1 veya yanlış hiyerarşi oluşur. Skip link için hedef/metin yok.
- **Seçenekler:**
  - **A)** Wordmark bir `<Link>`'tir (başlık değil); ana sayfa h1 = hero başlığı; odalar h1 = sayfa başlığı; credits h1 = sayfa başlığı. Skip link metni "İçeriğe geç" / "Skip to content", hedef `#main-content`, testid `skip-to-content`.
  - **B)** Wordmark tüm sayfalarda h1 kalır; sayfa başlıkları h2 olur.
- **Önerim:** **A.** `main` öğesi `id="main-content"` ve `tabindex="-1"` taşır; skip link ilk odaklanabilir öğe ve yalnız klavye odağında görünür. Aktif nav öğesinde `aria-current="page"`; header `banner`, footer `contentinfo` rolü. Telefon/e-posta `tel:`/`mailto:` bağlantısı.
- **Gerekçe:** Tek h1 ve doğru landmark hiyerarşisi hem AC-4 hem SEO için temel. Wordmark'ı link yapmak ana sayfaya dönüş affordance'ı sağlar; h1'i sayfa içeriğine bırakmak başlık hiyerarşisini düzeltir.
- **Etki:** AC-2, AC-3, AC-4; SEO.
- **Karar (2026-10-25):** **ONAYLANDI — A.**

### B-10 — Credits sayfasının kaynağı ve chrome'u belirsiz

- **Kategori:** Belirsiz kabul kriteri.
- **Spec yeri:** REQ-2 credits (`:36`), AC-2 (`:116`), doküman etkisi (`:78`), context (`:97`).
- **Soru:** `/[locale]/credits` sayfası `CREDITS.md`'yi mi render ediyor, yoksa aynı bilgileri elle mi tekrar ediyor? Sayfa ortak header/footer kullanacak mı? Locale switch ve metadata taşıyacak mı? Footer'daki bağlantının kullanıcıya görünen etiketi ne?
- **Neden açık:** "CREDITS.md bağlantısı ... sayfaya gider" deniyor; ama `.md` dosyası ile sayfa arasındaki ilişki, sayfanın chrome'u ve lokalizasyonu tanımlı değil. Ayrıca `CREDITS.md` font lisanslarını içerecekse B-02'ye bağımlı.
- **Seçenekler:**
  - **A)** `CREDITS.md` tek kaynak kalır; sayfa build zamanında bundan üretilir (veya üretilen içerik modülü kullanılır), ortak header/footer + h1 + metadata taşır; başlıklar TR/EN, lisans adları/URL'leri değişmez; footer etiketi "Kaynaklar ve Lisanslar" / "Credits & Licenses".
  - **B)** Sayfa lisans listesini elle yazar; `CREDITS.md` ayrı bir kayıt olarak kalır (duplikasyon riski).
  - **C)** Footer doğrudan GitHub'daki ham `CREDITS.md`'ye link verir; spec'teki "sayfa" şartı karşılanmaz.
- **Önerim:** **A.** Credits sayfası da axe taramasına ve metadata kontrolüne dahil edilir; locale switch `/tr/credits` ↔ `/en/credits` bağlamını korur.
- **Gerekçe:** Hukuki atıf metni tek kaynaktan üretilmelidir; elle kopyalama lisans/atıf tutarsızlığı yaratır. Ortak chrome, yeni sayfanın siteden kopuk görünmesini engeller.
- **Etki:** AC-2 (credits), AC-4 (axe kapsamı önerisi), `CREDITS.md`.
- **Karar (2026-10-25):** **ONAYLANDI — A.**

### B-11 — Performans bütçelerinin ölçüm yöntemi belirsiz

- **Kategori:** Eksik sınır / ölçüm.
- **Spec yeri:** REQ-5 (`:42-45`), AC-5 (`:131-135`).
- **Soru:** "250 KB gzip" hangi dosyaların toplamı? "200 KB font" yüklenen mi, repodaki tüm fontlar mı? Lighthouse kaç kez, hangi sürüm/ayarla koşacak? CLS scroll sırasında nasıl ölçülecek? KB/MB binary mi decimal mi?
- **Neden açık:** Aynı bütçe farklı ölçümlerle farklı sonuç verir; AC-5'in tekrarlanabilir olması için yöntem tanımı gerekir. Ayrıca Lighthouse CLS yalnız ilk yükü ölçer; "scroll sırasında layout shift" ayrı bir ölçüm gerektirir.
- **Seçenekler:**
  - **A)** Deterministik tanımlar: 1 KB = 1024 byte, 1 MB = 1.048.576 byte; JS = `/tr` ilk yükünde yüklenen tüm script chunk'larının `gzip -9` toplamı (lazy chunk'lar dahil, ağ idle'a kadar); font = `web/public/fonts` altındaki tüm `woff2` dosyaları; Lighthouse = production build, Chrome headless, mobile preset, 3 koşu median; CLS = Lighthouse + Playwright scroll sırasında `layout-shift` toplamı.
  - **B)** Next.js build çıktısındaki "First Load JS" değeri ve tek Lighthouse koşusu; hızlı ama gzip/scroll CLS belirsiz.
- **Önerim:** **A.** Kanıt logunda Lighthouse/Chrome sürümü, URL, build commit'i ve ölçüm komutu yer alır. Bütçe aşımı CI'da kırmızı build üretir; Lighthouse CI gate'i yoktur (spec ile uyumlu).
- **Gerekçe:** Bütçe ancak aynı yöntemle karşılaştırılabilir. Özellikle "lazy chunk'lar dahil mi?" sorusu 250 KB'nin etrafından dolaşmayı engeller; scroll CLS ise animasyonların gerçekten shift üretmediğini kanıtlar.
- **Etki:** AC-5; CI; performans kanıtı.
- **Karar (2026-10-25):** **ONAYLANDI — A.**

### B-12 — Animasyon kütüphanelerinin görev dağılımı ve reduced-motion kapsamı belirsiz

- **Kategori:** Belirsiz AC / performans.
- **Spec yeri:** REQ-2 scroll (`:32`), REQ-4 (`:38-41`), stack (`:55`), context (`:87`), AC-4 (`:125`).
- **Soru:** Motion, GSAP ScrollTrigger ve Lenis'ten hangisi ne için kullanılacak? Üçü de ilk yükte mi? `prefers-reduced-motion` altında "mikro animasyon" tam olarak neyi kapsıyor?
- **Neden açık:** Stack üç kütüphaneyi de sayıyor ve "bu dilimde eklenecektir" diyor; ancak görev dağılımı yok. 250 KB JS bütçesiyle üçünün birden ilk yükte olması riskli. Reduced-motion'da hover geçişleri, reveal ve smooth scroll'un hangilerinin kapanacağı tanımsız.
- **Seçenekler:**
  - **A)** GSAP ScrollTrigger yalnız pin/parallax; Motion yalnız reveal/mikro (opacity/transform); Lenis yalnız desktop smooth scroll. Üçü de ilk boyamadan sonra lazy-load edilir. Reduced-motion'da Lenis/ScrollTrigger hiç yüklenmez, reveal yok, içerik anında görünür; CSS geçişleri yalnız renk/opaklıkla sınırlı.
  - **B)** Üçü de her yerde kullanılır; daha zengin ama bütçe/erişilebilirlik riski yüksek.
  - **C)** Yalnız Motion + CSS kullanılır; D3'teki GSAP/Lenis stack'i fiilen terk edilir.
- **Önerim:** **A.** Mobilde (<1024 px) GSAP/Lenis yok; yalnız hafif Motion reveal (reduced-motion'da o da yok). "Mikro animasyon" kapsamı: hover renk/gölge, düğme basma, kart reveal; layout property'si (width/height/top/left) animasyonu yasak. Reduced-motion tercihi çalışma zamanında değişirse dinlenir.
- **Gerekçe:** Görev dağılımı bundle'ı ve performansı kontrol edilebilir kılar; reduced-motion'ın hangi hareketleri kapatacağı netleşmeden AC-4 testi yazılamaz. Layout property animasyonu CLS'yi doğrudan riske atar.
- **Etki:** AC-2, AC-4, AC-5.
- **Karar (2026-10-25):** **ONAYLANDI — A.**

### B-13 — Tanımsız hata ve fallback durumları

- **Kategori:** Tanımsız hata durumu.
- **Spec yeri:** Hero (`:31-32`), AC-2 (`:112-117`), AC-3 (`:120-123`), AC-5 (`:134`), `docs/frontend.md:15`.
- **Soru:** Video yüklenemez/autoplay engellenirse, font yüklenemezse, API zaman aşımına uğrar/ağ koparsa/bozuk JSON dönerse, `CREDITS.md` eksikse ne olacak? Statik ana sayfa ve credits için yükleme/boş/hata durumları gerekli mi?
- **Neden açık:** Spec yalnız `0002`'nin mevcut hata metinlerinin korunmasını söylüyor; yeni tasarımın video/font/credits hata davranışı yok. `docs/frontend.md` "her ekranda üç durum" diyor; statik sayfalar için istisna tanımlı değil. Mevcut API client'ta timeout yok; istek asılı kalırsa yükleme durumu sonsuza kadar kalabilir.
- **Seçenekler:**
  - **A)** Spec'e hata/fallback matrisi eklenir:
    - Video yüklenemez/autoplay reddedilir → video gizlenir, poster kalır, konsol hatası yok.
    - Font yüklenemez → sistem fallback'i, `font-display: swap`, kullanıcıya hata yok.
    - Rooms API 4xx/5xx/ağ hatası/zaman aşımı/şema hatası → mevcut `rooms-error` + sabit metin + `rooms-retry`; teknik detay gizli; 10 sn timeout.
    - `CREDITS.md` eksik/bozuksa → build fail-fast, açık hata.
    - Hero/poster/OG eksikse → build/bütçe kontrolü fail-fast.
    - Statik home/credits → yükleme/boş/hata zorunluluğundan muaf; `docs/frontend.md`'ye not.
  - **B)** Yalnız `0002`'deki 500 + retry korunur; diğer durumlar kapsam dışı bırakılır ve spec'te açıkça yazılır.
- **Önerim:** **A.** Özellikle 10 sn timeout ve video fallback'i eklenmeli; bunlar olmadan "loading" veya "hero" ekranı sessizce kilitlenebilir.
- **Gerekçe:** Hata durumları tasarım sisteminin `StatePanel` bileşeninin parçası; tanımsız bırakılırsa geliştirici her ekranda farklı davranır. Video fallback'i aynı zamanda LCP/poster stratejisinin doğal parçası.
- **Etki:** AC-2, AC-3, AC-4; `StatePanel`; E2E hata senaryoları.
- **Karar (2026-10-25):** **ONAYLANDI — A.**

### B-14 — E2E kanıt ve test altyapısı belirsiz

- **Kategori:** Belirsiz AC / kanıt.
- **Spec yeri:** REQ-6 (`:46-48`), AC-6 (`:137-142`), `e2e/README.md`.
- **Soru:** `E2E_EVIDENCE_DIR` nasıl çözülecek? `@design` ve `@seeded` etiketleri nasıl yazılacak? `save-data` nasıl simüle edilecek? axe raporu hangi araçla üretilecek? `.webm` kanıtı nasıl kaydedilecek ve 5 MB'a nasıl sığdırılacak? `0002` dosyalarının değişmediği nasıl kanıtlanacak?
- **Neden açık:** Spec hedefleri veriyor, mekanizmaları vermiyor. Mevcut `e2e/tests/smoke.spec.ts` kanıt dizinini `0002` olarak sabitliyor; `save-data`, axe ve video kaydı için bağımlılık/komut yok. `git status --short` "değişmedi" kanıtı olarak kullanılamaz.
- **Seçenekler:**
  - **A)** Şunlar spec'e yazılır:
    - `E2E_EVIDENCE_DIR` kök; her spec `.../0002`, `.../0003` altına yazar; ortak helper.
    - Playwright `test(..., { tag: ["@design", "@seeded"] })`; CI `--grep @seeded` design testlerini de koşar; `0002` başlık etiketleri değişmez.
    - `save-data`: `context.addInitScript` ile `navigator.connection.saveData = true`; 1280 ve 390'da test.
    - axe: `@axe-core/playwright`; `/tr`, `/en`, `/tr/rooms`, `/en/rooms`, `/tr/credits`, `/en/credits`; serious/critical = 0; Markdown rapor.
    - Video: Playwright `recordVideo` 1280×800, ~10 sn scripted scroll; ffmpeg ile ≤5 MB'a transcode; `AC-2-home-scroll-story.webm`.
    - `0002` izolasyonu: önce/sonra SHA-256 manifest; `git status` yerine checksum karşılaştırması.
    - Ekran görüntüleri: sayfa kanıtları `fullPage`, scroll/focus/menu kanıtları viewport.
  - **B)** Mekanizmalar geliştiriciye bırakılır; kanıtlar arasında tutarsızlık riski kabul edilir.
- **Önerim:** **A.**
- **Gerekçe:** AC-6'nın tekrarlanabilir olması ve `0002` arşivinin korunması ancak açık mekanizmayla kanıtlanır. Özellikle checksum, `git status`'un gösteremeyeceği "bayt düzeyinde değişmedi" şartını karşılar.
- **Etki:** AC-6; CI; kanıt üretimi.
- **Karar (2026-10-25):** **ONAYLANDI — A.**

### B-15 — Sınır değerleri ve birimler eksik

- **Kategori:** Eksik sınır değer.
- **Spec yeri:** AC-2 (`:112-115`), AC-5 (`:131-135`), AC-6 (`:139`).
- **Soru:** 767/768 ve 1023/1024 sınırlarında hangi davranış geçerli? Description 70/160 karakter sınırları dahil mi, nasıl sayılıyor? KB/MB binary mi decimal mi? "2–3 sahne" tam sayı kaç? Font ailesi "en fazla iki" iken 1 aile de kabul mü?
- **Neden açık:** Spec sayısal bütçeleri veriyor ama sınırların dahil/hariç olduğunu ve ölçü birimini tanımlamıyor. Bu, testlerde off-by-one ve farklı yorumlara yol açar.
- **Seçenekler:**
  - **A)** Spec'e boundary tablosu eklenir (aşağıdaki gibi).
  - **B)** Açık değerler geliştiriciye bırakılır; testler yalnız "makul" aralığı kontrol eder.
- **Önerim:** **A.** Önerilen tablo:

| Alan | Kural | Test noktaları |
|---|---|---|
| Viewport | 390×844 mobil; 768'de video açık; 1023'e kadar pin yok; 1024'te pin | 390, 767, 768, 1023, 1024, 1280 |
| Description | 70–160 karakter **dahil**; trim sonrası Unicode code point | 69, 70, 160, 161 |
| Byte birimi | 1 KB = 1024 B; 1 MB = 1.048.576 B; üst sınırlar dahil | 204.800 B font; 256.000 B JS gzip; 8.388.608 B video; 307.200 B poster/OG |
| Pinli sahne | Tam 2 (B-04/A) | `data-motion="pinned"` sayısı = 2 |
| Font | Tam 2 aile; 3 ağırlık | aile sayısı = 2, toplam woff2 ≤ 200 KB |
| Video kanıtı | ≤ 15 sn, ≤ 5 MB | ffprobe süre + dosya boyutu |

- **Gerekçe:** Sınırların ve birimlerin tek yerde tanımlanması AC-2/5/6 testlerini deterministik yapar; özellikle 767/768 ve 1023/1024 geçişleri geliştiriciye bırakılamaz.
- **Etki:** AC-2, AC-5, AC-6.
- **Karar (2026-10-25):** **ONAYLANDI — A.**

---

## 3. Bloklayıcı olmayan maddeler

> Bu iki madde de onaylanmıştır; başlıklar ilk incelemedeki soruyu tanımlar, bağlayıcı sonuç "Karar (2026-10-25)" satırındadır.

### B-16 — shadcn/Radix kapsamı ve bileşen yönetimi belirsiz

- **Kategori:** Belirsiz kapsam.
- **Spec yeri:** REQ-1 (`:26-29`), stack (`:55`), context (`:86-87`), AC-1 (`:105-109`).
- **Soru:** "shadcn/ui yaklaşımı" tam kütüphane kurulumu mu, yoksa mevcut el yazımı bileşenlerin korunması mı? Radix hangi bileşenler için? `Section/Container` tek bileşen mi, iki bileşen mi? `Skeleton` envanterde yer alacak mı? `RoomCard` tıklanabilir mi?
- **Neden açık:** Spec "tam kütüphane migrasyonu yapılmaz" ve mevcut `Button/Card/Skeleton` API'lerinin korunmasını söylüyor; ancak shadcn CLI kurulumu, Radix kapsamı ve bileşen sınırları net değil. `Section/Container` tek satırda iki kavram; `Skeleton` envanter listesinde yok.
- **Seçenekler:**
  - **A)** Tam shadcn migrasyonu yok; mevcut `Button`, `Card`, `Skeleton` korunur; Radix yalnız MobileNav dialog'u için eklenir; `Section` tek primitif (genişlik/boşluk varyantları), `Container` onun parçası; `StatePanel` loading/empty/error varyantları; `Skeleton` alt bileşen olarak belgelenir. `RoomCard` detay sayfası olmadığı için tıklanabilir değildir; hover'da yalnız hafif gölge/kenarlık olur, pointer cursor kullanılmaz.
  - **B)** shadcn CLI ile bileşenler yeniden kurulur; mevcut API'ler için wrapper yazılır.
  - **C)** Radix hiç eklenmez; mobile menü CSS/JS ile yapılır.
- **Önerim:** **A.**
- **Gerekçe:** AC-1 mevcut API'lerin korunmasını istiyor; tam migrasyon gereksiz risk ve bundle maliyeti getirir. RoomCard'ın tıklanabilir görünmemesi, henüz detay sayfası yokken ölü affordance yaratmaz.
- **Etki:** AC-1, AC-3; `docs/frontend.md`; bundle.
- **Karar (2026-10-25):** **ONAYLANDI — A.**

### B-17 — `CLARIFY-*` kararlarının izlenebilirliği yok

- **Kategori:** Doküman boşluğu.
- **Spec yeri:** Künye (`:15`), context (`:99`).
- **Soru:** `CLARIFY-1A…9A` kararlarının tarih, kapsam ve gerekçeleri nerede kayıtlı? Spec "bu kodlar repoda ayrıca tanımlı değildir" diyor.
- **Neden açık:** Gelecekteki geliştirici/QA, bir kuralın neden konduğunu yalnız spec metninden çıkaramaz; karar kodları sohbet geçmişine bağlı kalır. Bu, "tek gerçek kaynak" ilkesiyle çelişir.
- **Seçenekler:**
  - **A)** Spec sonuna kısa bir karar kaydı tablosu eklenir: kod, tarih, karar özeti, etkilenen REQ/AC.
  - **B)** Kodlar yalnız referans olarak kalır; detay sohbet geçmişinde.
- **Önerim:** **A.**
- **Gerekçe:** Karar kaydı, spec revizyonlarında ve code review'da "neden böyle" sorusunu yanıtlar; yeni ekip üyesi için bağlam sağlar.
- **Etki:** Doküman; kod davranışı yok.
- **Karar (2026-10-25):** **ONAYLANDI — A.**

---

## 4. Analistin spec rev.1 kontrol listesi

> Bu bölüm analist içindir. Amaç, onaylanan kararların spec metnine eksiksiz ve çelişkisiz işlenmesidir. Kod veya uygulama planı değildir.

### 4.1 Önce hazırlanacak içerik: B-03 copy deck

Analist, spec rev.1'in eki olarak TR/EN copy deck hazırlar. Deck en az şu blokları içermelidir:

| Blok | İçerik |
|---|---|
| Header | Ana Sayfa / Odalar nav etiketleri, skip link metni, mobil menü açma/kapama etiketi |
| Hero | Badge, başlık, açıklama, CTA metni (mevcut metinler korunacaksa açıkça belirtilir) |
| Story | Başlık, gövde, 2–3 hikâye paragrafı |
| Experience | Başlık, gövde, 4 deneyim/olanak kartı |
| Rooms CTA | Başlık, gövde, düğme metni |
| Contact | Başlık, gövde, adres/telefon/e-posta etiketleri |
| Footer | Kısa tagline, copyright, credits bağlantı etiketi |
| Credits | Sayfa başlığı, giriş, lisans bölüm başlıkları |
| Metadata | Home, rooms, credits için TR/EN title + description + OG alt metni |

Kurallar:

- `0002`'nin sabit boş, hata ve tekrar dene metinleri **aynen** korunur.
- İletişim bilgileri `CLARIFY-8A`'daki temsilî değerlerdir; `CREDITS.md` bunu belirtir.
- Title/description metinleri B-15'teki 70–160 karakter kuralına uygun olmalıdır.
- Copy deck onaylanmadan görsel kanıtların "premium" kabulü yapılmaz.

### 4.2 Spec metnine işlenecek kararlar

| Karar | Spec'e işlenecek değişiklik | Etkilenen bölüm/AC |
|---|---|---|
| **B-01** | §3'teki "en fazla 2 revizyon turu" ifadesi kaldırılır/geçersiz kılınır. Yerine: görsel kalite developer sorumluluğunda; takılınca Takım Yöneticisi'ne sorulur; tasarıma birlikte karar verilir; ek tur serbest. AC-1'deki token tablosu zorunluluğu korunur; token değerlerini developer belirler ve `docs/frontend.md`'ye yazar. D18 hero kaynağı/lisans/boyut sınırları varsayılan olarak korunur; kaynak değişikliği ayrı D18 kararıdır. | §3 risk, AC-1, AC-2, AC-3 |
| **B-02** | Font pairing: Cormorant Garamond 600 + Source Sans 3 400/600; SIL OFL; Latin Extended subset; toplam 3 `woff2`; ≤ 200 KB; `CREDITS.md`'de aile, lisans, kaynak URL ve ağırlıklar. | REQ-1 tipografi, AC-1, `CREDITS.md` |
| **B-03** | Copy deck §4.1'deki kapsamla spec eki olarak eklenir; metadata metinleri dahil. | REQ-2, AC-2, AC-3 |
| **B-04** | Tam 2 pinli sahne: `home-story`, `home-experience`; hero/CTA/contact statik. 768–1023 px: video açık, pin/parallax yok, hafif reveal. `data-motion` haritası ve ±40 px parallax sınırı yazılır. | REQ-2 scroll, AC-2, AC-4 |
| **B-05** | Hero: 1920×1080, 8–12 sn, 30 fps, sessiz; VP9/H.264 hedefleri; poster 1920×1080 WebP q80; poster her zaman DOM'da; video ilk boyamadan sonra üstte; `preload="none"`; `ffprobe` kanıtı. | REQ-2 hero, AC-2, AC-5 |
| **B-06** | Metadata tüm rotalarda (home, rooms, credits); canonical self; `hreflang` tr/en/x-default → TR karşılığı; tek metinsiz 1200×630 OG + locale `og:image:alt`; `twitter:card=summary_large_image`; `metadataBase` `NEXT_PUBLIC_SITE_URL`; `.env.example` güncellemesi. | REQ-2 metadata, AC-2 |
| **B-07** | Testid tablosu eklenir: `skip-to-content`, `site-main`, `mobile-nav-toggle`, `mobile-nav`, `mobile-nav-home`, `mobile-nav-rooms`, `footer-copyright`, `home-contact-*`; desktop `site-nav-*`; tek `web-site-title`; dil switch'i header'da. | AC-2, AC-3, AC-4, AC-6 |
| **B-08** | `<1024 px` mobile nav; tam ekran `role="dialog"` + `aria-modal="true"`; focus trap; Escape/close ile odak dönüşü; body scroll lock; route değişiminde kapanma; dil switch'i header'da. | REQ-4, AC-4, AC-6 |
| **B-09** | Wordmark linktir (h1 değil); home h1 = hero başlığı; rooms/credits h1 = sayfa başlığı; tek h1; skip link "İçeriğe geç"/"Skip to content" → `#main-content`; `tabindex="-1"`; `aria-current="page"`; header/footer landmark. | REQ-4, AC-2, AC-3, AC-4 |
| **B-10** | `CREDITS.md` tek kaynak; credits sayfası bundan üretilir; ortak header/footer + h1 + metadata; başlıklar TR/EN, lisans adları/URL'leri sabit; footer etiketi "Kaynaklar ve Lisanslar"/"Credits & Licenses"; axe ve metadata kapsamına dahil. | REQ-2 credits, AC-2, AC-4 |
| **B-11** | Ölçüm tanımları: 1 KB=1024 B, 1 MB=1.048.576 B; JS = `/tr` ilk yükünde yüklenen tüm script chunk'larının `gzip -9` toplamı (lazy dahil, ağ idle'a kadar); font = `web/public/fonts` altındaki tüm `woff2`; Lighthouse production + Chrome headless + mobile preset + 3 koşu median; CLS = Lighthouse + Playwright scroll `layout-shift` toplamı; sürüm/URL/commit loglanır. | REQ-5, AC-5 |
| **B-12** | GSAP ScrollTrigger yalnız pin/parallax; Motion yalnız reveal/mikro; Lenis yalnız desktop smooth scroll; üçü ilk boyamadan sonra lazy; <1024 px'te GSAP/Lenis yok; reduced-motion'da Lenis/ScrollTrigger yüklenmez, reveal yok, içerik anında görünür; CSS yalnız renk/opaklık; layout property animasyonu yasak; tercih değişimi dinlenir. | REQ-2, REQ-4, AC-4, AC-5 |
| **B-13** | Hata/fallback matrisi: video yüklenemez/autoplay reddi → poster kalır, video gizlenir, konsol hatası yok; font yüklenemez → fallback + `font-display: swap`; rooms API 4xx/5xx/ağ/timeout/şema hatası → mevcut `rooms-error` + sabit metin + retry, 10 sn timeout, teknik detay gizli; `CREDITS.md`/asset eksikliği → build fail-fast; statik home/credits üç-durum zorunluluğundan muaf. | AC-2, AC-3, AC-4, `docs/frontend.md` |
| **B-14** | `E2E_EVIDENCE_DIR` kök + spec alt dizini helper'ı; `@design`/`@seeded` Playwright tag'leri; `save-data` init script simülasyonu; axe `@axe-core/playwright` ile 6 rota; video `recordVideo` + ffmpeg ≤5 MB; 0002 izolasyonu SHA-256 manifest; sayfa/scroll/focus ekran görüntüsü kuralları. | REQ-6, AC-6 |
| **B-15** | Boundary tablosu: viewport 390/767/768/1023/1024/1280; description 69/70/160/161, Unicode code point; byte birimi 1024/1.048.576; pinli sahne tam 2; font tam 2 aile/3 ağırlık; video kanıtı ≤15 sn/≤5 MB. | AC-2, AC-5, AC-6 |
| **B-16** | Tam shadcn migrasyonu yok; mevcut `Button/Card/Skeleton` korunur; Radix yalnız MobileNav dialog'u; `Section` tek primitif; `StatePanel` loading/empty/error; `Skeleton` alt bileşen; `RoomCard` tıklanabilir değil, pointer cursor yok. | AC-1, AC-3, `docs/frontend.md` |
| **B-17** | Spec sonuna `CLARIFY-*` karar kaydı tablosu: kod, tarih, karar özeti, etkilenen REQ/AC. | Künye/context |

### 4.3 Tutarlılık kontrolleri

Analist spec rev.1'i yazdıktan sonra şunları kontrol etmelidir:

1. **B-01 çelişkisi:** "En fazla 2 revizyon turu" ifadesi spec'in hiçbir yerinde kalmamalı; ek tur serbestliği açıkça yazılmalı.
2. **D18 sınırı:** Hero video kaynağı Pexels 30653318, lisans ve dosya boyutu kuralları korunmalı; görsel ayarlama serbestliği bu sınırın dışına taşmamalı.
3. **AC-1 token zorunluluğu:** Görsel yön developer'a devredilse de `docs/frontend.md` token tablosu ve `@theme` eşleşmesi korunmalı.
4. **0002 sözleşmesi:** Tüm `0002` testid'leri ve sabit TR/EN boş/hata/retry metinleri değişmemeli.
5. **Kanıt adları:** AC-2/3/4/5/6 kanıt listeleri B-05/B-06/B-07/B-14 ile tutarlı olmalı; yeni testid/metadata/credits kanıtları eklenmeli.
6. **Ölçüm tutarlılığı:** B-11'deki tanımlar AC-5 ve CI bütçe kontrolünde aynı şekilde tekrarlanmalı; farklı birim/yöntem kalmamalı.
7. **Doküman etkisi:** `docs/frontend.md`, `CREDITS.md`, `e2e/README.md`, `docs/testing.md` §3, `specs/done/README.md`, `.github/workflows/ci.yml`, `.env.example` listesi eksiksiz olmalı.
8. **Kapsam:** `hotel-api`, `admin-api`, `admin-web` ve DB değişikliği önerilmemeli.

### 4.4 Analistin teslimi

- `specs/0003-ui-design-system.md` rev.1 (Durum: Onay bekliyor).
- B-03 copy deck'i (spec eki veya ayrı dosya).
- B-17 karar kaydı tablosu.
- Takım Yöneticisi'ne "hangi madde nereye işlendi" kısa değişiklik özeti.

## 5. Sıradaki adım

Takım Yöneticisi kararları bu brief ile tamamlanmıştır. Sıra:

1. Analist, §4'teki kontrol listesine göre spec rev.1'i ve copy deck'i hazırlar.
2. Takım Yöneticisi spec rev.1'i onaylar.
3. Developer, `releasable`'dan `feature/0003-ui-design-system` branch'ini açar ve uygulamaya başlar (`docs/git.md` §1).
4. Görsel kalite B-01 gereği developer sorumluluğundadır; takıldığı noktalarda Takım Yöneticisi ile birlikte karar verilir.

> Spec 0003 şu an **Taslak** ve repo'da **untracked** durumdadır. Altın Kural 1 gereği spec onaylanmadan kod yazılmaz.
