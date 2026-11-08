# 0006 — Kapanış Raporu (Takım Yöneticisi onayına sunulur)

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

## Test özeti

- `backend/hotel-api`: `mvn -B verify` → 74 test, 0 failure/error; `PricingIT` 16 test.
- `backend/admin-api`: değişiklik yok; mevcut doğrulama CI'da.
- `admin-web`: ESLint + `tsc --noEmit` geçti; 47 Vitest testi; `next build` başarılı.
- E2E: `npx playwright test tests/pricing --grep @pricing` → 1 passed.
- 0001–0005 kanıt arşivleri: `git diff` + untracked kontrolü temiz.

## Kapanış durumu

- Spec `specs/0006-pricing.md` olarak bırakıldı; DoD gereği Takım Yöneticisi kapanış onayından
  sonra `specs/done/` altına taşınacak.
- SCORECARD dolduruldu (spec revizyon 1, düzeltme turu 2).
