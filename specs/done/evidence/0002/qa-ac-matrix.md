# QA — Kabul Kriteri ↔ Kanıt Matrisi ve Bulgular (Spec 0002)

> Bu dosya `specs/done/evidence/0002/` altında **yeni bir QA kanıt kaydıdır**. `specs/0002-room-catalog.md`, `specs/done/0001/` ve uygulama kodu değiştirilmemiştir.
>
> **Fix doğrulaması (2026-10-20):** QA-0002-1/2/3 bulguları `15c845b` head'inde kapatıldı; ayrıntılı son kontrol için bkz. `qa-fix-verification.md`.

- **Denetleyen:** QA (`docs/roles/qa.md`)
- **Tarih:** 2026-10-20
- **Denetlenen head:** `3e89c25` (`feature/0002-room-catalog`)
- **Spec:** `specs/done/0002-room-catalog.md` (denetim anında: `specs/0002-room-catalog.md`, rev.2, Durum: Onaylandı → Tamamlandı)
- **Ortam:** Windows 11 · Java 25.0.2 · Node 24.11.1 · Docker 28.5.1 / Compose 2.40.3 · PostgreSQL 17.4-alpine · Playwright 1.51.0 (Chromium)
- **Yöntem:** Bağımsız test koşumu, temiz yerel yığın, canlı HTTP/DB probe'ları ve görsel kanıt incelemesi. Kod/test/spec yazılmadı.

## 1. Koşum kaydı

| # | Komut | Sonuç |
|---|---|---|
| 1 | `backend/hotel-api> .\mvnw.cmd -B verify` | **BUILD SUCCESS** — 79 birim + 10 IT = **89 test**, 0 kırmızı; Spotless + Modulith/ArchUnit yeşil |
| 2 | `backend/admin-api> .\mvnw.cmd -B verify` | **BUILD SUCCESS** — 11 birim + 5 IT = **16 test**, 0 kırmızı |
| 3 | `web> cmd /c "npm run lint && npm run build && npm test"` | lint + build **0 hata**; 2 dosya, **9 test** yeşil |
| 4 | `admin-web> cmd /c "npm run lint && npm run build && npm test"` | lint + build **0 hata**; 2 dosya, **6 test** yeşil |
| 5 | `docker compose --env-file .env -f infra/docker-compose.yml down -v` → `up -d --wait` → `--profile init run --rm minio-init` | postgres/minio/mailpit **healthy**; `hotel-media` bucket oluşturuldu |
| 6 | Temiz DB, seed'siz migrate: `flyway:migrate` (`hotel-api` 5433; `admin-api` 5433) | `hotel_core` **7 migration**, `hotel_admin` **1 migration**; `catalog.room_types` ve `catalog.amenities` **0 satır** |
| 7 | Backend'ler `local` profili, frontend'ler `npm run start` | `:8081`/`:8082` health **200**; `:3000/tr` ve `:3001` **200** |
| 8 | `e2e> npx playwright test --grep @empty` (temiz DB) | **1/1 passed** |
| 9 | Seed: `flyway:migrate -Pseed -Dseed.env=local -DHOTEL_CORE_DB_URL=...5433` | **BUILD SUCCESS**; 4 oda tipi (3 aktif + 1 pasif), 5 olanak, 11 ilişki; `installed_by=migrator` |
| 10 | `e2e> npx playwright test --grep @seeded` | **3/3 passed** (toplam E2E **4/4**) |
| 11 | Canlı API/CORS/status probe'ları (`curl`) | Aşağıdaki AC-2/AC-4 satırlarındaki tüm durumlar beklendiği gibi |
| 12 | DB check-constraint probe'u (`psql`, rollback'li) | Uzunluk, `<`/`>`, C0/C1 (`chr(1)`, `chr(31)`, `chr(127)`, `chr(133)`), `max_guests`, `display_order`, amenity code kuralları **reddedildi** |
| 13 | Seed SQL'in `migrator` ile **doğrudan iki kez** çalıştırılması | Sayılar değişmedi: 4 oda / 5 olanak / 11 ilişki; mükerrer kayıt yok |
| 14 | Negatif seed guard'ları (missing/prod seed.env; markersiz DB) | Beklenen fail-fast: `division by zero`; markersiz DB'de seed reddedildi |
| 15 | `git status --short` + `git diff --check` | Temiz; `specs/done/evidence/0001/` değişmedi |

