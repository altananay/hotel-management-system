# Spec 0004 — Karar ve Analist Brief'i

> **Amaç:** `specs/0004-admin-identity.md` uygulanmaya başlamadan önce geliştiricinin varsayım yapmak zorunda kalacağı boşlukları, Takım Yöneticisi kararlarını ve analistin rev.7 kapsam kaydını tek yerde toplamak.
>
> **Kapsam:** Yalnızca spec 0004'tür. Bu dosya bir spec, plan veya kod değildir. B-01…B-17 kararları ve rev.6 düzeltmeleri spec rev.6'da işlendi; Takım Yöneticisi 2026-10-29'da eşzamanlılık kapsamını **A seçeneğiyle** onayladı. Bu brief karar kaydı ve rev.7 talimatıdır. Geliştirici onaysız varsayım yapmaz.
>
> **Durum:** B-14 aktif SUPER_ADMIN/audit rollback düzeltmesi ve rev.6’daki beş küçük düzeltme + AC-1 (f)/(g) exit testleri spec rev.6’da işlendi. Exit code eşlemesi brief’te düzeltildi (env eksik/zayıf → **exit 2**; DB hatası → **exit 3**; spec değişmez). Eşzamanlılık kapsamı **A onaylandı**: B-14 “en az bir aktif SUPER_ADMIN” guard’ı; global “tek aktif SUPER_ADMIN” invariantı 0004 dışı. Rev.7’de yalnız iki doküman netleştirmesi kaldı; davranış/test değişmez.
>
> **Analist için:** B-01…B-17 ve rev.6 düzeltmeleri spec rev.6’da işlendi. Rev.7’de yalnız §3.2.1’deki iki doküman netleştirmesini işle; yeni DB kısıtı, partial unique index veya advisory lock **yok**.
> **Son karar — eşzamanlılık (2026-10-29):** **A seçeneği onaylandı.** B-14 “en az bir aktif SUPER_ADMIN” bootstrap guard’ıdır; aynı `ADMIN_BOOTSTRAP_USERNAME` yarışı unique conflict ile test edilir. Farklı username’lerle paralel bootstrap ve global “tek aktif SUPER_ADMIN” invariantı 0004 kapsamı dışıdır; 0004’te partial unique index/advisory lock yoktur.
> **Tarih:** 2026-10-28 *(Son güncelleme: 2026-10-29)*
> **İnceleme girdisi:** Spec 0004 rev.6, `docs/architecture.md` §4/§5/§6/§8/§10/§11, `docs/domain.md` §1/§3, `docs/conventions.md` §2.3/§2.4/§4, mevcut `admin-api`/`hotel-api`/`admin-web`/`infra`/CI kodu.

## 0. Nasıl kullanılır?

- Bu dosya artık **karar kaydıdır**; her maddede “Karar (2026-10-28)” satırı ve “Ek karar” notları bağlayıcıdır.
- **B-01…B-17** için A seçenekleri onaylandı; ayrıca B-03, B-07, B-11, B-12, B-14 ve B-17 ek kararları geçerlidir.
- B-01…B-17 kararları ve rev.6 düzeltmeleri spec rev.6'da işlendi; ayrıntılar §3.1–§3.2'de kayıtlıdır. Geliştirici onaysız varsayım yapmaz.

## 1. Özet tablo

| ID | Konu | Kategori | Bloklayıcı? | Karar |
|---|---|---|---|---|
| B-01 | Kullanıcı adı/e-posta normalizasyonu ve unique alanlar | Belirsiz AC / sınır | **Evet** | **A onaylandı** — 3–40 ASCII username; e-posta ≤254; case-insensitive unique |
| B-02 | Parola politikası ve Argon2id parametreleri | Eksik sınır | **Evet** | **A onaylandı** — NFC, 12–128 code point; Argon2id 19.456 KiB/2/1 |
| B-03 | Kilitlenme algoritması ve sınırları | Belirsiz AC / sınır | **Evet** | **A onaylandı + ek karar** — kayan pencere; anahtar **normalize identifier** (username veya e-posta) + IP |
| B-04 | Oturum token'ı, DB şeması, çoklu oturum/rotasyon | Belirsiz AC | **Evet** | **A onaylandı** — 256-bit token; SHA-256 hash; çoklu oturum |
| B-05 | Oturum zaman aşımı, `last_seen_at`, çerez semantiği | Eksik sınır | **Evet** | **A onaylandı** — 30 dk/8 saat kesin sınır; sabit cookie Max-Age |
| B-06 | CSRF token üretimi/doğrulaması ve ömrü | Belirsiz AC | **Evet** | **A onaylandı** — HMAC imzalı stateless token; 15 dk → oturum ömrü |
| B-07 | Origin allowlist'i, hata önceliği ve CSRF retry | Tanımsız hata | **Evet** | **A onaylandı + ek karar** — CSRF/Origin **önce**; 403 → 400/401; retry max 1 |
| B-08 | JWT anahtar formatı, `kid` ve rotasyon | Eksik sınır | **Evet** | **A onaylandı** — RSA 2048 PKCS#8; RFC 7638 `kid`; aktif+önceki |
| B-09 | JWKS cache/hata, clock skew ve rol hiyerarşisi | Tanımsız hata / belirsiz AC | **Evet** | **A onaylandı** — 5 dk cache; JWKS down 503; rol hiyerarşisi yok, uç bazlı roller |
| B-10 | `hotel-api` 401/403 kodları ve test controller | Belirsiz hata | **Evet** | **A onaylandı** — 401 `security.unauthorized`; 403 `security.forbidden` |
| B-11 | Gateway timeout, hata eşlemesi ve header hijyeni | Tanımsız hata | **Evet** | **A onaylandı + ek karar** — 504/502; **X-Request-Id gateway tarafından üretilir/doğrulanır** |
| B-12 | Audit taksonomisi, şema ve revocation olayları | Belirsiz AC | **Evet** | **A onaylandı + ek karar** — reason-code seti; **login rotasyonu `SESSION_REVOKED` ile audit edilir** |
| B-13 | Trusted proxy ve IP/UA sınırları | Eksik sınır | **Evet** | **A onaylandı** — CIDR trust; XFF yalnız güvenilir proxy'de |
| B-14 | Bootstrap CLI, exit kodları ve SUPER_ADMIN kontrolü | Belirsiz AC | **Evet** | **A onaylandı + ek karar** — **yalnız aktif SUPER_ADMIN varsa skip; pasif SUPER_ADMIN dahil diğer adminlerde fail-fast**; audit fail rollback/delete; aktif admin oluşmaz; exit 0/2/3/4 (env=2, DB=3, oluşturma audit=4; skip audit=0) |
| B-15 | Admin seed mekanizması, env, marker ve idempotency | Belirsiz AC | **Evet** | **A onaylandı** — SQL repeatable seed; `hotel_admin` marker; parola ezmez |
| B-16 | Admin-web login testid/redirect/CSRF retry/logout | Belirsiz AC / hata | **Evet** | **A onaylandı** — testid seti; güvenli returnTo; `/me` 401 → login; retry max 1 |
| B-17 | Admin E2E/CI anahtar, seed, 0002 smoke ve kanıt | Belirsiz AC / kanıt | **Evet** | **A onaylandı + ek karar** — `@admin`; 0002 3/3; **0003 design 40/40 korunur**; 0004 evidence |

