# 0005 — Medya ve Fotoğraf Yönetimi — QA Denetim Bulguları

> **Denetim hedefi:** `integration` @ `179d3d0` — `fix(media): address independent code review findings [plan 0005]`
> **Yeniden doğrulama:** `integration` @ `7f51fe4` — QA fix merge `06d8182` + evidence `7f51fe4` (§8)
> **Rezidüel doğrulama:** `integration` @ `9397915` — residual fix merge `9397915` (`fix/0005-residual-findings`) (§9)
> **Tarih:** 2026-11-07
> **Kapsam:** `hotel-api` media modülü, admin-api gateway, admin-web `/media`, web RoomCard, migration'lar, E2E ve `specs/done/evidence/0005/` kanıtları.
> **Yöntem:** Statik kod incelemesi + birim/IT/lint/build + yerel E2E + GitHub Actions kontrolü + SHA-256 arşiv doğrulaması.
> **Not:** Denetim sırasında üretim kodu veya test değiştirilmedi. Yalnızca bu bulgu belgesi eklendi. Lokal doğrulama için açılan geçici medya kayıtları temizlendi, denetim için başlatılan uygulama servisleri durduruldu.

---

## 1. Yönetici Özeti

Kod tabanı genel olarak sağlam: migration'lar, presigned yükleme, asenkron işleme, public/private depolama ayrımı, admin akışı ve public vitrin çalışıyor. Tüm otomatik testler ve `integration` CI hattı yeşil.

Doğrulanan güçlü yönler:
- `media → catalog` yalnız `RoomTypeLookup` public portu üzerinden; catalog internal/tablosuna erişim ve cross-schema FK yok; `HotelApiModularityTest` yeşil.
- Presigned PUT doğrudan MinIO'ya gidiyor; orijinal private, yalnız yayınlanmış varyantlar public; anonim PUT/listeleme 403; gerçek MinIO IT'si bunu doğruluyor.
- EXIF/GPS temizliği, orientation, altı WebP/JPEG varyantı ve no-upscale kuralı test edilmiş.
- `SUPER_ADMIN`/`MANAGER` rol kapısı ve `FRONT_DESK` 403 davranışı; CSRF retry; public API `no-store`, public objelerde immutable cache doğrulanmış.

Buna karşın üç işlevsel hata, üç test/kanıt/spec boşluğu, bir düşük sözleşme sapması ve iki açık risk tespit edildi. Bunların hiçbiri mevcut E2E akışını tamamen durdurmuyor; ancak özellikle `QA-01`, `QA-02` ve `QA-03` düzeltilmeden veya açıkça kabul edilmeden spec'in "tam APPROVE" ve "Done" durumuna geçirilmesini önermiyorum.

| ID | Önem | Bulgu | Durum |
|---|---|---|---|
| **QA-01** | Orta | Atanmamış/yayınlanmamış mevcut medya için `PUT .../cover` yanlışlıkla `404 media.not_found` dönüyor | Doğrulandı |
| **QA-02** | Orta | Oda tipi pasifleşmiş medyada admin-web'den alt metin kaydı `404 media.room_type_not_found` alıyor | Doğrulandı |
| **QA-03** | Orta | Yükleme hata yollarında liste yenilenmiyor; `FAILED`/`UPLOADED` kayıt admin ekranında görünmüyor | Kod yolu doğrulandı |
| **QA-04** | Düşük | `@media` E2E alt metni doğrulamıyor; kanıt "PNG" diyor ama fixture WebP | Doğrulandı |
| **QA-05** | Orta (süreç) | 0005 kanıt seti `integration`/`179d3d0` ile uyumsuz; eski commit ve eski test sayıları yazıyor | Doğrulandı |
| **QA-06** | Düşük | Spec yalnız V8 migration'ı anlatıyor; kodda V9–V11 ve ek hata kodları var | Doğrulandı |
| **QA-07** | Düşük | Public `/api/media` için spec'teki `503` sözleşmesi DB/servis hatalarında uygulanmıyor | Statik doğrulandı |
| **R-01** | Orta (risk) | admin-api 3 sn gateway timeout'u, senkron 6 varyant kopyalama/silme ve 15 MB complete okumasıyla çakışabilir | Doğrulanmadı / ölçüm gerekli |
| **R-02** | Düşük (risk) | Dead-letter sonrası public nesne manuel temizliğe kadar erişilebilir kalabilir | Kabul edilmiş risk / operasyonel |

**Önerilen sonuç:** **Koşullu geçer (conditional pass)**. Blocker yok; ancak QA-01/02/03 için düzeltme veya Takım Yöneticisi'nin açık kabulü, QA-04/05/06 için kanıt/spec güncellemesi, R-01 için ölçüm ve R-02 için operasyonel kapatma gerekir.

---

## 2. Doğrulama Kanıtları

### 2.1 Backend

| Uygulama | Komut | Sonuç |
|---|---|---|
| `backend/hotel-api` | `.\mvnw.cmd -B verify` | **179 birim testi**, **57 IT**; `MediaIT` **10/10**; Spotless temiz |
| `backend/admin-api` | `.\mvnw.cmd -B verify` | **101 birim testi**, **5 IT**; Spotless temiz |

`hotel-api` IT dağılımı: `CatalogSeedIT` 2/2, `RoomCatalogIT` 40/40, `DatabaseSchemaAndRoleSeparationIT` 5/5, `MediaIT` 10/10.

### 2.2 Frontend

| Uygulama | Komut | Sonuç |
|---|---|---|
| `web` | `npm test; npm run lint; npm run build` | **43/43** test, lint temiz, build başarılı |
| `admin-web` | `npm test; npm run lint; npm run build` | **35/35** test, lint temiz, build başarılı |

### 2.3 E2E

| Test | Sonuç |
|---|---|
| `tests/media --grep @media` | **1/1** |
| `tests/admin --grep @admin` | **3/3** |
| `tests/smoke.spec.ts --grep @seeded` | **2/2** |
| `tests/design --grep @seeded` | **40/40** |

`0002 @empty` bu denetimde yerelde yeniden koşulmadı; yerel veritabanı seed'liydi. Bununla birlikte GitHub Actions `integration` CI hattı (`179d3d0`) tam sırayı (`@empty → seed → @seeded → design → @admin → @media`) çalıştırıp yeşil geçti.

### 2.4 CI

- **GitHub Actions run:** `42326617436`
- **Head:** `179d3d04150f32d209c8a6f0214bace4e9e542c4`
- **Sonuç:** `success`
- **Job'lar:** `Backend (hotel-api & admin-api)`, `Frontend (web & admin-web)`, `E2E Smoke (Playwright Chromium)`, `CI Quality Gate` — hepsi başarılı.
- URL: https://github.com/altananay/hotel-management-system/actions/runs/42326617436

### 2.5 Arşiv bütünlüğü

`AC-6-archive-sha256.txt` içindeki 0001–0004 dosyaları elle doğrulandı:

- **129/129** dosya hash'i eşleşti.
- **0 eksik**, **0 değişmiş** dosya.
- Ancak manifestin başlığındaki `node e2e/scripts/sha256-manifest.mjs verify <dir> <manifest>` komutu bu metin manifestini okuyamaz; script JSON manifest bekler. Bu, QA-05 altında ayrıca not edilmiştir.

### 2.6 Denetim sınırları

