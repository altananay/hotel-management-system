# 0004 code review fixleri — 2026-11-01

- **İncelenen head:** `6eeb710`
- **Fix head:** `592507c` (`plan 0004/21`…`0004/28`)
- **Kaynak:** `code-review-brief-2026-11-01.md`

## Özet

| ID | Durum | Yapılan |
|---|---|---|
| CR-1 | Düzeltildi | `@EnableMethodSecurity` üretim `SecurityConfig`'e taşındı; test konfigürasyonundan kaldırıldı. |
| CR-2 | Düzeltildi | `AdminProductionSecurityValidator`: `local`/`ci` dışında `ADMIN_COOKIE_SECURE=true` ve default olmayan ≥32 byte CSRF secret zorunlu; aksi halde startup fail-fast. |
| CR-3 | Düzeltildi | `AdminClientContextFilter` tüm `/admin-api/**` için IP/UA/requestId'yi çözer; login dahil aynı bağlam kullanılır. Controller fallback'i kaldırıldı. |
| CR-4 | Düzeltildi | Malformed base64/payload `AdminCsrfException`'a çevrildi; 403 `security.csrf_invalid`. |
| CR-5 | Düzeltildi | Geçersiz XFF adayı doğrudan bağlantı IP'sine düşer ve uyarı loglanır; geçerli IP literal kontrolü eklendi. |
| CR-6 | Düzeltildi | `UsernamePolicy` / `EmailPolicy`; bootstrap ve writer aynı domain doğrulamasını kullanır. |
| CR-7 | Düzeltildi | Açık `JWKSourceBuilder`: 5 dk cache, 60 sn rate-limit, 2 sn timeout; `iat` +60 sn kontrolü; mock JWKS testleri; JWKS erişilemezse 503. |
| CR-8 | Büyük ölçüde düzeltildi | Bootstrap guard/CLI/concurrency, gateway, JWKS ve audit testleri + eksik kanıt dosyaları eklendi. |
| CR-9 | Kısmi / itiraz | Yalnız gereksiz `try/catch` kaldırıldı; tam port ayrımı gerekçeli olarak ertelendi. |
| CR-10 | İtiraz | 1–2 karakter local-part substring kontrolü pratikte tüm parolaları reddeder; tam e-posta ve kullanıcı adı kontrolü korunuyor. |
| CR-11 | Düzeltildi | `safeReturnTo` decode + canonicalize eder; encoded `//`, `\`, scheme ve kontrol karakterleri reddedilir. |

## CR-9 — neden tam bölme yapılmadı

`AdminIdentityService` zaten `AdminSessionService`, `AdminCsrfService`, `AdminJwtService` ve
`AdminBootstrapService` use-case'lerine delege eden ince bir facade. Kalan sorumluluk
login/lockout orkestrasyonu ve audit sırasıdır. `AdminIdentityApi`'yi login, session, CSRF ve
bootstrap olarak dörde bölmek, bugün tüketicisi olmayan dört public port ve modül sınırı
oluşturur; root filtre/controller tek bir kimlik portu üzerinden çalışıyor. Kullanıcı yönetimi
dilimi geldiğinde (yeni use-case'ler ve tüketiciler) port ayrımı yapılmalıdır. Bu, Altın Kural 4
gereği Takım Yöneticisi kararına sunulan bir erteleme önerisidir; bu turda yalnızca gereksiz
`try/catch` kaldırıldı.

## CR-10 — neden eşik korunuyor

Spec "parola e-posta local-part'ını içeremez" der. Ancak `a@example.com` gibi 1–2 karakterli
local-part'larda substring kontrolü neredeyse her parolayı reddeder (ör. `a`), politikayı
kullanılamaz hale getirir. Kodda bu neden `MIN_LOCAL_PART_LENGTH_FOR_CHECK = 3` sabiti ve
açıklamasıyla belgelendi. Tam e-posta adresi ve kullanıcı adı her uzunlukta kontrol edilmeye
devam eder. Öneri: spec'te bu istisnanın açıkça yazılması; aksi halde 1–2 karakterli
local-part'lı hesaplarda parola politikası pratikte uygulanamaz.

## Test / kanıt

- `admin-api`: **98/98** birim; Testcontainers IT CI'da.
- `hotel-api`: **89/89**.
- `admin-web`: **23/23**.
- Yeni kanıtlar: `AC-1-bootstrap-*.log`, `AC-2-login-context.log`,
  `AC-3-jwks-rotation.log`, `AC-4-request-id.log`, `AC-4-allowlist.log`,
  `AC-5-rotation-audit.log`, `AC-5-bootstrap-skip-audit.log`, `AC-5-audit-retention.md`.
