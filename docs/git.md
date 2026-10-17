# Git — Branch, Commit, PR ve Merge Kuralları

> Bu dosya git akışının **tek kaynağıdır**. İstisnasız herkes (insan veya ajan) bu kurallara uyar.

## 1. Branch Modeli

- **Korumalı branch'ler:** `main`/`master`, `integration`, `releasable`.
  - **`integration` = TEST ortamının branchidir.**
  - **`releasable` = PROD ortamının branchidir.**
  - `main`/`master` = yalnızca korumalı başlangıç noktasıdır; geliştirme yapılmaz.
- İlk kurulumda `master`'dan boş `integration` ve `releasable` açılır; üçü de korumaya alınır.
- **Her geliştirme, `releasable` üzerinden açılan `feature/<spec-no>-<kısa-ad>` branch'inde yapılır.**
  - Örnek: `feature/0007-oda-listeleme`
- **SPEC'SİZ BRANCH AÇILMAZ.** Branch adındaki numara, `specs/` altındaki spec numarasıdır.

## 2. Akış

```
feature/<spec-no>-<ad>  ──PR (squash)──►  integration (TEST)  ──doğrulama──►  releasable (PROD)
        ▲                                                                          │
        └──────────────────────── yeni feature, releasable'dan açılır ────────────┘
```

- Acil düzeltmeler dahil **tüm** değişiklikler aynı akışı izler; küçük kapsamlı da olsa spec zorunludur.

## 3. Commit Formatı

```
<tip>(<kapsam>): <kısa açıklama> [plan ####/n]
```

- **Tip:** `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `build`, `ci`, `chore`
- **Kapsam:** modül veya uygulama (`catalog`, `booking`, `payment`, `web`, `admin-api`, `admin-web`, `infra`, `docs`…)
- **Plan atfı:** `[plan 0001/3]` → spec numarası `0001`, spec içindeki adım `3`. Tek commit tüm spec'i kapsıyorsa `[plan 0001]`.
- Örnekler:
  - `feat(catalog): sayfalama endpoint'i [plan 0001/3]`
  - `fix(payment): webhook imza doğrulaması [plan 0012/1]`
- **Ajan/üye kuralı:** commit mesajını işi yapan üye (ajan) yazar; **onayı Takım Yöneticisi verir.** Ajan commit'leri de aynı standarda uyar.

## 4. Yasaklar

- `main`/`master`/`integration`/`releasable` branch'lerine **doğrudan commit** (her rol için).
- **Force push** (`--force` / `--force-with-lease`) — hiçbir branch'te.
- **Geçmiş silme** (yayımlanmış geçmişi rebase/amend ile değiştirmek). Geri alma zorunlu olarak **`git revert`** ile yapılır.
- Spec'siz branch, commit veya PR.
- Repoya sır (parola, anahtar, token) commit etmek.

## 5. PR Kuralları

1. **Şablon doldurulmuş olmalı:** bağlı spec, özet, kabul kriteri kontrol listesi, test/kanıt, riskler.
2. **Pipeline yeşil olmalı:** lint + build + testler (+ kritik akış smoke).
3. **En az bir onay:** Takım Yöneticisi (insan). Ajan gözden geçirme yorumları öneri niteliğindedir (Altın Kural 4).
4. **Squash-merge** ile `integration`'a birleştirilir; birleşen commit mesajı **PR başlığıdır** ve PR başlığı Conventional formatına uyar.

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

## 6. İlk Dilimde Kurulacaklar

- `integration` ve `releasable` branch'lerinin açılması + üç branch'e protection kuralı.
- `.github/pull_request_template.md` dosyasının oluşturulması.
- CI pipeline: lint + build + test + smoke (detay: `docs/testing.md`, `docs/frontend.md`).
