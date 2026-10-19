# Code Review Doğrulama Raporu — Spec 0001 Fix Turu

- **İnceleyen:** Code Reviewer
- **Tarih:** 2026-10-19
- **PR:** [#1](https://github.com/altananay/hotel-management-system/pull/1) — `feature/0001-project-architecture` → `integration`
- **İncelenen head:** `05952bd` (`docs(evidence): note gitguardian credential classification [plan 0001/7]`)
- **Yöntem:** Statik diff incelemesi + GitHub Actions check-run API doğrulaması. Rol gereği test/derleme çalıştırılmadı.

---

## 1. Sonuç özeti

**Kod düzeltmeleri:** CR-1 … CR-14 için uygulanan fixler incelendi; yeni bloklayıcı bir kod hatası bulunmadı. Fix raporundaki eşleme büyük ölçüde doğru.

**Merge/DoD:** İki süreç eksiği kaldı:

1. **Branch protection required approval = 0** ve ruleset merge/rebase’e izin veriyor; `docs/git.md` en az 1 onay ve squash-merge istiyor.
2. **AC-9 kanıt dosyası final HEAD’i göstermiyor:** dosya Run #2 / `633d902` diyor; final HEAD `05952bd` için Run #6 check’leri yeşil.

**SD-2** (spec taşıma + SCORECARD + DoD) doğası gereği Takım Yöneticisi onayından sonra kapanacak.

---

## 2. CR bazında doğrulama

| ID | Sonuç | Doğrulama notu |
|---|---|---|
| CR-1 | **Doğrulandı** | `admin-api` SecurityConfig `anyRequest().denyAll()`; health/scaffold/error permit. `AdminApiSurface...shouldReturn401...` mevcut. |
| CR-2 | **Doğrulandı** | Init SQL’de `REVOKE ALL ON SCHEMA public FROM PUBLIC`; app’e public grant yok; modül DML grant’leri migration’larda. `DatabaseSchemaAndRoleSeparationIT` ve `AdminDatabaseIsolationIT` public/history erişimini reddediyor, modül tablosunda DML’i kanıtlıyor. |
| CR-3 | **Doğrulandı (küçük not)** | `anyRequest().denyAll()` + `/error` + `ProblemErrorController`. `/api/non-existent` 404; bilinmeyen public path 401. Not: `docs/conventions.md` 404 matrisiyle ilişkisi aşağıda VR-4’te. |
| CR-4 | **Doğrulandı** | `ErrorResponseException`, 415/406, `HandlerMethodValidationException`/`BindException` → 400, `AccessDeniedException` → 403; testler her iki app’te mevcut. |
| CR-5 | **Doğrulandı** | `.env.example` yalnız gerçekten okunan değişkenleri içeriyor; Maven/Flyway `.env` okumadığı README’de açık; `application-local.yml` + `spring.config.import` yalnız local profil; `--env-file` ve `minio-init` akışı belgelenmiş. |
| CR-6 | **Doğrulandı** | `DomainErrorCategory` domain-nötr; `sharedkernel` Spring web/http import etmiyor; `room_not_available` 409, `not_found` 404; `docs/conventions` matrisi güncel. |
| CR-7 | **Doğrulandı** | Migrator probe tablosu; app INSERT/SELECT/UPDATE/DELETE; DDL ve history DML reddi; iki DB’de test. |
| CR-8 | **Doğrulandı (küçük not)** | `AdminApiModularityTest` `src/main` altındaki tüm `.java/.yml/.yaml/.properties` dosyalarını literal token ile tarıyor; CI guard `pom.xml` dahil tüm `backend/admin-api` üretim yüzeyini `git grep` ediyor. Unit test POM’u ayrıca taramıyor (VR-3). |
| CR-9 | **Doğrulandı** | ProblemDetailSchema zorunlu alanlar + `.passthrough()`; eksik zarf ve uzantı alanı testleri web + admin-web’de mevcut. |
| CR-10 | **Doğrulandı** | `docs/architecture.md` §4 boş kapalı allowlist; `AdminApiModularityTest` kapalı liste + negatif `adminaudit → adminidentity` testi. |
| CR-11 | **Doğrulandı** | `backend/pom.xml` parent; çocuk POM’lar sadeleşti; CI iki app’te yeşil. |
| CR-12 | **Doğrulandı** | `Object.hasOwn`; prototype kodu testleri mevcut. |
| CR-13 | **Doğrulandı** | Üç package.json’da `@types/node` 24.19.2. |
| CR-14 | **Doğrulandı** | E2E/README alt-kabuk ve ayrı terminal yönergeleri. |

---

## 3. SD / CI durumu

| ID | Durum | Not |
|---|---|---|
| SD-1 | **Koşullu** | Final HEAD `05952bd` için GitHub check-run’ları 4/4 success (`CI Quality Gate`, Backend, Frontend, E2E; run `39173316851`). Ancak `AC-9-ci-run.txt` hâlâ Run #2 / `633d902` diyor; final run’a güncellenmeli (VR-2). |
| SD-2 | **Beklemede** | Spec taşıma + SCORECARD + DoD, Takım Yöneticisi onayından sonra yapılacak. |
| SD-3 | **Doğrulandı** | `AC-2-infra-health.txt` varsayılan 5432 ile tazelenmiş; 5433 yerel override notu ve URL senkronu belgelenmiş. |

**Final CI check-run özeti (head `05952bd`):**
- `CI Quality Gate` — success
- `Backend (hotel-api & admin-api)` — success
- `Frontend (web & admin-web)` — success
- `E2E Smoke (Playwright Chromium)` — success
- `GitGuardian Security Checks` — skipped (test credential classification; required check değil)

---

## 4. Yeni / kalan bulgular

### [VR-1] Branch protection onay ve merge yöntemi kurallarını zorlamıyor — Yüksek (süreç)

**Kanıt:**
- `specs/done/evidence/0001/AC-0-branch-protection.txt:19`:
  `pull_request → ... required_approving_review_count=0`
- GitHub Rulesets API (`protect-release-branches`): `allowed_merge_methods` = `["merge","squash","rebase"]`
- PR #1 review API: `reviews: []` (henüz insan onayı yok).
- `docs/git.md:55`: “En az bir onay: Takım Yöneticisi (insan).”
- `docs/git.md:56`: “Squash-merge ile integration’a birleştirilir.”

**Etki:** PR, insan onayı olmadan ve merge/rebase commit’iyle birleştirilebilir. Bu, `docs/git.md` §5’teki iki bağlayıcı kuralı ve DoD’deki “Kod incelemesi onaylandı” koşulunu zayıflatır.

**Fix (Takım Yöneticisi kararı):**
- Ruleset’te `required_approving_review_count=1` yap; PR’ın Takım Yöneticisi tarafından onaylanmasını zorunlu kıl.
- `allowed_merge_methods` yalnız `["squash"]` olsun.
- PR #1 merge edilmeden önce en az bir insan review/approval kaydı oluşsun.

Not: PR’ı açan hesap aynı zamanda reviewer olamıyorsa, ikinci bir yönetici hesabı/reviewer akışı gerekir. Bu, AP-10 kapsamında yönetici kararıdır; alternatif olarak solo-maintainer istisnası `docs/git.md`’de açıkça tanımlanmalıdır.

---

### [VR-2] AC-9 kanıtı final HEAD’i göstermiyor — Orta (kanıt)

**Kanıt:**
- `AC-9-ci-run.txt:7-16`: Run #2, head `633d902`, run `39098813770`.
- PR #1 body: “Run #5 yeşil”, run `39166287794` (head `d647712`).
- Final HEAD `05952bd` check-run’ları: run `39173316851`, 4/4 success.
- `code-review-fixes-2026-10-18.md` ise “Run #2/#3/#4” diyor.

**Etki:** Aynı PR için üç farklı run referansı var; AC-9’un “required check’in yeşil run kanıtı” final HEAD’i işaret etmiyor. CI gerçekten yeşil; sorun kanıtın güncelliği.

**Fix:** `AC-9-ci-run.txt`’i final run `39173316851` / head `05952bd` ile güncelle; PR body ve fix raporundaki run numarasını aynı kaynağa çek. `AC-9-required-check.png` final run’a ait değilse yenile.

---

### [VR-3] `AdminApiModularityTest` POM’u taramıyor — Düşük

**Kanıt:** `AdminApiModularityTest.java:234-258` yalnız `src/main` altındaki `.java/.yml/.yaml/.properties` dosyalarını tarıyor; `pom.xml` bu kapsamın dışında.

**Etki:** Unit test tek başına POM’daki bir `hotel_core` referansını yakalamaz. CI’daki `Guard admin-api isolation` adımı `pom.xml` dahil tüm üretim yüzeyini `git grep` ile taradığı için mevcut koruma yeterli; yine de testin kapsamı CI adımına bağımlı.

**Fix (opsiyonel):** Teste `pom.xml`’i de ekle veya testi CI guard’a yönlendiren açık bir yorum bırak. Bloklayıcı değil.

---

### [VR-4] Bilinmeyen public path 401 dönüyor — Düşük (semantik)

**Kanıt:** `ApiSurfaceAndErrorHandlingTest.java:135-143` `GET /unknown-public-path` için `401 / security.unauthorized` bekliyor. `docs/conventions.md:40` “404 → system.not_found” diyor; `/api/non-existent` zaten 404.

**Etki:** Public allowlist dışındaki bilinmeyen yollar için 401 seçimi bilinçli default-deny ise kabul edilebilir; ancak genel “kaynak yok → 404” sözleşmesiyle çelişir. İstemciler/health probe’ları yanlış sınıflandırabilir.

**Fix (karar):** Bilinçli ise `docs/conventions.md` veya test javadoc’unda “allowlist dışı = 401” istisnası belgelenmeli; değilse bilinmeyen yollar 404 `system.not_found` dönecek şekilde düzenlenmeli. Bloklayıcı değil.

---

## 5. Karar

- **Kod fixleri:** CR-1 … CR-14 kabul edilebilir; yeni bloklayıcı kod bulgusu yok.
- **Merge:** VR-1 (branch protection onay/squash) kapatılmadan `docs/git.md` §5 ihlali devam eder. VR-2 kanıt güncellemesi merge öncesi yapılmalı.
- **SD-2:** Takım Yöneticisi onayı ve merge sonrası spec/SCORECARD/DoD kapanışı yapılmalı.
- **AP-07:** Developer’ın `docs/conventions.md` ve Flyway property rename açıklamaları kabul edilebilir; geçmiş yazımı değiştirilmeden gerekçe kayda geçmiş durumda.

**İmza ilkesi:** “Bulur, gösterir, önerir; kodu eline almaz.”