- Yerel `hotel_core` veritabanı seed'li olduğu için `0002 @empty` yerelde tekrar koşulmadı; CI bu adımı kapsıyor.
- Public API `503` ve gateway timeout riskleri dinamik olarak üretilmedi; kod/config üzerinden değerlendirildi.
- Dead-letter senaryosu S3 kalıcı hata enjeksiyonu ile ayrıca zorlanmadı; kod ve runbook üzerinden değerlendirildi.
- Yerel E2E ve API doğrulamaları için açılan geçici medya kayıtları silindi; repository çalışma ağacı temiz bırakıldı.

---

## 3. Bulgular

### QA-01 — Mevcut atanmamış medya için cover ucu `404 media.not_found` dönüyor

**Önem:** Orta
**Durum:** Doğrulandı (lokal API)
**İlgili dosyalar:**
- `backend/hotel-api/src/main/java/com/hotel/media/internal/application/MediaAdminService.java:176-183`
- `backend/hotel-api/src/main/java/com/hotel/media/internal/persistence/MediaAssetJpaRepository.java:36-37`
- `backend/hotel-api/src/main/java/com/hotel/media/internal/application/MediaAdminService.java:360-366`
- `backend/hotel-api/src/main/java/com/hotel/media/internal/application/MediaAdminService.java:377-382`

**Kanıt:**
`READY`, `room_type_id = NULL`, yayınlanmamış mevcut bir medya kaydı için:

```http
PUT /admin-api/media/{id}/cover
```

yanıtı:

```json
{
  "status": 404,
  "code": "media.not_found",
  "detail": "Media asset was not found."
}
```

Beklenen, kaydın var olduğunu ve yalnızca yayın/atama koşulunu sağlamadığını belirten **409** (`media.not_published` veya `media.not_ready`) olmalıdır. Kayıt DB'de mevcuttu; yani 404 “medya yok” anlamına gelmiyor.

**Kök neden:**
`cover()` önce oda tipini çözmek için:

```java
UUID roomTypeId = assetRepository.findRoomTypeIdById(id).orElseThrow(this::notFound);
```

kullanıyor. Repository sorgusu:

```java
@Query("SELECT a.roomTypeId FROM MediaAssetEntity a WHERE a.id = :id")
Optional<UUID> findRoomTypeIdById(@Param("id") UUID id);
```

şeklindedir. `room_type_id` `NULL` olduğunda JPQL tek satırda `NULL` döndürür; Spring Data bunu `Optional.empty()` olarak sarar. Böylece **“kayıt yok”** ile **“kayıt var ama oda tipi yok”** ayırt edilemez ve `notFound()` erken çalışır. `requirePublished()` hiç çalışmadığı için doğru 409 üretilmez.

**Gerekçe:**
Bu bir API sözleşmesi hatasıdır. İstemci 404 aldığında kaydın silinmiş olduğunu düşünür; oysa kayıt vardır ve yalnızca yayınlanmamıştır. Gelecekte cover aksiyonunu UI'da genişletmek veya doğrudan API kullanan bir istemci eklemek bu hatayı kullanıcıya taşır. Mevcut `MediaAdminServiceTest` (`:269-295`) yalnızca atanmış medya senaryolarını kapsıyor; `roomTypeId = NULL` durumu test edilmiyor.

**Öneri:**
1. `findRoomTypeIdById` yerine satırın varlığını `NULL` değerden ayıran bir sorgu kullanın:
   - `List<UUID>` döndüren bir projeksiyon ve `isEmpty()` kontrolü; veya
   - önce `existsById(id)` kontrolü, sonra oda tipi sorgusu; veya
   - kilit sırasını bozmayacak şekilde `SELECT a FROM MediaAssetEntity a WHERE a.id = :id` ile `roomTypeId` alıp, `null` ise oda kilidi almadan `findLockedById` ile devam etme.
2. `requirePublished()`/`requireNotArchived()` sırasının doğru 409'u üretmesini sağlayın.
3. Birim testi ekleyin: mevcut, `roomTypeId = NULL`, yayınlanmamış medya → `media.not_published` (409). Ek olarak gerçek API/IT testi: var olmayan ID → 404, atanmış ama yayınlanmamış → 409, yayınlanmış → 200.

**Kabul testi:**
- Var olan atanmamış medya + `PUT .../cover` → **409** `media.not_published` (veya `media.not_ready`).
- Var olmayan ID → **404** `media.not_found`.
- Yayınlanmış READY medya → **200** ve cover set.

---

### QA-02 — Pasif oda tipine atanmış medyada alt metin kaydı 404 alıyor

**Önem:** Orta
**Durum:** Doğrulandı (lokal API + UI kod yolu)
**İlgili dosyalar:**
- `admin-web/src/features/media/media-manager.tsx:355-366`
- `admin-web/src/features/media/use-media-mutations.ts:20-29`
- `backend/hotel-api/src/main/java/com/hotel/media/internal/web/AdminMediaController.java:107-113`
- `backend/hotel-api/src/main/java/com/hotel/media/internal/application/MediaAdminService.java:122-135`
- `backend/hotel-api/src/main/java/com/hotel/media/internal/application/MediaAdminService.java:368-375`

**Kanıt:**
Lokal test DB'sinde seed'deki pasif oda tipi `0199f2c0-0000-7000-8000-000000000304` için hazırlanan geçici medya kaydıyla:

```http
PATCH /admin-api/media/{id}
{
  "roomTypeId": "0199f2c0-0000-7000-8000-000000000304",
  "altTextTr": "Yeni TR",
  "altTextEn": "New EN"
}
```

yanıtı **404 `media.room_type_not_found`** oldu. Aynı kayıt için yalnızca alt metin alanları gönderildiğinde:

```http
PATCH /admin-api/media/{id}
{
  "altTextTr": "Yeni TR 2",
  "altTextEn": "New EN 2"
}
```

yanıtı **200** oldu ve alt metin güncellendi.

Admin-web'deki “Alt metni kaydet” butonu her zaman mevcut oda tipini gönderiyor:

```ts
mutations.update.mutateAsync({
  id: item.id,
  roomTypeId: item.roomTypeId ?? undefined,
  altTextTr: altTr,
  altTextEn: altEn,
});
```

`useMediaMutations` da bu değeri PATCH gövdesine koyuyor. Admin-web testi bu davranışı açıkça sabitliyor: `media-manager.test.tsx:358-391` PATCH gövdesinde `roomTypeId: ROOM_TYPE_ID` bekliyor. Sunucu, `roomTypeId` non-null olduğu sürece—mevcut değerle aynı olsa bile—`ensureActiveRoomType()` çağırıyor ve pasif oda tipinde 404 üretiyor.

**Ek gözlem:**
Admin ekranındaki oda tipi seçenekleri yalnızca aktif oda tiplerinden geliyor. Pasif oda tipine atanmış bir medyada select'in değeri seçeneklerde bulunmadığı için alan boş görünebiliyor; “Atanmadı” seçeneği de `disabled` olduğundan atamayı kaldırma yolu yok. Bu, aynı hatanın UI tarafındaki görünürlüğünü artırıyor.

**Gerekçe:**
Spec PATCH'in “aktif oda tipi ataması **ve** TR/EN alt metin güncellemesi” yaptığını söylüyor. Yalnızca alt metin değişiyorsa yeni bir atama yapılmıyor; dolayısıyla pasif oda tipi kontrolünün bu isteği engellememesi gerekir. Mevcut davranış, oda tipi sonradan pasifleştirilen medyanın alt metnini düzeltmeyi imkânsız kılıyor; bu özellikle erişilebilirlik (alternatif metin) açısından önemli. Ayrıca hata mesajı “oda tipi aktif değil” diyerek kullanıcıyı yanlış yönlendiriyor.

