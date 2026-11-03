# Code Review Response — Spec 0005

Tarih: 2026-11-04
İnceleme: `code-review-2026-11-04.md`
Fix branch: `fix/0005-code-review` → `integration` (merge yok)

| ID | Durum | Yapılan / Gerekçe |
|---|---|---|
| CR-1 | Düzeltildi | Publish artık `public/<id>/<publication-token>/...` üretir. S3 kopyalarından önce `media_public_cleanup_tasks` niyet kayıtları commit edilir; başarılı publish transaction'ı bunları siler, rollback'te kalır. Rollback/commit hatasında görevler `REQUIRES_NEW` ile yeniden kuyruğa alınır ve hemen işlenir. Unpublish/archive önce DB commit eder, public nesneler `afterCommit` cleanup ile silinir; başarısız silme 60 sn'lik scheduler retry'a kalır. Scheduler, devam eden publish ile yarışmaması için 5 dk grace uygular. Kalan risk: çökme sonrası public nesne en fazla grace+interval kadar erişilebilir kalabilir; kalıcı orphan oluşmaz. |
| CR-2 | Düzeltildi | `withCsrfRetry` ortak helper'ı eklendi; upload **start**, upload **complete** ve tüm media mutation'ları aynı 403 `security.csrf_invalid` → token yenile → bir kez retry politikasını kullanır. Unit test eklendi. |
| CR-3 | Düzeltildi | `mediaQuery.isError` olduğunda cache'teki medya yok sayılır ve `room-card-placeholder` gösterilir. Refresh hatası testi eklendi. |
| CR-4 | İtiraz / ertelendi | `MediaAdminService` SRP ayrımı davranış değiştirmeyen geniş bir refactor; bu fix PR'ında publish/cleanup güvenliğini riske atmamak için ayrı refactor spec'ine bırakılmasını öneriyorum. |
| CR-5 | Kısmi | CSRF retry kopyası kaldırıldı ve ortak helper'a taşındı. `MediaManager`'ın alt bileşenlere bölünmesi CR-4 ile birlikte ayrı refactor spec'ine bırakıldı. |
| CR-6 | Düzeltildi | Oda tipi seçenekleri hata verdiğinde `admin-media-room-types-error` + retry gösterilir; atama select'i disable edilir. |
| CR-7 | Düzeltildi | Yayındaki medyada oda tipi select'i disabled; “önce yayından kaldırın” açıklaması gösterilir. |
| CR-8 | Düzeltildi | Süresi geçen `UPLOADED` kaydı `FAILED/media.upload_expired` olur ve private nesne best-effort silinir. |
| CR-9 | Düzeltildi | Varyantlar artık silinip yeniden yaratılmıyor; `(variant, format)` anahtarıyla yerinde güncelleniyor. Deterministik private anahtarlar sayesinde yarım kalan işlem sonraki denemede üzerine yazar; DB'de boşluk/duplicate oluşmaz. |
| CR-10 | Düzeltildi | `AC-6-code-review.md` resmî onay olmadığı için “superseded” olarak işaretlendi; bu response ve bağımsız review dosyası eklendi. Spec DoD'deki kod incelemesi onayı yeniden açık bırakıldı. |

## Doğrulama (fix branch)

- Backend: 178 unit + 56 IT (MediaIT 9/9; gerçek PostgreSQL + MinIO; V10 cleanup tablosu dahil)
- Web: 43 Vitest
- Admin-web: 35 Vitest
- Spotless/lint/build: yeşil
- E2E `@media`: fix branch üzerinde yeniden koşuldu (1/1); `@seeded` smoke 2/2
