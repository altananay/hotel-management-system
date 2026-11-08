# 0006 — Kapanış Raporu (Takım Yöneticisi onayı verildi — 2026-11-08)

## 0007 planlama kaydı

**0007 oda detay sayfası spec'i, oda vitrini kartlarında fiyat gösterimini zorunlu olarak
içerecektir.** 0007 yazılırken bu madde kontrol edilecektir.

## Kod incelemesi — Rev.1 ve takip turu

- İlk tur: 17 bulgu; 15 doğrudan uygulandı; **CR-10 (maksimum gece)** ve
  **CR-13 (application→web)** ertelendi.
- Rev.1 turu (CR-01, CR-03, CR-14) Takım Yöneticisi kararlarıyla uygulandı:
  - **CR-01:** Baz/sezon fiyat satırı `DELETE` endpointleri + admin-ui satır silme +
    testler. Silme snapshot'ları etkilemez; default plan varsayılan kalır ve eksik
    kombinasyon `409` döner.
  - **CR-03:** `internal.application` → `internal.web` bağımlılığı kaldırıldı;
    application view record'ları + controller/mapper dönüşümü; ArchUnit kuralı eklendi.
    Bu, ilk turdaki **CR-13'ü kapatır**.
  - **CR-14:** Maksimum 30 gece; 31 gece `400 pricing.invalid_period`; sınır
    `hotel.pricing.max-stay-nights` ile yapılandırılabilir. Bu, ilk turdaki
    **CR-10'u kapatır**.
- Takip turu:
  - **CR-19:** Admin-web 404/403 delete hata testleri eklendi; 404'te matris refetch
    edilir ve kullanıcı dostu mesaj gösterilir.
  - **CR-20:** Kanıt ifadesi “gerçek HTTP” yerine “endpoint-level MockMvc” olarak
    düzeltildi; gerçek JWT/network testi iddia edilmiyor.
  - **CR-21:** SCORECARD ve kapanış raporu CR-10→CR-14 / CR-13→CR-03 eşleşmesiyle
    uzlaştırıldı.
- **Açık bulgu yok.** Anonim quote için rate limiting/operasyonel hardening ayrı
  spec/backlog konusudur.

## QA bulguları — Rev.2

- **QA-1:** Fiyat DTO'larındaki `@DecimalMin`/`@Min` kısıtları kaldırıldı; sıfır/negatif
  tutar `pricing.invalid_amount`, sıfır misafir `pricing.invalid_guests` döner.
  `AdminPricingControllerTest`, `PricingIT`, admin-web Vitest ve `@pricing` E2E ile
  doğrulandı.
- **QA-2:** Varsayılan plan endpoint'i `expectedDefaultPlanId` önkoşulu taşır; iki **farklı
  plana** eşzamanlı geçişte yalnız biri başarılı olur, zaten varsayılan plana istek
  idempotenttir; eskimiş önkoşul `409 pricing.rate_plan_in_use` döner. `PricingIT`
  eşzamanlılık testi ve admin-web mutation testi eklendi.
- QA yeniden doğrulaması 2026-11-08'de tamamlandı; QA-1/QA-2 kapandı, açık bulgu yok
  (`specs/0006-qa-retest.md`).

## Test özeti

- `backend/hotel-api`: `mvn -B verify` → 76 test, 0 failure/error; `PricingIT` 18 test.
- `backend/admin-api`: 5/5 test (QA retest, 2026-11-08).
- `admin-web`: ESLint + `tsc --noEmit` geçti; 50 Vitest testi; `next build` başarılı.
- E2E: `npx playwright test tests/pricing --grep @pricing` → 1 passed; sıfır/negatif
  tutar, sıfır misafir ve eskimiş varsayılan önkoşulu kodları doğrulanır.
- 0001–0005 kanıt arşivleri: `git diff` + untracked kontrolü temiz.

## Kapanış durumu

- Takım Yöneticisi kapanış onayı 2026-11-08'de verildi.
- Spec `specs/done/0006-pricing.md` altına taşındı; DoD tamamlandı.
- QA-1/QA-2 kapandı; QA retest `specs/0006-qa-retest.md`; açık bulgu yok.
- `specs/0006-qa-findings.md`, `specs/0006-qa-retest.md` ve `specs/done/evidence/0006/`
  kanıtları korundu; `0001–0005` arşivlerine dokunulmadı.
- SCORECARD dolduruldu (spec revizyon 2, düzeltme turu 4).
