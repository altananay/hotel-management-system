# Spec 0004 — QA Bulgu ve Düzeltme Brief'i

> **Tarih:** 2026-11-01
> **İnceleme branch'i:** `integration` (`b3f4340` — `feat(admin): implement 0004 admin identity and sessions (#18)`)
> **Spec:** `specs/0004-admin-identity.md` rev.8 (Onaylandı)
> **Kapsam:** Yalnızca spec 0004 ve integration branch'i. Bu brief kod/spec değişikliği değildir; QA bulgularını ve önerilen düzeltmeleri geliştiriciye iletmek için hazırlanmıştır.
> **Sonuç:** Otomatik testlerin tamamı yeşil. Buna rağmen **1 yüksek, 2 orta, 2 düşük** önemde 5 bulgu tespit edildi. Bulgular mevcut testlerin kapsamadığı kenar durumlarıdır.

## 1. Test ortamı ve sonuçları

- Java **25.0.2**, Node **24.11.1**, Maven Wrapper, Docker **28.5.1**, PostgreSQL **17.4**.
- Branch: `integration` / `b3f4340`; testler bu commit üzerinde koşuldu.
- Yerel altyapı Docker Compose ile ayağa kaldırıldı (`hotel-postgres` 5433, MinIO, Mailpit); backend'ler 8081/8082, frontend'ler 3000/3001 üzerinde çalıştırıldı.
- Test öncesi `hotel_admin` migration + seed, `hotel_core` catalog seed uygulandı; testlerde geçici kanıt dizini kullanıldı.

| Koşum | Sonuç |
|---|---|
| `backend/admin-api` `mvnw -B verify` | **98/98** birim + **5/5** Testcontainers IT (`AdminDatabaseIsolationIT`) — BUILD SUCCESS |
| `backend/hotel-api` `mvnw -B verify` | **89/89** birim + **47/47** IT (`CatalogSeedIT` 2, `RoomCatalogIT` 40, `DatabaseSchemaAndRoleSeparationIT` 5) — BUILD SUCCESS |
| `admin-web` `npm test` | **23/23** Vitest |
| `admin-web` `npm run lint` + `npm run build` | Başarılı |
| E2E `@admin` | **3/3** (başarılı login + korumalı sayfa, hatalı login, logout) |
| 0002 smoke | **3/3** (`@empty` 1/1 + `@seeded` 2/2) |
| 0003 design | **40/40** (`@seeded`) |

> Not: `0002` `@empty` testi için catalog tabloları geçici olarak boşaltılıp test sonrası yeniden seed edildi; `hotel_admin` verisi korundu. `0001`/`0002`/`0003` arşiv dosyalarında değişiklik yoktur (`git diff` temiz).

## 2. Bulgu özeti

| ID | Önem | Alan | Bulgu | Durum |
|---|---|---|---|---|
| **F-01** | **Yüksek** | Bootstrap CLI | Bootstrap CLI, JWT anahtarı yokken context başlatamıyor; beklenen 0/2/3/4 yerine **exit 1** | Üretildi |
| **F-02** | **Orta** | Parola doğrulama | Login'de parola NFC normalize edilmiyor; aynı parolanın NFD girdisiyle giriş başarısız | Üretildi |
| **F-03** | **Orta** | Admin-web logout | CSRF 403 sonrası retry 401 dönerse login'e yönlendirmiyor; hata gösteriyor | Üretildi |
| **F-04** | **Düşük/Orta** | Kilit anahtarı | Bilinen kullanıcıda kilit anahtarı spec'teki normalize identifier değil, `user_id` | Spec/uygulama uyuşmazlığı |
| **F-05** | **Düşük** | Gateway | Gateway ikinci bir `X-Request-Id` üretiyor; admin-api log/audit ile upstream/yanıt ID'si ayrışıyor | Kod incelemesi |

## 3. Bulgular

### F-01 — Bootstrap CLI, JWT anahtarı yokken çalışmıyor (Yüksek)

**Beklenen (spec §2.2.9 / AC-1):**
`java -jar admin-api.jar --bootstrap-admin` yalnız DB ve `ADMIN_BOOTSTRAP_*` env'leriyle çalışmalı; aktif SUPER_ADMIN varsa `exit 0`, yoksa oluşturup `exit 0`, env hatası `2`, DB hatası `3`, audit hatası `4` dönmeli. CLI modunda web server başlamamalı.

