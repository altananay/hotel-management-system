# Spec 0006 — QA Bulguları ve Kapanış Brief'i

> **Rol:** QA (`docs/roles/qa.md`)
> **Tarih:** 2026-11-08
> **İnceleme dalı / commit:** `integration` / `8d717da` (`feat(pricing): add pricing and quote management (#36) [plan 0006]`)
> **Not:** `integration` ağacı `feature/0006-pricing` ağacıyla birebir aynıdır (`git diff` boş; tree hash `593ef3d3…`).
> **Ortam:** İzole QA PostgreSQL (`hotel-qa-postgres`, `:55432`), mevcut dev veritabanına dokunulmadı; servisler `:8081/8082/3000/3001` üzerinde çalıştırıldı.
> **Durum:** 2 bulgu; 0 kritik. Kapanış için Takım Yöneticisi kararı bekleniyor.

## 1. Yönetici Özeti

| Sonuç | Değer |
|---|---|
| Kritik bulgu | 0 |
| Orta bulgu | 2 (QA-1, QA-2) |
| Regresyon | 0 |
| Backend testleri | hotel-api 74/74, admin-api 5/5 |
| Frontend testleri | admin-web 47/47, web 43/43 |
| E2E | `@pricing` 1/1; `@seeded` 2/2; design 40/40; `@admin` 3/3; `@media` 1/1 |
| CI | integration run 111 (commit `8d717da`) tüm job'larda **success** |
| Kanıt arşivi | `0001–0005` değişmedi (`git diff` + untracked kontrolü temiz) |

**Öneri:** QA-1 ve QA-2 kapatılmadan spec kapanışı yapılmamalı. İkisi de küçük/orta kapsamlı düzeltmelerle kapatılabilir; QA kod yazmaz, fix sonrası yeniden doğrulama yapar.

## 2. Çalıştırılan Testler ve Kanıtlar

### 2.1 Backend

| Uygulama | Komut | Sonuç | Kanıt |
|---|---|---|---|
| hotel-api | `.\mvnw.cmd -B verify` | `BUILD SUCCESS`; 74 test, 0 failure/error; Spotless + Modulith/ArchUnit + Testcontainers IT | `.local/qa-0006-hotel-api-verify.log` |
| admin-api | `.\mvnw.cmd -B verify` | `BUILD SUCCESS`; 5 test, 0 failure/error | `.local/qa-0006-admin-api-verify.log` |

### 2.2 Frontend

| Uygulama | Komut | Sonuç | Kanıt |
|---|---|---|---|
| admin-web | `npm run lint`, `npm run build`, `npm test` | lint 0, build 0, **47/47 Vitest** | `.local/qa-0006-admin-web.log` |
| web | `npm run lint`, `npm run build`, `npm test` | lint 0, build 0, **43/43 Vitest** | `.local/qa-0006-web.log` |

### 2.3 E2E (integration commit)

| Aşama | Komut | Sonuç | Kanıt |
|---|---|---|---|
| 0006 pricing | `npx playwright test tests/pricing --grep '@pricing'` | **1 passed** (8.0s) | `.local/qa-0006-e2e.log` |
| 0002 smoke | `npx playwright test tests/smoke.spec.ts --grep '@seeded'` | **2 passed** | `.local/qa-0006-e2e-0002.log` |
| 0003 design | `npx playwright test tests/design --grep '@seeded'` | **40 passed** | `.local/qa-0006-e2e-0003.log` |
| 0004 admin | `npx playwright test tests/admin --grep '@admin'` | **3 passed** | `.local/qa-0006-e2e-0004.log` |
| 0005 media | `npx playwright test tests/media --grep '@media'` | **1 passed** | `.local/qa-0006-e2e-0005.log` |
| SHA-256 manifest | `npm run test:manifest` | **4/4 passed** | `.local/qa-0006-manifest.log` |

