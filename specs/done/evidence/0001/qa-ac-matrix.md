# QA — Kabul Kriteri ↔ Kanıt Matrisi (Spec 0001)

> Bu dosya `specs/done/evidence/` altında **yeni bir kanıt kaydıdır**; arşivdeki spec (`specs/done/0001-project-architecture.md`) değiştirilmemiştir (`specs/done/README.md` §Kurallar).
> Amaç: her AC'nin hangi test/kanıtla doğrulandığını izlenebilir kılmak (`docs/testing.md` §1).

- **Denetleyen:** QA (`docs/roles/qa.md`)
- **Tarih:** 2026-10-19
- **Denetlenen head:** `1f7f1e0` (`docs(conventions): require globally unique Flyway migration versions [plan 0001/7]`)
- **Spec:** `specs/done/0001-project-architecture.md` (rev.2, Durum: Tamamlandı)
- **Ortam:** Windows 11 · Java 25.0.2 · Node 24.11.1 · npm 11.19.1 · Docker 28.5.1 / Compose 2.40.3 · PostgreSQL 17.4-alpine · MinIO RELEASE.2025-10-15T17-29-55Z · Mailpit v1.22.3

## 1. Koşum kaydı (QA tarafından bağımsız çalıştırıldı)

| # | Komut | Sonuç |
|---|---|---|
| 1 | `backend/hotel-api> .\mvnw.cmd -B verify` | **BUILD SUCCESS** — 39 birim + 5 IT = **44 test**, 0 kırmızı; Spotless temiz |
| 2 | `backend/admin-api> .\mvnw.cmd -B verify` | **BUILD SUCCESS** — 11 birim + 5 IT = **16 test**, 0 kırmızı |
| 3 | `web> npm.cmd run lint` / `build` / `test` | **0 / 0 / 0** — 2 dosya, **8 test** yeşil |
| 4 | `admin-web> npm.cmd run lint` / `build` / `test` | **0 / 0 / 0** — 2 dosya, **6 test** yeşil |
| 5 | `e2e> npx.cmd playwright test` (prod mod) | **3/3 passed**; art arda **5/5 yeşil** (AP-03 dayanıklılık kanıtı) |
| 6 | `docker compose -f infra/docker-compose.yml up -d --wait` | ExitCode 0 — postgres/minio/mailpit **healthy** |
| 7 | `docker compose ... --profile init run --rm minio-init` | ExitCode 0 — `hotel-media` bucket mevcut |
| 8 | `curl` ile çalışma zamanı uçları (8081/8082) | Aşağıdaki AC-6/AC-11 satırlarındaki durumlar doğrulandı |
| 9 | `psql` (hotel-postgres) | 6 şema; `app` DDL reddi; `migrator`/`app` rol ayrımı |

> **Düzeltme notu (QA):** Önceki denetim mesajımda admin-api için "22 test (17 birim + 5 IT)" yazmıştım; doğru sayı **16 test (11 birim + 5 IT)**'dir. Maven Surefire/Failsafe özetleri esas alınmıştır.

## 2. AC ↔ kanıt matrisi

