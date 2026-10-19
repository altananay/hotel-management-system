# Code Review — Spec 0001 (Project Architecture)

- **İnceleyen:** Code Reviewer
- **Tarih:** 2026-10-18
- **Branch:** `feature/0001-project-architecture`
- **Commit aralığı:** `origin/feature/0001-project-architecture..HEAD`
  - `2b36372` — `[plan 0001/2]`
  - `da4f681` — `[plan 0001/3]`
  - `4a774b4` — `[plan 0001/4]`
  - `744356b` — `[plan 0001/5]`
  - `d80c291` — `[plan 0001/6]`
- **Durum:** Fix planı — Takım Yöneticisi kararları ve CR-2, CR-5, CR-6 ek koşulları Developer tarafından kabul edildi; fixlere başlanabilir.
- **Karar güncellemesi:** 2026-10-18 — CR-2, CR-5 ve CR-6 için ek koşullar Developer tarafından kabul edildi.

> Rol gereği test/derleme çalıştırılmadı. Bulgular statik diff incelemesi, spec ve mevcut kanıt dosyalarına dayanır.

---

## 1. Karar özeti

| ID | Karar | Not |
|---|---|---|
| CR-1 | Açık | admin-api varsayılan olarak tüm yüzeyi açıyor |
| CR-2 | **Kabul edildi** | Hedef + `public` şemasında `PUBLIC` USAGE revoke’u kabul edildi |
| CR-3 | Açık | hotel-api bilinmeyen yolları public bırakıyor |
| CR-4 | Açık | catch-all handler istemci hatalarını 500’e çeviriyor |
| CR-5 | **Kabul edildi** | Maven/Flyway, `POSTGRES_PORT` senkronu ve profil önerisi kabul edildi |
| CR-6 | **Kabul edildi** | Seçenek A + `sharedkernel` içinde `HttpStatus` yasağı kabul edildi |
| CR-7 | Açık | Rol ayrımı IT’si DML/default privilege kanıtlamıyor |
| CR-8 | Açık | admin DB izolasyon testi dar kapsamlı |
| CR-9 | Açık | Frontend hata şeması zorunlu alanları optional kabul ediyor |
| CR-10 | **Kabul edildi** | Boş kapalı izin listesi + mimari doküman notu |
| CR-11 | Açık | İki POM’da build konfigürasyonu tekrarı |
| CR-12 | Açık | `code in errorMap` prototype zincirine bakıyor |
| CR-13 | Açık | Node 24’e karşı `@types/node` 22 |
| CR-14 | Açık | E2E/README quickstart komutları sıralı çalışmıyor |
| SD-1 | Açık | AC-9 CI kanıtı yok |
| SD-2 | Açık | Spec/SCORECARD kapanışı yapılmamış |
| SD-3 | Açık | AC-2 kanıtı 5433 portunu gösteriyor |

---

## 2. Kararlaştırılan fixler

### [CR-2] `app` rolü `public` şemasına ve Flyway history’ye erişememeli — Yüksek

**Karar:** Hedef ve ek koşullar Developer tarafından kabul edildi. Uygulama aşağıdaki ek adımları içermelidir; aksi halde hedef karşılanmaz.

**Mevcut durum / kanıt:** `infra/postgres/init/01-init-roles-and-dbs.sql:36-44,52-60`

```sql
ALTER DEFAULT PRIVILEGES FOR ROLE migrator
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app;
...
ALTER DEFAULT PRIVILEGES FOR ROLE migrator IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app;
```

**Kabul edilen zorunlu ek:** PostgreSQL’de `public` şeması varsayılan olarak `PUBLIC` rolüne `USAGE` verir. `app` da `PUBLIC` üyesi olduğu için yalnızca `GRANT USAGE ... TO app` satırını silmek yeterli değildir. Init SQL’de açıkça:

```sql
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA public TO migrator;
-- app'e public USAGE verilmez
```

yapılmalıdır. Ayrıca global `ALTER DEFAULT PRIVILEGES FOR ROLE migrator` ve `IN SCHEMA public` blokları kaldırılmalı; DML grant’leri yalnız modül migration’larındaki `IN SCHEMA <modül>` bloklarından gelmelidir. `hotel_admin` için de aynı düzen uygulanmalıdır.

