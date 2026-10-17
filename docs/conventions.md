# Konvansiyonlar

> Kural kopyalanmaz; bu dosya ilgili kuralların **tek kaynağıdır**.

## 1. Genel İlkeler

- Kod ve teknik isimler **İngilizce**; doküman, spec ve iş iletişimi **Türkçe**.
- Her değişikliğin bir spec'i vardır (`specs/TEMPLATE.md`). Spec kapsamı dışına taşan iş → yeni spec.
- Belirsizlikte varsayım yapma: **soru + öneri + gerekçe** ile Takım Yöneticisi'ne gel (Altın Kural 4).

## 2. Backend (Java)

### 2.1 Paket ve modül düzeni

- Kök paket: `com.hotel.<modül>` (ör. `com.hotel.booking`).
- Modül düzeni: `<modül>/api` **public** (arayüzler, DTO'lar, event tipleri) — import edilebilen tek alan; `<modül>/internal` **private** (domain, application servisleri, web ve persistence adaptörleri). Başka modül `internal`'ı import edemez (Spring Modulith + ArchUnit ile zorlanır).

### 2.2 Sınıf adları ve sorumluluklar

| Öğe | Desen | Örnek |
|---|---|---|
| Controller | `XxxController` | `ReservationController` (modül içindeki web adaptörü) |
| Uygulama servisi | `XxxService` | `ReservationService` |
| Repository | `XxxRepository` | `ReservationRepository` |
| İstek/yanıt DTO | `XxxRequest` / `XxxResponse` | `CreateReservationRequest` |
| Domain olayı | Geçmiş zaman, `api.event` paketi | `ReservationConfirmed` |
| Hata | `XxxException` + modül önekli kod | `booking.room_not_available` |

### 2.3 REST

- Kaynaklar çoğul: `/room-types`, `/reservations`. Sayfalama: `?page=0&size=20&sort=createdAt,desc`; yanıt zarfı `{ items, page, size, total }`.
- Hata gövdesi **RFC 7807** (`application/problem+json`); stabil, makine-okunur kod (`booking.room_not_available`); kullanıcıya teknik metin/stack trace dönmez.
- Doğrulama sınırda yapılır (Jakarta Bean Validation); domain'de iş kuralı doğrulaması ayrıdır.

### 2.4 Zaman, kimlik, log, config

- DB'de `timestamptz` + **UTC**; iş günü Europe/Istanbul (`HotelDate`); konaklama tarihleri `LocalDate`.
- Public ID'ler **UUIDv7**; DB sequence'i dışarı sızdırılmaz.
- Log: yapılı (structured); `X-Request-Id` → MDC; gereksiz PII loglanmaz.
- Config: environment değişkenleri; repoda sır **yok** (`.env.example` tutulur).

## 3. Para ve Yüzde

- **Her zaman `BigDecimal`.** `double`/`float` hiçbir katmanda (entity, DTO, TS istemci) kullanılamaz.
- `Money` value object: `amount: BigDecimal` + `currency: Currency`. Tutar, para birimini **her zaman** yanında taşır.
- DB: `NUMERIC(p,2)`. Ölçek ve yuvarlama yalnız **sınırda** ve açık modla uygulanır (varsayılan `HALF_UP`); yuvarlanmış ara sonuçla yeniden hesap yapılmaz.
- Yüzdeler de `BigDecimal` (ör. %2 → `0.02`); kural parametreleri (buffer, yuvarlama, iade kuralı) DB'de tutulur, kodda gömülü değildir.
- Karşılaştırma `compareTo` ile; `Money.equals` ölçekten bağımsız normalize eder (ör. `10` = `10.00`).
- Frontend tarafında para **string ondalıklı** taşınır ve `Intl.NumberFormat` ile gösterilir; JS `number` ile hesaplanmaz.

## 4. Veritabanı ve Flyway

- `hotel_core` içinde modül başına şema: `catalog`, `pricing`, `booking`, `payment`, `media`, `notification`, `reporting`. `hotel_admin` ayrı veritabanıdır (admin-api yönetir).
- Migration yolu: `db/migration/<modül>/V<yyyyMMddHHmm>__<aciklama>.sql`. **Bir modülün migration'ı başka şemaya dokunamaz.**
- İsimlendirme: tablolar çoğul `snake_case`; kolonlar `snake_case`; index `ix_<tablo>_<kolon>`; FK yalnızca modül içinde mümkündür (`fk_<tablo>_<hedef>`).
- DB kullanıcıları: `migrator` (DDL) ve `app` (yalnız DML) ayrıdır.

## 5. Frontend

- Ayrıntı: `docs/frontend.md` (API çağrıları hook'larla, zorunlu yükleme/boş/hata durumları, teknik hata sızdırmama).

## 6. Test ve Git

- Test stratejisi: `docs/testing.md`. Branch/commit/PR: `docs/git.md`.
