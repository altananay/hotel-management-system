# 0006 — Test ve CI Özeti

Tarih: 2026-11-08

## Backend

- `backend/hotel-api`: `.\mvnw.cmd -B verify`
  - Sonuç: `BUILD SUCCESS`
  - Unit + integration: `Tests run: 64, Failures: 0, Errors: 0, Skipped: 0`
  - `PricingIT`: 6 test (şema, TRY/EUR teklifi, hata kodları, default bütünlüğü)
  - `AdminPricingControllerTest`: 5 test (401/403, 503, eksik liste, MANAGER erişimi)
  - `HotelApiModularityTest`: `pricing → catalog` izinli read kenarı yeşil.
- TCMB ağı kullanılmadı; `TcmbFxParserTest` fixture'ları ve `hotel.pricing.tcmb.enabled=false` kullanıldı.

## Admin-web

- `npm run lint`: geçti (ESLint + `tsc --noEmit`).
- `npm test`: 42 test geçti; `PricingManager` yükleme/boş/hata/dolu ve bayat kur uyarısı.
- `npm run build`: başarılı; `/pricing` route'u üretildi.

## E2E

- `npx playwright test tests/pricing --grep @pricing`: 1 test geçti.
- Akış: SUPER_ADMIN login → yükleme/boş/hata/dolu → plan → sezon → baz fiyat →
  varsayılan plan → manuel kur → manuel fiyat → TRY/EUR public teklif →
  oda vitrininde fiyat yok → FRONT_DESK 403.
- Kanıtlar: `AC-2-admin-pricing-{loading,empty,error,filled}.png`,
  `AC-3-admin-fx-warning.png`, `AC-4-quote-response.md`,
  `AC-5-snapshot-immutability.md`.
