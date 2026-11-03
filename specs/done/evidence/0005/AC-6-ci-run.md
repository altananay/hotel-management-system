# 0005 CI / Verification Run

Date: 2026-11-03 (local CI-equivalent run, Docker Desktop 28.5.1)

## Backend

```
backend/hotel-api  ./mvnw.cmd -B verify
Spotless: clean
Unit tests: 173 passed
Integration tests: 56 passed
  - CatalogSeedIT 2/2
  - RoomCatalogIT 40/40
  - DatabaseSchemaAndRoleSeparationIT 5/5
  - MediaIT 9/9 (real PostgreSQL 17.4 + alpine/minio RELEASE.2025-10-15T17-29-55Z)
```

## Frontend

```
web        npm run lint -> clean; npm test -> 42/42; npm run build -> success
admin-web  npm run lint -> clean; npm test -> 32/32; npm run build -> success
```

## E2E (CI order, fresh volumes)

```
@empty   1/1
seed catalog
@seeded  2/2
@seeded design 40/40
@admin   3/3
@media   1/1
```

Notes:
- `@media` used the committed Pexels hero poster as the upload fixture and exercised
  SUPER_ADMIN login -> SVG/oversize rejection -> WebP upload -> READY -> assign ->
  publish -> cover -> public `room-card-image` WebP -> FRONT_DESK 403 -> unpublish
  (`{items: []}`) -> archive confirmation.
- 0002/0003 E2E suites were not modified; all passed.
- GitHub Actions CI: **success** — https://github.com/altananay/hotel-management-system/actions/runs/41717715249
  (`Backend`, `Frontend`, `E2E Smoke` and `CI Quality Gate` all green).
- GitGuardian reports one pre-existing local test password at base commit `c9e7da4`
  (`AdminIdentityServiceTest`), not introduced by 0005; CI quality gates are green.
