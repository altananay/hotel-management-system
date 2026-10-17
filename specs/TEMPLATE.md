# Spec Şablonu

> Doldurmadan önce: `AGENTS.md` (altın kurallar) ve `docs/architecture.md` okunur.
> Numara: `specs/` altındaki en büyük numaranın + 1'i. Dosya: `specs/<####>-<kısa-ad>.md`. Branch: `feature/<####>-<kısa-ad>` (yalnızca `releasable`'dan).
> Spec onaylanmadan kod yazılmaz (Altın Kural 1). Onayı Takım Yöneticisi verir.

---

## 0. Künye

- **Spec No:** `<####>`
- **Başlık:** 
- **Durum:** Taslak / Onaylandı / Geliştiriliyor / Tamamlandı
- **Branch:** `feature/<####>-<kısa-ad>`
- **İlgili doküman/kararlar:** ör. `docs/architecture.md` §5, karar D6
- **Tarih:** 
- **Sahip:** 
- **Onaylayan (Takım Yöneticisi):** 

## 1. Intent (Neden?)

<!--
Bu özelliği kim istiyor, neden istiyor, başarı neye benziyor?
3-5 cümle, İŞ dilinde yazılır. Teknik çözüm anlatılmaz.
-->

## 2. Requirements

- **REQ-1:** 
- **REQ-2:** 

## 3. Constraints (Kapsam Dışı Dahil)

- **Teknik/İş kısıtları:** (stack, performans, güvenlik, uyum, veri)
- **Kapsam dışı:** (bilinçli olarak yapılmayacaklar)
- **Kabul edilen risk/ödünç:** 

## 4. Context

<!--
Mevcut durum, ilgili modüller/uygulamalar, bağımlı spec'ler, önceki kararlar.
Varsa akış/ekran taslağı, API sözleşme notları, örnek veriler.
-->

## 5. Acceptance Criteria

<!--
Her satır TEK BAŞINA test edilebilir olmalı. Given/When/Then ya da kesin davranış cümlesi.
Her AC, en az bir test veya görsel kanıtla eşleşir (bkz. docs/testing.md).
-->

- **AC-1:** 
- **AC-2:** 
- **AC-3:** 

## 6. Definition of Done

- [ ] Tüm AC'ler doğrulandı (test ve/veya görsel kanıt)
- [ ] Pipeline yeşil (lint + build + test + kritik akış smoke)
- [ ] Kod incelemesi onaylandı (Takım Yöneticisi)
- [ ] Davranış değiştiyse dokümanlar güncellendi
- [ ] Spec `specs/done/` klasörüne taşındı, görsel kanıtlar eklendi
- [ ] SCORECARD dolduruldu

## 7. SCORECARD

- **Spec revizyon sayısı:** 
- **Düzeltme turu sayısı:** 
- **Bulgu gerçek/gürültü oranı:** 
- **Regresyon sayısı:** 
- **Kaçan hata (escaped bug):** 
