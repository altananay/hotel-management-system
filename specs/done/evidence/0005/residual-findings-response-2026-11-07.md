# 0005 — Rezidüel QA Bulguları Yanıtı (RES-01…RES-03)

- **Tarih:** 2026-11-07
- **Branch:** `fix/0005-residual-findings`
- **PR:** [#30](https://github.com/altananay/hotel-management-system/pull/30)
- **PR head:** `f1fc3bc52a593141303af3b5f22e88412cd6991d`
- **Merge commit / integration:** `939791579fd26ae8aff4f7897336e175fb21c785`
- **PR CI:** run [42384917204](https://github.com/altananay/hotel-management-system/actions/runs/42384917204) — success
- **Integration CI:** run [42389678373](https://github.com/altananay/hotel-management-system/actions/runs/42389678373) — success
- **QA retest:** `specs/0005-qa-findings.md` §9 (2026-11-07) — koşullu geçer
- **Kaynak:** `specs/0005-qa-findings.md` §8.3
- **Kapsam:** RES-01 (text-mode SHA doğrulayıcı), RES-02 (0004 gateway timeout sözleşmesi), RES-03 (R-01/R-02 operasyonel kapanış)

## 1. RES-01 — Text-mode SHA-256 doğrulayıcı eklenen dosyaları tespit etmiyor

**Durum: Kapatıldı.**

`e2e/scripts/sha256-manifest.mjs` text mode'da artık manifestte listelenen spec klasörlerini
(`## 0001` … `## 0004`) kullanır. Bu klasörlerde manifestte bulunmayan bir dosya varsa
`added: <yol>` üretir ve sonuç `CHANGED` olur. Manifestte yer almayan klasörlerdeki dosyalar
(ör. aktif `0005/`) arşiv kontrolünü etkilemez.

- Regresyon testi: `e2e/scripts/sha256-manifest.test.mjs` — **4/4**
  - listelenen arşiv klasörüne dosya eklenince `CHANGED`
  - listelenmeyen klasöre dosya eklenince `UNCHANGED`
  - eksik/değişmiş dosya tespiti korunur
- CI: `e2e` job'ına `npm run test:manifest` adımı eklendi.
- `AC-6-archive-sha256.txt` başlığı yeni davranışı açıklar.

## 2. RES-02 — 0004 done spec'i gateway timeout'unu hâlâ 3 sn olarak tanımlıyor

**Durum: Kapatıldı.**

- `specs/done/0004-admin-identity.md` §2.2.7'ye **2026-11-07 tarihli amendment** eklendi:
  genel 3 sn korunur; `/admin-api/media/**` için connect + okuma
  `ADMIN_GATEWAY_MEDIA_TIMEOUT_MS` (varsayılan 15 sn).
- `specs/0005-media.md` §2.1 ve D17 kararı media timeout'unu açıkça tanımlar.
- `docs/architecture.md` §6 connect + okuma bütçesini belirtir.
- `.env.example` ve `backend/admin-api/src/main/resources/application.yml` anahtarları zaten mevcut.

## 3. RES-03 — R-01/R-02 operasyonel kapanış

### 3.1 R-01 — media timeout / gerçek S3 ölçümü

**Durum: Kod ve ölçüm altyapısı kapatıldı; gerçek S3 ölçümü için açık sınırlama.**

- Media yolları için ayrı `HttpClient` kullanılıyor; connect timeout da 15 sn
  (`AdminGatewayClient.clientFor`). Böylece yavaş bağlantıda istek 3 sn'de erken düşmüyor.
- Test: `AdminGatewayClientTest.shouldUseMediaSpecificConnectTimeoutForMediaRequests`,
  `shouldTreatOnlyMediaPathsAsMediaRequests` — **6/6**.
- Ölçüm runbook'u: `docs/runbooks/media-gateway-timeout.md`
- Gecikme enjeksiyonu: `infra/scripts/tcp-latency-proxy.mjs` (MinIO health üzerinden doğrulandı, `200`).
- **İtiraz/sınırlama:** Gerçek S3 veya üretim benzeri depolama bu yerel ortamda yoktur; p95/p99
  değeri üretilemez. 15 sn, connect + read için güvenli üst sınır kabul edilmiştir. Üretim
  ölçümü yapılana kadar R-01 **koşullu** kapalıdır; sonuç gelirse 15 sn veya asenkron strateji
  yeniden değerlendirilir.

### 3.2 R-02 — dead-letter canlı alarm/tatbikat

**Durum: Çalıştırılabilir kural + scrape ucu + yerel tatbikat kapatıldı; canlı Prometheus dağıtımı operasyonel adım.**

- Çalıştırılabilir kural dosyası: `infra/prometheus/media-public-cleanup.rules.yml`
  - `increase(media_public_cleanup_dead_lettered_total[5m]) > 0` (page)
  - `increase(media_public_cleanup_failures_total[15m]) > 0` (warning)
  - `promtool check rules infra/prometheus/media-public-cleanup.rules.yml` ile doğrulanır.
- Metrik export'u: hotel-api `micrometer-registry-prometheus` + `/actuator/prometheus`; scrape yalnız `management.prometheus.allowed-cidr` (varsayılan `127.0.0.1/32,::1/128`) kaynağına açık. CI'da scrape smoke adımı var.
- Yerel tatbikat: `specs/done/evidence/0005/R-02-dead-letter-drill-2026-11-07.md`
  - geçici görev `attempts=1` ile dead-letter oldu
  - `ERROR Public media cleanup dead-lettered...` logu üretildi
  - manuel retry (`dead_lettered_at=NULL, attempts=0`) sonrası scheduler satırı sildi
  - görev ve anahtar temizlendi; tablo sayacı 0
- **İtiraz/sınırlama:** Canlı Prometheus/Alertmanager bu repoda/ortamda yoktur; alarmın canlı
  tetiklendiği ancak dağıtım ortamında doğrulanabilir. Kural dosyası, scrape ucu ve tatbikat kaydı
  repoda mevcuttur; dağıtım sonrası ilk alarm testi operasyon ekibinin kapanış adımıdır.

## 4. Doğrulama

- `e2e`: `node --test scripts/sha256-manifest.test.mjs` → **4/4**
- `admin-api`: `AdminGatewayClientTest` → **6/6**; tam verify **104 unit + 5 IT**, Spotless temiz
- `hotel-api`: tam verify **189 unit + 58 IT**, Spotless temiz
- `promtool check rules infra/prometheus/media-public-cleanup.rules.yml` → **SUCCESS: 2 rules found**
- Canlı scrape smoke (`http://localhost:8081/actuator/prometheus`) → **200**:
  ```text
  media_public_cleanup_dead_lettered_total 0.0
  media_public_cleanup_failures_total 0.0
  ```
- R-02 tatbikatı: dead-letter + manuel retry gözlemlendi, temizlik doğrulandı
- PR #30 CI run `42384917204`: Backend, Frontend, E2E Smoke, CI Quality Gate **success**
- Integration merge `9397915` CI run `42389678373`: Backend, Frontend, E2E Smoke, CI Quality Gate **success**
- Bağımsız QA retesti (`specs/0005-qa-findings.md` §9): RES-01/RES-02 kapandı; RES-03 koşullu kapandı

## 5. Sonuç

RES-01 ve RES-02 tamamen kapatıldı. RES-03'ün kod/araç/runbook kısımları kapatıldı; gerçek S3
p95/p99 ölçümü ve canlı Prometheus alarm tetiklemesi bu ortamda üretilemediği için koşullu
kapanış olarak bırakıldı. Bu iki nokta için üretim/dağıtım erişimi gerekiyor.
