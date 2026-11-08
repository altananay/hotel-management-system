# Spec 0006 — Code Review Düzeltme Brief'i (CR-01, CR-03, CR-14)

> **Amaç:** 0006 fiyatlandırma geliştirmesinin Code Reviewer turunda açılan üç bulgusunu,
> Takım Yöneticisi'nin 2026-11-08 tarihli kararları doğrultusunda kapatmak.
>
> **Kapsam:** Yalnız CR-01 (baz/sezon fiyatı delete endpointleri), CR-03 (application → web
> bağımlılığının kaldırılması) ve CR-14 (maksimum 30 gece). Diğer CR bulguları bu brief'in
> kapsamı dışındadır; ayrı karar/tur gerektirir.
>
> **Durum:** Takım Yöneticisi kararları onaylandı; uygulama ve spec revizyonu bekliyor.
> **Tarih:** 2026-11-08
> **Hazırlayan:** Code Reviewer
> **Onaylayan:** Takım Yöneticisi
> **İlgili spec:** `specs/0006-pricing.md`
> **İlgili bulgular:** CR-01, CR-03, CR-14 (0006 Code Review turu, 2026-11-08)
> **Branch:** Mevcut `feature/0006-pricing` üzerinde küçük commit'ler; istenirse
> `feature/0006-pricing`'ten `fix/0006-code-review` açılabilir. Korumalı branch'lere
> doğrudan commit yasaktır.

## 1. Takım Yöneticisi kararları

| Bulgu | Karar | Uygulama yönü |
|---|---|---|
| CR-01 | Delete endpointleri ile ilerle | `PUT` replace semantiği değiştirilmez; baz ve sezon fiyat satırları için ayrı `DELETE` uçları eklenir. UI'da satır bazlı silme aksiyonu olur. |
| CR-03 | Öneri kabul edildi | `internal.application` → `internal.web` bağımlılığı kaldırılır; application kendi view record'larını döndürür, DTO/mapper dönüşümü web katmanında yapılır. |
| CR-14 | Eklenecek; şimdilik 30 gün | Teklif en fazla 30 gece olur; 31 gece `400 pricing.invalid_period` döner. Değer config'ten yönetilir, varsayılan 30'dur. |

## 2. Ortak ilkeler

1. **Spec revizyonu önce gelir.** CR-01'in yeni API/error code'ları ve CR-14'ün 30 gece kuralı
   `specs/0006-pricing.md` içine işlenmeden kod yazılmaz. Bu brief'teki delta metinleri
   Takım Yöneticisi onayına sunulur; onay sonrası spec rev.1 olarak güncellenir.
2. **Küçük commit'ler.** Her adım ayrı commit; commit mesajı İngilizce, Conventional Commits
   ve `[plan 0006/n]` atıflıdır.
3. **Kanıtsız iş bitmez.** Her kabul kriteri en az bir testle eşleşir; UI değişiklikleri
   Vitest/RTL/MSW, API davranışı IT/web slice, kritik akış `@pricing` E2E ile doğrulanır.
4. **Snapshot değişmezliği korunur.** CR-01 silme işlemleri mevcut fiyat snapshot'larını
   etkilemez; CR-14 yalnız yeni teklifleri etkiler.
5. **Kapsam dışı:** CR-02, CR-04–CR-13, CR-15–CR-18; public fiyat UI'si (0007); booking
   snapshot kalıcılığı; rate limiting; `PUT` replace semantiği; yeni spec numarası.

## 3. CR-01 — Baz ve sezon fiyatı delete endpointleri

### 3.1 API sözleşmesi

İki yeni admin endpoint'i eklenir; ikisi de `SUPER_ADMIN`/`MANAGER` rolü ister ve 0004
CSRF/Origin kurallarına tabidir. `admin-api` gateway'i mevcut `/admin-api/**` pass-through
sayesinde ek route değişikliği gerektirmez.

