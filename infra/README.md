# Yerel Altyapı (`infra/`)

Bu dizin, geliştirme ortamı için gerekli altyapı servislerini (`PostgreSQL`, `MinIO`, `Mailpit`) ve veritabanı başlangıç betiklerini (`infra/postgres/init/`) içerir.

## Servisler ve Portlar (`docs/architecture.md` §11)

| Servis | Portlar | Açıklama |
|---|---|---|
| `postgres` | `5432` | Tek sunucuda iki veritabanı: `hotel_core` ve `hotel_admin`; roller: `migrator` (DDL) ve `app` (yalnız DML) |
| `minio` | `9000` (API) / `9001` (Konsol) | S3 uyumlu medya deposu (`minio-init` işi ile `hotel-media` bucket'ı otomatik açılır) |
| `mailpit` | `1025` (SMTP) / `8025` (Web UI) | Yerel e-posta yakalama ve test servisi |

## Tek Adımlık Başlatma (Quickstart)

```bash
docker compose -f infra/docker-compose.yml up -d --wait
```

Durumu doğrulamak için:

```bash
docker compose -f infra/docker-compose.yml ps
```

## Veritabanı Şema Geçişleri (Flyway — `migrator` Kimliğiyle)

Uygulama süreçleri `app` (yalnız DML) kullanıcısıyla ve `spring.flyway.enabled=false` ile çalışır. Şema geçişleri ayrı adımda `migrator` kullanıcısıyla çalıştırılır (`docs/architecture.md` §7):

```bash
# hotel-api (hotel_core üzerindeki 6 modül şeması)
cd backend/hotel-api && ./mvnw flyway:migrate

# admin-api (hotel_admin veritabanı)
cd backend/admin-api && ./mvnw flyway:migrate
```

## Sıfırlama (Geliştirme Verisini Temizleme)

```bash
docker compose -f infra/docker-compose.yml down -v
```