**Gerçek:**
Aktif SUPER_ADMIN varken (skip beklenirken), `ADMIN_JWT_ACTIVE_KEY_PATH` dosyası yok ve `ADMIN_JWT_ALLOW_GENERATED_DEV_KEY=false` iken uygulama context'i açılmıyor:

```powershell
$env:ADMIN_JWT_ACTIVE_KEY_PATH = "$env:TEMP\missing-admin-key.pem"
$env:ADMIN_JWT_ALLOW_GENERATED_DEV_KEY = "false"
$env:HOTEL_ADMIN_DB_URL = "jdbc:postgresql://localhost:5433/hotel_admin"
$env:HOTEL_ADMIN_DB_USER = "app"
$env:HOTEL_ADMIN_DB_PASSWORD = "app_local_dev"
$env:ADMIN_BOOTSTRAP_USERNAME = "probe"
$env:ADMIN_BOOTSTRAP_EMAIL = "probe@example.com"
$env:ADMIN_BOOTSTRAP_PASSWORD = "Probe-Password-2026!"
java -jar backend\admin-api\target\admin-api-0.0.1-SNAPSHOT.jar --bootstrap-admin
```

Aktif SUPER_ADMIN varken beklenen `exit 0` (skip); gerçek `exit 1`. Hata:

```
Caused by: java.lang.IllegalStateException:
  Active admin JWT private key is missing: ...\missing-admin-key.pem
  at AdminJwtKeyProvider.initialize(AdminJwtKeyProvider.java:55)
...
EXIT=1
```

Aynı komut geçerli bir JWT anahtarıyla `EXIT=0` dönüyor; yani sorun bootstrap mantığında değil, CLI context'inin web/JWT bean'lerini de başlatmasında.

**Neden:**
- `AdminApiApplication` bootstrap modunda `WebApplicationType.NONE` seçse de tüm `@Component` bean'lerini tarıyor (`AdminApiApplication.java:16-19`).
- `AdminJwtKeyProvider` `@PostConstruct` ile koşulsuz anahtar okuyor/üretiyor (`AdminJwtKeyProvider.java:51-55`).
- `AdminIdentityService`, `AdminJwtService` ve `AdminCsrfService` üzerinden JWT/CSRF bağımlılıkları CLI'ya taşınıyor.
- `AdminApiApplicationTest` yalnız `WebApplicationType`'ı test ediyor; gerçek bootstrap context'i test edilmiyor.

**Etki:**
Prod init job'ının JWT anahtarına ihtiyacı varmış gibi davranıyor; spec'teki exit sözleşmesi bozuluyor ve otomasyon `1` kodunu env/DB/audit hatasından ayıramıyor. Bu, prod bootstrap'in çalışmamasına yol açabilir.

**Önerilen düzeltme:**
1. Bootstrap için ayrı ve dar bir context/component scan kullanın; `AdminBootstrapCli` yalnız `AdminBootstrapApi`/`AdminBootstrapService` üzerinden çalışsın.
2. CLI modunda `AdminJwtKeyProvider`, `AdminJwtService`, `AdminCsrfService`, `SecurityConfig`, `AdminIdentityFilter`, `AdminCsrfFilter`, `AdminGateway*` ve web controller'larını devre dışı bırakın (`@Profile("bootstrap")`/`@ConditionalOnWebApplication` veya ayrı `SpringApplication`).
3. Alternatif kısa çözüm: Bu bean'leri `@Lazy` yapmak yeterli değil; `AdminJwksController` gibi controller'lar da key provider'a bağımlıysa kapsam dışı bırakılmalı.
4. Test: `--bootstrap-admin` ile JWT anahtarı/CSRF secret olmadan tam context IT'si; aktif SUPER_ADMIN → `exit 0` + `BOOTSTRAP_SKIPPED`; admin yok → `exit 0` + `BOOTSTRAP_CREATED`; env eksik → `2`; DB down → `3`; audit fail → `4`; web portu dinlenmiyor.

---

### F-02 — Login'de parola NFC normalizasyonu eksik (Orta)

**Beklenen (spec §2.2.1):**
Parola NFC normalize edilir; aynı parolanın composed (`é`) ve decomposed (`e` + combining acute) biçimleri aynı hesaba giriş yapabilmelidir.

