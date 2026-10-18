# Git — Branch, Commit, PR ve Merge Kuralları

> Bu dosya git akışının **tek kaynağıdır**. İstisnasız herkes (insan veya ajan) bu kurallara uyar.

## 1. Branch Modeli

- **Korumalı branch'ler:** `main`/`master`, `integration`, `releasable`.
  - **`integration` = TEST ortamının branchidir.**
  - **`releasable` = PROD ortamının branchidir.**
  - `main`/`master` = yalnızca korumalı başlangıç noktasıdır; geliştirme yapılmaz.
- İlk kurulumda `master`'dan boş `integration` ve `releasable` açılır; üçü de korumaya alınır.
- **Her geliştirme, `releasable` üzerinden açılan `feature/<spec-no>-<english-name>` branch'inde yapılır.**
  - İsimler İngilizce + kebab-case'dir (spec dosya adıyla aynı); içerik Türkçe'dir.
  - Örnek: `feature/0007-room-listing`
- **SPEC'SİZ BRANCH AÇILMAZ.** Branch adındaki numara, `specs/` altındaki spec numarasıdır.
- **Bootstrap istisnası (tek seferlik, 2026-10-18):** Depo ilk kurulumunda dokümantasyon commit'leri (3 adet) `master` üzerine doğrudan yapıldı; spec atıfı taşımazlar. `integration` ve `releasable` açıldıktan sonraki tüm değişiklikler §2'deki akışa tabidir.

## 2. Akış

```
feature/<spec-no>-<english-name>  ──PR (squash)──►  integration (TEST)  ──doğrulama──►  releasable (PROD)
        ▲                                                                          │
        └──────────────────────── yeni feature, releasable'dan açılır ────────────┘
```

- Acil düzeltmeler dahil **tüm** değişiklikler aynı akışı izler; küçük kapsamlı da olsa spec zorunludur.

## 3. Commit Formatı

```
<type>(<scope>): <short description> [plan ####/n]
```

- **Dil:** Commit mesajı **her zaman İngilizce**dir; açıklama emir kipiyle yazılır (`add`, `fix`, `remove`…); doküman/spec içeriklerinin dili Türkçe kalır.
- **Tip:** `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `build`, `ci`, `chore`
- **Kapsam:** modül veya uygulama (`catalog`, `booking`, `payment`, `web`, `admin-api`, `admin-web`, `infra`, `docs`…)
- **Plan atfı:** `[plan 0001/3]` → spec numarası `0001`, spec içindeki adım `3`. Tek commit tüm spec'i kapsıyorsa `[plan 0001]`.
- Örnekler:
  - `feat(catalog): add pagination endpoint [plan 0001/3]`
  - `fix(payment): verify webhook signature [plan 0012/1]`
- **Ajan/üye kuralı:** commit mesajını işi yapan üye (ajan) yazar; **onayı Takım Yöneticisi verir.** Ajan commit'leri de aynı standarda uyar.

## 4. Yasaklar

- `main`/`master`/`integration`/`releasable` branch'lerine **doğrudan commit** (her rol için).
- **Force push** (`--force` / `--force-with-lease`) — hiçbir branch'te.
- **Geçmiş silme** (yayımlanmış geçmişi rebase/amend ile değiştirmek). Geri alma zorunlu olarak **`git revert`** ile yapılır.
- Spec'siz branch, commit veya PR.
- Repoya sır (parola, anahtar, token) commit etmek (yerel, açıkça işaretli sabit varsayılanlar sır sayılmaz — tanım: `docs/conventions.md` §2.4).

## 5. PR Kuralları

1. **Şablon doldurulmuş olmalı:** bağlı spec, özet, kabul kriteri kontrol listesi, test/kanıt, riskler.
2. **Pipeline yeşil olmalı:** lint + build + testler (+ kritik akış smoke).
3. **En az bir onay:** Takım Yöneticisi (insan). Ajan gözden geçirme yorumları öneri niteliğindedir (Altın Kural 4).
4. **Squash-merge** ile `integration`'a birleştirilir; birleşen commit mesajı **PR başlığıdır**; PR başlığı da Conventional formatına ve İngilizce kuralına (§3) uyar.

PR şablonu taslağı (`.github/pull_request_template.md` olarak ilk dilimde oluşturulacak):

```markdown
## Bağlı Spec
[plan ####/n] — <spec başlığı ve linki>

## Özet
<Ne değişti, neden?>

## Kabul Kriterleri
- [ ] AC-1 ...
- [ ] AC-2 ...

## Test / Kanıt
- Testler: ...
- Görsel kanıt: specs/done/evidence/<####>/...

## Riskler / Notlar
```

## 6. Bootstrap ve İlk Dilim Kurulumu

- **Bootstrap (yapıldı — 2026-10-18):** `integration` ve `releasable`, `master`'dan açıldı ve origin'e gönderildi. **GitHub koruması yönetici tarafından arayüzden uygulanır** (öneri: PR zorunlu + en az 1 onay; force push ve branch silme kapalı; "Include administrators" kapalı). CI eklendiğinde "required status checks" de bağlanır.
- **İlk dilimde kurulacaklar:** `.github/pull_request_template.md` dosyasının oluşturulması; CI pipeline: lint + build + test + smoke (detay: `docs/testing.md`, `docs/frontend.md`).
