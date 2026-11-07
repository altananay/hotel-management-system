# 0005 — QA Düzeltmeleri Bağımsız Kod İncelemesi

- **Tarih:** 2026-11-07
- **Kapsam:** `integration..fix/0005-qa-findings` — QA-01…QA-07, R-01, R-02 düzeltmeleri
- **İncelenen anlık görüntü:** `7000b1c` (5 commit) + çalışma ağacındaki spec/kanıt değişiklikleri
- **İnceleme biçimi:** Salt-okunur; hiçbir dosya değiştirilmedi, commit atılmadı.
- **Yöntem:** `git diff integration...`, `git status`, statik kaynak/migration/test/CI incelemesi, `git diff --check`, arşiv manifesti (`129/129 UNCHANGED`). Tam test suite'leri yeniden çalıştırılmadı; test iddiaları repo'daki kanıt dosyaları ve statik incelemeyle doğrulandı.

## 1. Karar

**APPROVE WITH COMMENTS**

Kodda merge blocker oluşturan bir hata görülmedi. QA-01, QA-02, QA-03 ve QA-04 işlevsel olarak düzeltilmiş; QA-06'nın ana drift'i kapatılmış; R-01 15 saniyelik media timeout'u ile koşullu kapatılmıştır. Ancak QA-07'nin kabul testi tam kapanmamış, QA-05 ve R-02'nin süreç/operasyon kanıtları eksikti. Bu nedenle spec doğrudan "Done" yapılmadan önce F-01, F-04 ve F-05'in kapatılması veya Takım Yöneticisi tarafından açık risk kabulü verilmesi gerekiyordu.

## 2. Bulgular

| ID | Şiddet | Dosya:satır | Bulgu | Kanıt | Öneri |
|---|---|---|---|---|---|
| **F-01** | Düşük (sözleşme) | `MediaPublicQueryService.java:55-68`; `GlobalExceptionHandler.java:161-165` | QA-07'de `DataAccessException` yalnız metot içinde yakalanıyordu. `@Transactional` proxy'si transaction'ı metottan önce başlatıp sonra commit ediyor; transaction başlangıç/commit hataları (`CannotCreateTransactionException`, `TransactionSystemException`) catch dışında kalıp generic handler ile **500**'e düşebiliyordu. | Servis testleri repository'yi mock'luyor ve metodu doğrudan çağırıyor; controller testi servisi mock'luyor. Transaction sınırını veya gerçek DB kesintisini sınayan bir test yok. QA-07 kabul testi "DB erişilemezken 503" diyor. | Public media sınırında transaction hatalarını `media.service_unavailable` **503**'e eşleyin; en az bir transaction-boundary/DB-outage testi ekleyin. |
| **F-02** | Düşük | `specs/0005-media.md:151` | "Mevcut durum" bölümü hâlâ `media` modülünün boş iskelet olduğunu ve sıradaki migration'ın V8 olduğunu söylüyordu. Bu, aynı spec'in §2.2/AC-1 bölümündeki V8–V11 ve uygulanmış modül tanımıyla çelişiyordu. | QA-06'nın ana maddeleri düzeltilmiş olsa da bu satır güncellenmemişti. | Satırı güncel duruma göre yeniden yazın veya "spec yazımı sırasındaki durum" olarak açıkça tarihsel etiketleyin. |
| **F-03** | Düşük (süreç) | `specs/0005-media.md:204` | Definition of Done'da kod incelemesi `[x]` işaretli, ancak referans verilen `evidence/0005/qa-fix-code-review-<tarih>.md` dosyası mevcut değildi; yol `<tarih>` placeholder'ı içeriyordu. | `specs/done/evidence/0005/` altında bu isimde dosya yoktu. | Bu raporu gerçek bir dosya olarak ekleyip DoD'daki bağlantıyı güncelleyin. |
| **F-04** | Düşük (süreç / QA-05) | `AC-6-qa-fixes-verification.md:3-8`, `:122-124` | QA düzeltme kanıtı yalnız `Base: 179d3d0` diyordu; düzeltme branch'inin gerçek head'i ve CI run/PR bilgisi yoktu. CI bölümü "PR açıldıktan sonra doldurulacaktır" diyordu. | QA-05 kabul testi "CI URL'si ve commit hash'i doğrudur" ve "tüm kanıtlar son head ile eşleşir" diyor. | Kanıt dosyasına head, PR URL'si ve CI run sonuçlarını ekleyin. |
| **F-05** | Düşük (operasyonel / R-02) | `docs/runbooks/media-public-cleanup.md:12-26`; `MediaPublicCleanupService.java:50-51` | Dead-letter alarm ifadesi `media_public_cleanup_dead_lettered_total > 0` idi. Bu bir Micrometer **Counter**'dır; bir kez arttıktan sonra process ömrü boyunca `> 0` kalır ve alarm kalıcı olarak açık kalabilir. Ayrıca canlı alarm kuralı ve runbook tatbikat kaydı yoktu. | Sayaç `deadLetterCounter` olarak tanımlı; runbook'ta "5 dakika boyunca sürerse" denmesine rağmen ifade "süreklilik" değil "hiç sıfırlanmama" ölçüyordu. Tatbikat/manuel retry kanıtı yoktu. | Yeni dead-letter olayları için `increase(media_public_cleanup_dead_lettered_total[5m]) > 0` kullanın. Alertmanager kuralını ve manuel retry tatbikatını tarih/operatör/görev ID'siyle kaydedin. |

