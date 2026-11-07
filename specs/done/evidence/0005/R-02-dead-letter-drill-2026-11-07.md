# 0005 — R-02 Dead-Letter Tatbikatı (yerel)

- **Tarih:** 2026-11-07 17:36 (Europe/Istanbul)
- **Operatör:** AI agent (yerel tatbikat)
- **Ortam:** Windows 11, Java 25.0.2, PostgreSQL 17.4 (`hotel_core`, port 5433), hotel-api `local` profili
- **Amaç:** S3 silme hatasında cleanup-intent'in `max-attempts` sonunda dead-letter'a düştüğünü, ERROR logunun üretildiğini ve runbook'taki manuel temizlik adımının uygulanabilir olduğunu doğrulamak.
- **Sınır:** Bu tatbikat yerel ortamda erişilemeyen S3 endpoint'i ile yapıldı; üretim Prometheus/Alertmanager dağıtımı ve canlı alarm tetiklemesi bu ortamda mevcut değildir. Çalıştırılabilir kural dosyası `infra/prometheus/media-public-cleanup.rules.yml` altındadır.

## 1. Başlangıç durumu

```text
SELECT count(*) FROM media.media_public_cleanup_tasks;
0
```

## 2. Geçici görev

```sql
INSERT INTO media.media_public_cleanup_tasks (id, public_storage_key, attempts, created_at)
VALUES ('68cc126a-c062-4325-b6ad-1a7b46174f84',
        'public/drill/68cc126a-c062-4325-b6ad-1a7b46174f84/card.webp',
        0, now() - interval '1 hour');
```

## 3. Uygulama başlatma

```bash
java -jar backend/hotel-api/target/hotel-api-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=local \
  --server.port=8091 \
  --hotel.media.s3.endpoint=http://127.0.0.1:9 \
  --hotel.media.cleanup.max-attempts=1 \
  --hotel.media.cleanup.grace=0s \
  --hotel.media.cleanup.initial-delay=1s \
  --hotel.media.cleanup.interval=2s
```

`http://127.0.0.1:9` bilinçli olarak kapalıdır; S3 `deleteObject` bağlantısı reddedilir.

## 4. Sonuç

Scheduler ilk denemede görevi dead-letter'a aldı:

```text
ROW=1|2026-11-07 14:36:44.083122+00
```

Yani `attempts = 1`, `dead_lettered_at` doludur.

Log (özet):

```text
2026-11-07T17:36:44.083+03:00 ERROR ... MediaPublicCleanupService :
Public media cleanup dead-lettered after 1 attempts:
public/drill/68cc126a-c062-4325-b6ad-1a7b46174f84/card.webp

com.hotel.media.api.MediaStorageException: Could not delete stored object.
Caused by: software.amazon.awssdk.core.exception.SdkClientException:
Unable to execute HTTP request: Connect to http://127.0.0.1:9 failed:
Connection refused: getsockopt (SDK Attempt Count: 4)
```

## 5. Temizlik ve kapanış

```sql
DELETE FROM media.media_public_cleanup_tasks
WHERE id = '68cc126a-c062-4325-b6ad-1a7b46174f84';
```

```text
DELETE 1
SELECT count(*) FROM media.media_public_cleanup_tasks;
0
```

- hotel-api `:8091` durduruldu; port dinlemiyor.
- Geçici görev ve test anahtarı silindi.
- Runbook kapanış kontrolü: pending = 0, dead-lettered = 0.

## 6. Manuel retry tatbikatı (ikinci görev)

Aynı gün ikinci bir görevle runbook §3 manuel retry yolu doğrulandı:

1. Görev `c7a23e6e-d446-4b8b-a364-65fcadf84048` (`public/drill-retry/.../card.webp`)
   erişilemeyen S3 endpoint'i ve `max-attempts=1` ile dead-letter'a alındı (`attempts=1`).
2. Uygulama durduruldu; runbook §3 uygulandı:

```sql
UPDATE media.media_public_cleanup_tasks
SET dead_lettered_at = NULL,
    attempts = 0
WHERE id = 'c7a23e6e-d446-4b8b-a364-65fcadf84048';
-- UPDATE 1; kontrol: 0|NULL
```

3. Uygulama geçerli MinIO endpoint'i ve `grace=0s`, `initial-delay=1s`, `interval=2s` ile
   yeniden başlatıldı.
4. Scheduler görevi işledi; `deleteObject` başarılı oldu ve satır silindi:

```text
PHASE2_ROW_DELETED=true
SELECT count(*) FROM media.media_public_cleanup_tasks;
0
```

Bu, runbook'taki "dead-letter işaretini kaldır; scheduler tekrar dener" adımının çalıştığını
gösterir. Tatbikat sonunda tablo sayacı 0'dır; geçici görev bırakılmamıştır.

## 7. Değerlendirme

| Kontrol | Sonuç |
|---|---|
| Cleanup hatası `failures` yolunu tetikledi | ✅ (S3 bağlantı reddi) |
| `max-attempts` sonunda `dead_lettered_at` yazıldı | ✅ (`attempts=1`) |
| ERROR logu üretildi | ✅ |
| Runbook manuel retry yolu (`dead_lettered_at=NULL`, `attempts=0`) | ✅ (scheduler satırı sildi) |
| Runbook manuel temizlik/`DELETE` yolu | ✅ (birinci tatbikatta satır silindi) |
| Çalıştırılabilir alarm kuralı repoda | ✅ `infra/prometheus/media-public-cleanup.rules.yml` |
| Prometheus scrape ucu (`/actuator/prometheus`) | ✅ registry + CIDR kısıtı; CI smoke adımı |
| Canlı Prometheus/Alertmanager tetiklemesi | ⏳ Dağıtım ortamı gerektirir; kural dosyası `promtool check rules` ile doğrulanabilir |

**Sonuç:** R-02'nin yerel tatbikat ve runbook kapanışı doğrulandı. Üretim alarm dağıtımı operasyonel kurulum adımı olarak açıktır.
