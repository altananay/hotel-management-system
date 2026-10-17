# Roller

Bu klasör, oturum rollerini tanımlar. Her rol kartı aynı iskeleti izler:
Kimlik → Sahiplendiği omurga rozetleri → Okuması gerekenler → Yetkiler ve YASAKLAR → Çıktı formatı → Takım Yöneticisine sorduğu anlar → Rolün AP'leri → İmza ilkesi.

## 1. Bir Oturuma Rol Nasıl Verilir

Oturum açılışında **tek satır** yeterlidir:

`Rol: <rol> — docs/roles/<rol>.md'ye göre çalışıyorum.`

Örnekler:

- `Rol: analyst — docs/roles/analyst.md'ye göre çalışıyorum.`
- `Rol: developer — docs/roles/developer.md'ye göre çalışıyorum.`
- `Rol: qa — docs/roles/qa.md'ye göre çalışıyorum.`
- `Rol: code-reviewer — docs/roles/code-reviewer.md'ye göre çalışıyorum.`

Bir oturumda tek rol; rol değişimi yeni bir oturumda yapılır (bağlam karışmasın).

| Rol | Kart | Omurga rozetleri |
|---|---|---|
| Analist | `analyst.md` | INTENT · CLARIFY · SPEC |
| Developer | `developer.md` | PLAN · BUILD · düzeltme |
| QA | `qa.md` | REVIEW · TEST · VERIFY · repro |
| Code Reviewer | `code-reviewer.md` | REVIEW / VERIFY / BUILD |

## 2. Ortak Kurallar (tüm roller)

1. **AGENTS.md okunur.** Rol, kendi kartı olmadan işe başlamaz.
2. **Kanıtsız iddia yok** (test çıktısı, diff, komut çıktısı, ekran görüntüsü).
3. **Belirsizlikte VARSAYMA:** AP-10 uygulanır — seçenekleri bedelleriyle getir, yöneticiye sor (bkz. `docs/ap.md`).
4. **ÖNERİ KURALI:** soru/bulgu/seçenek önerisiz sunulmaz — öneri + gerekçe; karar Takım Yöneticisinde.
5. **Rol sınırı mutlaktır:** karttaki yasaklar delinemez; başka rolün işi yapılmaz (ör. QA kod yazmaz, Code Reviewer test yapmaz).