**Öneri:**
1. **Sunucu:** `ensureActiveRoomType` yalnızca `roomTypeId` mevcut değerden **farklıysa** veya medya henüz atanmamışsa çağrılsın. Aynı oda tipiyle gelen PATCH yalnızca alt metni güncellesin.
2. **UI:** Alt metin kaydında `roomTypeId` gönderilmesin; yalnızca select değiştiğinde gönderilsin. Mevcut pasif atama select'te açıklayıcı, `disabled` bir seçenek olarak gösterilsin.
3. **Spec kararı:** `roomTypeId: null` ile atamayı kaldırma desteklenecek mi? Şu anda `null`, “alan gönderilmedi” anlamına geliyor; UI'daki “Atanmadı” seçeneği de kullanılamıyor. Bu gerekiyorsa ayrı bir sözleşme değişikliği ve test gerekir.
4. Test: pasif oda tipine atanmış medya + yalnız alt metin → 200; aktif olmayan farklı oda tipine atama → 404; UI testinde alt-only PATCH gövdesinde `roomTypeId` bulunmadığı doğrulanmalı.

**Kabul testi:**
- Pasif oda tipine atanmış medya, yalnız alt metin PATCH'i → **200**, alt metin güncellenmiş.
- Aynı medyayı pasif oda tipinden başka bir pasif oda tipine taşıma → **404/409** (spec kararına göre).
- UI alt metin kaydı, oda tipi değişmediğinde `roomTypeId` göndermiyor.

---

### QA-03 — Yükleme hata yollarında liste yenilenmiyor; FAILED/UPLOADED kayıt görünmüyor

**Önem:** Orta (kurtarma akışı/UX)
**Durum:** Kod yolu doğrulandı
**İlgili dosyalar:**
- `admin-web/src/features/media/media-manager.tsx:50-66`
- `admin-web/src/features/media/upload-media.ts:13-41`
- `backend/hotel-api/src/main/java/com/hotel/media/internal/application/MediaUploadService.java:91-117`
- `admin-web/src/features/media/use-media-query.ts:25-31`

**Kanıt:**
`submitUpload` yalnızca `uploadMedia(file)` başarıyla çözüldüğünde listeyi yeniliyor:

```ts
try {
  await uploadMedia(file);
  setFile(null);
  await refresh();
} catch {
  setUploadError(true);
} finally {
  setUploading(false);
}
```

`uploadMedia` akışı üç adımlıdır: presigned başlat → doğrudan MinIO PUT → `complete`. Hata yolları:

- **Start 415/400:** Medya kaydı oluşmaz; liste yenilemesi gerekmez.
- **PUT hatası:** Start kaydı `UPLOADED` olarak DB'de kalır; liste yenilenmez.
- **Complete 422 (bozuk görsel):** Backend kaydı `FAILED` yapar ve commit eder; liste yenilenmez.
- **Complete 409 (süresi geçmiş):** Backend kaydı `FAILED` yapar; liste yenilenmez.
- **Complete 503 (depolama):** Transaction rollback olur; kayıt `UPLOADED` kalır; liste yenilenmez.

`useMediaQuery` yalnızca listede `PROCESSING` varsa 3 saniyede bir yenileniyor; `FAILED` veya `UPLOADED` kayıtlar otomatik yenileme tetiklemiyor. Bu nedenle admin, yükleme hatasından sonra oluşan kaydı ancak “Yenile”ye basarsa görüyor; FAILED kaydın retry butonu da o ana kadar erişilemiyor. Mevcut admin-web upload testi yalnızca başarılı akışı kapsıyor (`media-manager.test.tsx:290-356`).

**Gerekçe:**
Spec, `FAILED` işleme nedeni ve retry aksiyonunun admin ekranında görünmesini istiyor. Hata anında kayıt sunucuda oluştuğu halde UI bunu göstermiyor; kullanıcı aynı dosyayı yeniden yükleyerek yeni bir kayıt daha oluşturabiliyor. `UPLOADED` durumunda ise kayıt görünmediği için arşivleme/temizleme de yapılamıyor.

**Öneri:**
1. `submitUpload` içinde `refresh()` çağrısını `finally` bloğuna alın (veya en azından start başarılı olduktan sonraki tüm hata yollarında çağırın). Böylece `FAILED`/`UPLOADED` kayıtlar listede görünür.
2. `FAILED` kayıt için retry butonu zaten var; `UPLOADED` için “tamamlamayı yeniden dene” veya “arşivle” aksiyonu değerlendirin (presigned süresi dolduysa yeniden yükleme gerekebilir).
3. Vitest/MSW testi ekleyin: `complete` 422 döndüğünde liste yeniden çekiliyor ve `FAILED` + retry görünüyor; PUT hata verdiğinde `UPLOADED` kayıt görünüyor.
4. Mümkünse E2E'de bozuk görsel yükleyerek bu akışı doğrulayın.

**Kabul testi:**
- Complete 422 sonrası admin listesinde ilgili kayıt `FAILED` ve “Yeniden dene” butonuyla görünür.
- PUT hatası sonrası kayıt `UPLOADED` olarak görünür ve arşivlenebilir.
- Başarılı yüklemede mevcut davranış değişmez.

---

### QA-04 — `@media` E2E alt metni doğrulamıyor; kanıt fixture'ı yanlış tanımlıyor

**Önem:** Düşük (test/kanıt boşluğu)
**Durum:** Doğrulandı
**İlgili dosyalar:**
- `e2e/tests/media/media.spec.ts:78-110`, `:124-133`, `:197-211`
- `specs/done/evidence/0005/AC-6-media-e2e-summary.md:4`
- `specs/0005-media.md:106`, `:177`, `:192`

**Kanıt:**
E2E akışı:
- Fixture olarak `web/public/media/hero/home-hero-poster-1280.webp` dosyasını **WebP** olarak yüklüyor (`mimeType: "image/webp"`).
- Oda atama, publish ve cover adımlarını yapıyor.
- Public vitrinde yalnızca `src` ve `Content-Type` kontrol ediyor; `room-card-image` elemanının `alt` değerini hiç kontrol etmiyor.
- `admin-media-alt-tr` / `admin-media-alt-en` alanları hiç doldurulmuyor.
- Test sonunda yazılan `AC-6-media-e2e-summary.md` ise “real PNG upload” diyor.

Ayrıca `AC-4-admin-media-states.png` yalnızca testin başındaki boş durumda (`admin-media-empty`) alınıyor; loading ve error durumlarının görsel kanıtı bu dosyada yok.

**Gerekçe:**
Spec §2.7 ve AC-6, `@media` akışında “public vitrinde `room-card-image` ve alt metni doğrula” diyor. AC-4 ise loading/empty/error durumlarının doğrulanmasını istiyor. Alt metin davranışı web birim testlerinde kapsanıyor; backend IT alt metni yazıyor ancak public yanıtta bu alanları doğrulamıyor. Uçtan uca akışta özel alt metin ve dil değişimi kanıtlanmıyor. Kanıttaki “PNG” ifadesi fixture ile çeliştiği için kanıt güvenilirliğini azaltıyor.

**Öneri:**
1. Yükleme sonrası TR ve EN alt metinleri doldurun; publish sonrası `room-card-image` için `alt` değerini TR'de doğrulayın; EN sayfasına geçip EN alt metnini (veya eksikse oda adı fallback'ini) doğrulayın.
2. Public API yanıtında `altTextTr/altTextEn` alanlarını da kontrol edin.
3. Fixture'ı gerçekten PNG yapın veya `AC-6-media-e2e-summary.md` metnini “WebP” olarak düzeltin.
4. Loading/empty/error durumları için ayrı ekran görüntüleri alın (veya bu durumların Vitest kapsamında olduğunu kanıt dosyasında açıkça belirtin).
5. E2E kanıt özetini testin gerçekten yaptığı adımlarla birebir eşleştirin.

