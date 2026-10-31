# 0004 E2E ve CI özeti

- CI run: [41251924005](https://github.com/altananay/hotel-management-system/actions/runs/41251924005) — head `82bbe5d`, sonuç **success**.
- PR: [#18](https://github.com/altananay/hotel-management-system/pull/18) → `integration`.
- E2E job: **success**.

## Sonuçlar

| Paket | Komut | Sonuç |
|---|---|---|
| 0002 `@empty` | `npx playwright test tests/smoke.spec.ts --grep @empty` | **1 passed** (3.1s) |
| 0002 `@seeded` | `npx playwright test tests/smoke.spec.ts --grep @seeded` | **2 passed** (3.8s) |
| 0003 design `@seeded` | `npx playwright test tests/design --grep @seeded` | **40 passed** (2.3m) |
| 0004 `@admin` | `npx playwright test tests/admin --grep @admin` | **3 passed** (5.3s) |
| 0004 ekran kanıtı | `node scripts/capture-0004-admin-evidence.mjs` | 3 PNG + 2 log |

## `@admin` testleri (AC-6)

1. `shouldLoginAndRenderProtectedHome_whenCredentialsAreValid` — başarılı giriş + korumalı ana sayfa.
2. `shouldShowGenericError_whenCredentialsAreInvalid` — generic hata, teknik detay yok.
3. `shouldLogoutAndReturnToLogin_whenSessionIsActive` — oturum iptali + `/login` dönüşü.

## Arşiv ve seed

- Admin seed: `R__seed_admin_dev.sql` `@admin` öncesinde `flyway:migrate -Pseed -Dseed.env=ci` ile uygulandı.
- Seed guard: eksik/prod env ve marker'sız DB reddedildi.
- `0002` kanıt SHA-256: **UNCHANGED (12 dosya)**; `Result: UNCHANGED`.
- `0003` design **40/40** korundu.

## Komutlar

```bash
# backend
cd backend/hotel-api && ./mvnw verify
cd ../admin-api && ./mvnw verify

# admin-web
cd admin-web && npm run lint && npm run build && npm test

# 0002/0003/0004 E2E
cd e2e
E2E_EVIDENCE_DIR="$RUNNER_TEMP/evidence-0002" npx playwright test tests/smoke.spec.ts --grep @empty
E2E_EVIDENCE_DIR="$RUNNER_TEMP/evidence-0002" npx playwright test tests/smoke.spec.ts --grep @seeded
npx playwright test tests/design --grep @seeded
npx playwright test tests/admin --grep @admin
node scripts/capture-0004-admin-evidence.mjs
node scripts/sha256-manifest.mjs verify ../specs/done/evidence/0002 "$RUNNER_TEMP/0002-before.json" ../specs/done/evidence/0003/AC-6-0002-evidence-sha256.txt
```
