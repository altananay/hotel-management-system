# 0006 — Test ve CI Özeti

Tarih: 2026-11-08

## Backend

- `backend/hotel-api`: `.\mvnw.cmd -B verify`
  - Sonuç: `BUILD SUCCESS`
  - Unit + integration: `Tests run: 69, Failures: 0, Errors: 0, Skipped: 0`
  - `PricingIT`: 11 test (şema, TRY/EUR/USD, manuel fiyat/kur, sezon fallback,
    default bütünlüğü, overlap/duplicate, snapshot değişmezliği, ayar etkisi)
  - `AdminPricingControllerTest`: 5 test (401/403, 503, eksik liste, MANAGER erişimi)
  - `TcmbFxParserTest` + `TcmbFxServiceTest`: hafta içi/hafta sonu/bozuk XML/erişilemezlik.
  - `HotelApiModularityTest`: `pricing → catalog` izinli read kenarı yeşil.
- TCMB ağı kullanılmadı; fixture/fake ve `hotel.pricing.tcmb.enabled=false` kullanıldı.

## Admin-web

- `npm run lint`: geçti (ESLint + `tsc --noEmit`).
- `npm test`: 43 test geçti; `PricingManager` yükleme/boş/hata/dolu, bayat kur
  uyarısı ve plan düzenleme.
- `npm run build`: başarılı; `/pricing` route'u üretildi.

## E2E

- `npx playwright test tests/pricing --grep @pricing`: 1 test geçti.
- Akış: SUPER_ADMIN login → yükleme/boş/hata/dolu → plan → sezon → baz/sezon
  fiyat matrisi → varsayılan plan → manuel kur → manuel fiyat → TRY/EUR/USD
  public teklif → buffer/yuvarlama güncellemesinin yeni teklife etkisi →
  geçersiz tarih/misafir/para birimi/negatif tutar → oda vitrininde fiyat yok →
  FRONT_DESK okuma/yazma 403.
- Kanıtlar: `AC-2-admin-pricing-{loading,empty,error,filled}.png`,
  `AC-3-admin-fx-warning.png` (ayrı görsel), `AC-4-quote-response.md`,
  `AC-5-snapshot-immutability.md`.
