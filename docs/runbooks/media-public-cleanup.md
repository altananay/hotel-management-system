# Runbook — Media Public Cleanup / Dead Letter

**Kapsam:** `media.media_public_cleanup_tasks` (Spec 0005, D15)
**Metrikler:** `media.public_cleanup.failures`, `media.public_cleanup.dead_lettered`
**Log:** `MediaPublicCleanupService` — dead-letter için ERROR, retry için WARN.

> **Önemli:** `unpublish`/`archive` için dönen `200`/`204`, public S3 nesnesinin
> silindiğinin kanıtı **değildir**. DB commit'i ile S3 silme atomik değildir; silme
> cleanup-intent + retry/dead-letter ile telafi edilir. Kalıcı doğrulama için aşağıdaki
> görev tablosu ve nesne erişimi birlikte kontrol edilir.

## 0. Alarm / uyarı

Micrometer metrik adları: `media.public_cleanup.failures`, `media.public_cleanup.dead_lettered`
(Prometheus: `media_public_cleanup_failures_total`, `media_public_cleanup_dead_lettered_total`).

Alarm tanımlarının **tek kaynağı** `infra/prometheus/media-public-cleanup.rules.yml` dosyasıdır;
ifadeler ve eşikler orada yaşar. Dağıtımda bu dosya Prometheus rule path'ine kopyalanır veya
mount edilir. Doğrulama: `promtool check rules infra/prometheus/media-public-cleanup.rules.yml`.

| Alert | Amaç | Severity |
|---|---|---|
| `MediaPublicCleanupDeadLettered` | Yeni dead-letter olayı; public nesne hâlâ erişilebilir olabilir | page |
| `MediaPublicCleanupFailures` | 15 dk içinde tekrarlayan cleanup hatası | warning |

`media_public_cleanup_dead_lettered_total` bir Micrometer **Counter**'dır; process ömrü boyunca
artmaya devam eder. Bu nedenle alarm, çıplak `> 0` yerine **yeni olayı** ölçen `increase(...[5m])`
ifadesini kullanır. `failures_total` artışı tek başına geçici S3 hatası olabilir; dead-letter eşiği
ayrıca izlenir. Metrikler hotel-api `/actuator/prometheus` ucundan yayınlanır; scrape yalnız
`management.prometheus.allowed-cidr` (varsayılan `127.0.0.1/32,::1/128`) kaynağına açıktır.

> Alarm kuralının canlı ortamda yüklü olduğu, ilk kurulumda ve her runbook
> değişikliğinde doğrulanır; kural dosyasının yolu operasyon envanterine işlenir.

## 1. Durum tespiti

```sql
-- Bekleyen / dead-letter görevler
SELECT id, public_storage_key, attempts, created_at, last_attempt_at, dead_lettered_at
FROM media.media_public_cleanup_tasks
ORDER BY created_at
LIMIT 100;

-- Sadece dead-letter
SELECT *
FROM media.media_public_cleanup_tasks
WHERE dead_lettered_at IS NOT NULL
ORDER BY dead_lettered_at;
```

Kontrol:
1. `media.public_cleanup.failures` artıyor mu?
2. `media.public_cleanup.dead_lettered` > 0 mı?
3. ERROR logunda `Public media cleanup dead-lettered` var mı?
4. İlgili `public_storage_key` hâlâ erişilebilir mi?

## 2. Karar

- **Nesne yayındaki bir medyaya aitse** (in-doubt publish / yanlış cleanup):
  görev satırını silin; nesneyi **silmeyin**.
- **Nesne artık hiçbir yayındaki varyanta ait değilse**:
  nesneyi S3/MinIO'dan silin, ardından görev satırını silin.
- **S3 geçici hata veriyorsa**: görev satırını bırakın; `attempts`/`last_attempt_at`
  değerlerini not edin ve aşağıdaki manuel retry yolunu kullanın.

## 3. Manuel retry

Görev dead-letter olsa bile uygulama `processNow` ile manuel işleyebilir. Operasyon
ekibi için geçici yol:

```sql
-- Dead-letter işaretini kaldır; scheduler 5 dk grace sonrası tekrar dener
UPDATE media.media_public_cleanup_tasks
SET dead_lettered_at = NULL,
    attempts = 0
WHERE id = :task_id;
```