### F-01 hakkında ek not

Bu bulgu, QA-07'nin ana davranışını çürütmüyor: Sorgu sırasında oluşan `DataAccessException` doğru biçimde `media.service_unavailable` → **503** oluyordu ve bunun için unit/controller testleri vardı. Sorun, transaction sınırının bu catch'in dışında kalmasıydı. Gerçek bir DB kesintisinde hatanın hangi katmanda oluştuğuna göre 503 yerine 500 görülebilirdi. Bu nedenle QA-07 "tam fixed" değil, **partial** değerlendirildi.

## 3. QA Bulguları Bazında Doğrulama (inceleme anı)

| Bulgu | Durum | Değerlendirme |
|---|---|---|
| **QA-01** — Atanmamış mevcut medyada cover 404 | **Fixed** | `findRoomTypeIdsById` boş listeyi "kayıt yok", `[null]`'ı "kayıt var ama oda tipi yok" olarak ayırıyor; `cover()` bu durumda **409 `media.not_published`** üretiyor. Unit + gerçek PostgreSQL IT'si 409/404 ayrımını doğruluyor. |
| **QA-02** — Pasif oda tipinde alt metin kaydı 404 | **Fixed** | `update()` aktif oda tipi kontrolünü yalnız atama gerçekten değişiyorsa yapıyor; admin-web alt metin kaydında `roomTypeId` göndermiyor; pasif atama disabled seçenekle gösteriliyor. Unit + UI + IT kapsamı mevcut. |
| **QA-03** — Yükleme hata yollarında liste yenilenmiyor | **Fixed** | `refresh()` artık `finally` içinde; complete-422 → FAILED + retry ve PUT-503 → UPLOADED + archive akışları Vitest/MSW ile doğrulanıyor. |
| **QA-04** — E2E alt metni doğrulamıyor; fixture türü yanlış | **Fixed** | E2E TR/EN alt metinleri admin UI'dan kaydediyor, public `alt` ve public API `altTextTr`/`altTextEn` alanlarını doğruluyor; fixture gerçek WebP. |
| **QA-05** — Kanıt seti eski head/test sayılarıyla uyumsuz | **Partial** | Baseline ve arşiv manifesti düzeltilmişti; fix branch head'i ve CI run'ı eksikti (F-04). |
| **QA-06** — Spec/implementation drift (V9–V11, hata kodları) | **Fixed** (F-02 rezidüel) | §2.2 V8–V11 ve §2.1 hata kodları hizalanmıştı; §4'teki eski "boş iskelet / sıradaki V8" cümlesi kalmıştı (F-02). |
| **QA-07** — Public `/api/media` için 503 sözleşmesi | **Partial** | `DataAccessException` → 503 doğruydu; transaction başlangıç/commit hataları catch dışındaydı (F-01). |
| **R-01** — 3 sn gateway timeout / senkron medya I/O | **Fixed (koşullu)** | `/admin-api/media/**` için 15 sn media timeout tanımlı ve test edilmiş. **Sınırlama:** Gerçek S3 üzerinde p95 ölçümü yok; 15 sn güvenli üst sınır varsayımıdır ve üretim ölçümü sonrası yeniden değerlendirilmelidir. |
| **R-02** — Dead-letter sonrası public nesne erişilebilir kalabilir | **Partial** | Runbook'ta alarm ifadeleri, manuel retry, haftalık orphan taraması ve kapanış kontrolü vardı; canlı alarm kuralı ve tatbikat kaydı yoktu; dead-letter alarm ifadesi sayaç nedeniyle kalıcı alarm üretebilirdi (F-05). |

