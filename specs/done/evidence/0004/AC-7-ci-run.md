# AC-7 CI run

- **PR:** [#18](https://github.com/altananay/hotel-management-system/pull/18) — `feature/0004-admin-identity` → `integration`
- **Run:** [41250647651](https://github.com/altananay/hotel-management-system/actions/runs/41250647651)
- **Head:** `8663a14` (`ci(e2e): capture 0004 csrf and session gate evidence [plan 0004/16]`)
- **Sonuç:** **success** (2026-10-31T23:00:25Z → 2026-10-31T23:09:03Z)

## İşler

| İş | Sonuç | Log |
|---|---|---|
| Backend (hotel-api & admin-api) | success | [job](https://github.com/altananay/hotel-management-system/actions/runs/41250647651/job/124683277050) |
| Frontend (web & admin-web) | success | [job](https://github.com/altananay/hotel-management-system/actions/runs/41250647651/job/124683277091) |
| E2E Smoke (Playwright Chromium) | success | [job](https://github.com/altananay/hotel-management-system/actions/runs/41250647651/job/124683276838) |
| CI Quality Gate | success | [job](https://github.com/altananay/hotel-management-system/actions/runs/41250647651/job/124685152424) |

## Test toplamları

- `hotel-api`: **83/83** (`AdminJwtSecurityTest` 4/4 dahil)
- `admin-api`: **56/56** birim + **5/5** Testcontainers IT (`AdminDatabaseIsolationIT`)
- `admin-web`: **21/21** Vitest; lint + build yeşil
- `@admin` E2E: **3/3**
- `0002` smoke: **3/3** (1 `@empty` + 2 `@seeded`)
- `0003` design: **40/40**

## Not

Bu run 0004 kanıtlarını (`0004-evidence` artefaktı) üretmiştir. Kanıt
dosyaları `specs/done/evidence/0004/` altına işlenmiştir.
