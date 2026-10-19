# Code Review Düzeltme Raporu — Spec 0001

- **Düzelten:** Developer
- **Tarih:** 2026-10-18 (kanıt ekleri ve doğrulama turu: 2026-10-19)
- **Kaynak incelemeler:** `specs/done/evidence/0001/code-review-2026-10-18.md` (CR-1…CR-14, SD-1…SD-3) ve `code-review-verification-2026-10-19.md` (VR-1…VR-4)
- **Durum:** Tüm CR bulguları düzeltildi. Doğrulama turunda CR-1…CR-14 **doğrulandı**; VR-2/VR-3/VR-4 **kapatıldı**; **VR-1** yönetici kararıyla **(b) solo-maintainer istisnası** olarak çözüldü (`docs/git.md` §5) — ruleset "yalnız squash" ayarı yönetici tarafından yapılacak. SD-2 (spec taşıma + SCORECARD + DoD) inceleme onayı ve merge sonrası kapanacak.

## 1. Bulgu → Fix → Kanıt Eşlemesi

| ID | Durum | Fix | Kanıt (test/kanıt) |
|---|---|---|---|
| CR-1 | Düzeltildi | `admin-api` SecurityConfig: `anyRequest().denyAll()`; yalnız health + scaffold + `/error` açık | `AdminApiSurfaceAndErrorHandlingTest.shouldReturn401ProblemDetail_whenCallingUnknownAdminPath` |
| CR-2 | Düzeltildi | `infra/postgres/init`: `REVOKE ALL ON SCHEMA public FROM PUBLIC`; yalnız `migrator` USAGE+CREATE; global ve `IN SCHEMA public` default privilege blokları kaldırıldı (her iki DB) | `DatabaseSchemaAndRoleSeparationIT.shouldDenyAppAccessToPublicSchemaAndFlywayHistory…`; `AdminDatabaseIsolationIT.shouldDenyAppAccessToPublicSchemaAndFlywayHistory…`; canlı doğrulama (aşağıda §2) |
| CR-3 | Düzeltildi | `hotel-api` SecurityConfig: açık allowlist (`/actuator/health`, `/api/**`, `/error`) + `anyRequest().denyAll()`; `/error` için RFC 7807 `ProblemErrorController` | `ApiSurfaceAndErrorHandlingTest` (unknown path 401; `/error` dispatch 404 problem+json) |
| CR-4 | Düzeltildi | Her iki `GlobalExceptionHandler`: `ErrorResponseException` statusu korunur; `HandlerMethodValidationException`/`BindException` → 400; `AccessDeniedException` → 403; 415/406 → uygun status; `ErrorResponseException` fallback `system.http_<status>` | `shouldPreserveClientErrorStatuses_when415And406Occur`, `shouldReturn403Forbidden…`, `shouldPreserveResponseStatusExceptionStatus…` (her iki app) |
| CR-5 | Düzeltildi | `.env.example` envanteri temizlendi; Maven/Flyway `-D` override README'de; `application-local.yml` + `spring.config.import` (yalnız `local`); `POSTGRES_PORT` ↔ DB URL senkronu; compose `--env-file .env` | Canlı doğrulama (§2); `docker compose --env-file .env config` |
| CR-6 | Düzeltildi | `DomainErrorCategory` (domain-nötr); eşleme yalnız `com.hotel.web`; `room_not_available` → 409 | `DomainErrorTest`; `ApiSurfaceAndErrorHandlingTest`; `HotelApiModularityTest.shouldForbidSpringWebAndHttpDependencies…` |
| CR-7 | Düzeltildi | IT'ler migrator ile probe tablosu oluşturur; `app` ile INSERT/SELECT/UPDATE/DELETE; DDL ve history DML reddi — iki DB | `DatabaseSchemaAndRoleSeparationIT`, `AdminDatabaseIsolationIT` (5'er test) |
| CR-8 | Düzeltildi | `src/main` taraması + CI guard adımı | `AdminApiModularityTest.shouldNotReferenceBusinessCoreDatabase_…`; CI `Guard admin-api isolation` |
| CR-9 | Düzeltildi | `ProblemDetailSchema` zorunlu alanlar + `.passthrough()`; bozuk zarf → genel mesaj | İlgili web/admin-web testleri |
| CR-10 | Düzeltildi | Boş kapalı allowlist + negatif kenar testi | `shouldForbidCrossModuleDependencies_whenCheckingClosedAllowlist` |
| CR-11 | Düzeltildi | `backend/pom.xml` parent; çocuk POM'lar sadeleşti | `./mvnw verify` iki app'te yeşil |
| CR-12 | Düzeltildi | `Object.hasOwn` | `i18n.test.ts` / `error-messages.test.ts` |
| CR-13 | Düzeltildi | `@types/node` 24.19.2 | lint/build/test yeşil |
| CR-14 | Düzeltildi | `e2e/README.md` düzeltmeleri | Doküman; E2E zinciri yeşil |
| SD-1 | Düzeltildi | Push + PR #1; CI yeşil (Run #7 — head `a30b47c`, 4/4 job). Rulesets `protect-release-branches` (active; master+integration+releasable; deletion/force-push yasak; PR zorunlu; required check `CI Quality Gate`). Onay/squash ayarı VR-1 kapsamında yönetici kararında | `AC-9-ci-run.txt` (Run #7); `AC-0-branch-protection.txt`; `AC-0-branch-protection.png`, `AC-9-required-check.png` |
| SD-2 | **Beklemede** | Spec taşıma + SCORECARD + DoD, inceleme onayı + merge sonrası | Onay sonrası |
| SD-3 | Düzeltildi | AC-2 kanıtı varsayılan 5432 ile tazelendi; 5433 override + URL senkronu belgeli | `AC-2-infra-health.txt` |

