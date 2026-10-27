# QA — 0003 Fix Doğrulama Eki (2026-10-26)

> Bu ek, `test(e2e): address 0003 QA findings` commit'i (`7fe7797`, merge PR #13) sonrasında yapılan son kontrolü kaydeder. Uygulama kodu, test kodu ve kapanmış spec değiştirilmemiştir; yalnızca okuma/çalıştırma ve bu kanıt dosyası eklenmiştir.

- **Denetleyen:** QA
- **Tarih:** 2026-10-26
- **Denetlenen head:** `7fe7797` (`integration`), QA fix kodu `5f8b4b3`
- **Önceki QA raporu:** `specs/done/evidence/0003/qa-verification-2026-10-25.md`

## 1. Yeniden koşumlar

| Kontrol | Sonuç |
|---|---|
| `e2e> npx playwright test tests/design --grep @seeded` | **40/40 passed** |
| `e2e> npx playwright test tests/smoke.spec.ts --grep @seeded` | **3/3 passed** |
| `web` Vitest / lint / build | Önceki koşumla aynı: **39/39**, lint/build yeşil |
| `0002` evidence `git diff --exit-code` | **0** — 12 dosya değişmedi |
| QA fix CI run `40232356949` (`45e6fc8`) | GitHub API: **success** |

## 2. QA bulgu kapanışları

| Bulgu | Durum | Kanıt |
|---|---|---|
| **QA-0003-1** — Kanıt hijyeni | **Kapandı** | `AC-1-asset-and-font-budget.log` ve `AC-5-build-guard.log` artık `OFL-SourceSans3.txt` için **4.486 B** yazıyor; `AC-1-scope.txt` final diff istatistiğiyle güncel; `AC-6-e2e-ci-summary.md` #39/#40 ve QA fix run #44'ü listeliyor. |
| **QA-0003-2** — Windows repackage kilidi | **Kapandı** | `e2e/README.md` içine backend çalışırken `mvn verify` repackage uyarısı ve `-Dspring-boot.repackage.skip=true` notu eklendi. |
| **QA-0003-3** — Rooms mock verisi | **Kapandı** | `SAMPLE_ROOMS` her odada **3 olanak** içeriyor ve olanaklar `code` ASC sırasında; design testi metin sırasını doğruluyor; `AC-3-rooms-*` görselleri yenilendi. |

## 3. QA-0003-3 sıralama notu (kapandı)

**Mock olanak sırası gerçek API sırasıyla aynı değil.**

- Gerçek seed/API `amenities` dizisini `code` ASC döndürür (ör. `cave_jacuzzi`, `espresso_bar`, `wifi`).
- `e2e/helpers/rooms.ts` içindeki mock sırası farklı (ör. `cave_jacuzzi`, `wifi`, `espresso_bar`); UI diziyi olduğu gibi render ettiği için `AC-3-rooms-tr-desktop.png` sırası üretimden farklı.
- **Etki:** Ürün hatası yok; 0002 smoke gerçek seed ile doğru sırayı doğruluyor. Yalnızca design kanıtının üretim verisine benzemesi ve sıra regresyonunun yakalanması için iyileştirme.
- **Öneri:** Mock olanaklarını `code` ASC sıralayın ve design testinde `room-card-amenity` metin sırasını da doğrulayın; `AC-3` ekran görüntülerini bir kez daha alın.
- **Gerekçe:** Tasarım kanıtı üretim payload'ına ne kadar yakınsa, görsel regresyon ve sıralama hatası o kadar erken yakalanır.

### 3.1 Fix (2026-10-27)

- `SAMPLE_ROOMS` olanakları `code` ASC sırasına alındı:
  `cave_jacuzzi, espresso_bar, wifi` / `stone_fireplace, terrace_view, wifi` /
  `cave_jacuzzi, stone_fireplace, terrace_view`.
- Design testi her kart için `room-card-amenity` metin sırasını birebir doğruluyor.
- `AC-3-rooms-*` ekran görüntüleri yeniden alındı.
- **Durum:** Not kapandı; ürün davranışı değişmedi.

## 4. Sonuç

- **Bloklayıcı hata:** Yok.
- **QA-0003-1/2:** Kapandı.
- **QA-0003-3:** Sayı/3 olanak ve `code` ASC sıralaması tamam; sıralama notu kapandı.
- **Spec kapanışı:** 0003 spec'i bu haliyle kapatılabilir; kalan açık not yok.