**Etki:** `app`, `public.flyway_schema_history` üzerinde DML yapabilir; migration geçmişi silinebilir/bozulabilir. Ayrıca `public` şemasına USAGE erişimi, hedeflenen “public’e hiç erişim yok” kuralını ihlal eder.

**Kabul kriteri:**
- `app` ile `SELECT * FROM public.flyway_schema_history` → permission denied.
- `app` ile `CREATE TABLE public.x (...)` → permission denied.
- Modül şemasındaki bir tabloya `app` ile `INSERT/SELECT/UPDATE/DELETE` → başarılı.
- Aynı testler `hotel_admin` için de geçerli.
- Modül entity’leri ileride `public` varsayımına değil, açık şema adına dayanmalı.
- Mevcut local volume’lar init script’i yeniden çalıştırmaz; bu nedenle fix sonrası `docker compose down -v` ile temiz kurulum yapılmalı veya mevcut DB’ler için ayrı ve idempotent bir revoke adımı eklenmeli.

---

### [CR-5] `.env` ve local config senkronizasyonu — Orta

**Karar:** Önerilen kombinasyon ve ek koşullar Developer tarafından kabul edildi.
- Init SQL korunacak; `.sh`’e çevrilmeyecek.
- Kullanılmayan `DB_MIGRATOR_USER`, `DB_MIGRATOR_PASSWORD`, `DB_APP_USER`, `DB_APP_PASSWORD`, `WEB_PORT`, `ADMIN_WEB_PORT` `.env.example`’dan çıkarılacak.
- DB rol parolalarının init SQL’de sabit local default olduğu `.env.example` içinde açıkça belgelenecek.
- Her iki `application.yml`’e `spring.config.import` eklenecek.
- `infra/README.md` içinde `docker compose --env-file .env -f infra/docker-compose.yml up -d --wait` belgelenecek.

**Kabul edilen ek koşullar:**

1. **Maven/Flyway hâlâ `.env` okumuyor.** `spring.config.import` yalnız Spring uygulamasını etkiler; `./mvnw flyway:migrate` POM’daki `${HOTEL_CORE_DB_URL}` / `${HOTEL_CORE_MIGRATOR_PASSWORD}` değerlerini kullanır. Bu nedenle:
   - `HOTEL_CORE_MIGRATOR_USER/PASSWORD` ve `HOTEL_ADMIN_MIGRATOR_USER/PASSWORD` ya `.env.example`’dan çıkarılmalı ve README’de yalnız `-D` ile override edilebilir Maven/Flyway CLI değişkenleri olarak belgelenmeli,
   - ya da Maven’ın bu değerleri okuyacağı açık bir mekanizma (script/`-D`/`${env.*}`) kurulmalıdır.
   Aksi halde `.env.example` “yalnız compose ve 4 uygulamanın okuduğu değişkenler” envanteri olmaz; ayrıca `.env`’de `HOTEL_CORE_DB_URL` değiştirilirse Spring yeni portu kullanırken Flyway eski portu kullanır.

2. **`POSTGRES_PORT` ile `HOTEL_CORE_DB_URL` senkron kalmalı.** `POSTGRES_PORT=5433` yapılıp `HOTEL_CORE_DB_URL` 5432’de kalırsa uygulama bağlanamaz. README’de bu bağ açıkça yazılmalı veya URL türetilebilir hale getirilmelidir.

3. **`spring.config.import` prod’a taşınmamalı.** `optional:file:../../.env[.properties]` her ortamda denenir; prod’da üst dizinde bir `.env` varsa yüklenebilir. En azından `local` profiline bağlanması veya `application-local.yml` içinde tutulması önerilir. Zorunlu değilse de bu risk README’de belirtilmelidir.

**Kabul kriteri:**
- `.env.example` içindeki her değişkenin gerçekten okunduğu bir yer var (compose, Spring, Next veya açıkça Maven CLI).
- Kök `.env` ile Spring property override’ı çalışıyor.
- `docker compose --env-file .env ...` ile compose override’ı çalışıyor.
- `flyway:migrate` için önerilen komut README’de net.
- `POSTGRES_PORT` / DB URL ilişkisi belgelenmiş.
- AC-1 envanteri ile dosya birebir uyumlu.

---

### [CR-6] `DomainError` → HTTP statü eşlemesi — Orta