## 2. Canlı Doğrulamalar

- **CR-2 (public lockdown):** `docker compose down -v` + taze init sonrası:
  - `app` ile `CREATE TABLE public.denied_probe` → `ERROR: permission denied for schema public`
  - `app` ile `SELECT … FROM public.flyway_schema_history` → `ERROR: permission denied for schema public`
  - `migrator` ile `public` üzerinde DDL → başarılı
  - `public` ACL (her iki DB): yalnız `pg_database_owner` + `migrator` (PUBLIC yok)
  - `app` ile `catalog.probe` üzerinde INSERT/SELECT/DELETE → başarılı
- **CR-5 (.env mekanizması):**
  - Kök `.env` (`HOTEL_API_PORT=18081`, `HOTEL_CORE_DB_URL=…:5433/…`) ile `--spring.profiles.active=local` → uygulama `18081`'de `UP`
  - `docker compose --env-file .env -f infra/docker-compose.yml config` → `5433` yayınlanan port doğrulandı
- **CI e2e (compose `--wait` düzeltmesi, `633d902`):** `minio-init` "init" profiline alındı; `up --wait` exit 0; bucket init'i `--profile init run --rm minio-init` ile çalışır ve kendi exit koduyla doğrulanır. PR #1 Run #2–#7 yeşil (güncel: Run #7 — `a30b47c`).
- **Dal koruması (Rulesets):** `protect-release-branches` active; hedefler master/integration/releasable; deletion + non-fast-forward yasak; PR zorunlu; required status check `CI Quality Gate`; bypass listesi boş. PR #1 `mergeable_state=clean` (GitGuardian `skipped`). Arayüz: `AC-0-branch-protection.png` + `AC-9-required-check.png`.
- **E2E:** 4 uygulama ayakta, Playwright smoke `3 passed`; kanıt ekran görüntüleri yenilendi.

## 3. Doğrulama Turu (2026-10-19) — Code Reviewer

Kaynak: `specs/done/evidence/0001/code-review-verification-2026-10-19.md` (incelenen head: `05952bd`; CR-1…CR-14 **doğrulandı**).

| ID | Durum | Fix / Aksiyon | Kanıt |
|---|---|---|---|
| VR-1 | **Çözüldü (yönetici kararı: b)** | Solo-maintainer istisnası seçildi; `docs/git.md` §5'e işlendi (required check + Code Reviewer doğrulama raporu + yalnız squash-merge). Ruleset'te "yalnız squash" ayarı yönetici tarafından yapılacak | `docs/git.md` §5; `AC-0-branch-protection.txt` |
| VR-2 | Düzeltildi | AC-9 kanıtı güncel koda (`a30b47c`) ve Run #7'ye çekildi; PR body + fix raporu senkronlandı; Checks ekran görüntüsü final head ile yenilendi | `AC-9-ci-run.txt`; `AC-9-required-check.png` |
| VR-3 | Düzeltildi | `AdminApiModularityTest` üretim yüzeyi taramasına `pom.xml` eklendi (CI guard kapsamıyla eşitlendi) | `AdminApiModularityTest.shouldNotReferenceBusinessCoreDatabase_…` (6/6 yeşil); Run #7 |
| VR-4 | Düzeltildi (doküman) | Bilinçli default-deny davranışı (`allowlist dışı → 401`) `docs/conventions.md` durum matrisine işlendi; `ApiSurfaceAndErrorHandlingTest` testine javadoc eklendi | `docs/conventions.md` §2.3; Run #7 |

## 4. Onay Bekleyen Noktalar (CR yanıtı)

- **AP-07/1** (`docs/conventions.md` `V<n>` değişikliği, `chore(infra)` commit'i): Değişiklik, yöneticinin onayladığı Adım 2 dosya listesinde açıkça yer alıyordu; commit mesajının kapsamı eksik kaldı. Geçmiş yazımı yasak olduğundan geriye dönük bölme yapılmadı; gerekçe kayıt altına alındı.
- **AP-07/2** (Flyway property adları `hotel.flyway.*` → `HOTEL_*`): Değişiklik, `-D`/env override mekanizmasını (CR-5) mümkün kılmak için Adım 6'da yapıldı ve `infra/README.md`'de belgelendi.
- **VR-1:** Yukarıda §3; yönetici kararı bekleniyor.
- **SD-2:** Spec kapanışı (taşıma + SCORECARD + DoD) inceleme onayı ve merge sonrası yapılacaktır.
