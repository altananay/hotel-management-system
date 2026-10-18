# Mimari (v1.1)

> Bu dosya mimarinin **tek gerçek kaynağıdır**. Kurallar buraya yazılır; diğer dosyalar buraya işaret eder.
> Son güncelleme: 2026-10-18 — Karar kaydı (D1–D19) §10'dadır; toolchain §10'da, local portlar ve dev bağlantı düzeni §11'dedir.

## 1. Sistem Topolojisi

```
Misafir ──► web (Next.js, public) ──────────► hotel-api (Spring Boot, modüler monolit)
                                               │   İş verisinin ve iş kurallarının TEK sahibi
Personel ─► admin-web (Next.js, shadcn)        │
              │  aynı origin (rewrites / reverse proxy)
              ▼                                │
         admin-api (Spring Boot) ──────────────┘   Bearer admin JWT (aud=hotel-admin-api)
              │
              └── hotel_admin DB (admin platform verisi)
```

- **4 deploy edilebilir uygulama:** `hotel-api`, `admin-api`, `web`, `admin-web`.
- **Altyapı (local, docker):** PostgreSQL (`hotel_core` + `hotel_admin`), MinIO (medya), Mailpit (e-posta). Aşama 2'de: Redpanda + kafbat-ui.
- **Dış servisler:** ödeme sağlayıcı (V1: fake; sonra iyzico sandbox), TCMB kur servisi, e-posta sağlayıcı (local: Mailpit).

## 2. Uygulamalar ve Sorumlulukları

| Uygulama | Tek sorumluluk | Sahip olduğu veri | Kritik kural |
|---|---|---|---|
| **hotel-api** | İşin kendisi: oda, fiyat, rezervasyon, ödeme, medya | `hotel_core` (modül başına şema) | İş verisinin ve iş kurallarının tek sahibi |
| **admin-api** | Admin platformu: kimlik, RBAC, oturum, audit, tercihler + ince geçit | `hotel_admin` | İş kuralı içermez; business çağrıları pass-through |
| **web** | Public misafir deneyimi ve rezervasyon akışı | (veri sahibi değil) | Yalnız hotel-api public API |
| **admin-web** | Admin operatör arayüzü | (veri sahibi değil) | DB yok, sır yok; yalnız admin-api |

## 3. hotel-api Modülleri

