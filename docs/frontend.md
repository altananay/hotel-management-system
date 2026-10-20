# Frontend Konvansiyonları (İSKELET)

> Bu dosya bir **iskelettir**; ilk dilimde profesyonel standarda genişletilecektir (tasarım token'ları, bileşen envanteri, kalite kapıları).
> Şimdilik aşağıdaki ilkeler **bağlayıcıdır**; sapma önerileri Altın Kural 4'e tabidir.

## Kapsam

- `web` — public site: TR/EN, Tailwind CSS v4 + shadcn/ui (Radix), animasyon: Motion (Framer Motion) + GSAP ScrollTrigger + Lenis.
- `admin-web` — admin paneli: TR, shadcn/ui, sade ve fonksiyonel.
- **MUI kullanılmaz** (D3). İki uygulama da Tailwind + shadcn paradigmasındadır.

## Bağlayıcı İlkeler (şimdi)

1. **API çağrıları yalnızca ayrı hook'lar üzerinden:** `useXxxQuery` / `useXxxMutation` (TanStack Query). Bileşen içinde doğrudan `fetch`/`axios` **yasaktır**; hook'lar API client'ı kullanır.
2. **Her ekranda üç durum zorunlu:** **yükleme** (skeleton/spinner), **boş** (açıklayıcı boş durum metni), **hata** (kullanıcı dostu mesaj + tekrar dene). Kanıtı alınır (bkz. `docs/testing.md`).
3. **Kullanıcıya teknik hata metni sızmaz.** Backend hata kodu (`booking.room_not_available`) → kullanıcı dostu i18n mesajına eşlenir; teknik detay yalnızca log'a ve correlation id'ye yazılır.
4. **Feature bazlı klasör yapısı:** `src/features/<feature>/…`; `src/app` yalnızca routing; `src/components/ui` tasarım sistemi bileşenleri; `src/lib` API client, query client, config.
5. **Dilimler mini-spec ile koşar:** her kriter için ekran görüntüsü kanıtı + kritik akışta smoke test.
6. **Zod her sınıra:** form doğrulama ve API yanıtı ayrıştırma; şema = tip kaynağı.
7. **CI:** lint + build zorunlu (ayrıntılı kalite kapıları ilk dilimde eklenecek).
8. **E2E seçicileri `data-testid` ile:** ad deseni `<alan>-<öğe>` (ör. `reservation-submit`); CSS sınıfı veya görünür metinle seçim yapılmaz (metin dile göre değişir).
9. **Dil geçişi (web):** rota tabanlı `/[locale]` (`tr` varsayılan; `/` → `/tr`); sözlükler `src/lib/i18n/{tr,en}.json`; `<html lang>` locale'e göre ayarlanır; i18n kütüphanesi ihtiyacı arayüz spec'inde değerlendirilir.

## İlk Dilimde Genişletilecekler

- Tasarım token'ları (renk, tipografi, boşluk ölçeği) ve tema yapısı.
- Bileşen envanteri ("hangi durumda hangi bileşen") + erişilebilirlik kontrol listesi.
- Animasyon/video rehberi: Motion/GSAP kullanım sınırları, `prefers-reduced-motion` zorunluluğu, mobil veri bütçesi (hero video ≤ 8 MB hedefi).
- Performans bütçesi (LCP, bundle boyutu) ve kalite kapıları.
- Hata mesajı kataloğu (makine kodu → TR/EN metin).
- API client üretimi: OpenAPI → TypeScript (orval önerisi); `contracts/` artefaktı ile senkronizasyon.
- Form konvansiyonları (react-hook-form + Zod), tarih/saat ve para formatlama kuralları (`BigDecimal` string → `Intl.NumberFormat`).