**Gerçek:**
`PasswordPolicy.validate` oluşturma sırasında NFC'ye çeviriyor; fakat `AdminIdentityService.login` doğrudan ham parolayı hash ile karşılaştırıyor (`AdminIdentityService.java:79`). Kod seviyesinde üretilen probe sonucu:

```
normalized_equals_composed=true
login_matches_decomposed=false
login_matches_normalized=true
```

Yani NFC ile kaydedilen bir parola, NFD girdisiyle girişte reddediliyor.

**Etki:**
Türkçe/Unicode karakter içeren parolalarda kullanıcı, klavye/IME normalizasyonuna göre giriş yapamayabilir; spec'in NFC kuralı ihlal edilir. Mevcut `PasswordPolicyTest` yalnız policy'yi test ediyor, login yolunu değil.

**Önerilen düzeltme:**
- `Argon2PasswordHasher.hash` ve `matches` içinde parolayı `Normalizer.normalize(..., Form.NFC)` ile normalize edin; veya `AdminIdentityService.login` içinde `matches` çağrısından önce normalize edin.
- Aynı yerde 512 UTF-8 byte sınırını da uygulayın (login request'i `@Size(max=512)` karakter sayıyor; byte değil).
- Test: `Argon2PasswordHasherTest` veya `AdminIdentityServiceTest` içinde NFC hash + NFD giriş ve NFD hash + NFC giriş başarılı olmalı; `Café`/`Cafe\u0301` örneği kullanılabilir.

---

### F-03 — Logout CSRF retry 401 durumunda login'e yönlendirmiyor (Orta)

**Beklenen (spec §2.2.11 / B-16):**
Logout 403 `security.csrf_invalid` → token yenile + **1** retry; logout 401 → login'e yönlendir; 5xx/ağ → oturum açık + hata/retry.

**Gerçek:**
`LogoutButton` ilk istekte 403 alırsa `fetchCsrfToken` ile retry yapıyor; ancak **retry'ın 401 dönmesi** aynı `catch` bloğunda ele alınmıyor. İkinci hata dış `catch`'e gidiyor, `setFailed(true)` çalışıyor ve kullanıcı `/login`'e yönlendirilmiyor (`admin-web/src/components/auth/logout-button.tsx:23-36`). Mevcut testler 403→başarı ve ilk istek 401'i kapsıyor; **403→401** yok.

**Üretim:**
Geçici Vitest senaryosu (403 ardından 401) beklendiği gibi başarısız oldu: `push("/login")` çağrılmadı, ekranda `Çıkış yapılamadı.` kaldı.

**Önerilen düzeltme:**
- Logout isteğini küçük bir helper'a alın; helper 401'i her zaman `router.push("/login")` ile eşlesin.
- 403 durumunda retry'ı helper üzerinden yapın; retry'ın 401'ini de aynı helper yakalasın.
- Test: `logout-button.test.tsx` içine `403 security.csrf_invalid` → `401 security.unauthorized` senaryosu; `push("/login")` ve alert olmamalı.

---

### F-04 — Kilit anahtarı spec'teki normalize identifier değil, bilinen kullanıcıda `user_id` (Düşük/Orta)

**Beklenen (spec §2.2.2 / AC-2):**
Kilit anahtarı **normalize identifier (kullanıcı adı veya e-posta) + istemci IP**; e-posta girişi de aynı sayaç anahtarını kullanır.

**Gerçek:**
`AdminIdentityService.java:67`:

```java
String lockoutKey = user == null ? normalizedIdentifier : user.id().toString();
```

Bilinen kullanıcıda `admin_login_attempts.identifier_norm` alanına UUID yazılıyor; `AdminIdentityServiceTest.shouldUseSameUserCounter_whenLoggingInWithEmailAlias` bunu açıkça doğruluyor. Bilinmeyen kullanıcıda normalize identifier kullanılıyor.

**Etki:**
- Güvenlik açısından daha güçlü olabilir: username/e-posta alias'ları aynı sayaçta birleşir.
- Ancak spec ve DB şemasıyla uyuşmuyor: `identifier_norm` artık identifier değil, UUID taşıyor; operasyonel sorgular/audit analizi yanıltıcı.
- Gelecekte kullanıcı silinip aynı username ile yeniden oluşturulursa eski kilit kayıtları yeni kullanıcıya uygulanmaz.

**Önerilen karar:**
- **Tercih A:** Davranışı koru (alias birleşik kilit daha güvenli) ve spec §2.2.2/AC-2'yi “bilinen kullanıcıda `user_id`, bilinmeyende normalize identifier” olarak güncelle; `identifier_norm` alanını `subject_key` olarak netleştir/rename et. Bu, Takım Yöneticisi + analist kararı gerektirir.
- **Tercih B:** Spec'e birebir uy: `lockoutKey = normalizedIdentifier`; username/e-posta sayaçlarını ayrı tut; alias birleşik kilit isteniyorsa ayrıca canonical subject key ekle.
- Test: seçilen davranışa göre `identifierNorm` beklentisini güncelle; alias senaryosunu koru.

---

### F-05 — Gateway ikinci `X-Request-Id` üretiyor; korelasyon kopuyor (Düşük)

**Beklenen (B-11 / AC-4):**
`X-Request-Id` gateway tarafından üretilir/doğrulanır; istemci değeri yok sayılır; upstream'e yalnız gateway'in değeri gider.

**Gerçek:**
`RequestIdFilter` admin-api kenarında bir ID üretiyor; `AdminGatewayClient.java:36` ayrıca yeni bir UUID üretiyor ve upstream'e onu yazıyor; controller yanıtı da bu ikinci ID'yi döndürüyor. Admin-api log/audit'inde birinci ID, upstream ve istemci yanıtında ikinci ID var. Mevcut `AC-4-request-id.log` yalnız spoof'un engellendiğini doğruluyor; ID eşitliğini test etmiyor.

**Etki:**
Bir gateway hatasında (502/504) istemcinin gördüğü `X-Request-Id` ile admin-api logları eşleşmeyebilir; operasyonel iz sürme zorlaşır.

**Önerilen düzeltme:**
- `AdminGatewayClient.exchange` içinde `RequestIdFilter.resolveRequestId(request)` kullanın; yeni ID üretmeyin.
- Aynı ID'yi upstream header'ına ve `GatewayResponse`/controller yanıtına taşıyın.
- IT: istemcinin gönderdiği geçersiz/spoof ID yok sayılır; admin-api log/response/upstream aynı ID'yi görür.

## 4. Önerilen sıra

1. **F-01** — prod/bootstrap blocker; önce düzeltilmeli.
2. **F-02** — kimlik doğrulama doğruluğu; aynı PR'da.
3. **F-03** — admin-web kullanıcı hatası; aynı PR'da.
4. **F-04** — karar: spec mi güncellenecek, kod mu? Karar verilmeden kod değiştirilmemeli.
5. **F-05** — düşük öncelik; aynı PR veya takip eden küçük düzeltme.

## 5. Doğrulananlar (bulgu değil)

- B-03 kilit algoritması: pencere/5 deneme/15 dk, identifier+IP; e-posta alias testi.
- B-07 CSRF/Origin önceliği: 403 → 400/401/403/500; AC-2 ve §2.2.4 uyumlu.
- B-11 header hijyeni: Cookie/Authorization/X-Request-Id spoof'u engelleniyor; `Set-Cookie`/`Server` strip.
- B-12 audit: `SESSION_REVOKED`, `BOOTSTRAP_SKIPPED`, fail-closed/fail-open ayrımı.
- B-14 bootstrap: aktif SUPER_ADMIN skip, pasif SUPER_ADMIN/manager fail-fast, audit fail rollback/delete; aynı username eşzamanlılığı.
- B-17 CI/E2E: 0002 3/3, 0003 40/40, `@admin` 3/3; arşivler değişmedi.
- Baseline: username 3–40, parola 512 byte, SQL repeatable seed, güvenli `returnTo`, rol hiyerarşisi yok, önceden hesaplanmış Argon2id hash'leri.

## 6. Notlar

- Bu brief hazırlanırken **kod, spec veya arşiv değiştirilmedi**; yalnızca testler için geçici altyapı/DB kullanıldı.
- Testler `integration` branch'inde `b3f4340` üzerinde koşuldu; test sonrası başlatılan backend/frontend süreçleri ve Docker Compose altyapısı durduruldu (DB volume korundu). Çalışma branch'i `feature/0004-admin-identity` olarak geri alındı.
- F-01/F-02/F-03 için önerilen testler eklenmeden “düzeltildi” kabul edilmemelidir.
