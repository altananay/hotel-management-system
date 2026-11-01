# 0004 — Admin Kimlik ve Oturum kanıtları

Bu dizin 0004 kabul kriterlerini gerçek test/CI çıktılarıyla eşler.
Tüm loglar code review fixleri sonrası head `78104e6` üzerinde alınmıştır.

## İzlenebilirlik

| AC | Kapsam | Kanıt |
|---|---|---|
| AC-1 | Kimlik modeli, parola politikası, seed, bootstrap | `AC-1-admin-api-verify.log`, `AC-1-admin-seed-and-bootstrap.log`, `AC-1-bootstrap-*.log` |
| AC-2 | Oturum, kilit, CSRF, login context | `AC-2-login-session.log`, `AC-2-session-expiry.log`, `AC-2-lockout-and-enumeration.log`, `AC-2-csrf.log`, `AC-2-csrf-precedence.log`, `AC-2-login-context.log` |
| AC-3 | JWT, JWKS, hotel-api 401/403/503 | `AC-3-jwt-claims.log`, `AC-3-jwks-rotation.log`, `AC-3-hotel-api-auth.log` |
| AC-4 | Kök kimlik filtresi ve gateway | `AC-4-gateway-it.log`, `AC-4-header-hygiene.log`, `AC-4-request-id.log`, `AC-4-allowlist.log` |
| AC-5 | Audit olayları ve fail-closed | `AC-5-audit-events.log`, `AC-5-audit-failclosed.log`, `AC-5-rotation-audit.log`, `AC-5-bootstrap-skip-audit.log`, `AC-5-audit-retention.md` |
| AC-6 | Admin-web login, oturum kapısı, logout, CSRF | `AC-6-*.png`, `AC-6-scaffold-removed.log`, `AC-6-csrf-and-gate.log`, `AC-6-admin-web-vitest.log` |
| AC-7 | Testler, CI, arşiv izolasyonu | `AC-7-e2e-admin-summary.md`, `AC-7-ci-run.md`, `AC-7-0003-design-40.txt`, `AC-7-archive-sha256.txt` |

## Test toplamları

- `admin-api`: **101/101** birim, **5/5** Testcontainers IT
- `hotel-api`: **89/89**
- `admin-web`: **24/24** Vitest; lint + build yeşil
- E2E: `@admin` **3/3**, 0002 **3/3**, 0003 design **40/40**

## Code review fixleri

- `QA-fix-verification-2026-11-01.md` — QA F-01…F-05 fix ve doğrulama özeti.
- `code-review-fixes-2026-11-01.md` — CR-1…CR-11 eşlemesi; CR-9/CR-10 için gerekçeli
  erteleme/itiraz notları.
- `code-review-brief-2026-11-01.md` — incelenen brief.

## AC-6 görselleri

- `AC-6-admin-login-tr.png` — TR giriş ekranı.
- `AC-6-admin-login-error.png` — generic hata (parola sızmaz).
- `AC-6-admin-home-protected.png` — giriş sonrası korumalı ana sayfa.
- `AC-6-admin-logout.png` — çıkış sonrası `/login`.
- `AC-6-admin-redirect-401.png` — oturumsuz `/` isteği `/login`'e yönlenir.
- `AC-6-admin-csrf-fetch-fail.png` — CSRF alınamazsa hata + tekrar dene.
- `AC-6-admin-csrf-retry.png` — ilk login POST'a zorlanan 403 sonrası otomatik tekrar başarılı.

## Notlar

- `0002` kanıtları CI'da **12 dosya / byte-identical** doğrulandı; `0003`
  kanıtlarına dokunulmadı (`AC-7-archive-sha256.txt`).
- AC-6 CSRF/oturum kapısı görselleri, `@admin` testlerinden sonra
  `e2e/scripts/capture-0004-admin-evidence.mjs` ile üretilir.
- `AdminJwtConfigTest`, 5 dk cache, 60 sn unknown-kid cooldown, iat +60 sn
  ve JWKS erişilemezliği senaryolarını doğrular.
