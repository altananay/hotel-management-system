# 0005 — QA Düzeltmeleri ve Rezidüel Doğrulama

- **Tarih:** 2026-11-07
- **QA-fix branch:** `fix/0005-qa-findings`
- **QA-fix base:** `integration` @ `179d3d04150f32d209c8a6f0214bace4e9e542c4`
- **QA-fix head:** `5fb88a6709d88fab936d956b4ac1c950c32c7f00` (PR #28)
- **QA-fix PR:** [#28](https://github.com/altananay/hotel-management-system/pull/28) — `fix/0005-qa-findings` → `integration`
- **QA-fix merge commit:** `06d8182cb2999911b18a0d54dbb9d09de25c0af6`
- **Rezidüel branch:** `fix/0005-residual-findings` (RES-01…RES-03; bu dosyadaki RES bölümleri 2026-11-07 amendment)
- **Kapsam:** `specs/0005-qa-findings.md` — QA-01…QA-07, R-01, R-02 + §8.3 RES-01…RES-03
- **Ortam:** Windows 11, Java 25.0.2 (Temurin), Node 24.11.1, npm 11.19.1, Docker Desktop; PostgreSQL 17.4-alpine, MinIO `RELEASE.2025-10-15T17-29-55Z`, Mailpit
- **Yöntem:** Tam backend `verify` (unit + gerçek PostgreSQL IT), frontend unit/lint/build, yerel `@media` E2E, SHA-256 arşiv doğrulaması, dead-letter tatbikatı

---

## 1. Backend

```
backend/hotel-api  ./mvnw.cmd -B verify
Spotless: clean (155 dosya)
Unit tests: 189 passed, 0 failed
Integration tests: 58 passed, 0 failed
  - CatalogSeedIT 2/2
  - RoomCatalogIT 40/40
  - DatabaseSchemaAndRoleSeparationIT 5/5
  - MediaIT 11/11 (gerçek PostgreSQL 17.4 + MinIO)
BUILD SUCCESS

backend/admin-api  ./mvnw.cmd -B verify
Spotless: clean (109 dosya)
Unit tests: 104 passed, 0 failed
Integration tests: 5 passed, 0 failed (AdminDatabaseIsolationIT)
BUILD SUCCESS
```

Yeni/güçlendirilmiş testler:

| Bulgu | Test |
|---|---|
| QA-01 | `MediaAdminServiceTest.shouldRejectCoverAsNotPublishedWhenExistingMediaHasNoRoomType`, `shouldReportNotFoundOnlyWhenCoverAssetDoesNotExist`; `MediaIT.shouldDistinguishMissingMediaFromExistingUnassignedMediaWhenSelectingCover` |
| QA-02 | `MediaAdminServiceTest.shouldAllowAltTextUpdateWhenAssignedRoomTypeIsNoLongerActive`, `shouldRejectAssignmentToDifferentInactiveRoomType`; `media-manager.test.tsx` pasif oda tipi + PATCH gövdesi |
| QA-03 | `media-manager.test.tsx` complete-422 → `FAILED` + retry; PUT-503 → `UPLOADED` + archive |
| QA-07 | `MediaPublicQueryServiceTest` (2 `DataAccessException` senaryosu + transaction başlangıç/commit hataları); `PublicMediaControllerTest.shouldReturn503WithMediaServiceCode_whenDependencyIsUnavailable` |
| F-01 | `MediaPublicQueryServiceTest.shouldMapTransactionStartFailureToServiceUnavailable`, `shouldMapTransactionCommitFailureToServiceUnavailable` (transaction sınırı `TransactionTemplate` ile servis içinde) |
| R-01 | `AdminGatewayClientTest.shouldUseMediaSpecificTimeoutForMediaRequests`, `shouldUseMediaSpecificConnectTimeoutForMediaRequests`, `shouldTreatOnlyMediaPathsAsMediaRequests` (6/6) |
| RES-01 | `e2e/scripts/sha256-manifest.test.mjs` 4/4; canlı arşivde geçici dosya → `CHANGED`/`added`, silinince `UNCHANGED` |
| RES-02 | 0004 §2.2.7 amendment + 0005 D17/§2.1 + `docs/architecture.md` §6 aynı timeout sözleşmesi |
| O-1 | `ApiSurfaceAndErrorHandlingTest.shouldAllowPrometheusScrapeOnlyFromConfiguredCidr`; `/actuator/prometheus` scrape smoke (CI + yerel 200) |
| R-02 | Dead-letter + manuel retry tatbikatı (`R-02-dead-letter-drill-2026-11-07.md`); `/actuator/prometheus` scrape smoke (CI) |

## 2. Frontend

```
web        npm test -> 43/43 passed (13 dosya)
           npm run lint -> eslint + tsc + guard:design temiz (53 token)
           npm run build -> success

admin-web  npm test -> 39/39 passed (8 dosya; MediaManager 13/13)
           npm run lint -> eslint + tsc temiz
           npm run build -> success
```

## 3. E2E (`@media`)

```
npx playwright test tests/media --grep "@media"
1 passed (16.0s)
```

Bu koşu F-01 transaction-boundary düzeltmesi sonrası güncel jar ile yapılmıştır.

Doğrulanan akış:

- SUPER_ADMIN login; boş medya durumu
- SVG (415) ve 15 MB aşımı (400) reddi
- Gerçek WebP fixture (`home-hero-poster-1280.webp`) → PROCESSING → READY
- TR/EN alt metin UI üzerinden kaydedildi; PATCH 200
- Oda atama → publish → cover
- Public `room-card-image`: WebP, TR sayfasında `alt=altTr`, EN sayfasında `alt=altEn`
- Public API `altTextTr`/`altTextEn` alanları doğrulandı
- FRONT_DESK `/admin-api/media` → 403
- Unpublish → public API `{items: []}`, `room-card-image` yok
- Archive + onay → kayıt arşivlendi, public kopyalar temizlendi

Kanıt dosyaları bu koşuda yeniden üretildi: `AC-5-public-media.log`, `AC-5-public-access.log`,
`AC-6-media-e2e-summary.md`, `AC-4-admin-media-states.md`, `AC-4-admin-media.png`,
`AC-5-room-card-image.png`.

Ek regresyon: `@admin` oturum testleri **3/3** geçti. Testin yeniden yazdığı 0004 kanıt
PNG'leri git'ten geri yüklendi; arşiv manifesti yeniden doğrulandı (129/129 UNCHANGED).

## 4. Migration doğrulaması

`public.flyway_schema_history` (migrator):

```
 version |          description          | success
---------+-------------------------------+---------
 8       | create media assets           | t
 9       | add media processing lease    | t
 10      | create public cleanup tasks   | t
 11      | harden public cleanup retries | t
```

V8–V11 yalnız `media` şemasına dokunur; V5 init korunur.

## 5. Arşiv bütünlüğü

```
node e2e/scripts/sha256-manifest.mjs verify specs/done/evidence specs/done/evidence/0005/AC-6-archive-sha256.txt
Listed files: 129
Result: UNCHANGED
```

0001–0004 kanıtları byte-identical; 0 missing, 0 changed.

Text manifest doğrulayıcı artık manifestte listelenen 0001–0004 klasörlerine eklenen yeni dosyaları
`added` olarak raporlar. Canlı regresyon (2026-11-07):

```text
# 0001/RES-01-temp-added.txt eklendi
Listed files: 129
Result: CHANGED
added: 0001/RES-01-temp-added.txt

# geçici dosya silindikten sonra
Listed files: 129
Result: UNCHANGED
```

Regresyon testi: `e2e/scripts/sha256-manifest.test.mjs` — **4/4**. Bu davranış RES-01'i kapatır;
0005 gibi manifestte yer almayan klasörlerdeki aktif kanıtlar arşiv kontrolünü etkilemez.

## 6. R-01 — media gateway timeout

- `/admin-api/media/**` için `ADMIN_GATEWAY_MEDIA_TIMEOUT_MS` (varsayılan 15 sn) kullanılır; diğer admin çağrıları 3 sn'de kalır.
- Connect ve okuma bütçesi birlikte media timeout'udur: `AdminGatewayClient` media yolları için ayrı `HttpClient` (connect timeout 15 sn) kullanır. `AdminGatewayClientTest` media yolunda uzun request/connect timeout'un, diğer yollarda kısa timeout'un uygulandığını doğrular.
- **Sınırlama:** Gerçek S3 üzerinde p95/p99 ölçümü bu ortamda mevcut değildir. 15 sn değeri, senkron S3 kopyalama/silme için güvenli üst sınır olarak seçilmiştir; ölçüm yöntemi `docs/runbooks/media-gateway-timeout.md`, gecikme enjeksiyonu `infra/scripts/tcp-latency-proxy.mjs`. Üretim ölçümü sonrası yeniden değerlendirilmelidir (RES-03).

## 7. R-02 — operasyonel kapanış

`docs/runbooks/media-public-cleanup.md` güncellendi:

- Prometheus alarm ifadeleri (`media_public_cleanup_failures_total`; dead-letter için `increase(media_public_cleanup_dead_lettered_total[5m]) > 0`)
- Çalıştırılabilir Prometheus/Alertmanager kural dosyası: `infra/prometheus/media-public-cleanup.rules.yml` (`promtool check rules ...`)
- Dead-letter inceleme ve manuel retry adımları
- Haftalık orphan taraması (DB public anahtarları ↔ MinIO `public/` listesi)
- “200/204, S3 silme kanıtı değildir” notu
- Tatbikat kayıt tablosu (tarih, operatör, görev ID, anahtar, sonuç, alarm gözlemi)
- Metrik export'u: hotel-api `micrometer-registry-prometheus` + `/actuator/prometheus`; scrape yalnız `management.prometheus.allowed-cidr` (varsayılan `127.0.0.1/32,::1/128`) kaynağına açık. CI'da scrape smoke adımı var.
- `promtool check rules infra/prometheus/media-public-cleanup.rules.yml` → **SUCCESS: 2 rules found**
- Canlı scrape smoke: `/actuator/prometheus` **200**; `media_public_cleanup_dead_lettered_total 0.0`, `media_public_cleanup_failures_total 0.0`
- Yerel dead-letter + manuel retry tatbikatı: `R-02-dead-letter-drill-2026-11-07.md`
- Kapanış kontrol listesi

## 8. CI

- PR #28 final head (`5fb88a6`) — run [42376555577](https://github.com/altananay/hotel-management-system/actions/runs/42376555577): Backend, Frontend, E2E Smoke ve CI Quality Gate **success**.
- Merge sonrası `integration` (`06d8182`) — run [42377168433](https://github.com/altananay/hotel-management-system/actions/runs/42377168433): Backend, Frontend, E2E Smoke ve CI Quality Gate **success**.