Alternatif: nesneyi doğrudan silip görev satırını kaldırın:

```bash
mc rm local/hotel-media/<public_storage_key>
```

```sql
DELETE FROM media.media_public_cleanup_tasks WHERE id = :task_id;
```

## 4. Kalıcı düzeltme

- S3 kullanıcısında `s3:DeleteObject` izni olduğundan emin olun.
- Bucket policy/CORS değişikliklerinden sonra `minio-init`'in çalıştığını doğrulayın.
- Tekrarlayan hata varsa `MEDIA_CLEANUP_MAX_ATTEMPTS` ve alarm eşiğini gözden geçirin.

### 4.1 Orphan taraması (periyodik)

DB'de hiçbir public varyanta referans olmayan `public/` nesneleri, S3/DB atomik
olmadığı için oluşabilir. Tam otomatik silme yoktur; aşağıdaki iki liste
karşılaştırılarak manuel karar verilir.

```sql
-- DB'nin beklediği public anahtarlar
SELECT public_storage_key
FROM media.media_variants
WHERE public_storage_key IS NOT NULL
ORDER BY public_storage_key;
```

```bash
# Bucket'taki public nesneler (local örnek)
mc ls --recursive local/hotel-media/public
```

Farkta kalan anahtarlar için:

1. Anahtar `media_public_cleanup_tasks` içinde **pending** ise scheduler'ı bekleyin.
2. Görev **dead-letter** ise bölüm 3'teki manuel retry/silme adımını uygulayın.
3. Ne DB'de ne cleanup tablosunda referans yoksa nesneyi silin; silme sonrası
   `HEAD`/anonim GET ile `404` doğrulayın.

Önerilen sıklık: haftalık; ayrıca her dead-letter alarmından sonra.

## 5. Kapanış kontrolü

```sql
SELECT count(*) AS pending
FROM media.media_public_cleanup_tasks
WHERE dead_lettered_at IS NULL;

SELECT count(*) AS dead_lettered
FROM media.media_public_cleanup_tasks
WHERE dead_lettered_at IS NOT NULL;
```

Beklenen: her iki sayaç da 0; public API yalnız yayındaki medyayı döndürüyor.

Ek kapanış adımları:

1. Dead-letter görevlerinin `public_storage_key` değerleri için anonim GET/HEAD `404`.
2. Orphan taraması farkının boş olduğu doğrulanır.
3. Son 5 dakikada yeni dead-letter yok: `increase(media_public_cleanup_dead_lettered_total[5m]) == 0`.
4. Tatbikat kaydı: tarih, operatör, görev ID'leri ve silinen anahtarlar runbook
   ekine işlenir.

### 5.1 Tatbikat kaydı

Manuel retry tatbikatı en az üç ayda bir ve her runbook değişikliğinde yapılır.
Tatbikat, gerçek bir dead-letter görevini bölüm 3'teki adımlarla işler; sonuç
aşağıdaki tabloya eklenir. Kayıt, alarm kuralının ve operasyon adımlarının hâlâ
çalıştığının kanıtıdır.

| Tarih | Operatör | Görev ID | public_storage_key | Uygulanan adım | Sonuç | Alarm gözlemi |
|---|---|---|---|---|---|---|
| 2026-11-07 | AI agent (yerel) | `68cc126a-c062-4325-b6ad-1a7b46174f84` | `public/drill/68cc…/card.webp` | S3 erişilemez + `max-attempts=1` | `attempts=1`, `dead_lettered_at` doldu, ERROR log üretildi | Canlı alarm yok; kural dosyası `promtool` ile doğrulanacak |
| 2026-11-07 | AI agent (yerel) | `c7a23e6e-d446-4b8b-a364-65fcadf84048` | `public/drill-retry/c7a2…/card.webp` | Dead-letter sonrası `dead_lettered_at=NULL, attempts=0`; scheduler | Satır silindi (`count=0`) | Canlı alarm yok; manuel retry yolu doğrulandı |

Tatbikat sonunda bölüm 5'teki iki sayaç da 0 olmalıdır.