**Karar:** Seçenek A ve aşağıdaki mimari sınır Developer tarafından kabul edildi: `DomainError` kategori taşıyacak; `room_not_available` → `409`, `not_found` → `404`, validation → `400`.

**Kabul edilen zorunlu sınır:** `sharedkernel` içine `org.springframework.http.HttpStatus` veya web kodu **konmamalıdır** (`docs/architecture.md` §8.4, `docs/conventions.md` §2.1). Bu nedenle:

- `sharedkernel.api` içinde domain-nötr bir kategori/enum tanımlanmalı (ör. `DomainErrorCategory { VALIDATION, NOT_FOUND, CONFLICT, FORBIDDEN, INTERNAL }`).
- `com.hotel.web.GlobalExceptionHandler` bu kategoriyi HTTP statüsüne çevirmeli; `ProblemDetailFactory` statü/title üretmeli.
- `sharedkernel` için “Spring web/http paketlerine bağımlı olamaz” ArchUnit kuralı eklenmeli.

**Etki:** Aksi halde `sharedkernel` web katmanına bağımlı hale gelir; bu, projenin temel mimari sınırını ihlal eder.

**Kabul kriteri:**
- `booking.room_not_available` → `409` ve `application/problem+json`.
- `system.not_found`/`*.not_found` → `404`.
- Validation hataları → `400`.
- `docs/conventions.md` §2.3 durum matrisi 409 (ve gerekiyorsa 403) ile güncellenmiş.
- `ApiSurfaceAndErrorHandlingTest` 400 yerine 409’u doğruluyor; 404/409 testleri var.
- `sharedkernel` içinde Spring HTTP/web import’u yok.

---

### [CR-10] Admin modülleri için boş kapalı izin listesi — Düşük-Orta

**Karar:** Kabul edildi (Developer tarafından da onaylandı). Spekülatif `gateway → adminidentity` kenarı eklenmeyecek.

**Uygulama:**
- `docs/architecture.md` §4’e admin modülleri için kapalı izin listesi kuralı eklenecek:
  - “Admin modülleri arasında onaylanmış çapraz bağımlılık yoktur; yeni izin gerektiğinde önce bu doküman güncellenir.”
- `AdminApiModularityTest` içine `HotelApiModularityTest`’teki kapalı allowlist döngüsünün aynısı eklenecek; başlangıç listesi boş.
- `adminidentity`, `adminaudit`, `preferences`, `gateway` arasında döngü oluşturmayan yasak kenar da test edilecek.
- Mevcut “business modülleri cross-cutting kök paketlere bağımlı olamaz” kuralı korunacak.

**Kabul kriteri:**
- Boş allowlist ile test yeşil.
- `adminaudit → adminidentity` gibi bir kenar eklendiğinde test kırmızı.
- `docs/architecture.md` §4 ile test listesi aynı kaynağı işaret ediyor.
- Gerçek admin kimlik/oturum spec’i gelene kadar yeni kenar eklenmiyor.

---

## 3. Açık bulgular

### [CR-1] admin-api varsayılan olarak tüm uçları açıyor — Yüksek

**Dosya:** `backend/admin-api/src/main/java/com/hotel/config/SecurityConfig.java:42-45`

```java
.requestMatchers("/admin-api/system/sample-items")
        .permitAll()
        .anyRequest()
        .permitAll())
```

**Etki:** Scaffold istisnası tüm admin yüzeyine genişletilmiş. İlk gerçek admin endpoint’i eklendiğinde varsayılan olarak kimliksiz açılır.

**Fix:** `anyRequest().denyAll()` veya `.authenticated()`; yalnız health + scaffold permit. `GET /admin-api/unknown` → `401` testi ekle.

---

### [CR-3] hotel-api bilinmeyen yolları public bırakıyor — Orta

**Dosya:** `backend/hotel-api/src/main/java/com/hotel/config/SecurityConfig.java:71-80`

```java
.requestMatchers("/api/**")
        .permitAll()
        .anyRequest()
        .permitAll())
```

**Etki:** `/api/**` ve `/actuator/health` dışındaki yollar da public; iki yüzey kuralı kapalı değil.

**Fix:** Açık allowlist + `anyRequest().denyAll()`; `/error` için açık handling; bilinmeyen path testi.

---

### [CR-4] Catch-all handler istemci hatalarını 500’e çeviriyor — Orta