> **Toplam:** 17 bloklayıcı madde.

---

## 2. Bloklayıcı maddeler

### B-01 — Kullanıcı adı/e-posta normalizasyonu ve unique alanlar tanımsız

- **Kategori:** Belirsiz kabul kriteri / eksik sınır.
- **Spec yeri:** REQ-1, AC-1.
- **Soru:** Kullanıcı adı ve e-posta için uzunluk, karakter seti, case-folding ve saklama biçimi ne? Case-insensitive uniqueness DB'de nasıl garanti edilecek? Pasif kullanıcı girişte ne görecek?
- **Neden açık:** Spec "benzersiz kullanıcı adı/e-posta (case-insensitive)" diyor; ancak format, uzunluk, `lower()`/`citext`, `trim` ve pasif hesap davranışı yok. Mevcut `V1` yalnız şemayı oluşturuyor; tablo/kolon/index yok.
- **Seçenekler:**
  - **A)** Kullanıcı adı **3–40** karakter, `^[a-z0-9][a-z0-9._-]{2,39}$` (ASCII, `@` yok); e-posta **≤254**, trim + NFC + lowercase; ikisi de canonical lowercase saklanır; case-insensitive unique index; rol `varchar` + check (`SUPER_ADMIN`/`MANAGER`/`FRONT_DESK`), kullanıcı başına tek rol; pasif hesap generic 401 + audit `LOGIN_INACTIVE`.
  - **B)** 1–100 serbest metin; yalnız `lower()` unique; `@` içeren kullanıcı adı da kabul edilir.
  - **C)** Case-sensitive unique; giriş büyük/küçük harfe duyarlı.
- **Önerim:** **A.** Kullanıcı adında `@` yasağı, username/email çakışmasını da önler; canonical lowercase, login identifier'ının username mi e-posta mı olduğunu belirsizleştirmez.
- **Gerekçe:** Case-insensitive unique yarışını DB'de çözmek gerekir; yoksa iki eşzamanlı kayıt/seed aynı hesabı iki kez oluşturabilir. Pasif hesap için generic 401, kullanıcı numaralandırmasını korur.
- **Etki:** AC-1, AC-2; seed/bootstrap; login.
- **Karar (2026-10-28):** **ONAYLANDI — A.**

### B-02 — Parola politikası ve Argon2id parametreleri eksik

- **Kategori:** Eksik sınır.
- **Spec yeri:** REQ-1, AC-1, AC-2.
- **Soru:** "12–128 karakter" Unicode code point mi byte mı? NFC normalizasyonu var mı? "Kullanıcı adı/e-posta içeremez" tam olarak hangi substring'ler? Argon2id bellek/iterasyon/paralellik/tuz/hash değerleri ne?
- **Neden açık:** Spec yalnız "Argon2id" ve 12–128 diyor; AC "parametreler testle doğrulanır" diyor ama beklenen değer yok. Spring Security'nin Argon2 encoder'ı için ek bağımlılık ve açık parametre seti gerekir. `kullanıcı adı/e-posta içeremez` ifadesi case/email-local-part açısından belirsiz.
- **Seçenekler:**
  - **A)** NFC; **12–128 Unicode code point**, en fazla **512 UTF-8 byte**; trim yok (boşluklar geçerli); password casefold edilip tam username, tam e-posta veya e-posta local-part'ını içeriyorsa reddet; Argon2id **19.456 KiB memory, 2 iterations, 1 parallelism, 16-byte salt, 32-byte hash**; dummy hash aynı parametrelerle startup'ta üretilir; BouncyCastle bağımlılığı eklenir.
  - **B)** Byte uzunluğu 12–128; Spring default Argon2 parametreleri; contains kontrolü yalnız tam username.
  - **C)** 12–128 + büyük/küçük harf/rakam/sembol kompozisyonu.
- **Önerim:** **A.** Bootstrap/seed parolaları da aynı politikaya tabidir; "güçlü" ayrı bir tanım değildir.
- **Gerekçe:** Code point/byte ayrımı Türkçe ve emoji parolalarda farklı sonuç verir; Argon2 parametreleri güvenlik maliyeti ve test süresi için açık olmalıdır. Dummy hash, bilinmeyen kullanıcıda timing farkını azaltır.
- **Etki:** AC-1, AC-2; bootstrap/seed.
- **Karar (2026-10-28):** **ONAYLANDI — A.**

### B-03 — Kilitlenme algoritması ve sınırları belirsiz

- **Kategori:** Belirsiz AC / eksik sınır.
- **Spec yeri:** REQ-2 kilit, AC-2.
- **Soru:** "15 dk içinde 5 başarısız deneme" kayan pencere mi sabit pencere mi? Kilit 5. denemede mi başlar, ilk denemede mi? Kilitliyken yapılan denemeler sayacı uzatır mı? Başarılı giriş hangi kayıtları temizler? Bilinmeyen/pasif kullanıcı ve CSRF hatası sayılır mı?
- **Neden açık:** Spec yalnız "hesap+IP 15 dk kilit" diyor; deneme tablosu, pencere tipi ve reset davranışı yok. Bu, testlerde 4./5. deneme ve 15:00 sınırında farklı sonuçlara yol açar.
- **Seçenekler:**
  - **A)** Kayan pencere: son 15 dk içindeki başarısız denemeler; 5. denemede `locked_until = now + 15 dk`; kilitliyken yapılan denemeler sayılmaz/kilidi uzatmaz; başarılı giriş o identifier+IP için denemeleri ve kilidi siler; bilinmeyen/pasif kullanıcı da başarısız sayılır ve audit'lenir; CSRF/validation hataları sayılmaz.
  - **B)** Sabit 15 dk pencere; kilit ilk denemeden itibaren 15 dk; kilitli denemeler sayılır.
  - **C)** Yalnız hesap bazlı kilit (IP'siz) veya yalnız IP bazlı.
- **Önerim:** **A.** `login_attempts` tablosunda normalized identifier + IP + admin_id (varsa) + occurred_at tutulur; `(identifier, ip, occurred_at)` index'i; testler 4/5, 14:59/15:00, 15:00 sonrası ve restart ile yapılır.
- **Gerekçe:** Kayan pencere brute-force korumasını korur; kilitli denemelerin kilidi uzatması meşru kullanıcıyı süresiz kilitleyebilir. DB kalıcılığı restart gereksinimini karşılar.
- **Etki:** AC-2, AC-5; audit reason'ları.
- **Karar (2026-10-28):** **ONAYLANDI — A.**
- **Ek karar:** Kilit anahtarı **normalize identifier** (kullanıcı adı veya e-posta) **+ istemci IP**; e-posta girişinde de aynı sayaç kullanılır.

### B-04 — Oturum token'ı, DB şeması, çoklu oturum ve rotasyon belirsiz

- **Kategori:** Belirsiz AC.
- **Spec yeri:** REQ-2, AC-2.
- **Soru:** Oturum kimliği kaç bit, hangi encoding? DB'de hash algoritması ne? Oturum tablosunda hangi alanlar var? Aynı kullanıcı birden fazla oturum açabilir mi? Login sırasında mevcut oturum ne olur? "Girişte rotasyon" tam olarak neyi iptal eder?
- **Neden açık:** Spec "yeterli entropi" ve "özetiyle saklanır" diyor; token formatı, hash, tablo ve çoklu oturum davranışı yok. `logout yalnız mevcut oturumu iptal eder` ifadesi diğer oturumların süreceğini ima ediyor; login rotasyonu ise belirsiz.
- **Seçenekler:**
  - **A)** 32 byte CSPRNG → base64url (43 char) cookie; DB'de SHA-256 hex (64 char) unique `token_hash`; `admin_sessions` tablosu: UUIDv7 id, admin_id, token_hash, created_at, last_seen_at, absolute_expires_at, revoked_at; çoklu oturum serbest; her login yeni oturum açar; istekte geçerli eski oturum varsa o oturum iptal edilir (rotasyon), diğer oturumlar sürer; logout yalnız current.
  - **B)** Tek oturum zorunlu; her login önceki tüm oturumları iptal eder.
  - **C)** Token'ı DB'de düz metin sakla; hash yok.