**Kabul testi:**
- `@media` testi özel TR/EN alt metinle geçer ve `alt` özniteliğini doğrular.
- Kanıt özeti fixture türü ve adımlarla uyumludur.
- AC-4 durum görselleri/kanıtı loading, empty ve error durumlarını kapsar.

---

### QA-05 — 0005 kanıt seti `integration`/`179d3d0` ile uyumsuz

**Önem:** Orta (süreç / Altın Kural 6)
**Durum:** Doğrulandı
**İlgili dosyalar:**
- `specs/done/evidence/0005/AC-6-ci-run.md`
- `specs/done/evidence/0005/AC-1-media-schema.log`
- `specs/done/evidence/0005/AC-1-media-constraints.log`
- `specs/done/evidence/0005/AC-2-presigned-upload.log`
- `specs/done/evidence/0005/AC-3-processing-pipeline.log`
- `specs/done/evidence/0005/AC-6-minio-it.log`
- `specs/done/evidence/0005/AC-6-media-e2e-summary.md`
- `specs/done/evidence/0005/code-review-2026-11-07-verification.md`
- `specs/done/evidence/0005/AC-6-archive-sha256.txt`
- `specs/done/evidence/0005/AC-6-code-review.md`

**Kanıt:**

| Kanıt dosyası | Yazılan | Güncel gerçek |
|---|---|---|
| `AC-6-ci-run.md:10-15` | 173 birim / 56 IT / MediaIT 9/9 | **179 birim / 57 IT / MediaIT 10/10** |
| `AC-6-ci-run.md:21-22` | web 42, admin-web 32 | **web 43, admin-web 35** |
| `AC-6-ci-run.md:42` | commit `3900eea` CI success | `integration` **179d3d0**, run **42326617436** success |
| `AC-1-media-schema.log:16` | 54 IT, MediaIT 7/7 | **57 IT, MediaIT 10/10** |
| `AC-1-media-constraints.log:12` | MediaIT 7/7 | **10/10** |
| `AC-2-presigned-upload.log:14` | MediaIT 7/7 | **10/10** |
| `AC-3-processing-pipeline.log:11` | MediaIT 7/7 | **10/10** |
| `AC-6-minio-it.log:13` | MediaIT 9/9 | **10/10** |
| `AC-6-media-e2e-summary.md:4` | “real PNG upload” | Fixture **WebP** |
| `code-review-2026-11-07-verification.md:5` | İncelenen head `7f8a75b` | Son ağaç `8538124` ile aynı; `8538124` ek metrik/test değişiklikleri içeriyor |
| `AC-6-code-review.md:10` | “fix PR open, merge forbidden” | PR #27 **merged**; merge commit `179d3d0` (2026-11-07) |

Ek olarak:
- `AC-6-archive-sha256.txt:3` “verify with `node e2e/scripts/sha256-manifest.mjs verify ...`” diyor; ancak script JSON manifest bekliyor, dosya ise metin formatında. Hash'ler elle doğrulandı ve **129/129** eşleşti; yine de komut çalıştırılabilir değil.
- `AC-6-ci-run.md` 2026-11-03 tarihli; son fix commit'i 2026-11-07.
- `specs/0005-media.md:211` SCORECARD'da “fix PR açık” ifadesi güncellenmemiş.

**Gerekçe:**
Projenin “Kanıtsız iş bitmez” kuralı gereği her AC'nin kanıtı, denetlenen kodla aynı commit'i temsil etmelidir. Mevcut kanıt dosyaları eski commit'e ve eski test sayılarına işaret ediyor; bir gözden geçiren kişi kanıtları okuyarak `integration`'daki kodun doğruluğunu teyit edemez. Gerçek CI yeşil olsa bile kanıt seti güncel değilse spec kapatma ve denetim izi zayıflar. Ayrıca bağımsız review onayı son ağaçtaki küçük ekleri (metrikler ve güçlendirilmiş rollback testi) açıkça kapsamıyor.

**Öneri:**
1. `179d3d0` üzerinde tüm testleri yeniden çalıştırıp 0005 kanıt dosyalarını güncelleyin; en azından yeni bir `AC-6-integration-179d3d0.md` ekleyin:
   - komutlar,
   - tam test sayıları,
   - CI run URL'si,
   - E2E adımları,
   - tarih ve commit.
2. Eski dosyaları “historical” olarak işaretleyin veya güncel sayılarla değiştirin.
3. `AC-6-media-e2e-summary.md` fixture türünü düzeltin (QA-04).
4. Review kaydını son ağacı kapsayacak şekilde güncelleyin: `8538124` / `179d3d0` farkı için “delta review” notu ekleyin veya yeni bir doğrulama turu yapın.
5. `AC-6-archive-sha256.txt` için JSON manifest üretin veya script'i metin manifestini okuyacak şekilde güncelleyin; başlıktaki komutu çalıştırılabilir hale getirin.
6. `AC-6-code-review.md` ve SCORECARD'daki “fix PR açık” ifadelerini güncel duruma göre düzeltin.

**Kabul testi:**
- Tüm 0005 kanıtları `179d3d0` (veya sonraki head) ile eşleşir.
- Test sayıları gerçek çıktıyla birebir aynıdır.
- CI URL'si ve commit hash'i doğrudur.
- Arşiv manifesti verilen komutla doğrulanabilir.
- Review onayı son ağacı kapsar.

---

### QA-06 — Spec/implementation drift: V9–V11 migration'ları ve ek hata kodları

**Önem:** Düşük
**Durum:** Doğrulandı
**İlgili dosyalar:**
- `specs/0005-media.md:57`, `:61-64`, `:114-115`, `:158`, `:208-211`
- `backend/hotel-api/src/main/java/com/hotel/media/internal/domain/MediaErrorCodes.java:18-19`
- `backend/hotel-api/src/main/resources/db/migration/media/V9__add_media_processing_lease.sql`
- `backend/hotel-api/src/main/resources/db/migration/media/V10__create_public_cleanup_tasks.sql`
- `backend/hotel-api/src/main/resources/db/migration/media/V11__harden_public_cleanup_retries.sql`

**Kanıt:**
- Spec §2.2 hâlâ yalnızca `V8__create_media_assets.sql` diyor; veri modelinde `processing_lease_id` alanı geçiyor ama bu alanı ekleyen `V9` listede yok.
- Kodda ayrıca `V10__create_public_cleanup_tasks.sql` ve `V11__harden_public_cleanup_retries.sql` var; cleanup-intent tablosu ve dead-letter alanı bunlarla geliyor.
- Spec §2.1 hata kodu listesinde `media.alt_text_invalid` ve `media.storage_unavailable` yok; ikisi de kodda kullanılıyor.
- SCORECARD `:211` “fix PR açık” diyor; oysa PR #27 birleştirilmiş ve merge commit'i `179d3d0`. `specs/0005-media.md:13` ve `:202` durum/DoD ise hâlâ merge onayı bekliyor olarak işaretli.

**Gerekçe:**
Spec “tek gerçek kaynak” olma iddiasını taşıyor. Yeni bir geliştirici veya denetçi yalnızca spec'i okuyarak lease/cleanup tablolarının varlığını ve hata sözleşmesini öğrenemez. Bu, özellikle `V10/V11` operasyonel runbook'a bağlı olduğu için risklidir.

