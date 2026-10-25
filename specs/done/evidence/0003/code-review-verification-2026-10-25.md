# Kod İncelemesi Doğrulama — Spec 0003 (Fix Turu)

- **İnceleyen:** Code Reviewer
- **Tarih:** 2026-10-25
- **PR:** [#11](https://github.com/altananay/hotel-management-system/pull/11)
- **İncelenen head:** `5f3c397`
- **Fix commit'leri:** `0305043` (integration merge), `8cec9fc` (kod/test), `8537442` (kanıt), `5f3c397` (CI/doküman)
- **Yöntem:** Statik diff, kaynak kodu, E2E kanıtları ve GitHub PR/check-run API incelemesi. Test veya derleme çalıştırılmadı.

---

## 1. PR ve CI durumu

- PR: `base=integration`, `head=5f3c397`, `mergeable=true`, `mergeable_state=clean`, 140 dosya.
- Check-run'lar: **CI Quality Gate, Backend, Frontend, E2E, GitGuardian — 5/5 success**.
- Workflow run [40224276319](https://github.com/altananay/hotel-management-system/actions/runs/40224276319) `success` (head `8537442`); güncel head check-run'ları da success.
- `git diff --check 8bbe705..HEAD` temiz.

---

## 2. Bulgu durumu

| ID | Durum | Doğrulama |
|---|---|---|
| CR-1 | **Kapandı** | `origin/integration` merge edildi (`0305043`); PR clean; CI 5/5 success. |
| CR-2 | **Kapandı** | Takım Yöneticisi talebiyle slow-zoom/stroke-draw kapsamı kabul edildi. |
| CR-3 | **Kapandı — test eksiği CR-8** | `appErrors()` filtresi kaldırıldı; testler ham `console.errors` kullanıyor. Ancak font/video failure-path kanıtları gerçek hata yerine proxy kullanıyor; §3.1. |
| CR-4 | **Kapandı** | Parallax testi GSAP ve pinned durumunu bekliyor; en az bir `0 < |offset| ≤ 40` doğruluyor. |
| CR-5 | **Kapandı** | Ham renkler `color-mix()`/token'a çevrildi; `--color-scrim` eklendi; CSS guard genişletildi. |
| CR-6 | **Kapandı** | Layout preload'u kaldırıldı; `srcSet`/`sizes` ile responsive poster; rooms/credits poster istemiyor. |
| CR-7 | **Kapandı** | `navigator.connection` `change` dinleniyor; save-data aç/kapat runtime testi var. |
| CR-8 | **Açık (Düşük/Orta)** | Font/video failure-path testleri gerçek hata yolunu doğrulamıyor; §3.1. |

---

## 3. Kalan bulgu

### [CR-8] Failure-path kanıtları gerçek hata yerine başarılı yüklemeyi kullanıyor — **Düşük/Orta**

**Dosya:** `e2e/tests/design/rooms-design.spec.ts:163-203`
**Dosya:** `e2e/tests/design/hero.spec.ts:197-224`
**Spec:** `specs/0003-ui-design-system.md:41,152` (`B-13`, AC-3)

**Kanıt:**
- Font fallback testi artık font isteğini başarısız yapmıyor; `empty-latin.woff2` adlı **geçerli** bir WOFF2 fixture'ını `200` ile servis ediyor. Test `document.fonts.status === "loaded"` ve ölçülen genişliğin Source Sans 3'süz stack ile eşleşmesini doğruluyor. Bu, **font yüklemesi başarısız olduğunda** oluşan `font-display: swap`/ağ hatası fallback'ini değil, **fontun yüklenip Latin glyph'lerinin bulunmaması** durumundaki glyph fallback'ini kanıtlıyor.
- Video 404 testi gerçek bir 404 ağ isteği yerine `window.fetch` shim'i ile sentetik `Response(..., {status:404})` döndürüyor. Uygulamanın `response.ok === false` dalı çalışıyor; ancak gerçek 404'te Chromium'un ürettiği kaynak konsol hatası ve gerçek `<video>` kaynak hatası (`onError`) doğrulanmıyor.

**Etki:** AC-3'ün “font isteği başarısız → sistem fallback'i (konsol hatası yok)” ve AC-2'nin “video hatası/404 → poster, konsol hatası yok” koşulları gerçek hata yolu üzerinden kanıtlanmıyor. Testler, zayıf bir fallback davranışını veya yalnızca preflight dalını geçerli sayabilir.

**Fix önerisi:**
1. Font için ayrı bir testte isteği gerçekten `route.abort()` veya geçersiz font ile başarısız yap; görünür metin fallback'ini doğrula. Tarayıcının kaynak hatası console mesajı kaçınılmazsa spec'te bu istisnayı açıkça tanımla ve testi “uygulama hatası yok” olarak sınırlandır.
2. Video için gerçek 404/abort senaryosunu ayrı test et; `onError` sonrası posterin kaldığını ve uygulama kaynaklı hata olmadığını doğrula.
3. Mevcut “no-glyph WOFF2” testini glyph fallback testi olarak yeniden adlandır; font yükleme hatası testiyle karıştırma.

---

## 4. Sonuç

CR-1, CR-2, CR-4, CR-5, CR-6 ve CR-7 kapandı. CR-3'ün console filtresi düzeltmesi doğru, ancak failure-path kanıtı CR-8 olarak açık kaldı. CI 5/5 yeşil ve PR clean; CR-8 giderilene veya Takım Yöneticisi tarafından test yaklaşımı açıkça kabul edilene kadar **koşulsuz onay önerilmez**. Nihai karar Takım Yöneticisi'ne aittir.