**Dosya:** `backend/hotel-api/src/main/java/com/hotel/web/GlobalExceptionHandler.java:93-107`  
**Dosya:** `backend/admin-api/src/main/java/com/hotel/web/GlobalExceptionHandler.java:77-91`

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ProblemDetail> handleUnexpectedException(...) {
    ...
    HttpStatus.INTERNAL_SERVER_ERROR, ... "system.internal_error" ...
}
```

**Etki:** `HttpMediaTypeNotSupportedException` (415), `HttpMediaTypeNotAcceptableException` (406), `ResponseStatusException`, `HandlerMethodValidationException` (400), method-level `AccessDeniedException` (403) gibi hatalar catch-all’a düşüp 500 oluyor. Durum matrisi ve istemci davranışı bozuluyor.

**Fix:** `ErrorResponseException`/`ResponseStatusException` statusunu koru; `HandlerMethodValidationException`/`BindException` → 400; `AccessDeniedException` → 403; `HttpMediaTypeNotSupportedException` → 415. Her durum için test.

---

### [CR-7] Rol ayrımı IT’si DML/default privilege kanıtlamıyor — Orta

**Dosya:** `backend/hotel-api/src/test/java/com/hotel/DatabaseSchemaAndRoleSeparationIT.java:93-115`  
**Dosya:** `backend/admin-api/src/test/java/com/hotel/AdminDatabaseIsolationIT.java:80-94`

**Etki:** Hiç tablo oluşturulmadığı için `app`’in gerçekten DML yapabildiği ve `ALTER DEFAULT PRIVILEGES`’ın çalıştığı kanıtlanmıyor. Default privilege yanlış olsa bile test yeşil kalır.

**Fix:** Migrator ile test tablosu oluştur; `app` ile `INSERT/SELECT/UPDATE/DELETE`; DDL reddi; history DML reddi. Aynı testi `hotel_admin` için de ekle.

---

### [CR-8] admin DB izolasyon testi dar kapsamlı ve token’ı gizliyor — Orta

**Dosya:** `backend/admin-api/src/test/java/com/hotel/architecture/AdminApiModularityTest.java:187-192`

```java
String forbiddenDbToken = "hotel_" + "core";
String appYml = Files.readString(Path.of("src/main/resources/application.yml"));
String pomXml = Files.readString(Path.of("pom.xml"));

