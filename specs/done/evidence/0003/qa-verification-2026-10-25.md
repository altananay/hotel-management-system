# QA — 0003 Bağımsız Test ve Bulgu Raporu

> **Kapsam:** `specs/done/0003-ui-design-system.md` (kapanmış spec) ve `integration` üzerindeki uygulama.
> **Amaç:** Geliştiricinin testlerinden bağımsız olarak 0003 kabul kriterlerini, 0002 regresyonunu ve performans/erişilebilirlik kanıtlarını yeniden koşmak.
> **Not:** Bu raporda uygulama kodu, test kodu veya spec değiştirilmedi; yalnız okuma, çalıştırma ve kanıt toplama yapıldı. E2E kanıtları **geçici bir köke** yazıldı; `specs/done/evidence/0002/` ve `0003/` arşivleri değiştirilmedi.

- **Denetleyen:** QA
- **Tarih:** 2026-10-25
- **Denetlenen head:** `09a3cf0` (`integration`), feature merge `e15770c`
- **Spec:** `specs/done/0003-ui-design-system.md` (rev.3, Durum: Tamamlandı)
- **Ortam:** Windows 11 · Java 25.0.2 · Node 24.11.1 · Docker 28.5.1 · PostgreSQL 17.4-alpine · Playwright 1.51.0 (Chromium)
- **Yöntem:** Web lint/build/test, 0002 `@empty` + `@seeded`, 0003 `@design` (40 test), axe, bütçe/CLS testleri, 0002 SHA-256 izolasyonu ve bağımsız Lighthouse spot-check'i.
  - **Boş durum:** `@empty` öncesi `catalog.room_types`, `catalog.amenities` ve `catalog.room_type_amenities` boşaltıldı; seed `R__seed_catalog_dev.sql` ile `migrator` kimliğinde doğrudan çalıştırıldı. 0003 backend/DB değiştirmediği için migration tekrarı gerekmedi.

## 1. Koşum sonuçları

| # | Komut / kontrol | Sonuç |
|---|---|---|
| 1 | `web> npm run lint` | **Geçti** — ESLint + TypeScript + `guard:design` (53 token) |
| 2 | `web> npm test` | **39/39** test, 13 dosya |
| 3 | `web> npm run build` | **Geçti** — `guard:design`, `guard:assets`, Next.js production build |
| 4 | `e2e> npx playwright test tests/smoke.spec.ts --grep @empty` | **1/1** (@empty) |
| 5 | Seed (`R__seed_catalog_dev.sql`, `migrator`) | **4 oda tipi / 5 olanak / 11 ilişki** |
| 6 | `e2e> npx playwright test tests/smoke.spec.ts --grep @seeded` | **3/3** (@seeded smoke) |
| 7 | `e2e> npx playwright test tests/design --grep @seeded` | **40/40** design testi |
| 8 | `admin-web> npm run lint && npm run build && npm test` | **Geçti** — 6/6 test |
| 9 | `hotel-api> .\mvnw.cmd -B verify -Dspring-boot.repackage.skip=true` | **126 test** — 79 birim + 47 IT, BUILD SUCCESS |
| 10 | `admin-api> .\mvnw.cmd -B verify` | **16 test** — 11 birim + 5 IT, BUILD SUCCESS |
| 11 | Lighthouse 10.4.0 mobil, devtools throttling, 1 bağımsız koşu | **LCP 2299,27 ms**, **CLS 0,0426**, performans **0,96** |
| 12 | `git diff --exit-code -- specs/done/evidence/0002` | **0** — 12 dosya bayt düzeyinde değişmedi |

### Test toplamı

- **Backend:** 142 (hotel-api 126 + admin-api 16)
- **Frontend:** 45 (web 39 + admin-web 6)
- **E2E:** 44 (4 smoke + 40 design)
- **Toplam:** **231 test** + Lighthouse/axe/bütçe kontrolleri

### Bütçe ve erişilebilirlik

- JS gzip-9: **206.826 B / 256.000 B**
- Font toplamı: **126.408 B / 204.800 B**
- Hero WebM: **3.196.693 B**; MP4: **3.606.393 B** (limit 8.388.608 B)
- Poster: **111.972 B**; OG: **145.592 B** (limit 307.200 B)
- Scroll CLS: **0,0000**; Lighthouse CLS: **0,0426**
- Axe: altı rotada kritik/ciddi ihlal **0**
- Video kanıtı: **3,92 sn / 462.782 B**, VP9 1280×800 (limit 15 sn / 5.242.880 B)

### Görsel inceleme

Ana sayfa TR/EN desktop, mobil, story/experience scroll adımları, credits, rooms, no-JS, video fallback ve mobil menü ekran görüntüleri incelendi. **Görsel veya işlevsel bir kusur gözlenmedi.** 0002 smoke'un gerçek seed verisiyle ürettiği rooms ekranında üç olanak etiketi de doğru görünüyor.

## 2. Bulgular

### QA-0003-1 — [Düşük] 0003 kanıt setinin bir kısmı final commit ile birebir yeniden üretilemiyor

