# Code Review Düzeltme Raporu — Spec 0001

- **Düzelten:** Developer
- **Tarih:** 2026-10-18 (kapanış: 2026-10-19)
- **Kaynak incelemeler:** `specs/done/evidence/0001/code-review-2026-10-18.md` (CR-1…CR-14, SD-1…SD-3) ve `code-review-verification-2026-10-19.md` (VR-1…VR-4)
- **Durum:** Tüm CR bulguları düzeltildi ve **doğrulandı** (CR-1…CR-14). VR-1/VR-3/VR-4 **kapandı**; VR-2 kanıt senkronu **tamamlandı**. SD-1 **kapandı** (Run #9 — `23cf0a6`). SD-2 **tamamlandı** (spec `specs/done/`e taşındı; SCORECARD ve DoD dolduruldu). Merge: Takım Yöneticisi (squash-merge; solo-maintainer istisnası — `docs/git.md` §5).

## 1. Bulgu → Fix → Kanıt Eşlemesi

| ID | Durum | Fix | Kanıt (test/kanıt) |
|---|---|---|---|
| CR-1 | Düzeltildi | `admin-api` SecurityConfig: `anyRequest().denyAll()`; yalnız health + scaffold + `/error` açık | `AdminApiSurfaceAndErrorHandlingTest.shouldReturn401ProblemDetail_whenCallingUnknownAdminPath` |
| CR-2 | Düzeltildi | `infra/postgres/init`: `REVOKE ALL ON SCHEMA public FROM PUBLIC`; yalnız `migrator` USAGE+CREATE; default privilege blokları düzenlendi (her iki DB) | `DatabaseSchemaAndRoleSeparationIT`; `AdminDatabaseIsolationIT`; canlı doğrulama (§2) |
| CR-3 | Düzeltildi | `hotel-api` SecurityConfig: allowlist (`/actuator/health`, `/api/**`, `/error`) + `anyRequest().denyAll()`; `/error` RFC 7807 | `ApiSurfaceAndErrorHandlingTest` |
| CR-4 | Düzeltildi | `GlobalExceptionHandler` durum eşlemesi (400/403/415/406; `system.http_<status>` fallback) | İlgili testler (her iki app) |
| CR-5 | Düzeltildi | `.env.example` envanteri; Maven/Flyway `-D` override; `application-local.yml`; port ↔ URL senkronu; `--env-file` | Canlı doğrulama (§2); `docker compose --env-file .env config` |
| CR-6 | Düzeltildi | `DomainErrorCategory` (domain-nötr); eşleme yalnız `com.hotel.web` | `DomainErrorTest`; `ApiSurfaceAndErrorHandlingTest`; `HotelApiModularityTest` |
| CR-7 | Düzeltildi | Migrator probe tablosu; `app` DML; DDL/history reddi — iki DB | `DatabaseSchemaAndRoleSeparationIT`, `AdminDatabaseIsolationIT` |
| CR-8 | Düzeltildi | `src/main` taraması + CI guard adımı | `AdminApiModularityTest.shouldNotReferenceBusinessCoreDatabase_…`; CI guard |
| CR-9 | Düzeltildi | `ProblemDetailSchema` zorunlu alanlar + `.passthrough()` | web/admin-web testleri |
| CR-10 | Düzeltildi | Boş kapalı allowlist + negatif kenar testi | `shouldForbidCrossModuleDependencies_whenCheckingClosedAllowlist` |
| CR-11 | Düzeltildi | `backend/pom.xml` parent | `./mvnw verify` iki app'te yeşil |
| CR-12 | Düzeltildi | `Object.hasOwn` | `i18n.test.ts` / `error-messages.test.ts` |
| CR-13 | Düzeltildi | `@types/node` 24.19.2 | lint/build/test yeşil |
| CR-14 | Düzeltildi | `e2e/README.md` düzeltmeleri | Doküman; E2E zinciri yeşil |
| SD-1 | **Kapandı** | PR #1; CI yeşil (**Run #9 — head `23cf0a6`**, 4/4 job; GitGuardian success). Rulesets `protect-release-branches`: active; deletion/force-push yasak; PR zorunlu; required check `CI Quality Gate`; **allowed_merge_methods = squash**. Onay: solo-maintainer istisnası | `AC-9-ci-run.txt`; `AC-0-branch-protection.txt`; `AC-0-branch-protection.png`; `AC-9-required-check.png` |
| SD-2 | **Tamamlandı** | Spec `specs/done/0001-project-architecture.md`e taşındı; SCORECARD dolduruldu; DoD işaretlendi; referanslar güncellendi | `specs/done/0001-project-architecture.md` (§6–§7) |
| SD-3 | Düzeltildi | AC-2 kanıtı varsayılan 5432 ile tazelendi; 5433 override + URL senkronu belgeli | `AC-2-infra-health.txt` |

## 2. Canlı Doğrulamalar

- **CR-2 (public lockdown):** `app` ile `public` üzerinde DDL ve `flyway_schema_history` DML reddi; `migrator` DDL başarılı; `public` ACL yalnız `pg_database_owner` + `migrator`.
- **CR-5 (.env mekanizması):** Kök `.env` ile port/URL override doğrulandı (`HOTEL_API_PORT=18081`; DB URL `:5433`); `docker compose --env-file .env config` → `5433`.
- **CI e2e (compose `--wait` düzeltmesi, `633d902`):** `minio-init` "init" profiline alındı; `up --wait` exit 0; bucket init'i `--profile init run --rm minio-init` ile kendi exit koduyla doğrulanır. PR #1 Run #2–#9 yeşil (güncel: **Run #9 — `23cf0a6`**).
- **Dal koruması (Rulesets):** active; master/integration/releasable; deletion + non-fast-forward yasak; PR zorunlu; required check `CI Quality Gate`; bypass yok; squash-only. PR #1 `mergeable_state=clean`.
- **E2E:** 4 uygulama ayakta, Playwright smoke `3 passed`; kanıt ekran görüntüleri yenilendi.

## 3. Doğrulama Turu (2026-10-19) — Code Reviewer

Kaynak: `specs/done/evidence/0001/code-review-verification-2026-10-19.md` (CR-1…CR-14 doğrulandı; ek doğrulama §6).

| ID | Durum | Fix / Aksiyon | Kanıt |
|---|---|---|---|
| VR-1 | **Kapandı** | Yönetici kararı: (b) solo-maintainer istisnası → `docs/git.md` §5 (`48b3910`). Ruleset güncellendi: `allowed_merge_methods=["squash"]` (2026-10-19 06:19:43Z); approvals=0 istisna kapsamında kabul edildi | `docs/git.md` §5; `AC-0-branch-protection.txt` |
| VR-2 | **Düzeltildi** | AC-9 kanıtı **Run #9 / `23cf0a6`**'ya çekildi; PR body ve fix raporu senkronlandı; Checks ekran görüntüsü final head ile yenilendi (GitGuardian dahil tümü yeşil) | `AC-9-ci-run.txt`; `AC-9-required-check.png` |
| VR-3 | **Kapandı** | `AdminApiModularityTest` taramasına `pom.xml` eklendi (CI guard ile eşit kapsam) | `AdminApiModularityTest.shouldNotReferenceBusinessCoreDatabase_…`; Run #9 |
| VR-4 | **Kapandı** | Bilinçli default-deny (`allowlist dışı → 401`) `docs/conventions.md` §2.3'e işlendi; test javadoc'u güncel | `docs/conventions.md` §2.3; Run #9 |

## 4. Onay Bekleyen Noktalar (CR yanıtı)

- **AP-07/1** (`docs/conventions.md` `V<n>` değişikliği, `chore(infra)` commit'i): Değişiklik, yöneticinin onayladığı Adım 2 dosya listesindeydi; commit mesajı kapsamı eksik kaldı. Geçmiş yazımı yasak olduğundan geriye dönük bölme yapılmadı; gerekçe kayda geçti.
- **AP-07/2** (Flyway property adları `hotel.flyway.*` → `HOTEL_*`): `-D`/env override mekanizmasını (CR-5) mümkün kılmak için Adım 6'da yapıldı; `infra/README.md`'de belgeli.
- **Merge:** Tek kalan adım — Takım Yöneticisi'nin squash-merge'i (`docs/git.md` §5 solo-maintainer istisnası kapsamında).
