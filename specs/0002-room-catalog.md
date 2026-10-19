# Oda Kataloğu ve Vitrin

> Doldurmadan önce: `AGENTS.md` (altın kurallar) ve `docs/architecture.md` okunur.
> Numara: `specs/` altındaki en büyük numaranın + 1'i. Dosya: `specs/<####>-<english-name>.md` (İngilizce kebab-case; içerik Türkçe). Branch: `feature/<####>-<english-name>` (yalnızca `releasable`'dan). Örnek: `specs/0001-project-architecture.md`.
> Spec onaylanmadan kod yazılmaz (Altın Kural 1). Onayı Takım Yöneticisi verir.

---

## 0. Künye

- **Spec No:** `0002`
- **Başlık:** Oda Kataloğu ve Vitrin (`room-catalog`)
- **Durum:** Taslak *(CLARIFY-1A..5A onaylandı; son spec onayı bekleniyor)*
- **Branch:** `feature/0002-room-catalog`
- **İlgili doküman/kararlar:** `docs/architecture.md` (§3 `catalog`, §5 R1/R3/R6, §7, §8, §10 D3/D7/D8, §11), `docs/domain.md` (§1 `RoomType`, `Amenity`), `docs/conventions.md` (§1–§7), `docs/frontend.md`, `docs/testing.md`, `docs/git.md`, `specs/done/0001-project-architecture.md` (`REQ-11`/`AC-11` scaffold kaldırma sözü); Onaylı kararlar: `CLARIFY-1A`, `CLARIFY-2A`, `CLARIFY-3A`, `CLARIFY-4A`, `CLARIFY-5A`
- **Tarih:** 2026-10-19
- **Sahip:** Analist (`analyst`)
- **Onaylayan (Takım Yöneticisi):** *(Onay bekleniyor — `CLARIFY-1A..5A` onaylandı)*

## 1. Intent (Neden?)

Potansiyel otel misafirleri ve otel yönetimi, Nevşehir/Kapadokya otelinin sunduğu satılabilir oda kategorilerini (`RoomType`) ve oda olanaklarını (`Amenity`) Türkçe ve İngilizce olarak inceleyebilmek; geliştirme ekibi ise ilk gerçek iş modülünü (`catalog`) uçtan uca hayata geçirerek misafir tarafındaki geçici kurulum iskeletini gerçek ürün deneyimine dönüştürmek istiyor. Başarı; otelin oda tiplerinin iki dilde (TR/EN) ad, açıklama, maksimum misafir kapasitesi ve olanak bilgileriyle katalogda tanımlı olması (`CLARIFY-1A`), misafirin halka açık web vitrininde yalnızca yayında/aktif olan oda tiplerini seçtiği dilde **yükleme**, **boş**, **hata** ve **dolu** durumlarıyla sorunsuz görüntüleyebilmesi (`CLARIFY-2A`, `CLARIFY-3A`, `CLARIFY-4A`, `CLARIFY-5A`) ve ilk kurulumdan kalan geçici misafir scaffold yapısının sistemden temizlenmesidir. Bu dilimde fiziksel odalar ve operasyonel durumları (`Room` / `RoomStatus`), fiyatlandırma ve kur hesabı, tarih bazlı müsaitlik/rezervasyon, fotoğraf/360 tur medyası, ayrı oda tipi detay sayfası (`media` ve `pricing` hazır olduğunda geliştirilmek üzere planlanmıştır — `CLARIFY-2A`), yönetim paneli (admin) üzerinden katalog düzenleme işlemleri ile nihai görsel tasarım sistemi/animasyonlar bilerek kapsam dışı bırakılacaktır.

## 2. Requirements

