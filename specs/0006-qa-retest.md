# Spec 0006 — QA Yeniden Doğrulama (Retest)

> **Rol:** QA (`docs/roles/qa.md`)
> **Tarih:** 2026-11-08
> **İnceleme dalı / commit:** `integration` / `9532e25` (`fix(pricing): close QA-1 and QA-2 findings (#37) [plan 0006]`)
> **Önceki brief:** `specs/0006-qa-findings.md`
> **Durum:** QA-1 **kapandı**; QA-2 **kapandı**. §5'teki no-op kenarı Takım Yöneticisi tarafından 2026-11-08'de kabul edildi; **açık QA bulgusu yok**.

## 1. Sonuç Özeti

| Bulgu | Durum | Kısa kanıt |
|---|---|---|
| QA-1 — sıfır/negatif tutar ve sıfır misafir generic kod dönüyordu | **KAPANDI** | Create/base/season/update yollarında `pricing.invalid_amount` / `pricing.invalid_guests`; E2E ve admin-web testleri yeşil |
| QA-2 — eşzamanlı iki varsayılan isteği de başarılı oluyordu | **KAPANDI (çakışan değişiklik)** | İki farklı plana eşzamanlı geçişte bir `200`, bir `409 pricing.rate_plan_in_use`; tek default |
| QA-2 literal AC kenarı — biri zaten default olan plana no-op, diğeri başka plana geçiş | **KABUL EDİLDİ** | No-op idempotent kabul edildi; AC "iki farklı plana eşzamanlı geçiş" olarak yorumlanacak (Takım Yöneticisi, 2026-11-08) |

**Regresyon:** 0.
**Kapanış engeli:** Yok. Spec kapanışı analist tarafından yapılabilir.

## 2. Çalıştırılan Testler

| Katman | Komut | Sonuç | Kanıt |
|---|---|---|---|
| hotel-api | `.\mvnw.cmd -B verify` | **BUILD SUCCESS**; 76 test, 0 failure/error; `PricingIT` 18/18 | `.local/qa-0006-retest-hotel-api.log` |
| admin-api | `.\mvnw.cmd -B verify` | **BUILD SUCCESS**; 5/5 | `.local/qa-0006-retest-admin-api.log` |
| admin-web | `npm run lint`, `npm run build`, `npm test` | lint 0, build 0, **50/50 Vitest** | `.local/qa-0006-retest-admin-web.log` |
| E2E `@pricing` | `npx playwright test tests/pricing --grep '@pricing'` | **1/1 passed** | `.local/qa-0006-retest-e2e.log` |
| E2E `@seeded` (0002) | `npx playwright test tests/smoke.spec.ts --grep '@seeded'` | **2/2 passed** | `.local/qa-0006-retest-e2e-0002.log` |
| E2E `@admin` (0004) | `npx playwright test tests/admin --grep '@admin'` | **3/3 passed** | `.local/qa-0006-retest-e2e-0004.log` |
| E2E `@media` (0005) | `npx playwright test tests/media --grep '@media'` | **1/1 passed** | `.local/qa-0006-retest-e2e-0005.log` |
| CI run 113 | integration `9532e25` | Tüm job'lar **success** (backend, frontend, E2E, quality gate) | `https://github.com/altananay/hotel-management-system/actions/runs/42509290509` |

CI run 113, `@empty` → seed → `@seeded` → `@admin` → `@media` → `@pricing` sırasını ve 0001–0005 arşiv kontrolünü içerir. Yerel QA veritabanı seed'li olduğu için `@empty` yerelde tekrar koşturulmadı; CI kanıtı kullanıldı.

## 3. QA-1 Kapanış Kanıtı

Fix: `ManualPriceCreateRequest`, `ManualPriceUpdateRequest`, `PriceRowRequest` üzerindeki `@DecimalMin`/`@Min` kısıtları kaldırıldı; doğrulama domain `PriceRules` üzerinden yapılıyor.

Doğrudan HTTP probe'ları (SUPER_ADMIN admin oturumu):

| İstek | HTTP | `code` |
|---|---:|---|
| `POST /admin-api/pricing/manual-prices`, `amountTry:"0.00"` | 400 | `pricing.invalid_amount` |
| `POST /admin-api/pricing/manual-prices`, `amountTry:"-1.00"` | 400 | `pricing.invalid_amount` |
| `POST /admin-api/pricing/manual-prices`, `guests:0` | 400 | `pricing.invalid_guests` |
| `PUT /admin-api/pricing/rate-plans/{id}/prices`, `amountTry:"0.00"` | 400 | `pricing.invalid_amount` |
| `PUT /admin-api/pricing/rate-plans/{id}/prices`, `guests:0` | 400 | `pricing.invalid_guests` |
| `PUT /admin-api/pricing/seasons/{id}/prices`, `amountTry:"0.00"` | 400 | `pricing.invalid_amount` |
| `PATCH /admin-api/pricing/manual-prices/{id}`, `amountTry:"0.00"` | 400 | `pricing.invalid_amount` |
| `PATCH /admin-api/pricing/manual-prices/{id}`, `guests:0` | 400 | `pricing.invalid_guests` |

Update yolu gerçek tarayıcı fetch'iyle (admin-web oturumu, CSRF cookie) doğrulandı. Ek olarak:
- `admin-web` Vitest: `pricing.invalid_amount` için kullanıcı mesajı testi geçti.
- `@pricing` E2E: negatif/sıfır tutar ve sıfır misafir için HTTP `code` assert'leri geçti.
- Geçerli manuel fiyat oluşturma/silme akışı bozulmadı (E2E ve admin-web testleri).

## 4. QA-2 Kapanış Kanıtı

