# QA — Fix Doğrulama Eki (Spec 0002)

> Bu dosya `specs/done/evidence/0002/qa-ac-matrix.md` raporunun **ekidir**. İlk QA denetimi `3e89c25` head'inde yapılmıştı; bu ek, QA-0002-1/2/3 bulgularının kapatıldığı `15c845b` head'indeki son kontrolü kaydeder. Uygulama kodu, testler ve spec değiştirilmemiştir.

- **Denetleyen:** QA (`docs/roles/qa.md`)
- **Tarih:** 2026-10-20
- **İlk denetim head:** `3e89c25`
- **Fix doğrulama head:** `15c845b` (`feature/0002-room-catalog`)
- **İncelenen aralık:** `3e89c25..15c845b` (5 dosya, +274/−38)
- **İlgili fix commit'leri:**
  - `1888dcc` — `docs: document postgres port overrides for e2e seed runbook [plan 0002/12]` (QA-0002-1)
  - `d48fc62` — `test(catalog): strengthen V7 constraint coverage [plan 0002/13]` (QA-0002-3)
  - `66bd20d` — `ci: assert seed guard failure reasons [plan 0002/14]` (QA-0002-2)
  - `15c845b` — `docs(qa): archive QA acceptance matrix [plan 0002/15]` (önceki QA raporu)

## 1. Son kontrol koşumları

| # | Komut | Sonuç |
|---|---|---|
| 1 | `backend/hotel-api> .\mvnw.cmd -B verify` | **BUILD SUCCESS** — 79 birim + 47 IT = **126 test**, 0 kırmızı; `RoomCatalogIT` **40 test**; Spotless 89 dosya temiz |
| 2 | `backend/admin-api> .\mvnw.cmd -B verify` | **BUILD SUCCESS** — 11 birim + 5 IT = **16 test**, 0 kırmızı; Spotless temiz |
| 3 | `web> npm run lint && npm run build` + `npm test` | lint + build **0 hata**; **9/9** test geçti (2 dosya) |
| 4 | `admin-web> npm run lint && npm run build` + `npm test` | lint + build **0 hata**; **6/6** test geçti (2 dosya) |
| 5 | Seed guard — eksik `seed.env` ve `seed.env=prod`, 5433 override ile | İkisi de **exit 1**; iki logda da beklenen `division by zero` guard hatası |
| 6 | Seed guard — markersiz `hotel_core_guard_test` DB'si, `-Dflyway.url` override ile | **exit 1**; logda hem `division by zero` hem `R__seed_catalog_dev.sql`; scratch DB silindi |
| 7 | README'deki `-D` port override'lı migrate komutları (scratch `hotel_core` + `hotel_admin`) | hotel-api **7 migration**, admin-api **1 migration**; ikisi de **BUILD SUCCESS** |
| 8 | README'deki `-D` port override'lı seed komutu (marker'lı scratch DB) | **BUILD SUCCESS**; sonuç **4 oda tipi / 5 olanak / 11 ilişki**; scratch DB silindi |
| 9 | `git diff 3e89c25..15c845b --check` ve `git status --short` | Diff temiz; çalışma ağacında yalnızca QA kanıt değişiklikleri (bu ek + rapor bağlantısı) var |

> **Not:** 5–6 numaralı guard kontrolleri CI'daki komutların yerel eşdeğeridir. Bu makinede `.env` `POSTGRES_PORT=5433` olduğu için komutlara `-DHOTEL_CORE_DB_URL=...5433...` eklendi; CI'da varsayılan 5432 kullanıldığından ek override gerekmez. Guard'ın **başarısızlık nedeni** aynı şekilde doğrulandı.

## 2. Bulgu kapanış durumu

| Bulgu | Fix | Doğrulama | Durum |
|---|---|---|---|
| **QA-0002-1** — Özel `POSTGRES_PORT` runbook'a taşınmıyor | `1888dcc`: `e2e/README.md` adım 2/6 ve `infra/README.md` seed bloğuna 5433 override notu/komutu | README'deki override'lı migrate ve seed komutları scratch DB'lerde başarılı; seed sayıları 4/5/11 | **KAPATILDI** |
| **QA-0002-2** — CI negatif guard'ları yalnızca exit koduna bakıyor | `66bd20d`: missing/prod ve marker adımlarında log dosyası + `division by zero` (marker adımında ayrıca `R__seed_catalog_dev.sql`) grep'i | Guard'lar beklenen nedenle exit 1 verdi; loglarda aranan ifadeler mevcut | **KAPATILDI** |
| **QA-0002-3** — V7 DB kısıtları otomatik testlerle tam kapsanmıyor | `d48fc62`: `RoomCatalogIT`'e 20 oda tipi + 16 olanak parametreli senaryosu, duplicate unique anahtar ve duplicate ilişki testleri | `RoomCatalogIT` 40 test, tamamı yeşil; uzunluk aşımı (121/2001/81), whitespace, null, C0/C1/DEL, `<`/`>`, `max_guests`, `display_order`, amenity code ve unique kısıtları reddediliyor | **KAPATILDI** |

## 3. Bloklamayan gözlemler

1. **Seed sonrası standart migrate:** Yerel `hotel_core` DB'sinde seed uygulanmışken `-Pseed` olmadan `flyway:migrate` çalıştırılırsa Flyway `Detected applied migration not resolved locally: seed catalog dev` hatası veriyor. Bu, seed'in ayrı bir location'da tanımlı repeatable migration olmasının beklenen sonucudur; README'deki E2E akışı `docker compose down -v` ile temiz başladığı için belgelenen akışı etkilemez. Seed'li bir DB'de devam eden geliştirici `-Pseed -Dseed.env=local` kullanmalı veya `flyway:repair` çalıştırmalıdır. Bu spec için kod/aksiyon gerektirmez; ileride `infra/README.md`'ye tek satırlık bir not olarak eklenebilir.
2. **CI kanıtı:** GitHub check-run'ları bu oturumda bağımsız sorgulanamadı (`gh` yok). `15c845b` head'i için gerçek CI koşusu merge/close öncesi Takım Yöneticisi tarafından doğrulanmalıdır; yerel eşdeğer koşumlar (backend, frontend testleri ve guard adımları) yeşildir.
3. **Kapsam:** Son kontrolde frontend lint/build ve testler yeniden koşuldu; E2E 4/4 sonucu ve 8/8 görsel kanıt önceki `3e89c25` denetiminden geçerlidir ve bu commit aralığında E2E/uygulama kodu değişmemiştir.

## 4. Sonuç

- **Bloklayıcı hata:** Yok.
- **QA-0002-1/2/3:** Üçü de kapatıldı.
- **Test toplamı (son koşum):** Backend **142** (hotel-api 126 + admin-api 16), frontend **15** (web 9 + admin-web 6); E2E **4/4** (önceki denetim).
- **Spec kapanışı:** AC-1…AC-6 kanıtları ve bulgu kapanışları tamam; spec `specs/done/0002-room-catalog.md` olarak arşivlendi ve SCORECARD dolduruldu.
- **QA imzası:** Yalnızca okuma, çalıştırma ve kanıt toplama yapıldı; uygulama kodu, testler ve spec değiştirilmedi.
