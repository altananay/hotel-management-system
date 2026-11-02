# 0004 QA fix doğrulaması — 2026-11-01

- **QA brief:** `specs/0004-qa-findings-brief.md`
- **İncelenen integration head:** `b3f4340`
- **Fix PR:** [#19](https://github.com/altananay/hotel-management-system/pull/19)
- **Merge commit:** `78104e6` — `fix(admin): address 0004 QA findings [plan 0004] (#19)`
- **Post-merge CI:** [41328803979](https://github.com/altananay/hotel-management-system/actions/runs/41328803979) — **success**

## Bulgu → fix eşlemesi

| Bulgu | Fix | Doğrulama |
|---|---|---|
| **F-01** Bootstrap CLI JWT anahtarı istiyordu | `AdminBootstrapApi` ayrıldı; web/JWT/CSRF bean'leri `@ConditionalOnWebApplication(SERVLET)` ile CLI context'inden çıkarıldı; startup DB hatası exit 3'e eşlendi. | CI adımı: `--bootstrap-admin` + eksik JWT anahtarı → **success**; `AdminBootstrapCliTest`, `AdminApiApplicationTest`. |
| **F-02** Login'de NFC normalizasyonu yoktu | `Argon2PasswordHasher.hash/matches` NFC normalize eder. | `Argon2PasswordHasherTest` NFD↔NFC çift yönlü. |
| **F-03** Logout CSRF retry 401 login'e yönlendirmiyordu | Logout isteği helper'a alındı; ilk istek ve retry 401'i `/login`'e yönlendirir. | `logout-button.test.tsx` 403→401 senaryosu. |
| **F-04** Kilit anahtarı `user_id` idi | Bilinen kullanıcıda kanonik kullanıcı adı, bilinmeyende normalize identifier; alias'lar aynı sayaçta. | `AdminIdentityServiceTest` identifierNorm/alias beklentileri. |
| **F-05** Gateway ikinci `X-Request-Id` üretiyordu | Edge request id'si upstream ve yanıta taşınır; ikinci ID üretilmez. | `AdminGatewayClientTest` upstream/response eşitliği. |

## Test toplamları

- `admin-api`: **101/101** birim + **5/5** Testcontainers IT
- `hotel-api`: **89/89**
- `admin-web`: **24/24** Vitest; lint + build yeşil
- E2E: `@admin` **3/3**, 0002 **3/3**, 0003 design **40/40**
- CI: backend, frontend, E2E, quality gate ve JWT anahtarsız bootstrap adımı yeşil.

## Not

F-04'te spec'teki "normalize identifier" ifadesi bilinen kullanıcıda **kanonik kullanıcı adı** olarak yorumlandı; e-posta alias'ı aynı sayacı kullanmaya devam eder. Spec rev.9'da bu netleştirildi.