- **Önerim:** **A.** `token_hash` unique index; admin_id index; expired/revoked oturum kayıtları bu dilimde otomatik temizlenmez (90 gün hedefi yalnız audit içindir; session temizliği ayrı runbook).
- **Gerekçe:** 256-bit token ve SHA-256 özeti, DB sızıntısında oturum çerezlerinin kullanılmasını engeller; çoklu oturum, farklı cihazlarda çalışmayı destekler ve logout sözleşmesiyle uyumludur.
- **Etki:** AC-2, AC-5.
- **Karar (2026-10-28):** **ONAYLANDI — A.**

### B-05 — Oturum zaman aşımı, `last_seen_at` ve çerez semantiği belirsiz

- **Kategori:** Eksik sınır.
- **Spec yeri:** REQ-2, AC-2; §2.1 çerezler.
- **Soru:** 30 dk idle ve 8 saat mutlak sınırlarında `>=` mi `>` mi? `last_seen_at` hangi isteklerde güncellenir? Cookie `Max-Age` idle'da kayar mı? Süre dolunca cookie temizlenir mi? CSRF/health/login istekleri sayılır mı?
- **Neden açık:** Spec süreleri veriyor ama sınır karşılaştırması, cookie davranışı ve `last_seen_at` güncelleme kapsamı yok. Özellikle 8 saatlik cookie'nin idle süresini uzatıp uzatmadığı test edilemez.
- **Seçenekler:**
  - **A)** Idle: `now - last_seen_at >= 30 dk`; absolute: `now >= absolute_expires_at`; her doğrulanmış korumalı istekte (`/me` ve gateway dahil) `last_seen_at = now`; CSRF/health/login güncellemez; cookie `Max-Age=28800` sabittir, idle'da kaymaz; `Secure` local'de `ADMIN_COOKIE_SECURE=false`, prod'da `true`; süre dolunca 401 + `ADMIN_SESSION` ve `XSRF-TOKEN` temizleme; Clock ile test.
  - **B)** Idle'da cookie Max-Age'ı her istekte 30 dk'ya kaydır; absolute ayrı tut.
  - **C)** Yalnız idle timeout; mutlak 8 saat yok.
- **Önerim:** **A.** Sabit cookie Max-Age, tarayıcıda 8 saat sonra çerezin kendiliğinden gitmesini sağlar; idle sunucu tarafında ayrıca uygulanır.
- **Gerekçe:** İki süre birbirinden bağımsız olmalı; aksi halde aktif kullanıcı 8 saati aşabilir veya pasif kullanıcı cookie sayesinde oturumu sürdürebilir. Sınır testleri deterministik olur.
- **Etki:** AC-2.
- **Karar (2026-10-28):** **ONAYLANDI — A.**

### B-06 — CSRF token üretimi, saklaması ve ömrü belirsiz

- **Kategori:** Belirsiz AC.
- **Spec yeri:** REQ-2 CSRF, AC-2, Ek A.
- **Soru:** Login öncesi 15 dk TTL'li token nerede tutulur? Token imzalı mı, DB'de mi? Login sonrası "oturum ömrüne bağlama" nasıl yapılır? Secret nereden gelir? Logout'ta sunucu tarafında token iptali gerekir mi?
- **Neden açık:** Spec token'ın çerezde tutulduğunu ve TTL'leri söylüyor; ama token formatı, doğrulama anahtarı ve login sonrası bağlama yöntemi yok. Pre-login oturum olmadığı için token'ın server-side state'e mi yoksa imzalı stateless yapıya mı dayanacağı kritik bir tasarım kararı.
- **Seçenekler:**
  - **A)** **HMAC-SHA256 imzalı stateless token**: payload `{v, nonce, iat, exp, sidHash?}`; `ADMIN_CSRF_SECRET` (≥32 byte, prod zorunlu, local-only default) ile imza; pre-login 15 dk; login sonrası `sidHash` eklenmiş ve absolute oturum ömrüne ayarlı yeni token; cookie `XSRF-TOKEN` okunabilir; header `X-XSRF-TOKEN`; logout'ta cookie silinir; DB gerekmez.
  - **B)** DB tablosunda token hash + TTL; login sonrası session_id ile bağla; logout'ta satırı sil.
  - **C)** Yalnız double-submit cookie; token imzası/DB kaydı yok.
- **Önerim:** **A.** Token `base64url(payload).base64url(HMAC)` biçiminde; secret yoksa prod'da fail-fast, local'de açıkça işaretli default. Login başarısında yeni token `Set-Cookie` ile döner; eski token geçersizdir.
- **Gerekçe:** Pre-login anonim akışta DB tablosu açmadan TTL ve rotasyon sağlar; imza, saldırganın kendi ürettiği token'ı cookie'ye yazıp header'a koymasını engeller. DB alternatifi de geçerli; ancak token yaşam döngüsü tek yerde tanımlanmalı.
- **Etki:** AC-2, AC-6.
- **Karar (2026-10-28):** **ONAYLANDI — A.**

### B-07 — Origin allowlist'i, hata önceliği ve CSRF otomatik retry belirsiz

- **Kategori:** Tanımsız hata durumu.
- **Spec yeri:** REQ-2 CSRF, AC-2, AC-6, §2.1.
- **Soru:** Prod Origin allowlist'i hangi env'den ve hangi biçimde gelir? Origin yoksa/`null` ise? Login body bozuksa 400 mü 401 mi? CSRF hatası oturum hatasından önce mi? 403 CSRF'te otomatik retry kaç kez ve hangi isteklerde?
- **Neden açık:** Spec local `http://localhost:3001`, prod "admin alan adı" diyor; env adı/formatı yok. Hata matrisi yalnız bazı kodları veriyor; 400/401/403/500 önceliği ve retry'ın non-idempotent isteklerde tekrarı tanımsız.
- **Seçenekler:**
  - **A)** `ADMIN_ALLOWED_ORIGINS` virgülle ayrılmış tam `scheme://host[:port]`; local default `http://localhost:3001`; prod'da zorunlu; trailing slash normalize; wildcard yok; state-changing'de Origin yok/`null`/izinsiz → **403 `security.csrf_invalid`**. Sıra: CSRF/Origin → 403; body validation → 400 `validation.invalid_request`; kimlik/oturum → 401; rol → 403 `security.forbidden`; audit → 500 `system.internal_error`. Retry yalnız login/logout ve en fazla **1** kez; diğer state-changing isteklerde kullanıcıya yeniden dene sunulur.
  - **B)** Origin için `Referer` fallback; CSRF hatasını 401'e çevir; sınırsız retry.
  - **C)** Origin kontrolünü yalnız prod'da aç; local'de kapalı.
