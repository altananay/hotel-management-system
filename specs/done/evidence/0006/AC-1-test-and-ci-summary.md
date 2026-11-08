# 0006 — Test ve CI Özeti (Rev.1)

Tarih: 2026-11-08

## Backend

- `backend/hotel-api`: `.\mvnw.cmd -B verify`
  - Sonuç: `BUILD SUCCESS`
  - Unit + integration: `Tests run: 74, Failures: 0, Errors: 0, Skipped: 0`
  - `PricingIT`: 16 test (şema, TRY/EUR/USD, manuel fiyat/kur, sezon fallback,
    default bütünlüğü, overlap/duplicate, snapshot değişmezliği, ayar etkisi,
    30/31 gece sınırı, baz/sezon fiyat silme, yanlış plan/sezon sahipliği ve
    yanlış plan üzerinden HTTP `DELETE` 404).
  - `AdminPricingControllerTest`: 8 test (401/403, 204 delete, 404 yanlış/eksik satır,
    503, eksik liste, MANAGER erişimi).
  - `PricingPropertiesTest`: varsayılan 30 gece, property binding, geçersiz değer.
  - `QuoteServiceMaxStayTest`: sabit `Clock` ile 30/31 gece sınırı ve DB sorgusundan
    önce reddetme.
  - `TcmbFxParserTest` + `TcmbFxServiceTest`: hafta içi/hafta sonu/bozuk XML/erişilemezlik.
  - `HotelApiModularityTest`: `pricing → catalog` izinli read kenarı ve
    `internal.application → internal.web` yasağı yeşil.
- TCMB ağı kullanılmadı; fixture/fake ve `hotel.pricing.tcmb.enabled=false` kullanıldı.

## Admin-web

- `npm run lint`: geçti (ESLint + `tsc --noEmit`).
- `npm test`: 45 test geçti; `PricingManager` yükleme/boş/hata/dolu, bayat kur
  uyarısı, plan düzenleme, baz/sezon fiyat satırı silme ve pasif oda tipinde
  silme butonunun görünmemesi.
- `npm run build`: başarılı; `/pricing` route'u üretildi.

## E2E

- `npx playwright test tests/pricing --grep @pricing`: 1 test geçti.
- Akış: SUPER_ADMIN login → yükleme/boş/hata/dolu → plan → sezon → baz/sezon
  fiyat matrisi → varsayılan plan → manuel kur → manuel fiyat → TRY/EUR/USD
  public teklif → buffer/yuvarlama güncellemesi → 30 gece kabul / 31 gece 400 →
  sezon fiyatı silme (baz fallback) → baz fiyat silme (409 + eksik uyarısı) →
  geçersiz tarih/misafir/para birimi/negatif tutar → oda vitrininde fiyat yok →
  FRONT_DESK okuma/yazma/silme 403.
- Kanıtlar: `AC-2-admin-pricing-{loading,empty,error,filled,delete}.png`,
  `AC-3-admin-fx-warning.png` (ayrı görsel), `AC-4-quote-response.md`,
  `AC-5-snapshot-immutability.md`.
