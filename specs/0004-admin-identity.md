# Admin Kimlik ve Oturum

> Doldurmadan önce: `AGENTS.md` (altın kurallar) ve `docs/architecture.md` okunur.
> Numara: `specs/` altındaki en büyük numaranın + 1'i. Dosya: `specs/<####>-<english-name>.md` (İngilizce kebab-case; içerik Türkçe). Branch: `feature/<####>-<english-name>` (yalnızca `releasable`'dan). Örnek: `specs/0004-admin-identity.md`.
> Spec onaylanmadan kod yazılmaz (Altın Kural 1). Onayı Takım Yöneticisi verir.

---

## 0. Künye

- **Spec No:** `0004`
- **Başlık:** Admin Kimlik ve Oturum (`admin-identity`)
- **Durum:** Onaylandı *(rev.7 — eşzamanlılık kapsamı doküman netleştirmeleri işlendi)*
- **Branch:** `feature/0004-admin-identity`
- **İlgili doküman/kararlar:** `docs/architecture.md` (§2, §4, §5 R3/R6, §6, §8, §10 D2/D19, §11), `docs/domain.md` (§1 `AdminSession`/`AdminRole`/`AuditEntry`, §3 rol tablosu), `docs/conventions.md` (§2.3, §2.4, §4), `docs/frontend.md`, `docs/testing.md`, `docs/git.md`, `specs/done/0001-project-architecture.md` (`CLARIFY-3A`, `F5`, `F7`), `specs/done/0002-room-catalog.md` (seed konvansiyonu), `specs/done/0003-ui-design-system.md` (E2E kanıt helper'ı); Onaylı kararlar: `CLARIFY-1A`…`CLARIFY-9A`, `CLARIFY-11A`, `Q10=A` (CSRF double-submit + Origin, 2026-10-28); rev.4: identifier+IP kilidi, CSRF/Origin önceliği, gateway request-id, login rotasyonu audit'i ve 0003 40/40 CI koruması işlendi; rev.5: B-14 bootstrap aktif SUPER_ADMIN/audit rollback düzeltmesi işlendi; rev.6: bootstrap CLI/eşzamanlılık, `BOOTSTRAP_SKIPPED` skip audit'i, CSRF fetch hatası ve `@admin` test adları işlendi; rev.7: eşzamanlılık kapsamı aynı `ADMIN_BOOTSTRAP_USERNAME` ile netleştirildi, global tek-SUPER_ADMIN invariantı kapsam dışına alındı
- **Tarih:** 2026-10-28 *(Son revizyon: 2026-10-29, rev.7)*
- **Sahip:** Analist (`analyst`)
- **Onaylayan (Takım Yöneticisi):** Takım Yöneticisi (Onaylandı - 2026-10-29)

## 1. Intent (Neden?)

Otel personeli (`SUPER_ADMIN`, `MANAGER`, `FRONT_DESK`) ve otel yönetimi, bugün yalnızca sağlık kontrolü ve geçici scaffold ucu sunan admin platformunu güvenli biçimde kullanabilmek; medya, fiyat ve rezervasyon yönetimi gibi iş ekranları gelmeden önce kimlik, oturum ve yetki temelini kurmak istiyor. Başarı; personelin admin-web'den kullanıcı adı/parola ile giriş yapması, oturumun tarayıcıda token olarak değil sunucu tarafında opak bir oturum olarak yaşaması, korumalı sayfalarda çerezle gezinmesi ve çıkış yapması; `hotel-api` admin yüzeyinin yalnızca doğru imza, audience ve rol taşıyan kısa ömürlü JWT ile yanıt vermesi; giriş, başarısız giriş, çıkış ve oturum iptallerinin denetlenebilir olmasıdır. Bu dilimde katalog/fiyat/medya/rezervasyon yönetim ekranları, kullanıcı yönetimi CRUD'u, parola sıfırlama e-postası, 2FA/SSO, müşteri kimliği, admin-web tasarım sistemi ve analitik bilerek yapılmayacaktır.

## 2. Requirements

- **REQ-1 (Admin Kimlik Çekirdeği — `CLARIFY-2A`, `CLARIFY-4A`):** Admin kullanıcıları; benzersiz kullanıcı adı ve e-posta, Argon2id parola özeti, rol (`SUPER_ADMIN` / `MANAGER` / `FRONT_DESK`), aktiflik ve zaman damgalarıyla `hotel_admin.admin_platform` şemasında tanımlanmalıdır. Parola politikası en az 12 / en fazla 128 karakter olmalı; kompozisyon kuralı dayatmamalı; kullanıcı adı/e-posta parola içerememelidir. Giriş hataları kullanıcı numaralandırmasına izin vermemeli; bilinmeyen kullanıcıda da parola doğrulama maliyeti korunmalıdır. Local/CI için üç rolü temsil eden, açıkça local-only işaretli tekrar çalıştırılabilir seed; prod için ayrı bir CLI/init job olarak çalışan, env'den güçlü parola alan tek seferlik bootstrap bulunmalıdır. Bootstrap **yalnız aktif SUPER_ADMIN varsa skip + uyarı** ile çıkar; aktif SUPER_ADMIN yokken herhangi bir admin satırı (pasif SUPER_ADMIN, MANAGER veya FRONT_DESK) varsa **fail-fast** eder; audit yazılamazsa admin kaydı **rollback/delete** edilir, pending/pasif SUPER_ADMIN bırakılmaz ve aktif admin oluşmaz.
- **REQ-2 (Oturum Yönetimi ve CSRF — `CLARIFY-3A`, `Q10=A`):** Personel giriş yapabilmeli, oturum sorgulayabilmeli, çıkış yapabilmeli ve korumalı sayfalarda gezinebilmelidir. Oturum kimliği opak olmalı; tarayıcıda oturum çerezi `ADMIN_SESSION` adıyla `HttpOnly`, `SameSite=Lax`, `Path=/`, prod'da `Secure` (local'de env ile kapalı) taşınmalı; JWT/oturum token'ı tarayıcıda saklanmamalıdır. Oturum kimliği veritabanında düz metin değil, özetiyle saklanmalı; girişte oturum rotasyonu yapılmalıdır. Oturum **30 dk idle + 8 saat mutlak** ömürlüdür; `last_seen_at` her doğrulanmış istekte güncellenir. Çıkış yalnız mevcut oturumu iptal eder. Giriş hataları generic olmalı, teknik detay sızdırmamalıdır.
  - **CSRF (`Q10=A`):** Ayrı ve okunabilir `XSRF-TOKEN` çerezi + `X-XSRF-TOKEN` başlığı ile double-submit; login dahil tüm state-changing admin-api isteklerinde zorunludur. Login öncesi token **15 dk** TTL taşır; login sonrası rotasyonla oturum ömrüne bağlanır. Origin allowlist'i local'de `http://localhost:3001`, prod'da admin alan adıdır; Origin yoksa veya izinli değilse state-changing istek reddedilir. Token login sonrası döner, logout'ta temizlenir. Eksik/geçersiz token veya izinsiz Origin → **403 `security.csrf_invalid`**, `application/problem+json`. CSRF doğrulaması gateway'e geçmeden yapılır; upstream'e oturum/CSRF çerezi taşınmaz.
  - **Kilit:** Başarısız giriş denemeleri veritabanında tutulur; 15 dk içinde 5 başarısız denemede **normalize identifier (kullanıcı adı veya e-posta) + IP** 15 dk kilitlenir. Kilitliyken doğru parola da generic hata döner; başarılı giriş deneme sayacını temizler.
