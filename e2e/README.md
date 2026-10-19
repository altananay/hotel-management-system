# Uçtan Uca (E2E) Smoke Testleri (`e2e/`)

Bu dizin, `web` (`:3000`) ve `admin-web` (`:3001`) uygulamalarının açılışını, dil geçişini ve üç zorunlu ekran durumunu (**yükleme**, **boş**, **hata**) doğrulayan kök Playwright projesini içerir (`docs/architecture.md` §11, `docs/testing.md` §3).

## Ön Koşullar ve Yerel Çalıştırma Adımları

Tüm komutlar **depo kökünden** çalıştırılır. `(cd <dizin> && <komut>)` alt-kabuğu komut bitince otomatik olarak eski dizine döner; uzun süren süreçleri (backend/frontend) ayrı terminallerde başlatın.

1. **Altyapıyı başlatın:**

   ```bash
   cp .env.example .env   # kökte .env yoksa; varsa atlayın
   docker compose --env-file .env -f infra/docker-compose.yml up -d --wait
   docker compose --env-file .env -f infra/docker-compose.yml --profile init run --rm minio-init
   ```

2. **Veritabanı şema geçişlerini (`migrator` ile) çalıştırın:**

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

5. **Playwright smoke testlerini çalıştırın:**

   ```bash
   (cd e2e && npm ci && npx playwright install chromium && npx playwright test)
   ```

Test koşumu sırasında kanıt ekran görüntüleri `specs/done/evidence/0001/` dizinine üretilir.
