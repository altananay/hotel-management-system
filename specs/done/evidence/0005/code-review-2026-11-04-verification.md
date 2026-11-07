# Code Review Verification — Spec 0005 (Media)

- **Tarih:** 2026-11-04
- **PR:** [#27](https://github.com/altananay/hotel-management-system/pull/27) — `fix/0005-code-review` → `integration`
- **İncelenen head:** `7d228b0` (`fix(media): address independent code review findings [plan 0005]`)
- **Base:** `6f1694a` (`integration`)
- **CI:** Backend, Frontend, E2E ve CI Quality Gate **success**; GitGuardian **success**.
- **Yöntem:** Statik kaynak/migration/test/CI incelemesi. Test veya derleme çalıştırılmadı.
- **Önceki inceleme:** `code-review-2026-11-04.md`
- **Geliştirici yanıtı:** `code-review-2026-11-04-response.md`

---

## 1. Sonuç

CR-1'in ana tasarımı — publish öncesi commit edilen cleanup-intent kayıtları, başarılı publish'te aynı transaction içinde disarm, unpublish/archive'da önce DB commit'i, sonra public nesne silme ve retry scheduler — **önceki yüksek riski anlamlı biçimde kapatıyor**. Ancak publish'in rollback/catch telafi kodu, commit sonucunun belirsiz kaldığı (in-doubt commit) durumda yayındaki public nesneyi silebilir. Bu nedenle CR-1'i tamamen kapatmadan önce CR-13 düzeltilmeli.

CR-2, CR-3, CR-6, CR-7, CR-8 ve CR-10 kapatıldı. CR-9 kısmen kapandı; CR-4/CR-5 ertelendi. Yeni bulgular CR-11–CR-15 olarak listelendi.

| ID | Önceki durum | Bu tur | Kısa sonuç |
|---|---|---|---|
| CR-1 | Yüksek | **Tasarım kabul; küçük düzeltme gerekli** | Cleanup-intent + scheduler doğru yönde; CR-13 kapatılmalı |
| CR-2 | Orta | **Kapandı** | `withCsrfRetry` upload start/complete ve tüm mutation'larda |
| CR-3 | Orta | **Kapandı** | `mediaQuery.isError` → placeholder; refresh testi var |
| CR-4 | Orta | **Ertelendi** | Takip spec'i açılmadan kapanmış sayılmamalı |
| CR-5 | Düşük/Orta | **Kısmen** | CSRF kopyası kaldırıldı; component bölünmesi ertelendi |
| CR-6 | Düşük | **Kapandı** | Hata + retry görünür; atama select'i disable |
| CR-7 | Düşük | **Kapandı** | Yayındaki medyada oda tipi select'i disabled + açıklama |
| CR-8 | Düşük | **Kapandı** | Expired upload `FAILED/upload_expired` + private nesne temizliği |
| CR-9 | Düşük | **Kısmen** | Yerinde güncelleme var; atomiklik/eski private nesne temizliği yok |
| CR-10 | Düşük/process | **Kapandı** | AC-6 superseded; DoD'de kod incelemesi yeniden açık |
| CR-11 | — | **Yeni / Düşük** | `MEDIA_CLEANUP_GRACE` uygulanmıyor |
| CR-12 | — | **Yeni / Orta-Düşük** | CR-1 hata yolu için gerçek IT yok |
| CR-13 | — | **Yeni / Orta** | Rollback/catch telafisi in-doubt commit'te yayındaki nesneyi silebilir |
| CR-14 | — | **Yeni / Düşük** | Cleanup retry'da üst sınır/alert yok |
| CR-15 | — | **Yeni / Düşük** | CR-9'da transaction ve orphan private nesne temizliği eksik |

---

## 2. CR-1 doğrulaması

### 2.1 Yeni akış

**Dosyalar:**
- `backend/hotel-api/src/main/java/com/hotel/media/internal/application/MediaAdminService.java:200-249`
- `backend/hotel-api/src/main/java/com/hotel/media/internal/application/MediaPublicCleanupService.java:44-113`
- `backend/hotel-api/src/main/resources/db/migration/media/V10__create_public_cleanup_tasks.sql:1-15`

Publish akışı:

1. `publish` asset'i kilitler; READY/atama/arşiv kontrollerini yapar.
2. `plannedKeys` her varyant için `public/<asset-id>/<publication-token>/<variant>.<ext>` olarak üretilir.
3. `publicCleanup.enqueueAllInNewTransaction(plannedKeys)` ile cleanup-intent kayıtları **ayrı ve commit edilmiş** bir transaction'da yazılır.
4. S3 `copy` çağrıları yapılır; varyantlara public key yazılır.
5. Asset `publishedAt` ve varyantlar DB'ye kaydedilir.
6. `publicCleanup.removeAll(plannedKeys)` aynı publish transaction'ında intent'leri siler. Commit başarılıysa intent'ler de gitmiş olur; commit başarısızsa intent'ler rollback ile geri gelir.

Unpublish/archive akışı:

1. Varyantların public key'leri okunur, DB'de null'lanır.
2. Cleanup-intent kayıtları aynı DB transaction'ında yazılır.
3. Asset unpublish/archive edilir; DB commit edilir.
4. `afterCommit` içinde public nesneler silinmeye çalışılır; başarısız olursa intent'ler scheduler tarafından 5 dk grace sonrası yeniden denenir.

Scheduler:

- `processPending` her 60 sn'de `created_at < now - grace` olan en fazla 50 görevi işler.
- Başarılı silmede görev silinir; başarısız silmede `attempts` artırılır ve görev kalır.

### 2.2 Hata senaryoları

| Senaryo | Davranış | Değerlendirme |
|---|---|---|
| S3 copy başarısız | Publish transaction'ı rollback; `plannedKeys` intent'leri DB'de kalır | Scheduler temizler; doğru |
| Copy başarılı, DB commit başarısız | Rollback intent'leri geri getirir; public nesneler kalır | Scheduler temizler; doğru |
| Copy sonrası process çöker | Intent'ler copy'den önce commit edilmişti | Scheduler temizler; doğru |
| Publish commit başarılı | `removeAll` intent'leri aynı transaction'da siler | Public nesneler DB ile birlikte kalır; doğru |
| Unpublish/archive commit başarılı | Önce DB, sonra S3 silme; başarısızsa intent kalır | Doğru yön; kısa erişim penceresi kabul edilebilir |
| Unpublish/archive rollback | Intent'ler ve DB değişikliği geri alınır; public nesne kalır | DB hâlâ yayında; tutarlı |
| Cleanup silme başarısız | Görev `attempts++` ile kalır; scheduler retry eder | Doğru; CR-14 üst sınır/alert eksik |
| Commit sonucu belirsiz (in-doubt) | DB commit'i başarılı olabilir; `afterRollback` yine de çalışır | **CR-13: yayındaki nesne silinebilir** |

### 2.3 Neden ana tasarım kabul edilebilir?

- Cleanup-intent kaydı S3 side effect'inden **önce** commit ediliyor; böylece process çökse bile "silinecek public anahtar" bilgisi kalıcı.
- Başarılı publish'te intent silme, DB commit'i ile aynı transaction'da; ayrı bir "disarm" yarışı yok.
- Public key'e rastgele `publicationToken` eklenmesi, eski ve yeni yayınların anahtarlarının çakışmasını engelliyor; scheduler eski bir görevi işlerken yeni yayındaki nesneyi silmiyor.
- Unpublish/archive'da DB'nin önce commit edilmesi, "DB yayında değil ama public nesne var" durumunu sınırlı ve retry edilebilir hale getiriyor. Önceki tasarımda DB yayındayken nesne silinebiliyordu; bu daha kötüydü.

Bu, önceki incelemede istenen "explicit compensation/reconciliation" yaklaşımıdır. Sıfır toleranslı bir outbox/state-machine değildir; kalan riskler CR-12–CR-14'te açık.

### 2.4 CR-13 — rollback/catch telafisi fazladan ve riskli

**Dosya:** `MediaAdminService.java:226-248`

`plannedKeys`, publish sırasında kopyalanacak **tüm** anahtarları içeriyor. Intent'ler copy'den önce commit edildiği için:

- Publish rollback olursa intent'ler zaten DB'de kalır.
- Publish commit olursa `removeAll` onları siler.

Buna rağmen catch ve `afterRollback` şunları yapıyor:

```java
publicCleanup.enqueueAllInNewTransaction(copied);
publicCleanup.processNow(copied);
```

Bu iki adım gereksizdir ve `afterRollback` özelinde zararlıdır:

- DB commit'i **aslında başarılı olduysa** ancak client commit onayını alamadıysa (in-doubt commit), Spring `AbstractPlatformTransactionManager.processCommit` içinde `doCommit` bir `TransactionException` fırlatırsa `triggerAfterCompletion(..., STATUS_UNKNOWN)` çağırır. `STATUS_UNKNOWN != STATUS_COMMITTED` olduğu için `afterRollback` çalışır, `copied` anahtarlarını yeniden kuyruğa alır ve `processNow` ile **DB'de yayında olan public nesneleri siler**. Sonuç: DB published, public URL 404.
- Catch içindeki `enqueueAllInNewTransaction(copied)` de gereksizdir; aynı nedenle commit'in gerçekten başarısız olduğu durumda `plannedKeys` intent'leri zaten yerindedir.

**Öneri:**

1. Catch'ten `enqueueAllInNewTransaction(copied)` ve `processNow(copied)` çağrılarını kaldır; sadece exception'ı yeniden fırlat.
2. `afterRollback` içinde yeniden enqueue etme; yalnızca `processNow(copied)` çağır. `plannedKeys` intent'leri zaten var.
3. `processNow`'u `@Transactional(propagation = REQUIRES_NEW)` yap veya `TransactionTemplate` ile yeni transaction'da çalıştır. Böylece rollback sonrası görev silmeleri commit edilir; in-doubt commit'te ise `removeAll` intent'leri zaten sildiği için `processNow` hiçbir şey bulamaz ve yayındaki nesne silinmez.
4. Catch içinde `processNow` **çağırma**: outer transaction henüz rollback olmadığı için `removeAll`'ın uncommitted delete'iyle çakışabilir; işlem afterCompletion'a bırakılmalı.

Bu düzeltme, "hemen temizle" davranışını korurken in-doubt commit riskini kaldırır.

---

## 3. CR-2, CR-3 ve düşük öncelikli düzeltmeler

### CR-2 — Kapandı

**Dosyalar:** `admin-web/src/lib/csrf-retry.ts:10-26`, `admin-web/src/features/media/upload-media.ts:15-41`, `admin-web/src/features/media/use-media-mutations.ts:20-97`

- Ortak `withCsrfRetry` helper'ı eklendi.
- Upload **start** ve **complete** aynı helper'ı kullanıyor.
- Tüm media mutation'ları (update, reorder, cover, publish, unpublish, retry, archive) aynı politikada.
- `security.csrf_invalid` → token yenile → bir kez retry; diğer hatalar retry edilmiyor.
- `csrf-retry.test.ts` iki davranışı test ediyor.

### CR-3 — Kapandı

**Dosya:** `web/src/features/room-catalog/room-showcase.tsx:98`

```ts
const mediaItems = mediaQuery.isError ? [] : (mediaQuery.data ?? []);
```

- Refresh hatasında cache'teki medya yok sayılıyor; `room-card-placeholder` gösteriliyor.
- `room-showcase.test.tsx` içinde "önce başarılı, sonra 503" senaryosu var.

### CR-6 — Kapandı

**Dosyalar:** `admin-web/src/features/media/media-manager.tsx:145-161, 209-211`, `use-media-query.ts`

- `roomTypesQuery.isError` için görünür hata + retry eklendi.
- Hata/loading sırasında atama select'i disable.
- Test: `shouldShowRoomTypeOptionsErrorAndDisableAssignment_whenOptionsFail`.

### CR-7 — Kapandı

**Dosya:** `media-manager.tsx:289, 313-321`

- `disabled={item.published || roomTypesDisabled}`.
- Yayındaki medyada "önce yayından kaldırın" açıklaması gösteriliyor.
- Test, select'in disabled olduğunu doğruluyor.

### CR-8 — Kapandı

**Dosya:** `backend/hotel-api/src/main/java/com/hotel/media/internal/application/MediaUploadService.java:81-90`

- Expired upload'ta `markFailed(UPLOAD_EXPIRED)`, `save`, `safeDelete` yapılıyor; `noRollbackFor = DomainError` sayesinde FAILED durumu commit ediliyor.
- Test: `shouldRejectExpiredPresignedUpload` status/failureCode ve storage delete'i doğruluyor.

### CR-10 — Kapandı

- `AC-6-code-review.md` "superseded" olarak işaretlendi; eski self-review'ın resmi onay olmadığı açıkça yazıldı.
- `specs/done/0005-media.md:200` kod incelemesi maddesi yeniden açık (`[ ]`).
- SCORECARD'da kaçan hata 3 olarak güncellendi.

---

## 4. CR-4, CR-5 ve CR-9

### CR-4 — Ertelendi, ancak takip spec'i gerekli

`MediaAdminService` 434 satıra ve 8 bağımlılığa çıktı. Query, metadata mutation, publication ve processing command sorumlulukları hâlâ aynı sınıfta. Geliştiricinin "publish/cleanup güvenliğini riske atmamak için ayrı refactor" gerekçesi makul; ancak:

- Ertelenen iş için `specs/` altında ayrı bir refactor spec'i (ör. `specs/0006-media-service-refactor.md`) açılmadan bu borç takipsiz kalır.
- Önerilen ayrım: `MediaAdminQueryService`, `MediaMetadataService`, `MediaPublicationService`, `MediaProcessingCommandService`.

### CR-5 — Kısmen

- `withCsrf` kopyası kaldırıldı; ortak helper'a taşındı (CR-2).
- `MediaManager` hâlâ 441 satır; upload paneli, liste, satır eylemleri ve hata durumları tek component'te. CR-4 ile birlikte ayrı refactor spec'ine bırakılması kabul edilebilir.

### CR-9 — Kısmen

**Dosya:** `MediaProcessingWorker.java:118-159`

- `deleteByMediaAssetId` kaldırıldı; `(variant, format)` anahtarında yerinde güncelleme yapılıyor.
- Deterministik private anahtarlar sayesinde yarım kalan işlem sonraki denemede üzerine yazıyor; duplicate/gap riski azaldı.
- Ancak:
  - Varyant S3 yazımları ve DB kayıtları hâlâ tek transaction değil.
  - Üretim setinden çıkan eski varyantların private S3 nesneleri silinmiyor (orphan private object).
  - `MediaProcessingWorkerTest` yalnızca `deleteByMediaAssetId`'nin çağrılmadığını doğruluyor; mevcut varyantın aynı ID ile güncellendiğini test etmiyor.

Etki private depoyla sınırlı olduğu için CR-9'un "Düşük" kalması makul; ancak "tamamen düzeltildi" demek için yukarıdaki iki nokta kapanmalı.

---

## 5. Yeni bulgular

### [CR-11] `MEDIA_CLEANUP_GRACE` uygulanmıyor — **Düşük**

**Dosya:** `.env.example:58`
**Dosya:** `backend/hotel-api/src/main/resources/application.yml:52-54`

`.env.example` içinde:

```text
MEDIA_CLEANUP_GRACE=5m
```

tanımlı. Ancak `application.yml` yalnızca `initial-delay` ve `interval` eşliyor; `grace` yok. `MediaPublicCleanupService` ise `@Value("${hotel.media.cleanup.grace:5m}")` okuyor. Spring relaxed binding `MEDIA_CLEANUP_GRACE` değişkenini `hotel.media.cleanup.grace` anahtarına çevirmez; bu nedenle ortam değişkeni **etkisizdir** ve her zaman varsayılan 5m kullanılır.

**Etki:** Operatör grace süresini değiştirdiğini sanır; gerçekte değişmez. Şu an varsayılan 5m olduğu için davranış aynı, ancak konfigürasyon yanıltıcı.

**Öneri:** `application.yml` içine ekle:

```yaml
    cleanup:
      initial-delay: ${MEDIA_CLEANUP_INITIAL_DELAY:30s}
      interval: ${MEDIA_CLEANUP_INTERVAL:60s}
      grace: ${MEDIA_CLEANUP_GRACE:5m}
```

Gerekçe: `.env.example` ile kod arasındaki tek gerçek kaynak uyumu; aksi halde dokümante edilen ayar sessizce yok sayılır.

### [CR-12] CR-1 hata yolu için gerçek IT yok — **Orta-Düşük**

**Dosyalar:**
- `backend/hotel-api/src/test/java/com/hotel/media/internal/application/MediaAdminServiceTest.java:235-267`
- `backend/hotel-api/src/test/java/com/hotel/media/internal/application/MediaPublicCleanupServiceTest.java:1-104`
- `backend/hotel-api/src/test/java/com/hotel/media/MediaIT.java:193-196, 335-355`

Mevcut kanıt:

- `MediaAdminServiceTest` `MediaPublicCleanupService`'i mock'luyor; gerçek cleanup tablosu/S3 yok.
- `MediaPublicCleanupServiceTest` repository ve storage'ı mock'luyor; transaction yok.
- `MediaIT` yalnızca cleanup tablosunu `DELETE` ediyor ve republish URL'inin değiştiğini doğruluyor; publish rollback/cleanup failure senaryosu yok.

**Etki:** CR-1'in asıl invariant'ı — "başarısız publish sonrası intent kalır, başarılı publish intent'i siler, scheduler public nesneyi temizler" — gerçek PostgreSQL + MinIO ile kanıtlanmış değil.

**Öneri:** `MediaIT`'e en az bir senaryo ekle:
1. Storage portunu ikinci `copy`'de hata verecek şekilde sarmala (test config / spy).
2. İlk kopyanın public nesnesi ve cleanup intent'inin oluştuğunu doğrula.
3. Rollback sonrası `processNow`/`processPending` ile nesnenin ve intent'in silindiğini doğrula.
4. Başarılı publish'te cleanup tablosunun boş kaldığını doğrula.

Gerekçe: Cleanup-intent tasarımı doğru olsa da, transaction/S3 etkileşimi mock'larla değil gerçek altyapıyla kanıtlanmalı.

### [CR-13] Rollback/catch telafisi in-doubt commit'te riskli — **Orta**

Yukarıda §2.4'te ayrıntılı. Özet: `plannedKeys` intent'leri zaten yeterliyken `afterRollback` içinde `enqueueAllInNewTransaction(copied)` + `processNow(copied)` çalıştırmak, DB commit'i başarılı olduğu halde hata görülen durumda yayındaki nesneyi siler. Catch'teki aynı çağrılar da gereksizdir.

**Öneri:** Catch'ten iki çağrıyı kaldır; `afterRollback` içinde yalnızca `processNow(copied)` bırak ve `processNow`'u `REQUIRES_NEW` yap. Gerekçe: Intent'lerin kendisi rollback telafisidir; yeniden enqueue etmek commit belirsizliğinde yanlış tarafa düşürür.

### [CR-14] Cleanup retry'da üst sınır/alert yok — **Düşük**

**Dosya:** `MediaPublicCleanupService.java:94-106`

`attempts` her başarısız denemede artıyor, ancak:
- Maksimum deneme veya dead-letter yok; S3 kalıcı olarak silmeye izin vermiyorsa görev sonsuza kadar kalır.
- `attempts` alanı `short`; 32.767 denemede taşabilir ve `chk_attempts >= 0` ihlali oluşabilir.
- Görünürlük/alert/metric yok; operasyon ancak log'a bakarak fark eder.

**Öneri:** `attempts` için `int` kullan; `max-attempts` sonrası dead-letter/alert veya en azından yüksek öncelikli log/metric ekle. Gerekçe: S3/DB tutarlılığı için retry şart, ancak sessiz sonsuz retry operasyonel körlük yaratır.

### [CR-15] CR-9'da transaction ve orphan private nesne temizliği eksik — **Düşük**

Yukarıda §4'te ayrıntılı. Varyant güncellemeleri tek transaction değil; üretim setinden çıkan eski varyantların private S3 nesneleri silinmiyor. Etki private depoyla sınırlı; ancak "idempotent" iddiasını tamamlamak için:
- Varyant DB değişikliklerini tek transaction'a al veya
- Eski private anahtarları cleanup task'larına ekle.

### [CR-16] Doküman/spec yerleşimi — **Düşük/process**

- `specs/done/0005-media.md` hâlâ `specs/done/` altında, oysa DoD'de kod incelemesi maddesi açık. Durum satırı bunu kısmen telafi ediyor; yine de "done" klasörü yanıltıcı.
- `docs/architecture.md:143` media hattını anlatıyor ancak yeni cleanup-intent/retry mekanizmasına değinmiyor. D15/D16 spec'te var; mimari dokümana kısa bir cümle eklenmesi "tek gerçek kaynak" disipliniyle uyumlu olur.

---

## 6. Nihai öneri

**CR-1'in ana tasarımı kabul edilebilir; önceki "yüksek" riskin normal hata senaryoları kapanmıştır.** Ancak:

1. **Merge öncesi zorunlu:** CR-13 (rollback/catch telafisini sadeleştir; in-doubt commit'te yayındaki nesneyi silme) ve CR-11 (grace env mapping).
2. **Merge öncesi güçlü öneri:** CR-12 (gerçek PostgreSQL + MinIO IT) — en azından CR-13 düzeltmesiyle birlikte.
3. **Takip:** CR-4/CR-5 için ayrı refactor spec'i; CR-14/CR-15/CR-16 için düşük öncelikli iyileştirme.

CR-13 ve CR-11 düzeltildiğinde, kalan maddeler takip spec'lerine bırakılarak **APPROVE** önerilebilir. Bu haliyle bağımsız onay **koşulludur**; CR-13 kapatılmadan CR-1 tamamen kapanmış sayılmamalıdır.