- **REQ-3 (JWT, JWKS ve `hotel-api` Admin Yüzeyi — `CLARIFY-1A`, `CLARIFY-3A`, `CLARIFY-8A`):** Admin-api **RS256** imzalı, kısa ömürlü (**5 dk**) JWT üretmeli; `iss=admin-api`, `aud=hotel-admin-api`, `sub`, `roles` (array), `iat`, `exp`, `jti` ve `kid` taşımalıdır. JWT her istekte yeniden üretilir; cache'lenmez. JWKS `GET /admin-api/.well-known/jwks.json` altında public anahtarları yayınlar; özel anahtar yalnız admin-api'de yaşar. Local/CI'da ilk açılışta üretilen aktif özel anahtar git'e girmeyen `ADMIN_JWT_ACTIVE_KEY_PATH` dosyasında saklanır; prod'da PEM/keystore env veya secret manager'dan gelir. Rotasyon için `ADMIN_JWT_PREVIOUS_PUBLIC_KEY_PATH` opsiyoneldir; JWKS aktif ve varsa önceki public anahtarı `kid` ile yayınlar; önceki anahtar **5 dk + 60 sn clock skew (6 dk)** sonra runbook ile kaldırılır. `hotel-api` `HOTEL_ADMIN_JWKS_URI` (local varsayılan `http://localhost:8082/admin-api/.well-known/jwks.json`) üzerinden JWKS'i önbellekler ve bilinmeyen `kid`de yeniler. `hotel-api` `/admin-api/**` yüzeyinde imza, `aud`, `exp`, `iss` ve rol doğrulaması yapmalı; eksik/geçersiz/süresi geçmiş/yanlış audience JWT **401**, yetersiz rol **403** ve mevcut RFC 7807 zarfı dönmelidir. Rol zorlaması, üretimde ek probe/scaffold açılmadan test kaynaklarındaki bir test controller'ı ile kanıtlanmalıdır.
- **REQ-4 (Gateway Pass-through ve Kimlik Filtresi — `CLARIFY-5A`, `CLARIFY-8A`):** Oturum doğrulama ve JWT üretimi uygulama kökündeki kimlik filtresinde yapılmalı; **sorumluluk: kök kimlik filtresi JWT'yi ekler, gateway yalnız taşır.** Filtre oturumu `adminidentity.api` üzerinden doğrulamalı ve JWT'yi iliştirmelidir. `gateway` yalnız pass-through kalmalı; gelen admin isteğini Bearer JWT ile `hotel-api` admin yüzeyine iletmeli, timeout ve hata eşlemesi dışında mantık eklememelidir. Gateway, istemcinin gönderdiği `Cookie` (oturum + CSRF), `Authorization`, `X-Request-Id` ve iç kimlik başlıklarını upstream'e **taşımamalı/ezmeli**; `X-Request-Id` gateway tarafından üretilir/doğrulanır ve upstream'e yalnız gateway'in değeri yazılır; `Origin`/`Referer` gereksizse taşınmaz. Bu dilimde `gateway → adminidentity` modül kenarı açılmaz; `docs/architecture.md` §4'teki "gateway kimlik iliştirir" ifadesi "kimlik kök filtrede iliştirilir; gateway taşır" diye netleştirilir. Gateway, mock upstream ile IT'de doğrulanır; gerçek admin iş uçları gelene kadar E2E kapsamına girmez.
- **REQ-5 (Audit — `CLARIFY-6A`):** Giriş başarılı, giriş başarısız, çıkış, login rotasyonu ve oturum iptali olayları `adminaudit` tarafından kaydedilmelidir. Kayıt; aktör (biliniyorsa), denenen kullanıcı adı, sonuç, neden kodu, IP, User-Agent, `requestId` ve zaman içermelidir; parola, oturum kimliği, JWT veya istek gövdesi asla kaydedilmemelidir. Kayıtlar 90 gün saklama hedefine tabidir; otomatik silme bu dilimde kapsam dışıdır ve operasyonel runbook'a bağlanır. Audit yazımı kimlik transaction'ından ayrı kendi transaction'ında yapılmalıdır. Giriş başarısında sıra: oturum tx A commit → audit tx B; audit yazılamazsa **oturum iptal edilir** ve `500 system.internal_error` döner. Başarısız girişte audit yazılamazsa `500 system.internal_error` döner; oturum açılmaz. Çıkış/iptal audit hatası loglanır, işlem sürer. Bootstrap oluşturma ve skip olayları audit'e yazılır (`BOOTSTRAP_CREATED` / `BOOTSTRAP_SKIPPED`); skip audit hatası loglanır, işlemi durdurmaz. Bu dilimde `adminidentity → adminaudit` senkron kenarı onaylanır; `docs/architecture.md` §4 allowlist'i ve `AdminApiModularityTest` buna göre güncellenir. IP, yalnız `ADMIN_TRUSTED_PROXY_CIDRS` tanımlıysa ve doğrudan bağlantı bu CIDR'lerdeyse `X-Forwarded-For`'un ilk güvenilmeyen değerinden; aksi halde doğrudan bağlantı adresinden alınır.
- **REQ-6 (Admin-web Giriş, Oturum Kapısı ve Scaffold Kaldırma — `CLARIFY-9A`, `CLARIFY-11A`):** Admin-web'de TR giriş sayfası (`/login`) bulunmalı; giriş formu **yükleme** ve **hata** durumlarını göstermeli, boş durum yoktur. Login sayfası yüklenirken CSRF token'ı **client-side** alınmalı (Next server-side fetch'in `Set-Cookie`'si tarayıcıya taşınmaz). Hatalar kullanıcı dostu ve generic olmalı; teknik detay sızdırmamalıdır. Korumalı sayfalar, sunucu tarafında oturum doğrulanmadan render edilmemeli; `/me` çağrısı gelen oturum çerezi ile yapılmalı, geçersiz/expired oturumda doğrudan giriş sayfasına yönlendirilmeli ve korumalı içerik görünmemelidir. Çıkış, oturumu iptal edip giriş sayfasına dönmelidir. Admin-web API client'ı state-changing isteklerde `X-XSRF-TOKEN` taşımalı; 401'de girişe yönlendirmeli, **403 `security.csrf_invalid` durumunda token'ı yeniden çekip kullanıcıya otomatik tekrar deneme sunmalıdır** (bayat token senaryosu). Admin-web sade kalır; 0003 tasarım sistemi uygulanmaz. `admin-api` scaffold ucu ve admin-web "sistem durumu" kartı, i18n anahtarları ve testleri kaldırılır. `0002`'nin canlı admin-web üç durum smoke testi, `0004` login smoke'u ile değiştirilir; `0002`/`0003` arşiv kanıtları değiştirilmez.
- **REQ-7 (Testler, Kanıtlar, Doküman ve CI — tüm kararlar):** Admin-api için birim + Testcontainers IT; `hotel-api` için JWT doğrulama IT'leri; admin-web için Vitest + Testing Library + MSW; E2E için `@admin` etiketli giriş/çıkış/korumalı sayfa testleri yazılmalıdır. Güvenlik negatif testleri zorunludur: yanlış parola, kilitlenme, kullanıcı numaralandırması, geçersiz/expired/yanlış audience JWT, yetersiz rol, süresi dolmuş oturum, geçersiz oturum çerezi, CSRF'siz/yanlış token/yanlış Origin, audit fail-closed ve bootstrap guard. Kanıtlar `specs/done/evidence/0004/` altına yazılmalı; `0001`/`0002`/`0003` arşivleri değişmemelidir. Doküman/CI etkisi §3'te listelenmiştir.

### 2.1 Admin API Sözleşmesi (`Q10=A`)

| Uç | Kimlik | CSRF | Başarı | Hata |
|---|---|---|---|---|
| `POST /admin-api/auth/login` | anonim | gerekli | `204` + `Set-Cookie` (`ADMIN_SESSION`, `XSRF-TOKEN`) | `401 security.invalid_credentials` (kilit dahil generic), `403 security.csrf_invalid` |
| `GET /admin-api/auth/csrf` | anonim | — | `200` + `XSRF-TOKEN` çerezi | — |
| `POST /admin-api/auth/logout` | oturum | gerekli | `204` + çerez temizliği | `401 security.unauthorized`, `403 security.csrf_invalid` |
| `GET /admin-api/auth/me` | oturum | — | `200 {id, username, email, role}` | `401 security.unauthorized` |
| `GET /admin-api/.well-known/jwks.json` | anonim | — | `200` JWKS | — |
| `/admin-api/**` (gateway) | oturum | state-changing'de | upstream yanıtı | `401`, `403`, timeout |

- **Login gövdesi:** `identifier` (kullanıcı adı **veya** e-posta, case-insensitive) + `password`; hata mesajları generic.
- **Çerezler:** `ADMIN_SESSION` `HttpOnly`; `XSRF-TOKEN` okunabilir (double-submit); ikisi de `SameSite=Lax`, `Path=/`, prod'da `Secure` (local'de env ile kapalı). Oturum çerezi `Max-Age=28800` (8 saat mutlak). Login öncesi CSRF çerezi **15 dk** TTL taşır; login sonrası rotasyonla oturum ömrüne bağlanır (Max-Age=28800).
- **CSRF:** `X-XSRF-TOKEN` başlığı; Origin allowlist'i local `http://localhost:3001`, prod admin alan adı; Origin yoksa/izinsizse veya token eksik/geçersizse **403 `security.csrf_invalid`**. Token login sonrası döner, logout'ta temizlenir.
- **JWT claim'leri:** `iss=admin-api`, `aud=hotel-admin-api`, `sub` (admin UUIDv7), `roles` (array), `iat`, `exp` (5 dk), `jti`; header `kid`.
- **Gateway dışlamaları:** `/admin-api/auth/**`, `/admin-api/.well-known/**`, `/actuator/health`, `/error`.
- **Anonim uçlar:** Yalnız login, CSRF token, JWKS, health ve `/error` (container error dispatch) anonimdir; logout, `/me` ve diğer admin yüzeyi oturum gerektirir. `admin-api` güvenlik zincirinde bu uçlar `permitAll`, diğerleri kimlik doğrulamalıdır.
- **Header hijyeni:** Gateway upstream'e `Cookie` taşımaz; istemci `Authorization` ve iç kimlik başlıklarını ezer/siler; `X-Request-Id` gateway tarafından üretilir/doğrulanır ve upstream'e yalnız gateway'in değeri yazılır; `Origin`/`Referer` gereksizse taşınmaz.
- **Hata zarfı:** Tüm hatalar RFC 7807 (`application/problem+json`), `X-Request-Id` ve stack trace/iç sınıf adı içermez; kodlar `security.*` / `system.*` uzayındadır.
- **`/me`:** `{id, username, email, role}`; `role` tek değer, JWT `roles` her zaman array'dir.