| Modül | Tek sorumluluk | Kavramsal veri | Yayınladığı olaylar | Tükettiği olaylar |
|---|---|---|---|---|
| **sharedkernel** | Ortak dil: `Money` (BigDecimal + Currency), `StayPeriod`, `HotelDate`, `TypedId`, `DomainError`. İş kuralı içermez. | — | — | — |
| **catalog** | "Hangi odalar var, operasyonel durumları ne?" | Oda tipleri, odalar, olanaklar, oda operasyonel durumu (READY/DIRTY/OUT_OF_ORDER; V1 manuel) | `RoomOutOfOrder`, `RoomReleased`, `RoomTypeUpdated` | — |
| **pricing** | "Bir konaklama kaça ve hangi indirimle satılır?" | Tarife planları, sezonlar, promosyon kodları + kullanımları, kur tablosu + geçmişi, buffer/yuvarlama ayarları | `RatePlanChanged`, `PromoCodeRedeemed` | — |
| **media** | "Oda medyası ve 360 tur içeriği nedir, yayında mı?" | Medya varlıkları (foto/panorama), işleme durumu, tur sahneleri + hotspot'lar, oda–medya ilişkisi, yayın durumu | `MediaReady`, `TourPublished` | — |
| **booking** | "Kim, hangi odayı, hangi gecelerde tutuyor?" — **müsaitlik otoritesi** | Rezervasyonlar, oda-gece atamaları, rezervasyon kapsamı misafir bilgisi, fiyat anlık görüntüsü, statüler, hold süresi, iptal/iade kararı | `ReservationCreated`, `ReservationConfirmed`, `ReservationCancelled`, `ReservationExpired`, `CheckedIn`, `CheckedOut`, `NoShow` | `PaymentSucceeded/Failed/Expired/RefundCompleted`, (ops.) `RoomOutOfOrder` |
| **payment** | Para hareketi ve sağlayıcı durumu | Ödeme niyetleri, sağlayıcı oturum referansları, webhook kayıtları, iadeler, mutabakat durumu, idempotency anahtarları. **Kart verisi asla.** | `PaymentSucceeded`, `PaymentFailed`, `PaymentExpired`, `RefundCompleted` | — |
| **notification** | Sistem olaylarını insana ulaştırmak | Gönderim kayıtları, şablon eşlemesi (V1: e-posta) | — | identity (V2), booking, payment olayları |
| **reporting** | Admin için okunabilir iş metrikleri | V1: sahibi değil (diğer modüllerin read API'leri). V2: olayla beslenen projeksiyonlar | — | (V2) iş olayları |
| **identity** | *(V2 — V1'de inşa edilmez)* Müşteri kimliği | Müşteri hesapları | `CustomerRegistered` vb. | — |

> **Admin bir modül değildir**, bir roldür. Her iş modülü kendi admin REST uçlarını kendi içinde barındırır (aynı uygulama servisi, farklı yetki + ayrı DTO yüzeyi).

## 4. admin-api Modülleri

| Modül | Sorumluluk | Veri (hotel_admin) |
|---|---|---|
| **adminidentity** | Admin hesapları, roller, oturumlar, JWT üretimi, JWKS yayını | Admin kullanıcıları, roller, oturumlar |
| **adminaudit** | "Kim ne zaman ne yaptı" kaydı (platform olayları + iş işlemlerinin aktörü) | Audit kayıtları |
| **preferences** | Kayıtlı filtreler, kullanıcı tercihleri | Tercihler |
| **gateway** | hotel-api admin API'sine pass-through: kimlik iliştirme, zaman aşımı, hata eşleme | Veri sahibi değil |

Kurallar: gateway yalnızca **taşır**; dönüştürme dışında mantık eklemek yasaktır (§8.11). Admin-web'e özel bir birleştirme (aggregation) ihtiyacı doğarsa önce bu doküman güncellenir.

Ayrıca: admin-api'nin kendi REST yüzeyi de `/admin-api/**` ön ekini kullanır; admin-web yalnızca bu ön eki bilir. Gateway, gelen `/admin-api/**` isteğini hotel-api'nin aynı adlı yüzeyine iletir.

## 5. İletişim Kuralları

### R1 — Senkron (doğrudan) çağrı, yalnızca şu üç durumda:
1. Cevabı beklemek zorundasın: sorgu, müsaitlik kontrolü, fiyat teklifi.
2. Hata şimdi bilinmeli ve akış durmalı (ör. ödeme oturumu açılamadıysa rezervasyon `PENDING_PAYMENT`'a alınmaz).
3. Davranışın tek sahibi o modüldür: fiyat daima `pricing`'den sorulur; "ben hesaplarım" denmez.
Şekil: hedef modülün `api` paketindeki arayüz + DTO'lar. Read-only çağrı serbest; yazan çağrılar yalnız hedefin komut API'siyle.

### R2 — Domain event, şu durumlarda:
1. Tetikleyen işlemin sonucunu beklemeyen yan etkiler (bildirim, projeksiyon).
2. Fan-out: bir değişime birden çok modül tepki verecekse.
3. Döngü kırmak: ödeme sonucu `booking`'i senkron çağıramaz → event.
4. Zaman/tekrar ayrıştırması; ileride broker'a taşınabilirlik.

### R3 — Bağımlılık grafiği DAG olmalıdır
Etkin bağımlılık = senkron çağrı **+** event aboneliği (abone, yayıncıya bağımlıdır). İzinli kenarlar:

| Kaynak → Hedef | Tür | Örnek |
|---|---|---|
| booking → catalog | senkron (read) | "bu tarihlerde kaç oda müsait, hangileri bloklu" |
| booking → pricing | senkron | fiyat teklifi al, promosyon kullan/geri al |
| booking → payment | senkron (komut) | ödeme oturumu aç, iade başlat |
| booking ← payment | event | `PaymentSucceeded` → rezervasyon CONFIRMED |
| booking ← catalog | event (ops.) | `RoomOutOfOrder` → çakışan rezervasyon bayrağı |
| media → catalog | senkron (read) | oda var mı doğrulaması |
| notification ← booking/payment | event | voucher, makbuz |
| reporting → booking/catalog/payment/pricing | senkron (read) | doluluk, ciro |

Yaprak modüller: `catalog`, `pricing`, `payment`. `catalog` asla `booking`'e bağımlı olamaz. İzin listesi **kapalıdır**: listede olmayan her çapraz bağımlılık ihlaldir; tüm modüller `sharedkernel`'e bağımlı olabilir, `sharedkernel` hiçbir modüle bağımlı olamaz. Zorlama: **Spring Modulith doğrulaması + ArchUnit testleri CI'da zorunlu** — ihlal, kırmızı build'dir.

### R4 — Transaction sınırı
**1 transaction = 1 modül.** Cross-module tutarlılık senkron çağrı + kompanzasyonla sağlanır (hold süresi dolunca `ReservationExpired`; ödeme alınamazsa hold düşer). "Booking + payment tek transaction" yasaktır.

### R5 — Event sözleşmesi
- Payload: `eventId`, `occurredAt`, aggregate id ve **değişen gerçekler** (minimal fact). Entity/PII/token yok.
- Türler yayıncının `api.event` paketinde public; adlar versiyonlanabilir (`reservation.confirmed.v1`); alan ekleme serbest, kırma yasak.
- **At-least-once** varsayımı: tüm handler'lar idempotent (eventId dedupe veya doğal idempotency).
- **Sıra garantisi yok** kabul edilir.
- Teslim: `AFTER_COMMIT` + Modulith event publication registry (outbox etkisi). İşlenemeyen olay kayıt altında kalır, retry edilir, alarm üretir.

### R6 — REST yüzeyleri ve frontend kuralları
- İki API yüzeyi: **public API** (`/api/**`) ve **admin API** (`/admin-api/**`); ayrı Spring Security zinciri, ayrı kimlik profili.
- **Sağlık ucu:** her iki uygulamada Actuator `/actuator/health` (detay kapalı, kimliksiz; yalnız bu uç açık, diğer Actuator uçları kapalı).
- admin-web → yalnız admin-api; web → yalnız hotel-api public API. İki frontend arasında kod paylaşımı yok.
- Oda panosu gibi birleşik okuma ekranları tek read API olarak sunulur (V1: `reporting.RoomBoard(date)`); frontend iş mantığı birleştirmez.

### R7 — Akış örnekleri
1. **Rezervasyon + ödeme:** web → `booking.CreateReservation` → booking: catalog (uygun odalar) + pricing (teklif) senkron → oda-gece ataması + hold (15 dk) → `payment.createIntent` → `ReservationCreated` → notification. Sağlayıcı webhook → payment → `PaymentSucceeded` → booking CONFIRMED → `ReservationConfirmed` → notification (voucher).
2. **İptal/iade:** `booking.Cancel` → kural → `payment.refund` → `RefundCompleted` → booking finalize → `ReservationCancelled` → notification.
3. **Check-in/out:** admin → `booking.CheckIn/CheckOut` → event → reporting; oda durumu V1'de elle işaretlenir.
4. **360 tur:** admin → media'ya upload (presigned) → işleme → `MediaReady` → hotspot düzenleme → `TourPublished` → public görünür.

## 6. Güvenlik

- **Admin JWT:** admin-api üretir (RS256, kısa ömürlü); `aud=hotel-admin-api`; `roles` claim. hotel-api, admin-api'nin JWKS public anahtarıyla doğrular. Anahtar yalnız admin-api'de yaşar.
- **admin-web ↔ admin-api:** opak oturum çerezi (HttpOnly, Secure, SameSite=Lax) + CSRF double-submit + Origin kontrolü. Tarayıcıda token tutulmaz; adres aynı origin'dir (dev: Next rewrites; prod: reverse proxy).
- **Müşteri JWT'si V1'de yok** (müşteri hesabı yok — D4). Public akış anonimdir.
- **Webhook'lar:** JWT'siz; sağlayıcı imzası + idempotency + replay koruması zorunlu.
- **PCI:** kart verisi alınmaz/saklanmaz; yalnız hosted checkout.
- **DB kullanıcıları:** `migrator` (DDL) ve `app` (yalnız DML) ayrıdır; uygulama migration yetkisiyle çalışmaz.
- **Prod'da:** admin API uçları dış ağa açılmaz (yalnız admin-api iç ağdan erişir); local'de her şey localhost.

## 7. Veri ve Altyapı

- **Tek PostgreSQL sunucusu, iki veritabanı:** `hotel_core`, `hotel_admin`. `hotel_core` içinde şema/modül. Cross-schema erişim yasaktır.
- **Flyway:** modül başına migration klasörü (`db/migration/<modül>/`); bir modülün migration'ı başka şemaya dokunamaz. admin-api, `hotel_admin`'i kendi Flyway'ıyla yönetir.
- **Flyway çalıştırma modeli:** migration'lar ayrı adımda `migrator` kimliğiyle koşar (`./mvnw flyway:migrate`); uygulama süreçleri `app` kimliğiyle çalışır ve `spring.flyway.enabled=false`'tur. Local quickstart ve CI bu adımı içerir; IT'lerde migration test profilinde migrator kimliğiyle koşar.
- **ID:** UUIDv7. **Zaman:** DB'de UTC (`timestamptz`); iş günü Europe/Istanbul (`HotelDate`); konaklama tarihleri `LocalDate`.
- **Para:** her zaman `BigDecimal` + para birimi (`Money` value object). Ayrıntı: `docs/conventions.md`.
- **Event altyapısı aşamaları:**
  - *Aşama 1 (V1):* in-process event'ler (Spring Modulith + publication registry).
  - *Aşama 2:* Redpanda (Kafka API uyumlu) + kafbat-ui; `spring-modulith-events-kafka` ile externalization. Topic: `hotel.<modül>.events`; envelope: `{eventId, type, version, occurredAt, aggregateId, payload}`; tüketicide inbox/dedupe; hata → DLT. In-process tüketiciler çalışmaya devam eder.
  - *Aşama 3 (opsiyonel):* modüller arası tüketim tamamen broker üzerinden.
- **Medya:** S3 uyumlu depo (local: MinIO). İşleme hattı: normalize → EXIF temizle → önizleme/tile üretimi. 360 tur altyapısı hazırdır; gerçek panorama içeriği geldiğinde yayına alınır; geliştirmede CC0 örnek panoramalar seed'lenir.
- **Ödeme:** `PaymentProvider` portu. `FakePaymentProvider` (local varsayılan: mock checkout sayfası + imzalı webhook simülasyonu; başarılı/başarısız/3DS/zaman aşımı senaryoları). `IyzicoPaymentProvider` (sandbox) sonraki dilimde ikinci implementasyon olarak eklenir. Statüler: `INITIATED → PENDING → SUCCEEDED | FAILED | EXPIRED`; iade; günlük mutabakat işi.
- **Fiyatlandırma (D15):** Baz fiyat TRY (admin girer). TCMB `today.xml` günlük çekilir (iş günleri ~15:30; hafta sonu/tatil → son yayımlanan kur; kur geçmişi saklanır). EUR/USD fiyat = `TRY × kur(satış) × (1 + buffer)` → yuvarlama. Varsayılanlar: buffer %2, en yakın 5'e yuvarlama (ikisi de admin tarafından yapılandırılır). Öncelik: **elle girilen fiyat > elle kur > TCMB**. Onaylanan fiyat, kur/buffer/yuvarlama bilgisiyle **snapshot** olarak saklanır. Tahsilat para birimi = gösterim para birimi; ödeme anında çevrim yok; iade aynı para biriminde.

## 8. Yasaklar (bağlayıcı)

1. Başka modülün tablosuna/şemasına doğrudan SQL/JPA erişimi; cross-module JOIN; cross-module foreign key.
2. Başka modülün `internal` paketini import etmek; o modülün repository'sini enjekte etmek. Erişim **yalnızca** `<modül>.api` üzerinden.
3. Entity'nin modül/proje sınırını geçmesi (controller'a dahi sızmaz; yalnız DTO ve event tipleri).
4. `sharedkernel`'e iş kuralı koymak (yalnızca ilkel value object).
5. Tek transaction'da birden çok modülün yazması (1 tx = 1 modül).
6. Senkron çağrı döngüsü; event abonelikleri dahil bağımlılık grafiğinde döngü (DAG zorunlu).
7. Event payload'ında entity/PII/şifre/token taşımak; event sırasına veya exactly-once'a güvenmek; idempotent olmayan handler yazmak.
8. Bir iş kuralını sahibi olmayan modülde/uygulamada yeniden yazmak: fiyat, müsaitlik, iptal/iade kuralı, kimlik doğrulama kararı yalnız sahibinde hesaplanır.
9. Public ve admin yüzeylerinin aynı JWT anahtarını/audience'ını paylaşması; public frontend'in admin API çağırması.
10. `admin-web`'de DB erişimi, imza anahtarı veya sır bulunması; admin-web'in hotel-api'ye doğrudan istek atması (yalnız admin-api üzerinden).
11. `admin-api`'de iş kuralı bulunması; hotel_core'a erişmesi; gateway'de dönüştürme dışında mantık.
12. Admin DB'sine iş verisi yazmak/aynalamak; public'in göreceği veriyi (medya, tur vb.) admin DB'sinde tutmak.
13. Kart verisi almak/saklamak; hosted checkout dışında kart formu işletmek; imza doğrulaması yapmadan webhook sonucu kabul etmek.
14. Bir modülün Flyway migration'ının başka modülün şemasına dokunması.
15. Para/yüzde için `double`/`float` kullanmak; yuvarlamayı gizli varsayılanla uygulamak.
16. Frontend'ler arasında kaynak kod paylaşımı (tipli API client üretimi hariç).
17. Spec'siz branch/commit/PR; korumalı branch'e doğrudan commit; force push; geçmiş silme (ayrıntı: `docs/git.md`).

## 9. V1 Kapsam Dışı (bilinçli kararlar)

**Ürün/kapsam:** çoklu tesis/tenant; OTA/channel manager; dinamik fiyatlandırma/yield; sadakat, hediye kartı, kurumsal hesaplar; grup/çok-odalı rezervasyon (1 rezervasyon = 1 oda); ekstra hizmet satışı (transfer, spa); yorum/puanlama; housekeeping otomasyonu (oda durumu manuel); pazarlama içeriği CMS'i (içerik kodda/statik); SMS/WhatsApp bildirimi; e-Fatura/e-Arşiv; PDF voucher/fatura belgesi (V1: e-posta özeti); müşteri hesabı (guest checkout).

**Ödeme:** deposit/kısmi ödeme/pre-authorization; taksit; çok adımlı ödeme; rezervasyon değişikliği (V1: iptal + yeniden rezervasyon); politika motoru (V1: tek basit iade kuralı — girişten X gün öncesine kadar tam iade; X varsayılan 7; admin yapılandırır).

**Teknik:** mikroservisler; modül başına ayrı DB; read replica; CQRS/Event Sourcing; WebSocket/gerçek zamanlı özellikler; mobil uygulama/PWA; gelişmiş BI; ince taneli izin matrisi (V1: 3 kaba rol); Kafka Aşama 1'de yok (Aşama 2'de Redpanda); 360'ta VR/kat planı/canlı rehber (altyapı hazır, içerik sonra); normal fotoğraflardan 3D üretimi (V2 tartışması).

## 10. Karar Kaydı (D1–D19)

| No | Karar | Seçim |
|---|---|---|
| D1 | Admin DB'nin kapsamı | Admin DB **platform verisi** (kimlik, RBAC, oturum, audit, tercih); iş verisi yalnız hotel-api'de |
| D2 | Admin kimlik sağlayıcı | **admin-api** (Spring Boot) IdP; kısa ömürlü JWT + JWKS; admin-web saf istemci *(v1.1 revizyonu)* |
| D3 | Public site tasarım sistemi | Tailwind CSS v4 + shadcn/ui + Radix; animasyon: Motion + GSAP ScrollTrigger + Lenis; **MUI kullanılmaz** |
| D4 | Müşteri hesabı | V1'de yok; guest checkout + rezervasyon kodu + e-posta ile sorgulama |
| D5 | Ödeme sağlayıcı | `PaymentProvider` portu + **FakePaymentProvider** (varsayılan); iyzico sandbox sonraki dilim |
| D6 | Ödeme hold süresi | 15 dakika (konfigüre edilebilir) |
| D7 | Tesis sayısı | Tek tesis (property kavramı kodda yok) |
| D8 | Dil/para | TR/EN (public), TR (admin); TRY baz + EUR/USD ödeme |
| D9 | Pazarlama içeriği | V1'de statik (kod/dosya); panelden düzenleme V2 |
| D10 | 360 deneyim | V1: premium foto galeri + tur altyapısı; içerik/panorama çekimi ayrı iş; V2: fotoğraflardan 3D tartışması |
| D11 | Hero/animasyon | Hero video döngüsü + scroll hikâyesi (pinli bölümler + parallax) + mikro etkileşimler |
| D12 | Housekeeping | V1 manuel oda durumu; otomasyon V2 |
| D13 | e-Fatura | V1 kapsam dışı |
| D14 | Repo yapısı | Monorepo (backend/, web/, admin-web/, contracts/, docs/, specs/, infra/, e2e/) |
| D15 | Fiyat/kur modeli | TRY baz + TCMB kuru + buffer/yuvarlama + manuel override (öncelik: elle fiyat > elle kur > TCMB) |
| D16 | Event altyapısı | Aşama 1: in-process; Aşama 2: Kafka uyumlu broker; Aşama 3 opsiyonel broker tüketimi |
| D17 | Broker + arayüz | **Redpanda** (KRaft Kafka yerine; Java ekip öğrenme tercihi) + **kafbat-ui** |
| D18 | Hero video | Pexels 30653318 (Pexels License; kaynak + lisans `CREDITS.md`'ye işlenir) |
| D19 | Admin DB erişimi | **Spring Data JPA + Flyway** (admin-api içinde); Drizzle/Prisma kullanılmaz |
| — | Para kuralı | Para/yüzde alanları **her zaman BigDecimal**; `double`/`float` yasak (kural) |
| — | Toolchain | Backend **Maven** (`pom.xml` + `mvnw`), frontend **npm** (`package-lock.json`); Java 25 + Spring Boot 4.x + uyumlu Spring Modulith; Gradle/pnpm kullanılmaz (karar, 2026-10-18) |

## 11. Ortam ve Portlar (local)

| Bileşen | Port | Not |
|---|---|---|
| web | 3000 | Next.js public |
| admin-web | 3001 | Next.js admin (rewrites → 8082) |
| hotel-api | 8081 | Spring Boot |
| admin-api | 8082 | Spring Boot |
| postgres | 5432 | `hotel_core` + `hotel_admin` |
| minio | 9000 / 9001 | S3 API / konsol |
| mailpit | 1025 / 8025 | SMTP / web arayüz |
| redpanda | 9092 | Aşama 2 |
| kafbat-ui | 8090 | Aşama 2 |
| webhook (fake sağlayıcı) | hotel-api içinde | `/api/webhooks/payments/fake` |

**Dev bağlantı düzeni:**
- web → hotel-api: doğrudan (`NEXT_PUBLIC_API_BASE_URL=http://localhost:8081`); hotel-api CORS allowlist yalnız `http://localhost:3000` (credential yok).
- admin-web → admin-api: rewrites (`/admin-api/:path*` → `http://localhost:8082`); aynı origin (çerez/CSRF).
- admin-api → hotel-api: iç çağrı `http://localhost:8081/admin-api/**`.
- Not (2026-10-18): Local'de 8080 portu dolu olduğundan hotel-api `8081`'e, admin-api `8082`'ye alındı. Prod topolojisi (ters proxy/alan adları) ayrıca ele alınır.

**Repo düzeni:**

```
backend/hotel-api/    backend/admin-api/    web/    admin-web/
contracts/            docs/                 specs/  infra/  e2e/
```

**Çalıştırma (V1):** altyapı `infra/docker-compose.yml` ile ayağa kalkar; uygulamalar geliştirme sırasında IDE / `npm run dev` ile çalıştırılır. E2E smoke, kök `e2e/` Playwright projesiyle koşar (CI: compose → migrate + jar → frontend build + start → test).
