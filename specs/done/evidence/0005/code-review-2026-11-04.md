# Code Review Brief — Spec 0005 (Media)

- **Tarih:** 2026-11-04
- **İncelenen branch:** `integration`
- **Head:** `6f1694a` (`feat(media): add room media management and public showcase [plan 0005]`)
- **Feature branch:** `feature/0005-media` (`846c13c`)
- **PR:** [#26](https://github.com/altananay/hotel-management-system/pull/26) — `feature/0005-media` → `integration` (merged)
- **Yöntem:** Statik kaynak kodu, migration, test/kanıt ve GitHub check-run incelemesi. Test veya derleme çalıştırılmadı.
- **CI:** Backend, Frontend, E2E ve CI Quality Gate **success**; GitGuardian **skipped**.

> Not: `specs/done/evidence/0005/AC-6-code-review.md` daha önce APPROVE vermiş ve spec DoD'nin tamamı işaretlenmiş. Bu dosya bağımsız yeniden inceleme notudur; aşağıdaki bulgular kapanmadan önceki APPROVE ve DoD durumu tekrar değerlendirilmelidir.

---

## 1. Yönetici özeti

Medya dilimi geniş ve çalışan bir akış sunuyor: V8/V9 migration'ları, presigned yükleme, asenkron işleme, WebP/JPEG varyantları, public/private depolama ayrımı, admin ekranı ve vitrin entegrasyonu mevcut. Ancak özellikle **S3 ile DB arasındaki atomik olmayan yayın/yayından kaldırma akışı** ve **admin-web yükleme başlangıcındaki CSRF retry eksikliği** üretim davranışı açısından önemli. Ek olarak web vitrini, medya API hatasında önceki veriyi kullanmaya devam edebiliyor.

| ID | Önem | Konu |
|---|---|---|
| CR-1 | **Yüksek** | Publish/unpublish/archive S3-DB tutarlılığı; external side effect'ler transaction içinde |
| CR-2 | Orta | Admin upload **start** isteğinde CSRF 403 retry yok |
| CR-3 | Orta | Media API hatasında RoomShowcase önceki medyayı göstermeye devam ediyor |
| CR-4 | Orta | `MediaAdminService` SRP sınırlarını aşıyor; query/mutation/publication/processing tek sınıfta |
| CR-5 | Düşük/Orta | `MediaManager` çok büyük; CSRF retry kodu iki kez kopyalanmış |
| CR-6 | Düşük | Room type options hatası admin ekranında görünmüyor |
| CR-7 | Düşük | Yayındaki medyanın oda tipi seçimi UI'da hâlâ değiştirilebilir |
| CR-8 | Düşük | Süresi geçen UPLOADED kaydı FAILED/cleanup'a taşınmıyor |
| CR-9 | Düşük | Processing varyant yenilemesi transaction/cleanup açısından yarım kalabilir |
| CR-10 | Düşük/process | AC-6 APPROVE ve dolu DoD, bu bağımsız bulgularla çelişiyor |

---

## 2. Bulgular

### [CR-1] S3 ve DB işlemleri atomik değil; public/DB invariant'ı bozulabilir — **Yüksek**

**Dosya:** `backend/hotel-api/src/main/java/com/hotel/media/internal/application/MediaAdminService.java:197-268`

**Kanıt:** `publish`, `unpublish` ve `archive` `@Transactional` içinde harici S3 çağrıları yapıyor:

- `publish`: `storage.copy(...)` ile public nesneler oluşturuluyor, ardından `assetRepository.save`/`variantRepository.saveAll` çağrılıyor.
- `unpublish`/`archive`: `removePublicCopies()` public nesneleri DB güncellemesinden **önce** siliyor.

DB transaction commit'i metodun dönüşünden sonra gerçekleşir. Commit başarısız olursa:
- `publish`: public nesneler S3'te kalır, fakat DB'de medya yayında değildir; bilinen public URL doğrudan erişilebilir.
- `unpublish`/`archive`: public nesneler silinir, fakat DB'de medya hâlâ yayında kalır; public API var olmayan görsel URL'leri döndürür.

**Etki:** Public görünürlük ile DB gerçeği ayrışır; yayınlanmamış bir görselin public kopyası erişilebilir kalabilir veya yayındaki kayıt kırık görsel üretir. S3 ve PostgreSQL arasında dağıtık transaction olmadığı için bu yarış kalıcıdır.

**Öneri ve gerekçe:** Publish için DB commit'ini ve S3 kopyalamayı açık bir durum makinesiyle ayır: `PENDING_PUBLISH` kaydı commit et → public kopyaları oluştur → `published` işaretle; rollback/commit hatasında `afterCompletion` veya outbox/reconciliation ile kopyaları temizle. Unpublish/archive için önce DB durumunu commit et, sonra public nesneleri sil; silme başarısızsa retry/reconciliation job'u çalıştır. Gerekçe: Harici depo ve DB tek atomik işlem değildir; public/private invariant'ı yalnız açık telafi mekanizmasıyla korunabilir.

---

### [CR-2] Upload start isteğinde CSRF retry yok — **Orta**

**Dosya:** `admin-web/src/features/media/upload-media.ts:34-47`
**Dosya:** `admin-web/src/features/media/use-media-mutations.ts:12-28`

**Kanıt:** `uploadMedia()` başlangıç isteğini `adminApiRequest` ile tek seferde gönderiyor. `withCsrf()` yalnız `complete` çağrısında kullanılıyor. CSRF token bayatsa start isteği `403 security.csrf_invalid` alır ve kullanıcıya generic upload hatası döner; token yenilenip istek otomatik tekrarlanmaz.

**Etki:** 0004 sözleşmesindeki “403 `security.csrf_invalid` → token yenile + bir kez otomatik tekrar dene” davranışı upload akışının ilk adımında uygulanmıyor. Kullanıcı gereksiz yere tekrar yükleme yapmak zorunda kalıyor.

**Öneri ve gerekçe:** `withCsrf` yardımcısını ortak bir API client helper'ına taşı ve upload start isteğinde de kullan. Gerekçe: Upload başlatma state-changing bir admin isteğidir; CSRF retry politikası tüm state-changing çağrılarda aynı olmalıdır.

---

### [CR-3] Media API hatasında vitrin önceki medyayı göstermeye devam ediyor — **Orta**

**Dosya:** `web/src/features/room-catalog/room-showcase.tsx:93-99`

**Kanıt:** `const mediaItems = mediaQuery.data ?? [];` satırı `mediaQuery.isError` durumunu yok sayıyor. React Query daha önce başarılı veri tutuyorsa, sonraki media isteği hata verse bile `data` korunur ve RoomCard gerçek görseli göstermeye devam eder. Spec, medya servisi hata verdiğinde `room-card-placeholder` gösterilmesini istiyor.

**Etki:** Yayından kaldırılmış veya artık erişilemeyen bir görsel, media API hatası sırasında vitrinde kalabilir; fallback sözleşmesi ihlal edilir.

**Öneri ve gerekçe:** `mediaQuery.isError` olduğunda `mediaItems` boş kabul edilip placeholder'a düşülmeli; gerekirse stale veri temizlenmeli. Bu durum için Vitest/E2E testi eklenmeli. Gerekçe: “Hata → placeholder” koşulu yalnız ilk yüklemede değil, sonraki refresh hatalarında da geçerli olmalıdır.

---

### [CR-4] `MediaAdminService` SRP sınırlarını aşıyor — **Orta**

**Dosya:** `backend/hotel-api/src/main/java/com/hotel/media/internal/application/MediaAdminService.java:32-60,62-283`

**Kanıt:** 397 satırlık sınıf; listeleme, oda tipi doğrulama, alt metin/atama, sıralama, kapak, publish/unpublish, archive, retry ve S3/public kopya yönetimini tek sınıfta topluyor. Yedi bağımlılık var ve `publish`/`unpublish`/`archive` aynı sınıf içinde harici depo ile DB transaction'ını birleştiriyor.

**Etki:** Tek sorumluluk ve değişim izolasyonu zayıf; publish hatalarını veya yarışları test etmek için geniş bir servis fixture'ı gerekiyor. Harici S3 koordinasyonu ile saf DB mutation'ları aynı sınıfta karışıyor.

**Öneri ve gerekçe:** Sınıfı en az `MediaAdminQueryService` (liste/oda tipleri), `MediaMetadataService` (atama/alt metin/sıra/kapak), `MediaPublicationService` (publish/unpublish/archive) ve `MediaProcessingCommandService` (retry) olarak ayır; controller facade üzerinden konuşsun. Gerekçe: SRP, özellikle harici yan etkilerin compensation/outbox ile yönetildiği publish akışını izole eder ve test yüzeyini küçültür.

---

### [CR-5] `MediaManager` çok büyük; CSRF retry kodu tekrarlanmış — **Düşük/Orta**

**Dosya:** `admin-web/src/features/media/media-manager.tsx:1-416`
**Dosya:** `admin-web/src/features/media/upload-media.ts:17-32`
**Dosya:** `admin-web/src/features/media/use-media-mutations.ts:12-28`

**Kanıt:** 416 satırlık tek client component upload formu, liste, satır eylemleri, sıralama, kapak, yayın, arşiv ve hata durumlarını birlikte yönetiyor. `withCsrf` implementasyonu `upload-media.ts` ve `use-media-mutations.ts` içinde neredeyse aynı şekilde iki kez bulunuyor.

**Etki:** UI değişiklikleri geniş bir dosyayı etkiliyor; CSRF retry davranışı iki yerde farklılaşabilir. Testler büyük component üzerinden kuruluyor.

**Öneri ve gerekçe:** `MediaUploadPanel`, `MediaList`, `MediaRow` ve ortak `useCsrfRetry` hook'u/helper'ı ayır. Gerekçe: Tek sorumluluk ve tekrarın kaldırılması, CR-2'deki retry tutarlılığını da doğal olarak sağlar.

---

### [CR-6] Room type options hatası admin ekranında görünmüyor — **Düşük**

**Dosya:** `admin-web/src/features/media/media-manager.tsx:120-122`
**Dosya:** `admin-web/src/features/media/use-media-query.ts:38-46`

**Kanıt:** `roomTypesQuery` yalnız `data ?? []` olarak kullanılıyor; `isError` durumunda select boş kalıyor, kullanıcıya hata veya retry sunulmuyor. Kullanıcı medyayı hiçbir oda tipine atayamaz.

**Etki:** Admin ekranı yükleme/liste hatasını gösterirken oda tipi seçenekleri hatasını sessizce yutuyor; AC-4’teki kullanıcı dostu hata/retry beklentisiyle çelişir.

**Öneri ve gerekçe:** Room type options için loading/error/retry durumunu ayrı göster; hata varsa atama kontrolünü devre dışı bırak. Gerekçe: Atama akışının ön koşulu olan veri kaynağı sessizce boş gösterilmemelidir.

---

### [CR-7] Yayındaki medyanın oda tipi seçimi UI'da değiştirilebilir — **Düşük**

**Dosya:** `admin-web/src/features/media/media-manager.tsx:260-289`

**Kanıt:** `admin-media-room-type` select’i `item.published` olsa da disabled değil. Kullanıcı seçimi değiştirdiğinde backend `409` döner; UI bunu generic `actionError` olarak gösterir.

**Etki:** Kullanıcı, iş kuralını ancak sunucu hatasıyla öğrenir; gereksiz hata gürültüsü oluşur.

**Öneri ve gerekçe:** Yayındaki kayıtta oda tipi select’ini disabled yap ve nedenini kısa metinle belirt; değişiklik için önce unpublish gerektiğini göster. Gerekçe: İş kuralı UI’da görünür olmalı; backend yine tek yetkili kalmalıdır.

---

### [CR-8] Süresi geçen UPLOADED kaydı kalıcı yüklenmiş durumda kalıyor — **Düşük**

**Dosya:** `backend/hotel-api/src/main/java/com/hotel/media/internal/application/MediaUploadService.java:81-87`

**Kanıt:** `complete` içinde upload süresi kontrolü `try` bloğundan önce yapılıyor; `DomainError` fırlatıldığında `markFailed`/`safeDelete` çalışmıyor. Kayıt `UPLOADED` olarak kalıyor.

**Etki:** Admin listesinde sonsuza kadar “Yüklendi” görünen, tamamlanması imkânsız kayıtlar birikebilir; operatör yalnız arşivleyebilir.

**Öneri ve gerekçe:** Expired upload'ı `FAILED`/`upload_expired` durumuna geçir ve private nesneyi best-effort temizle; retry/cleanup davranışını test et. Gerekçe: Yaşam döngüsü her terminal hatada açık bir duruma geçmelidir.

---

### [CR-9] Processing varyant yenilemesi transaction/cleanup açısından yarım kalabilir — **Düşük**

**Dosya:** `backend/hotel-api/src/main/java/com/hotel/media/internal/application/MediaProcessingWorker.java:114-138`

**Kanıt:** `processOnce` önce `variantRepository.deleteByMediaAssetId` çağırıyor, sonra S3'e private varyantları yazıp DB'ye kaydediyor; bu adımlar tek transaction değil. S3 yazımı veya DB kaydı ortasında hata olursa eski DB varyantları silinmiş, yeni private nesnelerin bir kısmı depoda kalmış olabilir.

**Etki:** Private depoda orphan nesneler ve yarım varyant seti oluşabilir; retry yeniden üretse de eski nesneler temizlenmez.

**Öneri ve gerekçe:** Varyant üretimini önce benzersiz geçici anahtarlara yaz, DB transaction'ında referansları değiştir, commit sonrası eski anahtarları temizle; alternatif olarak idempotent reconciliation job'u ekle. Gerekçe: S3 ve DB arasındaki çok adımlı işlem, hata durumunda da tekrar çalıştırılabilir olmalıdır.

---

### [CR-10] Önceki APPROVE ve dolu DoD bu bulgularla çelişiyor — **Düşük/process**

**Dosya:** `specs/done/0005-media.md:196-203` (DoD tümü işaretli)
**Dosya:** `specs/done/evidence/0005/AC-6-code-review.md:13-19`

**Kanıt:** Spec DoD “Kod incelemesi onaylandı” dahil tüm maddeleri işaretli; `AC-6-code-review.md` APPROVE veriyor. Ancak CR-1 gibi public görünürlük/S3-DB tutarlılığı ve CR-2 gibi CSRF retry eksikliği bu incelemede açık.

**Etki:** “Done” statüsü, gerçekten kapanmamış üretim risklerini gizler; sonraki dilimler bu davranışı temel alır.

**Öneri ve gerekçe:** CR-1/CR-2/CR-3 kapatılana kadar spec'i yeniden açık statüye al veya SCORECARD'a açık risk olarak işle; yeni kanıt ve review notu ekle. Gerekçe: APPROVE, yalnız testlerin yeşil olması değil public/DB invariant'larının ve hata yollarının da kanıtlanması anlamına gelmelidir.

---

## 3. Olumlu noktalar

- `media → catalog` yalnız `catalog.api.RoomTypeLookup` üzerinden; cross-schema FK ve internal import yok.
- Presigned PUT doğrudan depoya gidiyor; orijinal private, public kopyalar ayrı anahtarlarla yönetiliyor.
- V8/V9 migration'ları `media` şemasında; sıra/kapak kısmi unique indeksleri ve lease alanı mevcut.
- İşleme hattı EXIF/GPS strip, orientation, WebP/JPEG varyantları ve no-upscale kuralını uyguluyor.
- Public API yalnız READY/published/archived olmayan/aktif oda tipli medyayı döndürüyor; `no-store`.
- Admin media ekranı ve E2E akışı gerçek yükleme ile çalışıyor; 0002/0003 arşiv izolasyonu korunmuş.
- CI Quality Gate ve dört ana job yeşil.

---

## 4. Sonuç

**CR-1 yüksek öncelikli olarak ele alınmalı; CR-2 ve CR-3 aynı turda düzeltilmeli.** CR-4–CR-9 bakım/kalite ve hata yolu iyileştirmeleridir; CR-10 ise spec/evidence statüsünün dürüstçe güncellenmesini gerektirir.

Mevcut haliyle bağımsız **APPROVE** önerilmez. CR-1 için outbox/compensation veya açık bir publish-state makinesi olmadan S3-DB invariant'ı üretimde güvenilir değildir.
