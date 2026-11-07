# Medya ve Fotoğraf Yönetimi

> Doldurmadan önce: `AGENTS.md` (altın kurallar) ve `docs/architecture.md` okunur.
> Numara: `specs/` altındaki en büyük numaranın + 1'i. Dosya: `specs/<####>-<english-name>.md` (İngilizce kebab-case; içerik Türkçe). Branch: `feature/<####>-<english-name>` (yalnızca `releasable`'dan). Örnek: `specs/0005-media.md`.
> Spec onaylanmadan kod yazılmaz (Altın Kural 1). Onayı Takım Yöneticisi verir.

---

## 0. Künye

- **Spec No:** `0005`
- **Başlık:** Medya ve Fotoğraf Yönetimi (`media`)
- **Durum:** Uygulandı — bağımsız review düzeltmeleri `fix/0005-code-review` PR'ında; merge için Takım Yöneticisi onayı bekleniyor *(rev.2 - Q1-Q10 + D1-D16)*
- **Branch:** `feature/0005-media`
- **İlgili doküman/kararlar:** `docs/architecture.md` (§3 `media`, §5 R3, §7 Medya, §8, §10 D10, §11), `docs/domain.md` (§1 `MediaAsset`/`Scene`/`Hotspot`/`Tour`, §3.12, §3 rol tablosu), `docs/conventions.md` (§1, §2.1, §2.3, §2.4, §3, §4), `docs/frontend.md`, `docs/testing.md`, `docs/git.md`, `specs/done/0002-room-catalog.md` (oda tipi/detay notu), `specs/done/0004-admin-identity.md` (auth, gateway, roller, CSRF); Onaylı CLARIFY kararları: Q1–Q10 A seçenekleri (2026-11-03)
- **Tarih:** 2026-11-03
- **Sahip:** Analist (`analyst`)
- **Onaylayan (Takım Yöneticisi):** Takım Yöneticisi (Onaylandı - 2026-11-03)

## 1. Intent (Neden?)

Otel yönetimi, oda tiplerinin fotoğraflarını admin panelinden yetkili biçimde yükleyip düzenleyebilmek; hangi fotoğrafın hangi oda tipinde, hangi sırada ve kapak olarak görüneceğine karar verebilmek istiyor. Potansiyel misafirler ise oda vitrininde gerçek, hızlı yüklenen ve erişilebilir fotoğraflarla odaları tanıyabilmeli; yayında olmayan veya işlenmemiş görseller hiçbir koşulda görünmemeli. Başarı; yetkili personelin fotoğrafı yükleyip oda tipine atayabildiği, işleme tamamlandığında yayınlayabildiği ve misafirin vitrinde yalnız yayınlanmış fotoğrafları gördüğü uçtan uca medya hattının çalışmasıdır. Bu dilimde 360 tur deneyimi, oda detay sayfası, fiyat/rezervasyon/ödeme, video yükleme, CDN ve AI görsel üretimi bilinçli olarak yapılmayacaktır.

## 2. Requirements

