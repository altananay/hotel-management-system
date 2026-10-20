# Kod İncelemesi Doğrulama — Spec 0002

- **İnceleyen:** Code Reviewer
- **Tarih:** 2026-10-20
- **PR:** [#8](https://github.com/altananay/hotel-management-system/pull/8) — `feature/0002-room-catalog` → `releasable`
- **İncelenen head:** `12d3055`
- **Önceki inceleme:** `79cd4cc` / [`code-review-2026-10-20.md`](code-review-2026-10-20.md)
- **Yöntem:** Statik diff ve commit incelemesi + GitHub PR/check-run API doğrulaması. Rol gereği test veya derleme çalıştırılmadı.

---

## 1. CI ve PR durumu

- PR head: `12d30552bc5c2fea49e3be9d3067c15fcb95fc26`
- `mergeable`: `true`; `mergeable_state`: `clean`
- Check-run sonuçları (5/5): **Backend**, **Frontend**, **E2E Smoke**, **CI Quality Gate**, **GitGuardian** — hepsi `success`.
- PR açıklamasındaki kontrol karakterleri temizlenmiş; `Spec 002` yazım hatası kalmamış.
- Yalnız GitHub Actions runner uyarıları var (Node.js 20 ve `setup-java@v4`); bu PR'ın işlevsel koduyla ilgili değil.

---

## 2. Bulgu durumu

| ID | Önceki bulgu | Durum | Kanıt / not |
|---|---|---|---|
| CR-1 | Seed profili prod'a karşı çalıştırılabiliyor | **Açık (kısmi fix)** | Guard eklendi, ancak etkin Flyway bağlantısını doğrulamıyor; spec'te guardsız bir seed komutu da duruyor. Ayrıntı: §3.1. |
| CR-2 | Kullanılmayan amenity write path'i ve ölü bağımlılıklar | **Kapalı** | `createAmenity`, `AmenityJpaRepository`, `AmenityEntity.fromDomain`, `Clock` ve ölü test kaldırıldı; `git grep` üretim kodu referansı bulmadı. `RoomCatalogQueryService` yalnız okuma yapıyor. |
| CR-3 | Pagination hatası `IllegalArgumentException` | **Kapalı** | `DomainError(validation.invalid_request, VALIDATION)` kullanılıyor; `@CsvSource` testi sınırları doğruluyor; controller global handler ile 400 üretiyor. |
| CR-4 | Tekrarlanan SVG `id="sunGlow"` | **Kapalı** | `useId()` tabanlı, sanitize edilmiş `gradientId` kullanılıyor; `id` ve `fill` aynı değişkenden geliyor; statik aramada eski sabit `sunGlow` kalmadı. |
| CR-5 | Seed profili dokümanı ile uygulama uyuşmazlığı | **Kısmi** | `docs/conventions.md:67`, `infra/README.md:80-87`, `e2e/README.md`, `docs/architecture.md:222` ve `.github/workflows/ci.yml:146` yeni profile hizalandı. Ancak `specs/0002-room-catalog.md:33,56,102` hâlâ `-Dflyway.locations=...` alternatifini gösteriyor. |
| CR-6 | SCORECARD trailing whitespace ve PR kontrol karakterleri | **Kapalı** | `specs/0002-room-catalog.md` SCORECARD satırları temiz; PR body'de kontrol karakteri yok; hata ipucu yalnız anlamlı ve farklı kodlarda gösteriliyor. Ancak yeni arşiv dokümanında trailing whitespace var: §3.3. |

---

## 3. Kalan bulgular

### 3.1. [CR-1 residual] Seed guard etkin Flyway bağlantısını doğrulamıyor — Orta

**Dosya:** `backend/hotel-api/pom.xml:163-175`

**Dosya:** `specs/0002-room-catalog.md:33,56,102`

**Dosya:** `docs/conventions.md:67`, `infra/README.md:80-87` (guard'ın kapsamını fazla geniş anlatıyor)

Guard, `initSql` içinde yalnız `${seed.env}` ve `${HOTEL_CORE_DB_URL}` değerlerini kontrol ediyor:

```sql
SELECT CASE
    WHEN '${seed.env}' IN ('local', 'ci')
     AND ('${HOTEL_CORE_DB_URL}' LIKE 'jdbc:postgresql://localhost:%/hotel_core%'
          OR '${HOTEL_CORE_DB_URL}' LIKE 'jdbc:postgresql://127.0.0.1:%/hotel_core%')
    THEN 1
    ELSE 1 / 0
END;
```

Flyway Maven plugin'inin [dokümante edilmiş öncelik sırası](https://documentation.red-gate.com/fd/maven-goal-277579365.html) system properties > environment variables > custom config files > Maven properties > plugin configuration şeklindedir. Bu nedenle etkin bağlantı URL'si şu yollarla değiştirilebilir:

- `-Dflyway.url=...`
- `FLYWAY_URL=...`
- `-Dflyway.configFiles=...` / `FLYWAY_CONFIG_FILES=...`
- `-Dflyway.initSql=...` (guard'ın kendisi override edilebilir)

Bu durumda guard `${HOTEL_CORE_DB_URL}` yerel göründüğü için geçer, fakat Flyway fiilen prod'a bağlanır ve seed yazar. Guard, Maven property'sini kontrol ettiği için **etkin bağlantıyı garanti etmez**.

Ayrıca spec hâlâ aşağıdaki guardsız alternatifi belgeliyor:

```text
./mvnw flyway:migrate -Dflyway.locations=filesystem:src/main/resources/db/migration/<modüller>,filesystem:src/main/resources/db/seed/catalog
```

Bu komut `-Pseed` içermediğinden `initSql` guard'ı hiç çalışmaz; `HOTEL_CORE_DB_URL` prod'a ayarlanmışsa seed doğrudan prod'a yazılır. Bu, CR-1'in orijinal riskini açık bırakır.

**Etki:** Seed hâlâ yanlışlıkla veya bilinçli bir override ile prod veritabanına yazabilir. `ON CONFLICT (id) DO UPDATE` nedeniyle sabit UUID'li kayıtlar güncellenir; geri alması zordur.

**Fix önerisi:**

1. Seed'i tek bir wrapper script üzerinden çalıştır (örn. `scripts/seed-local.sh` / `.ps1`); script etkin URL'yi doğrulasın, `FLYWAY_URL`, `flyway.url`, `flyway.configFiles` ve `flyway.initSql` override'larını temizlesin veya reddetsin, sonra Flyway'i açık URL ile çağırsın.
2. Alternatif olarak guard'ı POM `initSql` yerine, override edilemeyen bir seed runner'da veya veritabanı tarafında etkin bağlantıyı kontrol edecek şekilde uygula.
3. `specs/0002-room-catalog.md:33,56,102` içindeki `-Dflyway.locations=...` alternatifini kaldır; seed için tek yol `-Pseed -Dseed.env=local|ci` olsun.
4. CI'ya negatif test ekle: `-Dseed.env` yok veya `prod` iken komutun başarısız olduğunu doğrula.

### 3.2. [CR-8] Guard için otomatik negatif test yok — Düşük

**Dosya:** `.github/workflows/ci.yml:144-146`

CI yalnız başarılı yolu (`-Dseed.env=ci`) çalıştırıyor. PR açıklamasında `-Pseed` (env yok) ve `-Dseed.env=prod` için manuel fail-fast kanıtı var; ancak bunu koruyan otomatik bir test yok. Guard ileride kaldırılır veya override'larla bozulursa CI bunu yakalamaz.

**Fix önerisi:** Yerel/CI'da geçici bir hedefe karşı `-Pseed` ve geçersiz/eksik `seed.env` ile komutun başarısız olduğunu doğrulayan bir adım ekle. Wrapper seçilirse test wrapper'ı hedeflesin.

### 3.3. [CR-7] Arşiv inceleme dokümanında trailing whitespace — Düşük

**Dosya:** `specs/done/evidence/0002/code-review-2026-10-20.md:31,32,60,61,62,118,119`

`git diff --check 79cd4cc..HEAD` şu satırlarda trailing whitespace bildiriyor:

```text
specs/done/evidence/0002/code-review-2026-10-20.md:31: trailing whitespace.
specs/done/evidence/0002/code-review-2026-10-20.md:32: trailing whitespace.
specs/done/evidence/0002/code-review-2026-10-20.md:60: trailing whitespace.
specs/done/evidence/0002/code-review-2026-10-20.md:61: trailing whitespace.
specs/done/evidence/0002/code-review-2026-10-20.md:62: trailing whitespace.
specs/done/evidence/0002/code-review-2026-10-20.md:118: trailing whitespace.
specs/done/evidence/0002/code-review-2026-10-20.md:119: trailing whitespace.
```

Bu satırlar Markdown hard-break için iki boşlukla bitiyor; `git diff --check` bunu hata olarak görüyor. CR-6'daki SCORECARD temizliği tamamlanmış olsa da arşiv dokümanı aynı kontrolü geçmiyor.

**Fix önerisi:** İki boşluklu satır sonlarını kaldır ve paragrafları boş satırla ayır; gerekiyorsa `<br>` kullan. Ardından `git diff --check 79cd4cc..HEAD` temiz olmalı.

---

## 4. Sonuç

- **CR-2, CR-3, CR-4 ve CR-6 kapandı.**
- **CR-1 tam kapanmadı:** guard normal `-Pseed` akışında çalışıyor, ancak etkin Flyway URL'si override edilebiliyor ve spec'te guardsız `-Dflyway.locations` alternatifi duruyor.
- **CR-5 kısmi:** doküman/README/CI hizalandı; spec'teki alternatif komut güncellenmedi.
- Yeni işlevsel regresyon veya kritik kod hatası görülmedi; CI 5/5 yeşil.
- CR-1 prod verisi güvenliğiyle ilgili olduğundan, guard kapatılmadan **merge onayı önerilmez**. Nihai karar Takım Yöneticisi'ne aittir.