**Öneri:**
1. Spec §2.2'yi `V8` + `V9` + `V10` + `V11` olarak güncelleyin; her migration'ın ne eklediğini yazın.
2. §2.1 hata kodu listesine `media.alt_text_invalid` ve `media.storage_unavailable` ekleyin (veya kodun kullanmadığı bir listeye indirin).
3. AC-1/§3'teki “yalnız V8” ifadelerini düzeltin; V9–V11'in yalnız `media` şemasına dokunduğunu açıkça belirtin.
4. D3/D15 kararlarının migration karşılıklarını yazın (D3 → V9, D15 → V10/V11).
5. SCORECARD ve spec durumunu Takım Yöneticisi onayı sonrası güncelleyin; onay öncesi “bekliyor” ifadesi kalacaksa bunu net bir tarih/koşulla yazın.

**Kabul testi:**
- Spec'teki migration listesi `flyway_schema_history` ile birebir eşleşir.
- Hata kodu listesi `MediaErrorCodes` ile uyumludur.
- AC-1 kanıtı V8–V11'i kapsar.

---

### QA-07 — Public `/api/media` için `503` sözleşmesi uygulanmıyor

**Önem:** Düşük
**Durum:** Statik doğrulandı
**İlgili dosyalar:**
- `specs/0005-media.md:49`
- `backend/hotel-api/src/main/java/com/hotel/media/internal/web/PublicMediaController.java:50-57`
- `backend/hotel-api/src/main/java/com/hotel/media/internal/application/MediaPublicQueryService.java:64-83`
- `backend/hotel-api/src/main/java/com/hotel/web/GlobalExceptionHandler.java:95-108`, `:159-164`

**Kanıt:**
Spec, `GET /api/media` için hata setini `400`, `503` olarak veriyor. Controller doğrudan servisi çağırıyor; serviste DB/catalog sorguları `DataAccessException` üretebilir. `GlobalExceptionHandler` yalnızca `MediaStorageException` için 503 dönüyor; diğer tüm beklenmeyen exception'lar generic handler ile **500** oluyor. Public media akışı storage'a dokunmadığı için pratikte 503 üretilmesi gereken tek altyapı hatası DB/catalog erişimi olacaktır; bu durumda 500 dönüyor.

**Gerekçe:**
Sözleşme, istemcinin “servis geçici olarak kullanılamıyor” ile “beklenmeyen iç hata”yı ayırt etmesini bekliyor. Web fallback'i her iki durumda da placeholder gösterdiği için kullanıcı etkisi sınırlı; ancak monitoring, retry ve API sözleşmesi açısından fark önemlidir.

**Öneri:**
1. Public media için `DataAccessException` (ve gerekirse `CannotAcquireLockException`/`PessimisticLockingFailureException`) → **503** `media.service_unavailable` eşlemesi ekleyin.
2. Alternatif olarak spec'i bilinçli olarak 500 kabul edecek şekilde güncelleyin; hangi seçenek seçilirse testle sabitleyin.
3. Public endpoint için DB hatası enjekte eden bir birim/IT testi ekleyin.

**Kabul testi:**
- DB erişilemezken `GET /api/media` → **503** (veya spec güncellenmişse beklenen kod).
- Mevcut 400/200 davranışı değişmez.

---

## 4. Açık Riskler / Operasyonel Notlar

### R-01 — 3 sn gateway timeout'u ile senkron medya I/O çakışabilir

**Önem:** Orta (risk; yerelde yeniden üretilmedi)
**İlgili dosyalar:**
- `backend/admin-api/src/main/resources/application.yml:36`
- `backend/admin-api/src/main/java/com/hotel/gateway/internal/application/AdminGatewayClient.java:39`, `:55-56`
- `backend/hotel-api/src/main/java/com/hotel/media/internal/application/MediaUploadService.java:100-101`
- `backend/hotel-api/src/main/java/com/hotel/media/internal/application/MediaAdminService.java:228-237`, `:267`, `:280`, `:285-296`, `:311-334`
- `backend/hotel-api/src/main/java/com/hotel/media/internal/application/MediaPublicCleanupService.java:84-89`
- `specs/done/0004-admin-identity.md:99`

**Kanıt:**
admin-api gateway tüm istekler için **3 saniye** timeout kullanıyor. Buna karşılık:
- `complete` 15 MB'a kadar nesneyi **tamamen okuyup** `inspect` ediyor.
- `retry`, FAILED orijinali **tamamen okuyup** yeniden `inspect` ediyor.
- `publish` altı varyantı **sırayla** S3'e kopyalıyor.
- `unpublish`/`archive` commit sonrası altı public nesneyi **sırayla** siliyor (`processNow` request thread'inde afterCommit).

Yerel MinIO'da bu süreler 3 sn altında kaldığı için E2E geçti. Gerçek S3/uzak depoda gecikme arttığında gateway 504 dönebilir; hotel-api işlemi arka planda tamamlamaya devam edebilir. Bu durumda kullanıcı “başarısız” görürken DB'de `PROCESSING`/`published`/`archived` durumu değişmiş olabilir; tekrar denemeleri 409 veya 200 ile karışık sonuç verebilir.

**Gerekçe:**
0005 D6 kararı, `complete`'in gateway timeout'unu koruyacak kadar hafif olmasını söylüyor; ancak tam nesne okuma (`complete`/`retry`) ve 6'lı kopyalama/silme döngüleri bu hedefi zorlayabilir. 0004'teki 3 sn değeri tüm gateway için sabit; media için özel bir değer yok.

**Öneri:**
1. Gerçek S3 veya gecikme enjekte edilmiş MinIO ile `complete`, `retry`, `publish`, `unpublish`, `archive` p95 ölçümü yapın.
2. Sonuca göre:
   - media için gateway timeout'unu artırın (0004 spec'i güncelleyerek), veya
   - `publish`/cleanup'ı kalıcı niyet + asenkron işleme alın; admin polling ile sonucu görsün, veya
   - `complete`'te tam decode'u worker'a bırakıp yalnız HEAD + sınırlı header/magic-byte kontrolü yapın.
3. Admin-web'de 504/timeout için açık bir mesaj ve “durumu yenile” yönlendirmesi ekleyin.
4. Ölçüm ve karar, kanıt dosyasına yazılmalı; “3 sn yeterli” varsayımı kanıtlanmadan bırakılmamalı.

**Kabul testi:**
- Hedef ortamda p95 < gateway timeout; veya timeout davranışı ve retry semantiği açıkça dokümante edilmiş.
- 504 sonrası admin, işlemin gerçek durumunu yenileyerek görebiliyor.

### R-02 — Dead-letter sonrası public nesne erişilebilir kalabilir

**Önem:** Düşük (kabul edilmiş risk / operasyonel)
**İlgili dosyalar:**
- `backend/hotel-api/src/main/java/com/hotel/media/internal/application/MediaPublicCleanupService.java:107-130`
- `docs/runbooks/media-public-cleanup.md`
- `specs/0005-media.md:246` (D15)

**Kanıt:**
Public obje silme işlemi 10 kez başarısız olursa görev `dead_lettered_at` ile işaretleniyor; scheduler dead-letter'lı görevleri bir daha otomatik denemiyor. Nesne S3'te kalırsa, public API onu göstermese bile doğrudan URL erişilebilir olabilir. Runbook manuel temizlik tarif ediyor; otomatik alert/retry endpoint'i yok.

**Gerekçe:**
Spec D15 dead-letter'ı bilinçli olarak kabul ediyor; ancak AC-5 “unpublish/archive sonrası public obje erişimi kesilir” diyor. S3'ün kalıcı olarak silmeye izin vermediği durumda sistemin yapabileceği sınırlıdır; yine de operatörün zamanında haberdar olması ve manuel kapatma yolunun net olması gerekir. Bu, CR-14'te de not edilmiş bilinen bir konudur.