### 2.2 Sözleşme Detayları (QA B-01…B-17)

**2.2.1 Kimlik ve parola (B-01, B-02)**
- **Kullanıcı adı:** 3–40 karakter; `^[a-z0-9][a-z0-9._-]{2,39}$`; girişte küçük harfe normalize edilir; benzersizlik `lower(username)` üzerinde DB unique index ile sağlanır.
- **E-posta:** en fazla 254 karakter, RFC 5322 biçimi; küçük harfe normalize edilir; benzersizlik `lower(email)` üzerinde DB unique index ile sağlanır.
- **Pasif hesap:** `active=false` olan kullanıcı giriş yapamaz; doğru parola verilse bile `401 security.invalid_credentials` generic yanıtı döner. Pasifleştirilen kullanıcının mevcut oturumları bir sonraki doğrulamada geçersizdir (kullanıcı yönetimi 0004 kapsamı dışındadır; kural hazırdır).
- **Parola:** NFC normalize edilmiş **12–128 Unicode code point** ve en fazla **512 UTF-8 byte**; kompozisyon kuralı yok; parola, kullanıcı adını veya e-postayı (case-insensitive, tam veya local-part) içeremez.
- **Hash:** Argon2id; bellek **19.456 KiB**, iterasyon **2**, paralellik **1**, salt **16 B**, çıktı **32 B**, version **0x13**. Parametreler kodda tek yerde tanımlıdır; testler hash'in düz metin olmadığını ve doğrulama maliyetinin bilinmeyen kullanıcıda da korunduğunu doğrular.
- **Generic hata:** Kullanıcı yok, parola yanlış, pasif hesap ve kilitli hesap aynı `401 security.invalid_credentials` gövdesini döner; yanıt süresi belirgin fark üretmez.

**2.2.2 Kilit (B-03)**
- **Pencere:** Kayan **15 dk**; anahtar **normalize identifier (kullanıcı adı veya e-posta) + istemci IP**'sidir. E-posta ile giriş de aynı sayaç anahtarını kullanır.
- **Tetik:** 5. **başarısız** doğrulamada kilit **15 dk** başlar; ilk 4 hata yalnız 401 döner. 5. denemede parola doğruysa giriş başarılı olur ve sayaç temizlenir.
- **Kilitliyken:** Doğru parola da generic 401 döner; kilitli denemeler sayılmaz ve kilidi uzatmaz.
- **Temizlik:** Başarılı giriş, o identifier+IP için pencereyi temizler. Denemeler DB'de tutulur; restart sonrası korunur. 15 dk'dan eski denemeler sorgu dışıdır; fiziksel temizlik operasyonel runbook'a bağlıdır.

