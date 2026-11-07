# Code Review Verification — Spec 0005 (Media) — Round 2

- **Tarih:** 2026-11-07
- **PR:** [#27](https://github.com/altananay/hotel-management-system/pull/27) — `fix/0005-code-review` → `integration`
- **İncelenen head:** `7f8a75b` (`fix(media): harden cleanup fencing and close verification findings [plan 0005]`)
- **Base:** `6f1694a` (`integration`)
- **CI:** Backend, Frontend, E2E ve CI Quality Gate **success** (run `42323375997`); GitGuardian **success**.
- **Yöntem:** Statik kaynak/migration/test/CI incelemesi. Test veya derleme çalıştırılmadı.
- **Önceki doğrulama:** `code-review-2026-11-04-verification.md` (`7d228b0` head'i için tarihsel kayıt)

---

## 1. Sonuç

**Onay önünde engel yok; APPROVE öneriyorum.** CR-1/CR-13'ün in-doubt commit riski kapatıldı, CR-11 ve CR-14 düzeltildi, CR-12 gerçek PostgreSQL + MinIO IT'siyle güçlendirildi, CR-16 spec/doküman yerleşimi düzeltildi. CR-15 private-only düşük risk olarak kısmen kapandı; CR-4/CR-5 ertelemesi gerekçeli ve Takım Yöneticisi onayına bağlı.

| ID | Önceki durum | Bu tur | Kısa sonuç |
|---|---|---|---|
| CR-1 | Yüksek | **Kapandı** | Cleanup-intent + disarm + afterCompletion/retry doğru |
| CR-2 | Orta | **Kapandı** | `withCsrfRetry` tüm state-changing media çağrılarında |
| CR-3 | Orta | **Kapandı** | Media refresh hatasında placeholder |
| CR-4 | Orta | **Ertelendi — kabul** | Ayrı refactor spec'i Takım Yöneticisi onayına sunulmalı |
| CR-5 | Düşük/Orta | **Kısmen — kabul** | CSRF kopyası kaldırıldı; component bölünmesi CR-4 ile |
| CR-6 | Düşük | **Kapandı** | Oda tipi hata/retry + disable |
| CR-7 | Düşük | **Kapandı** | Yayındaki medyada oda tipi kilitli |
| CR-8 | Düşük | **Kapandı** | Expired upload FAILED + private temizlik |
| CR-9 | Düşük | **Kısmen — kabul** | Yerinde güncelleme + obsolete private temizlik |
| CR-10 | Düşük/process | **Kapandı** | AC-6 superseded; DoD açık |
| CR-11 | Düşük | **Kapandı** | `MEDIA_CLEANUP_GRACE` yaml'a bağlandı |
| CR-12 | Orta-Düşük | **Kapandı (küçük not)** | Gerçek IT var; otomatik afterRollback assert'i güçlendirilebilir |
| CR-13 | Orta | **Kapandı** | In-doubt commit'te yayındaki nesne silinmiyor |
| CR-14 | Düşük | **Kapandı (operasyon notu)** | Dead-letter var; metric/runbook takip |
| CR-15 | Düşük | **Kısmen — kabul** | Private-only; transactional servis CR-4 refactor'unda |
| CR-16 | Düşük/process | **Kapandı** | Spec `specs/`e alındı; docs güncellendi |

---

## 2. CR-1 / CR-13 — in-doubt commit doğrulaması

**Dosyalar:**
- `backend/hotel-api/src/main/java/com/hotel/media/internal/application/MediaAdminService.java:218-243`
- `backend/hotel-api/src/main/java/com/hotel/media/internal/application/MediaPublicCleanupService.java:75-112`

Yeni akış:

1. `plannedKeys` üretilir; cleanup-intent kayıtları `enqueueAllInNewTransaction(plannedKeys)` ile S3 copy'den **önce** commit edilir.
2. `afterRollback(() -> publicCleanup.processNow(copied))` yalnızca rollback/unknown commit durumunda çalışır.
3. Copy'ler başarılıysa asset/variant DB kayıtları yapılır; `removeAll(plannedKeys)` aynı publish transaction'ında intent'leri siler.
4. Catch artık yeniden enqueue/process yapmıyor; sadece exception'ı yeniden fırlatıyor. Böylece publish transaction'ı içinde `processNow` çağrılmıyor.
5. `processNow` `@Transactional(propagation = REQUIRES_NEW)` oldu.

In-doubt commit senaryosu:

- DB commit'i **başarılı** olduysa `removeAll` intent'leri silmiştir. Spring commit sırasında `TransactionException` alırsa `afterCompletion(STATUS_UNKNOWN)` tetiklenir; `afterRollback` çalışır, ancak `processNow(copied)` artık intent bulamaz ve **public nesneyi silmez**. DB published + public nesne korunur.
- DB commit'i **başarısız** olduysa intent'ler rollback ile geri gelir; `processNow(copied)` yeni transaction'da nesneleri ve intent'leri siler.
- `processNow` catch içinde çağrılmadığı için outer transaction'ın uncommitted delete'iyle çakışma/self-block riski yok.

Bu, önceki CR-13 bulgusunu doğru biçimde kapatıyor. `MediaIT#shouldCleanupPublicCopiesWhenPublishFailsMidCopy` gerçek PostgreSQL + MinIO üzerinde ikinci copy'de hata enjekte ediyor; rollback sonrası intent'lerin kaldığını, `processNow` ile temizlendiğini ve başarılı publish'te cleanup tablosunun boş kaldığını doğruluyor.

**Küçük not (bloklamaz):** IT, `afterRollback`'in otomatik temizliğini doğrudan assert etmiyor. Test, rollback sonrası `plannedKeys`'i alıp `processNow`'u **manuel** çağırıyor. `afterRollback` hiçbir şey yapmasa bile test geçebilir. İstenirse ilk kopyalanan anahtarın task'ı/nesnesinin manuel çağrıdan **önce** silindiği assert edilebilir. Durable intent + scheduler zaten ana güvence olduğu için bu bir merge engeli değil.

---

## 3. CR-11 — grace config

**Dosyalar:** `backend/hotel-api/src/main/resources/application.yml:52-56`, `.env.example:56-59`

```yaml
cleanup:
  initial-delay: ${MEDIA_CLEANUP_INITIAL_DELAY:30s}
  interval: ${MEDIA_CLEANUP_INTERVAL:60s}
  grace: ${MEDIA_CLEANUP_GRACE:5m}
  max-attempts: ${MEDIA_CLEANUP_MAX_ATTEMPTS:10}
```

`MEDIA_CLEANUP_GRACE` artık gerçekten `hotel.media.cleanup.grace`'e bağlı. `.env.example` ile kod uyumlu. **Kapandı.**

---

## 4. CR-12 — gerçek IT

**Dosya:** `backend/hotel-api/src/test/java/com/hotel/media/MediaIT.java:460-498`

- `@MockitoSpyBean MediaStoragePort storage` ile ikinci `copy` çağrısı `MediaStorageException` fırlatıyor.
- Publish rollback oluyor; cleanup tablosunda intent'lerin kaldığı doğrulanıyor.
- `processNow` ile intent'ler ve public nesneler temizleniyor; asset `publishedAt` null kalıyor.
- Başarılı publish testinde cleanup tablosunun boş kaldığı ayrıca assert ediliyor (`MediaIT.java:251-256`).

Bu, CR-1'in durable-intent davranışını gerçek PostgreSQL + MinIO ile kanıtlıyor. **Kapandı.** (Otomatik `afterRollback` assert'i için yukarıdaki küçük not geçerli.)

---

## 5. CR-14 — dead-letter

**Dosyalar:**
- `backend/hotel-api/src/main/resources/db/migration/media/V11__harden_public_cleanup_retries.sql`
- `MediaPublicCleanupTaskEntity.java`
- `MediaPublicCleanupService.java:88-112`

- `attempts` `INTEGER` oldu; `dead_lettered_at` eklendi.
- `max-attempts` (varsayılan 10) sonrası görev dead-letter işaretleniyor, scheduler sorgusu dead-letter'lı görevleri dışlıyor, ERROR loglanıyor.
- `processNow` dead-letter'lı görevi manuel olarak hâlâ işleyebilir; bu operatör/otomatik olmayan bir kurtarma yolu.

**Operasyon notu (bloklamaz):** Dead-letter, public nesnenin **otomatik** silinmesini durdurur. S3 kalıcı olarak silmeye izin vermiyorsa nesne public kalabilir; görev satırı ve ERROR log operatöre sinyal verir. Metric/alert ve dead-letter görevlerini yeniden deneme yolu (runbook veya küçük admin endpoint) eklenmesi önerilir. Bu, CR-1'in ana invariant'ını bozmaz; S3'ün silmeye izin vermediği durumda sistemin yapabileceği sınırlıdır.

---

## 6. CR-15 — private varyant temizliği

**Dosya:** `backend/hotel-api/src/main/java/com/hotel/media/internal/application/MediaProcessingWorker.java:163-174`

- Üretim setinden çıkan eski varyantların DB kaydı siliniyor, ardından private S3 nesnesi best-effort siliniyor.
- Silme başarısız olursa WARN + "operator cleanup required"; task/retry yok.
- `MediaProcessingWorkerTest#shouldUpdateExistingVariantInPlaceAndRemoveObsoletePrivateObject` aynı ID ile yerinde güncellemeyi ve obsolete private silmeyi doğruluyor.

DB varyant yazımlarının tek transaction'a alınması private-only düşük risk olarak CR-4 refactor'una bırakılabilir. **Kısmen kapandı / kabul.**

---

## 7. CR-4 / CR-5 — erteleme

Geliştiricinin itirazı makul:

- Davranış değiştirmeyen geniş refactor, publish/cleanup güvenliğini bu PR'da yeniden riske atabilir.
- Onaysız spec açmamak Altın Kural 1/4 ile uyumlu; spec açmak Analist/Takım Yöneticisi onayı gerektirir.

**Koşul:** Takım Yöneticisi ayrı bir refactor spec'i (ör. `specs/0007-media-service-refactor.md`) açılmasını onaylamalı. Spec açılmadan CR-4/CR-5 "kapandı" sayılmamalı; bu PR'ın merge'ini engellemez.

---

## 8. CR-16 — spec/doküman

- `specs/done/0005-media.md` → `specs/0005-media.md` taşındı; DoD'de "spec done'a taşındı" maddesi açık bırakıldı.
- `docs/architecture.md:143` cleanup-intent/retry ve yerinde varyant güncellemesini içerecek şekilde güncellendi.

**Kapandı.** Onay sonrası spec `specs/done/`e taşınmalı ve DoD maddesi işaretlenmeli.

---

## 9. Küçük / operasyonel notlar (merge engeli değil)

1. **PR açıklaması ve response dokümanı kısmen eski:** PR body hâlâ 178 unit + 56 IT diyor; response dokümanındaki CR-1 satırı "rollback/commit hatasında REQUIRES_NEW ile yeniden kuyruğa alınır" ifadesini koruyor. Yeni akışta yeniden enqueue yok; CR-13 satırı doğru. Merge öncesi PR body/CR-1 satırı güncellenmeli.
2. **SCORECARD:** `specs/0005-media.md` hâlâ "Düzeltme turu sayısı: 1" ve "CR-1–CR-10" diyor. İkinci tur (CR-11–CR-16) eklenmeli.
3. **Eski doğrulama dosyası:** `code-review-2026-11-04-verification.md` `7d228b0` head'ine aittir ve "CR-13 düzeltilmeli" sonucunu taşır. Tarihsel kayıt olarak kalabilir; bu yeni dosya güncel head'i gösterir.
4. **Dead-letter izleme:** Metric/alert ve operatör retry yolu önerilir (CR-14 notu).
5. **CR-12 test güçlendirmesi:** İlk kopyalanan anahtarın otomatik `afterRollback` ile silindiğini manuel `processNow`'dan önce assert etmek, regresyon güvencesini artırır.

---

## 10. Nihai karar

**APPROVE öneriyorum.** Kod tarafında merge'ü engelleyen bir bulgu kalmadı:

- CR-1 ve CR-13'ün in-doubt commit riski kapatıldı.
- CR-11/CR-14 düzeltildi; CR-12 gerçek IT ile güçlendirildi.
- CR-15 private-only kısmi risk olarak kabul edilebilir; CR-4/CR-5 ertelemesi gerekçeli.
- CR-16 spec/doküman yerleşimini düzeltti.

Onay sonrası yapılması gerekenler: PR body/response CR-1 satırını güncellemek, SCORECARD'a ikinci turu eklemek, `specs/0005-media.md` DoD'deki kod incelemesi maddesini işaretlerken kanıt referansını `code-review-2026-11-07-verification.md` yapmak ve spec'i `specs/done/`e taşımak, CR-4/CR-5 için ayrı refactor spec'ini Takım Yöneticisi onayına sunmak.