> **Kanıt notu:** QA, `specs/done/evidence/0002/` altındaki 8 PNG'yi yeniden üretti ve görsel olarak inceledi. Üç web PNG'si (TR/EN/loading) byte düzeyinde render zamanlamasına bağlı küçük farklar gösterdi; işlevsel durumlar aynıydı. Depoyu kirletmemek için commit'li kanıt dosyaları geri yüklendi (`git status` temiz).

## 2. AC ↔ kanıt matrisi

| AC | Kriter (özet) | Test / kanıt | Sonuç |
|---|---|---|---|
| **AC-1** | `catalog` V7 şeması; `RoomType`/`Amenity` domain kuralları; metin/kod sınırları; DB kolon/check kısıtları; `maxGuests` 1–20; sabit hata kodları; `double`/`float` yok | `RoomTypeTest` **11**, `AmenityTest` **14**, `HotelApiModularityTest` **9**, `RoomCatalogIT` **3**; `V7__create_room_types_and_amenities.sql`; QA DB probe'u (12) | **GEÇTİ** — test kapsamı notu: QA-0002-3 |
| **AC-2** | `GET /api/room-types`: anonim 200; `{items,page,size,total}`; aktif filtre; `ORDER BY display_order,id`; alan dışlamaları; `amenities` code ASC; varsayılan 0/20; 1–100 sınırı; 400 `validation.invalid_request`; taşan sayfa 200 boş; `sort` yok; `/admin-api/**` 401 | `RoomTypeControllerTest` **8**, `RoomCatalogQueryServiceTest` **7**, `RoomCatalogIT` **3**; QA canlı probe: `page=-1`, `page=abc`, `size=0`, `size=101` → **400 problem+json**; `page=1` → **200** boş + `total=3`; `sort` yok sayıldı; `/admin-api/...` → **401**; CORS allow 3000 / deny evil | **GEÇTİ** |
| **AC-3** | `/tr|en/rooms`; ana sayfa linki; locale switch; `page=0&size=100`; testid/metin seti; kapasite + olanaklar + placeholder; fiyat yok; loading/empty/error/full Vitest | `web` **9 test** (`room-showcase` 5 + i18n 4); `smoke.spec.ts` `@empty` + `@seeded`; QA: `/tr` ve `/en` ana sayfalarında `home-rooms-link` ve doğru href; `/en/rooms` ↔ `/tr/rooms` linkleri; 8 PNG | **GEÇTİ** |
| **AC-4** | `hotel-api` scaffold ucu kaldırıldı; `/api/system/sample-items` → **404 problem+json/system.not_found**; `web` `system-status` ve eski i18n anahtarları kaldırıldı; `api-client` korundu; `admin-api`/`admin-web` scaffold'u yeşil | `ApiSurfaceAndErrorHandlingTest` **13** (404 testi dahil); `admin-api` **16**; `admin-web` **6**; QA canlı: hotel-api 404, admin-api `GET /admin-api/system/sample-items` **200**; `web/src/features/system-status` yok; `web/src/lib/api-client.ts` mevcut; TR/EN üst-düzey eski anahtarlar yok | **GEÇTİ** |
| **AC-5** | Standart migrate seed'siz; seed ayrı `-Pseed -Dseed.env=local|ci`, `migrator`; `spring.flyway.enabled=false`; marker guard; 3 aktif + 1 pasif; 4–5 olanak; 2–3/oda; sabit UUID/`ON CONFLICT`; `CatalogSeedIT` doğrudan iki kez | Temiz migrate **0 satır**; seed **4/3/1/5/11**; `CatalogSeedIT` **2**; doğrudan iki kez idempotent; marker'sız DB reddi; missing/prod guard reddi; `flyway_history.installed_by=migrator`; `application.yml` `spring.flyway.enabled=false` | **GEÇTİ** — doküman/CI notları: QA-0002-1, QA-0002-2 |
| **AC-6** | `@empty` → seed → `@seeded`; `workers:1`, `retries:0`; TR/EN + dil geçişi; loading/hata/boş/dolu; admin-web üç durum; 8 görsel; CI akışı | `e2e/tests/smoke.spec.ts` **4 test** (1 empty + 3 seeded); `playwright.config.ts`; `.github/workflows/ci.yml`; QA temiz DB iki aşamalı koşum; 8 PNG görsel inceleme | **GEÇTİ** |

