# Code Review Düzeltme Raporu — Spec 0001

- **Düzelten:** Developer
- **Tarih:** 2026-10-18 (kanıt ekleri 2026-10-19)
- **Kaynak inceleme:** `specs/done/evidence/0001/code-review-2026-10-18.md` (CR-1…CR-14, SD-1…SD-3)
- **Durum:** Tüm CR bulguları düzeltildi ve testlerle kanıtlandı; SD-1 düzeltildi (PR #1 CI yeşil; Rulesets ile dal koruması + "CI Quality Gate" required check aktif; arayüz ekran görüntüleri eklendi — `AC-0-branch-protection.png`, `AC-9-required-check.png`); SD-2 inceleme onayı sonrası kapanacak.

## 1. Bulgu → Fix → Kanıt Eşlemesi

| ID | Durum | Fix | Kanıt (test/kanıt) |
|---|---|---|---|
| CR-1 | Düzeltildi | `admin-api` SecurityConfig: `anyRequest().denyAll()`; yalnız health + scaffold + `/error` açık | `AdminApiSurfaceAndErrorHandlingTest.shouldReturn401ProblemDetail_whenCallingUnknownAdminPath` |
| CR-2 | Düzeltildi | `infra/postgres/init`: `REVOKE ALL ON SCHEMA public FROM PUBLIC`; yalnız `migrator` USAGE+CREATE; global ve `IN SCHEMA public` default privilege blokları kaldırıldı (her iki DB) | `DatabaseSchemaAndRoleSeparationIT.shouldDenyAppAccessToPublicSchemaAndFlywayHistory…`; `AdminDatabaseIsolationIT.shouldDenyAppAccessToPublicSchemaAndFlywayHistory…`; canlı doğrulama (aşağıda §2) |
| CR-3 | Düzeltildi | `hotel-api` SecurityConfig: açık allowlist (`/actuator/health`, `/api/**`, `/error`) + `anyRequest().denyAll()`; `/error` için RFC 7807 `ProblemErrorController` | `ApiSurfaceAndErrorHandlingTest` (unknown path 401; `/error` dispatch 404 problem+json) |
| CR-4 | Düzeltildi | Her iki `GlobalExceptionHandler`: `ErrorResponseException` statusu korunur; `HandlerMethodValidationException`/`BindException` → 400; `AccessDeniedException` → 403; 415/406 → uygun status; `ErrorResponseException` fallback `system.http_<status>` | `shouldPreserveClientErrorStatuses_when415And406Occur`, `shouldReturn403Forbidden…`, `shouldPreserveResponseStatusExceptionStatus…` (her iki app) |
| CR-5 | Düzeltildi | `.env.example` envanteri temizlendi (kullanılmayan/okunmayanlar çıkarıldı); Maven/Flyway `-D` override olarak README'de belgelendi; `application-local.yml` + `spring.config.import` (yalnız `local` profili); `POSTGRES_PORT` ↔ DB URL senkronu belgelenmiş; compose `--env-file .env` README'de | Canlı doğrulama: kök `.env` ile port/URL override (aşağıda §2); `docker compose --env-file .env config` |
| CR-6 | Düzeltildi | `DomainErrorCategory { VALIDATION, NOT_FOUND, CONFLICT, FORBIDDEN, INTERNAL }` (domain-nötr); eşleme yalnız `com.hotel.web` katmanında; `room_not_available` → 409 | `DomainErrorTest` (kategori); `ApiSurfaceAndErrorHandlingTest.shouldMapDomainErrorCategoriesToHttpStatuses…`; `HotelApiModularityTest.shouldForbidSpringWebAndHttpDependencies_whenInspectingSharedKernel` |
| CR-7 | Düzeltildi | IT'ler migrator ile probe tablosu oluşturur; `app` ile INSERT/SELECT/UPDATE/DELETE; DDL ve history DML reddi — hem `hotel_core` hem `hotel_admin` | `DatabaseSchemaAndRoleSeparationIT`, `AdminDatabaseIsolationIT` (5'er test) |
| CR-8 | Düzeltildi | `src/main` altındaki tüm `.java/.yml/.yaml/.properties` taranır; token literal; CI'a koruma adımı eklendi | `AdminApiModularityTest.shouldNotReferenceBusinessCoreDatabase_whenScanningAllAdminApiMainSources`; CI `Guard admin-api isolation` adımı |
| CR-9 | Düzeltildi | `ProblemDetailSchema` zorunlu alanlar required + bilinçli `.passthrough()`; bozuk zarf → genel mesaj | `shouldFallBackToGenericMessage_whenProblemJsonIsMissingRequiredFields` (web + admin-web); `shouldStillParseProblemJson_whenBackendAddsUnknownExtensionFields` |
| CR-10 | Düzeltildi | `docs/architecture.md` §4 boş kapalı izin listesi; `AdminApiModularityTest` kapalı allowlist + negatif kenar (`adminaudit → adminidentity` yasağı) | `shouldForbidCrossModuleDependencies_whenCheckingClosedAllowlist` |
| CR-11 | Düzeltildi | `backend/pom.xml` parent (packaging=pom): BOM/dependencyManagement + pluginManagement/plugins; çocuk POM'lar sadeleşti | `./mvnw verify` iki app'te yeşil |
| CR-12 | Düzeltildi | `Object.hasOwn(errorMap, code)` (web i18n + admin-web error-messages) | `i18n.test.ts` / `error-messages.test.ts` prototype kodu testleri |
| CR-13 | Düzeltildi | `@types/node` `24.19.2` (web, admin-web, e2e) | lint/build/test yeşil |
| CR-14 | Düzeltildi | `e2e/README.md` alt-kabuk `(cd … && …)` ve ayrı terminal yönergeleri | Doküman; E2E zinciri yeşil |
| SD-1 | Düzeltildi | Push + PR #1; CI yeşil (Run #2/#3/#4, 4/4 job). Dal koruması: Rulesets `protect-release-branches` (active; master+integration+releasable; dal silme ve force push yasak; PR zorunlu; required check `CI Quality Gate`; bypass: 0 aktör). Arayüz kanıtı: `AC-0-branch-protection.png`, `AC-9-required-check.png` | `AC-9-ci-run.txt` (run URL + job tablosu); `AC-0-branch-protection.txt` (Rulesets API doğrulaması; PR #1 mergeable_state=clean) |
| SD-2 | **Beklemede** | Spec taşıma + SCORECARD + DoD kapanışı **inceleme onayından sonra** yapılır (DoD: "Kod incelemesi onaylandı" ve "Pipeline yeşil" koşulları henüz sağlanmadı) | Onay sonrası |
| SD-3 | Düzeltildi | AC-2 kanıtı varsayılan `5432` ile yeniden üretildi; yerel `5433` override'ı ve DB URL senkronu nota işlendi; kanıt yeni iki komutlu başlatma akışıyla tazelendi | `specs/done/evidence/0001/AC-2-infra-health.txt` |

