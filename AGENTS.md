# AGENTS.md — İşaret Levhası

> Bu dosya **kural içermez, işaret eder**. Kuralların tek kaynağı `docs/` klasörü ve `specs/TEMPLATE.md`'dir.
> Çelişki durumunda dokümanlar kazanır.

## Proje Özeti

Nevşehir/Kapadokya'daki tek bir otel için rezervasyon ve yönetim sistemi. Dört uygulama ve altyapı:

- **hotel-api** — Spring Boot modüler monolit. İş verisinin ve iş kurallarının **tek sahibi**: oda, fiyat, rezervasyon, ödeme, medya.
- **admin-api** — Spring Boot. Admin kimlik sağlayıcı (JWT), RBAC, oturum, audit (kendi DB'si: `hotel_admin`) ve hotel-api'ye **ince geçit**. İş kuralı içermez.
- **web** — Next.js public site (oda vitrini, rezervasyon, ödeme). Tailwind + shadcn; animasyon: Motion/GSAP/Lenis.
- **admin-web** — Next.js admin paneli (shadcn). **DB'siz, sırsız, saf arayüz.**
- **Altyapı** — PostgreSQL (2 DB: `hotel_core` + `hotel_admin`), Flyway, MinIO, Mailpit; Aşama 2: Redpanda + kafbat-ui.

Para birimleri: TRY (baz), EUR, USD — TCMB kurlarıyla otomatik türetme + manuel override. Diller: TR/EN (public), TR (admin).

## Altın Kurallar (7)

1. **Spec'siz iş yok.** Her feature `specs/<####>-<english-name>.md` spec'iyle başlar; kabul kriterleri yazılmadan kod yazılmaz. Branch: `feature/<spec-no>-<english-name>` (yalnız `releasable`'dan). İsimler İngilizce kebab-case; içerik Türkçe. → `docs/git.md`, `docs/conventions.md`
2. **Sınıra saygı.** Modüller ve uygulamalar birbirine yalnızca public API üzerinden erişir; tabloya, internal tipe, karşı tarafın veritabanına doğrudan erişim yasaktır. admin-web hiçbir DB/sırrı görmez; iş verisi yalnız hotel-api'de yaşar. → `docs/architecture.md` §Yasaklar
3. **Para ve yüzde her zaman BigDecimal.** `double`/`float` yasak. Tutar her yerde para birimini taşır; yuvarlama gizli varsayımla değil, açık kuralla uygulanır. → `docs/conventions.md`
4. **Öneri kuralı.** Soru, seçenek, bulgu veya açık konu getiren; kendi önerisini ve gerekçesini de getirir. Kararı Takım Yöneticisi verir; onaylanmamış öneri uygulanmaz.
5. **Git disiplini.** `main`/`master`, `integration`, `releasable` korumalıdır: doğrudan commit, force push, geçmiş silme yasak — geri alma `git revert` ile. Commit: Conventional Commits + `[plan ####/n]` atfı. PR: doldurulmuş şablon + yeşil pipeline; squash-merge. → `docs/git.md`
6. **Kanıtsız iş bitmez.** Her kabul kriteri en az bir testle (JUnit 5 + Mockito) ya da görsel kanıtla (ekran görüntüsü) eşleşir; kritik akışlar smoke test ile korunur. → `docs/testing.md`
7. **Tek gerçek kaynak.** Kural ve konvansiyonlar yalnızca `docs/` ve `specs/TEMPLATE.md`'de yaşar; kopyalanmaz, işaret edilir. Davranış değişirse önce doküman güncellenir.

## Neyi Nerede Bulursun

| Konu | Yer |
|---|---|
| Proje özeti, altın kurallar, çalışma disiplini | `AGENTS.md` |
| Kısayol (kural içermez) | `CLAUDE.md` |
| Mimari, modüller, iletişim kuralları, yasaklar, kapsam dışı, karar kaydı, portlar | `docs/architecture.md` |
| Terimler sözlüğü (ubiquitous language) + iş kuralları | `docs/domain.md` |
| Adlandırma, katmanlar, hata yönetimi, para ve DB konvansiyonları | `docs/conventions.md` |
| Branch, commit, PR ve merge kuralları | `docs/git.md` |
| Test stratejisi, kanıt ve CI beklentileri | `docs/testing.md` |
| Frontend konvansiyonları (web + admin-web) | `docs/frontend.md` |
| Spec şablonu | `specs/TEMPLATE.md` |
| Tamamlanmış spec'ler + görsel kanıtlar | `specs/done/` |

## Çalışma Disiplini

- **Roller:** *Takım Yöneticisi* karar verir ve onaylar (insan). Ajanlar ekip üyesi gibi çalışır: analist (spec yazar), geliştirici, gözden geçiren, test eden. Ajan commit'leri de aynı standarda uyar; mesajı yazan üye, onayı Takım Yöneticisi verir.
- **Başlamadan önce:** ilgili spec'i ve dokümanı oku. Spec kapsamı dışına taşan iş → yeni spec.
- **Küçük adımlar:** spec'teki adımlara bağlı, küçük commit'ler.
- **Bloke olunca:** dur ve sor — soru + öneri + gerekçe (Altın Kural 4).
- **Bitirince:** Definition of Done kontrolü → SCORECARD doldur → spec'i `specs/done/`e taşı.
- **Mimariye aykırı ihtiyaç:** kod yazma; önce `docs/architecture.md` için değişiklik önerisi getir.
