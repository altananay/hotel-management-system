# Code Review — Spec 0002 (Room Catalog)

- **İnceleyen:** Code Reviewer
- **Tarih:** 2026-10-20
- **PR:** [#8](https://github.com/altananay/hotel-management-system/pull/8) — `feature/0002-room-catalog` → `releasable`
- **İncelenen head:** `79cd4cc`
- **Yöntem:** Statik diff incelemesi + GitHub check-run doğrulaması. Rol gereği test/derleme çalıştırılmadı.
- **Not:** Integration conflict nedeniyle PR `releasable` hedefli; Takım Yöneticisi integration kuralını geçici olarak devre dışı bıraktı.

---

## 1. Sonuç özeti

AC-1 … AC-6 işlevsel olarak karşılanmış görünüyor. PR #8 head `79cd4cc` için CI check-run’ları **5/5 success** (Backend, Frontend, E2E, CI Quality Gate, GitGuardian). Yeni **kritik işlevsel hata** bulunmadı; aşağıdaki bulgular çoğunlukla üretim güvenliği, SRP/YAGNI ve doküman tutarlılığı ile ilgili.

| ID | Önem | Konu |
|---|---|---|
| CR-1 | Orta | `-Pseed` profili prod’a karşı çalıştırılabiliyor; ortam guard’ı yok |
| CR-2 | Orta | `RoomCatalogQueryService` içinde kullanılmayan write path ve ölü bağımlılıklar |
| CR-3 | Düşük | Query service `IllegalArgumentException` fırlatıyor; HTTP dışı çağrıda 500 riski |
| CR-4 | Düşük | `RoomShowcase` içinde her kartta tekrar eden SVG `id="sunGlow"` |
| CR-5 | Düşük | Seed profili dokümanı ile uygulama arasında adlandırma uyuşmazlığı |
| CR-6 | Düşük | Spec SCORECARD trailing whitespace + PR açıklamasında kontrol karakterleri |

---

## 2. Bulgular

### [CR-1] Seed profili prod’a karşı çalıştırılabilir — Orta

**Dosya:** `backend/hotel-api/pom.xml:153-178`<br>
**Dosya:** `infra/README.md:74-92`<br>
**Dosya:** `backend/hotel-api/src/main/resources/db/seed/catalog/R__seed_catalog_dev.sql`

```xml
<profile>
    <id>seed</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.flywaydb</groupId>
                <artifactId>flyway-maven-plugin</artifactId>
                <configuration>
                    <url>${HOTEL_CORE_DB_URL}</url>
                    ...
                    <location>filesystem:src/main/resources/db/seed/catalog</location>
```

**Etki:** Varsayılan `flyway:migrate` seed içermiyor (AC-5’in temel izolasyonu doğru). Ancak `-Pseed` profili `HOTEL_CORE_DB_URL` ile herhangi bir veritabanına yönlendirilebilir; prod URL’si verilirse örnek katalog verisi prod’a yazılır ve `ON CONFLICT (id) DO UPDATE` ile sabit UUID’li kayıtlar güncellenir. AC-5’teki “canlı ortama örnek veri sızdırmamalıdır” hedefi için ortam guard’ı yok.

**Fix önerisi:**
- `seed` profilini yalnız `SEED_ENV=local|ci` (veya `-Dseed.env=local|ci`) ile çalışacak şekilde koru; prod URL’sinde fail-fast et.
- Alternatif: `-Pseed` yerine yalnız local/CI’da çalışan ayrı bir seed script’i kullan ve script’te host doğrulaması yap.
- En azından README’de “prod URL ile `-Pseed` kesinlikle çalıştırılmaz” uyarısını ve CI dışı kullanım kuralını netleştir.

---

### [CR-2] Query service içinde kullanılmayan write path ve ölü bağımlılıklar — Orta

**Dosya:** `backend/hotel-api/src/main/java/com/hotel/catalog/internal/application/RoomCatalogQueryService.java:25-27,31-37,73-84`<br>
**Dosya:** `backend/hotel-api/src/main/java/com/hotel/catalog/internal/persistence/AmenityJpaRepository.java:9-11`<br>
**Dosya:** `backend/hotel-api/src/main/java/com/hotel/catalog/internal/persistence/AmenityEntity.java:53-61`<br>
**Dosya:** `backend/hotel-api/src/test/java/com/hotel/catalog/internal/application/RoomCatalogQueryServiceTest.java:144-159`

```java
private final AmenityJpaRepository amenityRepository;
private final Clock clock;

@Transactional
public Amenity createAmenity(String code, String nameTr, String nameEn) {
    Amenity amenity = Amenity.create(code, nameTr, nameEn);
    if (amenityRepository.existsByCode(amenity.code())) { ... }
    ...
}
```

**Etki:** `createAmenity` hiçbir controller veya başka üretim kodu tarafından çağrılmıyor; yalnız testte kullanılıyor. Bu, spec’te kapsam dışı olan admin katalog CRUD’una ait bir write path’i `QueryService` içine taşıyor (SRP ihlali) ve `AmenityJpaRepository`, `Clock`, `AmenityEntity.fromDomain`, `findByCode` gibi kullanılmayan bağımlılıkları getiriyor (YAGNI). Gelecekte admin spec’i geldiğinde yanlış servis üzerinden yazma riski doğar.

**Fix önerisi:** `createAmenity` ve ilgili write bağımlılıklarını kaldır; `RoomCatalogQueryService` yalnız okuma yapsın. Admin CRUD ihtiyacı “Admin Kimlik ve Oturum”/admin katalog spec’inde ayrı bir `CatalogCommandService` olarak ele alınsın. `AmenityJpaRepository.findByCode`, `AmenityEntity.fromDomain` ve ölü test kaldırılsın.

---

### [CR-3] Query service `IllegalArgumentException` fırlatıyor — Düşük

**Dosya:** `backend/hotel-api/src/main/java/com/hotel/catalog/internal/application/RoomCatalogQueryService.java:41-45`

```java
if (page < 0 || size < 1 || size > 100) {
    throw new IllegalArgumentException(
            "Invalid pagination parameters: page must be >= 0 and size between 1 and 100.");
}
```

**Etki:** HTTP yolunda controller `@Min/@Max` doğrulaması 400 `validation.invalid_request` üretiyor; ancak servis başka bir adapter/internal çağrıdan geçersiz parametreyle çağrılırsa `GlobalExceptionHandler` catch-all’ına düşüp 500 döner. Aynı doğrulama iki farklı hata tipiyle temsil edilmiş olur.

**Fix önerisi:** Servis doğrulamasını `DomainError` (`DomainErrorCategory.VALIDATION`, `validation.invalid_request`) veya özel bir `Pagination` value object’i ile yap; controller doğrulamasıyla tek sözleşmeye indir. Alternatif olarak servis kontrolünü kaldırıp doğrulamayı yalnız controller’a bırakma kararını açıkça dokümante et.

---

### [CR-4] Her kartta tekrar eden SVG `id="sunGlow"` — Düşük

**Dosya:** `web/src/features/room-catalog/room-showcase.tsx:38-44,90-93`

```tsx
<radialGradient id="sunGlow" ...>
...
<RoomVisualPlaceholder ... />
```

**Etki:** Her `room-card` aynı `id="sunGlow"` tanımını DOM’a basıyor. Birden çok kartta duplicate ID oluşur; `fill="url(#sunGlow)"` tarayıcıda ilk tanıma çözümlenir, HTML validity ve erişilebilirlik/otomasyon açısından kırılganlık yaratır. Görsel çıktı bugün aynı görünse de kart sayısı arttıkça risk büyür.

**Fix önerisi:** `useId()` ile kart başına benzersiz gradient ID’si üret veya gradient tanımını tek bir üst seviye SVG’ye taşı. `RoomVisualPlaceholder` içinde `const gradientId = useId();` ve `id={gradientId}` / `fill={`url(#${gradientId})`}` kullan.

---

### [CR-5] Seed profili dokümanı ile uygulama arasında ad uyuşmazlığı — Düşük

**Dosya:** `docs/conventions.md:67`<br>
**Dosya:** `backend/hotel-api/pom.xml:153-178`<br>
**Dosya:** `infra/README.md:74-92`

```text
... yalnız `local`/`ci` profilinde `flyway.locations` ile devreye alınır ...
```

**Etki:** Doküman “local/ci profili” diyor; uygulamada ise Maven `seed` profili (`-Pseed`) kullanılıyor. Yeni geliştirici `-Plocal`/`-Pci` deneyip seed’in çalışmamasıyla karşılaşabilir; tek kaynak ilkesi zedelenir.

**Fix önerisi:** Dokümanı `-Pseed` (veya `-Dflyway.locations`) ile hizala; ya da profili `local`/`ci` olarak yeniden adlandırıp README/CI’ı güncelle. Hangi isim seçilirse `docs/conventions.md`, `infra/README.md`, `e2e/README.md` ve CI aynı terimi kullanmalı.

---

### [CR-6] Küçük hijyen bulguları — Düşük

- **Spec:** `specs/0002-room-catalog.md:128-131` SCORECARD satırlarında trailing whitespace var (`git diff --check`).
- **PR #8 açıklaması:** API’den okunan gövdede `\u0007` gibi C0 kontrol karakterleri ve “Spec 002” yazımı görünüyor; PR metni temizlenmeli.
- **`RoomShowcase` hata durumu:** `rooms-error-message` sabit kullanıcı mesajının altında `detailedHint` ile aynı/benzer genel mesaj tekrar basılıyor; tek mesaj yeterli olabilir (opsiyonel UX iyileştirmesi).

Bunlar bloklayıcı değil; PR/spec hijyeni için düzeltilmeli.

---

## 3. Doğrulananlar

- **AC-1:** `V7` yalnız `catalog` şemasında; `@Table(schema="catalog")`; `RoomType`/`Amenity` doğrulamaları, DB check/unique/FK ve hata kodları mevcut.
- **AC-2:** `GET /api/room-types` public; `{items,page,size,total}`; aktif filtre; DB `ORDER BY display_order, id`; amenities `code` ASC; sayfalama sınırları; boş/overflow davranışı testli.
- **AC-3:** `/[locale]/rooms`, `home-rooms-link`, TR/EN locale switch, testid seti, loading/empty/error/full, fiyat yok; Vitest + MSW testleri mevcut.
- **AC-4:** `SampleItemsController` ve `web/src/features/system-status` kaldırılmış; `GET /api/system/sample-items` 404 problem+json testli; `admin` scaffold’u korunmuş.
- **AC-5:** Standart migrate seed’siz; `R__seed_catalog_dev.sql` ayrı konumda; 3 aktif + 1 pasif, 5 amenity, tie-break; `CatalogSeedIT` doğrudan iki kez çalıştırma kanıtı.
- **AC-6:** `@empty` → seed → `@seeded` CI akışı; 8 görsel kanıt `specs/done/evidence/0002/`; `0001` arşivi değişmemiş; admin-web smoke korunmuş.

**CI (PR #8, head `79cd4cc`):** Backend, Frontend, E2E, CI Quality Gate ve GitGuardian — 5/5 success; PR `mergeable_state=clean`.

---

## 4. Karar

- **Kritik işlevsel hata yok;** AC-1 … AC-6 kapsamı büyük ölçüde karşılanmış.
- **CR-1** (prod seed riski) ve **CR-2** (kullanılmayan write path/SRP) merge öncesi düzeltilmeli.
- **CR-3, CR-4, CR-5, CR-6** düşük öncelikli; aynı fix turunda veya hemen sonrasında kapatılabilir.
- Integration kuralı Takım Yöneticisi tarafından geçici olarak devre dışı bırakıldığı için PR’ın `releasable` hedefi bu incelemede ayrıca bulgu sayılmadı.

**İmza ilkesi:** “Bulur, gösterir, önerir; kodu eline almaz.”