- **Önerim:** **A.** Login/logout dışındaki işlemlerde otomatik retry, idempotency anahtarı olmadan çift kayıt riski yaratır; bu dilimde yalnız login/logout güvenli kabul edilir.
- **Gerekçe:** Hata önceliği test edilebilir olmalı; CSRF'in auth'tan önce reddedilmesi, geçersiz oturumla CSRF denemesinin 401/403 karışmasını önler. Sınırsız retry, 403 döngüsü ve çift işlem riski yaratır.
- **Etki:** AC-2, AC-6, AC-7.
- **Karar (2026-10-28):** **ONAYLANDI — A.**
- **Ek karar:** Tüm state-changing isteklerde **CSRF/Origin kontrolü önce**; geçersizse **403** döner, sonra sırasıyla 400/401/403/500 değerlendirilir. Spec §2.2.4 ve AC-2 bu sıraya göre düzeltilir.

### B-08 — JWT anahtar formatı, `kid` ve rotasyon belirsiz

- **Kategori:** Eksik sınır.
- **Spec yeri:** REQ-3, AC-3.
- **Soru:** RSA anahtar boyutu kaç? Private/public dosya formatı ne? `kid` nasıl üretilir? Aktif/önceki anahtar dosyaları hangi izinlerle tutulur? Rotasyon adımları ve JWKS alanları ne?
- **Neden açık:** Spec "RS256" ve dosya yollarını söylüyor; anahtar boyutu, PEM formatı, `kid` üretimi ve JWKS temsili yok. `hotel-api` bu anahtarları JWKS üzerinden okuyacağı için format standardı gerekir.
- **Seçenekler:**
  - **A)** **RSA 2048**, private **PKCS#8 PEM**, public **SPKI PEM**; `kid` = RFC 7638 JWK thumbprint (SHA-256, base64url); JWKS `{kty:RSA, use:sig, alg:RS256, kid, n, e}`; local dosya `0600`; aktif dosya yoksa ilk açılışta üretilir; bozuk/eksik dosya fail-fast; önceki public path opsiyonel ve 5 dk JWKS'te kalır.
  - **B)** RSA 4096; `kid` rastgele UUID; private PKCS#1.
  - **C)** Anahtar formatını developer seçsin; JWKS yalnız çalışma zamanında test edilsin.
- **Önerim:** **A.** 2048 bit RS256 için yeterli ve test hızı makul; thumbprint `kid`, anahtar değişiminde çakışmayı önler.
- **Gerekçe:** `kid` standardı, JWKS tüketicilerinin doğru anahtarı seçmesini sağlar; PKCS#8/SPKI Java ve Nimbus tarafından doğrudan okunur. Dosya izni ve fail-fast, yanlış anahtarla sessiz başlamayı önler.
- **Etki:** AC-3, AC-7.
- **Karar (2026-10-28):** **ONAYLANDI — A.**

### B-09 — JWKS cache/hata, clock skew ve rol hiyerarşisi belirsiz

- **Kategori:** Tanımsız hata / belirsiz AC.
- **Spec yeri:** REQ-3, AC-3, REQ-4.
- **Soru:** `hotel-api` JWKS'i ne kadar cache'ler? Bilinmeyen `kid`de kaç kez yeniler? JWKS erişilemezse 401 mi 503 mü? Saat kayması toleransı ne? `SUPER_ADMIN`, `MANAGER`, `FRONT_DESK` arasında hiyerarşi var mı?
- **Neden açık:** Spec cache ve unknown `kid` refresh diyor; TTL, cooldown, fetch timeout ve hata durumu yok. `docs/domain.md` rol tablosunda yetkiler `?`; 0004 test controller'ı için hangi rolün neyi göreceği tanımsız.
- **Seçenekler:**
  - **A)** JWKS cache **5 dk**; unknown `kid`de tek refresh, başarısızsa **60 sn cooldown**; fetch timeout **3 sn**; JWKS erişilemezse **503 `system.http_503`** (401 değil); startup'ta JWKS fetch edilmez, ilk korumalı istekte denenir; clock skew **60 sn**; **V1'de rol hiyerarşisi yoktur**, her uç izinli rolleri açıkça belirtir (0004 test controller matrisi: authenticated tüm roller, manager MANAGER+SUPER_ADMIN, super-admin yalnız SUPER_ADMIN).
  - **B)** Cache yok; her istekte JWKS; JWKS down 401; rol eşitliği (hiyerarşi yok).
  - **C)** Cache 1 saat; unknown `kid` refresh yok; hiyerarşi sonraki spec.
- **Önerim:** **A.** 0004'te yalnız test controller rol zorlaması kanıtlanır; iş uçları gelmediği için hiyerarşi burada sözleşme olarak tanımlanır.
- **Gerekçe:** JWKS down durumunu 401'e çevirmek, geçerli token'ı olan kullanıcıyı kimlik hatası gibi gösterir; 503 altyapı hatasını doğru temsil eder. 5 dk cache, 5 dk JWT TTL ile uyumludur; 60 sn skew makul saat farkını tolere eder.
- **Etki:** AC-3, AC-4, AC-7.
- **Karar (2026-10-28):** **ONAYLANDI — A.**

### B-10 — `hotel-api` 401/403 kodları ve rol zorlaması testi belirsiz

- **Kategori:** Belirsiz hata.
- **Spec yeri:** REQ-3, AC-3.
- **Soru:** Eksik/geçersiz JWT, yanlış `aud`/`iss`, expired ve yetersiz rol için tam `code` alanları ne? Test controller hangi rollerle hangi uçları açacak? Üretimde probe/scaffold olmadığı nasıl kanıtlanacak?
- **Neden açık:** Spec "401/403 RFC 7807" diyor; mevcut `ProblemDetailFactory` kodları var ama JWT için hangi kodun kullanılacağı açık değil. Test controller rol matrisi yok.
- **Seçenekler:**
  - **A)** 401 → `security.unauthorized` (eksik/imza/aud/iss/expired); 403 → `security.forbidden` (rol); `X-Request-Id` her yanıtta; test controller yalnız `src/test` altında `SUPER_ADMIN`, `MANAGER`, `FRONT_DESK` için üç endpoint açar; üretimde `git grep`/ArchUnit ile test controller yokluğu kanıtlanır.
  - **B)** 401/403 için modül önekli `auth.*` kodları; test controller main'de `@Profile("test")`.
  - **C)** Rol için 404 dön (bilgi gizleme).
- **Önerim:** **A.** Mevcut conventions `security.unauthorized`/`security.forbidden` ile uyumlu; 404 rol gizlemesi bu dilimde gerekli değil ve AC ile çelişir.
- **Gerekçe:** Kodların sabit olması istemci hata eşlemesini ve IT assert'lerini netleştirir; test-only controller üretim yüzeyini kirletmez.
- **Etki:** AC-3, AC-7.
- **Karar (2026-10-28):** **ONAYLANDI — A.**

