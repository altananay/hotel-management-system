# 0006 — QA-1 / QA-2 Düzeltme Özeti

Tarih: 2026-11-08
Kaynak: `specs/0006-qa-findings.md`

## QA-1 — Stabil fiyat doğrulama kodları

- `ManualPriceCreateRequest`, `ManualPriceUpdateRequest` ve `PriceRowRequest`
  üzerindeki `@DecimalMin` / `@Min` kısıtları kaldırıldı; `@NotNull` alanlar korundu.
- Sıfır/negatif tutar artık domain `PriceRules.requirePositiveAmount` üzerinden
  `pricing.invalid_amount`, sıfır/negatif misafir `PriceRules.requireGuests` üzerinden
  `pricing.invalid_guests` döner.
- Kapsam:
  - `AdminPricingControllerTest`: sıfır/negatif tutar ve sıfır misafir isteklerinin
    controller'a ulaştığını ve stabil kodları döndürdüğünü doğrular.
  - `PricingIT`: manuel fiyat, baz matris ve sezon matrisi için gerçek servis
    doğrulaması; `pricing.invalid_amount` ve `pricing.invalid_guests`.
  - admin-web Vitest: `pricing.invalid_amount` için kullanıcı mesajı.
  - `@pricing` E2E: sıfır/negatif tutar ve sıfır misafir için HTTP `code` assert'i.

## QA-2 — Eşzamanlı varsayılan plan değişimi

- `PUT /admin-api/pricing/rate-plans/{id}/default` artık isteğe bağlı
  `{"expectedDefaultPlanId": "<uuid|null>"}` gövdesi kabul eder.
- `RatePlanService.setDefault(id, expectedDefaultPlanId)` kilit altında mevcut
  varsayılanı kontrol eder; istemcinin gördüğü değer farklıysa
  `409 pricing.rate_plan_in_use` döner. İlk varsayılan için `null` geçerlidir.
- admin-web, `rate-plans` sorgusundaki güncel `defaultPlan` ID'sini gönderir;
  çakışmada listeyi yenileme mesajı gösterir.
- Kapsam:
  - `PricingIT`: `ExecutorService` + `CountDownLatch` ile iki eşzamanlı istek;
    bir başarı, bir `409 pricing.rate_plan_in_use`, tam olarak bir varsayılan.
  - `AdminPricingControllerTest`: `expectedDefaultPlanId` gövdesinin servise
    aktarıldığı ve eskimiş değerde `409` döndüğü doğrulanır.
  - admin-web Vitest: gönderilen `expectedDefaultPlanId` ve çakışma mesajı.
  - `@pricing` E2E: eskimiş `expectedDefaultPlanId` için `409` ve stabil kod.

## Doğrulama

- `backend/hotel-api`: `.\mvnw.cmd -B verify` → **BUILD SUCCESS**, 76 test,
  0 failure/error; `PricingIT` 18 test.
- `admin-web`: `npm run lint` (ESLint + `tsc --noEmit`) geçti; **50 Vitest**
  testi geçti; `npm run build` başarılı.
- E2E: `E2E_EVIDENCE_DIR=<temp> npx playwright test tests/pricing --grep @pricing`
  → **1 passed**.
- E2E kanıtları geçici köke yazıldı; 0001–0005 arşivleri değişmedi.

## Durum

- QA-1 ve QA-2 developer tarafında kapatıldı.
- QA yeniden doğrulaması 2026-11-08'de tamamlandı; QA-1/QA-2 kapandı, açık bulgu yok
  (`specs/0006-qa-retest.md`).
