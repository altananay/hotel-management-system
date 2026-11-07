# Runbook — Media Public Cleanup / Dead Letter

**Kapsam:** `media.media_public_cleanup_tasks` (Spec 0005, D15)
**Metrikler:** `media.public_cleanup.failures`, `media.public_cleanup.dead_lettered`
**Log:** `MediaPublicCleanupService` — dead-letter için ERROR, retry için WARN.

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