- **Kanıt:**
  - `AC-6-e2e-ci-summary.md` CI run **#39** (`ae0f92d`, 40227343692) olarak kayıtlı; spec DoD ise final run **#40** (`ad4603f`, 40227913825) diyor. Her iki run da GitHub API'de **success**.
  - `AC-1-asset-and-font-budget.log` ve `AC-5-build-guard.log`, `OFL-SourceSans3.txt` boyutunu **4.579 B** gösteriyor. Mevcut checkout'ta dosya **4.486 B**; `npm run guard:assets -- --self-test` bugün **4.486 B** yazıyor. Fark, CRLF/LF satır sonu farkından (93 satır) kaynaklanıyor.
  - `AC-1-scope.txt` içindeki bazı satır sayıları/eski görsel boyutları final koda göre eski: ör. `hero.spec.ts` 136 satır (güncel 311), `rooms-design.spec.ts` 169 satır (güncel 243), credits PNG boyutları eski.
- **Etki:** Çalışma zamanı veya ürün davranışı etkilenmiyor; tüm testler yeşil. Ancak bir denetçi kanıtı aynı commit'te yeniden ürettiğinde sayılar/faydalar birebir eşleşmiyor; izlenebilirlik zayıflıyor.
- **Öneri:** 0003 kanıt setini final commit'te yeniden üretip `AC-6-e2e-ci-summary.md`'yi final run #40'a güncelleyin. Alternatif olarak bir kısa "kanıt revizyonu" eki ekleyip hangi dosyanın hangi head'de üretildiğini belirtin.
- **Gerekçe:** Kanıtın değeri, aynı komutla aynı sonucu vermesidir. Bu düzeltme kod değişikliği gerektirmez; yalnızca evidence hijyenidir.

### QA-0003-2 — [Düşük / ortam] `hotel-api` `mvn verify`, çalışan jar nedeniyle repackage aşamasında kilitlenebiliyor

- **Kanıt:** İlk `hotel-api` `mvn verify` koşusu 79 birim testini geçti ancak `spring-boot:repackage` aşamasında `hotel-api-0.0.1-SNAPSHOT.jar` yeniden adlandırılamadığı için **BUILD FAILURE** verdi (Windows dosya kilidi; uygulama `java -jar` ile açıktı). Aynı koşu `-Dspring-boot.repackage.skip=true` ile **126 test** ve BUILD SUCCESS tamamlandı.
- **Etki:** Ürün/CI etkisi yok; CI'da uygulama çalışmıyorken bu durum oluşmaz. Yalnızca yerel QA/doğrulama akışını etkiler.
- **Öneri:** Yerel doğrulamada `mvn verify` öncesi çalışan backend'i durdurun veya paket kilidi gerekmeyen doğrulamada `-Dspring-boot.repackage.skip=true` kullanın; `e2e/README.md`/QA runbook'una tek satır not ekleyin.
- **Gerekçe:** Aynı komutun temiz CI ile yerelde farklı sonuç vermesi, QA'da yanlış kırmızı algısı yaratabilir.

### QA-0003-3 — [Bilgi] Rooms tasarım kanıtı mock payload ile üretiliyor; gerçek seed'de 3 olanak var

- **Kanıt:** `AC-3-rooms-tr-desktop.png`, `rooms-design.spec.ts` içindeki `mockRooms` ile üretiliyor; mock veride kart başına 1–2 olanak var. Gerçek seed'de her odada 3 olanak bulunuyor; `AC-6-web-rooms-tr.png` bunu doğruluyor ve görsel olarak sorun görünmüyor.
- **Etki:** Ürün hatası değil; tasarım kanıtı en uzun olanak listesini göstermiyor. Gerçek veriyle 0002 smoke kapsamı mevcut.
- **Öneri:** İsteğe bağlı olarak `mockRooms` verisine 3. olanağı ekleyin veya en az bir design ekran görüntüsünü gerçek seed ile alın. Zorunlu değil; mevcut görsel ve 0002 smoke kapsamı yeterli.
- **Gerekçe:** Üretim yüküne en yakın yerleşim, üç etiketli karttır; kanıtın bunu göstermesi tasarım regresyonunu daha erken yakalar.

## 3. Sonuç

- **Bloklayıcı hata:** Yok.
- **AC kapsamı:** 0003 AC-1…AC-6 test/kanıtla eşleşiyor; `@empty`/`@seeded` 0002 regresyonu 4/4, design 40/40, web 39/39.
- **0002 izolasyonu:** 12 dosya SHA-256/`git diff` ile değişmedi.
- **Kapanış:** Spec'in "Tamamlandı" durumu ve DoD işaretleri bu test sonuçlarıyla uyumlu. QA tarafından spec, uygulama kodu veya test kodu değiştirilmedi; yalnızca bu rapor eklendi.

> **Not:** Bu rapor `specs/done/evidence/0003/qa-verification-2026-10-25.md` altına yeni kanıt olarak eklenmiştir. Mevcut 0003 kanıtları ve 0002 arşivi değiştirilmemiştir.