### B-11 — Gateway timeout, hata eşlemesi ve header hijyeni belirsiz

- **Kategori:** Tanımsız hata.
- **Spec yeri:** REQ-4, AC-4.
- **Soru:** Timeout varsayılanı 3 sn; hangi env? Timeout/connection refused/upstream 5xx hangi status ve `code`? Upstream'e hangi header'lar gider, hangileri strip edilir? Upstream `Set-Cookie` ne olur? Body/query streaming nasıl?
- **Neden açık:** Spec "timeout ve hata eşlemesi dışında mantık yok" diyor; kesin status/kod/header listesi yok. Gateway IT'de mock upstream ile bu davranışlar doğrulanacak.
- **Seçenekler:**
  - **A)** `ADMIN_GATEWAY_TIMEOUT_MS=3000`; timeout → **504 `system.gateway_timeout`**; connection refused → **502 `system.gateway_unavailable`**; upstream 4xx/5xx status/body korunur, `Set-Cookie` ve `Server` strip; request header allowlist: `Accept`, `Content-Type`, `Content-Length`, `X-Request-Id` (server-generated); strip: `Cookie`, `Authorization`, tüm `X-*` (request-id hariç), `Origin`, `Referer`; response: `Content-Type`, `X-Request-Id` korunur; `Set-Cookie` strip; path/query/body aynen iletilir.
  - **B)** Upstream 5xx → 502; timeout → 503; tüm header'lar geçer.
  - **C)** Gateway her şeyi 500'e çevirir; header filtrelemez.
- **Önerim:** **A.** `X-Request-Id` gateway tarafından üretilir/doğrulanır; istemcinin gönderdiği değer güvenilmez. Büyük body için ayrı limit bu dilimde yok; streaming korunur.
- **Gerekçe:** 504 timeout, 502 bağlantı hatası için doğru ayrımdır; çerez/authorization sızıntısı önlenir. `Set-Cookie` strip, upstream'in admin-web oturumunu ezmesini engeller.
- **Etki:** AC-4, AC-7.
- **Karar (2026-10-28):** **ONAYLANDI — A.**
- **Ek karar:** `X-Request-Id` **gateway tarafından üretilir/doğrulanır**; istemcinin gönderdiği değer upstream'e taşınmaz. Gateway upstream'e yalnız kendi ürettiği request-id'yi yazar.

### B-12 — Audit taksonomisi, şema ve revocation olayları belirsiz

- **Kategori:** Belirsiz AC.
- **Spec yeri:** REQ-5, AC-5.
- **Soru:** Audit reason code'ları hangileri? Tablo/kolonlar ne? Audit actor'ı ve attempted username nasıl? `adminidentity`/`adminaudit` arasında FK var mı? "Oturum iptali" hangi olayları kapsar (logout, login rotasyonu, expiry, admin disable)? Audit fail-closed sırası tam olarak ne?
- **Neden açık:** Spec "neden kodu" ve "oturum iptali" diyor; kod listesi, tablo şeması ve revocation tetikleyicileri yok. `adminaudit` modülünün `adminidentity` tablosuna FK'i architecture sınırını ihlal edebilir.
- **Seçenekler:**
  - **A)** `admin_audit` tablosu: UUIDv7 id, actor_id nullable (FK yok), attempted_username, result (`SUCCESS`/`FAILURE`), reason_code, ip, user_agent (≤512), request_id, occurred_at; reason set: `LOGIN_SUCCESS`, `LOGIN_INVALID_CREDENTIALS`, `LOGIN_LOCKED`, `LOGIN_INACTIVE`, `LOGOUT`, `SESSION_REVOKED`, `BOOTSTRAP_CREATED`, `BOOTSTRAP_SKIPPED`; login rotasyonunda eski oturum `SESSION_REVOKED`; expiry ayrı `SESSION_EXPIRED` (veya kapsam dışı); 90 gün, job yok; FK yok (modül sınırı).
  - **B)** Serbest string reason; actor_id FK; yalnız login/logout; expiry audit yok.
  - **C)** Audit'i aynı transaction'da yaz; fail-closed davranışını kaldır.
- **Önerim:** **A.** Session revocation = explicit logout + login rotasyonu + admin tarafından iptal (0004'te yok); expiry pasif olduğu için `SESSION_EXPIRED` yalnız istekte tespit edilirse yazılır. Giriş başarısı: session tx A commit → audit tx B; B fail → session revoke + 500; failed login: audit B fail → 500; logout/iptal: audit fail logla, işlem sürer; bootstrap: audit fail → rollback/fail-fast.
- **Gerekçe:** Sabit reason seti test/raporlamayı mümkün kılar; FK'siz actor_id modül sınırını korur; fail-closed sırası güvenlik beklentisini açıkça tanımlar.
- **Etki:** AC-5, AC-7.
- **Karar (2026-10-28):** **ONAYLANDI — A.**
- **Ek karar:** Login rotasyonunda iptal edilen eski oturum **`SESSION_REVOKED`** olarak audit edilir; REQ-5 ve AC-5 buna göre güncellenir. Logout `LOGOUT` olarak kalır.

### B-13 — Trusted proxy ve IP/UA sınırları belirsiz

- **Kategori:** Eksik sınır / güvenlik.
- **Spec yeri:** REQ-5, AC-5.
- **Soru:** `ADMIN_TRUSTED_PROXY` tek IP mi CIDR mi? `X-Forwarded-For`'un ilk değeri her koşulda güvenilir mi? Proxy yoksa/başlık yoksa? IPv6 ve çoklu değer? User-Agent çok uzunsa?
- **Neden açık:** Spec "ADMIN_TRUSTED_PROXY tanımlıysa XFF ilk değeri" diyor; env tipi, güven koşulu ve spoofing koruması yok. İstemci doğrudan admin-api'ye erişebiliyorsa XFF spoof edilebilir.
- **Seçenekler:**
  - **A)** `ADMIN_TRUSTED_PROXY_CIDRS` virgülle ayrılmış IPv4/IPv6 CIDR; yalnız `request.remoteAddr` bu CIDR'lerdeyse XFF'in ilk değeri alınır; değer geçerli IP değilse remoteAddr'a düşülür; proxy'nin XFF'i overwrite etmesi beklenir; UA max 512 karaktere kırpılır; yoksa boş string.
  - **B)** Env varsa XFF ilk değeri koşulsuz kullanılır.
  - **C)** XFF hiç kullanılmaz; her zaman remoteAddr.
- **Önerim:** **A.** Tek bir IP yerine CIDR, prod proxy'lerin değişebilmesini destekler; koşullu güven spoofing'i engeller.
- **Gerekçe:** Audit IP'si hukuki/operasyonel kayıt; yanlış IP audit'i değersizleştirir. UA uzunluğu DB taşmasını önler.
- **Etki:** AC-5.
- **Karar (2026-10-28):** **ONAYLANDI — A.**

### B-14 — Bootstrap CLI, exit kodları ve SUPER_ADMIN kontrolü belirsiz