Fix: `PUT /admin-api/pricing/rate-plans/{id}/default` artık `expectedDefaultPlanId` taşıyor; `RatePlanService` kilit altında mevcut default ile karşılaştırıyor; uyuşmazsa `409 pricing.rate_plan_in_use` dönüyor.

Bağımsız probe'lar:
1. **Eskimiş önkoşul:** Mevcut default P1 iken P2'yi `expectedDefaultPlanId=P1` ile set etmek → `200`; mevcut default P2 iken P1'i eski `P1` beklentisiyle set etmek → **409 `pricing.rate_plan_in_use`**.
2. **Eşzamanlı çakışan değişiklik:** Mevcut default P2; iki ayrı oturum, senkronize başlangıçla P1 ve P3'e `expectedDefaultPlanId=P2` gönderdi:
   - P1 isteği → **409 `pricing.rate_plan_in_use`**
   - P3 isteği → **200**
   - DB'de **tek default**: P3.
3. **UI entegrasyonu:** Gerçek tarayıcıda "Varsayılan Yap" tıklandı; istek gövdesi `{"expectedDefaultPlanId":"<mevcut default ID>"}` olarak gözlendi. Eksik fiyatlı bir plana geçişte UI `409 pricing.default_rate_plan_incomplete` + eksik kombinasyon listesini gösterdi; ekran çökmedi.
4. **Plan düzenleme:** Gerçek tarayıcıda plan PATCH isteği `200` döndü; plan adı güncellendi.

`PricingIT` içindeki yeni eşzamanlılık testi (`ExecutorService` + `CountDownLatch`) iki ayrı plana geçişte bir başarı + bir `409` ve tek default doğruluyor; 18/18 IT geçti.

## 5. Kapanış Notu — Takım Yöneticisi Kararı (Kabul Edildi)

AC-2'nin yeni metni "eşzamanlı iki varsayılan isteğinde yalnız biri başarılıdır" diyor. Aşağıdaki özel durumda iki istek de `200` dönebilir:

- Mevcut default P3.
- Aynı anda:
  - `PUT /rate-plans/P3/default` (`expectedDefaultPlanId=P3`) → zaten default olan plana **no-op**; `200`.
  - `PUT /rate-plans/P1/default` (`expectedDefaultPlanId=P3`) → P1'e geçiş; `200`.
- Sonuç: tek default (P1), iki başarılı yanıt.

**Takım Yöneticisi kararı (2026-11-08):** No-op isteğin idempotent kabul edilmesi onaylandı. AC-2, "iki farklı plana eşzamanlı geçişte yalnız biri başarılıdır; zaten varsayılan plana yapılan istek idempotenttir" şeklinde netleştirilecek. Bu madde için ek kod düzeltmesi yapılmayacak.

**Gerekçe:**
- Bu, iki **farklı** plana yarışan geçiş değildir; bir istek idempotent no-op'tur.
- Admin UI, default plan satırında "Varsayılan Yap" butonu göstermiyor; bu senaryo UI'dan üretilemiyor, yalnız doğrudan API çağrısıyla mümkün.
- No-op'u yarışta reddetmek operasyonel fayda sağlamaz; ek sürüm/önkoşul karmaşıklığı getirir.

## 6. Regresyon ve Arşiv

- CI run 113: backend, frontend, E2E ve quality gate **success**.
- Yerel `@seeded`, `@admin`, `@media`, `@pricing` yeşil.
- `specs/done/evidence/0001`–`0005` üzerinde `git diff` ve untracked kontrolü temiz.
- QA-1/QA-2 fix'leri 0001–0005 davranışlarını değiştirmedi.

## 7. Karar

- **QA-1:** Kapandı; ek iş yok.
- **QA-2:** Kapandı; çakışan değişiklik senaryosu ve no-op yorumu onaylandı.
- **Açık QA bulgusu:** Yok.
- Spec'teki `[ ] QA yeniden doğrulaması` maddesi bu raporla karşılanmıştır; işaretleme analist/Takım Yöneticisi kapanışında yapılmalıdır.

## 8. Kanıt Yolları

- Loglar: `.local/qa-0006-retest-*.log`
- E2E kanıtları: `.local/qa-0006-retest-e2e-evidence/0006/`
- Önceki QA brief'i: `specs/0006-qa-findings.md`
- Fix özeti: `specs/done/evidence/0006/QA-1-QA-2-fix-summary.md`
- CI: `https://github.com/altananay/hotel-management-system/actions/runs/42509290509`

## 9. Analist Kapanışına Devir (Öneri)

Spec kapanışı analist tarafından yapılırken:

1. **AC-2 cümlesi netleştirilmeli:** "Aynı anda tek varsayılan plan korunur; **iki farklı plana** eşzamanlı geçiş isteğinde yalnız biri başarılıdır; zaten varsayılan plana yapılan istek idempotenttir; eskimiş `expectedDefaultPlanId` `409 pricing.rate_plan_in_use` döner."
2. **Context API sözleşme notu güncellenmeli:** `PUT /admin-api/pricing/rate-plans/{id}/default` için istek gövdesi `{"expectedDefaultPlanId":"<uuid|null>"}` olarak belirtilmeli; ilk varsayılan için `null`.
3. **DoD:** `[ ] QA yeniden doğrulaması` maddesi bu retest raporuna atıfla işaretlenebilir.
4. **Arşiv:** Spec `specs/done/`e taşınırken `specs/0006-qa-findings.md`, `specs/0006-qa-retest.md` ve `specs/done/evidence/0006/` kanıtları korunmalı; `0001–0005` arşivlerine dokunulmamalı.

QA açısından kapanış için başka iş yoktur.
