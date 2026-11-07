# Runbook — Media Gateway Timeout Ölçümü (R-01)

**Kapsam:** `/admin-api/media/**` için `ADMIN_GATEWAY_MEDIA_TIMEOUT_MS` (varsayılan 15 sn) ve
`AdminGatewayClient` connect/read bütçesi.
**İlgili:** `docs/architecture.md` §6, `specs/done/0005-media.md` D17/R-01,
`specs/done/0004-admin-identity.md` §2.2.7 amendment.

## 1. Amaç

`complete`, `retry`, `publish`, `unpublish` ve `archive` çağrılarının p95/p99 süresini ölçmek;
15 sn bütçesinin yeterli olduğunu kanıtlamak veya asenkron tasarıma geçiş kararını vermek.
Gerçek S3 p95 ölçümü üretim benzeri ortam gerektirir; local MinIO tek başına kanıt sayılmaz.

## 2. Local gecikme enjeksiyonu

MinIO önüne TCP gecikme proxy'si koyun:

```bash
# Terminal 1: MinIO :9000 -> proxy :9100, her chunk'a 50 ms
LISTEN_PORT=9100 TARGET_PORT=9000 DELAY_MS=50 node infra/scripts/tcp-latency-proxy.mjs
```

hotel-api'yi proxy'yi kullanacak şekilde başlatın (public endpoint doğrudan MinIO'da kalır):

```bash
MEDIA_S3_ENDPOINT=http://localhost:9100 \
MEDIA_S3_PUBLIC_ENDPOINT=http://localhost:9000 \
java -jar backend/hotel-api/target/hotel-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

Ardından `@media` akışını veya admin ekranındaki beş operasyonu çalıştırın. Her istek için
gateway access logundan / tarayıcı DevTools'tan süreleri toplayın. En az 30 örnek hedefleyin.

## 3. Üretim ölçümü

1. S3 SDK istemci metriklerini (istek süresi, retry sayısı) ve admin-api gateway sürelerini
   gözlemlenebilir sistemde toplayın.
2. `complete`, `retry`, `publish`, `unpublish`, `archive` için ayrı p50/p95/p99 hesaplayın.
3. Sonuçları tarih, commit, ortam ve örnek sayısıyla `specs/done/evidence/0005/` altına yazın.

Örnek PromQL (gateway histogram metriği varsa):

```promql
histogram_quantile(0.95,
  sum by (le) (rate(admin_gateway_request_seconds_bucket{path=~"/admin-api/media/.*"}[15m])))
```

> Bu metrik şu an kodda yoksa gateway erişim logları veya reverse-proxy süreleri kullanılır;
> ölçüm yöntemi kanıt dosyasında açıkça yazılır.

## 4. Karar kuralı

- **p95 < 15 sn ve p99 < 15 sn:** 15 sn bütçesi yeterli; R-01 kapanır.
- **p95/p99 sınıra yakın veya aşıyor:** `/admin-api/media/**` timeout'u yükseltilir (0004
  amendment güncellenir) **veya** publish/cleanup kalıcı niyet + asenkron işleme alınır;
  admin polling ile sonucu görür.
- **Connect süresi 15 sn'yi aşıyor:** `ADMIN_GATEWAY_MEDIA_TIMEOUT_MS` connect + read bütçesidir;
  daha yavaş ağlarda değer büyütülür veya bağlantı havuzu/keep-alive stratejisi değiştirilir.

## 5. Mevcut durum (2026-11-07)

- Media'ya özel connect + read timeout kodu ve testi eklendi
  (`AdminGatewayClientTest.shouldUseMediaSpecificConnectTimeoutForMediaRequests`).
- Gerçek S3 p95/p99 ölçümü bu yerel ortamda yapılamadı; üretim benzeri ortam gerekiyor.
- Bu nedenle R-01 **koşullu** kapalıdır; ölçüm sonucu gelene kadar 15 sn güvenli üst sınırdır.
