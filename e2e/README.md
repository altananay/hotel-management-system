# Uçtan Uca (E2E) Smoke Testleri (`e2e/`)

Bu dizin, `web` (`:3000`) oda vitrininin (`/[locale]/rooms`) ve `admin-web` (`:3001`) uygulamasının açılışını, TR/EN dil geçişini ve zorunlu ekran durumlarını (**yükleme**, **boş**, **hata**, **dolu**) iki aşamalı (`@empty` ve `@seeded`) olarak doğrulayan kök Playwright projesini içerir (`docs/architecture.md` §11, `docs/testing.md` §3).

## İki Aşamalı Yerel Doğrulama Akışı (`@empty` → Seed → `@seeded`)

Tüm komutlar **depo kökünden** çalıştırılır. `(cd <dizin> && <komut>)` alt-kabuğu komut bitince otomatik olarak eski dizine döner; uzun süren süreçleri (backend/frontend) ayrı terminallerde başlatın.

1. **Veritabanını temizleyin ve altyapıyı başlatın (gerçek boş durum doğrulaması için):**

   ```bash
   cp .env.example .env   # kökte .env yoksa; varsa atlayın
   docker compose --env-file .env -f infra/docker-compose.yml down -v
   docker compose --env-file .env -f infra/docker-compose.yml up -d --wait
   docker compose --env-file .env -f infra/docker-compose.yml --profile init run --rm minio-init
   ```

2. **Standart veritabanı şema geçişlerini (seed'siz, `migrator` ile) çalıştırın:**

   ```bash
   (cd backend/hotel-api && ./mvnw flyway:migrate)
   (cd backend/admin-api && ./mvnw flyway:migrate)
   ```

3. **Backend uygulamalarını başlatın (ayrı terminaller; yerel `.env` override'ları için `local` profili):**

   ```bash
   (cd backend/hotel-api && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local)
   (cd backend/admin-api && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local)
   ```

4. **Frontend uygulamalarını (`web :3000` ve `admin-web :3001`) başlatın (ayrı terminaller):**

   ```bash
   (cd web && npm ci && npm run build && npm run start)
   (cd admin-web && npm ci && npm run build && npm run start)
   ```

5. **Aşama 1 (`@empty`): Seed öncesi gerçek uçtan boş vitrin durumunu doğrulayın:**

   ```bash
   (cd e2e && npm ci && npx playwright install chromium && npx playwright test --grep @empty)
   ```

6. **Örnek katalog verisini (`migrator` kimliğiyle) yükleyin:**

   ```bash
   (cd backend/hotel-api && ./mvnw flyway:migrate -Pseed -Dseed.env=local)
   ```

   > `seed` profili ortam guard'ı taşır: `-Dseed.env=local|ci` ve `localhost`/`127.0.0.1` JDBC host'u zorunludur; aksi halde fail-fast eder. Seed SQL ayrıca hedef veritabanında `local-ci-seed` local/CI database comment marker'ını arar (bkz. `infra/README.md`).

7. **Aşama 2 (`@seeded`): Dolu vitrin (TR/EN + dil geçişi), yükleme/hata ve `admin-web` smoke testlerini çalıştırın:**

   ```bash
   (cd e2e && npx playwright test --grep @seeded)
   ```

Test koşumu sırasında görsel kanıtlar `specs/done/evidence/0002/` dizinine üretilir (`specs/done/evidence/0001/` arşivi değiştirilmez).