- **REQ-1 (Oda Kataloğu Çekirdeği — `RoomType` ve `Amenity` — `CLARIFY-1A`, `CLARIFY-3A`, `CLARIFY-4A`):** Sistemde satılabilir oda kategorileri (`RoomType`) ve oda olanakları (`Amenity`) `catalog` modülünün sorumluluğunda tanımlanmalıdır. Her oda tipi; benzersiz kimlik, Türkçe ve İngilizce zorunlu ad ve açıklama metinleri (`CLARIFY-1A`), pozitif maksimum misafir kapasitesi, aktif/yayında durumu (`CLARIFY-3A`), deterministik görüntüleme sırası (`CLARIFY-4A`) ve sahip olduğu olanaklar kümesi (her olanak için benzersiz kod/kimlik ile Türkçe ve İngilizce zorunlu ad) ile temsil edilmeli; iş kuralları geçersiz kapasiteye veya eksik TR/EN metinlerine izin vermemelidir.
- **REQ-2 (Halka Açık Oda Tipleri Okuma Yüzeyi — `CLARIFY-1A`, `CLARIFY-3A`, `CLARIFY-4A`):** Misafirlerin oda tiplerini inceleyebilmesi için `hotel-api` public yüzeyinde anonim erişime açık, sayfalanmış oda tipleri listeleme hizmeti (`GET /api/room-types`) sunulmalıdır. Bu yüzey yalnızca aktif/yayında olan oda tiplerini (`CLARIFY-3A`), TR ve EN metinleri ve olanak bilgileriyle birlikte deterministik sırada ve standart sayfalama zarfıyla (`{ items, page, size, total }` — `docs/conventions.md` §2.3) döndürmeli; pasif oda tipleri misafir listesinde yer almamalı ve `/admin-api/**` yüzeyine bu dilimde dokunulmamalıdır.
- **REQ-3 (Web Oda Vitrini ve Ana Sayfa Gezinme Kurgusu — `CLARIFY-1A`, `CLARIFY-2A`, `CLARIFY-4A`):** Misafir arayüzünde (`web`), seçili dile (TR/EN) duyarlı oda vitrini sayfası (`/[locale]/rooms`) sunulmalı ve ana sayfa (`/[locale]`) misafiri oda vitrinine yönlendiren sade bir karşılama ve geçiş alanı içermelidir (`CLARIFY-2A`). Vitrin sayfasında her oda tipi kartı; seçili dildeki ad, açıklama, maksimum misafir kapasitesi, olanak etiketleri ve sade bir görsel yer tutucu (placeholder) alanı göstermeli; kartlarda kesinlikle sabit veya uydurma fiyat yer almamalıdır (`CLARIFY-4A`). Vitrin ekranı **yükleme**, **boş**, **hata** (kullanıcı dostu mesaj + tekrar dene) ve **dolu** durumlarının tamamını ayrı sorgu hook'u ve şema doğrulamasıyla desteklemelidir (`docs/frontend.md`).
- **REQ-4 (Misafir Tarafı Geçici Scaffold Yapısının Kaldırılması — `0001` Sözü):** İlk mimari kurulumda (`0001`) yalnızca üç durum doğrulaması için eklenen `hotel-api` üzerindeki geçici `GET /api/system/sample-items` ucu ve `web` uygulamasındaki geçici `system-status` özelliği, ilgili çeviri anahtarları ve testleriyle birlikte kaldırılmalıdır. `admin-api` ve `admin-web` üzerindeki geçici scaffold yapısına bu dilimde dokunulmamalıdır ("Admin Kimlik ve Oturum" spec'inde kaldırılacaktır).
- **REQ-5 (Geliştirme/CI Örnek Verisi ve Boş/Dolu Doğrulama Akışı — `CLARIFY-3A`, `CLARIFY-5A`):** Standart veritabanı şema geçişleri yalnızca boş katalog yapısını oluşturmalı ve canlı (prod) ortama örnek veri sızdırmamalıdır. Yerel geliştirme ve CI ortamları için tekrar çalıştırılabilir (idempotent), hem aktif oda tiplerini hem de görünürlük kuralını sınamak üzere en az bir pasif oda tipini ve olanakları içeren ayrı bir örnek veri (seed) mekanizması bulunmalıdır (`CLARIFY-5A`). Doğrulama akışı; örnek veri yüklenmeden önce **gerçek uçtan boş durumu**, örnek veri yüklendikten sonra ise **dolu vitrini** kanıtlamalıdır.
- **REQ-6 (Uçtan Uca Smoke Testleri ve Görsel Kanıtlar — `CLARIFY-1A`, `CLARIFY-5A`):** Kök `e2e/` Playwright smoke testleri; misafir arayüzünde TR/EN dil geçişini ve oda vitrininin **yükleme**, **boş** (seed öncesi gerçek uçtan), **hata** ve **dolu** (seed sonrası TR ve EN) durumlarını doğrulayacak şekilde güncellenmeli, mevcut `admin-web` smoke testleri aynen korunmalı ve görsel kanıtlar `specs/done/evidence/0002/` altına üretilmelidir (`docs/testing.md` §3).

## 3. Constraints (Kapsam Dışı Dahil)

- **Teknik/İş kısıtları:**
  - **Modül ve Veritabanı Sınırları (`docs/architecture.md` §3, §5 R3, §7, §8; `docs/conventions.md` §2, §4):** `catalog` yaprak modüldür; hiçbir iş modülüne (`booking`, `pricing`, `media` vb.) bağımlı olamaz (yalnız `sharedkernel` serbesttir). Tüm katalog tabloları `hotel_core` veritabanındaki `catalog` şemasında yaşar ve JPA varlıkları `@Table(schema = "catalog")` taşır. `catalog` şeması ve `app` kullanıcısı DML varsayılan izinleri `V1__init_catalog_schema.sql` ile kurulu olduğundan, yeni şema geçişi `db/migration/catalog/V7__*.sql` (mevcut en büyük global numara `V6` sonrası) olarak yalnızca `catalog` şemasına tablo/indeks/kısıt ekler ve başka şemaya dokunamaz.
  - **Veri ve Tip Kuralları (`docs/conventions.md` §2.4, §3):** Public kimlikler `UUIDv7` (`TypedId`), zaman alanları UTC `timestamptz`'dir. Hiçbir katmanda `double`/`float` kullanılamaz.
  - **REST ve Hata Zarfı (`docs/conventions.md` §2.3):** Liste yanıtı `{ items, page, size, total }` zarfını kullanır; hatalar RFC 7807 (`application/problem+json`) formatındadır.
  - **Arayüz ve Test Kuralları (`docs/frontend.md`, `docs/testing.md`):** API çağrıları yalnızca ayrı TanStack Query hook'ları (`useXxxQuery`) ve Zod şema doğrulaması ile yapılır; bileşen içinde doğrudan `fetch`/`axios` yasaktır; E2E seçicileri `data-testid` kullanır; MUI kullanılmaz.
- **Kapsam dışı (bilinçli olarak yapılmayacaklar):**
  - **`CLARIFY-2A` (Oda Tipi Detay Sayfası — Zorunlu Yol Haritası Notu):** Ayrı oda tipi detay sayfası (`/[locale]/rooms/[id-veya-slug]`) bu dilimde **yapılmayacaktır**; ancak **ürün kapsamında kesin olarak yer alacaktır** ve **`media` (fotoğraf galerisi / 360 tur) ile `pricing` (fiyat teklifi) modülleri hazır olduğunda ilgili dilimde geliştirilmesi zorunludur** (bkz. §4).
  - **`CLARIFY-3A` (Fiziksel Odalar ve Operasyonel Durum):** Fiziksel oda birimleri (`Room`) ve operasyonel durumları (`RoomStatus`: `READY`/`DIRTY`/`OUT_OF_ORDER`) bu dilimde kapsam dışıdır; `booking` ve admin operasyon dilimlerine bırakılmıştır.
  - **`CLARIFY-4A` (Fiyat ve Medya):** Oda fiyatları, kur/buffer hesabı (`pricing`), oda fotoğrafları ve 360 tur sahneleri (`media`) kapsam dışıdır; vitrin kartlarında uydurma fiyat gösterilmez, görsel alanı sade yer tutucu (placeholder) olarak kalır.
  - **Admin Katalog Yönetimi (CRUD):** Oda tiplerinin ve olanakların yönetim panelinden eklenmesi/düzenlenmesi (`admin-api` / `admin-web` ve `/admin-api/**` uçları) "Admin Kimlik ve Oturum" spec'i sonrasına bırakılmıştır; `admin-api` ve `admin-web` içindeki geçici scaffold yapısına bu dilimde dokunulmaz.
  - **Görsel Tasarım Sistemi ve Animasyonlar:** Tasarım token'ları, hero video, GSAP/Lenis scroll hikâyesi ve `docs/frontend.md` genişletmesi (`CLARIFY-4A` / `0001`) ayrı arayüz spec'inin kapsamındadır; bu spec fonksiyonel vitrin iskeletidir.
- **Kabul edilen risk/ödünç:**
  - Admin katalog yönetim ekranları sonraki dilimde ekleneceğinden, bu dilimde oda tipleri ve aktiflik durumları veritabanı ve yerel/CI örnek veri (seed) mekanizması üzerinden beslenir.

## 4. Context

- **Mevcut durum:** `specs/done/0001-project-architecture.md` tamamlanmıştır. `backend/hotel-api` içinde `catalog` modülü boş paket iskeleti (`api` / `internal`) ve `V1__init_catalog_schema.sql` şema/izin tanımıyla hazırdır (mevcut en yüksek Flyway numarası `V6__init_notification_schema.sql`). `hotel-api` içinde geçici `SampleItemsController` (`GET /api/system/sample-items`) ve `web` içinde geçici `src/features/system-status` özelliği yer almaktadır (`TODO(scaffold-0001)`).
- **İlgili uygulamalar ve modüller:**
  - `backend/hotel-api` (`:8081`): `com.hotel.catalog` modülü (`V7` migration, domain/application/persistence/web katmanları, yerel/CI seed mekanizması) ve `com.hotel.scaffold` (`SampleItemsController`'ın kaldırılması).
  - `web` (`:3000`): `/[locale]/rooms` vitrin sayfası, `/[locale]` ana sayfa karşılama/vitrin yönlendirmesi, katalog feature modülü (`src/features/...`), `src/features/system-status` scaffold özelliğinin kaldırılması.
  - `e2e`: Web vitrin (boş/dolu/yükleme/hata + TR/EN) smoke testleri; `admin-web` smoke testleri korunur.
- **Sonraki Dilimler ve Oda Detay Sayfası Planlama Notu (`CLARIFY-2A`):**
  - Önerilen dilim sırası: `0002` Oda Kataloğu ve Vitrin → Arayüz/Tasarım + Ana Sayfa → Admin Kimlik ve Oturum → Medya → Fiyatlandırma → Ödeme → Rezervasyon → Bildirim.
  - **Planlama Kaydı (`CLARIFY-2A` — Unutulmaması Gereken Karar):** Misafirin bir oda tipinin tüm fotoğraflarını/360 turunu ve tarih bazlı fiyat/müsaitlik detayını inceleyebileceği **Oda Tipi Detay Sayfası** üründe **kesinlikle** bulunacaktır. Bu sayfa, `media` ve `pricing` modülleri hazır olduğunda ilgili spec kapsamında zorunlu olarak geliştirilecektir.
- **Bağımlı spec'ler:** `specs/done/0001-project-architecture.md`.

## 5. Acceptance Criteria

- **AC-1 (`catalog` Modülü Veri Modeli, İş Kuralları ve `V7` Şema Geçişi — `REQ-1`, `CLARIFY-1A`, `CLARIFY-3A`, `CLARIFY-4A`):**
  - `db/migration/catalog/V7__*.sql` şema geçişi yalnızca `catalog` şemasında `RoomType`, `Amenity` ve ilişki tablolarını oluşturur; başka hiçbir şemaya dokunmaz; JPA entity sınıfları `@Table(schema = "catalog")` taşır ve hiçbir katmanda `double`/`float` kullanılmaz (Spring Modulith ve ArchUnit sınır testleri yeşildir).
  - `RoomType` (UUIDv7 kimlik, zorunlu TR/EN ad ve açıklama, `maxGuests >= 1` kapasite, `active` bayrağı, `displayOrder` sıralama alanı ve olanak kümesi) ile `Amenity` (UUIDv7 kimlik, benzersiz teknik kod, zorunlu TR/EN ad) domain kuralları birim testleriyle (`*Test`) doğrulanır: boş/eksik TR veya EN metin ya da `maxGuests < 1` durumunda `catalog.*` (`DomainErrorCategory.VALIDATION`) hatası fırlatılır.
- **AC-2 (Halka Açık `/api/room-types` Okuma Yüzeyi, Aktiflik ve Sayfalama — `REQ-2`, `CLARIFY-1A`, `CLARIFY-3A`, `CLARIFY-4A`):**
  - `GET /api/room-types` ucu anonim isteklere `200 OK` ve `docs/conventions.md` §2.3 sayfalama zarfıyla (`{ items, page, size, total }`; varsayılan `page=0`, `size=20`) yanıt verir; öğeler `displayOrder` (ve eşitlikte kararlı ikincil anahtar) ile deterministik sırada döner.
  - Her oda tipi öğesi; kimlik (`id`), TR/EN ad, TR/EN açıklama, maksimum misafir kapasitesi (`maxGuests`) ve olanaklar listesini (`amenities`: kod + TR/EN ad) içerir.
  - Yalnızca `active = true` olan oda tipleri listelenir; veritabanında `active = false` olan oda tiplerinin yanıtta yer almadığı ve hiç aktif oda tipi yokken `200 {"items": [], "page": 0, "size": 20, "total": 0}` döndüğü Testcontainers entegrasyon testiyle (`*IT`) kanıtlanır.
  - Geçersiz sayfalama parametrelerinde (negatif `page`, `size < 1` veya izin verilen üst sınırın üzerindeki `size`) RFC 7807 (`application/problem+json`, `400`, `validation.*`) hata yanıtı döner; `/admin-api/**` yüzeyinin mevcut `401` davranışı korunur.
- **AC-3 (`web` Oda Vitrini (`/[locale]/rooms`), Ana Sayfa Geçişi ve Kart Kuralları — `REQ-3`, `CLARIFY-1A`, `CLARIFY-2A`, `CLARIFY-4A`):**
  - `web` uygulamasında `/tr/rooms` ve `/en/rooms` rotaları çalışır; `/tr` ve `/en` ana sayfalarında misafiri oda vitrinine yönlendiren görünür bir bağlantı (`data-testid` ile seçilebilir) yer alır ve dil geçişi (`/tr/rooms` ↔ `/en/rooms`) çalışır.
  - Oda vitrini veriyi yalnızca ayrı TanStack Query hook'u (`useXxxQuery`) ve Zod şema doğrulaması üzerinden çeker; her oda tipi kartında seçili dildeki ad, açıklama, maksimum misafir kapasitesi, olanak etiketleri ve sade görsel yer tutucu (placeholder) gösterilir; kartlarda hiçbir fiyat bilgisi bulunmaz.
  - Vitrin bileşeninin **yükleme**, **boş**, **hata** (kullanıcı dostu TR/EN mesaj + tekrar dene düğmesi; teknik hata kodu sızdırmadan) ve **dolu** (TR/EN içerik, kapasite ve olanak etiketleri) durumları Vitest + Testing Library + MSW birim testleriyle doğrulanır.
- **AC-4 (Misafir Tarafı Geçici Scaffold Yapısının Kaldırılması — `REQ-4`):**
  - `hotel-api` içinde geçici `GET /api/system/sample-items` ucu (`SampleItemsController` ve ilgili testleri) ile `web` içinde `src/features/system-status` dizini, bileşenleri, testleri ve `systemStatus` i18n anahtarları tamamen kaldırılmıştır (`GET /api/system/sample-items` artık `200` dönmez).
  - `admin-api` (`GET /admin-api/system/sample-items`) ve `admin-web` üzerindeki geçici scaffold yapısı ve testleri dokunulmadan yeşil kalır.
- **AC-5 (Geliştirme/CI Örnek Verisi (Seed) ve Canlı Ortam İzolasyonu — `REQ-5`, `CLARIFY-3A`, `CLARIFY-5A`):**
  - Standart Flyway şema geçişi (`./mvnw flyway:migrate`) çalıştığında `catalog` tabloları boş oluşur; standart migration dosyalarında örnek oda tipi verisi yer almaz.
  - Yerel geliştirme ve CI için sağlanan ayrı, idempotent örnek veri (seed) mekanizması çalıştırıldığında Kapadokya oteline uygun en az 3 aktif oda tipi (TR/EN ad, açıklama, kapasite ve olanaklarla) ve görünürlük kuralını sınamak üzere en az 1 pasif oda tipi yüklenir; aynı seed tekrar çalıştırıldığında hata veya mükerrer kayıt oluşmaz.
- **AC-6 (E2E Smoke Testleri, CI Akışı ve Görsel Kanıtlar — `REQ-5`, `REQ-6`, `CLARIFY-1A`, `CLARIFY-5A`):**
  - Kök `e2e/` Playwright testleri ve `.github/workflows/ci.yml` akışı; (1) seed öncesi `/tr/rooms` üzerinde **gerçek uçtan boş durumu**, (2) seed uygulandıktan sonra `/tr/rooms` ve `/en/rooms` üzerinde **dolu vitrin durumunu** ve dil geçişini, (3) Playwright ağ simülasyonuyla **yükleme** ve **hata** durumlarını ve (4) mevcut `admin-web` üç durum smoke testlerini başsız (headless) modda çalıştırır ve tümü yeşildir (`data-testid` seçicileriyle).
  - `specs/done/evidence/0002/` dizini altına aşağıdaki görsel kanıtlar üretilir:
    - `AC-6-web-rooms-empty.png` (seed öncesi gerçek uçtan boş durum)
    - `AC-6-web-rooms-loading.png` (yükleme durumu)
    - `AC-6-web-rooms-error.png` (hata durumu + tekrar dene)
    - `AC-6-web-rooms-tr.png` (seed sonrası Türkçe dolu vitrin)
    - `AC-6-web-rooms-en.png` (seed sonrası İngilizce dolu vitrin)

## 6. Definition of Done

- [ ] Tüm AC'ler doğrulandı (test ve/veya görsel kanıt)
- [ ] Pipeline yeşil (lint + build + test + kritik akış smoke)
- [ ] Kod incelemesi onaylandı (Takım Yöneticisi)
- [ ] Davranış değiştiyse dokümanlar güncellendi
- [ ] Spec `specs/done/` klasörüne taşındı, görsel kanıtlar eklendi
- [ ] SCORECARD dolduruldu

## 7. SCORECARD

- **Spec revizyon sayısı:** 0
- **Düzeltme turu sayısı:** 
- **Bulgu gerçek/gürültü oranı:** 
- **Regresyon sayısı:** 
- **Kaçan hata (escaped bug):** 