## 2. Canlı Doğrulamalar

- **CR-2 (public lockdown):** `docker compose down -v` + taze init sonrası:
  - `app` ile `CREATE TABLE public.denied_probe` → `ERROR: permission denied for schema public`
  - `app` ile `SELECT … FROM public.flyway_schema_history` → `ERROR: permission denied for schema public`
  - `migrator` ile `public` üzerinde DDL → başarılı
  - `public` ACL (her iki DB): yalnız `pg_database_owner` + `migrator` (PUBLIC yok)
  - `app` ile `catalog.probe` üzerinde INSERT/SELECT/DELETE → başarılı
- **CR-5 (.env mekanizması):**
  - Kök `.env` (`HOTEL_API_PORT=18081`, `HOTEL_CORE_DB_URL=…:5433/…`) ile `--spring.profiles.active=local` → uygulama `18081`'de `UP` (kanıt: env değişkenleri kabukta yoktu)
  - `docker compose --env-file .env -f infra/docker-compose.yml config` → `5433` yayınlanan port doğrulandı
- **CI e2e (compose `--wait` düzeltmesi, `[plan 0001/7]` `633d902`):** `minio-init` tek seferlik job'u "init" profiline alındı; `up --wait` exit 0; bucket init'i `--profile init run --rm minio-init` ile çalışır ve kendi exit koduyla doğrulanır. PR #1 Run #2 tüm job'larla yeşil (bkz. `AC-9-ci-run.txt`).
- **Dal koruması (Rulesets):** `protect-release-branches` active; hedefler master/integration/releasable; deletion + non-fast-forward yasak; PR zorunlu (0 onay); required status check `CI Quality Gate`; bypass listesi boş. PR #1 `mergeable_state=clean`; arayüz kanıtı: `AC-0-branch-protection.png` (ruleset) + `AC-9-required-check.png` (Checks, 4/4 yeşil) — bkz. `AC-0-branch-protection.txt`.
- **E2E:** 4 uygulama ayakta, Playwright smoke `3 passed`; kanıt ekran görüntüleri yenilendi.

## 3. Onay Bekleyen Noktalar (CR yanıtı)

- **AP-07/1** (`docs/conventions.md` `V<n>` değişikliği, `chore(infra)` commit'i): Değişiklik, yöneticinin onayladığı Adım 2 dosya listesinde açıkça yer alıyordu (plan mesajı: "`docs/conventions.md` … `V<n>__<aciklama>.sql` olarak düzeltilecek"); commit mesajının kapsamı eksik kaldı. Geçmiş yazımı (rebase/amend) yasak olduğundan geriye dönük bölme yapılmadı; gerekçe burada kayıt altına alındı.
- **AP-07/2** (Flyway property adları `hotel.flyway.*` → `HOTEL_*`): Değişiklik, `-D`/env override mekanizmasını (CR-5) mümkün kılmak için Adım 6 sırasında yapıldı ve artık `infra/README.md`'de resmi olarak belgelenmiştir.
- **SD-1:** Push + CI koşusu tamamlandı (Run #2/#3/#4 yeşil); dal koruması ve required check aktif (Rulesets API doğrulaması: `AC-0-branch-protection.txt`); arayüz ekran görüntüleri eklendi (`AC-0-branch-protection.png`, `AC-9-required-check.png`).
- **SD-2:** Spec kapanışı (taşıma + SCORECARD + DoD) inceleme onayından sonra yapılacaktır.