- **Kategori:** Belirsiz AC.
- **Spec yeri:** REQ-1, §2.2.9, §3 Bootstrap, AC-1, `CLARIFY-2A`.
- **Soru:** Bootstrap nasıl çalıştırılır (`--bootstrap-admin`, ayrı main, Maven profili)? Hangi env'ler? Skip koşulu herhangi bir admin mi, yoksa yalnız **aktif SUPER_ADMIN** mi? Pasif SUPER_ADMIN veya yalnız MANAGER/FRONT_DESK varsa ne olur? Env eksik/zayıf/audit fail durumunda exit kodları ne? Audit fail'de admin kaydı rollback mi?
- **Neden açık:** Spec "ayrı CLI/init job" diyor; komut, aktivasyon ve exit sözleşmesi yok. "Admin varsa skip" ifadesi, yalnız FRONT_DESK veya pasif SUPER_ADMIN varsa aktif SUPER_ADMIN'siz bir sistem bırakabilir.
- **Seçenekler:**
  - **A)** `java -jar admin-api.jar --bootstrap-admin`; env `ADMIN_BOOTSTRAP_USERNAME/EMAIL/PASSWORD`; aynı parola politikası; **en az bir aktif SUPER_ADMIN varsa env kontrolünden önce skip + uyarı, exit 0**; aktif SUPER_ADMIN yokken herhangi bir admin satırı varsa (pasif SUPER_ADMIN, MANAGER veya FRONT_DESK) **fail-fast exit 2** (manuel karar); hiç admin yoksa env eksik/zayıf **exit 2**, DB hatası **exit 3**; admin oluşturma audit'i başarısızsa kayıt **rollback/delete** edilir (pending bırakılmaz, aktif admin kalmaz), exit 4; aynı `ADMIN_BOOTSTRAP_USERNAME` ile concurrent unique conflict → exit 2/0; normal startup'ta çalışmaz, CLI modunda web server başlatılmaz.
  - **B)** Herhangi bir admin varsa skip (spec literal); SUPER_ADMIN yoksa da exit 0.
  - **C)** Uygulama startup'ında otomatik bootstrap.
- **Önerim:** **A.** "Admin varsa skip" koşulunu **aktif SUPER_ADMIN**e daraltmak, yönetim kilidini önler; pasif SUPER_ADMIN veya yalnız MANAGER/FRONT_DESK varsa sessizce skip etmek yerine operatöre açık hata verir.
- **Gerekçe:** Bootstrap yalnız ilk kurulum içindir; yanlış skip, prod'da yönetim yapılamamasına yol açar. Exit kodları CI/init job'ın hatayı ayırt etmesini sağlar.
- **Etki:** REQ-1, AC-1, AC-7.
- **Karar (2026-10-28):** **ONAYLANDI — A.**
- **Ek karar:** Yalnız **aktif SUPER_ADMIN** varsa skip; pasif SUPER_ADMIN dahil herhangi bir admin satırı varsa **fail-fast**. Audit başarısızsa admin kaydı **rollback/delete** edilir; pending/pasif SUPER_ADMIN bırakılmaz, aktif admin oluşmaz. **Exit kodları:** 0 oluşturuldu/skip; 2 env eksik/zayıf veya aktif SUPER_ADMIN'siz mevcut admin; 3 DB hatası; 4 oluşturma audit hatası. Skip audit hatası loglanır ve exit 0 kalır (§3.2).
- **Kapsam:** B-14 **“en az bir aktif SUPER_ADMIN” guard'ıdır**; global “tek aktif SUPER_ADMIN” invariantı 0004 kapsamında değildir. Farklı `ADMIN_BOOTSTRAP_USERNAME` değerleriyle paralel bootstrap unique çakışması üretmez ve bu yarış testin kapsamı dışındadır.

### B-15 — Admin seed mekanizması, env, marker ve idempotency belirsiz

- **Kategori:** Belirsiz AC.
- **Spec yeri:** REQ-1, AC-1, AC-7, §3.
- **Soru:** Seed SQL mi Java runner mı? Hangi komut/profil? Env değişken adları ve local değerler ne? `hotel_admin` marker'ı init'e nasıl eklenir? İkinci çalıştırmada parola/rol güncellenir mi? Üç rolün kimlik bilgileri nerede?
- **Neden açık:** Mevcut `V1` yalnız şema; `hotel_admin` marker'ı yok; `admin-api` POM'unda seed profili yok. SQL Flyway `.env` okuyamadığı için "env'den kimlik bilgileri" ile SQL seed çelişir.
- **Seçenekler:**
  - **A)** `db/seed/adminidentity/R__seed_admin_dev.sql` Flyway repeatable seed; sabit local-only parolalar için önceden hesaplanmış Argon2id hash'leri; `.env.example` aynı local parolaları E2E için listeler; üç hesap (`superadmin`/`manager`/`frontdesk`); `hotel_admin` `local-ci-seed` marker'ı ve local/CI host guard; `-Pseed -Dseed.env=local|ci`; `INSERT ... ON CONFLICT (lower(username)) DO NOTHING`; mevcut kayıtta parola/rol değiştirilmez; seed audit'e yazılmaz.
  - **B)** Ayrı Java `AdminSeedRunner`/CLI; env'den parola okuyup Argon2id hash üretir; `ADMIN_SEED_*` env; aynı marker/guard/idempotency.
  - **C)** Uygulama startup'ında otomatik seed.
- **Önerim:** **A.** Mevcut SQL repeatable yaklaşımı korunur; hash'ler önceden üretildiği için env'den secret okuma ve SQL placeholder riski yoktur. Marker `hotel_admin` init'e eklenir; mevcut hacimler `down -v` gerektirir.
- **Gerekçe:** Seed idempotency ve "parola değişmez" kuralı SQL `ON CONFLICT ... DO NOTHING` ile açıkça uygulanır; CI/local aynı komutu kullanır, prod'a marker/guard ile sızmaz.
- **Etki:** AC-1, AC-7.
- **Karar (2026-10-28):** **ONAYLANDI — A.**

### B-16 — Admin-web login testid'leri, redirect, CSRF retry ve logout hataları belirsiz

- **Kategori:** Belirsiz AC / tanımsız hata.
- **Spec yeri:** REQ-6, AC-6.
- **Soru:** Login formu için testid'ler ne? Login başarısında nereye gidilir, `next` parametresi var mı? `/me` 5xx olursa? CSRF fetch başarısızsa? 403 retry kaç kez, hangi isteklerde? Logout 403/500 olursa kullanıcı login'e döner mi?
- **Neden açık:** Spec login/loading/error diyor; testid, redirect ve hata davranışları yok. E2E `@admin` bu seçicilere ihtiyaç duyar. Mevcut admin-web yalnız scaffold kartı içeriyor; login yok.
- **Seçenekler:**
  - **A)** Testid seti: `admin-login-page`, `admin-login-form`, `admin-login-identifier`, `admin-login-password`, `admin-login-submit`, `admin-login-loading`, `admin-login-error`, `admin-login-retry`, `admin-home-page`, `admin-home-error`, `admin-home-retry`, `admin-user-menu`, `admin-logout`; login sayfası CSRF'i client-side `credentials: same-origin` ile alır; korumalı sayfa server-side `/me` çağrısını `ADMIN_API_INTERNAL_URL` ve gelen Cookie ile yapar; login sonrası yalnız `/` ile başlayan güvenli `returnTo`, yoksa `/`; `/me` 401 → `/login`, 5xx → `admin-home-error` + `admin-home-retry`; CSRF fetch fail → form error + retry; 403 CSRF → token yenile + **1** retry; logout 401 → login, 5xx/ağ → oturum açık + hata/retry, başarılı logout → login; scaffold anon 401, auth 404.
  - **B)** Testid yok, CSS/metin seçici; `next` query; sınırsız retry; logout hatasında login'e zorla.
  - **C)** Login başarısında her zaman `/login` kal; ayrı protected home yok.