| Method | Path | Başarı | Hata |
|---|---|---|---|
| `DELETE` | `/admin-api/pricing/rate-plans/{ratePlanId}/prices/{priceRowId}` | `204 No Content` | Plan yok: `404 pricing.rate_plan_not_found`; satır yok/plana ait değil: `404 pricing.rate_plan_price_not_found`; yetkisiz rol: `403 security.forbidden` |
| `DELETE` | `/admin-api/pricing/seasons/{seasonId}/prices/{priceRowId}` | `204 No Content` | Sezon yok: `404 pricing.season_not_found`; satır yok/sezona ait değil: `404 pricing.season_price_not_found`; yetkisiz rol: `403 security.forbidden` |

Kurallar:

- Satır, path'teki üst kayda ait değilse silinmez; `404` döner. Başka planın/sezonun satırı
  path üzerinden silinemez.
- Silme idempotent olmak zorunda değildir; ikinci çağrı `404` dönebilir.
- Başarılı silme `204` ve gövdesizdir.
- Silme sonrası `updatedAt`/audit kaydı üretilmez; 0006'da fiyat audit'i yoktur. Uygulama
  logu + `X-Request-Id` yeterlidir.
- Varsayılan plandan baz fiyat silmek **engellenmez**. Plan varsayılan kalır; o oda tipi +
  misafir sayısı için public teklif `409 pricing.price_not_configured` döner, diğer
  kombinasyonlar çalışmaya devam eder. Admin ekranında `missing` uyarısı görünür.
- Sezon fiyatı silinirse o sezon için plan baz fiyatına düşülür; baz fiyat da yoksa
  `409 pricing.price_not_configured` döner.

### 3.2 Backend uygulaması

- `PricingErrorCodes` içine `pricing.rate_plan_price_not_found` ve
  `pricing.season_price_not_found` eklenir; `DomainErrorCategory.NOT_FOUND` kullanılır.
- `RatePlanPriceJpaRepository` ve `SeasonPriceJpaRepository` için üst kayda ait satırı
  getiren türetilmiş sorgular eklenir (ör. `findByIdAndRatePlanId`,
  `findByIdAndSeasonId`). Alternatif: `findById` + üst kimlik kontrolü; ancak tek sorgu
  tercih edilir.
- `PriceMatrixService`:
  - `deleteBasePrice(UUID ratePlanId, UUID priceRowId)`: planı kilitle
    (`requireLockedPlan`), satırı üst kayda ait olarak bul, yoksa
    `pricing.rate_plan_price_not_found` fırlat, sil.
  - `deleteSeasonPrice(UUID seasonId, UUID priceRowId)`: sezonu bul, sezonun planını kilitle,
    satırı sezona ait olarak bul, yoksa `pricing.season_price_not_found` fırlat, sil.
  - Silme, mevcut `saveBasePrices`/`saveSeasonPrices` ile aynı plan kilidini kullanır; eşzamanlı
    fiyat kaydetme ile yarışı önler.
- `PricingAdminService` iki delete metodu ekler; `AdminRatePlanController` iki
  `@DeleteMapping` endpoint'i açar. Sınıf seviyesindeki `@PreAuthorize` korunur.
- `PricingWebMapper`/DTO değişikliği gerekmez; delete `204` döner.

### 3.3 Admin-web uygulaması

- `use-pricing-mutations.ts` içine `deleteBasePrice` ve `deleteSeasonPrice` mutation'ları
  eklenir; `adminApiSend` ile `DELETE` çağrısı yapılır, başarıda `["pricing"]` query'leri
  invalidate edilir.
- `PriceMatrixSection` her satırda, satır DB'de varsa ve oda tipi aktifse `Sil` butonu
  gösterir. Buton `data-testid="admin-pricing-price-delete"` taşır; satırın
  `roomTypeId`/`guests`/`id` bilgisini kullanır. Pasif oda tiplerinde buton görünmez.
