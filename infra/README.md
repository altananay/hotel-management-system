# Yerel Altyapı (`infra/`)

Bu dizin, geliştirme ortamı için gerekli altyapı servislerini (`PostgreSQL`, `MinIO`, `Mailpit`) ve veritabanı başlangıç betiklerini (`infra/postgres/init/`) içerir.

## Servisler ve Portlar (`docs/architecture.md` §11)

| Servis | Portlar | Açıklama |
|---|---|---|
| `postgres` | `5432` | Tek sunucuda iki veritabanı: `hotel_core` ve `hotel_admin`; roller: `migrator` (DDL) ve `app` (yalnız DML; `public` şemasına erişemez) |
| `minio` | `9000` (API) / `9001` (Konsol) | S3 uyumlu medya deposu; `hotel-media` bucket'ı tek seferlik `minio-init` init job'u ile açılır |
| `mailpit` | `1025` (SMTP) / `8025` (Web UI) | Yerel e-posta yakalama ve test servisi |

## Ortam Değişkenleri: Her Katman Nereden Okur?

| Katman | Okuduğu yer | Not |
|---|---|---|
| Docker Compose | Yalnızca `--env-file` ile verilen dosya | `docker compose --env-file .env -f infra/docker-compose.yml ...` |
| Spring (`hotel-api`, `admin-api`) | Kök `.env`, **yalnızca `local` profili** ile (`application-local.yml` → `spring.config.import`) | Prod'da `.env` okunmaz; varsayılanlar `application.yml`'dedir |
| Maven/Flyway CLI (`./mvnw flyway:migrate`) | POM property'leri (`.env` **okumaz**) | Override yalnızca `-D` ile (aşağıdaki örnek) |
| Next.js (`web`, `admin-web`) | Kendi dizinindeki `.env.local` veya süreç ortamı (kök `.env` **okunmaz**) | `NEXT_PUBLIC_API_BASE_URL`, `ADMIN_API_INTERNAL_URL` |

Kök `.env.example` tüm değişkenlerin envanteridir; `.env` olarak kopyalanır (açıkça işaretli yerel varsayılanlar sır sayılmaz — `docs/conventions.md` §2.4). DB rol parolaları `infra/postgres/init/01-init-roles-and-dbs.sql` içinde sabit local default'tur; `.env` bu parolaları değiştirmez.

### Port senkronizasyonu (önemli)

`POSTGRES_PORT` ile `HOTEL_CORE_DB_URL` / `HOTEL_ADMIN_DB_URL` **senkron** tutulmalıdır. Birini değiştirirseniz diğerini de güncelleyin; migration komutuna da aynı adresi `-D` ile verin. Örnek (yerel 5432 başka bir PostgreSQL tarafından kullanılıyorsa):

```bash
# .env içinde:
POSTGRES_PORT=5433
HOTEL_CORE_DB_URL=jdbc:postgresql://localhost:5433/hotel_core
HOTEL_ADMIN_DB_URL=jdbc:postgresql://localhost:5433/hotel_admin
```

```bash
docker compose --env-file .env -f infra/docker-compose.yml up -d --wait
docker compose --env-file .env -f infra/docker-compose.yml --profile init run --rm minio-init
(cd backend/hotel-api && ./mvnw flyway:migrate "-DHOTEL_CORE_DB_URL=jdbc:postgresql://localhost:5433/hotel_core")
(cd backend/admin-api && ./mvnw flyway:migrate "-DHOTEL_ADMIN_DB_URL=jdbc:postgresql://localhost:5433/hotel_admin")
```

## Tek Adımlık Başlatma (Quickstart)

```bash
cp .env.example .env
docker compose --env-file .env -f infra/docker-compose.yml up -d --wait
docker compose --env-file .env -f infra/docker-compose.yml --profile init run --rm minio-init
docker compose --env-file .env -f infra/docker-compose.yml ps
```

> `.env` dosyanız yoksa `--env-file .env` kısmını çıkarın; compose dosyadaki `:-` varsayılanlarını kullanır.
> İkinci komut tek seferlik `minio-init` job'udur (idempotent; `hotel-media` bucket'ını açar). `up --wait` exit eden job'ları hata saydığı için (docker/compose#10596) init job'u `--profile init` ile ayrı `run` edilir ve kendi exit koduyla doğrulanır.

## Veritabanı Şema Geçişleri (Flyway — `migrator` Kimliğiyle)

Uygulama süreçleri `app` (yalnız DML) kullanıcısıyla ve `spring.flyway.enabled=false` ile çalışır. Şema geçişleri ayrı adımda `migrator` kullanıcısıyla çalıştırılır (`docs/architecture.md` §7). Maven `.env` okumaz; farklı adres/kimlik için `-D` kullanın:

```bash
(cd backend/hotel-api && ./mvnw flyway:migrate)
(cd backend/admin-api && ./mvnw flyway:migrate)
```

Override örneği:

```bash
(cd backend/hotel-api && ./mvnw flyway:migrate \
  "-DHOTEL_CORE_DB_URL=jdbc:postgresql://localhost:5432/hotel_core" \
  "-DHOTEL_CORE_MIGRATOR_USER=migrator" \
  "-DHOTEL_CORE_MIGRATOR_PASSWORD=migrator_local_dev")
```

## Veritabanı Yetkileri

- `public` şeması uygulama erişimine **kapalıdır**: `app` rolü `public`'e erişemez (`public.flyway_schema_history` dahil); yalnız `migrator` `USAGE, CREATE` alır.
- DML yetkileri yalnız modül şemalarında, modül migration'larındaki `ALTER DEFAULT PRIVILEGES ... IN SCHEMA <modül>` bloklarıyla verilir.
- `infra/postgres/init/` değişiklikleri yalnızca **boş** bir veri hacminde çalışır; mevcut hacimle geliyorsanız sıfırlayın:

```bash
docker compose --env-file .env -f infra/docker-compose.yml down -v
docker compose --env-file .env -f infra/docker-compose.yml up -d --wait
docker compose --env-file .env -f infra/docker-compose.yml --profile init run --rm minio-init
```

## Sıfırlama (Geliştirme Verisini Temizleme)

```bash
docker compose --env-file .env -f infra/docker-compose.yml down -v
```