Kanıt dizini: `.local/qa-0006-e2e-evidence/0006/`
Görseller: `AC-2-admin-pricing-{loading,empty,error,filled,delete}.png`, `AC-3-admin-fx-warning.png`.
`@empty` yerelde yeniden koşturulmadı (QA veritabanı seed'liydi); aynı commit'in CI run 111'indeki `@empty` adımı **success**'tir:
`https://github.com/altananay/hotel-management-system/actions/runs/42500749839`

### 2.4 Bağımsız API probe'ları (özet)

Aşağıdakiler QA tarafından doğrudan HTTP ile doğrulandı; bulgu üretmedi:

- Teklif sözleşmesi: `nightCount`/`nights`, string ondalık tutarlar, `quotedAt`/`expiresAt` farkı **900 sn**, TRY'de FX alanları `null`, buffer/rounding `null`.
- Yuvarlama: buffer 0/step 5 → `4255.00`; buffer 2/step 5 → `4340.00`; buffer 0/step 0.01 → `4250.43`; tam kat (`4250.00`) yukarı taşmıyor.
- Gece bazlı toplam: 3 gece × `4340.00` = **13020.00**; toplam yeniden yuvarlanıp `13010.00` yapılmıyor.
- Tarih sınırları: bugün giriş 200; 30 gece 200; 31 gece `400 pricing.invalid_period`; çıkış = giriş 400; geçmiş giriş 400.
- Misafir sınırları: `0` ve `maxGuests+1` → `400 pricing.invalid_guests`; `1..maxGuests` kabul.
- Oda tipi: pasif/bulunmayan → `404 pricing.room_type_not_found`; admin oda tipi listesi yalnız aktifleri döndürüyor.
- Fiyat/kur hataları: fiyat eksik `409 pricing.price_not_configured`; varsayılan plan yok `503 pricing.default_rate_plan_required`; kur yok `503 pricing.fx_rate_unavailable`; hepsi RFC 7807 + `X-Request-Id`.
- Sezon: başlangıç/bitiş günleri dahil; bitişik sezon kabul; çakışan sezon `409 pricing.season_overlap`; sezon fiyatı yoksa plan baz fiyatına düşüyor.
- Varsayılan plan: eksik fiyatla `409 pricing.default_rate_plan_incomplete` + `missing` listesi (9/2 kombinasyon); varsayılan plan pasifleştirme `409 pricing.rate_plan_in_use`.
- Dinamik yeni oda tipi: matriste eksik uyarısı; yeni kombinasyon `409`, diğer oda tipleri `200`; varsayılan plan varsayılan kalıyor.
- RBAC: `MANAGER` 200; `FRONT_DESK` okuma/yazma/silme `403 security.forbidden`; arayüzde forbidden paneli.
- Migration/entity: V12/V13 yalnız `pricing` şeması; tüm pricing entity'lerinde `@Table(schema = "pricing")`; `double`/`float` yok.
- 0001–0005 kanıt arşivleri değişmedi; `git status` temiz (brief dosyası hariç).

## 3. Bulgular

### [QA-1] Sıfır/negatif tutar ve sıfır misafir sayısı, stabil pricing hata kodları yerine generic validation kodu dönüyor

- **İlgili AC:** AC-2 (negatif/sıfır tutar `400`, hata gövdeleri **stabil kodlu**), AC-1 (tutar > 0; misafir `1..maxGuests`). REQ-2/Context hata kodu listesi: `pricing.invalid_amount`, `pricing.invalid_guests`.
- **Önem:** Orta.
- **Kanıt (HTTP):**

| İstek | HTTP | Dönen `code` | Beklenen (asgari) |
|---|---:|---|---|
| `POST /admin-api/pricing/manual-prices` `amountTry:"0.00"` | 400 | `validation.invalid_request` | `pricing.invalid_amount` |
| `POST /admin-api/pricing/manual-prices` `amountTry:"-1.00"` | 400 | `validation.invalid_request` | `pricing.invalid_amount` |
| `PUT /admin-api/pricing/rate-plans/{id}/prices` `amountTry:"0.00"` | 400 | `validation.invalid_request` | `pricing.invalid_amount` |
| `PUT /admin-api/pricing/rate-plans/{id}/prices` `amountTry:"-5.00"` | 400 | `validation.invalid_request` | `pricing.invalid_amount` |
| `PUT /admin-api/pricing/seasons/{id}/prices` `amountTry:"0.00"` | 400 | `validation.invalid_request` | `pricing.invalid_amount` |
| `POST /admin-api/pricing/manual-prices` `guests:0` | 400 | `validation.invalid_request` | `pricing.invalid_guests` |
| `PUT /admin-api/pricing/rate-plans/{id}/prices` `guests:0` | 400 | `validation.invalid_request` | `pricing.invalid_guests` |
| Karşılaştırma: aynı uçlarda `guests:maxGuests+1` | 400 | `pricing.invalid_guests` | `pricing.invalid_guests` |

- **Kullanıcı etkisi:** `admin-web/src/lib/error-messages.ts` içinde `pricing.invalid_amount` ve `pricing.invalid_guests` için özel Türkçe mesajlar var; ancak bu hatalar generic kodla döndüğü için ekranda **"İşlem sırasında beklenmeyen bir hata oluştu"** görünüyor. Örn. manuel fiyat tutarı `0,00` girilip "Manuel Fiyat Ekle" denendiğinde beklenen "Tutar sıfırdan büyük…" mesajı yerine generic mesaj çıkar.
- **Minimal repro:**
  1. `SUPER_ADMIN` ile `/pricing` ekranına gir.
  2. "Manuel Fiyatlar" formunda tutar `0,00` (veya `-1,00`) gir, "Manuel Fiyat Ekle"ye bas.
  3. Ağ yanıtında `400` ve `code: "validation.invalid_request"` görülür; UI generic hata gösterir.
  4. Aynı şeyi "Baz Fiyat Matrisi"nde bir hücreyi `0`/negatif yapıp kaydet ve "Sezon Fiyat Matrisi"nde tekrarla.
  5. Misafir sayısını `0` yaparak manuel fiyat ve baz matris isteğini tekrarla.
- **Gözlenen neden:** Pricing request DTO'larındaki `@DecimalMin("0.01")` / `@Min(1)` kısıtları, domain'e (`PriceRules.requirePositiveAmount` / `requireGuests`) ulaşmadan Spring validation'ı tetikliyor. `GlobalExceptionHandler` bu validation istisnalarını tek bir generic `validation.invalid_request` koduna eşliyor; bu yüzden domain'in stabil kodları üretilmiyor.
- **Fix önerisi (QA önerisi — karar Takım Yöneticisinde):**
  1. **Önerilen:** `ManualPriceCreateRequest`, `ManualPriceUpdateRequest` ve `PriceRowRequest` içindeki `@DecimalMin`/`@Min` kısıtlarını kaldır; `@NotNull` kalsın. `PriceRules` zaten `pricing.invalid_amount` / `pricing.invalid_guests` üretiyor. Böylece kuralın tek otoritesi domain olur.
  2. Alternatif: validation istisnalarını alan adına göre eşleyen bir handler ekle (`amountTry`/`amount` → `pricing.invalid_amount`, `guests` → `pricing.invalid_guests`); ancak bu, domain kuralını ikinci kez kodlamak demektir.
  3. Testler: `AdminPricingControllerTest` ve `PricingIT`'e sıfır/negatif tutar ve `guests:0` vakaları; `@pricing` E2E'de negatif tutar için yalnız `400` değil `code` da assert edilsin; sıfır tutar/misafir vakası eklensin; admin-web Vitest'te `pricing.invalid_amount` mesajı doğrulansın.
- **Gerekçe:** AC-2 "stabil kodlu" diyor; UI'de bu kodlara bağlı özel mesajlar var. Generic kod, hem API sözleşmesini hem kullanıcı deneyimini bozuyor; fiyat doğruluğunu etkilemiyor ama kapanış öncesi düzeltilmesi düşük maliyetli.

### [QA-2] Eşzamanlı iki "varsayılan plan yap" isteği de başarılı oluyor

- **İlgili AC:** AC-2 — "Aynı anda tek varsayılan plan korunur; **eşzamanlı iki varsayılan isteğinde yalnız biri başarılıdır**."
- **Önem:** Orta.
- **Kanıt:** İki ayrı admin oturumu, senkronize başlangıç (aynı UTC milisaniyesine kadar bekleyen iki ayrı job) ile:
  - `PUT /admin-api/pricing/rate-plans/{A}/default` → **200**
  - `PUT /admin-api/pricing/rate-plans/{B}/default` → **200**
  - Son durumda yalnız bir default vardır; ancak **iki istek de başarılı** dönmüştür.
  - QA çıktısı: `SESSION_A: STATUS=200 …`, `SESSION_B: STATUS=200 …`; ardından DB sorgusu `QA-COMPLETE-…` planını default gösterdi (daha sonra A'ya geri alındı).
- **Minimal repro:**
  1. En az iki aktif ve **tam** plan olsun (A ve B; tüm aktif oda tiplerinin `1..maxGuests` baz fiyatları dolu).
  2. İki ayrı admin oturumu aç (ayrı tarayıcı profilleri veya iki ayrı cookie jar).
  3. Aynı anda:
     - `PUT /admin-api/pricing/rate-plans/A/default`
     - `PUT /admin-api/pricing/rate-plans/B/default`
  4. Beklenen: biri `200`, diğeri `409` (örn. `pricing.rate_plan_in_use` / "başka bir varsayılan seçildi, yenileyin").
     Gözlenen: ikisi de `200`; son default, son işlenen isteğe göre değişiyor.
- **Gözlenen neden:** `setDefault`, tüm planları pessimistic lock ile serileştiriyor; ikinci işlem birincinin commit'ini bekleyip sonra kendi planını default yapıyor. DB'deki partial unique index yalnızca **nihai** tek-default durumunu koruyor; ikinci isteği reddetmiyor. Bu nedenle kodda bulunan `DataIntegrityViolationException` yakalama yolu bu senaryoda tetiklenmiyor.
- **Fix önerisi (QA önerisi — karar Takım Yöneticisinde):**
  1. **Önerilen:** `PUT /rate-plans/{id}/default` için bir önkoşul ekle: `expectedDefaultPlanId` (veya `If-Match`/ETag). İstek, istemcinin gördüğü mevcut default planı taşısın. `setDefault` kilit altında mevcut default'u kontrol etsin; `expectedDefaultPlanId` ile uyuşmuyorsa `409 pricing.rate_plan_in_use` dönsün. İlk default için `expectedDefaultPlanId=null` kabul edilsin. Admin-web, `rate-plans` sorgusundaki güncel `defaultPlan` ID'sini göndersin.
  2. Alternatif: `pricing` içinde singleton/default-pointer satırı + `version` ve koşullu `UPDATE ... WHERE version = ?`; istemci version gönderir, çakışmada `409`.
  3. Test: `PricingIT` içinde `ExecutorService` + `CountDownLatch` ile iki eşzamanlı `setDefault`; **bir 200 + bir 409** ve tam olarak bir default assert edilsin. Ayrıca admin-web mutation testi `expectedDefaultPlanId` gönderimini doğrulasın.
- **Gerekçe:** AC-2 açıkça "yalnız biri başarılıdır" diyor. Mevcut davranış veri bütünlüğünü bozmuyor (tek default kalıyor) ama bir operatöre yanlış başarı sinyali veriyor ve eşzamanlılık kabul kriterini karşılamıyor. Fix, yarışı sessizce son-kazanır yerine açık `409` + yenileme akışına çevirir.

## 4. Test Kapsamı Notları (bulguların kaçmasına yol açan boşluklar)

- `@pricing` E2E negatif tutar için yalnız `status=400` kontrol ediyor; `code` kontrolü yok. Sıfır tutar ve `guests:0` vakaları E2E'de yok. QA-1'in E2E'de yakalanması için bu assert'ler eklenmeli.
- Eşzamanlı `setDefault` için `PricingIT`'te bir test yok; mevcut IT'ler yalnız sıralı default değişimini doğruluyor. QA-2'nin kalıcı koruması için concurrency IT'si gerekli.
- Yerel `@empty` bu QA turunda koşturulmadı (QA DB seed'liydi); aynı commit'in CI run 111'indeki `@empty` adımı yeşil olduğu için kapsam CI kanıtıyla tamamlandı.

## 5. Takım Yöneticisi Kararı

QA aşağıdaki iki kararı istiyor:

1. **QA-1:** Fix kabul edilsin mi? Önerilen fix, DTO kısıtlarının kaldırılıp domain kodlarının kullanılması + testler. Alternatif olarak "generic validation kodu kabul edilebilir" denirse spec/AC-2 ve UI hata mesajı buna göre güncellenmeli.
2. **QA-2:** Fix kabul edilsin mi? AC-2 lafzı "yalnız biri başarılıdır" olduğu için fix öneriliyor. Yönetim "yalnız nihai tek default yeterli" yorumunu seçerse AC-2 metni netleştirilmeli; aksi halde expected-default/version önkoşulu uygulanmalı.

**QA sınırı:** QA kod yazmaz, testi düzeltmez/zayıflatmaz. Fix'ler developer tarafından yapılır; ardından QA yalnız bu iki bulgu + etkilenen `@pricing`/admin-web/IT kapsamıyla yeniden doğrulama yapar.

## 6. Referanslar

- Spec: `specs/0006-pricing.md`
- Integration commit: `8d717da` (`origin/integration`)
- CI: `https://github.com/altananay/hotel-management-system/actions/runs/42500749839`
- Yerel kanıtlar: `.local/qa-0006-*` ve `.local/qa-0006-e2e-evidence/0006/`
- İlgili dosyalar (fix için): `backend/hotel-api/src/main/java/com/hotel/pricing/internal/web/dto/ManualPriceCreateRequest.java`, `ManualPriceUpdateRequest.java`, `PriceRowRequest.java`; `backend/hotel-api/src/main/java/com/hotel/web/GlobalExceptionHandler.java`; `backend/hotel-api/src/main/java/com/hotel/pricing/internal/application/RatePlanService.java`; `admin-web/src/features/pricing/use-pricing-mutations.ts`; `admin-web/src/lib/error-messages.ts`.

---

**QA imzası:** "QA, Dev'in penceresine girmez." Bu brief davranış kanıtı ve fix önerisi içerir; kod değişikliği içermez.

## 7. Developer yanıtı (2026-11-08)

- **QA-1 uygulandı:** DTO `@DecimalMin`/`@Min` kısıtları kaldırıldı; sıfır/negatif tutar
  `pricing.invalid_amount`, sıfır misafir `pricing.invalid_guests` döner.
- **QA-2 uygulandı:** `expectedDefaultPlanId` önkoşulu eklendi; eşzamanlı iki istekten
  yalnız biri başarılı olur, eskimiş değer `409 pricing.rate_plan_in_use` döner.
- Testler: `backend/hotel-api` 76 test (`PricingIT` 18), admin-web 50 Vitest,
  `@pricing` E2E 1 passed.
- Ayrıntı: `specs/done/evidence/0006/QA-1-QA-2-fix-summary.md`.
- QA yeniden doğrulaması bekleniyor.
