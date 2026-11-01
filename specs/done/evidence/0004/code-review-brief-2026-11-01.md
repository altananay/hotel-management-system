# Code Review Brief — Spec 0004 (Admin Identity)

- **Tarih:** 2026-11-01
- **PR:** [#18](https://github.com/altananay/hotel-management-system/pull/18) — `feature/0004-admin-identity` → `integration`
- **İncelenen head:** `6eeb710`
- **Yöntem:** Statik kaynak kodu, migration, test, doküman ve GitHub PR/check-run incelemesi. Test veya derleme çalıştırılmadı.
- **PR durumu:** `mergeable=true`, `mergeable_state=clean`, 144 dosya.
- **CI:** Backend, Frontend, E2E ve CI Quality Gate **success**; GitGuardian check-run'ı **skipped**.

---

## 1. Yönetici özeti

Kod tabanı 0004 kapsamını büyük ölçüde uyguluyor; migration, seed, bootstrap, oturum, CSRF, JWT/JWKS, gateway ve admin-web akışları mevcut. Ancak aşağıdaki maddeler **güvenlik ve üretim sertleştirmesi** açısından merge öncesi ele alınmalı. Özellikle üretimde rol zorlamasının yalnız test konfigürasyonunda etkin olması, CSRF/çerez varsayılanlarının üretimde güvenli olduğunun garanti edilmemesi ve login audit/lockout bağlamının proxy arkasında bozulması öne çıkıyor.

| ID | Önem | Konu |
|---|---|---|
| CR-1 | **Yüksek** | Üretimde rol zorlaması yok; `@EnableMethodSecurity` yalnız test konfigürasyonunda |
| CR-2 | **Yüksek** | Üretim için güvenli olmayan CSRF secret ve cookie varsayılanları |
| CR-3 | **Yüksek** | Login audit requestId/IP bağlamı; proxy arkasında lockout ve audit yanlış |
| CR-4 | Orta | Bozuk CSRF token base64'ü 403 yerine 500 üretiyor |
| CR-5 | Orta | `ClientIpResolver` bozuk XFF'te doğrudan IP'ye düşmüyor |
| CR-6 | Orta | Bootstrap kullanıcı adı/e-posta formatı doğrulanmıyor |
| CR-7 | Orta | JWKS unknown-kid/cache/503 ve `iat` doğrulaması uygulanmış/kanıtlanmış değil |
| CR-8 | Orta | AC-1/AC-3/AC-4/AC-5 için gerekli test ve kanıtlar eksik |
| CR-9 | Düşük/Orta | `AdminIdentityService` SRP/ISP sınırlarını aşıyor |
| CR-10 | Düşük | `PasswordPolicy` kısa e-posta local-part'ını kontrol etmiyor |
| CR-11 | Düşük | `safeReturnTo` encoded open-redirect varyantlarını engellemiyor |

---

## 2. Bulgular

### [CR-1] Üretimde rol zorlaması yok — **Yüksek**

**Dosya:** `backend/hotel-api/src/main/java/com/hotel/config/SecurityConfig.java:53-76`
**Dosya:** `backend/hotel-api/src/main/java/com/hotel/config/AdminJwtConfig.java:21-49`
**Dosya:** `backend/hotel-api/src/test/java/com/hotel/web/AdminJwtTestConfiguration.java:25`

**Kanıt:** `hotel-api` admin zinciri yalnız `anyRequest().authenticated()` uyguluyor. `@PreAuthorize` içeren test controller'ı `AdminJwtSecurityTest` içinde çalışıyor ve test konfigürasyonu `@EnableMethodSecurity` taşıyor. Üretim `SecurityConfig`/`AdminJwtConfig` üzerinde `@EnableMethodSecurity` yok; dolayısıyla üretimde eklenecek `@PreAuthorize`/`hasRole` kontrolleri etkisiz kalır.

**Etki:** JWT'de rol taşınsa bile `/admin-api/**` üzerinde yalnız kimlik doğrulanır; gelecekteki admin uçlarında yetkisiz rol erişimi riski oluşur. Testin yeşil olması üretim davranışını kanıtlamaz.

**Öneri ve gerekçe:** `@EnableMethodSecurity` üretim konfigürasyonuna taşınmalı veya roller `SecurityConfig` içinde path bazlı `hasRole/hasAnyRole` ile zorlanmalı. Test, test konfigürasyonu yerine üretim güvenlik zincirini kullanmalı. Gerekçe: `JwtAuthenticationConverter` yalnız authority üretir; yetkilendirme ayrı bir mekanizmadır ve testte etkinleştirilen bir ayar üretimde varsayılmamalıdır.

---

### [CR-2] Üretim için güvenli olmayan CSRF/çerez varsayılanları — **Yüksek**

**Dosya:** `backend/admin-api/src/main/resources/application.yml:47-51`
**Dosya:** `backend/admin-api/src/main/java/com/hotel/adminidentity/internal/web/AdminCookieFactory.java:15-26`
**Dosya:** `backend/admin-api/src/main/java/com/hotel/adminidentity/internal/application/AdminCsrfService.java:56-61`

**Kanıt:** `admin.cookie.secure` varsayılanı `false`; `admin.csrf.secret` varsayılanı repoda bilinen `local-admin-csrf-secret-32-bytes-min!!` değeri. `AdminCsrfService` yalnız secret uzunluğunu kontrol ediyor; üretim profilinde bu bilinen değerin kullanılmasını veya `Secure` çerezin kapalı kalmasını engelleyen fail-fast yok. `application-local.yml` yalnız local kolaylığı sağlıyor; prod için ayrı doğrulama bulunmuyor.

**Etki:** Prod deploy'da `ADMIN_CSRF_SECRET` unutulursa herkesin bildiği anahtarla CSRF token üretilir; `ADMIN_COOKIE_SECURE` unutulursa oturum çerezi HTTPS'te `Secure` olmadan gönderilir. Her ikisi de doğrudan oturum güvenliğini zayıflatır.

**Öneri ve gerekçe:** `local` dışı profilde `ADMIN_CSRF_SECRET` zorunlu ve bilinen default değerden farklı olmalı; `ADMIN_COOKIE_SECURE=true` zorunlu olmalı; eksikse uygulama başlamamalı. Gerekçe: "Deploy sırasında unutulmaz" varsayımı güvenlik kontrolü değildir; güvenli olmayan default, yanlış yapılandırmayı sessizce kabul eder.

---

### [CR-3] Login audit ve lockout bağlamı proxy arkasında yanlış — **Yüksek**

**Dosya:** `backend/admin-api/src/main/java/com/hotel/web/AdminIdentityFilter.java:37-47`
**Dosya:** `backend/admin-api/src/main/java/com/hotel/adminidentity/internal/web/AdminAuthController.java:96-101`
**Dosya:** `backend/admin-api/src/main/java/com/hotel/web/ClientIpResolver.java:30-53`

**Kanıt:** `AdminIdentityFilter` `/admin-api/auth/login` yolunu filtre dışında bırakıyor; dolayısıyla `adminClientIp` ve `adminRequestId` request attribute'ları login sırasında set edilmiyor. `AdminAuthController.context()` fallback olarak `request.getRemoteAddr()` ve `"unknown"` kullanıyor. `RequestIdFilter`'ın `requestId` attribute'u ve `ClientIpResolver` login için kullanılmıyor.

**Etki:** Login audit kayıtlarında `requestId` çoğunlukla `"unknown"` olur. Ters proxy arkasında tüm login denemeleri proxy IP'siyle görülür; identifier+IP kilidi tüm kullanıcıları etkileyebilir ve audit IP'si gerçek istemciyi göstermez. Bu, REQ-5 audit ve REQ-2 lockout gereksinimleriyle çelişir.

**Öneri ve gerekçe:** Login dahil tüm `/admin-api/**` için tek bir client-context filtresi kullan; `ClientIpResolver` ve `RequestIdFilter.resolveRequestId` üzerinden IP/requestId üret. Controller'daki fallback kopyasını kaldır. Gerekçe: IP/requestId çözümü iki farklı yerde farklı sonuç üretmemeli; özellikle login, kilit ve audit aynı güvenilir bağlamı paylaşmalı.

---

### [CR-4] Bozuk CSRF token'ı 500 üretiyor — **Orta**

**Dosya:** `backend/admin-api/src/main/java/com/hotel/adminidentity/internal/application/AdminCsrfService.java:102-117,177-179`
**Dosya:** `backend/admin-api/src/main/java/com/hotel/web/AdminCsrfFilter.java:46-61`

**Kanıt:** `AdminCsrfService.validate()` token payload'ını `Base64.getUrlDecoder().decode()` ile çözüyor. Geçersiz base64 `IllegalArgumentException` fırlatır; `AdminCsrfFilter` yalnız `AdminCsrfException` yakalar. Bu durumda istek 403 `security.csrf_invalid` yerine global 500'e düşer.

**Etki:** Saldırganın gönderebileceği malformed token, beklenen güvenlik yanıtı yerine 500 üretir; hata gürültüsü ve potansiyel log/DoS yükü oluşturur. Spec açıkça eksik/geçersiz token için 403 istiyor.

**Öneri ve gerekçe:** Decode/işleme hatalarını `AdminCsrfException`'a çevir veya filter'da `IllegalArgumentException`/`RuntimeException` sınırını güvenli biçimde 403'e eşle. Malformed base64, yanlış imza ve süre aşımı için test ekle. Gerekçe: Doğrulama girdisi saldırgan kontrollüdür; hata sınıflandırması HTTP 403 sözleşmesinin parçası olmalıdır.

---

### [CR-5] `ClientIpResolver` bozuk XFF'te doğrudan IP'ye düşmüyor — **Orta**

**Dosya:** `backend/admin-api/src/main/java/com/hotel/web/ClientIpResolver.java:35-53`

**Kanıt:** Sağdan sola XFF taramasında `isTrusted(candidate)` geçersiz adres için `IllegalArgumentException` yakalayıp `false` döndürüyor; `resolve()` daha sonra `!isTrusted(candidate)` olduğu için **geçersiz adayı** döndürüyor. Spec B-13 bozuk XFF'te doğrudan bağlantı IP'sine düşülmesini ve uyarı loglanmasını istiyor.

**Etki:** Audit `ip_address` alanına geçersiz/uzun değer yazılabilir; DB uzunluk sınırı aşılırsa audit transaction'ı patlar, login/audit fail-closed davranışı beklenmedik 500 üretir. Kilit anahtarı ve IP güvenilirliği bozulur.

**Öneri ve gerekçe:** Geçersiz adayı "güvenilmeyen geçerli IP" ile karıştırma; ayrıştırma hatasında doğrudan `remoteAddress` döndür ve uyarı logla. Geçerli/geçersiz ayrımını test et. Gerekçe: "İlk güvenilmeyen IP" yalnız geçerli IP adayları için anlamlıdır; bozuk değer asla kimlik/audit anahtarı olmamalıdır.

---

### [CR-6] Bootstrap kullanıcı adı/e-posta formatı doğrulanmıyor — **Orta**

**Dosya:** `backend/admin-api/src/main/java/com/hotel/adminidentity/internal/application/AdminBootstrapService.java:54-68`
**Dosya:** `backend/admin-api/src/main/java/com/hotel/adminidentity/internal/application/AdminUserWriter.java:30-42`

**Kanıt:** Bootstrap yalnız `isBlank` ve parola politikasını kontrol ediyor; kullanıcı adı için `^[a-z0-9][a-z0-9._-]{2,39}$`, e-posta için RFC 5322/254 karakter doğrulaması yok. `AdminUserWriter` değerleri yalnız trim/lowercase edip kaydediyor. Spec §2.2.1 bu formatları zorunlu kılıyor.

**Etki:** Geçersiz kullanıcı adı/e-posta ile prod SUPER_ADMIN oluşturulabilir; DB constraint'leri yalnız uzunluk sınırlar ve unique'lik sağlar. Bu, kimlik veri bütünlüğünü ve login/audit davranışını bozar.

**Öneri ve gerekçe:** `UsernamePolicy`/`EmailPolicy` gibi tek bir domain doğrulaması oluştur; bootstrap ve seed öncesi kullan; testlerde sınır değerlerini (3/40, 254, regex dışı) doğrula. Gerekçe: Aynı kuralın DB, CLI ve gelecekteki kullanıcı yönetimi arasında kopyalanmaması gerekir; doğrulama domainin sorumluluğudur.

---

### [CR-7] JWKS unknown-kid/cache/503 ve `iat` doğrulaması kanıtlanmış değil — **Orta**

**Dosya:** `backend/hotel-api/src/main/java/com/hotel/config/AdminJwtConfig.java:21-37`
**Dosya:** `backend/hotel-api/src/main/java/com/hotel/web/AdminJwtAuthenticationEntryPoint.java:28-45`
**Spec:** `specs/0004-admin-identity.md` §2.2.5, AC-3

**Kanıt:** `hotel-api` `NimbusJwtDecoder.withJwkSetUri()` varsayılanına bırakılmış; 5 dk cache, unknown `kid` yenileme, 60 sn cooldown ve JWKS erişilemezken 503 davranışı için özel konfigürasyon yok. `AdminJwtSecurityTest` yerel public key ile çalışan test konfigürasyonunu kullanıyor; gerçek JWKS endpoint'i, rotation ve cache yenileme test edilmiyor. Spec'in istediği `AC-3-jwks-rotation.log` da yok.

**Etki:** Anahtar rotasyonu sonrası eski/yeni `kid` davranışı, JWKS kesintisinde 503 ve bilinmeyen `kid` yenilemesi garanti edilmiyor. Testler JWKS entegrasyonunu değil, test decoder'ını doğruluyor.

**Öneri ve gerekçe:** `JwtDecoder` için önbellek/refresh politikasını açıkça yapılandır; mock JWKS sunucusuyla unknown kid, rotation, 5 dk cache, 60 sn cooldown ve 503 senaryolarını IT'de kanıtla. `iat` gelecekte >60 sn kontrolünü de ekle/test et. Gerekçe: Varsayılan kütüphane davranışı sürüm değişiminde sessizce değişebilir; güvenlik sözleşmesi açık ve testli olmalıdır.

---

### [CR-8] AC-1/AC-3/AC-4/AC-5 test ve kanıtları eksik — **Orta**

**Dosya:** `specs/done/evidence/0004/README.md:10-14`
**Dosya:** `backend/admin-api/src/test/java/com/hotel/adminidentity/AdminBootstrapServiceTest.java:47-123`

**Kanıt:** AC-1 için yalnız iki log var; `AdminBootstrapServiceTest` dört test içeriyor. Spec'in istediği pasif SUPER_ADMIN, yalnız MANAGER/FRONT_DESK, eksik/zayıf env, DB hatası, aynı username ile eşzamanlı iki CLI, CLI modunda web server açılmaması ve skip audit hatası senaryoları yok. AC-3 için `AC-3-jwks-rotation.log`, AC-4 için `AC-4-request-id.log`/`AC-4-allowlist.log`, AC-5 için `AC-5-rotation-audit.log`/`AC-5-bootstrap-skip-audit.log`/`AC-5-audit-retention.md` eksik.

**Etki:** CI yeşil olsa da AC-1/AC-3/AC-4/AC-5'in tamamı kanıtlanmıyor; özellikle bootstrap güvenlik guard'ları ve JWKS/gateway header sözleşmesi test dışı kalıyor. Bu, "kanıtsız iş bitmez" kuralıyla çelişir.

**Öneri ve gerekçe:** Eksik testleri ve spec'te adlandırılan kanıtları ekle; mümkünse `AdminBootstrapService` testlerini gerçek Testcontainers/CLI süreciyle destekle. Gerekçe: Aynı servis içindeki guard dalları birim testte görünse bile exit code, audit ve CLI/web-server davranışı ancak süreç düzeyinde kanıtlanır.

---

### [CR-9] `AdminIdentityService` SRP/ISP sınırlarını aşıyor — **Düşük/Orta**

**Dosya:** `backend/admin-api/src/main/java/com/hotel/adminidentity/internal/application/AdminIdentityService.java:26-40,63-152`

**Kanıt:** Sınıf dokuz bağımlılıkla login, lockout, session doğrulama, JWT üretimi, CSRF, logout, bootstrap ve audit orkestrasyonunu tek sınıfta topluyor. `AdminIdentityApi` de aynı şekilde auth, CSRF ve bootstrap sözleşmelerini birleştiriyor. `validateSession` içindeki `try/catch` aynı exception'ı yeniden fırlatıyor.

**Etki:** Tek sorumluluk ve arayüz ayrımı zayıf; değişiklikler geniş test yüzeyi ve yüksek bağımlılık yükü getirir. Yeni bir auth/CSRF/bootstrap kuralı aynı sınıfı etkiler.

**Öneri ve gerekçe:** Login/lockout, session, token/CSRF ve bootstrap use-case'lerini ayrı servis/portlara böl; `AdminIdentityService` yalnız ince bir facade kalsın. Gerekçe: SOLID/SRP, güvenlik kurallarının bağımsız test edilebilirliğini ve değişim izolasyonunu artırır.

---

### [CR-10] Parola politikası kısa e-posta local-part'ını atlıyor — **Düşük**

**Dosya:** `backend/admin-api/src/main/java/com/hotel/adminidentity/internal/domain/PasswordPolicy.java:33-41`

**Kanıt:** E-posta local-part'ı yalnız `localPart.length() >= 3` ise kontrol ediliyor. Spec parolanın e-posta local-part'ını (uzunluk istisnası olmadan) içeremeyeceğini söylüyor. `a@example.com` gibi kısa local-part'lı hesaplarda parola local-part'ı içerebilir.

**Etki:** Kimlik bilgisiyle ilişkili parola seçimi engellenemez; düşük entropili parola riski artar.

**Öneri ve gerekçe:** Uzunluk eşiğini kaldır veya nedenini spec'te gerekçelendir; testte 1–2 karakterli local-part senaryosu ekle. Gerekçe: Politika metni ile kod aynı olmalı; kısa local-part daha az olası olsa da kontrolün atlanması için neden değildir.

---

### [CR-11] `safeReturnTo` encoded open-redirect varyantlarını engellemiyor — **Düşük**

**Dosya:** `admin-web/src/lib/return-to.ts:1-8`
**Dosya:** `admin-web/src/lib/__tests__/return-to.test.ts:10-14`

**Kanıt:** Yalnız `//`, `://` ve `\` kontrol ediliyor. `/%2F%2Fevil.example` veya `/%5Cevil.example` gibi encoded değerler `startsWith("/")` kontrolünü geçiyor. Router'ın decode davranışına bağlı olarak dış yönlendirme riski doğabilir.

**Etki:** Login sonrası `returnTo` üzerinden açık yönlendirme; oltalama riski.

**Öneri ve gerekçe:** Değeri decode edip URL/path olarak canonicalize et; yalnız same-origin path kabul et; encoded slash/backslash ve scheme testleri ekle. Gerekçe: String prefix kontrolü URL semantiği için yeterli değildir.

---

## 3. Olumlu noktalar

- Migration'lar ayrı V2/V3 olarak modüllenmiş; audit tablosu FK'sız ve `admin_platform` sınırında.
- Argon2id parametreleri spec ile uyumlu; dummy hash ile kullanıcı numaralandırması azaltılmış.
- Session token'ı 32 byte CSPRNG, DB'de SHA-256; cookie `HttpOnly`, `SameSite=Lax`, `Path=/`.
- Login/kilit/CSRF hata önceliği ve audit fail-closed akışları büyük ölçüde uygulanmış.
- Gateway header allowlist'i ve response strip davranışı test edilmiş.
- 0002/0003 arşiv izolasyonu ve CI 4/4 yeşil.

---

## 4. Sonuç

**CR-1, CR-2 ve CR-3 merge öncesi kapatılmalı.** CR-4–CR-8 güvenlik/kanıt sertleştirmesi için aynı turda ele alınmalı. CR-9–CR-11 takip borcu olarak bırakılabilir; ancak CR-9'un ayrılması sonraki admin dilimlerinde değişiklik maliyetini düşürür.

CI ve PR durumu iyi; fakat testlerin üretim güvenlik konfigürasyonunu değil test konfigürasyonunu doğruladığı CR-1 örneğinde olduğu gibi, yeşil pipeline tek başına yeterli değildir.
