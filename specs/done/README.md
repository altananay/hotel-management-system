# Tamamlanmış Spec'ler

Definition of Done'ı karşılanan spec dosyaları buraya taşınır.

- **Spec dosyası:** `specs/done/<####>-<english-name>.md`
- **Görsel kanıtlar:** `specs/done/evidence/<####>/AC-n-<english-name>.png`; çoklu uygulama/durum gerektiğinde `AC-n-<app>-<state>.png` (ör. `AC-11-web-loading.png`, `AC-11-admin-web-error.png`).
- **Video kanıtları:** `specs/done/evidence/<####>/AC-n-<english-name>.webm`; süre ≤ 15 sn, boyut ≤ 5 MB. İsimlendirme İngilizce kebab-case'dir.
- **İsimlendirme:** İngilizce kebab-case (bkz. `docs/conventions.md` §1); spec içerikleri Türkçe.
- Taşımadan önce SCORECARD alanı doldurulmuş olmalıdır (bkz. `specs/TEMPLATE.md`).

Kurallar:

- Bu klasör arşivdir; buradaki spec'ler sonradan **değiştirilmez**. Davranış değişikliği gerekirse yeni bir spec açılır ve bu spec'e atıf yapılır.
- Kanıt görselleri PR'da link olarak verilir (bkz. `docs/testing.md`).
