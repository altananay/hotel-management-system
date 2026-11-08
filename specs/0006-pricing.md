# Fiyatlandırma ve Fiyat Teklifi (`pricing`)

> Doldurmadan önce: `AGENTS.md` (altın kurallar) ve `docs/architecture.md` okunur.
> Numara: `specs/` altındaki en büyük numaranın + 1'i. Dosya: `specs/<####>-<english-name>.md` (İngilizce kebab-case; içerik Türkçe). Branch: `feature/<####>-<english-name>` (yalnızca `releasable`'dan).
> Spec onaylanmadan kod yazılmaz (Altın Kural 1). Onayı Takım Yöneticisi verir.

---

## 0. Künye

- **Spec No:** `0006`
- **Başlık:** Fiyatlandırma ve Fiyat Teklifi (`pricing`)
- **Durum:** Rev.1 uygulama tamamlandı; kapanış onayı bekliyor (2026-11-08)
- **Branch:** `feature/0006-pricing`
- **İlgili doküman/kararlar:** `docs/architecture.md` (§3 `pricing`, §5 R1/R3/R6, §7/D15, §8, §10 D8/D15, §11), `docs/domain.md` (§1 fiyat terimleri, §3.4–§3.7, §3.13–§3.14, rol tablosu), `docs/conventions.md` (§1, §2.1, §2.3, §2.4, §3, §4), `docs/frontend.md`, `docs/testing.md`, `docs/git.md`, `specs/done/0002-room-catalog.md`, `specs/done/0004-admin-identity.md`, `specs/done/0005-media.md`, Takım Yöneticisi 0006 brief'i (2026-11-08); Onaylı kararlar: `Q1=C`, `Q2=A`, `Q3=A (koşullu)`, `Q4=A`, `Q5=A`, `Q6=A`, `Q7=A`, `Q8=A`; misafir sayısı fiyatı etkiler (2026-11-08)
- **Tarih:** 2026-11-08
- **Sahip:** Analist (`analyst`)
- **Onaylayan (Takım Yöneticisi):** Takım Yöneticisi (Onaylandı — 2026-11-08)

## 1. Intent (Neden?)