- Kullanıcı input'u boşaltıp kaydetmek **silme sayılmaz**; silme yalnız açık `Sil`
  aksiyonuyla yapılır. Böylece yanlışlıkla veri kaybı önlenir.
- Silme başarısızsa `run` üzerinden kullanıcı dostu mesaj gösterilir; `404` için
  “Fiyat satırı bulunamadı; liste yenilendi.” benzeri mesaj + refetch önerilir.
- `error-messages.ts` içine yeni iki error code'un Türkçe mesajları eklenir.
- Silme sonrası matris refetch edilir; `missing` listesi güncellenir. Varsayılan plan için
  eksik kalan kombinasyon varsa `admin-pricing-action-error` veya matris uyarısı görünür.

### 3.4 Testler

- `AdminPricingControllerTest`:
  - `SUPER_ADMIN`/`MANAGER` delete `204`.
  - `FRONT_DESK` delete `403 security.forbidden`.
  - Anonim delete `401 security.unauthorized`.
  - Bilinmeyen satır `404 pricing.rate_plan_price_not_found` /
    `pricing.season_price_not_found`.
  - Yanlış plan/sezon altındaki satır `404`.
- `PricingIT`:
  - Baz fiyat satırı silinir; satır DB'de yoktur; aynı oda/misafir için quote `409`,
    diğer misafir sayıları `200`.
  - Sezon fiyatı silinir; quote plan baz fiyatına düşer; baz fiyat yoksa `409`.
  - Silmeden önce alınan `PriceSnapshot` değişmez; silme sonrası yeni snapshot farklıdır.
  - Varsayılan plan silme sonrası varsayılan kalır; admin matrisinde `missing` görünür.
- `pricing-manager.test.tsx`:
  - `Sil` butonu doğru `DELETE` path'ine gider; başarıda liste refetch edilir.
  - `404`/`403` hata mesajı gösterilir; ekran çökmez.
  - Pasif oda tipinde `Sil` butonu yoktur.
- `@pricing` E2E:
  - Sezon fiyatı UI'dan silinir; public quote baz fiyatına düşer.
  - Baz fiyat silinirse eksik uyarısı görünür; ilgili kombinasyon quote'u `409` döner.
  - `FRONT_DESK` delete `403` negatifi korunur.

### 3.5 Kabul kriterleri

- [ ] İki delete endpoint'i yalnız `SUPER_ADMIN`/`MANAGER` tarafından çağrılabilir.
- [ ] Başarılı silme `204`; yok/yanlış üst kayıt `404`; yetkisiz `403`.
- [ ] Satır, path'teki plan/sezon dışında bir kayda aitse silinmez.
- [ ] UI'da satır bazlı `Sil` aksiyonu vardır; input temizleme otomatik silme yapmaz.
- [ ] Silme snapshot'ları etkilemez; eksik fiyat davranışı spec ile uyumludur.
- [ ] Testler ve `@pricing` E2E yeşildir.

## 4. CR-14 — Maksimum 30 gece

### 4.1 Spec ve doküman deltası

`specs/0006-pricing.md` rev.1 içine:

- **REQ-4 doğrulama:** “Konaklama en fazla **30 gece** olabilir; `checkOut - checkIn > 30`
  ise `400 pricing.invalid_period` döner. 30 gece dahil kabul edilir.”
- **AC-4:** “30 gece kabul, 31 gece `400`; sınır enjekte edilen `Clock` ile test edilir.”
- **Varsayımlar:** “Maksimum konaklama 30 gecedir; `hotel.pricing.max-stay-nights` ile
  yapılandırılabilir, varsayılan 30'dur.”
- **API notları:** Public quote `400` hata kodları listesine bu kuralın
  `pricing.invalid_period` ile döndüğü eklenir.

`docs/domain.md` iş kurallarına kısa bir madde eklenir: “Fiyat teklifi en fazla 30 gece;
aşım reddedilir.” `docs/architecture.md` D15 değişmez; bu bir teklif doğrulama sınırıdır.