| AC | Kriter (özet) | Test / kanıt | Sonuç |
|---|---|---|---|
| **AC-0** | `integration` + `releasable` korumalı; feature `releasable` tabanlı; spec branch'te commit'li | `AC-0-git-bootstrap.txt` (`git branch -a`, `merge-base --is-ancestor` → 0) · `AC-0-branch-protection.txt` + `.png` (ruleset `25358074`: deletion/non-FF/pull_request/required check `CI Quality Gate`; `allowed_merge_methods=["squash"]`) · `AC-9-required-check.png` · QA: GitHub API ile PR #1 (`7c352c4`) ve PR #2 (`4ed1c75`) merge doğrulandı | **GEÇTİ** — süreç notu: QA-1 (yönetici kapsamı dışı) |
| **AC-1** | Monorepo dizinleri + `.github/pull_request_template.md`, `.gitattributes`, `.editorconfig`, tek `.env.example`; sır yok | `AC-1-secret-scan.txt` (özel anahtar/AWS/GitHub token desenleri: **0 eşleşme**) · QA: `.gitattributes` (`* text=auto eol=lf` + binary tanımları), `.editorconfig` (root/utf-8/LF/final newline; Java-XML 4, TS-JSON-YAML-CSS 2), `.env.example` envanteri, `.gitignore` (`.env` hariç, `!.env.example`), `contracts/.gitkeep` + `README.md`, PR şablonu mevcut | **GEÇTİ** |
| **AC-2** | Compose ile postgres/minio/mailpit healthy; pinli imaj; `hotel-media` bucket; iki DB + `migrator`/`app`; init tek kaynak; quickstart | `AC-2-infra-health.txt` · QA: `up -d --wait` → 3 servis healthy; `minio-init` → `Bucket created successfully local/hotel-media`; `mc stat` ile bucket doğrulandı; imajlar tam tag pinli (`postgres:17.4-alpine`, `alpine/minio:RELEASE...`, `axllent/mailpit:v1.22.3`, `rancher/mirrored-minio-mc:RELEASE...`); healthcheck'ler (`pg_isready`, `/minio/health/live`, `mailpit readyz`) · `infra/postgres/init/01-init-roles-and-dbs.sql` (roller, iki DB, `REVOKE public`) · `infra/README.md` | **GEÇTİ** — açık not: QA-4 (bucket init ayrı adım; A seçeneği ile belgelenecek) |
| **AC-3** | 8 modül `api`/`internal`; kapalı izin listesi; entity sızması/döngü/`double`-`float` yasakları; 6 şema; Modulith + ArchUnit yeşil | `HotelApiModularityTest` **9 test** (`shouldVerifyModularStructure…8BusinessModules`, `shouldDefineApiAndInternalSubpackages…`, `shouldForbidSpringWebAndHttpDependencies…SharedKernel`, `shouldForbidForbiddenModuleDependencies…ClosedAllowlist`, `shouldForbidInternalPackageAccess…`, `shouldForbidCyclicDependencies…`, `shouldForbidEntityLeakage…`, `shouldForbidDoubleAndFloat…`, `shouldNotTouchOtherSchemas…ModuleMigrations`) · `DatabaseSchemaAndRoleSeparationIT` (6 şema) · QA: prod kodda `\b(double\|float\|Double\|Float)\b` taraması → **0 eşleşme**; `com.hotel` altında `web/config/scaffold` kök paketleri, 8 iş modülü ayrık | **GEÇTİ** |
| **AC-4** | `Money`, `HotelDate`, `StayPeriod`, `TypedId`, `DomainError` sınır değerleriyle birim testli; iş kuralı yok | `MoneyTest` **6** (plus/minus/multiply; negatif-sıfır; yalnız açık `round(2, HALF_UP)`; `sharedkernel.currency_mismatch`; ölçekten bağımsız `equals` + farklı currency `false`; `divide`/float overload yok) · `HotelDateTest` **2** (Europe/Istanbul sabit clock; ISO `yyyy-MM-dd`) · `StayPeriodTest` **2** (`checkOut > checkIn`; eşit/geri → `sharedkernel.invalid_stay_period`; geçmiş/uzun aralık serbest; gece = gün farkı) · `TypedIdTest` **3** (v7 + aynı ms artan sıra; biçim kabulü/`sharedkernel.invalid_id`; somut tip + UUID eşitliği) · `DomainErrorTest` **4** (modül önekli kod, İngilizce mesaj, kategori) | **GEÇTİ** |
| **AC-5** | 4 admin modülü; `hotel_admin`'e bağlı, `hotel_core` erişimi yok; mimari testler yeşil | `AdminApiModularityTest` **6** (`…FourAdminModules`, `…ApiAndInternalSubpackages`, `…ClosedAllowlist`, `…Boundaries`, `…EntityLeakageAndDoubleFloat`, `shouldNotReferenceBusinessCoreDatabase…`) · `AdminDatabaseIsolationIT` **5** (`shouldConnectToHotelAdminDatabase…`, `current_database`; `app` DML; DDL reddi; public/history reddi; yalnız health) · `AC-5-admin-api-no-hotel-core.txt` (`git grep … hotel_core` → **0**, testler hariç) | **GEÇTİ** |
| **AC-6** | Public/admin yüzey ayrımı; anonim `/admin-api/**` → 401 problem+json; health 200; RFC 7807 zarfı + `X-Request-Id`; CORS; stack trace yok | `ApiSurfaceAndErrorHandlingTest` **13** · `AdminApiSurfaceAndErrorHandlingTest` **5** · QA çalışma zamanı: `/actuator/health` → **200** `{"status":"UP"}` (detay yok); `/admin-api/anything` → **401** `application/problem+json` (`code=security.unauthorized`, `requestId`); `/api/nonexistent` → **404** `system.not_found`; POST → **405** `system.method_not_allowed`; geçerli `X-Request-Id: qa-probe-12345678` **aynen** döndü; geçersiz `"bad id!"` → UUIDv7 ile değiştirildi; `Origin: http://localhost:3000` → `ACAO` + `Access-Control-Expose-Headers: X-Request-Id`; `Origin: http://evil.example` → **403**, ACAO yok; admin-web rewrite `3001/admin-api/...` → 200; `admin-api` yanıtında CORS başlığı yok (aynı origin) | **GEÇTİ** — açık not: QA-3 (admin-api CORS cümlesi netleştirilecek) |
| **AC-7** | `web` klasör düzeni; TR/EN rota geçişi; lint + birim test; TR/EN görsel kanıt | `AC-7-web-tr.png`, `AC-7-web-en.png` · `web/src/lib/i18n/__tests__/i18n.test.ts` **3** · `web/src/features/system-status/__tests__/system-status-card.test.tsx` **5** · QA: `lint`/`build`/`test` → 0/0/0; `/` → `/tr`; `<html lang="tr">`; EN geçişinde `lang="en"`; `src/app`, `src/features`, `src/components/ui`, `src/lib` mevcut | **GEÇTİ** |
| **AC-8** | `admin-web` DB/sır yok; yalnız `admin-api` (rewrite); TR arayüz; lint + test; izolasyon kanıtı | `AC-8-admin-web-tr.png` · `AC-8-admin-web-isolation.txt` (`git grep 'hotel-api\|8081' -- admin-web` → **0**) · `admin-system-status-card.test.tsx` **3** · `error-messages.test.ts` **3** · QA: `lint`/`build`/`test` → 0/0/0; `next.config.ts` rewrite `/admin-api/:path*` → `ADMIN_API_INTERNAL_URL` (varsayılan `:8082`); `password/secret/DATABASE_URL/prisma/drizzle` taraması → **0** | **GEÇTİ** |
| **AC-9** | `ci.yml`; iki backend `./mvnw verify`; iki frontend `npm ci && lint && build && test`; required check yeşil | `AC-9-ci-run.txt` (Run #9 / `39209335358` / head `23cf0a6`: Backend, Frontend, E2E, CI Quality Gate **success**) · `AC-9-required-check.png` · `AC-0-branch-protection.txt` (required check `CI Quality Gate`) · QA: `.github/workflows/ci.yml` adımları spec ile birebir (JDK 25, `.nvmrc`, `spotless:check` `verify` içinde, `eslint . && tsc --noEmit`, prod build + start, Playwright, `ci-gate`) | **GEÇTİ (kapsam: `23cf0a6`)** — `1f7f1e0` CI kaydı yok; QA-1 yönetici kapsamında |
| **AC-10** | Kök `e2e/` tek Playwright projesi (chromium); iki uygulama açılışı + üç durum; route ile yükleme/hata; `data-testid`; `e2e/README.md` | `e2e/tests/smoke.spec.ts` **3 test** (TR/EN geçişi; web üç durum; admin-web üç durum) · `e2e/playwright.config.ts` (tek proje: chromium, `workers:1`, `retries:0`) · `e2e/README.md` (5 adımlı local koşum) · `AC-10-smoke-web.png`, `AC-10-smoke-admin-web.png` · QA: prod modda **3/3**, art arda **5/5** yeşil | **GEÇTİ** |
| **AC-11** | `sample-items` uçları `200 {"items":[]}`; item şeması; üç durum gerçek hook + Zod; scaffold işaretli; 6 görsel | `AC-11-web-{loading,empty,error}.png` + `AC-11-admin-web-{loading,empty,error}.png` · QA: uçlar → `200 {"items":[]}` (8081 ve 8082); `TODO(scaffold-0001)` + javadoc **iki** uygulamada mevcut (`SampleItemsController`, `AdminSampleItemsController`); `useSampleItemsQuery`/`useAdminSampleItemsQuery` + `zod` şemaları; QA yeniden koşumunda 6 PNG **byte-identical** (git status temiz) | **GEÇTİ** |
| **AC-12** | `./mvnw verify` IT yeşil; 6 şema; `app` DDL reddi; admin-api `hotel_admin` bağlantısı; init tek kaynak; `spring.flyway.enabled=false` | `DatabaseSchemaAndRoleSeparationIT` **5** · `AdminDatabaseIsolationIT` **5** · QA: iki `application.yml` → `spring.flyway.enabled: false`; migration envanteri (`hotel_core` V1–V6 modül klasörlerinde, `hotel_admin` V1); `psql`: 6 şema, `app` `CREATE TABLE` → **permission denied** | **GEÇTİ** |

## 3. Kapsam ve açık maddeler

1. **QA-1 (merge sonrası `1f7f1e0` commit'i):** Takım Yöneticisi tarafından "benim işim, es geç" olarak kapsam dışı bırakıldı. Bu matriste CI kanıtı `23cf0a6` head'ine aittir; `1f7f1e0` için CI/PR kanıtı **iddia edilmemektedir**.
2. **QA-3 (AC-6 / admin-api CORS):** Davranış doğru (admin-api'de CORS yok, aynı origin rewrite). Eksik olan doküman cümlesi; analist taslağı iletildi.
3. **QA-4 (AC-2 / bucket init):** Bucket init ayrı `--profile init` adımıdır; yönetici **A seçeneğini** (belgele) kabul etti. Doküman notu analist tarafından eklenecek.
4. **AC-9 kapsamı:** Kanıt dosyasındaki run `23cf0a6` head'ine aittir; sonraki commit'ler için CI kanıtı bu matriste yer almaz (madde 1).

## 4. Sonuç

- **Kod/test durumu:** Kırmızı yok. Backend **60 test** (hotel-api 44 + admin-api 16), frontend **14 test** (web 8 + admin-web 6), E2E **3 test × 5 tekrar** — tümü yeşil.
- **AC kapsamı:** **13/13** AC kanıtla eşleşti (AC-0…AC-12).
- **Açık doküman maddeleri:** QA-3, QA-4/A (analist); QA-1 yönetici kapsamında.
- **QA imzası:** "QA, Dev'in penceresine girmez." Bu matris yalnız okuma, çalıştırma ve kanıt toplama ile üretildi; kod/test/spec değiştirilmedi.
