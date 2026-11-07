# 0005 — Integration Baseline Verification (`integration` @ `179d3d0`)

- **Tarih:** 2026-11-07
- **Head:** `179d3d04150f32d209c8a6f0214bace4e9e542c4` (`fix(media): address independent code review findings [plan 0005]`)
- **CI:** GitHub Actions run `42326617436` — success (Backend, Frontend, E2E Smoke, CI Quality Gate)
- **URL:** https://github.com/altananay/hotel-management-system/actions/runs/42326617436
- **Kaynak:** Bağımsız QA denetimi (`specs/0005-qa-findings.md`, 2026-11-07)

Bu dosya, 0005 kanıt setinin eski (2026-11-03/04) sayıları yerine `integration` üzerindeki
güncel ağacı temsil etmesi için eklendi. Eski dosyalar tarihsel olarak işaretlendi.

## Backend

```
backend/hotel-api  ./mvnw.cmd -B verify
Spotless: clean
Unit tests: 179 passed
Integration tests: 57 passed
  - CatalogSeedIT 2/2
  - RoomCatalogIT 40/40
  - DatabaseSchemaAndRoleSeparationIT 5/5
  - MediaIT 10/10 (real PostgreSQL 17.4 + alpine/minio RELEASE.2025-10-15T17-29-55Z)

backend/admin-api  ./mvnw.cmd -B verify
Unit tests: 101 passed
Integration tests: 5 passed
Spotless: clean
```

## Frontend

```
web        npm run lint -> clean; npm test -> 43/43; npm run build -> success
admin-web  npm run lint -> clean; npm test -> 35/35; npm run build -> success
```

## E2E

```
@empty   1/1        (CI; local audit DB was seeded)
seed catalog
@seeded  2/2
@seeded design 40/40
@admin   3/3
@media   1/1
```

`@media` fixture: `web/public/media/hero/home-hero-poster-1280.webp` (**WebP**, Pexels-licensed).
The pre-QA-fix flow did not assert alt text; that gap is closed by `fix/0005-qa-findings` and
recorded in `AC-6-qa-fixes-verification.md`.

## Arşiv bütünlüğü

`AC-6-archive-sha256.txt` lists 0001–0004 evidence files. The text-mode verifier now supports the
documented command:

```
node e2e/scripts/sha256-manifest.mjs verify specs/done/evidence specs/done/evidence/0005/AC-6-archive-sha256.txt
```

Result: 129/129 listed files byte-identical; 0 missing, 0 changed.