### 4.2 Config

- `.env.example`:
  - `HOTEL_PRICING_MAX_STAY_NIGHTS=30`
  - Açıklama: “Local/CI varsayılanı; 30 gece üst sınırı.”
- `backend/hotel-api/src/main/resources/application.yml`:
  - `hotel.pricing.max-stay-nights: ${HOTEL_PRICING_MAX_STAY_NIGHTS:30}`
- `PricingProperties`:
  - `private int maxStayNights = 30;` + getter/setter.
  - Değer pozitif olmalı; `1` altındaki config startup'ta fail-fast veya ilk quote'ta
    açık hata ile reddedilmeli. Öneri: `PricingProperties` doğrulaması/`@PostConstruct`
    ile `maxStayNights >= 1` kontrolü.

### 4.3 Doğrulama

`QuoteService.quote` içinde, tarih doğrulamasından sonra ve DB/oda/plan sorgularından önce:

```java
long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
if (nights > properties.getMaxStayNights()) {
    throw new DomainError(
        PricingErrorCodes.INVALID_PERIOD,
        "Stay must be at most " + properties.getMaxStayNights() + " nights.",
        DomainErrorCategory.VALIDATION);
}
```

- Aynı `nights` değeri daha sonra yeniden hesaplanmamalı; tek yerde hesaplanıp kullanılmalı.
- `checkIn == checkOut` ve geçmiş tarih kontrolleri mevcut sırayla korunur.
- 30 gece: `checkOut = checkIn.plusDays(30)` kabul; 31 gece: `plusDays(31)` red.
- Hata kodu mevcut `pricing.invalid_period`; yeni kod açılmaz.

### 4.4 Testler

- `PricingIT`:
  - 30 gece quote `200` ve 30 gece dökümü.
  - 31 gece quote `400 pricing.invalid_period`; gövde RFC 7807.
  - `Clock` sabitlenerek sınır testi; `LocalDate.now(...)` doğrudan kullanılmaz.
- `pricing.spec.ts`:
  - 31 gece public quote `400`; 30 gece `200`.
- Config testi:
  - Varsayılan 30; `HOTEL_PRICING_MAX_STAY_NIGHTS` override edildiğinde yeni değer kullanılır
    (en azından property binding testi).
- Mevcut testler 1–3 gece kullandığı için kırılmamalı.

### 4.5 Kabul kriterleri

- [ ] Varsayılan maksimum 30 gece; config ile değiştirilebilir.
- [ ] 30 gece kabul, 31 gece `400 pricing.invalid_period`.
- [ ] Doğrulama DB/oda/plan sorgularından önce çalışır.
- [ ] Spec, `docs/domain.md`, `.env.example`, `application.yml` güncellendi.
- [ ] Sınır testleri `Clock` ile deterministik.

## 5. CR-03 — Application → web bağımlılığının kaldırılması

### 5.1 Hedef mimari

- `internal.application` yalnız domain, application view record'ları ve persistence
  port/repository'lerini bilir; `internal.web` sınıflarını import etmez.
- `internal.web` controller'ları application servislerini çağırır ve `PricingWebMapper` ile
  DTO'ya çevirir. `PricingWebMapper` `internal.web` içinde kalır.
- API yanıt sözleşmesi değişmez; yalnız iç katman bağımlılığı düzelir.

Önerilen application view record'ları (isimler geliştirici tarafından gerekçeli
uyarlanabilir):

- `RatePlanAdminView` + `SeasonAdminView`: plan ve sezonları birlikte taşır.
- `ManualPriceAdminView`: `ManualPrice` + `RoomTypeSnapshot` (pasif oda tipi adları dahil).
- `FxRateHistoryView`: `latest`, `items`, `page`, `size`, `total`, `stale`,
  `staleCurrencies`.
- `PriceMatrixService.MatrixView` mevcut application record'u olarak kalır.
- `PricingSettings`, `FxRate`, `ActiveRoomType` gibi domain/catalog public tipleri
  doğrudan dönebilir; web mapper bunları DTO'ya çevirir.

