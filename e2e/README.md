# Uçtan Uca (E2E) Smoke Testleri (`e2e/`)

Bu dizin, `web` (`:3000`) ve `admin-web` (`:3001`) uygulamalarının açılışını, dil geçişini ve üç zorunlu ekran durumunu (**yükleme**, **boş**, **hata**) doğrulayan kök Playwright projesini içerir (`docs/architecture.md` §11, `docs/testing.md` §3).

## Ön Koşullar ve Yerel Çalıştırma Adımları

1. **Altyapıyı başlatın:**
   ```bash
   docker compose -f infra/docker-compose.yml up -d --wait
   ```

2. **Veritabanı şema geçişlerini (`migrator` ile) çalıştırın ve backend uygulamalarını başlatın:**
   ```bash
   cd backend/hotel-api && ./mvnw flyway:migrate && ./mvnw spring-boot:run
   cd backend/admin-api && ./mvnw flyway:migrate && ./mvnw spring-boot:run
   ```

3. **Frontend uygulamalarını (`web :3000` ve `admin-web :3001`) başlatın:**
   ```bash
   cd web && npm run build && npm run start
   cd admin-web && npm run build && npm run start
   ```

4. **Playwright smoke testlerini çalıştırın:**
   ```bash
   cd e2e
   npm ci
   npx playwright install chromium
   npx playwright test
   ```

Test koşumu sırasında kanıt ekran görüntüleri `specs/done/evidence/0001/` dizinine üretilir.