**Öneri:**
1. `media.public_cleanup.dead_lettered` için alert/runbook tatbikatı ekleyin.
2. Dead-letter görevlerini listeleyen ve yeniden denemeyi tetikleyen küçük bir operasyon ucu veya CLI ekleyin.
3. Periyodik orphan taraması (DB'de public key olarak referanslanmayan `public/` nesneleri) değerlendirin.
4. Runbook'a “unpublish/archive 200 yanıtı, S3 nesnesinin silindiğinin kanıtı değildir” notunu ekleyin.

**Kabul testi:**
- Dead-letter metric/alert çalışıyor.
- Runbook'taki manuel retry adımı doğrulandı.
- Public nesne erişimi manuel temizlik sonrası kesiliyor.

---

## 5. Kabul Edilen / Kapsam Dışı Konular

- **CR-4/CR-5 refactor:** `MediaAdminService` ve `MediaManager` bölünmesi bilinçli olarak ayrı refactor spec'ine ertelenmiş; bu denetimde yeni bir bulgu olarak sayılmadı.
- **Atama kaldırma (unassign):** `roomTypeId: null` şu anda “gönderilmedi” anlamına geliyor; UI'daki “Atanmadı” seçeneği disabled. Spec açıkça unassign istemiyor; gelecekte gerekiyorsa ayrı sözleşme değişikliği gerekir.
- **Kalıcı silme, audit, 360 tur, detay sayfası, CDN, video:** 0005 kapsamı dışı; spec ile uyumlu.
- **`0002 @empty` yerel tekrarı:** Denetim ortamı seed'li olduğu için yapılmadı; CI `179d3d0` üzerinde tam sırayı çalıştırdı.
- **Dead-letter:** D15 ile kabul edilmiş; R-02 olarak operasyonel takip önerildi.

---

## 6. Sonuç ve Öneri

**Merge blocker yok.** `integration` @ `179d3d0` üzerinde backend, frontend, E2E ve CI kalite kapıları yeşil; arşiv hash'leri değişmemiş.

Ancak tam APPROVE ve spec'in `done` durumuna alınması için aşağıdaki koşullar önerilir:

1. **QA-01** (cover 404) düzeltilmeli ve regresyon testi eklenmeli. UI bu yolu henüz kullanmıyor; ancak API sözleşmesi yanlış.
2. **QA-02** (pasif oda tipinde alt metin) düzeltilmeli veya Takım Yöneticisi tarafından açık risk olarak kabul edilmeli; erişilebilirlik metni düzeltilemiyor.
3. **QA-03** (yükleme hatasında liste yenileme) düzeltilmeli; aksi halde FAILED kayıtların retry akışı eksik kalıyor.
4. **QA-04/QA-05/QA-06** kanıt ve spec güncellemeleri yapılmalı; özellikle 0005 kanıt seti `179d3d0` ile eşleştirilmeli.
5. **QA-07** için 503 eşlemesi eklenmeli veya spec bilinçli olarak 500 kabul edecek şekilde güncellenmeli.
6. **R-01** için gerçekçi gecikme ölçümü yapılmalı; 3 sn timeout kararı kanıtlanmalı veya media için ayrı bir strateji belirlenmeli.
7. **R-02** için dead-letter alert/runbook kapanışı yapılmalı.

**Önerim:** Bu koşullar kapanana kadar **“koşullu geçer”**; özellikle QA-01/02/03 ve QA-05 kapanmadan **tam APPROVE** verilmemesi. Kodun mevcut mutlu yolunu bu bulgular engellemiyor; ancak spec'in “kanıtlı Done” iddiası için düzeltme ve kanıt güncellemesi gerekiyor.

---

## 7. Ek — Denetimde Kullanılan Başlıca Komutlar

```powershell
# Backend
cd backend/hotel-api; .\mvnw.cmd -B verify
cd backend/admin-api; .\mvnw.cmd -B verify

# Frontend
cd web; npm test; npm run lint; npm run build
cd admin-web; npm test; npm run lint; npm run build

# E2E (yerel)
cd e2e
$env:E2E_EVIDENCE_DIR = Join-Path $env:TEMP 'hotel-0005-qa\evidence'
npx playwright test tests/media --grep '@media'
npx playwright test tests/admin --grep '@admin'
npx playwright test tests/smoke.spec.ts --grep '@seeded'
npx playwright test tests/design --grep '@seeded'

# Arşiv bütünlüğü (manifest metin formatında olduğu için elle doğrulandı)
# 129/129 dosya hash'i eşleşti; 0 eksik, 0 değişmiş.
```

---

## 8. QA Düzeltmeleri Sonrası Bağımsız Yeniden Doğrulama (2026-11-07)

> **Yeniden denetim hedefi:** `integration` @ `7f51fe4` — QA fix merge `06d8182` + final evidence commit `7f51fe4`
> **Kaynak:** QA fix PR [#28](https://github.com/altananay/hotel-management-system/pull/28), docs PR [#29](https://github.com/altananay/hotel-management-system/pull/29)
> **Not:** Bu bölüm, §1–§7'deki ilk denetim bulgularının düzeltme sonrası durumunu kaydeder. İlk bulgular tarihsel kayıt olarak korunmuştur. **§8 tarihsel ara durumdur;** RES-01/RES-02 daha sonra `9397915` üzerinde kapatıldı, güncel durum §9'dadır.

### 8.1 Test sonuçları

| Alan | Komut | Sonuç |
|---|---|---|
| `hotel-api` | `.\mvnw.cmd -B verify` | **188 birim**, **58 IT**; `MediaIT` **11/11**; Spotless temiz; BUILD SUCCESS |
| `admin-api` | `.\mvnw.cmd -B verify` | **102 birim**, **5 IT**; Spotless temiz; BUILD SUCCESS |
| `web` | `npm test; npm run lint; npm run build` | **43/43**; lint/build başarılı |
| `admin-web` | `npm test; npm run lint; npm run build` | **39/39**; MediaManager **13/13**; lint/build başarılı |
| E2E `@media` | `npx playwright test tests/media --grep '@media'` | **1/1** (alt metin TR/EN dahil) |
| E2E `@admin` | `npx playwright test tests/admin --grep '@admin'` | **3/3** |
| E2E `@seeded` | `npx playwright test tests/smoke.spec.ts --grep '@seeded'` | **2/2** |
| E2E design | `npx playwright test tests/design --grep '@seeded'` | **40/40** |

`hotel-api` IT dağılımı: `CatalogSeedIT` 2/2, `RoomCatalogIT` 40/40, `DatabaseSchemaAndRoleSeparationIT` 5/5, `MediaIT` 11/11.

CI:
- PR #28 final head `5fb88a6` → run **42376555577**: success.
- Integration merge `06d8182` → run **42377168433**: success.
- Final integration `7f51fe4` → run **42379789000**: success.
- Dört job da yeşil: Backend, Frontend, E2E Smoke, CI Quality Gate.

E2E sırası: `@empty` yerelde yeniden koşulmadı (yerel DB seed'li); CI `7f51fe4` üzerinde tanımlı sırayı çalıştırdı. Diğer yerel E2E adımları yukarıdaki gibi geçti.

### 8.2 Bulgu bazında kapanış

| Bulgu | Durum | Bağımsız kanıt |
|---|---|---|
| **QA-01** — cover 404 | **Kapatıldı** | `MediaAdminServiceTest` + `MediaIT` 11/11; canlı API’de atanmamış mevcut medya → **409 `media.not_published`** |
| **QA-02** — pasif oda tipinde alt metin | **Kapatıldı** | Unit + UI testleri; canlı API’de aynı pasif `roomTypeId` ile alt metin PATCH → **200**, alt metin güncellendi |
| **QA-03** — yükleme hatasında liste yenileme | **Kapatıldı** | `media-manager.test.tsx`: complete 422 → FAILED + retry; PUT 503 → UPLOADED + archive |
| **QA-04** — E2E alt metni / fixture türü | **Kapatıldı** | `@media` 1/1; TR/EN `alt`, public API `altTextTr/altTextEn`, WebP fixture doğrulandı; `AC-4-admin-media-states.md` eklendi |
| **QA-05** — kanıt seti / sürüm uyumu | **Kapatıldı (küçük rezidüel)** | Güncel kanıtlar ve tarihsel etiketler eklendi; tüm sayılar bağımsız koşuyla eşleşiyor. Rezidüel: `RES-01` |
| **QA-06** — spec/migration drift | **Kapatıldı (0005)** | V8–V11, `media.alt_text_invalid`, `media.storage_unavailable`, `media.service_unavailable` ve tarihsel Context güncellendi. Rezidüel: `RES-02` |
| **QA-07** — public 503 | **Kapatıldı** | `TransactionTemplate` sınırı + `DataAccessException`/`TransactionException` → 503; gerçek DB durdurma testinde **503 `media.service_unavailable`**, restart sonrası **200** |
| **R-01** — gateway timeout | **Koşullu kapandı** | `/admin-api/media/**` için 15 sn media timeout + test; gerçek S3 p95 ölçümü ve 3 sn connect timeout rezidüeli sürüyor |
| **R-02** — dead-letter operasyonu | **Kısmen kapandı** | Runbook alarm ifadeleri, manuel retry, orphan taraması ve tatbikat tablosu eklendi; canlı alarm kuralı/gerçek tatbikat kaydı yok |

### 8.3 Rezidüel bulgular

#### RES-01 — Text-mode SHA-256 doğrulayıcı eklenen dosyaları tespit etmiyor

**Önem:** Düşük
**Durum:** Doğrulandı
**İlgili dosyalar:**
- `e2e/scripts/sha256-manifest.mjs:23-46`, `:61-85`
- `specs/done/evidence/0005/AC-6-archive-sha256.txt:3-5`

**Kanıt:**
`0001` klasörüne geçici bir dosya eklendiğinde:

```text
Listed files: 129
Result: UNCHANGED
```

döndü. Script text mode’da yalnızca manifestte listelenen dosyaların hash’lerini kontrol ediyor; `0001`–`0004` altına eklenen yeni dosyaları “added” olarak raporlamıyor. JSON modunda bu kontrol vardı; text mode’da kaldırılmış.

**Gerekçe:**
AC-6 “0001–0004 arşivleri değişmemiş” diyor. Bir dosya eklenmesi de arşiv değişikliğidir; mevcut doğrulayıcı bunu kaçırabilir. Manifest başlığı bu sınırı açıkça yazsa da, komutun “UNCHANGED” sonucu tam bir arşiv değişmezliği kanıtı değildir.

**Öneri:**
1. Text mode’da manifestteki section’ları (`0001`–`0004`) kullanarak ilgili dizinlerdeki gerçek dosya kümesini çıkarın; manifestte olmayan dosyaları `added` olarak raporlayın.
2. Alternatif olarak arşiv doğrulaması için JSON manifest kullanın (CI’daki 0002 yaklaşımı gibi); text manifest yalnızca insan-okur özet kalsın.
3. Regresyon testi: ilgili klasöre geçici dosya eklenince komut `CHANGED` dönmeli.

**Kabul testi:**
- `0001`–`0004` altına eklenen yeni dosya → `CHANGED`/`added`.
- Mevcut 129 dosyanın hash’i değişmediğinde → `UNCHANGED`.

#### RES-02 — 0004 done spec’i gateway timeout’unu hâlâ 3 sn olarak tanımlıyor

**Önem:** Düşük (doküman)
**Durum:** Doğrulandı
**İlgili dosyalar:**
- `specs/done/0004-admin-identity.md:99`
- `docs/architecture.md:125`
- `backend/admin-api/src/main/resources/application.yml:36-37`

**Kanıt:**
0004 spec’i “Bağlantı + okuma toplamı **3 sn**; `ADMIN_GATEWAY_TIMEOUT_MS=3000`” diyor. Yeni davranışta `/admin-api/media/**` için **15 sn** media timeout kullanılıyor; `docs/architecture.md` güncellendi ancak 0004 done spec’i güncellenmedi. 0005 spec’i de media timeout’unu ayrıca listelemiyor.

**Gerekçe:**
0004 tarihsel/done bir spec olsa da gateway sözleşmesinin tek kaynağı olarak okunabilir. Media için 15 sn istisnası yalnızca architecture dosyasında kalırsa, 0004’ü okuyan geliştirici/denetçi çelişkili sözleşme görür.

**Öneri:**
1. 0004 done spec’ine tarihli bir amendment notu ekleyin: “0005 ile `/admin-api/media/**` için 15 sn media timeout eklendi; genel 3 sn korunuyor.”
2. Veya 0005 spec’inde R-01/D17 olarak media timeout’unu açıkça kararlaştırın ve 0004’e referans verin.
3. `specs/done/README.md` / doküman indeksinde güncel gateway sözleşmesinin `docs/architecture.md` olduğunu belirtin.

**Kabul testi:**
- 0004, 0005 ve architecture dosyaları aynı timeout sözleşmesini anlatır.
- `.env.example` ve `application.yml` anahtarları dokümante edilir.

#### RES-03 — R-01/R-02 operasyonel kapanışı hâlâ kanıt/ölçüm bekliyor

**Önem:** Düşük (risk)
**Durum:** Açık
**İlgili dosyalar:**
- `specs/done/evidence/0005/AC-6-qa-fixes-verification.md:112-124`
- `docs/runbooks/media-public-cleanup.md:12-52`, `:150-180`

**Kanıt:**
- R-01 için 15 sn media timeout kodu/testi var; ancak gerçek S3 üzerinde p95/p99 ölçümü yok. `HttpClient` connect timeout’u hâlâ 3 sn; yavaş bağlantıda media isteği yine erken 504 alabilir.
- R-02 için runbook’ta Prometheus/Alertmanager örnek kuralları ve tatbikat tablosu var; ancak repoda çalıştırılabilir alert kuralı/config’i ve gerçek tatbikat kaydı yok.

**Gerekçe:**
Kod ve runbook iyileştirmesi anlamlı; ancak “kanıtlı kapanış” için üretim benzeri ölçüm ve operasyon tatbikatı gerekir. Bunlar bu QA turunda üretilemedi.

**Öneri:**
1. Gerçek S3 veya gecikme enjekte edilmiş ortamda `complete`, `retry`, `publish`, `unpublish`, `archive` p95/p99 ölçün; 15 sn ve connect timeout kararını buna göre doğrulayın.
2. Alert kurallarını dağıtım reposuna/ortama ekleyin; en az bir dead-letter tatbikatını tarih/operatör/görev ID ile kaydedin.
3. Ölçüm ve tatbikat sonucunu `specs/done/evidence/0005/` altına yeni bir kanıt dosyası olarak ekleyin.

**Kabul testi:**
- Gerçekçi gecikme altında p95 < 15 sn; veya timeout/retry semantiği dokümante edilmiş.
- Dead-letter alarmı canlı ortamda test edilmiş; tatbikat kaydı mevcut.

### 8.4 Yeniden denetim sonucu

- **QA-01, QA-02, QA-03, QA-04, QA-07:** bağımsız testlerle **kapandı**.
- **QA-05, QA-06:** ana bulgular kapandı; `RES-01`/`RES-02` düşük doküman/araç rezidüelleri kaldı.
- **R-01:** koşullu; gerçek S3 p95 ölçümü ve connect timeout değerlendirmesi açık.
- **R-02:** runbook seviyesinde kapandı; canlı alarm/tatbikat kanıtı açık.

**Güncel öneri:** İşlevsel merge blocker yok. Takım Yöneticisi `RES-01`/`RES-02` için küçük düzeltme veya açık kabul kararı verirse, R-01/R-02 için operasyonel ölçüm/tatbikat planıyla spec `done` aşamasına alınabilir. Aksi halde “koşullu geçer” durumu devam eder.

---

## 9. Rezidüel Düzeltmeler Sonrası Bağımsız Retest (2026-11-07)

> **Retest hedefi:** `integration` @ `9397915` — `fix(media): close residual Spec 0005 QA findings (#30) [plan 0005]`
> **Kaynak:** PR [#30](https://github.com/altananay/hotel-management-system/pull/30); PR head `f1fc3bc` / merge `9397915`
> **Not:** §8’deki RES-01/RES-02/RES-03 durumlarının düzeltme sonrası bağımsız retestidir.

### 9.1 Test sonuçları

| Alan | Komut | Sonuç |
|---|---|---|
| `hotel-api` | `.\mvnw.cmd -B verify` | **189 birim**, **58 IT**; `MediaIT` **11/11**; Spotless temiz; BUILD SUCCESS |
| `admin-api` | `.\mvnw.cmd -B verify` | **104 birim**, **5 IT**; `AdminGatewayClientTest` **6/6**; Spotless temiz; BUILD SUCCESS |
| `web` | `npm test; npm run lint; npm run build` | **43/43**; lint/build başarılı |
| `admin-web` | `npm test; npm run lint; npm run build` | **39/39**; MediaManager **13/13**; lint/build başarılı |
| E2E `@media` | `npx playwright test tests/media --grep '@media'` | **1/1** |
| E2E `@admin` | `npx playwright test tests/admin --grep '@admin'` | **3/3** |
| E2E `@seeded` | `npx playwright test tests/smoke.spec.ts --grep '@seeded'` | **2/2** |
| E2E design | `npx playwright test tests/design --grep '@seeded'` | **40/40** |
| Manifest testleri | `npm run test:manifest` | **4/4** |
| Prometheus kuralları | `promtool check rules infra/prometheus/media-public-cleanup.rules.yml` | **SUCCESS: 2 rules found** |
| Prometheus scrape | `GET http://localhost:8081/actuator/prometheus` | **200**; iki cleanup metriği `0.0` |
| Actuator koruması | `GET /actuator/env` | **401** |
| Arşiv manifesti | `node e2e/scripts/sha256-manifest.mjs verify ...` | **129/129 UNCHANGED** |
| Eklenen dosya testi | `0001` altına geçici dosya + verify | **CHANGED**; `added: 0001/...` |

CI:
- `integration` @ `9397915` → run **42389678373**: success.
- Dört job da yeşil: Backend, Frontend, E2E Smoke, CI Quality Gate.
- `@empty` yerelde yeniden koşulmadı (yerel DB seed'li); CI `9397915` üzerinde tam sırayı çalıştırdı.

R-02 tatbikat kontrolü:
- `R-02-dead-letter-drill-2026-11-07.md` içindeki iki geçici görev ID’si DB’de yok.
- `media.media_public_cleanup_tasks` sayısı **0**.
- `hotel-api :8091` dinlemiyor.

R-01 gecikme proxy kontrolü:
- `infra/scripts/tcp-latency-proxy.mjs` MinIO health üzerinden **HTTP 200** döndü.
- Gerçek S3 p95/p99 ölçümü hâlâ yapılmadı; bu, R-01’in koşullu kapanışını koruyor.

### 9.2 Rezidüel bulgu kapanışı

| Bulgu | Durum | Bağımsız kanıt |
|---|---|---|
| **RES-01** — text-mode manifest added-file | **Kapatıldı** | `npm run test:manifest` 4/4; `0001` altına eklenen geçici dosya → **CHANGED**; normal manifest 129/129 **UNCHANGED** |
| **RES-02** — 0004 timeout drift | **Kapatıldı** | `specs/done/0004-admin-identity.md` §2.2.7 amendment; `specs/0005-media.md` §2.1 + D17; `docs/architecture.md` §6; config ve `.env.example` uyumlu |
| **RES-03** — R-01/R-02 operasyonel kapanış | **Koşullu kapandı** | R-01: media’ya özel **connect + read 15 sn**, 6/6 gateway testi, proxy health 200; gerçek S3 p95/p99 yok. R-02: `infra/prometheus` kuralı (promtool 2/2), `/actuator/prometheus` 200 + CIDR testi, yerel dead-letter/manuel retry tatbikatı ve tablo 0; canlı Prometheus/Alertmanager tetiklemesi yok |
| **QA-01–QA-07** | **Regresyon yok** | §8’deki bağımsız testler ve bu turdaki tam suite yeşil |

### 9.3 Kalan operasyonel notlar

1. **R-01 üretim ölçümü:** 15 sn güvenli üst sınır olarak alınmış; gerçek S3 üzerinde `complete`, `retry`, `publish`, `unpublish`, `archive` p95/p99 ölçülmeli. Ölçüm `docs/runbooks/media-gateway-timeout.md` ile yapılabilir.
2. **R-02 canlı alarm:** Kural dosyası ve yerel tatbikat var; canlı Prometheus/Alertmanager dağıtımı ve ilk gerçek alarm tetiklemesi operasyon ekibinin kapanış adımı.
3. **Prometheus CIDR güvenlik notu:** Lokal Docker Desktop’ta container → host portu testi `remoteAddr`’ı `127.0.0.1` olarak gösterebildiği için dış-kaynak CIDR davranışı yerelde güvenilir doğrulanamadı. Üretimde scrape doğrudan pod/IP üzerinden yapılmalı; `/actuator/prometheus` reverse proxy’de public’e açılmamalı.
4. **CI kanıt izi:** `residual-findings-response-2026-11-07.md` branch head’i (`f1fc3bc`), PR #30 merge bilgisini ve son `integration` CI run’ını (`42389678373`) içermiyor; bu retest kaydı §9.1’de tutuluyor. İstenirse kanıt dosyasına da eklenmeli.
5. **Promtool CI:** `promtool` yerelde başarılı; CI’da ayrı bir `promtool check rules` adımı yok. Kural dosyası değişikliklerinde CI doğrulaması eklenmesi önerilir.
6. **Spec durumu:** `specs/0005-media.md` hâlâ “QA yeniden testi bekleniyor” diyor; bu retest geçti. Durum/DoD güncellemesi Takım Yöneticisi onayına bağlıdır.

### 9.4 Retest sonucu

- **RES-01 ve RES-02 kapandı.**
- **RES-03 koşullu kapandı:** kod, test, runbook, scrape ve yerel tatbikat tamam; gerçek S3 p95/p99 ve canlı alarm dağıtımı açık.
- **İşlevsel/merge blocker yok.**
- **Öneri:** Takım Yöneticisi R-01/R-02 için üretim ölçümü ve canlı alarm dağıtımını operasyonel kabul/plan olarak kaydederse spec `done` aşamasına alınabilir. Aksi halde sonuç **“koşullu geçer”** olarak kalır.