**2.2.3 Oturum (B-04, B-05)**
- **Token:** 32 byte CSPRNG, base64url (padding'siz, 43 karakter); cookie değeri budur. DB'de yalnız **SHA-256** özeti saklanır; karşılaştırma sabit zamanlıdır.
- **Çoklu oturum:** Bir kullanıcı birden çok oturum açabilir. Login sırasında mevcut oturum çerezi varsa yalnız o oturum iptal edilir (rotasyon); diğer oturumlar sürer. Logout yalnız mevcut oturumu iptal eder.
- **Sınırlar:** Idle süre `last_seen_at + 30 dk`; mutlak süre `created_at + 8 saat`. `now >= idle_expiry` veya `now >= absolute_expiry` ise oturum geçersizdir (eşitlik geçersiz). `last_seen_at` her başarılı kimlik doğrulamalı istekte güncellenir.
- **Cookie:** `ADMIN_SESSION` `HttpOnly`, `SameSite=Lax`, `Path=/`, prod'da `Secure`; `ADMIN_COOKIE_SECURE` env'i local'de `false`, prod'da `true` varsayılanıyla; `Max-Age=28800`. Mutlak süre dolduğunda çerez de geçersizdir; idle süre dolduğunda oturum reddedilir.

**2.2.4 CSRF ve hata önceliği (B-06, B-07)**
- **Login öncesi token:** Stateless HMAC-SHA256 imzalı token (`base64url(payload).base64url(sig)`); payload `iat`, `exp` (**15 dk**), `nonce`; `ADMIN_CSRF_SECRET` (en az 32 byte) ile imzalanır; DB'de saklanmaz. Cookie `XSRF-TOKEN` okunabilir; `SameSite=Lax`, `Path=/`, prod'da `Secure`.
- **Login sonrası token:** Rotasyonla yeni token üretilir; payload'a `sid` eklenir, `exp` oturum mutlak süresine bağlanır (`Max-Age=28800`). Doğrulama imza + süre + `sid` eşleşmesini kontrol eder. Logout çerezi temizler.
- **Secret:** `ADMIN_CSRF_SECRET` local'de açıkça işaretli varsayılan, prod'da env/secret manager; secret rotasyonu login öncesi token'ları geçersiz kılar (kabul edilen davranış).
- **Origin:** `ADMIN_ALLOWED_ORIGINS` virgülle ayrılmış allowlist; local varsayılan `http://localhost:3001`; prod'da zorunlu. State-changing istekte Origin yok veya izinsizse **403 `security.csrf_invalid`**; `Referer` tek başına kabul edilmez.
- **Hata önceliği:** Tüm state-changing isteklerde **CSRF/Origin → 403**; ardından **400 validation → 401 session → 403 role → 500 audit**. Login'de oturum/rol adımları yoktur: CSRF/Origin → 403, validation → 400, credentials → 401, audit → 500. Read-only: **401 → 403 → 500**.
- **Retry:** Admin-web 403 `security.csrf_invalid` sonrası token'ı bir kez yeniler ve isteği **bir kez** otomatik tekrarlar; ikinci 403'te generic hata gösterir.

**2.2.5 JWT ve JWKS (B-08, B-09)**
- **Anahtar:** RSA **2048 bit**; özel anahtar PKCS#8 PEM, public anahtar SPKI PEM. `kid`, RFC 7638 JWK thumbprint (SHA-256, base64url) olarak üretilir.
- **JWKS:** `{keys:[{kty:"RSA", use:"sig", alg:"RS256", kid, n, e}]}`; `n`/`e` base64url unsigned. Özel anahtar/JWK asla yayınlanmaz.
- **Cache:** `hotel-api` JWKS'i **5 dk** önbellekler. Bilinmeyen `kid` tek bir yenileme tetikler; 60 sn cooldown vardır (stampede koruması).
- **Erişilemezlik:** Geçerli önbellek varsa doğrulama sürer; önbellek yoksa ve JWKS alınamıyorsa **503 `system.http_503`** döner (401 değil). 401 yalnız token eksik/geçersiz olduğunda kullanılır.
- **Clock skew:** `exp` için **60 sn** leeway; `iat` gelecekte 60 sn'den fazla olamaz. `roles` array'dir; V1'de rol hiyerarşisi yoktur, her uç izinli rolleri açıkça belirtir.

**2.2.6 `hotel-api` 401/403 ve rol matrisi (B-10)**
- Eksik/geçersiz imza, yanlış `aud`/`iss`, süresi geçmiş token → **401 `security.unauthorized`**; yetersiz rol → **403 `security.forbidden`**; her ikisi RFC 7807 + `X-Request-Id`.
- Test controller matrisi: `/admin-api/test/authenticated` tüm roller; `/admin-api/test/manager` `MANAGER` + `SUPER_ADMIN`; `/admin-api/test/super-admin` yalnız `SUPER_ADMIN`. `FRONT_DESK` manager/super-admin uçlarında 403 alır. Üretimde bu uçlar yoktur.

**2.2.7 Gateway durum/header sözleşmesi (B-11)**
- **Timeout:** Bağlantı + okuma toplamı **3 sn**; `ADMIN_GATEWAY_TIMEOUT_MS=3000` ile yapılandırılır.
- **Eşleme:** Timeout → **504 `system.gateway_timeout`**; connection refused/DNS → **502 `system.gateway_unavailable`**; upstream 4xx/5xx durum ve gövdesi sanitize edilerek korunur (`Set-Cookie`, `Server` strip); upstream 401/403 aynen iletilir. Path/query/body aynen iletilir.
- **İstek allowlist:** `Accept`, `Content-Type`, `Content-Length` (gateway yeniden hesaplar) ve gateway'in ürettiği `X-Request-Id` iletilir; `Cookie`, `Authorization`, `Origin`, `Referer`, `Host`, `X-Forwarded-*` ve diğer `X-*` başlıkları upstream'e taşınmaz. İstemcinin `X-Request-Id` değeri yok sayılır.
- **Yanıt allowlist:** `Content-Type` ve gateway'in `X-Request-Id` değeri istemciye taşınır; upstream `Set-Cookie` ve `Server` strip edilir; diğer upstream başlıkları filtrelenir.

**2.2.8 Audit tablosu, kodlar ve proxy (B-12, B-13)**
- **Tablo:** `admin_audit`; `id` UUIDv7, `occurred_at` timestamptz, `event_type`, `actor_user_id` (nullable), `username_attempted` (nullable), `result` (`SUCCESS`/`FAILURE`), `reason_code` (nullable), `ip_address`, `user_agent` (≤512), `request_id`, `metadata` (jsonb, opsiyonel). `actor_user_id` **FK değildir**; modüller arası FK yasaktır.
- **Reason-code seti:** `LOGIN_SUCCESS`, `LOGIN_INVALID_CREDENTIALS`, `LOGIN_LOCKED`, `LOGIN_INACTIVE`, `LOGOUT`, `SESSION_REVOKED`, `BOOTSTRAP_CREATED`, `BOOTSTRAP_SKIPPED`; `event_type` bunlardan türetilir (`LOGIN`, `LOGOUT`, `SESSION_REVOKED`, `BOOTSTRAP`). Login rotasyonunda iptal edilen eski oturum `SESSION_REVOKED`, logout `LOGOUT` olarak yazılır. Skip yolunda `BOOTSTRAP_SKIPPED` (`SUCCESS`), oluşturmada `BOOTSTRAP_CREATED` yazılır.
- **Sıra:** Başarılı girişte oturum tx A commit → audit tx B; audit başarısızsa oturum iptal + 500. Başarısız girişte audit başarısızsa 500. Logout/iptal audit hatası loglanır, işlem sürer; bootstrap oluşturma audit hatası fail-fast (exit 4), skip audit hatası loglanır ve exit 0 korunur.
- **Proxy:** `ADMIN_TRUSTED_PROXY_CIDRS` virgülle ayrılmış IPv4/IPv6 CIDR listesi. Doğrudan bağlantı bu listedeyse `X-Forwarded-For` sağdan sola ayrıştırılır, güvenilen proxy'ler atlanır ve ilk güvenilmeyen IP alınır; değilse doğrudan bağlantı IP'si kullanılır. Bozuk XFF'te doğrudan IP kullanılır ve uyarı loglanır. IP spoofing'e izin verilmez.

**2.2.9 Bootstrap (B-14)**
- Ayrı CLI/init job: `java -jar admin-api.jar --bootstrap-admin` (runbook'ta belgelenir); startup'ta çalışmaz; CLI modunda web server başlatılmaz ve süreç exit koduyla kapanır. Env: `ADMIN_BOOTSTRAP_USERNAME`, `ADMIN_BOOTSTRAP_EMAIL`, `ADMIN_BOOTSTRAP_PASSWORD`.
- **Koşul:** `admin_users` tablosunda **en az bir aktif SUPER_ADMIN** varsa env kontrolünden önce skip + uyarı, **exit 0**; skip kararı DB'den alınır, DB erişilemezse exit 3; skip yolunda audit `BOOTSTRAP_SKIPPED` (`SUCCESS`) yazılır, audit yazılamazsa uyarı loglanır ve exit 0 korunur. Aktif SUPER_ADMIN yokken herhangi bir admin satırı (pasif SUPER_ADMIN, yalnız MANAGER veya FRONT_DESK) varsa **fail-fast exit 2** (manuel karar). Hiç admin yoksa env'den güçlü parola ile aktif `SUPER_ADMIN` oluşturur; audit `BOOTSTRAP_CREATED` yazar.
- **Exit kodları:** 0 oluşturuldu/skip; 2 eksik/zayıf env veya aktif SUPER_ADMIN'siz mevcut admin; 3 DB hatası (skip sorgusu veya admin insert); 4 oluşturma audit hatası (admin insert sonrası audit yazımı başarısız; skip audit hatası loglanır, exit 0 kalır). Oluşturma audit başarısızsa admin kaydı **rollback/delete** edilir; **pending/pasif SUPER_ADMIN bırakılmaz, aktif admin oluşmaz**. Parola/secret loglanmaz.
- **Eşzamanlılık:** Aynı `ADMIN_BOOTSTRAP_USERNAME` ile iki CLI aynı anda çalışırsa unique çakışması oluşur; çakışma sonrası aktif SUPER_ADMIN varsa `BOOTSTRAP_SKIPPED` audit'i ile **exit 0**, yoksa **exit 2**. Testte tek aktif SUPER_ADMIN kalır; exit 0/2 kabul edilir.

**2.2.10 Local/CI seed (B-15)**
- `db/seed/adminidentity/R__seed_admin_dev.sql` repeatable seed; `hotel_admin` `local-ci-seed` marker'ı; `-Pseed -Dseed.env=local|ci` profili; prod'da konum yok/guard fail-fast.
- Sabit local-only parolalar için **önceden hesaplanmış Argon2id hash'leri** SQL'de tutulur; `.env.example` aynı parolaları E2E için listeler (hash üretmez). Üç hesap: `superadmin` (`SUPER_ADMIN`), `manager` (`MANAGER`), `frontdesk` (`FRONT_DESK`); hepsi aktif.
- Idempotency: `INSERT ... ON CONFLICT (lower(username)) DO NOTHING`; mevcut kullanıcının parolası/rolü değiştirilmez. Seed iki kez çalıştırıldığında üç kayıt kalır. Parola değiştirilirse Argon2id hash'leri yeniden üretilir; varsayılan local değerler sabittir.

**2.2.11 Admin-web sözleşmesi (B-16)**
- **Testid'ler:** `admin-login-page`, `admin-login-form`, `admin-login-identifier`, `admin-login-password`, `admin-login-submit`, `admin-login-loading`, `admin-login-error`, `admin-login-retry`, `admin-home-page`, `admin-home-error`, `admin-home-retry`, `admin-user-menu`, `admin-logout`.
- **CSRF ve `/me`:** Login sayfası CSRF token'ını client-side `credentials: same-origin` ile alır; korumalı sayfa server-side `/me` çağrısını `ADMIN_API_INTERNAL_URL` ve gelen Cookie ile yapar.
- **CSRF fetch hatası:** Login sayfası CSRF token'ını alamazsa `admin-login-error` + `admin-login-retry` gösterilir; kullanıcı tekrar deneyebilir, form submit edilmez.
- **returnTo:** Yalnız `/` ile başlayan göreli yol; `//`, şema veya host içeren değerler reddedilir; yoksa `/`. Login sonrası güvenli `returnTo`'ya, yoksa ana sayfaya gidilir.
- **`/me` 5xx/ağ hatası:** Girişe yönlendirilmez; `admin-home-error` + `admin-home-retry` gösterilir. Yalnız 401 login'e yönlendirir.
- **CSRF retry:** 403 `security.csrf_invalid` → token yenile + bir kez otomatik tekrar; ikinci 403'te `admin-login-error`/generic hata.
- **Logout hatası:** 401 → login'e yönlendir; 5xx/ağ → oturum açık kalır, hata + retry gösterilir; başarılı logout'ta login'e dönülür.

**2.2.12 CI/E2E sırası ve 0002 geçişi (B-17)**
- CI: local RSA anahtarı üret → her iki DB migrate → admin seed (§2.2.10 SQL repeatable) → backend/frontend start → 0002 web `@empty` (1/1) → catalog seed → 0002 `@seeded` (2/2) → 0003 `@seeded` design (40/40) → `@admin` (0004: (a) başarılı login + korumalı sayfa, (b) başarısız login generic hata, (c) logout + login'e dönüş; 3/3) → 0004 evidence.
- **0002 canlı test değişikliği:** `0002` admin-web üç-durum smoke'u kaldırılır; yerine 0004 `@admin` login/logout smoke'u gelir. 0002 canlı web smoke toplamı **3/3** olur (1 empty + 2 seeded); 0003 design **40/40** korunur; 0002 arşiv kanıtları (8 PNG) değişmez. `@admin` testleri admin seed'den sonra koşar; 0004 kanıtları `specs/done/evidence/0004/` altına yazılır.

## 3. Constraints (Kapsam Dışı Dahil)

- **Teknik/İş kısıtları:**
  - **Kapsam:** Değişiklik `admin-api`, `hotel-api` (yalnız JWT doğrulama/filtre), `admin-web`, `e2e`, `docs`, `infra` ve `.env.example` ile sınırlıdır. İş modülleri (`catalog`, `pricing`, `media`, `booking`, `payment`, `notification`, `reporting`) değişmez; admin iş ekranları açılmaz.
  - **Veritabanı ve modül sınırı:** `admin_api` yalnız `hotel_admin`'e erişir; `hotel_core`'a erişemez. `admin_platform` şemasında V2 admin kullanıcı/oturum/deneme tablolarını, V3 audit tablosunu oluşturur. `adminidentity → adminaudit` senkron kenarı dışında yeni modül kenarı yoktur; `gateway → adminidentity` yoktur. `AdminApiModularityTest` kapalı allowlist'i bu kararlara göre güncellenir.
  - **Anahtar yönetimi:** Local/CI anahtarı git'e girmez; `ADMIN_JWT_ACTIVE_KEY_PATH` `.gitignore`'a eklenir; prod anahtarı env/secret manager'dadır. `ADMIN_JWT_ACTIVE_KEY_PATH` aktif özel anahtar, `ADMIN_JWT_PREVIOUS_PUBLIC_KEY_PATH` opsiyonel önceki public anahtar; önceki anahtar **6 dk** (5 dk TTL + 60 sn skew) JWKS'te kalır. Repoda gerçek sır bulunmaz; local varsayılanlar açıkça işaretlidir (`docs/conventions.md` §2.4). Özel anahtar `hotel-api`'ye verilmez.
  - **Parola, oturum ve kilit:** Argon2id; en az 12 / en fazla 128 karakter; kullanıcı numaralandırması yok; 15 dk içinde 5 başarısız denemede **normalize identifier (kullanıcı adı veya e-posta) + IP** için 15 dk kilit; denemeler DB'de tutulur, restart'a dayanır. Oturum 30 dk idle + 8 saat mutlak; `last_seen_at` her doğrulanmış istekte güncellenir; JWT 5 dk; girişte oturum rotasyonu; çıkış mevcut oturumu iptal eder.
  - **CSRF (`Q10=A`):** `docs/architecture.md` §6 korunur; double-submit + Origin zorunludur. CSRF doğrulaması gateway öncesinde yapılır; upstream'e çerez taşınmaz.
  - **Audit:** IP/UA tam saklanır; 90 gün hedefi; otomatik silme bu dilimde yoktur. Giriş başarısında audit tx B başarısızsa oturum iptal edilir ve 500 döner; başarısız girişte audit yazılamazsa 500 döner; çıkış/iptal audit hatası loglanır, işlem sürer; bootstrap oluşturma audit hatası fail-fast, skip audit hatası loglanır ve exit 0 kalır. Parola/oturum/JWT/istek gövdesi audit'e yazılmaz. IP yalnız `ADMIN_TRUSTED_PROXY_CIDRS` varsa ve doğrudan bağlantı bu CIDR'lerdeyse `X-Forwarded-For` ilk güvenilmeyen değerinden alınır.
  - **Bootstrap:** Startup'ta değil, ayrı CLI/init job; env `ADMIN_BOOTSTRAP_USERNAME/EMAIL/PASSWORD`; CLI modunda web server başlatılmaz; yalnız aktif SUPER_ADMIN varsa skip + uyarı (exit 0); skip audit `BOOTSTRAP_SKIPPED`, audit yazılamazsa uyarı + exit 0; aktif SUPER_ADMIN yokken herhangi bir admin satırı (pasif SUPER_ADMIN, MANAGER veya FRONT_DESK) varsa fail-fast (exit 2); env eksik/zayıfsa fail-fast (exit 2); aynı `ADMIN_BOOTSTRAP_USERNAME` ile eşzamanlı unique çakışması aktif SUPER_ADMIN varsa exit 0, yoksa exit 2; oluşturma audit yazılamazsa kayıt **rollback/delete**, pending/pasif SUPER_ADMIN yok, aktif admin yok (exit 4). Restart uygulamayı düşürmez.
  - **Admin-web:** TR, sade; 0003 tasarım sistemi uygulanmaz; yeni UI/i18n kütüphanesi eklenmez. Login ekranında üç-durum kuralı yükleme + hata olarak uygulanır; boş durum yoktur. CSRF token'ı login sayfasında client-side alınır; server-side `/me` çağrısı gelen çerezi iletir.
  - **Arşivler:** `specs/done/evidence/0001|0002|0003/` değişmez. `0002`'nin canlı admin-web smoke testi 0004 login smoke'u ile değiştirilir; bu bilinçli bir canlı test değişikliğidir, arşiv kanıtı değildir.
- **Kapsam dışı (bilinçli olarak yapılmayacaklar):**
  - Admin iş ekranları: katalog, fiyat, medya, rezervasyon yönetimi; `preferences` modülü.
  - Kullanıcı yönetimi CRUD'u (listeleme/oluşturma/pasifleştirme) ve tüm oturumları iptal etme; ayrı spec.
  - Parola sıfırlama e-postası, parola değiştirme ekranı, 2FA, OAuth/SSO, müşteri kimliği (V2 `identity`).
  - Admin-web tasarım sistemi, karanlık tema, analitik.
  - Gateway üzerinden gerçek admin iş uçları; gerçek iş uçları gelene kadar gateway yalnız IT ile doğrulanır.
  - Global “tek aktif SUPER_ADMIN” invariantı ve farklı `ADMIN_BOOTSTRAP_USERNAME` değerleriyle paralel bootstrap 0004 kapsamı dışıdır; 0004 yalnız “en az bir aktif SUPER_ADMIN” bootstrap guard'ı sağlar. Yeni DB kısıtı, partial unique index veya advisory lock eklenmez.
  - Otomatik audit temizleme job'u ve eski login denemelerinin fiziksel temizliği (90 gün hedefi operasyonel runbook'a bağlıdır).
- **Kabul edilen risk/ödünç:**
  - **İlk giriş parola değişikliği yok:** Prod bootstrap parolası secret manager'da güçlü tutulmalıdır; self-service parola değiştirme sonraki dilimdir.
  - **Audit/deneme temizliği:** 90 gün audit hedefi ve eski login denemeleri otomatik job olmadan operasyonel disipline bağlıdır.
  - **Gateway E2E yok:** Gateway IT ile mock upstream üzerinden doğrulanır; gerçek uçlar gelene kadar uçtan uca gateway testi yoktur.
  - **0002 canlı test değişikliği:** Scaffold kaldırıldığı için 0002 admin-web smoke testi değişir; arşiv kanıtları korunur.
- **Doküman ve CI Etkisi (Developer):**
  1. **`docs/architecture.md` §4/§6:** `adminidentity → adminaudit` allowlist kenarı; §4'ün "gateway kimlik iliştirir" cümlesinin kök filtreye göre netleştirilmesi; Q10=A kararı §6'yı değiştirmez; §6'ya operasyonel ayrıntılar (JWKS yolu, TTL'ler, anahtar yönetimi/rotasyon, oturum çerezi, CSRF double-submit + Origin ve gateway header hijyeni) eklenir.
  2. **`docs/frontend.md`:** admin-web login için TR, yükleme + hata (boş yok) istisnası; CSRF token'ının client-side alınması; server-side `/me` kapısı; 0003 tasarım sisteminin admin-web'e uygulanmadığı notu.
  3. **`infra/README.md` ve `e2e/README.md`:** `hotel_admin` seed marker'ı, admin seed/bootstrap komutu, local/CI kimlik bilgileri, login/logout E2E runbook'u ve Windows repackage notu.
  4. **`.env.example`:** Admin seed/bootstrap, `ADMIN_JWT_ACTIVE_KEY_PATH`, `ADMIN_JWT_PREVIOUS_PUBLIC_KEY_PATH`, `HOTEL_ADMIN_JWKS_URI`, `ADMIN_COOKIE_SECURE`, `ADMIN_CSRF_SECRET`, `ADMIN_ALLOWED_ORIGINS`, `ADMIN_TRUSTED_PROXY_CIDRS`, `ADMIN_GATEWAY_TIMEOUT_MS`, oturum TTL ve seed/E2E parola değişkenleri; local varsayılanlar açıkça işaretli. `.gitignore`'a yerel anahtar dosyası eklenir.
  5. **`.github/workflows/ci.yml`:** Admin seed adımı, admin E2E (`@admin`), JWT/JWKS IT'leri, `0002` smoke'un güncellenmiş hali ve **0003 `@seeded` design 40/40** koruması.
  6. **`specs/done/README.md`:** 0004 kanıt adlandırması (gerekirse) ve arşiv notu.

## 4. Context

- **Mevcut durum:** `admin-api` (`:8082`) içinde `adminidentity`, `adminaudit`, `preferences` ve `gateway` boş paket iskeletidir; yalnız health ve kimliksiz `GET /admin-api/system/sample-items` scaffold ucu vardır. `SecurityConfig` CSRF kapalı, stateless ve yalnız health + scaffold ucunu açık bırakır. `hotel_admin` veritabanında `admin_platform` şeması ve `app` DML izinleri `V1__init_admin_schema.sql` ile hazırdır; kimlik tabloları yoktur. `admin-web` (`:3001`) scaffold "sistem durumu" kartını gösterir; `/admin-api/:path*` rewrite'ı `:8082`'ye gider.
- **`hotel-api` (`:8081`):** `/admin-api/**` zinciri `authenticated()` fakat hiçbir kimlik mekanizması yok; tüm istekler 401 döner. 0004, JWKS tabanlı JWT doğrulamasını bu yüzeye bağlar; mevcut RFC 7807 401/403 zarfı korunur.
- **E2E ve kanıt altyapısı:** 0003'te `E2E_EVIDENCE_DIR` ve ortak helper eklendi; iki aşamalı `@empty` → seed → `@seeded` akışı vardır. 0004 kanıtları `specs/done/evidence/0004/` altına yazılabilir; `0002`/`0003` arşivleri değişmez.
- **Bulgular ve kararlar:**
  - **0002 ↔ 0004 scaffold çelişkisi:** `0002` AC-6 admin-web smoke'unun korunmasını istiyor; `0004` scaffold'u kaldırıyor. `CLARIFY-11A` ile canlı admin-web smoke'u 0004 login smoke'u ile değiştirilecek; arşiv korunacak.
  - **Allowlist:** `AdminApiModularityTest` kapalı allowlist'i boş ve `adminaudit → adminidentity` yönünü açıkça yasaklıyor. `CLARIFY-6A` ile `adminidentity → adminaudit` kenarı onaylandı; `docs/architecture.md` §4 ve test güncellenecek. `CLARIFY-5A` ile `gateway → adminidentity` açılmıyor; kimlik kök filtrede.
  - **Seed marker:** `local-ci-seed` marker'ı şu an yalnız `hotel_core` init'inde; `hotel_admin` seed'i için init'e marker eklenecek.
  - **Q10=A:** Software Architect kararıyla CSRF double-submit + Origin korunuyor; CSRF yalnız tarayıcı oturum çerezi için geçerlidir, servis-servis JWT çağrılarını etkilemez. Sözleşme §2.1'de, uygulama detayları Ek A'dadır.
- **Sonraki dilimler:** `0004` Admin Kimlik ve Oturum → Kullanıcı Yönetimi (ayrı spec) → Admin iş ekranları (medya, fiyat, rezervasyon). **0002 planlama kaydı:** Oda Tipi Detay Sayfası `media` + `pricing` hazır olduğunda ayrı spec'te açılacaktır.
- **Bağımlı spec'ler:** `specs/done/0001-project-architecture.md`, `specs/done/0002-room-catalog.md`, `specs/done/0003-ui-design-system.md`.

## 5. Acceptance Criteria

- **AC-1 (Admin Kimlik Modeli, Parola Politikası, Seed ve Bootstrap — `REQ-1`, `CLARIFY-2A`, `CLARIFY-4A`):**
  - `hotel_admin.admin_platform` şemasında V2 admin kullanıcı/oturum/deneme tabloları, V3 audit tablosu oluşur; JPA entity'leri `@Table(schema = "admin_platform")` taşır; `hotel_core` referansı yoktur.
  - Admin kullanıcı; UUIDv7 kimlik, benzersiz kullanıcı adı (case-insensitive), benzersiz e-posta, Argon2id parola özeti, rol (`SUPER_ADMIN`/`MANAGER`/`FRONT_DESK`), aktiflik ve zaman damgaları taşır. Parola özeti düz metin değildir; Argon2id parametreleri testle doğrulanır.
  - Parola politikası: 12–128 karakter, kompozisyon kuralı yok, kullanıcı adı/e-posta içeremez. Kısa/uzun/aynı kullanıcı adını içeren parolalar reddedilir; hata generic ve kullanıcı numaralandırması yapmaz.
  - Local/CI seed üç rolü temsil eden üç aktif hesap oluşturur; kimlik bilgileri `.env.example`'da local-only işaretli env değerlerinden gelir; aynı seed iki kez çalıştırıldığında mükerrer kayıt/parola değişikliği olmaz. Seed yalnız `local`/`ci` guard'ı ve `hotel_admin` `local-ci-seed` marker'ı ile çalışır; prod'da çalışmaz.
  - Prod bootstrap ayrı CLI/init job'dur; CLI modunda web server başlatılmaz ve süreç exit koduyla kapanır; **yalnız aktif SUPER_ADMIN** varsa env kontrolünden önce skip + uyarı ile exit 0 yapar; skip yolunda `BOOTSTRAP_SKIPPED` audit'i yazılır, audit yazılamazsa uyarı loglanır ve exit 0 korunur; aktif SUPER_ADMIN yokken herhangi bir admin satırı (pasif SUPER_ADMIN, yalnız MANAGER veya FRONT_DESK) varsa fail-fast exit 2; hiç admin yoksa env'den güçlü parola ile bir `SUPER_ADMIN` oluşturur; env eksik/zayıfsa exit 2. Oluşturma audit'e yazılır; audit başarısızsa kayıt **rollback/delete** edilir, **pending/pasif SUPER_ADMIN bırakılmaz**, aktif admin oluşmaz (exit 4). Repoda prod parolası yoktur.
  - Bootstrap eşzamanlılık: Aynı `ADMIN_BOOTSTRAP_USERNAME` ile iki CLI aynı anda çalışırsa unique çakışması oluşur; çakışma sonrası aktif SUPER_ADMIN varsa `BOOTSTRAP_SKIPPED` audit'i ile exit 0, yoksa exit 2; testte tek aktif SUPER_ADMIN kalır.
  - Bootstrap testleri: (a) aktif SUPER_ADMIN varken skip exit 0 + `BOOTSTRAP_SKIPPED`; (b) pasif SUPER_ADMIN varken fail-fast exit 2; (c) yalnız MANAGER/FRONT_DESK varken fail-fast exit 2; (d) oluşturma audit hatasında admin kaydı yoktur ve retry tek aktif SUPER_ADMIN oluşturur; (e) hiç admin yokken geçerli env ile tek aktif SUPER_ADMIN oluşur; (f) env eksik/zayıfken exit 2; (g) DB hatasında exit 3; (h) aynı username ile eşzamanlı iki CLI'da tek aktif SUPER_ADMIN ve exit 0/2; (i) CLI modunda web server portu dinlenmez ve süreç exit koduyla kapanır; (j) skip audit'i yazılamazsa uyarı loglanır, exit 0 ve durum değişmez.
  - **Kanıt:** `AC-1-admin-api-verify.log`, `AC-1-admin-seed-idempotency.log`, `AC-1-bootstrap-cli.log`, `AC-1-bootstrap-superadmin.log`, `AC-1-bootstrap-active-skip.log`, `AC-1-bootstrap-passive-superadmin.log`, `AC-1-bootstrap-audit-fail.log`, `AC-1-bootstrap-exit-codes.log`, `AC-1-bootstrap-concurrent.log`, `AC-1-bootstrap-cli-mode.log`, `AC-1-bootstrap-skip-audit.log`.
  - **Sözleşme:** §2.2.1, §2.2.9 ve §2.2.10'daki kimlik/parola, bootstrap ve seed kuralları test edilir.
- **AC-2 (Oturum, Kilit ve CSRF — `REQ-2`, `CLARIFY-3A`, `Q10=A`):**
  - Başarılı girişte `ADMIN_SESSION` `HttpOnly`, `SameSite=Lax`, `Path=/`, prod'da `Secure` oturum çerezi ve okunabilir `XSRF-TOKEN` çerezi set edilir; yanıt gövdesinde JWT/oturum kimliği yoktur. Geçersiz girişte `401 security.invalid_credentials` generic hata ve RFC 7807 döner.
  - Oturum kimliği yeterli entropiyle üretilir; DB'de yalnız özeti saklanır. Girişte oturum rotasyonu yapılır; eski oturum geçersizdir. `GET /admin-api/auth/me` geçerli `{id, username, email, role}` döner; geçersiz/expired oturum 401 problem+json döner.
  - Oturum 30 dk idle + 8 saat mutlak sonra geçersizdir; `last_seen_at` her doğrulanmış istekte güncellenir; süre enjekte edilen `Clock` ile test edilir, gerçek bekleme yapılmaz. Çıkış mevcut oturumu iptal eder; diğer oturumlar sürer.
  - 15 dk içinde 5 başarısız girişten sonra **normalize identifier (kullanıcı adı veya e-posta) + IP** 15 dk kilitlenir; aynı kullanıcıya e-posta ile giriş de aynı sayacı kullanır; denemeler DB'de tutulur, restart sonrası korunur; kilitliyken doğru parola da generic hata döner; başarılı giriş sayacı temizler. Bilinmeyen kullanıcıda dummy hash çalışır ve yanıt süresi belirgin fark üretmez.
  - **CSRF:** `GET /admin-api/auth/csrf` anonim çağrıyla `XSRF-TOKEN` çerezi döner; login öncesi token **15 dk** TTL taşır. Login dahil tüm state-changing istekler `X-XSRF-TOKEN` + izinli `Origin` taşır. **Hata önceliği:** CSRF/Origin → 403; sonra 400 validation → 401 session → 403 role → 500 audit (login'de oturum/rol adımları yoktur). Eksik/yanlış token veya Origin yok/izinsiz → 403 `security.csrf_invalid` problem+json. Login sonrası token döner, logout'ta temizlenir; CSRF doğrulaması gateway'e geçmeden yapılır.
  - **Kanıt:** `AC-2-login-session.log`, `AC-2-session-expiry.log`, `AC-2-lockout-and-enumeration.log`, `AC-2-csrf.log`, `AC-2-csrf-precedence.log`.
  - **Sözleşme:** §2.2.2–§2.2.4'teki kilit, oturum, CSRF ve hata önceliği test edilir.
- **AC-3 (JWT, JWKS, Anahtar Yönetimi ve `hotel-api` 401/403 — `REQ-3`, `CLARIFY-1A`, `CLARIFY-3A`, `CLARIFY-8A`):**
  - JWT RS256 imzalıdır; `iss=admin-api`, `aud=hotel-admin-api`, `sub`, `roles` (array), `iat`, `exp` (5 dk), `jti` ve `kid` taşır. JWT her istekte yeniden üretilir; cache yoktur. Özel anahtar yalnız admin-api'dedir; `hotel-api` yalnız JWKS public anahtarlarını kullanır.
  - Local/CI'da aktif anahtar `ADMIN_JWT_ACTIVE_KEY_PATH` altında git dışı yerel dosyada saklanır ve restart'ta korunur; prod'da env/secret manager'dan gelir. JWKS `/admin-api/.well-known/jwks.json` altında `kid` ile yayınlanır; `ADMIN_JWT_PREVIOUS_PUBLIC_KEY_PATH` varsa önceki public anahtar da **6 dk** (5 dk TTL + 60 sn skew) yayında kalır; `hotel-api` bilinmeyen `kid`de JWKS'i yeniler.
  - `hotel-api` `HOTEL_ADMIN_JWKS_URI` üzerinden JWKS'i önbellekler; `/admin-api/**` yüzeyinde geçerli JWT ile isteği kabul eder; eksik/geçersiz imza, yanlış `aud`/`iss`, süresi geçmiş JWT **401**; yetersiz rol **403** döner; her iki yanıt RFC 7807 ve `X-Request-Id` taşır, stack trace/iç sınıf adı içermez.
  - Rol zorlaması, `hotel-api` test kaynaklarındaki bir test controller'ı ile IT'de kanıtlanır; üretimde ek probe/scaffold yoktur. JWT tarayıcıya verilmez; admin-web yalnız oturum çerezi kullanır.
  - **Kanıt:** `AC-3-jwt-claims.log`, `AC-3-jwks-rotation.log`, `AC-3-hotel-api-auth.log`.
  - **Sözleşme:** §2.2.5–§2.2.6'daki RSA/JWKS/cache/503/skew/rol matrisi test edilir.
- **AC-4 (Kök Kimlik Filtresi ve Gateway Pass-through — `REQ-4`, `CLARIFY-5A`, `CLARIFY-8A`):**
  - Oturum doğrulama ve JWT üretimi kök kimlik filtresindedir; filtre `adminidentity.api` üzerinden oturumu doğrular. Modül sınıfları kök `config`/`web` paketlerine bağımlı değildir; `AdminApiModularityTest` bu yönü yasaklamaya devam eder.
  - Gateway, kimlik/JWKS uçları hariç `/admin-api/**` isteğini `hotel-api` admin yüzeyine Bearer JWT ile iletir; timeout ve hata eşlemesi dışında mantık içermez. `gateway → adminidentity` modül kenarı yoktur; allowlist bu yönde boş kalır.
  - Gateway IT'de mock upstream ile: oturum çerezi olmayan istek 401; geçerli oturumda Bearer JWT iletilir; istemci `Cookie`, `Authorization`, `X-Request-Id` ve iç kimlik başlıkları upstream'e taşınmaz/ezilir; **X-Request-Id gateway tarafından üretilir/doğrulanır ve upstream'e yalnız gateway'in değeri yazılır**; `Origin`/`Referer` gereksizse taşınmaz; timeout **504 `system.gateway_timeout`**, connection refused **502 `system.gateway_unavailable`**, upstream 4xx/5xx sanitize edilerek korunur; `Set-Cookie`/`Server` strip edilir.
  - `docs/architecture.md` §4'teki "gateway kimlik iliştirir" cümlesi kök filtre yaklaşımına göre netleştirilmiştir; `AdminApiModularityTest` yeşildir.
  - **Kanıt:** `AC-4-gateway-it.log`, `AC-4-header-hygiene.log`, `AC-4-request-id.log`, `AC-4-allowlist.log`.
  - **Sözleşme:** §2.2.7'deki gateway durum/header sözleşmesi test edilir.
- **AC-5 (Audit: Olaylar, Sıra, Fail-closed ve Allowlist — `REQ-5`, `CLARIFY-6A`):**
  - Giriş başarılı, giriş başarısız, çıkış, **login rotasyonu** ve bootstrap oluşturma/skip olayları audit'e yazılır; login rotasyonunda iptal edilen eski oturum `SESSION_REVOKED`, logout `LOGOUT` olarak kaydedilir. Kayıt; aktör (biliniyorsa), denenen kullanıcı adı, sonuç/neden kodu, IP, User-Agent, `requestId` ve zaman içerir; parola, oturum kimliği, JWT veya istek gövdesi yoktur. `reason_code` seti §2.2.8'deki sekiz koddur.
  - Giriş başarısında oturum tx A commit edilir, audit tx B yazılır; audit yazılamazsa oturum iptal edilir ve `500 system.internal_error` döner. Başarısız girişte audit yazılamazsa `500 system.internal_error` döner; oturum açılmaz. Çıkış/iptal audit hatası loglanır, işlem sürer; bootstrap oluşturma audit hatası fail-fast (exit 4), skip audit hatası loglanır ve exit 0 kalır.
  - Audit kayıtları 90 gün saklama hedefine tabidir; otomatik silme bu dilimde yoktur ve runbook'ta belirtilir. IP yalnız `ADMIN_TRUSTED_PROXY_CIDRS` varsa ve doğrudan bağlantı bu CIDR'lerdeyse `X-Forwarded-For` ilk güvenilmeyen değerinden, aksi halde doğrudan bağlantı adresinden alınır; UA başlığından okunur.
  - `adminidentity → adminaudit` senkron kenarı `docs/architecture.md` §4 allowlist'ine ve `AdminApiModularityTest`'e işlenmiştir; `adminaudit → adminidentity` yasağı korunur. Modül sınırı ihlali yoktur.
  - **Kanıt:** `AC-5-audit-events.log`, `AC-5-audit-failclosed.log`, `AC-5-rotation-audit.log`, `AC-5-bootstrap-skip-audit.log`, `AC-5-audit-retention.md`.
  - **Sözleşme:** §2.2.8'deki audit tablo/kod/no-FK/proxy kuralları test edilir.
- **AC-6 (Admin-web Giriş, CSRF, Oturum Kapısı, Çıkış ve Scaffold Kaldırma — `REQ-6`, `CLARIFY-9A`, `CLARIFY-11A`):**
  - `/login` TR giriş sayfası; form yükleme ve hata durumlarını gösterir, boş durum yoktur. Hatalar generic ve kullanıcı dostudur; teknik hata kodu/stack trace görünmez. Login sayfası CSRF token'ını client-side alır; form `X-XSRF-TOKEN` taşır. CSRF token'ı alınamazsa `admin-login-error` + `admin-login-retry` gösterilir; kullanıcı tekrar deneyebilir.
  - Korumalı sayfalar sunucu tarafında `/me` çağrısı ve gelen oturum çerezi ile doğrulanmadan render edilmez; geçersiz/expired oturumda giriş sayfasına yönlendirilir ve korumalı içerik görünmez. Giriş sonrası korumalı ana sayfa açılır; çıkış oturumu iptal eder ve giriş sayfasına döner.
  - Oturum çerezi same-origin rewrite üzerinden taşınır; tarayıcıda JWT/oturum token'ı saklanmaz. Admin-web API client'ı state-changing isteklerde CSRF başlığını taşır; 401'de girişe yönlendirir, **403 `security.csrf_invalid` durumunda token'ı yeniden çekip otomatik tekrar deneme sunar**. Admin-web sade kalır; 0003 tasarım sistemi uygulanmaz.
  - `admin-api` scaffold ucu ve admin-web "sistem durumu" kartı, i18n anahtarları ve testleri kaldırılmıştır; `GET /admin-api/system/sample-items` kimliksiz erişimde 401, kimlikli erişimde 404 döner (scaffold kodu yoktur). `0002` canlı admin-web smoke testi 0004 login smoke'u ile değiştirilmiş, `0002`/`0003` arşiv kanıtları değişmemiştir.
  - **Kanıt:** `AC-6-admin-login-tr.png`, `AC-6-admin-login-error.png`, `AC-6-admin-home-protected.png`, `AC-6-admin-redirect-401.png`, `AC-6-admin-logout.png`, `AC-6-csrf-retry.png`, `AC-6-csrf-fetch-fail.png`, `AC-6-scaffold-removed.log`.
  - **Sözleşme:** §2.2.11'deki testid/CSRF fetch/returnTo/5xx/retry/logout kuralları test edilir.
- **AC-7 (Testler, Kanıtlar, Arşiv İzolasyonu ve CI/Doküman Etkisi — `REQ-7`):**
  - Admin-api birim + Testcontainers IT; login/logout, çerez öznitelikleri, oturum TTL/rotasyon/iptal, kilit, JWT claim'leri, Argon2id, CSRF, audit sırası/fail-closed ve bootstrap guard testleri yeşildir.
  - `hotel-api` JWT IT: geçerli/geçersiz imza, yanlış `aud`, süresi geçmiş, yanlış `iss`, yetersiz rol; 401/403 RFC 7807 zarfı ve `X-Request-Id` doğrulanır.
  - Admin-web Vitest + RTL + MSW: login formu, yükleme, hata, CSRF token'ı, CSRF fetch hatası + retry, 401 yönlendirme, korumalı sayfa, logout, 403 `security.csrf_invalid` sonrası token yenileme/otomatik tekrar deneme. E2E `@admin` (3/3): (a) başarılı login + korumalı sayfa, (b) başarısız login generic hata, (c) logout + login'e dönüş; admin seed aşaması `@admin` öncesinde çalışır; 0002 canlı smoke **3/3** (1 `@empty` + 2 `@seeded`) ve 0003 `@seeded` design **40/40** korunur.
  - Kanıtlar `specs/done/evidence/0004/` altındadır; `0001`/`0002`/`0003` arşivleri SHA-256/`git diff` ile değişmemiştir. CI'da admin seed adımı, admin E2E ve JWT IT'leri çalışır; pipeline yeşildir.
  - Dokümanlar güncellenmiştir: `docs/architecture.md` §4/§6, `docs/frontend.md`, `infra/README.md`, `e2e/README.md`, `.env.example`, `.github/workflows/ci.yml`, `specs/done/README.md` (gerekliyse).
  - **Kanıt:** `AC-7-e2e-admin-summary.md`, `AC-7-ci-run.md`, `AC-7-0003-design-40.txt`, `AC-7-archive-sha256.txt`.
  - **Sözleşme:** §2.2.12'deki CI sırası, 0002 3/3 geçişi ve `@admin` kanıtları doğrulanır.

## 6. Definition of Done

- [ ] Tüm AC'ler doğrulandı (test ve/veya görsel kanıt)
- [ ] Pipeline yeşil (lint + build + test + kritik akış smoke)
- [ ] Kod incelemesi onaylandı (Takım Yöneticisi)
- [ ] Davranış değiştiyse dokümanlar güncellendi
- [ ] Spec `specs/done/` klasörüne taşındı, görsel kanıtlar eklendi
- [ ] SCORECARD dolduruldu

## 7. SCORECARD

- **Spec revizyon sayısı:** 7
- **Düzeltme turu sayısı:**
- **Bulgu gerçek/gürültü oranı:**
- **Regresyon sayısı:**
- **Kaçan hata (escaped bug):**

## 8. Ek A — Q10 Kararı (A) ve CSRF Uygulama Notları

> **Karar (2026-10-28):** Q10 = **A** — CSRF double-submit + Origin korunur; `docs/architecture.md` §6 kararı aynı kalır, yalnız operasyonel ayrıntılar §6'ya eklenir. Aşağıdaki notlar sözleşmenin uygulama ayrıntısıdır.

- **Token edinme:** `GET /admin-api/auth/csrf` oturum gerektirmez; login sayfası yüklenirken **client-side** çağrılır. Next server-side fetch'in `Set-Cookie`'si tarayıcıya taşınmadığı için bu adım client-side olmalıdır.
- **Çerez ayrımı:** Oturum çerezi `ADMIN_SESSION` `HttpOnly`; CSRF çerezi `XSRF-TOKEN` okunabilir. İkisi de `SameSite=Lax`, `Path=/`, prod'da `Secure` (local'de env ile kapalı). Login öncesi CSRF çerezi **15 dk** TTL taşır; login sonrası rotasyonla `Max-Age=28800` olur.
- **Başlık ve Origin:** Login dahil tüm state-changing admin-api isteklerinde `X-XSRF-TOKEN` + Origin allowlist (local `http://localhost:3001`, prod admin alan adı). Origin yoksa veya izinli değilse reddedilir; `Referer` tek başına yeterli sayılmaz.
- **Rotasyon/temizlik:** Token login sonrası döner; logout'ta çerez temizlenir. Eksik/geçersiz token veya izinsiz Origin → **403 `security.csrf_invalid`**, `application/problem+json`.
- **Sınır:** CSRF doğrulaması gateway'e geçmeden yapılır; upstream'e oturum/CSRF çerezleri taşınmaz.
- **Testler:** Tokensız istek, yanlış token, yanlış Origin, geçerli token, login öncesi 15 dk TTL, login rotasyonu, logout temizliği, 403 sonrası token yenileme/otomatik tekrar deneme, Next rewrite'ın `Origin`'i koruması ve CSRF çerezini tarayıcının görmesi doğrulanır.
- **Gerekçe:** Oturum tarayıcıda `HttpOnly` çerezle taşındığı sürece CSRF koruması gerekir; modüller arası REST + JWT iletişimi bu riski ortadan kaldırmaz.

## 9. Ek B — Karar Kaydı (`CLARIFY-*`, `Q10`)

> Tarih: 2026-10-28. Tüm maddeler onaylıdır; ayrıntılı gerekçeler Takım Yöneticisi/Software Architect karar kayıtlarındadır.

| Kod | Karar özeti | Etkilenen REQ/AC |
|---|---|---|
| `CLARIFY-1A` | Local/CI anahtarı git dışı yerel dosyada; prod env/secret; `ADMIN_JWT_PREVIOUS_PUBLIC_KEY_PATH` ile 6 dk overlap; hotel-api unknown `kid`de yeniler | REQ-3, AC-3 |
| `CLARIFY-2A` | Local/CI üç rollü seed + `hotel_admin` marker; prod ayrı CLI bootstrap: yalnız aktif SUPER_ADMIN varsa skip, pasif SUPER_ADMIN dahil diğer adminlerde fail-fast; audit fail rollback/delete | REQ-1, AC-1 |
| `CLARIFY-3A` | Oturum 30 dk idle + 8 saat mutlak; JWT 5 dk; istek başına üretim; login rotasyonu; logout mevcut oturum | REQ-2, REQ-3, AC-2, AC-3 |
| `CLARIFY-4A` | Argon2id; 12–128; generic hata; dummy hash; DB'de 5/15 dk → 15 dk kilit | REQ-1, REQ-2, AC-1, AC-2 |
| `CLARIFY-5A` | Kimlik/JWT kök filtrede; gateway pass-through; `gateway → adminidentity` kenarı yok; X-Request-Id gateway'de üretilir/doğrulanır | REQ-4, AC-4 |
| `CLARIFY-6A` | `adminidentity → adminaudit` senkron; IP/UA tam; 90 gün; giriş fail-closed; login rotasyonu `SESSION_REVOKED` audit'i | REQ-5, AC-5 |
| `CLARIFY-7A` | Kullanıcı yönetimi CRUD'u ayrı spec; 0004 yalnız seed/bootstrap + auth | Kapsam, AC-1/AC-6 |
| `CLARIFY-8A` | Gateway IT mock upstream; hotel-api rol kanıtı test-only controller; üretim probe yok | REQ-3, REQ-4, AC-3, AC-4 |
| `CLARIFY-9A` | Admin-web korumalı sayfalar sunucu tarafı `/me` doğrulamasıyla; içerik sızmaz | REQ-6, AC-6 |
| `CLARIFY-11A` | Scaffold kaldırılır; 0002 canlı admin-web smoke'u 0004 login smoke'u ile değişir; 0002 3/3 ve 0003 40/40 korunur; arşiv korunur; login yükleme+hata | REQ-6, AC-6, AC-7 |
| `Q10=A` | CSRF double-submit + Origin; login dahil state-changing; CSRF/Origin önce → 403; 403 `security.csrf_invalid`; gateway öncesi; docs §6 korunur | REQ-2, AC-2 |
