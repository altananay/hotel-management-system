# Code Review — Spec 0003 (UI Design System)

- **İnceleyen:** Code Reviewer
- **Tarih:** 2026-10-25
- **PR:** [#11](https://github.com/altananay/hotel-management-system/pull/11) — `feature/0003-ui-design-system`
- **İncelenen head:** `8bbe705`
- **Yöntem:** Statik diff, kaynak kodu, E2E kanıtları ve GitHub PR/check-run API incelemesi. Rol gereği test veya derleme çalıştırılmadı.

---

## 1. Sonuç özeti

Kodun ana işlevleri büyük ölçüde mevcut; ancak **PR bu haliyle onaylanamaz**. Branch `releasable` tabanlıyken PR `integration` hedefliyor; PR `mergeable=false` / `dirty`, yalnız GitGuardian kontrolü var ve branch için hiç CI workflow run'ı yok. Ayrıca kanıtları zayıflatan test filtreleri bulundu. CR-2 (slow-zoom/stroke-draw) Takım Yöneticisi tarafından bizzat talep edildiği için kapatıldı.

| ID | Önem | Konu |
|---|---|---|
| CR-1 | **Blocker** | PR base/CI: `integration` hedefi, conflict ve hiç CI run'ı yok |
| CR-2 | Kapandı | Takım Yöneticisi talebiyle slow-zoom ve stroke-draw kapsamı kabul edildi |
| CR-3 | Orta | Console filtreleri “konsol hatası yok” kanıtını geçersiz kılıyor |
| CR-4 | Düşük/Orta | Parallax testi GSAP yüklenmeden de geçebiliyor |
| CR-5 | Düşük | Global dekoratif renkler token dışı; design-system guard CSS'i taramıyor |
| CR-6 | Düşük | Hero poster 960 tüm rotalarda ve masaüstünde preload ediliyor |
| CR-7 | Düşük | Hero video `save-data` çalışma zamanı değişimini dinlemiyor |

---

## 2. Bulgular

### [CR-1] PR base/CI bloklayıcı — **Blocker**

**Dosya/alan:** PR #11; `.github/workflows/ci.yml:3-7`

**Kanıt:**
- GitHub API: `base=integration`, `head=8bbe705`, `mergeable=false`, `mergeable_state=dirty`, `changed_files=373`.
- Head için check-run'lar yalnızca **GitGuardian Security Checks**; `CI Quality Gate`, Backend, Frontend ve E2E yok.
- `actions/runs?branch=feature/0003-ui-design-system` sonucu **0 workflow run**.
- `git merge-base HEAD origin/integration` = `3b24aa6`; branch fiilen `releasable` (`eac31b9`) üzerine kurulu. `git diff --stat origin/integration...HEAD` 373 dosya gösteriyor; oysa `releasable...HEAD` 137 dosya.

**Etki:** PR'ın gerçek kapsamı ve hedef branch'i belirsiz; conflict nedeniyle merge edilemiyor. CI çalışmadığı için build/lint/test/axe/budget sonuçları bağımsız olarak doğrulanamıyor. `AC-6` kanıtındaki “CI run linki” de henüz yok.

**Fix önerisi:**
1. PR hedefini spec'e uygun şekilde `releasable` yap veya branch'i `integration` üzerine rebase edip conflictleri çöz.
2. Push sonrası CI'nın **5/5** çalıştığını doğrula; `CI Quality Gate`, Backend, Frontend, E2E yeşil olmalı.
3. `AC-6-e2e-ci-summary.md` içine gerçek CI run linkini ekle.
4. PR diff'inin yalnız `web`, `e2e`, `docs`, `CREDITS.md` ve spec/evidence kapsamında kaldığını doğrula.

---

### [CR-2] Kapandı — Takım Yöneticisi talebi

**Dosya:** `web/src/features/home/home-motion.tsx:133-160,218-243`
**Dosya:** `web/src/features/home/home-story.tsx:50`
**Dosya:** `web/src/features/home/home-experience.tsx:64`

**Durum:** `data-slow-zoom` ve `data-icon-draw` efektleri Takım Yöneticisi tarafından bizzat talep edildi; bu nedenle CR-2 onay engeli değildir. `docs/frontend.md` bu kapsamı yansıtıyor. Spec `B-12` ile doküman arasındaki uyum ayrı bir doküman işi olarak takip edilebilir; bu inceleme kapsamında açık bulgu değildir.

---

### [CR-3] Console filtreleri “konsol hatası yok” kanıtını geçersiz kılıyor — **Orta**

**Dosya:** `e2e/helpers/console.ts:9,25`
**Dosya:** `e2e/tests/design/hero.spec.ts:104-116,121-134`
**Dosya:** `e2e/tests/design/rooms-design.spec.ts:151-161`
**Dosya:** `e2e/tests/design/home.spec.ts:176-185`

**Kanıt:** `collectConsole().appErrors()` şu mesajları filtreliyor:

```ts
const RESOURCE_ERROR = /Failed to load resource|net::ERR_/i;
...
appErrors: () => errors.filter((message) => !RESOURCE_ERROR.test(message)),
```

Buna karşın spec:
- Video 404/autoplay hatasında “konsola hata basılmaz” diyor.
- Font isteği başarısız olduğunda “konsol hatası yok” diyor.
- Tüm sayfalarda gezinmede “konsol hatası oluşmaz” diyor.

Testler `appErrors()` kullandığı için tarayıcının font/video kaynak hatalarını console'a yazması testi düşürmüyor. Özellikle font fallback testi (`rooms-design.spec.ts:151-161`) başarısız font isteklerinin doğal kaynak hatasını bilinçli olarak yok sayıyor.

**Etki:** AC-2/AC-3/AC-4 için “konsol hatası yok” kanıtı gerçekte doğrulanmıyor; bozuk asset veya kaynak hatası testten kaçabilir.

**Fix önerisi:** Bu senaryolarda `console.errors` üzerinden ham konsol çıktısını doğrula. Tarayıcının kaçınılmaz kaynak logları varsa bunu spec'te açık istisna olarak tanımla ve kanıt notunda belirt; testi “application error” yerine “no unexpected console error” olarak yeniden adlandır. Font/video için route intercept veya kaynak yükleme stratejisiyle gerçekten sessiz fallback sağlanmalı.

---

### [CR-4] Parallax testi GSAP yüklenmeden de geçebiliyor — **Düşük/Orta**

**Dosya:** `e2e/tests/design/scroll-story.spec.ts:59-77`

**Kanıt:** Test `openHome()` sonrası doğrudan `evaluateAll` ile `[data-parallax]` transformlarını okuyor. `HomeMotion` GSAP'ı 2 saniyelik gecikmeden sonra dinamik import ediyor. Test `window.__hotelMotionLoaded.gsap === true` beklemiyor ve en az bir parcelin sıfırdan farklı transform ürettiğini doğrulamıyor; GSAP hiç yüklenmezse tüm transformlar `none` → `0` olur ve `Math.abs(0) <= 40` testi geçer.

**Etki:** AC-2/AC-5 parallax kanıtı zayıf; animasyon tamamen bozuk olsa bile test yeşil kalabilir.

**Fix önerisi:** `page.waitForFunction(() => window.__hotelMotionLoaded?.gsap === true)` ve `data-motion="pinned"` bekle; ardından en az bir `[data-parallax]` için sıfırdan farklı ve `<= 40` offset doğrula. Testi GSAP yüklenmeden geçemeyecek hale getir.

---

### [CR-5] Global dekoratif renkler token dışı; guard CSS'i taramıyor — **Düşük**

**Dosya:** `web/src/app/globals.css:270-303`
**Dosya:** `web/scripts/check-design-system.mjs:83-111`

**Kanıt:** `paper-grain`, `hero-vignette` ve `fine-grid` utility'leri doğrudan `rgb(...)` değerleri kullanıyor. Oysa AC-1/REQ-1 token'ların tek kaynak olduğunu söylüyor. `check-design-system.mjs` yalnız `.ts`/`.tsx` dosyalarını tarıyor; CSS'teki bu ham değerleri yakalamıyor.

**Etki:** Token değişiklikleri dekoratif katmanlara yansımaz; tasarım sistemi tek kaynak olmaktan çıkar ve guard yanlış güven verir.

**Fix önerisi:** overlay/grid/grain renkleri için token veya `color-mix()` türetmesi kullan; `check-design-system.mjs` kapsamına CSS taraması ekle. Gerekirse ham renkler için bilinçli istisna listesi tanımla.

---

### [CR-6] Hero poster tüm rotalarda ve masaüstünde preload ediliyor — **Düşük**

**Dosya:** `web/src/app/[locale]/layout.tsx:70-73`
**Dosya:** `web/src/features/home/home-hero-media.tsx:6-18`

**Kanıt:** Locale layout'unda her rota için `home-hero-poster-960.webp` preload ediliyor. Oysa bu poster yalnız ana sayfada kullanılıyor; rooms/credits sayfalarında gereksiz. Ayrıca masaüstünde `<picture>` varsayılan olarak 1920 poster kullanırken 960 preload ediliyor; iki ayrı istek doğabilir.

**Etki:** Gereksiz ağ kullanımı ve olası LCP/performans etkisi; performans bütçesi yalnız `/tr` üzerinde ölçüldüğü için bu maliyet görünmüyor.

**Fix önerisi:** Preload'u ana sayfa bileşenine taşı veya `imagesrcset`/`imagesizes` ile viewport'a uygun kaynağı preload et. Rooms/credits rotalarında hero posterini preload etme.

---

### [CR-7] Hero video `save-data` çalışma zamanı değişimini dinlemiyor — **Düşük**

**Dosya:** `web/src/features/home/hero-video.tsx:22-69`

**Kanıt:** `HomeHeroVideo` yalnız `desktop` ve `reduced` MediaQueryList değişimlerini dinliyor. `navigator.connection.saveData` değeri başlangıçta okunuyor; çalışma zamanında değişirse video mount/unmount edilmiyor. Spec `B-12` tercih değişiminin dinlenmesini istiyor; video koşulları arasında `save-data` da var.

**Etki:** Kullanıcı save-data'yı açtığında video yüklenmeye devam edebilir; veri tasarrufu tercihi uygulanmaz.

**Fix önerisi:** `connection` varsa `change` listener ekle; `handleChange` ile `canUseVideo` yeniden değerlendirilsin ve cleanup'ta listener kaldırılsın. Testte `save-data` başlangıç ve runtime değişimi ayrıca doğrulansın.

---

## 3. DoD durumu

- [ ] Tüm AC'ler bağımsız CI ve kanıtlarla doğrulandı — **CI çalışmadı**
- [ ] Pipeline yeşil — **hayır; workflow run yok**
- [ ] Kod incelemesi onaylandı — **bu inceleme ile onaylanmıyor**
- [x] Dokümanlar güncellendi; CR-2 kapsamı Takım Yöneticisi talebiyle kabul edildi
- [ ] Spec `specs/done/`e taşındı — henüz değil
- [ ] SCORECARD dolduruldu — henüz değil

**Sonuç:** CR-1 giderilmeden ve CI yeşil olmadan merge/QA kapanışı yapılmamalı. CR-3 onay öncesi düzeltilmeli veya Takım Yöneticisi tarafından açıkça kabul edilmeli; CR-2 Takım Yöneticisi talebiyle kapatıldı.