## 4. Rezidüel Riskler

1. **Gerçek S3 p95 ölçümü yok (R-01).** 15 sn media timeout'u yerel MinIO ve kod incelemesiyle sınırlıdır. Gerçek S3'te 15 MB `complete` okuması, altı varyant kopyalama/silme ve soğuk başlangıç gecikmeleri 15 sn'yi aşarsa admin 504 görürken hotel-api işlemi arka planda tamamlanabilir. Üretimde p95/p99 ölçülüp timeout ve gerekirse asenkron publish/cleanup stratejisi yeniden değerlendirilmelidir.
2. **QA-07 transaction sınırı.** DB kesintisinin transaction başlangıcında veya commit'inde oluşması hâlinde public endpoint 500 dönebilir; bu, operasyonel izleme ve retry sözleşmesini zayıflatır. (İnceleme sonrası F-01 ile kapatıldı.)
3. **R-02 dead-letter görünürlüğü.** Dead-letter edilen public nesne manuel temizliğe kadar doğrudan URL ile erişilebilir kalabilir. Canlı alarm ve tatbikat yoksa operatör geç haberdar olabilir; alarm ifadesi sayaç üzerinden kurulursa kalıcı alarm gürültüsü oluşabilir. (İnceleme sonrası F-05 ile kapatıldı.)
4. **QA-05/CI izlenebilirliği.** Fix branch'inin head'i ve CI sonucu kanıtlanmadan spec'in "kanıtlı Done" iddiası tam değildir. (İnceleme sonrası F-04 ile kapatıldı.)
5. **Doküman/kanıt commit'leri.** `specs/0005-qa-findings.md`, `AC-4-admin-media-states.md`, `AC-6-integration-179d3d0.md` ve `AC-6-qa-fixes-verification.md` inceleme anında untracked; `specs/0005-media.md` ve runbook değişiklikleri uncommitted'dı. PR'a eklenmeden merge edilmemelidir.
6. **API dışı veri bozulması (düşük).** `cover()` yayınlanmış ama atamasız bir satır için açık `roomTypeId != null` kontrolü yapmıyor; normal API akışında `publish()` atamayı zorunlu kıldığı için bu durum erişilebilir değil. Yalnız manuel DB müdahalesiyle oluşabilecek teorik bir tutarsızlıktır; bulgu olarak sayılmadı.

## 5. İnceleme Sonrası Kapatma (F-01…F-05)

| Bulgu | Kapatma | Kanıt |
|---|---|---|
| **F-01** | `MediaPublicQueryService` transaction sınırını `TransactionTemplate` içine aldı; `DataAccessException` ve `TransactionException` (`CannotCreateTransactionException`, `TransactionSystemException`) birlikte `media.service_unavailable` → 503'e eşleniyor. | `MediaPublicQueryServiceTest.shouldMapTransactionStartFailureToServiceUnavailable`, `shouldMapTransactionCommitFailureToServiceUnavailable` |
| **F-02** | `specs/0005-media.md` §4 "Context" bölümü tarihsel olarak etiketlendi; V8–V11 ve uygulanmış modül durumu ile hizalandı. | `specs/0005-media.md` §4 |
| **F-03** | Bu dosya eklendi; DoD bağlantısı gerçek dosya adına güncellendi. | `specs/0005-media.md` §6 |
| **F-04** | `AC-6-qa-fixes-verification.md` fix branch head'i, PR #28 ve CI run bilgisiyle güncellendi. | `AC-6-qa-fixes-verification.md` §8 |
| **F-05** | Dead-letter alarmı `increase(media_public_cleanup_dead_lettered_total[5m]) > 0` oldu; örnek Alertmanager kuralı ve tatbikat kayıt tablosu eklendi. | `docs/runbooks/media-public-cleanup.md` §0, §5.1 |

## 6. Sonuç

Düzeltmelerin ana işlevsel hedefleri karşılanmıştır: QA-01/02/03/04 için kod ve regresyon testleri anlamlı; QA-06'nın ana spec drift'i kapatılmış; R-01 için media'ya özel timeout doğru yere konmuş; R-02 runbook'u operasyonel olarak iyileştirilmiştir. Mimari sınırlar korunmuş (`admin-api` ince gateway, iş kuralı yok; `media → catalog` yalnız public port), hata zarfı ve mevcut test düzeni bozulmamıştır.

İnceleme sonrası F-01, F-02, F-03, F-04 ve F-05 kapatılmıştır; merge için kod bloğu yoktur.