assertThat(appYml).contains("hotel_admin").doesNotContain(forbiddenDbToken);
assertThat(pomXml).contains("hotel_admin").doesNotContain(forbiddenDbToken);
```

**Etki:** Yalnız 2 dosya taranıyor; `src/main` altındaki bir referans kaçar. Token’ın parçalanması `git grep` kanıtını da zayıflatıyor.

**Fix:** `src/main` altındaki tüm `.java/.yml/.yaml/.properties` dosyalarını tara; test dizinini hariç tut. Token’ı literal kullan veya test resource’unda tut. CI grep’ini koru.

---

### [CR-9] Frontend hata şeması zorunlu alanları optional kabul ediyor — Orta-Düşük

**Dosya:** `web/src/lib/api-client.ts:4-12`  
**Dosya:** `admin-web/src/lib/api-client.ts:3-11`

```ts
export const ProblemDetailSchema = z.object({
  type: z.string().optional(),
  title: z.string().optional(),
  status: z.number().optional(),
  detail: z.string().optional(),
  instance: z.string().optional(),
  code: z.string().optional(),
  requestId: z.string().optional(),
});
```

**Etki:** `docs/conventions.md` §2.3 zorunlu alanları optional kabul edildiği için eksik/bozuk hata zarfı sessizce geçer; `code`/`requestId` kaybı generic mesaja düşer.

**Fix:** Zorunlu alanları required yap; `.strict()` veya bilinçli `.passthrough()`; geçersiz payload testi ekle.

---

### [CR-11] İki POM’da build konfigürasyonu tekrarı — Düşük

**Dosya:** `backend/hotel-api/pom.xml:20-33,140-243`  
**Dosya:** `backend/admin-api/pom.xml:20-33,140-239`

**Etki:** Sürüm/plugin değişiklikleri iki yerde; drift riski.

**Fix:** `backend/pom.xml` parent (`packaging=pom`) ile `dependencyManagement`/`pluginManagement`; uygulama POM’ları miras alsın.

---

### [CR-12] `code in errorMap` prototype zincirine bakıyor — Düşük

**Dosya:** `web/src/lib/i18n/index.ts:23-29`  
**Dosya:** `admin-web/src/lib/error-messages.ts:11-16`

```ts
if (code && code in errorMap) {
  return errorMap[code];
}
```

**Fix:** `Object.hasOwn(errorMap, code)` kullan.

---

### [CR-13] Node 24’e karşı `@types/node` 22 — Düşük

**Dosya:** `web/package.json:5-6,30`  
**Dosya:** `admin-web/package.json:5-6,30`  
**Dosya:** `e2e/package.json:5-6,12`

**Fix:** Üç package.json’da `@types/node` 24 hattına yükselt.

---

### [CR-14] E2E/README quickstart komutları — Düşük

**Dosya:** `e2e/README.md:12-22`

**Etki:** Sıralı `cd` komutları ikinci adımda yanlış dizine gider.

**Fix:** `(cd ... && ...)` alt shell’leri kullan veya her komutu ayrı terminalde çalıştır.

---

## 4. Spec / DoD bulguları

### [SD-1] AC-9 CI kanıtı eksik — Yüksek (DoD)

`specs/done/evidence/0001/` altında AC-9 için CI run/log veya required check ekran görüntüsü yok. `ci.yml` içinde `ci-gate` var; ancak branch protection’a bağlandığı ve yeşil koştuğu kanıtlanmıyor.

**Fix:** GitHub Actions run URL’si/logu + `CI Quality Gate` required check ekran görüntüsü ekle.

### [SD-2] Spec/SCORECARD kapanışı — Orta (DoD)

- Spec `specs/done/`e taşınmamış.
- §7 SCORECARD boş.
- DoD kutuları işaretlenmemiş.

**Fix:** İnceleme onayından sonra spec’i taşı, SCORECARD’ı doldur, DoD’yi kanıtlarla işaretle.

### [SD-3] AC-2 kanıtı 5433 portunu gösteriyor — Düşük

`specs/done/evidence/0001/AC-2-infra-health.txt:6`:

```text
hotel-postgres ... 0.0.0.0:5433->5432/tcp
```

Spec/README 5432 diyor.

**Fix:** Varsayılan 5432 ile yeniden üret veya yerel override’ı açıkça belgeleyip kanıta not düş.

---

## 5. AP-07 — Plan dışı değişiklikler

1. **Plan 0001/2:** `docs/conventions.md:61` migration adlandırması `V<yyyyMMddHHmm>` → `V<n>` değişti; commit mesajı yalnız `chore(infra)`. Spec kapsamında gerekli olsa da ayrı docs commit’i veya commit mesajında açık atıf beklenir.
2. **Plan 0001/6:** `backend/hotel-api/pom.xml` ve `backend/admin-api/pom.xml` içindeki Flyway property adları `hotel.flyway.*` → `HOTEL_*` değişti; commit mesajı `test(e2e)`. Build konfigürasyonu değişikliği commit mesajında görünmüyor.

**Öneri:** Gerekçelendir veya ayrı `chore`/`docs` commit’lerine ayır; onaysız kabul etme.

---

## 6. Fix sırası

1. **CR-1, CR-2** — güvenlik; merge öncesi.
2. **CR-3, CR-4, CR-5, CR-6** — API sözleşmesi ve local çalıştırma.
3. **CR-7, CR-8, CR-9** — test/kanıt ve frontend sözleşmesi.
4. **CR-10, CR-11, CR-12, CR-13, CR-14** — kalite/bakım.
5. **SD-1, SD-2, SD-3** — DoD kapanışı.

## 7. Fix kabul kriteri (Definition of Fix Done)

- Her bulgu için en az bir otomatik test veya kanıt.
- `./mvnw verify` iki backend’de yeşil; frontend lint/build/test yeşil; E2E smoke yeşil.
- Yeni testler AC referansı taşır.
- `docs/conventions.md` / `docs/architecture.md` değişiklikleri ilgili bulguyla birlikte yapılır.
- Fix sonrası spec `specs/done/`e taşınır; SCORECARD doldurulur; AC-9 kanıtı eklenir.

---

**İmza ilkesi:** “Bulur, gösterir, önerir; kodu eline almaz.”
