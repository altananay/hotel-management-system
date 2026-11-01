# AC-7 CI run

- **PR:** [#18](https://github.com/altananay/hotel-management-system/pull/18) — `feature/0004-admin-identity` → `integration`
- **Run:** [41308208928](https://github.com/altananay/hotel-management-system/actions/runs/41308208928)
- **Head:** `04c4673` (`test(admin-api): prove login uses the shared client context [plan 0004/28]`)
- **Sonuç:** **success** (2026-11-01T12:48:11Z → 2026-11-01T12:55:03Z)

## İşler

| İş | Sonuç | Log |
|---|---|---|
| Backend (hotel-api & admin-api) | success | [job](https://github.com/altananay/hotel-management-system/actions/runs/41308208928/job/124852895976) |
| Frontend (web & admin-web) | success | [job](https://github.com/altananay/hotel-management-system/actions/runs/41308208928/job/124852895885) |
| E2E Smoke (Playwright Chromium) | success | [job](https://github.com/altananay/hotel-management-system/actions/runs/41308208928/job/124852895752) |
| CI Quality Gate | success | [job](https://github.com/altananay/hotel-management-system/actions/runs/41308208928/job/124854436622) |

## Test toplamları

- `hotel-api`: **89/89** (`AdminJwtSecurityTest` 4/4, `AdminJwtConfigTest` 4/4 dahil)
- `admin-api`: **95/95** birim + **5/5** Testcontainers IT (`AdminDatabaseIsolationIT`)
- `admin-web`: **23/23** Vitest; lint + build yeşil
- `@admin` E2E: **3/3**
- `0002` smoke: **3/3** (1 `@empty` + 2 `@seeded`)
- `0003` design: **40/40**

## Not

Bu run code review fixlerinden sonraki doğrulama run'ıdır; `@admin` testleri
ve kanıt yakalama adımı (`capture-0004-admin-evidence.mjs`) yeşildir.
`0002` kanıtları byte-identical, `0003` design 40/40 korunmuştur.