### 5.2 Uygulama adımları

1. `PricingAdminService` ve `PricingAdminQueryService` içindeki `PricingWebMapper` ve
   `internal.web.dto.*` import'larını kaldır.
2. Bu servislerin dönüş tiplerini application view record'larına çevir; iş kuralları
   değişmesin.
3. `AdminRatePlanController`, `AdminManualOverrideController`,
   `AdminPricingSettingsController` içine `PricingWebMapper` enjekte et; application
   view'ları DTO'ya controller'da çevir.
4. `PricingWebMapper`'a application view → DTO metotları ekle; mevcut domain → DTO
   metotlarını koru.
5. `docs/conventions.md` §2.1'e katman yönü kuralını ekle:
   “`internal.application` web DTO/mapper'ına bağımlı olamaz; dönüşüm `internal.web`'de
   yapılır.”
6. `HotelApiModularityTest`'e ArchUnit kuralı ekle:

```java
noClasses()
    .that().resideInAPackage("com.hotel..internal.application..")
    .should().dependOnClassesThat().resideInAPackage("com.hotel..internal.web..")
    .allowEmptyShould(true)
    .check(productionClasses);
```

### 5.3 Testler

- Mevcut `AdminPricingControllerTest` JSON sözleşmesi değişmeden yeşil kalmalı.
- `HotelApiModularityTest` yeni katman kuralı yeşil; mevcut modül/şema testleri korunur.
- `PricingAdminService`/`PricingAdminQueryService` için varsa unit testler view dönüşlerine
  göre güncellenir; yoksa controller slice testleri yeterli.
- `mvnw verify` ve admin-web `npm test` yeşil.

### 5.4 Kabul kriterleri

- [ ] `internal.application` içinde `internal.web` import'u yok.
- [ ] Controller'lar DTO dönüşünü web katmanında yapıyor.
- [ ] API yanıtları alan/format olarak değişmedi.
- [ ] ArchUnit katman kuralı eklendi ve yeşil.
- [ ] `docs/conventions.md` güncellendi.
- [ ] Mevcut testler ve `@pricing` E2E yeşil.

## 6. Uygulama sırası ve commit önerisi

1. **Spec/doküman deltası:** `specs/0006-pricing.md` rev.1 (CR-01 API/error code + CR-14
   30 gece), `docs/domain.md`, `docs/conventions.md`, `.env.example` notları.
2. **CR-14:** `PricingProperties`, `QuoteService`, `application.yml`, `.env.example`,
   testler.
3. **CR-03:** application view record'ları, mapper/controller refactor, ArchUnit kuralı,
   testler.
4. **CR-01 backend:** repository sorguları, `PriceMatrixService` delete metotları,
   `PricingAdminService`, controller endpoint'leri, error code'lar, testler.
5. **CR-01 admin-web:** mutation'lar, `PriceMatrixSection` silme aksiyonu, error mesajları,
   Vitest/RTL/MSW testleri.
6. **E2E/kanıt/SCORECARD:** `@pricing` silme + 31 gece negatifi, evidence ve kapanış
   raporu güncellemesi.

Önerilen commit mesajları (plan numaraları geliştirici tarafından netleştirilebilir):

- `docs(pricing): record 0006 code review decisions [plan 0006/13]`
- `feat(pricing): cap quote stays at 30 nights [plan 0006/14]`
- `refactor(pricing): remove application dependency on web layer [plan 0006/15]`
- `feat(pricing): add base and season price delete endpoints [plan 0006/16]`
- `feat(admin-web): add pricing price delete actions [plan 0006/16]`
- `test(pricing): cover review fixes and refresh evidence [plan 0006/17]`

## 7. Doküman, CI ve kanıt etkisi

- `specs/0006-pricing.md`: rev.1; CR-01 API/error code, CR-14 30 gece, SCORECARD
  “Spec revizyon sayısı: 1”, “Düzeltme turu sayısı: 2”.
