# 0006 — Kapanış Raporu (Takım Yöneticisi onayına sunulur)

## 0007 planlama kaydı

**0007 oda detay sayfası spec'i, oda vitrini kartlarında fiyat gösterimini zorunlu olarak
içerecektir.** 0007 yazılırken bu madde kontrol edilecektir.

## Kod incelemesi — Rev.1

- İlk tur: 17 bulgu; 15 gerçek/uygulandı, 2 kapsam/katman itirazı (CR-10, CR-13).
- Rev.1 turu (CR-01, CR-03, CR-14) Takım Yöneticisi kararlarıyla uygulandı:
  - **CR-01:** Baz/sezon fiyat satırı `DELETE` endpointleri + admin-ui satır silme +
    testler. Silme snapshot'ları etkilemez; default plan varsayılan kalır ve eksik
    kombinasyon `409` döner.
  - **CR-03:** `internal.application` → `internal.web` bağımlılığı kaldırıldı;
    application view record'ları + controller/mapper dönüşümü; ArchUnit kuralı eklendi.
  - **CR-14:** Maksimum 30 gece; 31 gece `400 pricing.invalid_period`; sınır
    `hotel.pricing.max-stay-nights` ile yapılandırılabilir.
- Açık takip: anonim quote için rate limiting/operasyonel hardening (0007 veya ayrı spec).

## Test özeti

- `backend/hotel-api`: `mvn -B verify` → 73 test, 0 failure/error; `PricingIT` 15 test.
- `backend/admin-api`: değişiklik yok; mevcut doğrulama CI'da.
- `admin-web`: ESLint + `tsc --noEmit` geçti; 45 Vitest testi; `next build` başarılı.
- E2E: `npx playwright test tests/pricing --grep @pricing` → 1 passed.
- 0001–0005 kanıt arşivleri: `git diff` + untracked kontrolü temiz.

## Kapanış durumu

- Spec `specs/0006-pricing.md` olarak bırakıldı; DoD gereği Takım Yöneticisi kapanış onayından
  sonra `specs/done/` altına taşınacak.
- SCORECARD dolduruldu (spec revizyon 1, düzeltme turu 2).
