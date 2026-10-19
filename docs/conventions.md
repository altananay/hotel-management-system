# Konvansiyonlar

> Kural kopyalanmaz; bu dosya ilgili kuralların **tek kaynağıdır**.

## 1. Genel İlkeler

- Kod ve teknik isimler **İngilizce**; doküman, spec ve iş iletişimi **Türkçe**.
- **Dosya, klasör, spec ve branch isimleri İngilizce + kebab-case'dir; içerikleri Türkçe'dir.** İsimlerde Türkçe karakter kullanılmaz. Örnek: `specs/0001-project-architecture.md` (içerik Türkçe), branch `feature/0001-project-architecture`.
- Her değişikliğin bir spec'i vardır (`specs/TEMPLATE.md`). Spec kapsamı dışına taşan iş → yeni spec.
- Belirsizlikte varsayım yapma: **soru + öneri + gerekçe** ile Takım Yöneticisi'ne gel (Altın Kural 4).

## 2. Backend (Java)

### 2.1 Paket ve modül düzeni

- Kök paket: `com.hotel.<modül>` (ör. `com.hotel.booking`); `admin-api` modülleri de `com.hotel` kökündedir (ör. `com.hotel.adminidentity`; ayrı artifact olduğu için çakışma yoktur).
- Modül düzeni: `<modül>/api` **public** (arayüzler, DTO'lar, event tipleri) — import edilebilen tek alan; `<modül>/internal` **private**, alt paketleri sabittir: `internal.domain`, `internal.application`, `internal.web`, `internal.persistence`. Başka modül `internal`'ı import edemez (Spring Modulith + ArchUnit ile zorlanır).
- JPA `@Entity` sınıfları `..api..` ve `..internal.web..` paketlerinden referans alamaz (entity sızması yasağı; ArchUnit).
- **Cross-cutting sınıflar** (problem advice, request-id filtresi, security/CORS config, geçici scaffold controller'lar) uygulama kökünde ayrı paketlerde yaşar (`com.hotel.web`, `com.hotel.config`, `com.hotel.scaffold`); ayrı ortak modül açılmaz; `sharedkernel`'e web kodu girmez; iş modülleri bu paketlere bağımlı olamaz (bağımlılık tek yönlüdür).
- Çapraz modül erişimi yalnızca `docs/architecture.md` §5 R3 izin listesindedir (kapalı liste; listede olmayan çapraz import ihlaldir). Tüm modüller `sharedkernel`'e bağımlı olabilir; `sharedkernel` hiçbir modüle bağımlı olamaz.

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
- Doğrulama sınırda yapılır (Jakarta Bean Validation); domain'de iş kuralı doğrulaması ayrıdır.
- **Hata gövdesi RFC 7807** (`application/problem+json`). Zorunlu alanlar: `type` (bu dilimde `about:blank`), `title` (EN), `status`, `detail` (EN; stack trace/iç sınıf adı içermez), `instance` (istek yolu), `code` (stabil makine kodu), `requestId`; kullanıcıya teknik metin/stack trace dönmez. Spring'in varsayılan hata gövdesi kullanılmaz.
- **Kod uzayı:** `security.*`, `validation.*`, `system.*` rezerve öneklerdir (`security.unauthorized`, `security.forbidden`, `system.not_found`, `system.method_not_allowed`, `system.not_acceptable`, `system.unsupported_media_type`, `system.conflict`, `system.internal_error`); iş kodları `<modül>.<snake_case>` (ör. `booking.room_not_available`).
- **Domain hata kategorileri:** `DomainError` semantik kategori taşır (`DomainErrorCategory`: `validation`, `not_found`, `conflict`, `forbidden`, `internal`); HTTP statü eşlemesi **yalnız web katmanında** yapılır — `sharedkernel`'e HTTP/Spring web bilgisi girmez.
- **Durum matrisi:** `400 → validation.*` ve kategori `validation` olan `<modül>.*` · `401 → security.unauthorized` · `403 → security.forbidden` ve kategori `forbidden` olan `<modül>.*` · `404 → system.not_found` ve kategori `not_found` olan `<modül>.*` · `405 → system.method_not_allowed` · `406 → system.not_acceptable` · `409 → system.conflict` ve kategori `conflict` olan `<modül>.*` (ör. `booking.room_not_available`) · `415 → system.unsupported_media_type` · `500 → system.internal_error` ve kategori `internal` olan `<modül>.*` (genel mesaj; en az bir otomatik testle kanıtlanır). Matriste bulunmayan durumlar `system.http_<durum>` kodunu taşır (ör. `system.http_422`). Allowlist dışındaki yollar routing'e ulaşmadan güvenlik katmanında reddedilir ve varlığından bağımsız olarak `401 security.unauthorized` döner (bilinçli default-deny; `404` yalnız allowlist içindeki API yüzeyinde değerlendirilir).
- **CORS:** yalnızca `docs/architecture.md` §11'deki origin'ler; izin verilmeyen origin'e `Access-Control-Allow-Origin` dönmez (negatif test edilir).

### 2.4 Zaman, kimlik, log, config

- DB'de `timestamptz` + **UTC**; iş günü Europe/Istanbul (`HotelDate`); konaklama tarihleri `LocalDate`.
- Public ID'ler **UUIDv7** (üretim daima v7, pinlenmiş kütüphane ile); gelen kimliklerde yalnızca biçim doğrulanır (geçersiz → `sharedkernel.invalid_id`); DB sequence'i dışarı sızdırılmaz.
- Log: yapılı (structured); `X-Request-Id` → MDC; gereksiz PII loglanmaz. **Request-id kabulü:** istemci değeri yalnız `^[A-Za-z0-9._-]{8,64}$` ile eşleşirse kabul edilir, aksi halde UUIDv7 üretilir; değer her yanıta eklenir. JSON log encoder'ı ilk ops dilimine ertelenmiştir.
- Config: environment değişkenleri; repoda **gerçek sır yok** (`.env.example` tutulur). Yerel geliştirme için açıkça işaretlenmiş sabit varsayılanlar sır sayılmaz; prod değerleri asla repoya girmez.

## 3. Para ve Yüzde

- **Her zaman `BigDecimal`.** `double`/`float` hiçbir katmanda (entity, DTO, TS istemci) kullanılamaz.
- `Money` value object: `amount: BigDecimal` + `currency: Currency`. Tutar, para birimini **her zaman** yanında taşır.
- DB: `NUMERIC(p,2)`. Ölçek ve yuvarlama yalnız **sınırda** ve açık modla uygulanır (varsayılan `HALF_UP`); yuvarlanmış ara sonuçla yeniden hesap yapılmaz.
- Yüzdeler de `BigDecimal` (ör. %2 → `0.02`); kural parametreleri (buffer, yuvarlama, iade kuralı) DB'de tutulur, kodda gömülü değildir.
- Karşılaştırma `compareTo` ile; `Money.equals` ölçekten bağımsız normalize eder (ör. `10` = `10.00`).
- Frontend tarafında para **string ondalıklı** taşınır ve `Intl.NumberFormat` ile gösterilir; JS `number` ile hesaplanmaz.

## 4. Veritabanı ve Flyway

- `hotel_core` içinde modül başına şema: `catalog`, `pricing`, `booking`, `payment`, `media`, `notification` (6 şema). `sharedkernel` ve V1'de veri sahibi olmayan `reporting` şemasızdır. `hotel_admin` ayrı veritabanıdır (admin-api yönetir).
- Migration yolu: `db/migration/<modül>/V<n>__<aciklama>.sql` (ör. `V1__init_catalog_schema.sql`). Numara tahsisi: `hotel_core` tek Flyway history kullandığından `V<n>` **global benzersizdir**; yeni numara tüm modül klasörlerindeki en büyük numaranın +1'idir (çakışma CI'da kırmızı build'dir). **Bir modülün migration'ı başka şemaya dokunamaz.**
- İsimlendirme: tablolar çoğul `snake_case`; kolonlar `snake_case`; index `ix_<tablo>_<kolon>`; FK yalnızca modül içinde mümkündür (`fk_<tablo>_<hedef>`).
- DB kullanıcıları: `migrator` (DDL) ve `app` (yalnız DML) ayrıdır.
- `public` şeması uygulama akışının dışındadır: yalnız `migrator` (`USAGE`+`CREATE`) kullanır; `app` rolüne `public` üzerinde hiçbir yetki verilmez (`public.flyway_schema_history` dahil). DML yetkileri yalnız modül şemalarında (`IN SCHEMA <modül>` default privilege'ları) tanımlanır.
- Entity'ler şema adını açıkça taşır (`@Table(schema = "<modül>")`); varsayılan `public` şemasına dayanılmaz.

## 5. Frontend

- Ayrıntı: `docs/frontend.md` (API çağrıları hook'larla, zorunlu yükleme/boş/hata durumları, teknik hata sızdırmama).

## 6. Test ve Git

- Test stratejisi: `docs/testing.md`. Branch/commit/PR: `docs/git.md`.

## 7. Toolchain ve Sürümler

- **Backend build:** **Maven** (`pom.xml` + `mvnw`); **Gradle kullanılmaz.** Her uygulama (`hotel-api`, `admin-api`) kendi Maven projesidir; Maven Wrapper commit edilir, CI `./mvnw` kullanır.
- **Java:** 25 (LTS); `maven.compiler.release=25`.
- **Spring:** Spring Boot 4.x + uyumlu Spring Modulith; sürümler BOM üzerinden yönetilir; tam sürüm scaffold'ta pinlenir.
- **Frontend paket yöneticisi:** **npm**; **pnpm/yarn kullanılmaz.** `package-lock.json` commit edilir; CI `npm ci` kullanır.
- **Node:** güncel LTS; `package.json` → `engines` ile pinlenir.
- Sürümler tek kaynaktan yönetilir; majör yükseltmeler ayrı spec ister.