- **REQ-1 (Media Domain Modeli ve Şema — Q1, Q4, Q7, Q8):** Medya varlıkları `media` şemasında; UUIDv7 kimlik, tür (`PHOTO`; `PANORAMA` ileride), işleme durumu (`UPLOADED`, `PROCESSING`, `READY`, `FAILED`), orijinal depo anahtarı, MIME/boyut/genişlik/yükseklik, oda tipi ilişkisi, yayın/arşiv durumu, sıralama/kapak ve opsiyonel TR/EN alt metinle tanımlanmalıdır. Oda tipi doğrulaması yalnız `catalog` modülünün public read portu üzerinden yapılmalı; `media`, `catalog` internal'ına veya tablosuna erişmemelidir. Tüm medya tabloları `media` şemasında yaşamalı; başka şemaya tablo, indeks veya kısıt eklenmemelidir; `double`/`float` kullanılmamalıdır.
- **REQ-2 (Yükleme ve Yönetim — Q1, Q2, Q7):** `SUPER_ADMIN` ve `MANAGER`, admin-web'deki sade TR medya ekranından fotoğraf yükleyebilmeli; yüklenen medyayı listeleyebilmeli, aktif bir oda tipine atayabilmeli, sıralayabilmeli, kapak seçebilmeli, yayınlayabilmeli/yayından kaldırabilmeli, arşivleyebilmeli ve başarısız işlemeyi yeniden deneyebilmelidir. `FRONT_DESK` bu işlemlerin tamamında **403** almalıdır; arayüzde gizleme güvenlik sayılmaz. Yükleme, tarayıcıdan doğrudan depoya presigned PUT ile yapılmalı; dosya admin-api/hotel-api gateway'inden geçmemelidir.
- **REQ-3 (İşleme Hattı — Q2, Q4):** Yükleme tamamlandığında medya asenkron işleme alınmalı; normalize, EXIF/GPS temizliği, yön düzeltmesi ve varyant üretimi yapılmalıdır. İşlem durumu `PROCESSING` → `READY`/`FAILED` olarak izlenebilmeli; başarısız işlem en fazla üç otomatik denemeyle yeniden denenmeli, manuel retry sunulmalı ve uygulama yeniden başladığında yarıda kalan işler toparlanmalıdır. Public tüketici yalnız `READY` medyayı görebilmelidir.
- **REQ-4 (Public Servis ve Web Entegrasyonu — Q3, Q5, Q7, Q8):** Public API yalnız `READY`, yayınlanmış, arşivlenmemiş ve aktif oda tipine bağlı medyayı sunmalıdır. Public yanıt oda tipi alanlarını kopyalamamalı; web, catalog'dan gelen oda tipleriyle `roomTypeId` üzerinden sunum eşlemesi yapmalıdır. Oda vitrini kartı gerçek kapak fotoğrafını, uygun alt metni ve yeni `room-card-image` testid'ini göstermeli; medya yoksa veya medya servisi hata verirse mevcut `room-card-placeholder` korunmalıdır. Oda detay sayfası bu dilimde yoktur.
- **REQ-5 (Güvenlik, Depolama ve Erişim — Q1, Q3, Q4, Q8, Q9):** Yükleme presigned ve kısa ömürlü olmalı; yalnız JPEG/PNG/WebP kabul edilmeli; SVG, GIF, video ve bozuk görseller reddedilmeli; boyut/piksel sınırları uygulanmalı; EXIF/GPS temizlenmeli; nesne anahtarları rastgele ve PII'siz olmalıdır. Orijinaller private, yalnız yayınlanmış varyantlar public-read olmalıdır; public write ve bucket listeleme kapalı olmalıdır. Local/CI MinIO policy ve CORS kurulumu tekrarlanabilir olmalı; tarayıcının kullanacağı presigned/public host erişilebilir olmalıdır. Depolama kimlik bilgileri yalnız hotel-api'de bulunmalı; admin-web ve admin-api sır görmemelidir.
- **REQ-6 (Testler, Kanıtlar, CI ve Doküman — Q6, Q9, Q10):** Birim testleri + Testcontainers IT (PostgreSQL ve en az bir gerçek MinIO IT'si), admin-web Vitest + RTL + MSW ve E2E `@media` kapsamı yazılmalıdır. `@media`, 0002/0003 canlı testlerinden ve `@admin` aşamasından sonra çalışmalı; standart seed'e medya eklenmemeli; 0002/0003 placeholder testleri değişmemelidir. Kanıtlar `specs/done/evidence/0005/` altına yazılmalı; `0001`–`0004` arşivleri SHA-256/git diff ile değişmemelidir. Doküman, `.env.example`, `infra` ve CI etkileri §3'te listelenmiştir.

### 2.1 Admin API Sözleşmesi

Tüm admin media uçları `admin-api` gateway'i üzerinden `hotel-api` media modülüne gider. Gateway yalnız taşır; iş kuralı içermez. State-changing isteklerde 0004'teki CSRF/Origin ve rol kuralları geçerlidir; `X-Request-Id` admin-api kenarında üretilir/doğrulanır ve aynen taşınır.

| Uç | Yetki | Davranış | Başarı | Hata |
|---|---|---|---|---|
| `POST /admin-api/media/uploads` | SUPER_ADMIN, MANAGER | Presigned yükleme başlatır; medya `UPLOADED` olur | `201 {mediaId, uploadUrl, expiresAt}` | `400`, `403`, `415` |
| `POST /admin-api/media/{id}/complete` | SUPER_ADMIN, MANAGER | Yüklenen nesneyi doğrular; işleme alır | `202 {status: PROCESSING}` | `404`, `409`, `415`, `422` |
| `GET /admin-api/media` | SUPER_ADMIN, MANAGER | Oda tipi/durum filtresiyle sayfalı listeler; arşiv varsayılan gizli | `200 {items, page, size, total}` | `400`, `403` |
| `PATCH /admin-api/media/{id}` | SUPER_ADMIN, MANAGER | Aktif oda tipi ataması ve TR/EN alt metin güncellemesi | `200` | `400`, `403`, `404`, `409` |
| `PUT /admin-api/media/order` | SUPER_ADMIN, MANAGER | Oda tipi için tam sıralı medya listesini atomik uygular | `200` | `400`, `403`, `404`, `409` |
| `PUT /admin-api/media/{id}/cover` | SUPER_ADMIN, MANAGER | Oda tipinin kapak fotoğrafını belirler; önceki kapağı temizler | `200` | `403`, `404`, `409` |
| `POST /admin-api/media/{id}/publish` | SUPER_ADMIN, MANAGER | `READY` + atanmış + arşivsiz medyayı yayınlar | `200` veya `204` | `403`, `404`, `409` |
| `POST /admin-api/media/{id}/unpublish` | SUPER_ADMIN, MANAGER | Yayından kaldırır; public görünürlüğü anında keser | `200` veya `204` | `403`, `404` |
| `POST /admin-api/media/{id}/retry` | SUPER_ADMIN, MANAGER | `FAILED` işlemeyi yeniden başlatır | `202` | `403`, `404`, `409`, `422` |
| `DELETE /admin-api/media/{id}` | SUPER_ADMIN, MANAGER | Medyayı arşivler; public kopyaları kaldırır | `204` | `403`, `404`, `409` |
| `GET /api/media?roomTypeIds=...` | Public | Yalnız yayınlanmış READY medyayı toplu döndürür | `200 {items}` | `400`, `503` |

- **Admin liste:** `roomTypeId`, `status` (`UPLOADED`/`PROCESSING`/`READY`/`FAILED`), `includeArchived` (varsayılan `false`) filtreleri; `page`/`size` varsayılan 0/20, maksimum 100.
- **Atama/alt metin:** `PATCH` gövdesi `{roomTypeId?, altTextTr?, altTextEn?}`; oda tipi yalnız aktifken atanır; yayındaki medyanın oda tipi değiştirilemez (`409`).
- **Sıralama:** `PUT /admin-api/media/order` gövdesi `{roomTypeId, mediaIds}`; liste oda tipinin arşivsiz tüm medyalarını eksiksiz içermeli; eksik/fazla/başka oda tipi `409`.
- **Public liste:** En fazla **50** `roomTypeId`; yalnız aktif oda tipleri; oda tipi başına yayınlanmış medya, `displayOrder` sırasıyla. Her oda tipi için tam bir `cover: true` (explicit kapak yoksa ilk sıradaki) döner. Public yanıt orijinal depo anahtarını, işleme hatasını veya yayınlanmamış kaydı içermez.
- **Public yanıt öğesi:** `id`, `roomTypeId`, `cover`, `displayOrder`, `altTextTr`, `altTextEn`, `variants {thumb, card, cover}`; her varyant `url`, `width`, `height`, `format` taşır.
- **Admin yanıt öğesi:** `id`, `type`, `status`, `roomTypeId`, `published`, `cover`, `displayOrder`, `altTextTr`, `altTextEn`, `original {mimeType, sizeBytes, width, height}`, `variants`, `failureCode`, `createdAt`, `updatedAt`.
- **Hata kodları:** `media.not_found`, `media.upload_expired`, `media.invalid_content_type`, `media.file_too_large`, `media.image_too_large`, `media.invalid_image`, `media.room_type_not_found`, `media.not_ready`, `media.not_published`, `media.cover_conflict`, `media.order_conflict`, `media.already_archived`, `media.processing_failed`; RFC 7807 zarfı ve 0004 hata önceliği geçerlidir.

### 2.2 Veri Modeli ve Şema (REQ-1)

- **Migration:** `db/migration/media/V8__create_media_assets.sql` (global sıradaki numara V8; V5 init korunur). Yalnız `media` şemasına tablo/indeks/kısıt ekler; başka şemaya dokunamaz. `HotelApiModularityTest` migration/schema ve modül sınırı kontrolleri yeşil kalmalıdır.
- **`media.media_assets`:** `id UUID PK` (UUIDv7), `type` (`PHOTO`/`PANORAMA`; 0005'te yalnız `PHOTO`), `status` (`UPLOADED`/`PROCESSING`/`READY`/`FAILED`), `storage_key` (orijinal, unique, rastgele), `mime_type`, `size_bytes BIGINT`, `width INT`, `height INT`, `room_type_id UUID NULL` (catalog'a FK değil), `published_at TIMESTAMPTZ NULL`, `archived_at TIMESTAMPTZ NULL`, `display_order INT`, `is_cover BOOLEAN`, `alt_text_tr VARCHAR(200) NULL`, `alt_text_en VARCHAR(200) NULL`, `failure_code VARCHAR(100) NULL`, `processing_attempts SMALLINT`, `processing_started_at TIMESTAMPTZ NULL`, `processing_lease_id UUID NULL`, `created_at`/`updated_at TIMESTAMPTZ`.
- **`media.media_variants`:** `media_asset_id` FK (media içi), `variant` (`THUMB`/`CARD`/`COVER`), `format` (`WEBP`/`JPEG`), `private_storage_key` (unique), `public_storage_key` (nullable, unique), `width`, `height`, `size_bytes`; aynı asset+variant+format için tek kayıt. Public kopya yalnız yayında oluşturulur; unpublish/archive public kopyayı kaldırır, private anahtar korunur.
- **Kısıtlar:** `type`, `status`, `variant` ve `format` check'leri; `display_order >= 0`; `size_bytes > 0`; `width/height > 0`; alt metinlerde HTML/kontrol karakteri yok. `room_type_id` için indeks; yayınlanmış medya için `(room_type_id, display_order)` kısmi indeksi; oda tipi başına en fazla bir yayınlanmış kapak için kısmi unique indeks; arşivsiz kayıtlar için sıra tekilliği. Cross-schema FK yoktur.
- **Yaşam döngüsü:** `UPLOADED` → `PROCESSING` → `READY`/`FAILED`. `published_at` yalnız `READY` + atanmış + arşivsizken set edilir. `unpublish` `published_at` ve `is_cover` değerlerini temizler; public kopyaları kaldırır. `archive` `archived_at` set eder, yayını kaldırır ve public kopyaları kaldırır; arşiv geri alınmaz, yeniden yükleme gerekir. `PANORAMA` türü rezerve edilir; `Scene`/`Hotspot`/`Tour` tabloları 0005'te oluşturulmaz.
- **Oda tipi doğrulaması:** `catalog.api` içine salt-okunur bir port eklenir; media yalnız bu portla aktif oda tipi varlığını doğrular. `catalog` internal'ına import, tablo erişimi veya ters bağımlılık yasaktır. Oda tipi pasif/silinmişse yeni atama reddedilir; mevcut medya public'te görünmez.

### 2.3 Yükleme (REQ-2, REQ-5)

- **Presigned akış:** Admin, dosya adı/içerik türü/boyutla yükleme başlatır; sunucu rastgele nesne anahtarı ve **10 dakika** geçerli, tek nesne için presigned PUT döner. Tarayıcı dosyayı doğrudan depoya yükler; dosya admin-api/hotel-api üzerinden geçmez.
- **Ön doğrulama:** İçerik türü JPEG/PNG/WebP; en fazla **15 MB**; SVG, GIF, video ve diğer türler `415`/`400` ile reddedilir. Dosya adı depo anahtarına yazılmaz; anahtar rastgele UUID'dir.
- **Tamamlama:** `complete` çağrısı nesneyi HEAD ile doğrular; gerçek byte'lar üzerinden MIME/magic-byte, boyut ve piksel sınırları kontrol edilir. Geçersiz nesne silinir ve medya `FAILED`/uygun hata koduyla işaretlenir; hiçbir geçersiz nesne public olmaz.
- **Sınırlar:** Maksimum **6000×6000 piksel** ve **36.000.000** toplam piksel; minimum **800×600**. Orijinalden büyütme yapılmaz; yön (orientation) uygulanır.
- **Yetki ve güvenlik:** Yükleme başlatma/tamamlama yalnız SUPER_ADMIN/MANAGER; FRONT_DESK `403`. Presigned URL loglanmaz; TTL ve tek nesne kapsamlıdır; complete sonrası aynı medya için yeniden yükleme kabul edilmez. Yarım kalan yüklemeler public değildir; temizlik operasyonel runbook'a bağlıdır.

### 2.4 İşleme ve Varyantlar (REQ-3)

- **Asenkron:** `complete` başarılıysa medya `PROCESSING` olur ve yanıt `202` döner. İşlem uygulama içi worker'da yapılır; V1'de broker yoktur. Admin ekranı durumu yoklar; public yalnız `READY` görür.
- **Dönüşüm:** Orijinal private kalır; orientation uygulanır; EXIF/GPS ve diğer metadata temizlenir; renkler web için normalize edilir. Varyantlar **WebP + JPEG** olarak üretilir: `thumb` **480×320**, `card` **1200×800**, `cover` **1920×1280**; hedef oran 3:2, merkezden kırpma, orijinalden büyütme yok (küçük geçerli görselde çıktı kaynak kırpım boyutuyla sınırlanır).
- **Durum ve hata:** Başarılı işlem `READY`; hata `FAILED` + stabil `failure_code`. En fazla **3 otomatik deneme**; `FAILED` için manuel retry. Retry orijinali yeniden işler; orijinal geçersizse `422` döner. Uygulama restart'ında belirli eşikten eski `PROCESSING` kayıtları yeniden kuyruğa alınır; işlem idempotenttir.
- **Public gizlilik:** `UPLOADED`, `PROCESSING` ve `FAILED` medya public API'de ve vitrinde görünmez. İşleme tamamlanana kadar oda kartı placeholder gösterir.
- **Depolama:** Orijinal ve private varyantlar public bucket önekine yazılmaz; yayınlanan varyantlar public-read önekine kopyalanır ve public anahtar üretilir; private anahtar korunur. Public anahtarlar immutable ve rastgele UUID tabanlıdır.

### 2.5 Admin Yönetimi (REQ-2, Q7)

- **Ekran:** Admin-web'de TR, sade `/media` ekranı; yükleme, liste, oda tipi atama, sıralama, kapak, yayınla/kaldır, arşivle ve retry. 0003 tasarım sistemi uygulanmaz; yeni UI/i18n kütüphanesi eklenmez.
- **Durumlar:** Yükleme/liste için yükleme, boş ve hata durumları zorunludur; hata kullanıcı dostu ve generic, retry sunar. İşleme durumu (`PROCESSING`) ve `FAILED` nedeni görünür; `FAILED` için retry, `READY` için yayınla aksiyonu.
- **Kurallar:** Her medya en fazla bir oda tipine atanır. Yayın için `READY`, aktif oda tipi ve arşivsiz olma şartı vardır. Yayındaki medyanın oda tipi değiştirilemez; önce yayından kaldırılır. Oda tipi başına yayında tek kapak; kapak seçilmemişse en düşük sıradaki yayınlanmış fotoğraf kapak sayılır. Sıra oda tipi içinde benzersizdir; reorder atomiktir, çakışmada `409` döner.
- **Alt metin:** TR/EN opsiyonel, en fazla 200 karakter, düz metin; yayında eksikse admin uyarısı gösterilir. Public'te eksik dil için oda tipinin o dildeki adı fallback olur.
- **Arşiv:** Silme = arşivleme; public görünürlük anında kesilir, kayıt/orijinal/private varyantlar V1'de tutulur; kalıcı silme yoktur. Arşivlenen medya yeniden yayınlanamaz.
- **Testid'ler:** `admin-media-page`, `admin-media-loading`, `admin-media-empty`, `admin-media-error`, `admin-media-retry`, `admin-media-upload`, `admin-media-file-input`, `admin-media-upload-submit`, `admin-media-list`, `admin-media-item`, `admin-media-status`, `admin-media-room-type`, `admin-media-alt-tr`, `admin-media-alt-en`, `admin-media-cover`, `admin-media-order-up`, `admin-media-order-down`, `admin-media-publish`, `admin-media-unpublish`, `admin-media-archive`, `admin-media-retry-processing`.

### 2.6 Public Servis ve Web (REQ-4)

- **Public API:** Yalnız `READY` + `published_at` dolu + `archived_at` boş + aktif oda tipi. Oda tipi başına `displayOrder` sırası; `cover` tam bir kez true. Public yanıt catalog alanlarını kopyalamaz; web, catalog oda tipleriyle `roomTypeId` üzerinden eşler. Bu eşleme yalnız sunum birleştirmesidir; fiyat/uygunluk gibi iş kuralı üretmez.
- **Web RoomCard:** Yayınlanmış medya varsa `room-card-image` testid'li gerçek görsel; `alt` seçili dildeki alt metin, yoksa oda tipi adı. RoomCard, kapak olarak seçilen medyanın `card` varyantını `thumb` srcset ile kullanır; `cover` varyantı gelecekteki detay sayfası içindir. Görsel `next/image` veya `srcset` ile optimize edilir; `width`/`height` ve 3:2 oranla layout shift önlenir; kart tıklanabilir değildir, fiyat gösterilmez.
- **Fallback:** Medya yoksa, medya `PROCESSING`/`FAILED`/yayında değilse veya public medya isteği hata verirse mevcut `room-card-placeholder` görünür; medya hatası tüm vitrini düşürmez. Oda tipi/katalog hatası mevcut 0002 hata durumunu kullanır.
- **Erişim/cache:** Yalnız yayınlanmış varyantlar public-read; orijinaller private; bucket listeleme ve public write kapalı. Varyant URL'leri immutable ve uzun cache (`public, max-age=31536000, immutable`); public JSON yanıtı `no-store` (yayın/kaldırma anında görünür). Unpublish/archive public kopyaları kaldırır; daha önce indirilmiş tarayıcı kopyası V1 kabulüdür. CDN yoktur.
- **Kapsam:** Oda detay sayfası, galeri sayfası ve 360 tur bu dilimde yoktur; yalnız mevcut oda vitrini kartı.

### 2.7 Depolama, Local/CI ve Testler (REQ-5, REQ-6)

- **MinIO:** Local/CI `hotel-media` bucket'ı ve yalnız `public/*` öneki için public-read policy `minio-init` ile kurulur; tarayıcı CORS'u MinIO server'ında `MINIO_CORS_ALLOW_ORIGIN` ile verilir (local/CI `*`). Public write ve listeleme kapalıdır. Tarayıcı presigned PUT için `http://localhost:9000` gibi erişilebilir host kullanır; uygulama içi endpoint ayrı env'dir.
- **IT:** En az bir gerçek MinIO Testcontainers IT'si depolama, presigned, public/private ayrımı ve işleme hattını kanıtlar; birim testlerde depo portu mock'lanır. PostgreSQL Testcontainers migration/şema ve sorguları doğrular.
- **E2E sırası:** 0002 `@empty` **1/1** → catalog seed → 0002 `@seeded` **2/2** → 0003 `@seeded` design **40/40** → `@admin` **3/3** → `@media`. `@media` gerçek admin yüklemesiyle çalışır; standart seed'e medya eklenmez. 0002/0003 testleri ve arşivleri değişmez.
- **`@media` kapsamı:** Yetkili admin login → dosya yükle → `PROCESSING`/`READY` bekle → oda tipine ata/kapak yap/yayınla → public vitrinde `room-card-image` ve alt metni doğrula; yayından kaldır/arşivle → placeholder'a dön. Ayrıca FRONT_DESK **403**, geçersiz tür/boyut/SVG reddi ve yayında olmayan medyanın public'te görünmemesi negatif testleri. Presigned TTL ve EXIF/GPS temizliği backend IT'sinde doğrulanır.
- **Kanıtlar:** `specs/done/evidence/0005/`; 0001–0004 arşivleri SHA-256/git diff ile değişmez.

## 3. Constraints (Kapsam Dışı Dahil)

- **Teknik/İş kısıtları:**
  - **Kapsam:** Değişiklik `hotel-api` (`media` modülü, `catalog.api` read portu, public/admin web adaptörleri), `admin-api` (yalnız gateway route/header hijyeni), `admin-web` (`/media`), `web` (RoomCard), `e2e`, `docs`, `infra`, `.env.example`, `.github/workflows/ci.yml` ve `CREDITS.md` ile sınırlıdır.
  - **Modül sınırı:** `media → catalog` yalnız senkron read portu; `catalog → media` yasak; `media` catalog internal/tablosuna erişemez; cross-schema FK yoktur. Admin gateway iş kuralı içermez; admin-web yalnız admin-api'ye gider; `hotel-api` `/admin-api/**` JWT + rol korumalıdır.
  - **Veritabanı:** `V8__create_media_assets.sql` yalnız `media` şemasına dokunur; V5 init korunur; tablolar `@Table(schema = "media")`; UUIDv7; `timestamptz` UTC; `double`/`float` yok. `PANORAMA` rezerve; `Scene`/`Hotspot`/`Tour` yok.
  - **Depolama:** S3 uyumlu MinIO; orijinal private; yayınlanmış varyant public-read; public write/listeleme yok; public varyant anahtarları immutable/rastgele; presigned TTL 10 dk; public URL `MEDIA_PUBLIC_BASE_URL` ile yapılandırılır; CDN yok.
  - **Güvenlik:** SUPER_ADMIN/MANAGER; FRONT_DESK 403; CSRF/Origin 0004 kuralı; presigned URL loglanmaz; SVG/GIF/video yok; dosya boyutu/piksel sınırı; EXIF/GPS strip; HTML/kontrol karakteri yok; public yalnız yayınlanmış; admin-web'de sır yok.
  - **Performans:** Public medya toplu okunur; oda tipi başına N+1 yok; public JSON kısa cache; görsel varyantlar uzun cache; RoomCard görselinde layout shift yok.
  - **Test:** Her AC test/kanıta bağlı; gerçek MinIO IT; admin-web Vitest/MSW; `@media` E2E; 0002/0003 canlı testleri ve arşivleri değişmez.
- **Kapsam dışı (bilinçli olarak yapılmayacaklar):**
  - 360 tur içeriği: `Scene`, `Hotspot`, `Tour`, panorama işleme/viewer; D10 altyapı notu korunur.
  - Oda tipi detay sayfası, galeri sayfası, pricing/booking/payment, rezervasyon/ödeme.
  - Admin-web tasarım sistemi, karanlık tema, analitik.
  - CDN, AI görsel üretimi, video yükleme, hero video değişikliği.
  - Çoklu dosya yükleme, toplu yayın/silme/sıralama; V1 tek dosya akışı.
  - Medya işlemleri için admin audit kaydı; 0004 audit'i platform olaylarıyla sınırlıdır.
  - Public bucket listeleme, public write, kullanıcı tarafından seçilen depo anahtarı.
  - Kalıcı silme; V1 arşivleme.
- **Kabul edilen risk/ödünç:**
  - **CDN yok:** Public görseller MinIO'dan servis edilir; prod ölçek/gecikme ihtiyacı CDN ile ayrı spec'te ele alınır.
  - **Public-read cache:** Yayından kaldırılan görselin daha önce indirilmiş tarayıcı kopyası geri alınamaz; public API ve obje erişimi kesilir.
  - **Arşiv depolama büyümesi:** Orijinal/private varyantlar V1'de tutulur; otomatik temizlik yoktur, operasyonel runbook'a bağlıdır.
  - **Audit yok:** Medya işlemleri uygulama logları/request-id ile izlenir; denetim ihtiyacı ayrı spec'tir.
  - **In-process işleme:** Broker yok; restart toparlaması ve idempotency ile sınırlıdır; yüksek hacim Aşama 2 konusudur.
  - **Tek dosya:** Toplu yükleme/toplu işlem yoktur; admin operasyonu dosya bazındadır.
  - **0002/0003 sıra bağımlılığı:** `@media` önce çalışırsa placeholder testleri etkilenir; CI sırası korunmalıdır.
- **Doküman ve CI Etkisi (Developer):**
  1. **`docs/architecture.md` §3/§7/§11:** media hattı, MinIO public/private ayrımı, presigned/public URL, `media → catalog` read portu ve `@media` CI notu.
  2. **`docs/frontend.md`:** RoomCard gerçek görsel, alt metin/fallback, placeholder, `room-card-image` ve görsel optimizasyonu.
  3. **`infra/README.md`:** `hotel-media` bucket policy/CORS, public/private önekler, presign host ve local/CI komutları.
  4. **`.env.example`:** `MEDIA_S3_ENDPOINT`, `MEDIA_S3_PUBLIC_ENDPOINT`, `MEDIA_S3_BUCKET`, `MEDIA_S3_ACCESS_KEY`, `MEDIA_S3_SECRET_KEY`, `MEDIA_PUBLIC_BASE_URL`, `MEDIA_PRESIGN_TTL` (Duration, ör. `10m`), `MEDIA_PROCESSING_MAX_ATTEMPTS`, `MEDIA_PROCESSING_STALE_THRESHOLD`, `MINIO_CORS_ALLOW_ORIGIN`; boyut/piksel limitleri V1'de `MediaRules` sabitleridir. Local değerler açıkça işaretli, prod sırları repoda yok.
  5. **`.github/workflows/ci.yml`:** MinIO policy/CORS init, gerçek MinIO IT'si, `@media` aşaması ve 0002/0003/`@admin` sırasının korunması.
  6. **`CREDITS.md`:** `@media` fixture'ı olarak kullanılan mevcut Pexels lisanslı hero posterinin kaynağı/lisansı; `e2e/README.md` `@media` çalıştırma notu.
  7. **`HotelApiModularityTest`:** `media → catalog` read portu allowlist'i ve migration şema kontrolü yeşil.

## 4. Context

- **Mevcut durum:** `media` modülü boş iskelet; `V5__init_media_schema.sql` yalnız `media` şeması ve `app` DML izinlerini kurar, tablo yoktur. `hotel_core` sıradaki migration numarası **V8**'dir (V1–V7 dolu).
- **MinIO:** Local compose'da `hotel-media` bucket'ı `minio-init` ile oluşturulur (API `9000`, konsol `9001`); uygulamada henüz S3 istemcisi/bağımlılığı yoktur. 0005'te S3 istemcisi, bucket policy/CORS ve presigned akış eklenecektir.
- **Catalog:** V7 ile `catalog.room_types`/`amenities` ve public `GET /api/room-types` hazırdır. `catalog` yaprak modüldür; `media → catalog` read izni R3'te vardır, ters yön yasaktır. `catalog.api` şu an yalnız kimlik tiplerini taşır; 0005'te salt-okunur aktif oda tipi portu eklenecektir.
- **Admin/auth:** 0004 ile admin-api kimlik/JWT/JWKS/gateway ve admin-web login tamamlandı; `/admin-api/**` hotel-api'de JWT + rol korumalıdır. Medya/tur yönetimi SUPER_ADMIN + MANAGER; FRONT_DESK değildir (`docs/domain.md` §3).
- **E2E:** `E2E_EVIDENCE_DIR`, `@empty`/`@seeded`/`@admin` aşamaları çalışır. 0002/0003 canlı testleri `room-card-placeholder` bekler; 0005 medyası bu aşamalardan sonra `@media` ile yüklenmelidir. 0005 kanıtları `specs/done/evidence/0005/` altına yazılır; 0001–0004 arşivleri değişmez.
- **Bağımlı spec'ler:** `specs/done/0002-room-catalog.md` (oda tipleri, RoomCard placeholder, detay sayfası planı), `specs/done/0004-admin-identity.md` (auth, gateway, roller, CSRF). Oda detay sayfası media + pricing sonrası ayrı spec'tir.
- **Onaylanan kararlar:** Q1 presigned doğrudan yükleme; Q2 asenkron işleme; Q3 yalnız yayınlanmış varyantlar public-read; Q4 WebP+JPEG varyantları ve limitler; Q5 medya toplu okuma + web sunum eşlemesi; Q6 medya seed'i yok, `@media` sonda; Q7 taslak/tek oda tipi, kapak fallback, sıra; Q8 opsiyonel alt metin + arşivleme; Q9 MinIO public-read/CORS + gerçek MinIO IT; Q10 audit yok.

## 5. Acceptance Criteria

- **AC-1 (Media Modeli, Şema ve Catalog Sınırı — `REQ-1`, Q1/Q4/Q7/Q8):**
  - `media.media_assets` ve `media.media_variants`, `db/migration/media/V8__create_media_assets.sql` ile yalnız `media` şemasında oluşur; V5 init korunur; başka şemaya tablo/indeks/kısıt eklenmez; JPA entity'leri `@Table(schema = "media")` taşır; `double`/`float` yoktur.
  - `MediaAsset`; UUIDv7, `PHOTO` türü, `UPLOADED/PROCESSING/READY/FAILED` durumu, orijinal anahtar/MIME/boyut/ölçü, nullable `room_type_id`, `published_at`, `archived_at`, `display_order`, `is_cover`, TR/EN alt metin, `failure_code` ve deneme sayısı taşır; varyantlar private/public depo anahtarlarını ayrı taşır. `PANORAMA` rezerve; `Scene`/`Hotspot`/`Tour` tablosu yoktur.
  - Oda tipi yalnız `catalog.api` read portuyla doğrulanır; `catalog.internal` importu, catalog tablosuna erişim ve cross-schema FK yoktur; `catalog → media` bağımlılığı yoktur; `HotelApiModularityTest` yeşildir.
  - Kısıtlar: oda tipi başına yayında en fazla bir kapak; arşivsiz kayıtlarda oda tipi içinde benzersiz sıra; geçersiz `type/status/variant/format` reddi; negatif sıra/boyut/ölçü reddi; arşiv/publish/unpublish yaşam döngüsü test edilir.
  - **Kanıt:** `AC-1-media-schema.log`, `AC-1-modularity.log`, `AC-1-media-constraints.log`.

- **AC-2 (Presigned Yükleme, Doğrulama ve Depolama — `REQ-2`, `REQ-5`, Q1/Q4/Q9):**
  - Yetkili admin yükleme başlattığında 10 dk geçerli, tek nesne için presigned PUT döner; tarayıcı dosyayı doğrudan depoya yükler; dosya gateway'den geçmez; `complete` nesneyi doğrular ve `202 PROCESSING` döner.
  - JPEG/PNG/WebP kabul; SVG, GIF, video ve diğer türler `415`/`400`; 15 MB üstü, 6000×6000/36 MP üstü veya 800×600 altı reddedilir; geçersiz nesne silinir ve public olmaz.
  - Depo anahtarları rastgele/PII'siz; orijinal private; presigned URL/TTL test edilir; süresi geçmiş veya complete sonrası tekrar tamamlama `409`/`404`; FRONT_DESK tüm yükleme uçlarında `403`.
  - **Kanıt:** `AC-2-presigned-upload.log`, `AC-2-upload-validation.log`, `AC-2-upload-ttl.log`, `AC-2-frontdesk-403.log`.

- **AC-3 (Asenkron İşleme, EXIF/GPS ve Varyantlar — `REQ-3`, Q2/Q4):**
  - `complete` sonrası medya `PROCESSING`; worker `READY` veya `FAILED` üretir; public yalnız `READY` görür; admin durumu yoklayabilir.
  - Orientation uygulanır; EXIF/GPS ve diğer metadata temizlenir; WebP + JPEG `thumb` 480×320, `card` 1200×800, `cover` 1920×1280 üretilir; orijinalden büyütme yok; 3:2 merkez kırpma; orijinal private kalır.
  - `FAILED` stabil hata kodu taşır; en fazla 3 otomatik deneme; manuel retry çalışır; restart sonrası eski `PROCESSING` toparlanır; işlem idempotenttir. `FAILED`/`PROCESSING` medya public API'de yoktur.
  - **Kanıt:** `AC-3-processing-pipeline.log`, `AC-3-exif-gps-strip.log`, `AC-3-variants.log`, `AC-3-retry-recovery.log`.

- **AC-4 (Admin Yönetimi, Roller, Yayın/Kapak/Sıra — `REQ-2`, Q7/Q8):**
  - Admin-web `/media` ekranı TR ve sade; yükleme, liste, oda tipi atama, sıralama, kapak, yayınla/kaldır, arşivle, retry ve alt metin düzenleme akışları çalışır; loading/empty/error durumları ve testid'ler doğrulanır.
  - SUPER_ADMIN ve MANAGER tüm işlemleri yapabilir; FRONT_DESK tüm admin media uçlarında `403` alır; arayüzde gizleme güvenlik sayılmaz.
  - Her medya en fazla bir oda tipine atanır; yayın için `READY` + aktif oda tipi + arşivsiz; yayındaki medyanın oda tipi değiştirilemez; oda tipi başına yayında tek kapak, kapak yoksa ilk sıradaki; sıra benzersiz ve atomik; unpublish anında gizler; archive geri alınamaz ve public kopyaları kaldırır.
  - TR/EN alt metin opsiyonel, ≤200 karakter; eksikse public fallback oda tipi adı; admin uyarısı gösterilir.
  - **Kanıt:** `AC-4-admin-media.png`, `AC-4-admin-media-states.png`, `AC-4-frontdesk-403.log`, `AC-4-publish-cover-order.log`, `AC-4-archive.log`.

- **AC-5 (Public Servis, Oda Vitrini ve Fallback — `REQ-4`, Q3/Q5/Q7/Q8):**
  - Public API yalnız `READY` + yayınlanmış + arşivsiz + aktif oda tipli medyayı, `displayOrder` sırasıyla ve oda tipi başına tam bir `cover:true` ile döndürür; en fazla 50 oda tipi; catalog alanları kopyalanmaz; orijinal anahtar/işleme hatası/yayınlanmamış kayıt sızmaz.
  - Web, catalog ve medya yanıtlarını `roomTypeId` ile sunum amaçlı eşler; RoomCard, kapak olarak seçilen medyanın `card` varyantını `room-card-image` testid'i, seçili dilde alt metin ve optimize `srcset`/`next/image` ile gösterir; görsel yoksa/processing/failed/API hatasında `room-card-placeholder` görünür ve sayfa düşmez; kart tıklanabilir değildir, fiyat içermez.
  - Public görseller yalnız yayınlanmış varyantlardan public-read servis edilir; orijinaller private; public write/listeleme yok; cache başlıkları doğrulanır; unpublish/archive sonrası public API ve obje erişimi kesilir.
  - **Kanıt:** `AC-5-public-media.log`, `AC-5-room-card-image.png`, `AC-5-placeholder-fallback.png`, `AC-5-unpublished-hidden.log`, `AC-5-public-access.log`.

- **AC-6 (Testler, E2E Sırası, Kanıtlar ve Doküman/CI — `REQ-6`, Q6/Q9/Q10):**
  - Birim + Testcontainers IT (PostgreSQL + gerçek MinIO) depolama/presigned/public-private/işleme hattını; admin-web Vitest + RTL + MSW yükleme/liste/hata/retry/yetki akışlarını doğrular.
  - E2E sırası: 0002 `@empty` 1/1 → catalog seed → 0002 `@seeded` 2/2 → 0003 design 40/40 → `@admin` 3/3 → `@media`; 0002/0003 testleri değişmez, standart seed'e medya eklenmez, `@media` gerçek yükleme fixture'ı kullanır.
  - `@media`: yetkili login → upload → READY → assign/cover/publish → public `room-card-image`; FRONT_DESK 403; geçersiz tür/boyut/SVG; yayında olmayan medya public'te yok; unpublish/archive → placeholder. Presigned TTL ve EXIF/GPS backend IT'sinde doğrulanır.
  - Kanıtlar `specs/done/evidence/0005/` altında; 0001–0004 arşivleri SHA-256/git diff ile değişmemiş; `.env.example`, `infra`, `docs`, CI ve `CREDITS.md` güncellenmiştir; pipeline yeşildir.
  - **Kanıt:** `AC-6-media-e2e-summary.md`, `AC-6-minio-it.log`, `AC-6-ci-run.md`, `AC-6-archive-sha256.txt`.

## 6. Definition of Done

- [x] Tüm AC'ler doğrulandı (test ve/veya görsel kanıt)
- [x] Pipeline yeşil (lint + build + test + kritik akış smoke)
- [ ] Kod incelemesi onaylandı (bağımsız review: `evidence/0005/code-review-2026-11-04.md`; fix PR açık)
- [x] Davranış değiştiyse dokümanlar güncellendi
- [ ] Spec `specs/done/` klasörüne taşındı, görsel kanıtlar eklendi (bağımsız onay sonrası)
- [x] SCORECARD dolduruldu

## 7. SCORECARD

- **Spec revizyon sayısı:** 2
- **Düzeltme turu sayısı:** 1 (bağımsız kod incelemesi; B1–B13 bulguları)
- **Bulgu gerçek/gürültü oranı:** 13 gerçek / 1 harici yanlış pozitif (GitGuardian, base commit `c9e7da4`); bağımsız review CR-1–CR-10
- **Regresyon sayısı:** 0
- **Kaçan hata (escaped bug):** 3 (bağımsız review merge sonrası CR-1/CR-2/CR-3 buldu; fix PR açık)

## 8. Ek A — Onaylanan CLARIFY Kararları (Q1–Q10)

| Kod | Karar | Etkilenen REQ/AC |
|---|---|---|
| Q1=A | Presigned doğrudan yükleme; 10 dk TTL; dosya gateway'den geçmez; yükleme sonrası sunucu doğrulaması | REQ-2, REQ-5, AC-2 |
| Q2=A | Asenkron işleme; `202 PROCESSING`; durum yoklama; 3 otomatik deneme + manuel retry; restart toparlaması | REQ-3, AC-3 |
| Q3=A | Yalnız yayınlanmış varyantlar public-read; orijinaller private; immutable URL + uzun cache; CDN yok | REQ-4, REQ-5, AC-5 |
| Q4=A | WebP + JPEG; thumb 480×320, card 1200×800, cover 1920×1280; 15 MB, 36 MP, 800×600 min; EXIF/GPS strip; SVG/GIF/video reddi | REQ-1, REQ-3, REQ-5, AC-1/AC-2/AC-3 |
| Q5=A | Medya toplu public okuma; web catalog ile `roomTypeId` üzerinden sunum eşlemesi; catalog alanları kopyalanmaz | REQ-4, AC-5 |
| Q6=A | Standart seed'e medya yok; `@media` `@admin` sonrası gerçek yükleme; 0002/0003 testleri değişmez | REQ-6, AC-6 |
| Q7=A | Taslak/atamasız yükleme; bir medya en fazla bir oda tipi; yayında tek kapak + ilk sıra fallback; benzersiz sıra | REQ-1, REQ-2, AC-1/AC-4 |
| Q8=A | TR/EN alt metin opsiyonel; fallback oda tipi adı; arşivleme, kalıcı silme yok | REQ-2, REQ-4, AC-4/AC-5 |
| Q9=A | MinIO `minio-init` public-read policy + server CORS env; tarayıcı erişilebilir presign host; gerçek MinIO IT | REQ-5, REQ-6, AC-2/AC-6 |
| Q10=A | Medya audit'i 0005'te yok; uygulama logları/request-id; ayrı spec | REQ-6, AC-6 |

## 9. Uygulama Kararları (rev.2, onaylı)

| Kod | Karar | Gerekçe / Etki |
|---|---|---|
| D1 | `GET /admin-api/media/room-types` aktif oda tipi seçeneklerini döner | Admin-web'in catalog internal'ına erişmeden atama yapabilmesi; REQ-2, AC-4 |
| D2 | Görsel işleme `org.sejda.imageio:webp-imageio` + TwelveMonkeys JPEG + `metadata-extractor` ile yapılır | WebP yazımı ve EXIF/GPS okuma; native WebP kütüphanesi CI'da doğrulanır; REQ-3, AC-3 |
| D3 | `processing_started_at` ve DB lease/claim (`claimForProcessing`) eklendi | Restart/gövde çökmesinde aynı işin iki worker'ca işlenmemesi; REQ-3, AC-3 |
| D4 | Kapak yalnız yayınlanmış, `READY`, oda tipi atanmış ve arşivsiz medya için seçilebilir | Public'te kapak tutarlılığı; REQ-2, AC-4 |
| D5 | Public API her varyant için WebP tercihli URL döner; admin önizlemesi kısa ömürlü signed GET URL kullanır | Private/public ayrımı ve admin önizleme; REQ-2, REQ-4, AC-4/AC-5 |
| D6 | `complete` yalnız HEAD + magic-byte + boyut/piksel doğrulaması yapar; derin işleme asenkron worker'dadır | Gateway zaman aşımını korumak; REQ-3, AC-3 |
| D7 | Web RoomCard native `<img>` + `srcset` kullanır; `next/image` kullanılmaz | MinIO hazır varyantları zaten optimize eder; Next image optimizer gereksiz; REQ-4, AC-5 |
| D8 | Reorder arşivsiz tüm medya listesini ister; oda değişiminde yeni sıra `max+1` olur | Sıra tekilliği ve atomiklik; REQ-2, AC-4 |
| D9 | Manuel retry otomatik deneme bütçesini sıfırlar | Operatörün FAILED kaydı yeniden işleyebilmesi; REQ-3, AC-3 |
| D10 | Web public medya isteklerini 50 oda tipi ID'siyle parçalar | Public API üst sınırıyla uyum; REQ-4, AC-5 |
| D11 | Local/CI/test dışında güvenlik açısından zayıf depolama config'i başlangıçta fail-fast eder | Prod'da varsayılan MinIO kimlik bilgisi/localhost URL ile açılmayı engeller; REQ-5, AC-2 |
| D12 | MinIO CORS'u `minio-init` yerine MinIO server `MINIO_CORS_ALLOW_ORIGIN` env'i ile verilir | Kullanılan `mc` sürümünde `mc cors` yok; server env'i tekrarlanabilir ve local/CI'da `*` |
| D13 | Varyant çıktısı kaynak kırpımından büyük olamaz | "Orijinalden büyütme yok" kuralını 800×600 gibi küçük geçerli görsellerde de uygular; REQ-3, AC-3 |
| D14 | Worker claim'i `processing_lease_id` ile fencing yapar; ready/failed geçişleri lease koşulludur | Arşiv/paralel worker yarışında eski worker'ın yazmasını engeller; REQ-3, AC-3/AC-4 |
| D15 | Public S3 nesneleri için DB cleanup-intent tablosu + afterCommit/rollback telafisi, grace süreli retry ve dead-letter kullanılır | S3/DB atomik olmadığından public kopya invariant'ını kalıcı orphan bırakmadan korur; in-doubt commit'te yayındaki nesneyi silmez; REQ-5, AC-5 |
| D16 | Processing varyantları silinip yeniden yaratılmaz; `(variant, format)` anahtarında yerinde güncellenir | Yarım kalan işlemde DB varyant boşluğu/duplicate oluşmasını engeller; REQ-3, AC-3 |