- **Önerim:** **A.** Form `autocomplete="username"`/`"current-password"`, required; generic hata metni; boş durum yok. Logout başarısında `/login`.
- **Gerekçe:** Sabit testid'ler E2E'yi dile/metne bağlı olmaktan çıkarır; sınırlı retry çift oturum/çift logout riskini azaltır. 5xx'te login'e zorlamak, geçici altyapı hatasını oturum hatası gibi göstermemeli.
- **Etki:** AC-6, AC-7.
- **Karar (2026-10-28):** **ONAYLANDI — A.**

### B-17 — Admin E2E/CI anahtar, seed, 0002 smoke ve kanıt akışı belirsiz

- **Kategori:** Belirsiz AC / kanıt.
- **Spec yeri:** REQ-6, REQ-7, AC-6, AC-7.
- **Soru:** CI'da admin seed ve JWT anahtarı nasıl hazırlanır? `@admin` testleri hangi aşamada? 0002'deki admin-web smoke testi tam olarak neyle değişir; 0002 `@empty`/`@seeded` sayıları ne olur? 0004 kanıtları ve arşiv SHA'sı nasıl üretilir?
- **Neden açık:** Mevcut CI'da admin seed, admin E2E, JWT key ve `@admin` yok; `0002` smoke'ta admin-web üç durum testi var. Spec "0002 smoke yeşil kalır" ve "admin-web smoke 0004 login smoke ile değişir" ifadeleri test sayısı/tag açısından çelişebilir.
- **Seçenekler:**
  - **A)** Hotel seed'den sonra `@seeded` 0002 web smoke **2/2** (loading/error + showcase) kalır; admin-web üç durum testi `tests/admin` altında `@admin` login/logout/protected testlerine dönüşür (3 test); `@empty` 0002 **1/1**; `@seeded` 0003 design 40/40; `@admin` 3/3. CI admin seed'i `hotel_admin` marker ile, admin-api `ADMIN_JWT_ACTIVE_KEY_PATH` temp dosya, `HOTEL_ADMIN_JWKS_URI` ile başlar; kanıt `specs/done/evidence/0004/`; 0001/2/3 SHA-256.
  - **B)** Admin-web testi `@seeded` içinde kalır ve login smoke'a dönüşür; 0002 `@seeded` 3/3 olur; admin seed CI'da `@seeded` öncesi çalışır.
  - **C)** Admin E2E ayrı job; 0002 smoke hiç değişmez (scaffold kaldırıldığı için mümkün değil).
- **Önerim:** **A.** Admin testleri `@admin` olarak ayrılır; böylece 0002 web smoke'unun anlamı korunur ve admin seed gereksinimi netleşir. 0002 admin-web testi arşiv kanıtı değil, canlı testtir; kaldırılması arşivi etkilemez.
- **Gerekçe:** Test sayıları/tag'ler CI ve kanıt raporunda deterministik olur; `@admin` başarısızlığı admin seed/key sorunundan ayrılabilir. 0004 evidence ve arşiv SHA'sı ayrı doğrulanır.
- **Etki:** AC-6, AC-7; CI.
- **Karar (2026-10-28):** **ONAYLANDI — A.**
- **Ek karar:** CI sırasında **0003 `@seeded` design 40/40** korunur; 0002 canlı smoke **3/3** (1 `@empty` + 2 `@seeded`), `@admin` 0004 login/logout/redirect testleri ayrı koşar.

---

## 3. Analistin spec rev.7 kontrol listesi

> **Baseline:** Spec rev.6, B-01…B-17'yi, B-14 aktif SUPER_ADMIN/audit rollback düzeltmesini ve rev.6 küçük düzeltmelerini kapsıyor; Takım Yöneticisi eşzamanlılık kapsamını **A** olarak onayladı. Aşağıdaki tabloda **altı madde doğrulaması** vardır; rev.7'de yalnız §3.2.1'deki iki doküman netleştirmesi işlenecek. Baseline ayrıntıları korunur:
> - username **3–40** ve `^[a-z0-9][a-z0-9._-]{2,39}$`
> - parola **512 UTF-8 byte** üst sınırı
> - seed için **SQL repeatable** yaklaşımı
> - admin-web'de güvenli `returnTo`
> - V1'de rol hiyerarşisi yok; uç bazlı roller
> - SQL seed'de önceden hesaplanmış Argon2id hash'leri

| # | Madde | Spec rev.6 durumu | Doğrulama |
|---|---|---|---|
| 1 | **B-03:** Kilit anahtarı **normalize identifier (username veya e-posta) + IP** | İşlendi (§2.2.2, AC-2) | Doğrula; eksik yok |
| 2 | **B-07:** State-changing isteklerde **CSRF/Origin önce → 403**, sonra 400/401/403/500 | İşlendi (§2.2.4, AC-2) | Doğrula; AC-2 ile §2.2.4 aynı mı |
| 3 | **B-11:** `X-Request-Id` **gateway tarafından üretilir/doğrulanır**; istemci değeri taşınmaz | İşlendi (§2.2.7, AC-4) | Doğrula; header hijyeni IT |
| 4 | **B-12:** Login rotasyonunda eski oturum **`SESSION_REVOKED`**; logout `LOGOUT` | İşlendi (§2.2.8, REQ-5, AC-5) | Doğrula; event listesi |
| 5 | **B-14:** Bootstrap **yalnız aktif SUPER_ADMIN varsa skip**; pasif SUPER_ADMIN dahil diğer adminlerde **fail-fast**; audit fail → **rollback/delete**, pending bırakılmaz | **İşlendi:** REQ-1, §2.2.9 ×2, §3, AC-1, `CLARIFY-2A`; exit 0/2/3/4 | **Doğrula;** exit code brief düzeltmesi (env=2, DB=3) spec'i değiştirmez |
| 6 | **B-17:** CI'da **0003 `@seeded` design 40/40**; 0002 canlı smoke **3/3**; `@admin` 3/3 | İşlendi (§2.2.12, AC-7) | Doğrula; kanıt adları |

### 3.1 B-14 doğrulaması ve exit code düzeltmesi

Rev.5'te B-14 şu altı satırda işlendi ve doğrulandı:

| Yer | Satır | Rev.5 durumu |
|---|---|---|
| `REQ-1` | 26 | Aktif SUPER_ADMIN skip; audit fail rollback/delete |
| `§2.2.9` koşul | 112 | Aktif SUPER_ADMIN; pasif SUPER_ADMIN dahil fail-fast |
| `§2.2.9` exit | 113 | 0/2/3/4; audit fail rollback/delete |
| `§3` Bootstrap | 141 | Aynı kural |
| `AC-1` | 184-185 | Aynı kural + (a)-(e) testleri |
| `CLARIFY-2A` | 269 | Aynı kural |

**Brief düzeltmesi (spec değişmez):** Env eksik/zayıf için exit kodu **2** olarak kabul edildi; DB hatası **3**'tür. Nihai eşleme:

- **0** = oluşturuldu / skip
- **2** = env eksik/zayıf **veya** aktif SUPER_ADMIN'siz mevcut admin
- **3** = DB hatası
- **4** = oluşturma audit hatası (skip audit hatası loglanır, exit 0 kalır)

Spec §2.2.9 satır 113 zaten bu eşlemeyi taşıyor; bu nedenle **spec'e dokunulmaz**. Rev.6'da AC-1 testlerine (f) env eksik/zayıf → exit 2 ve (g) DB hatası → exit 3 eklendi.

### 3.2 Rev.6'da işlenen beş küçük düzeltme

Aşağıdaki maddeler spec rev.6'da işlendi; güvenlik davranışını değiştirmez, kayıt için burada tutulur. Eşzamanlılık kapsamı ayrıca §3.2 sonundaki kapsam notunda netleştirilmiştir:

| # | Düzeltme | Spec'te yer | Kabul/test |
|---|---|---|---|
| 1 | **Concurrent unique conflict (aynı username):** Aynı `ADMIN_BOOTSTRAP_USERNAME` ile iki CLI aynı anda çalışırsa unique çakışması → aktif SUPER_ADMIN oluştuysa **exit 0**; aksi halde **exit 2**. Farklı username'ler unique çakışması üretmez; bu testin kapsamı dışındadır. | §2.2.9, §3 | Aynı username ile eşzamanlı iki CLI; bu username için tek aktif SUPER_ADMIN, exit 0/2 |
| 2 | **CLI modu:** `--bootstrap-admin` çalışırken web server başlatılmaz; işlem bitince süreç exit koduyla kapanır. | §2.2.9 | CLI çıktısı; port dinlenmemesi |
| 3 | **`@admin` 3/3:** Test adları net üç test olacak: (a) başarılı login + korumalı sayfa, (b) başarısız login generic hata, (c) logout + login'e dönüş. | AC-7, §2.2.12 | `@admin` 3/3; 0002 3/3 ve 0003 40/40 korunur |
| 4 | **CSRF fetch fail:** Login sayfası CSRF token'ını alamazsa `admin-login-error` + `admin-login-retry` gösterilir; kullanıcı tekrar deneyebilir. | §2.2.11, AC-6 | Vitest/MSW: fetch fail → hata + retry |
| 5 | **`BOOTSTRAP_SKIPPED`:** Skip yolunda audit `BOOTSTRAP_SKIPPED` yazılır; audit yazılamazsa uyarı loglanır ve skip **exit 0** kalır (skip durum değiştirmez). | §2.2.9, §2.2.8, AC-5 | Skip audit; audit fail → exit 0 + log |

> **Kapsam notu (eşzamanlılık — Karar A, 2026-10-29):** B-14 genel “tek aktif SUPER_ADMIN” invariantı değil, **“en az bir aktif SUPER_ADMIN” bootstrap guard'ıdır**. Farklı `ADMIN_BOOTSTRAP_USERNAME` değerleriyle paralel bootstrap unique çakışması üretmez; bu durumda birden çok aktif SUPER_ADMIN oluşabilir ve bu 0004 kapsamı dışındadır. Global tek-SUPER_ADMIN garantisi istenirse ayrı karar gerekir: **kısmi unique index** (`role='SUPER_ADMIN' AND active=true`) veya **bootstrap advisory lock**. Takım Yöneticisi **A seçeneğini** onayladı: 0004’te yeni kısıt yoktur. Spec §2.2.9 ve AC-1 same-username ile sınırlıdır; rev.7’de §3 özet satırı netleştirilecek ve §3 “Kapsam dışı”na global singleton notu eklenecek.

### 3.2.1 Rev.7 doküman netleştirmeleri (davranış değişmez)

1. **§3 Bootstrap özet satırı:** “eşzamanlı unique çakışması” → “**aynı `ADMIN_BOOTSTRAP_USERNAME` ile** eşzamanlı unique çakışması”.
2. **§3 Kapsam dışı:** “Global ‘tek aktif SUPER_ADMIN’ invariantı ve farklı `ADMIN_BOOTSTRAP_USERNAME` değerleriyle paralel bootstrap 0004 kapsamı dışıdır; 0004 yalnız ‘en az bir aktif SUPER_ADMIN’ guard’ı sağlar.” maddesi eklenir.

### 3.3 Tutarlılık kontrolleri

1. **Audit:** `SESSION_REVOKED` login rotasyonunda kullanılıyor; REQ-5/AC-5 aynı olay listesini taşımalı.
2. **Hata önceliği:** AC-2 ve §2.2.4 CSRF-first sırasını aynı söylemeli.
3. **Gateway:** İstek allowlist'inde istemci `X-Request-Id` yok; upstream'e yalnız gateway değeri gidiyor.
4. **CI:** 0002 3/3, 0003 40/40, `@admin` 3/3 ve 0004 evidence sırası korunmalı.
5. **Doküman/env:** `.env.example`'a `ADMIN_COOKIE_SECURE`, `ADMIN_CSRF_SECRET`, `ADMIN_ALLOWED_ORIGINS`, `ADMIN_TRUSTED_PROXY_CIDRS`, `ADMIN_GATEWAY_TIMEOUT_MS`, bootstrap env'leri ve seed/E2E parola değişkenleri; `docs/architecture.md` §4/§6, `docs/frontend.md`, `infra/README.md`, `e2e/README.md`.
6. **Kanıt:** 0004 evidence altında B-14 aktif/pasif/audit-fail, rotation audit, CSRF precedence, X-Request-Id hijyeni ve 0003 design 40/40; 0001/2/3 arşivleri SHA-256.

### 3.4 Analistin teslimi

- `specs/0004-admin-identity.md` rev.7 (Durum: Onay bekliyor).
- Yalnız §3.2.1'deki iki doküman netleştirmesi; davranış, test ve exit kodları değişmez.
- B-14 **A kararı** kaydedildi; yeni DB kısıtı, partial unique index veya advisory lock yok.
- Takım Yöneticisi'ne “iki madde nereye işlendi” kısa özeti.
- Spec onaylandıktan sonra developer `releasable`'dan `feature/0004-admin-identity` branch'ini açar.

## 4. Ön koşul notu

Spec 0004 şu an **Taslak (rev.6)** ve repo'da **untracked** durumdadır. Altın Kural 1 gereği spec onaylanmadan kod yazılmaz. Eşzamanlılık kapsamı **A onaylandı**; rev.7'de yalnız §3.2.1'deki iki doküman netleştirmesi işlenecek. Bunlar işlendikten sonra Takım Yöneticisi son spec onayını verir; ardından geliştirme branch'i `releasable`'dan açılır (`docs/git.md` §1).

> **Sonuç:** B-01…B-17 ve beş ek karar spec rev.6'da işlendi; B-14 doğrulandı. Exit code eşlemesi brief'te düzeltildi (env=2, DB=3; spec değişmez). Eşzamanlılık kapsamı **A onaylandı**: B-14 “en az bir aktif SUPER_ADMIN” guard'ı; global “tek aktif SUPER_ADMIN” invariantı 0004 dışı. Rev.7'de yalnız iki doküman netleştirmesi kaldı; bunlar işlendiğinde açık nokta kalmıyor.