Otel yönetimi, oda tipleri için tarife planı ve sezon bazlı, misafir sayısına göre değişen TRY fiyatlarını tanımlayabilmek, gerektiğinde elle fiyat ve kur düzeltmesi yapabilmek ve EUR/USD fiyatların güncel TCMB kurlarıyla tutarlı biçimde türetilmesini istiyor. Misafirler, konaklayacakları dönem ve misafir sayısı için hangi gece ne kadar ödeyeceklerini, kullanılan kur ve kur güvenlik payını (buffer) anlaşılır bir fiyat teklifinde görmek istiyor; rezervasyon anında onaylanan fiyatın sonradan değişmemesi kritik. Başarı; fiyatın tek bir otorite tarafından yönetilmesi, fiyat/kur değişimlerinin teklife doğru ve açıklanabilir biçimde yansıması, TCMB erişilemediğinde akışın durmaması ve onaylanan fiyatın kur, buffer ve yuvarlama bilgisiyle değişmez biçimde saklanmasıdır. Bu dilimde oda detay sayfası ve oda kartlarında fiyat gösterimi (0007'de zorunlu olarak ele alınacaktır), rezervasyon/ödeme akışı, promosyon kodları, KDV ayrıştırma/fatura, katalog ve medya değişiklikleri, admin kullanıcı yönetimi, 360 tur ve analitik bilinçli olarak yapılmayacaktır.

## 2. Requirements

- **REQ-1 (Fiyatlandırma Çekirdeği ve Şema — Q1=C, Q2=A, Q5=A, Q6=A, misafir sayısı kararı):** Fiyatlandırma modülü; tarife planları (`RatePlan`), sezonlar (`Season`), tarife planı + oda tipi + misafir sayısı bazlı gece fiyatları, manuel fiyat ve manuel kur override'ları, TCMB kur geçmişi (`FxRate`), buffer/yuvarlama ayarları, fiyat teklifi (`Quote`) ve fiyat anlık görüntüsü (`PriceSnapshot`) kavramlarını taşımalıdır. Fiyatın tek otoritesi `pricing`'dir; başka modül fiyat hesaplamaz. Öncelik iki aşamalıdır: fiyat kaynağında **elle fiyat > sezon fiyatı > plan baz fiyatı**; kur kaynağında **elle kur > TCMB (son bilinen dahil)**. Onaylanan fiyat; kur, buffer ve yuvarlama bilgisiyle birlikte değişmez snapshot olarak saklanmalıdır.
  - **Tarife planı (`RatePlan`):** Birden çok plan tanımlanabilir. Her planın benzersiz bir kodu, TR/EN adı, aktiflik durumu ve “varsayılan” işareti vardır. Aynı anda **yalnız bir aktif varsayılan plan** bulunabilir; varsayılan plan pasifleştirilemez, önce başka bir aktif plan varsayılan yapılmalıdır. Fiziksel silme yoktur; kullanımdaki plan pasifleştirilir. Public fiyat teklifi yalnız varsayılan planı kullanır; misafir plan seçimi 0007'ye bırakılmıştır.
  - **Sezon (`Season`):** Her sezon bir tarife planına aittir; benzersiz kod, TR/EN ad, başlangıç ve bitiş tarihi taşır. Aynı plan içinde sezonlar **çakışamaz**; tarih aralığı boşlukları plan baz fiyatına düşer. Konaklama geceleri tek tek eşlenir; çıkış günü fiyatlanmaz. Sezon fiyatı belirli bir oda tipi + misafir sayısı için tanımlanmamışsa plan baz fiyatı geçerlidir.
  - **Baz ve sezon fiyatı:** Gece fiyatı; plan + oda tipi + misafir sayısı ekseninde TRY olarak tanımlanır. Misafir sayısı **1..oda tipi `maxGuests`** aralığındadır; fiyat her misafir sayısı için açıkça girilir, otomatik kişi başı artış/ek ücret formülü uygulanmaz. Sezon fiyatı aynı eksende opsiyonel override'dır. Tutarlar pozitif, en fazla iki ondalıklı ve `BigDecimal`/`Money` taşıyıcılıdır; `double`/`float` hiçbir katmanda kullanılamaz.
  - **Manuel fiyat override'ı:** Plan + oda tipi + misafir sayısı + tarih aralığı düzeyinde TRY gece fiyatıdır; sezon ve plan baz fiyatını ezer. Aynı anahtar için çakışan override tanımlanamaz. Manuel fiyat silinebilir; mevcut snapshot'ları etkilemez.
  - **Manuel kur override'ı:** EUR/USD + tarih düzeyinde pozitif kur değeridir; o tarih için TCMB yerine geçer. Buffer manuel kurda da uygulanır. Aynı para birimi + tarih için tek kayıt bulunur.
  - **Kur geçmişi (`FxRate`):** TCMB satış kuru, kur tarihi, kaynak (TCMB/manuel) ve çekim zamanı saklanır. Geçmiş kur admin ekranında görüntülenebilir. Fiyat değişikliklerinin ayrı bir audit/geçmiş kaydı bu dilimde yoktur; snapshot'lar geçmiş fiyatın kanıtıdır.
  - **Ayarlar:** Buffer yüzdesi (varsayılan `%2,00`, `0–100` aralığı) ve yuvarlama adımı (varsayılan `5,00`, pozitif) global olarak yapılandırılır. Yuvarlama yönü **her zaman yukarı** (bir üst adımın katı) sabittir. Değişiklikler yalnız yeni teklifleri etkiler; mevcut snapshot'lar değişmez.
  - **Fiyat teklifi (`Quote`):** Oda tipi + konaklama dönemi + misafir sayısı + para birimi için hesaplanır; gece dökümü, fiyat kaynağı, sezon, kur kaynağı/tarihi, buffer, yuvarlama ve toplam taşır. Teklif gösterge niteliğindedir; `quotedAt` ve `expiresAt` (15 dakika) taşır. Uzun süreli teklif kaydı zorunlu değildir; rezervasyon anında fiyat yeniden hesaplanır.
  - **Fiyat anlık görüntüsü (`PriceSnapshot`):** Rezervasyon anında onaylanan fiyatın değişmez kaydıdır; gece dökümü, kaynaklar, kur, buffer, yuvarlama, toplam ve para birimini taşır. Snapshot'ı `pricing` üretir; kalıcı sahibi `booking`'dir (architecture §3). Snapshot hiçbir koşulda geriye dönük değişmez.
  - **Para birimi:** Baz para birimi TRY'dir; yalnız TRY, EUR ve USD desteklenir. EUR/USD fiyatı TRY baz fiyatından TCMB/manuel kur + buffer + yukarı yuvarlama ile türetilir. Gösterim para birimi = tahsilat para birimidir; ödeme anında çevrim yapılmaz; iade aynı para birimindedir. Fiyatlar KDV dahil tek tutardır.
  - **Şema:** `db/migration/pricing/V12__*.sql` (global sıradaki ilk numara V12; V1–V11 dolu) ve gerekiyorsa devamı yalnız `pricing` şemasına tablo/indeks/kısıt ekler; başka şemaya dokunamaz. V2 init şeması ve `app` DML izinleri korunur. JPA varlıkları `@Table(schema = "pricing")` taşır; UUIDv7, UTC `timestamptz`, iş günü Europe/Istanbul, konaklama/sezon tarihleri `LocalDate`'tir. Cross-schema foreign key yoktur.
  - **Oda tipi sınırı (Q2=A):** `pricing`, oda tipi varlığını/aktifliğini yalnız `catalog.api` public read portu üzerinden doğrular; `catalog` internal'ına, tablosuna veya repository'sine erişemez. `pricing → catalog` read kenarı bu spec ile onaylanmıştır; `catalog → pricing` yasaktır.
  - **Olaylar:** `RatePlanChanged` olayı ve promo kullanım olayları bu dilimde yayımlanmaz; tüketicisi yoktur. İhtiyaç doğduğunda ayrı spec/ekleme ile ele alınır.

- **REQ-2 (Admin Fiyat Yönetimi — Q1=C, Q2=A, Q8=A):** `admin-web` üzerinde TR, sade bir fiyat yönetimi ekranı bulunmalıdır. `SUPER_ADMIN` ve `MANAGER` tarife planlarını, sezonları, plan + oda tipi + misafir sayısı baz/gece fiyatlarını, manuel fiyat/kur override'larını ve buffer/yuvarlama ayarlarını yönetebilmeli; güncel TCMB kuru, son güncelleme zamanı, kur kaynağı ve bayat kur uyarısını görebilmelidir. `FRONT_DESK` tüm fiyat yönetimi işlemlerinde **403** almalıdır; arayüzde gizleme güvenlik sayılmaz. Tüm admin çağrıları `admin-api` gateway üzerinden `hotel-api` admin yüzeyine gitmeli; admin-web doğrudan `hotel-api`'ye istek atmamalıdır.
  - **Kapsam:** Tarife planı oluşturma/düzenleme/aktifleştirme/pasifleştirme/varsayılan yapma; sezon oluşturma/düzenleme; baz ve sezon fiyatlarını oda tipi + misafir sayısı matrisinde girme; manuel fiyat ve manuel kur override'ları; buffer/yuvarlama ayarları; güncel ve geçmiş kur görüntüleme; varsayılan plan için eksik fiyat uyarısı. Toplu içe aktarma, takvim grid'i, promo kodları, fiyat audit'i, admin kullanıcı yönetimi ve onay iş akışı bu dilimde yoktur.
  - **Varsayılan plan ve bütünlük:** Bir plan varsayılan yapılırken aktif olmalı ve aktif tüm oda tiplerinin `1..maxGuests` misafir sayıları için baz fiyatı tanımlı olmalıdır; eksik varsa varsayılan yapma reddedilir ve eksik kombinasyonlar listelenir. Sonradan eklenen yeni bir oda tipi için fiyat girilene kadar admin ekranında eksik uyarısı görünür; o kombinasyon için public teklif üretilmez, diğer kombinasyonlar çalışmaya devam eder.
  - **Sezon ve override doğrulaması:** Sezon başlangıcı bitişten sonra olamaz; aynı plan içinde çakışan sezon `409` ile reddedilir. Manuel fiyat aralığı başlangıcı bitişten sonra olamaz; aynı plan + oda tipi + misafir sayısı için çakışan manuel fiyat `409` ile reddedilir. Manuel kur aynı para birimi + tarih için tektir. Negatif/sıfır tutar, geçersiz buffer/yuvarlama adımı ve geçersiz para birimi `400` ile reddedilir.
  - **Değişikliğin etkisi/iptali:** Plan, sezon, fiyat, override veya ayar değişikliği yalnız yeni teklifleri etkiler; mevcut fiyat snapshot'ları değişmez. Plan pasifleştirme veya sezon/override silme, geçmiş snapshot'ları geriye dönük etkilemez. Admin ekranı bu etkiyi kullanıcıya açıkça göstermelidir.
  - **Durumlar:** Fiyat ekranı **yükleme**, **boş** (henüz plan/fiyat yok), **hata** (kullanıcı dostu mesaj + tekrar dene) ve **dolu** durumlarını desteklemelidir. Teknik hata kodu/stack trace kullanıcıya gösterilmez. Hata ve boş durumlarında ekran çökmemelidir.
  - **Geçmiş kur görünürlüğü:** TCMB ve manuel kur kayıtları tarih/para birimi/kaynak ile listelenebilmeli; bayat kur varsa uyarı gösterilmelidir. Fiyat değişiklikleri için ayrı audit/geçmiş ekranı bu dilimde yoktur; `updatedAt` gösterilir.
  - **Oda tipi seçenekleri:** Admin ekranındaki oda tipi seçenekleri `catalog.api` public read portundan gelen **aktif** oda tipleridir; pasif oda tipleri yeni fiyat/override seçiminde yer almaz, mevcut kayıtlar read-only görünür. Medya modülünün oda tipi ucu bu amaçla yeniden kullanılmaz.

- **REQ-3 (TCMB Entegrasyonu, Kur ve Fallback — Q4=A, Q6=A):** TCMB `today.xml` günlük olarak çekilmeli (iş günleri ~15:30 Europe/Istanbul; hafta sonu/tatil → son yayımlanan), EUR/USD **satış** kuru geçmişiyle saklanmalıdır. EUR/USD fiyatı `TRY × satış kuru × (1 + buffer)` formülüyle türetilmeli ve **her zaman bir üst yuvarlama adımının katına** yuvarlanmalıdır; varsayılan buffer `%2,00`, varsayılan adım `5,00`'dir. TRY fiyatlarına buffer ve yuvarlama uygulanmaz.
  - **Gece bazlı hesap:** Konaklamanın her gecesi ayrı fiyatlanır; gece tutarı yukarı yuvarlanır ve toplam, yuvarlanmış gece tutarlarının toplamıdır. Yuvarlanmış ara sonuç yeniden hesaplanmaz; toplam yeniden yuvarlanmaz (`docs/conventions.md` §3).
  - **Kur kaynağı:** O gün için manuel kur varsa o kullanılır; yoksa TCMB'nin o gün veya öncesinde yayımladığı son satış kuru kullanılır. TCMB'ye canlı çağrı teklif hesaplaması sırasında yapılmaz; depolanan kur kullanılır. Kur yanıtında kaynak (TCMB/manuel), kur tarihi ve bayat olup olmadığı görünür.
  - **Fallback:** TCMB erişilemezse **son bilinen kur** kullanılır ve admin ekranında uyarı gösterilir; akış bloke edilmez. Hiç kur yoksa TRY teklifi çalışır; EUR/USD teklifi `503 pricing.fx_rate_unavailable` ile reddedilir. Bayat kur için maksimum yaş sınırı yoktur; kur tarihi bugünden eskiyse uyarı gösterilir.
  - **Test edilebilirlik:** Local/CI ortamında TCMB'ye ağ bağımlılığı olmadan fixture/fake kullanılmalı; hafta içi, hafta sonu, tatil, bozuk XML, erişilemezlik ve kur yokluğu senaryoları test edilmelidir. Zaman bağımlı davranışlar enjekte edilen `Clock` ile test edilir; gerçek bekleme yapılmaz.
  - **Ayarların etkisi:** Buffer/yuvarlama değişikliği yalnız yeni teklifleri etkiler; mevcut teklifin gösterimi ve snapshot değişmez. Admin ekranı güncel ayarları ve son değişiklik zamanını gösterir.

- **REQ-4 (Public Fiyat Teklifi ve Booking Portu — Q1=C, Q2=A, Q4=A, Q5=A):** Misafir; oda tipi, konaklama dönemi, misafir sayısı ve para birimi ile **anonim** fiyat teklifi alabilmelidir. Teklif, varsayılan aktif tarife planını kullanmalı; oda tipi aktif olmalı; misafir sayısı `1..maxGuests` aralığında olmalı; giriş tarihi bugünden (HotelDate) önce olamaz ve çıkış girişten sonra olmalıdır. **Konaklama en fazla 30 gece olabilir; `checkOut - checkIn > 30` ise `400 pricing.invalid_period` döner. 30 gece dahil kabul edilir; sınır `hotel.pricing.max-stay-nights` ile yapılandırılabilir (varsayılan 30).** Para birimi TRY/EUR/USD'dir; başka değer reddedilir.
  - **Yanıt dökümü:** Teklif; oda tipi, varsayılan plan kodu/adı, dönem, misafir sayısı, para birimi, gece sayısı, her gece için tarih, sezon, fiyat kaynağı (manuel/sezon/baz), TRY gece tutarı, kur ve kaynağı/tarihi, buffer, yuvarlama adımı, gece tutarı ve toplamı taşımalıdır. Tutarlar frontend'e **string ondalıklı** taşınır; JS `number` ile hesaplanmaz. Teklifte misafir kimliği/PII yer almaz.
  - **Geçerlilik ve yeniden hesaplama:** Teklif `quotedAt` ve `expiresAt` (15 dakika) taşır; gösterge niteliğindedir. Rezervasyon anında fiyat yeniden hesaplanır; eski teklifin tutarı körü körüne kullanılamaz. Fiyat değiştiyse yeni tutar misafire gösterilmeden rezervasyon oluşturulamaz. `pricing` uzun süreli teklif kaydı tutmaz; rezervasyon anında üretilen snapshot `booking` tarafından saklanır.
  - **Booking portu:** `booking → pricing` senkron çağrısı (R3) ile rezervasyon anında fiyat istenir; `pricing` snapshot payload'ını döndürür. `booking` onaylanan fiyatı kendi snapshot'ı olarak saklar; fiyatı kendisi hesaplamaz, başka modülden fiyat kopyalamaz.
  - **Hata davranışı:** Geçersiz dönem/misafir sayısı/para birimi `400`; pasif/bulunmayan oda tipi `404 pricing.room_type_not_found`; varsayılan plan yok `503 pricing.default_rate_plan_required`; fiyat tanımsız `409 pricing.price_not_configured`; kur yok `503 pricing.fx_rate_unavailable`; yetkisiz admin rolü `403 security.forbidden`; hatalar RFC 7807 `application/problem+json` ve `X-Request-Id` taşır, stack trace/iç sınıf adı sızdırmaz.
  - **Gösterim/tahsilat:** Teklif para birimi = tahsilat para birimi; ödeme anında çevrim yok; iade aynı para biriminde. Teklif TRY/EUR/USD dışında para birimi kabul etmez. Public teklif yüzeyi 0006'da yalnız API'dir; misafir arayüzünde fiyat gösterimi 0007'dedir.

- **REQ-5 (Fiyat Gösterimi ve Cross-Spec Uyumu — Q3=A koşullu):** Bu dilimde oda vitrini kartlarında fiyat **gösterilmez**; oda detay sayfası ve kart fiyatı 0006 kapsamı dışıdır. 0002/0003 canlı E2E testleri ve 0005 kart sözleşmesi (kartta fiyat yok) değişmez. Takım Yöneticisi kararı gereği **0007 spec'i, oda detay sayfasıyla birlikte oda vitrini kartlarında fiyat gösterimini kesinlikle kapsayacaktır**; bu, 0007 yazılırken kontrol edilmesi gereken planlama kaydıdır. 0006'da public fiyat UI'si, kart fiyatı, "başlangıç fiyatı" veya fiyat CTA'sı eklenmez.
  - **Admin UI:** Yalnız admin-web fiyat yönetimi ekranı eklenir; 0003 public tasarım sistemi admin-web'e uygulanmaz, yeni UI/i18n kütüphanesi eklenmez.
  - **Katalog/medya:** 0006'da katalog veya medya verisi değiştirilmez; yalnız `catalog.api` read portu tüketilir. Oda tipi adları/admin seçenekleri catalog'dan gelir; fiyat yanıtı catalog alanlarını kopyalamaz.
  - **Frontend kuralı:** 0007'de fiyat UI'si eklenirse `docs/frontend.md` kuralları (TR/EN, yükleme/boş/hata/dolu, `data-testid`, `Intl.NumberFormat`, string ondalık) uygulanır. 0006'da bu UI yoktur.

- **REQ-6 (Testler, Kanıtlar, Doküman ve CI — tüm kararlar):** Fiyat önceliği, sezon/gece hesabı, misafir sayısı, kur/buffer/yuvarlama, snapshot değişmezliği, fallback, varsayılan plan, overlap ve quote geçerliliği birim testleriyle; şema/sorgular Testcontainers IT'leriyle; admin-web Vitest + RTL + MSW ile; admin fiyat akışı ve API seviyesinde public teklif `@pricing` E2E etiketiyle doğrulanmalıdır. TCMB ağı testlerde kullanılmaz; fixture/fake ve `Clock` zorunludur.
  - **Kanıtlar:** `specs/done/evidence/0006/` altına admin fiyat ekranı yükleme/boş/hata/dolu görselleri, bayat kur uyarısı görseli ve test özetleri yazılır. `0001`–`0005` arşivleri SHA-256/`git diff` ile değişmez.
  - **E2E sırası:** 0002 `@empty` → catalog seed → 0002 `@seeded` → 0003 `@seeded` → `@admin` → `@media` → `@pricing` korunur. 0002/0003/0005 canlı testleri değişmez. `@pricing` gerçek admin akışıyla plan/sezon/fiyat/override/ayar oluşturur; public teklifi API isteğiyle doğrular; FRONT_DESK 403 ve geçersiz girdi negatiflerini içerir.
  - **Doküman/CI (developer):** `docs/architecture.md` §3/§5 R3/§7/D15 (fiyat modeli, `pricing → catalog` read kenarı, yukarı yuvarlama, TCMB fallback), `docs/conventions.md` §3 (yuvarlama adımı/yönü) ve §4 (V12+), `docs/frontend.md` (admin fiyat ekranı notu), `.env.example` (TCMB URL/çekim saati, buffer, yuvarlama adımı, quote TTL), `.github/workflows/ci.yml` (TCMB fixture, `@pricing` aşaması), `HotelApiModularityTest` (izinli kenar) ve gerekirse `e2e/README.md` güncellenir. `docs/` değişikliklerini developer yapar; analist dokunmaz.

## 3. Constraints (Kapsam Dışı Dahil)

- **Teknik/İş kısıtları:**
  - **Kapsam:** Değişiklik `hotel-api` (`pricing` modülü, `catalog.api` read tüketimi, public/admin web adaptörleri), `admin-api` (yalnız gateway route/header hijyeni), `admin-web` (TR `/pricing` ekranı ve gezinme), `e2e`, `docs`, `.env.example` ve `.github/workflows/ci.yml` ile sınırlıdır. `web` public uygulamasında 0006'da fiyat UI'si değişmez.
  - **Modül sınırı:** `pricing`, `catalog` read kenarı dışında yaprak modüldür; yalnız `sharedkernel` ve yeni onaylı `catalog` read kenarına bağımlı olabilir; başka iş modülüne bağımlı olamaz. `booking → pricing` ve `reporting → pricing` mevcut izinli kenarlardır; `pricing → booking/payment/media/notification/reporting` yasaktır. `catalog` internal'ına/tablosuna erişim, cross-schema FK ve cross-module JOIN yasaktır. `pricing → catalog` kenarı `HotelApiModularityTest` ve `docs/architecture.md` §5 R3’e işlenmeden kod yazılamaz.
  - **Veritabanı:** Migration `V12`'den başlar; yalnız `pricing` şemasına tablo/indeks/kısıt ekler; V2 init şeması ve `app` DML izinleri korunur. Tüm entity'ler `@Table(schema = "pricing")` taşır. UUIDv7; `timestamptz` UTC; iş günü Europe/Istanbul; konaklama/sezon tarihleri `LocalDate`. Para `NUMERIC(p,2)` ve `BigDecimal`; kur en az 6 ondalık hassasiyetle `NUMERIC` ve `BigDecimal`; `double`/`float` yasak; yuvarlama yalnız sınırda ve açık kuralla.
  - **REST ve güvenlik:** Public yüzey `/api/**` anonim; admin yüzey `/admin-api/**` JWT + rol korumalı; admin-web yalnız admin-api'ye gider. State-changing admin isteklerinde 0004 CSRF/Origin kuralları geçerlidir. Hatalar RFC 7807; `security.forbidden` 403; public/admin yanıtlarında stack trace, iç sınıf adı, PII veya sır bulunmaz.
  - **Zaman ve TCMB:** TCMB çekimi Europe/Istanbul iş günü ~15:30; hafta sonu/tatilde son yayımlanan kur. Teklif hesaplaması TCMB'ye canlı ağ çağrısı yapmaz. Local/CI'da ağ yok; fixture/fake ve `Clock` kullanılır. TCMB URL/çekim saati, buffer, yuvarlama adımı ve quote TTL environment/config üzerinden yönetilir; repoda gerçek sır yoktur.
  - **Performans:** Teklif tek istekte ve depolanan kur/fiyatlarla hesaplanır; TCMB canlı çağrısı, N+1 sorgu veya gece başına ayrı ağ isteği yoktur. Günlük TCMB çekimi tek iştir. Bu dilimde sayısal p95 hedefi tanımlanmaz; ölçüm/optimizasyon ihtiyacı ayrı spec'tir.
  - **Veri:** Public teklif anonimdir; misafir kimliği/PII toplanmaz. Fiyat/override kayıtları iş verisidir; admin DB'sine yazılmaz/aynalanmaz. Admin-web'de DB/sır yoktur.
- **Kapsam dışı (bilinçli olarak yapılmayacaklar):**
  - **Oda detay sayfası (0007)** ve **oda vitrini kartlarında fiyat gösterimi**; 0007'de kart fiyatı zorunlu olarak ele alınacaktır (Q3 koşulu).
  - Rezervasyon/ödeme akışı, ödeme sağlayıcı entegrasyonu, deposit/kısmi ödeme, iade politikası motoru.
  - Promosyon kodları, kuponlar, kampanya yönetimi, sadakat/hediye kartı (Q7=A; ayrı dilim, acil değil).
  - KDV ayrıştırma, vergi/fatura/PDF, e-Fatura/e-Arşiv.
  - Admin fiyat audit'i, fiyat değişiklik geçmişi, onay iş akışı, toplu içe aktarma, takvim grid'i, dinamik/yield fiyatlandırma, OTA/channel manager.
  - Admin kullanıcı/rol yönetimi, katalog yönetimi, medya değişiklikleri, 360 tur, CDN, analitik, çoklu tesis/tenant, grup/çok odalı rezervasyon.
  - `RatePlanChanged`/`PromoCodeRedeemed` olay yayını; 0006'da tüketici yoktur.
  - Public fiyat UI'si, kart fiyatı, "başlangıç fiyatı" ve fiyat CTA'sı; bunlar 0007'dedir.
  - Public quote için rate limiting/anti-abuse; bu dilimde yoktur, operasyonel izleme/ileri spec.
- **Kabul edilen risk/ödünç:**
  - **TCMB bağımlılığı:** Bayat kur veya TCMB kesintisi fiyat doğruluğunu etkileyebilir; son bilinen kur + admin uyarısı kabul edilir, akış bloke edilmez. Hiç kur yoksa yalnız EUR/USD teklifi durur.
  - **Fiyat audit'i yok:** Plan/sezon/fiyat/ayar değişiklikleri için ayrı denetim kaydı yoktur; uygulama logları/request-id ve snapshot'lar kullanılır.
  - **Fiyat geçmişi yok:** Geçmiş fiyat listesi/audit ekranı yoktur; yalnız kur geçmişi ve mevcut değerler görüntülenir. Geçmiş fiyat kanıtı snapshot'lardır.
  - **Tek varsayılan plan:** Public teklif tek varsayılan planı kullanır; misafir plan seçimi 0007'ye kalır. Çoklu plan yönetimi vardır ama public tüketimi tek plandır.
  - **Kart fiyatı yok:** 0006'da vitrin kartlarında fiyat yoktur; 0007'de zorunlu olarak eklenecektir.
  - **Gösterge teklif:** 15 dk sonra fiyat değişebilir; rezervasyon anında yeniden hesaplama ve misafir onayı gerekir.
  - **Public rate limiting yok:** Anonim teklif ucu kötüye kullanıma açıktır; bu dilimde operasyonel izleme kabul edilir.
- **Doküman ve CI Etkisi (Developer):**
  1. **`docs/architecture.md`:** §3 `pricing` veri kavramları; §5 R3 `pricing → catalog` read kenarı; §7/D15 fiyat modeli, misafir sayısı ekseni, yukarı yuvarlama ve TCMB fallback; §8 ilgili yasaklar.
  2. **`docs/conventions.md`:** §3 para/yuvarlama (yukarı yuvarlama, adım, TRY exact); §4 migration V12+ ve yalnız `pricing` şeması.
  3. **`docs/frontend.md`:** Admin fiyat ekranı TR/sade, yükleme/boş/hata/dolu, testid ve 0003 uygulanmaz notu; public fiyat UI'si 0007'ye ertelenir.
  4. **`.env.example`:** TCMB URL/çekim saati/timezone, buffer varsayılanı, yuvarlama adımı, quote TTL; local değerler açıkça işaretli, prod sırları repoda yok.
  5. **`.github/workflows/ci.yml`:** TCMB fixture, `@pricing` aşaması ve 0002/0003/`@admin`/`@media` sırasının korunması.
  6. **`HotelApiModularityTest`:** `pricing → catalog` izinli read kenarı ve migration şema kontrolü yeşil.
  7. **`e2e/README.md`:** `@pricing` çalıştırma notu ve API seviyesinde public teklif doğrulaması; 0002/0003/0005 testleri değişmez.
  8. **Admin-web:** `/pricing` route'u ve gezinme bağlantısı; yalnız admin-api; 0003 tasarım sistemi yok.

## 4. Context

- **Mevcut durum (2026-11-08):** `0005` medya hattı kapandı; admin kimlik/oturum/JWT/JWKS/gateway ve admin-web login `0004`'te hazır. `pricing` modülü boş iskelettir; `pricing` şeması `V2__init_pricing_schema.sql` ile (şema + `app` DML izinleri) hazırdır, tablo yoktur. `hotel_core`'da sıradaki global migration numarası **V12**'dir (V1–V11 dolu). `catalog.api` içinde aktif oda tipi read portu (`RoomTypeLookup`/`ActiveRoomType`) `0005` ile mevcuttur; `media → catalog` read kenarı aynı ihtiyacın örneğidir.
- **İlgili uygulamalar/modüller:**
  - `backend/hotel-api` (`:8081`): `pricing` modülü (domain/application/persistence/web), `catalog.api` read tüketimi, public quote ve admin fiyat yüzeyi.
  - `backend/admin-api` (`:8082`): yalnız gateway pass-through; iş kuralı yok.
  - `admin-web` (`:3001`): TR `/pricing` ekranı; yalnız admin-api; DB/sır yok.
  - `e2e`: `@pricing` aşaması; 0002/0003/0005 ve `@admin` korunur.
- **Onaylı kararlar (2026-11-08):**

| Karar | Seçim | Özet |
|---|---|---|
| Q1 | C | Çoklu tarife planı yönetimi; her planın sezonları ve oda tipi + misafir sayısı fiyatları; public teklif yalnız varsayılan planı kullanır; plan seçimi 0007'de. |
| Q2 | A | `pricing → catalog` read kenarı eklenir; architecture §5 R3 + `HotelApiModularityTest` güncellenir; medya ucu yeniden kullanılmaz. |
| Q3 | A (koşullu) | 0006'da oda kartlarında fiyat yok; 0007'de oda detay sayfasıyla birlikte kart fiyatı kesinlikle kapsanacak. |
| Q4 | A | Teklif gösterge niteliğinde, 15 dk; rezervasyon anında yeniden hesaplama; snapshot booking'de; pricing uzun süreli teklif saklamaz. |
| Q5 | A | Elle fiyat TRY baz fiyatını ezer; elle kur TCMB yerine geçer; öncelik elle fiyat > elle kur > TCMB; EUR/USD kur + buffer ile türetilir. |
| Q6 | A | Yuvarlama her zaman bir üst adımın katına (yukarı); buffer yalnız FX türevi EUR/USD; TRY exact; developer dokümanları günceller. |
| Q7 | A | Promosyon kodları 0006 dışında; ayrı ve acil olmayan dilim. |
| Q8 | A | Admin MVP: plan/varsayılan, sezon, oda tipi + misafir sayısı fiyatları, manuel fiyat/kur, buffer/yuvarlama, mevcut değerler; audit/promo/toplu/takvim yok. |
| — | Misafir sayısı | Misafir sayısı fiyatı **kesinlikle** etkiler; her misafir sayısı için açık fiyat girilir; otomatik formül yok. |

- **Karar yorumları:**
  - **Q5 + misafir sayısı:** Onaylanan “oda tipi + tarih aralığı” manuel fiyat kapsamı, Q1=C plan ekseni ve misafir sayısının fiyatı etkilemesi nedeniyle **plan + oda tipi + misafir sayısı + tarih aralığı** olarak uygulanır. Bu, onaylı ilkenin (elle fiyat baz/sezon fiyatını ezer) zorunlu genişletmesidir; aksi hâlde bir misafir sayısı için yapılan override diğerlerini haksız biçimde ezerdi.
  - **Snapshot sahipliği:** `docs/architecture.md` §3 fiyat anlık görüntüsünü booking'in verisi sayar; brief `Quote`/`PriceSnapshot` kavramlarını pricing tarafında listeler. Çözüm: `pricing` hesaplar ve snapshot payload'ını üretir; **kalıcı snapshot sahibi `booking`'dir**. `pricing` uzun süreli quote saklamaz.
  - **Q6 doküman çelişkisi:** `docs/architecture.md` §7 “en yakın 5” ve `docs/domain.md` §3.6 “en yakın 5'e yukarı” ifadeleri çelişir. Onay Q6=A gereği **yukarı** yuvarlama esastır; developer `docs/architecture.md` §7 ve `docs/conventions.md` §3'ü aynı kurala getirir.
  - **Q3 koşulu:** 0006'da kart fiyatı yoktur; 0007 spec'i yazılırken “oda vitrini kartlarında fiyat gösterimi” maddesi **zorunlu** olarak kontrol edilmelidir. Bu spec'te planlama kaydı olarak tutulur; 0006 AC'si kartta fiyat olmadığını doğrular.

- **Kavramsal veri modeli (teknolojiden bağımsız):**
  - `RatePlan` 1—N `Season`; `RatePlan` 1—N baz gece fiyatı (oda tipi + misafir sayısı); `Season` 1—N sezon gece fiyatı (oda tipi + misafir sayısı).
  - `RatePlan` + oda tipi + misafir sayısı + tarih aralığı 1—N manuel fiyat override'ı.
  - Para birimi + tarih 1—N kur kaydı (TCMB veya manuel).
  - Global fiyat ayarları (buffer, yuvarlama adımı) tek aktif kayıt.
  - `Quote` hesaplanmış sonuçtur; `PriceSnapshot` onaylanmış değişmez kayıttır (booking'de saklanır).
  - Oda tipi referansı `catalog` tarafına aittir; `pricing` yalnız kimlik ve aktiflik doğrulaması için read portu kullanır; cross-schema FK yoktur.

- **API sözleşme notları (öneri — path/method developer tarafından netleştirilebilir; yetki ve davranış bağlayıcıdır):**
  - **Public teklif:** Anonim `GET /api/pricing/quote?roomTypeId=...&checkIn=YYYY-MM-DD&checkOut=YYYY-MM-DD&guests=N&currency=TRY|EUR|USD`. `200` yanıtı `roomTypeId`, `ratePlanCode`, `ratePlanNameTr/En`, `checkIn`, `checkOut`, `guests`, `currency`, `nights[]` (tarih, `seasonCode`, `seasonNameTr/En`, `priceSource`, `baseAmountTry`, `fxRate`, `fxSource`, `fxRateDate`, `fxStale`, `bufferPercent`, `roundingStep`, `amount`), `total`, `quotedAt`, `expiresAt` taşır. Tutarlar string ondalıklıdır. `400/404/409/503` RFC 7807.
  - **Booking portu:** HTTP değil, `pricing.api` üzerinden senkron read portu; rezervasyon anında snapshot payload'ı döndürür. `booking` snapshot'ı saklar; fiyat hesaplamaz.
  - **Admin plan/sezon:** `GET/POST /admin-api/pricing/rate-plans`, `PATCH /admin-api/pricing/rate-plans/{id}`, `PUT /admin-api/pricing/rate-plans/{id}/default`; `GET/POST /admin-api/pricing/rate-plans/{id}/seasons`, `PATCH/DELETE /admin-api/pricing/seasons/{id}`. Tümü `SUPER_ADMIN`/`MANAGER`; gateway üzerinden. Varsayılan plan bütünlüğü eksikse `409 pricing.default_rate_plan_incomplete`.
  - **Admin fiyat/override/ayar:** `GET/PUT /admin-api/pricing/rate-plans/{id}/prices` (baz matris), `DELETE /admin-api/pricing/rate-plans/{id}/prices/{priceRowId}`, `GET/PUT /admin-api/pricing/seasons/{id}/prices` (sezon matris), `DELETE /admin-api/pricing/seasons/{id}/prices/{priceRowId}`, `GET/POST/PATCH/DELETE /admin-api/pricing/manual-prices`, `GET/POST/PATCH/DELETE /admin-api/pricing/manual-fx-rates`, `GET/PUT /admin-api/pricing/settings`, `GET /admin-api/pricing/fx-rates`, `GET /admin-api/pricing/room-types` (aktif oda tipleri). Tümü `SUPER_ADMIN`/`MANAGER`; gateway üzerinden. Fiyat satırı silme `204` döner; satır üst kayda ait değilse veya yoksa `404 pricing.rate_plan_price_not_found` / `pricing.season_price_not_found`.
  - **Hata kodları (asgari):** `pricing.invalid_period`, `pricing.invalid_guests`, `pricing.invalid_currency`, `pricing.invalid_amount`, `pricing.invalid_buffer`, `pricing.invalid_rounding_step`, `pricing.room_type_not_found`, `pricing.price_not_configured`, `pricing.default_rate_plan_required`, `pricing.default_rate_plan_incomplete`, `pricing.rate_plan_not_found`, `pricing.rate_plan_duplicate`, `pricing.rate_plan_in_use`, `pricing.rate_plan_price_not_found`, `pricing.season_not_found`, `pricing.season_price_not_found`, `pricing.season_overlap`, `pricing.manual_price_overlap`, `pricing.manual_fx_duplicate`, `pricing.fx_rate_unavailable`, `security.forbidden`, `system.internal_error`. HTTP eşlemesi `docs/conventions.md` §2.3'e uyar.
  - **Admin testid'leri (asgari):** `admin-pricing-page`, `admin-pricing-loading`, `admin-pricing-empty`, `admin-pricing-error`, `admin-pricing-retry`, `admin-pricing-rate-plans`, `admin-pricing-rate-plan-row`, `admin-pricing-rate-plan-default`, `admin-pricing-seasons`, `admin-pricing-season-row`, `admin-pricing-prices`, `admin-pricing-price-row`, `admin-pricing-price-delete`, `admin-pricing-manual-prices`, `admin-pricing-manual-fx`, `admin-pricing-settings`, `admin-pricing-fx-warning`, `admin-pricing-save`.
  - **Not:** Path/method önerileri uygulama kolaylığı içindir; bağlayıcı olan yetki, doğrulama, hata kodu, para/yuvarlama ve snapshot davranışıdır. Developer gerekçeli sapmayı spec uygulama notunda belirtir.

- **Bağımlı spec'ler ve cross-spec:**
  - `0002`: oda tipleri ve `GET /api/room-types`; kartlarda fiyat yok testi korunur.
  - `0004`: admin JWT, roller, gateway, CSRF; fiyat admin yüzeyi bu temele oturur.
  - `0005`: admin deseni, `catalog.api` read portu, `/admin-api/media` gateway; fiyat ekranı medya ucunu yeniden kullanmaz.
  - `0007` (gelecek): oda detay sayfası + **oda kartlarında fiyat gösterimi zorunlu** (Q3 koşulu).
  - **Sonraki dilimler:** promosyon kodları (Q7=A, acil değil), booking/payment entegrasyonu, KDV/fatura.

- **Varsayımlar (aksi yönetim tarafından değiştirilmedikçe):**
  - Çocuk/yaş indirimi yoktur; tüm misafirler aynı sayılır; ek yatak/ekstra hizmet fiyatı yoktur.
  - RatePlan yalnız fiyat/sezon taşır; iade koşulu global X gün kuralıdır (`docs/domain.md` §3.7); plan bazlı iade 0006'da yoktur.
  - TCMB bayat kuru için maksimum yaş sınırı yoktur; son bilinen kur + uyarı yeterlidir.
  - Maksimum konaklama 30 gecedir; `hotel.pricing.max-stay-nights` ile yapılandırılabilir, varsayılan 30'dur.
  - Buffer yalnız FX çevrimine uygulanır; TRY baz/manuel fiyatına uygulanmaz.
  - KDV dahil tek tutar; ödeme anında çevrim yok; iade aynı para biriminde.
  - Public teklifte misafir plan seçimi yoktur; yalnız varsayılan plan.
  - Oda kartlarında fiyat 0006'da yoktur; 0007'de zorunludur.

## 5. Acceptance Criteria

- **AC-1 (Fiyatlandırma Çekirdeği, Şema ve İş Kuralları — REQ-1, Q1=C, Q2=A, Q5=A, Q6=A):**
  - `db/migration/pricing/V12__*.sql` (ve varsa devamı) yalnız `pricing` şemasına tablo/indeks/kısıt ekler; başka hiçbir şemaya dokunmaz. JPA entity'leri `@Table(schema = "pricing")` taşır; hiçbir katmanda `double`/`float` yoktur; Spring Modulith/ArchUnit ve migration şema testleri yeşildir.
  - UUIDv7 kimlik, UTC `timestamptz`, Europe/Istanbul iş günü ve `LocalDate` konaklama/sezon tarihleri kullanılır; cross-schema FK yoktur; para `NUMERIC(p,2)`, kur en az 6 ondalık `NUMERIC` ve `BigDecimal` taşınır.
  - `RatePlan`: benzersiz kod, zorunlu TR/EN ad, aktiflik ve varsayılan işareti; aynı anda yalnız bir aktif varsayılan plan bulunur. Varsayılan plan pasifleştirilemez; kullanımdaki plan fiziksel silinemez (pasifleştirilir). Testler: ikinci varsayılan reddi, varsayılan pasifleştirme reddi, kullanımdaki plan silme reddi.
  - `Season`: başlangıç ≤ bitiş; aynı plan içinde çakışma yok; boşlukta plan baz fiyatı; her gece tarihle eşlenir, çıkış günü fiyatlanmaz. Testler: sınır tarihler, bitişik sezonlar, çakışan sezon `409`, çok sezonlu konaklama.
  - Baz/sezon fiyatı plan + oda tipi + misafir sayısı ekseninde; misafir sayısı `1..maxGuests`; tutar > 0 ve 2 ondalık; eksik fiyat `409 pricing.price_not_configured`; otomatik kişi başı formül yok. Testler: 1, `maxGuests`, `maxGuests+1`, 0/negatif, eksik kombinasyon.
  - Manuel fiyat plan + oda tipi + misafir sayısı + tarih aralığı; TRY > 0; sezon/baz fiyatını ezer; aynı anahtar için çakışma `409`; silme snapshot'ları etkilemez. Manuel kur EUR/USD + tarih; > 0; TCMB yerine geçer; aynı para birimi + tarih tektir; buffer uygulanır.
  - Ayarlar: buffer `0–100` (varsayılan `2,00`), yuvarlama adımı > 0 (varsayılan `5,00`), yön yukarı; geçersiz değerler `400`. Ayar değişikliği yalnız yeni teklifleri etkiler; snapshot değişmez.
  - Fiyat önceliği iki aşamalı test edilir: elle fiyat > sezon > baz; elle kur > TCMB > son bilinen. Aynı gece için tüm kaynak kombinasyonları; manuel fiyat + manuel kur; manuel fiyat + TCMB; sezon + manuel kur; baz + TCMB.
  - `pricing → catalog` read kenarı yalnız `catalog.api` üzerinden çalışır; `catalog` internal/tablosuna erişim yoktur; `HotelApiModularityTest` izinli kenarı doğrular. Pasif/bulunmayan oda tipi fiyat yönetiminde yeni seçim olarak kullanılamaz; public teklifte `404 pricing.room_type_not_found` döner.
  - Birim testleri (`*Test`) ve Testcontainers IT (`*IT`) şema, kısıt, sorgu ve iş kurallarını kapsar; test adları AC referansı taşır.

- **AC-2 (Admin Fiyat Yönetimi, Roller ve Ekran Durumları — REQ-2, Q1=C, Q2=A, Q8=A):**
  - `admin-web` `/pricing` ekranı TR ve sadedir; `SUPER_ADMIN`/`MANAGER` plan, sezon, baz/sezon fiyatı (oda tipi + misafir sayısı), manuel fiyat/kur, buffer/yuvarlama ve güncel/geçmiş kur yönetimini yapabilir; `FRONT_DESK` tüm fiyat işlemlerinde **403** alır. Arayüzde gizleme güvenlik sayılmaz; admin-web doğrudan `hotel-api`'ye istek atmaz.
  - Varsayılan plan yapma; aktif tüm oda tiplerinin `1..maxGuests` baz fiyatları tamamsa başarılı olur; eksik varsa `409 pricing.default_rate_plan_incomplete` ile reddedilir ve eksik kombinasyonlar listelenir. Aynı anda tek varsayılan plan korunur; istek `expectedDefaultPlanId` taşır ve eşzamanlı iki varsayılan isteğinde yalnız biri başarılıdır; eskimiş önkoşul `409 pricing.rate_plan_in_use` döner. İlk varsayılan için `null` kabul edilir.
  - Sezon çakışması, manuel fiyat çakışması, aynı para birimi + tarih manuel kur tekrarı `409`; negatif/sıfır tutar `pricing.invalid_amount`, sıfır misafir `pricing.invalid_guests`, geçersiz buffer/yuvarlama, geçersiz para birimi `400`; hata gövdeleri RFC 7807 ve stabil kodludur. Başarılı kayıtlar `updatedAt` ile görünür.
  - Ekran **yükleme**, **boş**, **hata** (kullanıcı dostu mesaj + `admin-pricing-retry`) ve **dolu** durumlarını gösterir; teknik hata kodu/stack trace sızdırmaz. Asgari testid seti Context'te listelenmiştir.
  - Güncel TCMB kuru, son güncelleme, kaynak ve bayat kur uyarısı görünür; kur geçmişi tarih/para birimi/kaynak ile listelenir. TCMB yoksa son bilinen kur + uyarı gösterilir; akış bloke edilmez.
  - Plan/sezon/fiyat/override/ayar değişikliğinin yalnız yeni teklifleri etkileyeceği, mevcut snapshot'ları değiştirmeyeceği admin ekranında açıkça belirtilir. Plan pasifleştirme/sezon veya override silme geçmiş snapshot'ları etkilemez.
  - Admin-web Vitest + Testing Library + MSW testleri; E2E `@pricing` akışı (plan → sezon → fiyat → override → ayar → public teklif) ve `FRONT_DESK` 403 negatifi. Audit, promo, toplu içe aktarma, takvim grid'i ve admin kullanıcı yönetimi yoktur.

- **AC-3 (TCMB, Kur, Buffer, Yuvarlama ve Fallback — REQ-3, Q6=A):**
  - Günlük TCMB `today.xml` çekimi iş günleri ~15:30 Europe/Istanbul'da çalışır; hafta sonu/tatilde son yayımlanan kur kullanılır; EUR/USD **satış** kuru geçmişi kaynak/tarih/çekim zamanıyla saklanır. Testler: hafta içi, hafta sonu, tatil, günün kuru henüz yayımlanmamış, bozuk XML, erişilemezlik.
  - EUR/USD hesabı: `TRY gece tutarı × satış kuru × (1 + buffer)` → **bir üst yuvarlama adımının katına** yuvarlanır. Varsayılan buffer `%2,00`, adım `5,00`; TRY fiyatlarına buffer/yuvarlama uygulanmaz. Testler: `%0`, `%2`, `%100`; `0,01`, `4,99`, `5,00`, `5,01`; çok geceli toplam; adım `5` ve farklı pozitif adım.
  - Gece bazlı yuvarlanır; toplam yuvarlanmış gece tutarlarının toplamıdır; toplam yeniden yuvarlanmaz. `BigDecimal`/string taşıma doğrulanır; `double`/`float` yoktur.
  - Manuel kur, o para birimi + tarih için TCMB yerine geçer; buffer manuel kurda da uygulanır; aynı para birimi + tarih için tek kayıt. Manuel kur yoksa TCMB'nin o gün/öncesinde yayımladığı son satış kuru; TCMB erişilemezse son bilinen kur + `fxStale`/admin uyarısı.
  - Hiç kur yoksa TRY teklifi çalışır; EUR/USD teklifi `503 pricing.fx_rate_unavailable` döner. Teklif hesaplaması TCMB'ye canlı ağ çağrısı yapmaz; local/CI'da fixture/fake ve `Clock` kullanılır, gerçek ağ/gerçek bekleme yoktur.
  - Admin ekranında güncel kur, kaynak, tarih ve bayat uyarısı; kur geçmişi görünür. Ayar değişikliği yeni teklifleri etkiler; eski snapshot'ı değiştirmez.

- **AC-4 (Public Fiyat Teklifi, Doğrulama ve Booking Portu — REQ-4, Q1=C, Q2=A, Q4=A, Q5=A):**
  - Anonim public teklif yüzeyi; oda tipi + giriş/çıkış + misafir sayısı + para birimi ile `200` ve gece dökümü döner. Giriş tarihi bugünden önce, çıkış ≤ giriş, misafir `0` veya `> maxGuests`, geçersiz/eksik para birimi ve **31 gece** `400`; 30 gece kabul edilir. Pasif/bulunmayan oda tipi `404 pricing.room_type_not_found`; varsayılan plan yok `503 pricing.default_rate_plan_required`; fiyat eksik `409 pricing.price_not_configured`; kur yok `503 pricing.fx_rate_unavailable`. Hatalar RFC 7807, stack trace yok. Sınır enjekte edilen `Clock` ile test edilir.
  - Yanıt; oda tipi, varsayılan plan kodu/adı, dönem, misafir sayısı, para birimi, gece sayısı, her gece için tarih/sezon/fiyat kaynağı/TRY tutar/kur/kaynak/tarih/bayat bayrağı/buffer/yuvarlama/gece tutarı ve toplamı taşır. Tutarlar string ondalıklıdır; PII yoktur. TRY yanıtında FX alanları yok/`null`; buffer/yuvarlama uygulanmaz.
  - Teklif `quotedAt` + `expiresAt` (15 dk) taşır; gösterge niteliğindedir. Rezervasyon anında fiyat yeniden hesaplanır; eski teklif tutarı körü körüne kullanılamaz; fiyat değiştiyse yeni tutar misafire gösterilmeden rezervasyon oluşturulamaz. `pricing` uzun süreli teklif saklamaz; snapshot booking'de saklanır.
  - `booking → pricing` portu rezervasyon anında snapshot payload'ı döndürür; booking fiyatı kendisi hesaplamaz, başka modülden kopyalamaz. Snapshot; gece dökümü, kaynaklar, kur, buffer, yuvarlama, toplam ve para birimini taşır.
  - Testler: TRY/EUR/USD; tek/çok gece; çok sezonlu konaklama; manuel fiyat/kur; sezon→baz fallback; 15 dk expiry (`Clock`); fiyat değişikliği sonrası yeni teklif; pasif oda tipi; eksik fiyat; varsayılan plan yok; kur yok; sınır misafir sayıları. Public UI 0006'da yoktur; doğrulama API seviyesindedir.
  - Teklif para birimi = tahsilat para birimi; ödeme anında çevrim yok; iade aynı para biriminde. Public teklifte misafir plan seçimi yoktur; yalnız varsayılan plan.

- **AC-5 (Fiyat Snapshot'ı, Değişmezlik ve Yeniden Hesaplama — REQ-1, REQ-4, Q4=A):**
  - `pricing`, rezervasyon anı için `PriceSnapshot` payload'ı üretir; oda tipi, plan kodu, dönem, misafir sayısı, para birimi, gece dökümü, fiyat kaynakları, kur kaynağı/tarihi, buffer, yuvarlama adımı ve toplamı taşır. Payload immutable value object'tir; mutable koleksiyon/referans sızdırmaz; aynı girdi deterministik aynı payload'ı üretir.
  - 0006'da `pricing` snapshot'ı kalıcı saklamaz; kalıcı saklama ve geriye dönük değişmezlik `booking` spec'inin sorumluluğudur. Bu spec'te `booking`'in saklayacağı sözleşme tanımlanır; `pricing` payload'ı üretir, `booking` saklar.
  - Aynı girdi ve sabit `Clock` ile fiyat/kur/ayar değişmeden üretilen iki payload birebir aynıdır; fiyat/kur/ayar değiştiğinde yeni payload farklıdır ve eski payload referansı değişmez. Test: payload oluştur → kaynakları değiştir → eski payload aynı; yeni payload farklı. (Booking'in kalıcı snapshot testi booking spec'inde.)
  - Teklif 15 dk sonra geçersiz kabul edilir; yeni teklif `quotedAt`/`expiresAt` güncellenir; eski teklif tutarı yeni rezervasyonda kullanılmaz. Test: `Clock` ile 15 dk öncesi/sonrası; fiyat değişimi; yeniden hesaplama.
  - Snapshot'ta PII yoktur; yalnız fiyat ve konaklama parametreleri bulunur. KDV dahil tek tutar; ayrıştırma yoktur.

- **AC-6 (Cross-Spec, Testler, Kanıtlar, Doküman ve CI — REQ-5, REQ-6, Q3=A, Q7=A):**
  - 0006'da oda vitrini kartlarında fiyat yoktur; `data-testid^="price"` ve `₺/€/$` kart metni bulunmaz; oda detay sayfası ve public fiyat UI'si yoktur. 0002/0003 canlı E2E testleri ve 0005 kart sözleşmesi değişmez; 0001–0005 arşivleri SHA-256/`git diff` ile değişmez.
  - 0007 planlama kaydı spec'te ve kapanış raporunda açıkça yer alır: “0007 oda detay sayfası spec'i, oda vitrini kartlarında fiyat gösterimini zorunlu olarak içerecektir.” 0007 yazılırken bu madde kontrol edilir.
  - Test kapsamı: birim (`*Test`) + Testcontainers IT (`*IT`) + admin-web Vitest/RTL/MSW + `@pricing` E2E. TCMB fixture/fake ve `Clock`; gerçek ağ yok. E2E sırası 0002 `@empty` → seed → 0002 `@seeded` → 0003 `@seeded` → `@admin` → `@media` → `@pricing`; 0002/0003/0005, `@admin` ve `@media` yeşil kalır.
  - `@pricing` E2E: yetkili admin login → plan/sezon/baz fiyat (oda tipi + misafir sayısı) → varsayılan plan → manuel fiyat/kur → buffer/yuvarlama → public teklif API doğrulaması (TRY/EUR/USD, çok gece, misafir sayısı) → bayat kur uyarısı; `FRONT_DESK` 403; geçersiz tarih/misafir/para birimi/negatif tutar reddi; 0002/0003/0005 testleri değişmez.
  - Kanıtlar `specs/done/evidence/0006/` altına: `AC-2-admin-pricing-loading.png`, `AC-2-admin-pricing-empty.png`, `AC-2-admin-pricing-error.png`, `AC-2-admin-pricing-filled.png`, `AC-3-admin-fx-warning.png`, `AC-4-quote-response.md`, `AC-5-snapshot-immutability.md`, `QA-1-QA-2-fix-summary.md` ve test/CI özetleri. 0001–0005 arşivleri değişmez.
  - Doküman/CI: `docs/architecture.md` §3/§5 R3/§7/D15, `docs/conventions.md` §3/§4, `docs/frontend.md`, `.env.example`, `.github/workflows/ci.yml`, `HotelApiModularityTest`, `e2e/README.md` güncellenir; pipeline (lint + build + test + smoke) yeşildir.
  - Promosyon kodları, oda detay sayfası, booking/payment, KDV/fatura, audit, toplu içe aktarma ve takvim grid'i 0006'da yoktur; kapsam dışı maddeleri ihlal eden değişiklik build/inceleme kapısında reddedilir.

### Revizyon 1 (2026-11-08) — Code Review düzeltmeleri

- **CR-01:** Baz ve sezon fiyat satırları için ayrı `DELETE` endpointleri eklendi; `PUT`
  replace semantiği değişmedi. UI'da satır bazlı `Sil` aksiyonu vardır; input temizleme
  otomatik silme yapmaz. Silme snapshot'ları etkilemez; eksik fiyat davranışı korunur.
- **CR-03:** `internal.application` artık `internal.web` DTO/mapper'ına bağımlı değildir;
  dönüşüm controller + `PricingWebMapper` katmanında yapılır. ArchUnit kuralı eklendi.
- **CR-14:** Teklif en fazla 30 gece; 31 gece `400 pricing.invalid_period`. Sınır
  `hotel.pricing.max-stay-nights` ile yapılandırılabilir, varsayılan 30'dur.

### Revizyon 2 (2026-11-08) — QA düzeltmeleri

- **QA-1:** Fiyat DTO'larındaki `@DecimalMin`/`@Min` kısıtları kaldırıldı; sıfır/negatif
  tutar ve sıfır misafir artık domain doğrulamasından geçerek stabil
  `pricing.invalid_amount` ve `pricing.invalid_guests` kodlarını döner.
  `AdminPricingControllerTest`, `PricingIT`, admin-web Vitest ve `@pricing` E2E bu
  davranışı doğrular.
- **QA-2:** `PUT /admin-api/pricing/rate-plans/{id}/default` artık
  `expectedDefaultPlanId` önkoşulu taşır. İstemci gördüğü varsayılan planı gönderir;
  kilit altında mevcut varsayılan farklıysa `409 pricing.rate_plan_in_use` döner.
  İlk varsayılan için `null` kabul edilir. `PricingIT` eşzamanlı iki isteğin yalnız
  birinin başarılı olduğunu, admin-web ise güncel varsayılan ID'sinin gönderildiğini
  doğrular.

## 6. Definition of Done

- [x] Tüm AC'ler doğrulandı (test ve/veya görsel kanıt)
- [x] Pipeline yeşil (lint + build + test + kritik akış smoke)
- [ ] Kod incelemesi onaylandı (Takım Yöneticisi) — Code Reviewer subagent turları tamamlandı; 2 kapsam/katman itirazı raporlandı, Takım Yöneticisi kapanış onayı bekliyor
- [x] Davranış değiştiyse dokümanlar güncellendi (`docs/architecture.md`, `docs/conventions.md`, `docs/frontend.md`, `.env.example`, CI)
- [ ] Spec `specs/done/` klasörüne taşındı, görsel kanıtlar eklendi — Takım Yöneticisi kapanış onayından sonra taşınacak
- [x] CR-01 baz/sezon fiyatı delete endpointleri ve admin-ui satır silme tamamlandı
- [x] CR-03 `internal.application` → `internal.web` bağımlılığı kaldırıldı; ArchUnit kuralı eklendi
- [x] CR-14 maksimum 30 gece sınırı ve config tamamlandı
- [x] QA-1 sıfır/negatif tutar ve sıfır misafir için stabil pricing hata kodları
- [x] QA-2 eşzamanlı varsayılan plan değişimi için `expectedDefaultPlanId` önkoşulu
- [ ] QA yeniden doğrulaması (QA ajanı) — fix sonrası bekliyor
- [x] SCORECARD dolduruldu
- [x] 0007 planlama kaydı (oda kartlarında fiyat gösterimi zorunlu) kapanış raporunda doğrulandı
- [x] 0001–0005 arşivleri değişmedi (SHA-256/`git diff`)

## 7. SCORECARD

- **Spec revizyon sayısı:** 2 (Rev.1 CR-01/CR-03/CR-14; Rev.2 QA-1/QA-2)
- **Düzeltme turu sayısı:** 4 (ilk Code Review turu + CR-01/CR-03/CR-14 düzeltme turu + CR-19/CR-20/CR-21 takip turu + QA-1/QA-2 turu)
- **Bulgu gerçek/gürültü oranı:** 25 bulgu (23 Code Review + 2 QA); 25 gerçek/uygulandı. İlk turda ertelenen CR-10 (maksimum gece) ve CR-13 (application→web), Rev.1'de sırasıyla CR-14 ve CR-03 olarak uygulandı ve kapatıldı; QA-1 ve QA-2 Rev.2'de kapatıldı; açık bulgu yok.
- **Regresyon sayısı:** 0
- **Kaçan hata (escaped bug):** 0 (bilinen)