- `docs/conventions.md`: application → web yasağı.
- `docs/domain.md`: maksimum 30 gece kuralı.
- `.env.example` / `application.yml`: `HOTEL_PRICING_MAX_STAY_NIGHTS`.
- `admin-web/src/lib/error-messages.ts`: yeni iki error code.
- `docs/frontend.md`: `admin-pricing-price-delete` testid'i ve silme davranışı notu.
- `specs/done/evidence/0006/`:
  - `AC-1-test-and-ci-summary.md` güncellenir.
  - `AC-2-admin-pricing-delete.png` (opsiyonel görsel) veya delete test özeti.
  - `AC-4-quote-response.md` 30/31 gece sonucu ile güncellenir.
  - `closing-report.md` CR-01/CR-03/CR-14 kapanışı ile güncellenir.
- `e2e/README.md`: `@pricing` silme ve 31 gece negatifi notu.
- `.github/workflows/ci.yml`: yeni aşama gerekmez; mevcut `@pricing` aşaması yeterli.
- 0001–0005 arşivleri değişmez; `git diff` + untracked kontrolü korunur.

## 8. Yapılmayacaklar

- `PUT /prices` için replace semantiği uygulamak (CR-01 kararı delete endpointleri).
- Input boşaltıldığında otomatik silme yapmak.
- Varsayılan plandan fiyat silmeyi tamamen engellemek (mevcut spec böyle bir kural
  içermiyor; eksik fiyat davranışı korunur).
- Public fiyat UI'si, oda kartı fiyatı veya oda detay sayfası eklemek (0007).
- Booking snapshot kalıcılığını değiştirmek.
- Rate limiting eklemek; bu brief yalnız 30 gece doğrulamasıdır.
- `internal.application` içine DTO/mapper taşımak veya web bağımlılığını farklı adla
  korumak.
- CR-01/CR-03/CR-14 dışındaki CR bulgularını bu turda kapatmaya çalışmak.
- Yeni spec numarası açmak; bu, 0006'nın revizyonudur.
- 0001–0005 evidence arşivlerini değiştirmek.

## 9. Definition of Done

- [ ] Spec rev.1 Takım Yöneticisi tarafından onaylandı.
- [ ] CR-01 delete endpointleri + UI aksiyonları + testler tamam.
- [ ] CR-14 30 gece sınırı + config + testler tamam.
- [ ] CR-03 application → web bağımlılığı kaldırıldı; ArchUnit kuralı yeşil.
- [ ] `mvnw verify`, admin-web `npm test`, `@pricing` E2E yeşil.
- [ ] Evidence ve SCORECARD güncellendi; 0001–0005 arşivleri değişmedi.
- [ ] Code Reviewer doğrulama turu tamamlandı; Takım Yöneticisi kapanış onayı verdi.

## 10. Riskler ve açık noktalar

- **CR-01:** Varsayılan plandan baz fiyat silinirse plan varsayılan kalır ve ilgili
  kombinasyon `409` döner. Bu, spec'in “eksik fiyat” davranışıyla uyumludur; admin
  ekranındaki `missing` uyarısı görünür olmalıdır.
- **CR-01:** Silme endpoint'lerinin path/error code isimleri bu brief'te önerilmiştir;
  Takım Yöneticisi onayıyla kesinleşir.
- **CR-14:** Mevcut 30 geceden uzun teklifler (varsa) artık `400` alacaktır; public UI
  0007'de bu sınırı kullanıcıya göstermelidir. Bu dilimde public UI yoktur.
- **CR-03:** Refactor davranış değiştirmemeli; controller JSON testleri ve E2E korunur.
  Application view record isimleri geliştirici tarafından gerekçeli uyarlanabilir.
- **CR-03:** `PricingWebMapper` web katmanında kalır; application'a taşınırsa brief'in
  amacı ihlal edilir.