## 3. Bulgular

### QA-0002-1 — [Düşük] Yerel E2E/seed runbook'u özel `POSTGRES_PORT` değerini Maven komutlarına taşımıyor

- **Kanıt:** Bu makinede kök `.env` `POSTGRES_PORT=5433` ve compose Postgres `5433 -> 5432` çalışıyor. `infra/README.md` §Port senkronizasyonu 5433'ü destekliyor ve migrate için `-D` override'ı gösteriyor; ancak `e2e/README.md` adım 2 (satır 21–22) ve adım 6 (satır 48) ile `infra/README.md` seed bloğu (satır 85/88) bu override'ı içermiyor. QA'da `./mvnw flyway:migrate -Pseed -Dseed.env=local` (PowerShell'de `"-Dseed.env=local"`) **exit 1** verdi: `jdbc:postgresql://localhost:5432/hotel_core` → `password authentication failed for user "migrator"`. Aynı komut `-DHOTEL_CORE_DB_URL=jdbc:postgresql://localhost:5433/hotel_core` ile başarılı oldu.
- **Gerekçe:** Varsayılan `.env.example` (5432) ile akış çalışır; fakat dokümanda açıkça desteklenen 5433 senaryosunda "exact" runbook uygulanamıyor. Bu, yerel E2E/seed tekrarlanabilirliğini düşürür ve yanlışlıkla guard hatası gibi görünen auth hatası üretir.
- **Öneri:** `e2e/README.md` adım 2 ve 6'ya, `infra/README.md` seed bloğuna şu notu/alternatif komutu ekle: `POSTGRES_PORT != 5432 ise -DHOTEL_CORE_DB_URL=jdbc:postgresql://localhost:<port>/hotel_core` (ve admin için `HOTEL_ADMIN_DB_URL`). İstenirse iki README'de ortak bir "port override" örneğine atıf yap.
- **Sahip:** Developer (`e2e/README.md`, `infra/README.md`). Kod değişikliği gerekmez.

### QA-0002-2 — [Düşük] CI negatif seed guard'ları yalnızca sıfır-dışı exit kodu arıyor

- **Kanıt:** `.github/workflows/ci.yml` satır 147–158'de missing/prod komutları `set +e` ile çalıştırılıp yalnızca `$?` kontrol ediliyor; satır 160–172'de markersiz DB komutu `if ./mvnw ...; then` ile yalnızca başarısızlık aranıyor. QA'da yanlış port (5432) nedeniyle oluşan **auth hatası da exit 1** verdi; yani guard dışı bir nedenle test "geçmiş" sayılabilir. Gerçek marker guard'ı `CatalogSeedIT` içinde `division by zero` assert'iyle ayrıca korunuyor; bu bulgu CI hardening içindir.
- **Gerekçe:** Negatif testin doğru nedenle kırmızı olduğunu doğrulamazsa, DB erişilemezken veya yanlış adres verilirken guard testi yanlış pozitif üretir.
- **Öneri:** Komut çıktısını log dosyasına alıp `grep -q "division by zero"` (veya seed/marker'a özgü bir ifade) ile doğrula; markersiz DB adımında logda `hotel_core_guard_test`/`seed catalog dev` ve `division by zero` birlikte aransın. Alternatif olarak guard'ları JUnit IT'ye taşı (mevcut `CatalogSeedIT` gibi).
- **Sahip:** Developer (`.github/workflows/ci.yml`).

### QA-0002-3 — [Düşük] DB check-constraint test kapsamı V7'nin tüm sınırlarını otomatik doğrulamıyor

- **Kanıt:** `RoomCatalogIT.shouldEnforceDatabaseCheckAndUniqueConstraints...` yalnızca `max_guests=21`, `display_order=-1`, `<script>` içeren ad ve geçersiz amenity code'u test ediyor. V7'de ayrıca ad/açıklama uzunlukları (120/2000/80), yalnız boşluk, C0/C1 kontrol karakterleri ve `amenity_code` unique kısıtı var. QA'nın rollback'li psql probe'u bu kuralların **şu an** doğru reddettiğini kanıtladı; ancak otomatik regresyon testi yok.
- **Gerekçe:** AC-1 veritabanı kısıtlarını açıkça istiyor; ileride bir migration/constraint değişikliği bu sınırları kaldırırsa mevcut IT'ler bunu yakalamayabilir.
- **Öneri:** `RoomCatalogIT`'e parametreli geçersiz insert senaryoları ekle: 121/2001/81 karakter, yalnız boşluk, `chr(1)`/`chr(127)`/`chr(133)`, duplicate amenity code ve blank/null ad. Bu bir test güçlendirmesidir; üretim davranışı değişmez.
- **Sahip:** Developer (`RoomCatalogIT`).

## 4. Bloklamayan gözlemler ve DoD durumu

1. **Spec izlenebilirliği (analist notu):** AC-1'in "sabit hata kodları" listesinde `displayOrder` için kod yok; uygulama `catalog.room_type_invalid_display_order` kullanıyor ve `RoomTypeTest` bunu doğruluyor. Davranış doğru; liste kapsayıcı kabul edilecekse AC-1'e bu kodun eklenmesi önerilir. Kod değişikliği gerekmez.
2. **Görsel kanıt:** 8/8 PNG mevcut ve görsel olarak doğru durumları gösteriyor (boş, loading, error + retry, TR/EN dolu, admin loading/empty/error). QA yeniden koşumunda üç PNG byte düzeyinde farklılaştı; commit'li kanıtlar geri yüklendi.
3. **CI kanıtı:** Yerel tüm suite'ler yeşil. GitHub check-run'ları bu oturumda bağımsız sorgulanamadı (gh yok); `code-review-verification-2026-10-20.md` 5/5 success kaydını taşıyor. Nihai merge/CI onayı Takım Yöneticisi'nin kararıdır.
4. **DoD:** AC'ler ve kanıtlar tamam; denetim anında spec `specs/0002-room-catalog.md` altındaydı, kapanış adımında `specs/done/0002-room-catalog.md` olarak taşındı ve SCORECARD dolduruldu.

## 5. Sonuç

- **Bloklayıcı hata:** Yok.
- **AC kapsamı:** **6/6 AC** test ve/veya görsel kanıtla eşleşti.
- **Test toplamı:** Backend **105** (hotel-api 89 + admin-api 16), frontend **15** (web 9 + admin-web 6), E2E **4** (1 `@empty` + 3 `@seeded`).
- **Açık bulgular:** QA-0002-1, QA-0002-2, QA-0002-3 — üçü de **Düşük** ve bloklayıcı değil; sırasıyla doküman, CI test sağlamlığı ve test kapsamı iyileştirmesi.
- **QA imzası:** QA yalnızca okuma, çalıştırma ve kanıt toplama yaptı; uygulama kodu, testler ve spec değiştirilmedi. Bu rapor `specs/done/evidence/0002/` altına kanıt olarak eklendi.
