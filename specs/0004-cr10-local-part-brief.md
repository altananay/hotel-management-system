# Spec 0004 — CR-10 Analist Brief'i (Kısa E-posta Local-part İstisnası)

> **Amaç:** Code review CR-10 bulgusunu kapatmak için `specs/0004-admin-identity.md`
> §2.2.1 ve AC-1 metninde e-posta local-part kontrolünün **1–2 karakterlik local-part
> istisnasını** açıkça yazmak.
>
> **Kapsam:** Yalnız doküman/spec düzeltmesi. Kod davranışı zaten bu istisnayı
> uyguluyor; bu brief kod değişikliği veya yeni bir güvenlik kuralı istemez.
>
> **Durum:** Analist işlemi bekliyor; spec revizyonu Takım Yöneticisi onayına sunulacak.
> **Tarih:** 2026-11-01
> **İlgili bulgu:** CR-10 — `specs/done/evidence/0004/code-review-brief-2026-11-01.md`
> **İlgili kod:** `backend/admin-api/src/main/java/com/hotel/adminidentity/internal/domain/PasswordPolicy.java`
> (`MIN_LOCAL_PART_LENGTH_FOR_CHECK = 3`)
> **İlgili test:** `backend/admin-api/src/test/java/com/hotel/adminidentity/PasswordPolicyTest.java`

## 1. Bulgu ve mevcut durum

Code review bulgusu:

> Parola politikası kısa e-posta local-part'ını atlıyor. E-posta local-part'ı yalnız
> `localPart.length() >= 3` ise kontrol ediliyor. Spec parolanın e-posta local-part'ını
> (uzunluk istisnası olmadan) içeremeyeceğini söylüyor. `a@example.com` gibi kısa
> local-part'lı hesaplarda parola local-part'ı içerebilir.

Kodda uygulanan davranış:

- Tam e-posta adresi her zaman kontrol edilir.
- Kullanıcı adı her zaman kontrol edilir.
- E-posta local-part'ı yalnız **3 veya daha fazla** karakterse substring olarak aranır.
- 1–2 karakterli local-part'lar substring kontrolünden muaftır.

Bu istisna kodda açıklama ve sabit ile belgelenmiştir, ancak spec §2.2.1 ve AC-1
metninde yazmadığı için kod/spec uyumsuzluğu görünür. Çözüm, davranışı değiştirmek
değil, spec'i kodla aynı hale getirmektir.

## 2. Neden eşik korunuyor?

1–2 karakterli bir local-part'ı substring olarak aramak neredeyse tüm parolaları
reddeder. Örnek: `a@example.com` için `a` harfini içeren her parola reddedilir; bu
da 12–128 karakterlik parola politikasını pratikte kullanılamaz hale getirir.
Buna karşılık:

- Tam e-posta adresi (`a@example.com`) her zaman yasaktır.
- Kullanıcı adı her zaman yasaktır.
- 3+ karakterli local-part (`abc@example.com` → `abc`) her zaman yasaktır.

Kabul edilen risk: 1–2 karakterli local-part'lı bir hesap, parolasında o karakteri
kullanabilir. Bu, parola uzunluğu ve diğer kontroller nedeniyle düşük risklidir;
kullanılabilirlik açısından bilinçli tercihtir.

## 3. Analistten istenen spec değişiklikleri

Yalnız `specs/0004-admin-identity.md` içinde aşağıdaki değişiklikler yapılmalıdır.
Başka spec, doküman veya kod değişikliği bu brief kapsamında değildir.

### 3.1 §0 Künye

- **Durum:** `Onaylandı` kalır; revizyon numarası **rev.8** olur.
- **Tarih:** Son revizyon `2026-11-01, rev.8`.
- **İlgili doküman/kararlar:** Mevcut revizyon notuna ekle:
  `rev.8: CR-10 — e-posta local-part kontrolünün 1–2 karakterlik local-part istisnası açıkça yazıldı; davranış değişmedi.`

### 3.2 §2.2.1 Kimlik ve parola — Parola maddesi

Mevcut cümlenin sonuna aşağıdaki açıklamayı ekle (veya cümleyi bu şekilde netleştir):

> Parola; kullanıcı adını veya e-postayı (case-insensitive, tam veya local-part)
> içeremez. E-posta local-part'ı için substring kontrolü yalnız local-part uzunluğu
> **3 veya daha fazla** ise uygulanır; 1–2 karakterli local-part'lar pratikte tüm
> parolalarla çakışacağı için bu kontrolden muaftır. Tam e-posta adresi ve kullanıcı
> adı her uzunlukta kontrol edilir.

### 3.3 AC-1 — Parola politikası maddesi

Mevcut AC-1 parola cümlesini aşağıdaki şekilde genişlet:

> Parola politikası: 12–128 karakter, kompozisyon kuralı yok, kullanıcı adı/e-posta
> içeremez. Kısa/uzun/aynı kullanıcı adını içeren parolalar reddedilir; hata generic
> ve kullanıcı numaralandırması yapmaz. E-posta local-part kontrolü 3+ karakterli
> local-part'larda uygulanır; **1–2 karakterli local-part istisnası** testle
> doğrulanır (tam e-posta ve kullanıcı adı kontrolü her uzunlukta korunur).

### 3.4 AC-1 — Test beklentisi

AC-1 test listesine aşağıdaki senaryoları ekle (isimler öneridir; geliştirici
mevcut `PasswordPolicyTest` yapısına uyarlayabilir):

1. `shouldAcceptPasswordContainingOneCharacterLocalPart_whenFullEmailAndUsernameAreAbsent`
   - Kullanıcı: `a@example.com`, kullanıcı adı: `shortmail`.
   - Parola 12–128 karakter, `a` içeriyor ama `a@example.com` ve `shortmail` içermiyor.
   - Beklenen: kabul.
2. `shouldRejectPasswordContainingThreeCharacterLocalPart`
   - Kullanıcı: `abc@example.com`.
   - Parola `abc` içeriyor.
   - Beklenen: `InvalidPasswordException`.
3. `shouldRejectPasswordContainingFullShortEmail`
   - Kullanıcı: `a@example.com`.
   - Parola tam `a@example.com` içeriyor.
   - Beklenen: `InvalidPasswordException`.

Mevcut `shouldRejectPasswordContainingEmailLocalPart_whenLongEnough` testi
korunmalıdır.

### 3.5 Kanıt / izlenebilirlik

Spec revizyonu sonrası `specs/done/evidence/0004/code-review-fixes-2026-11-01.md`
içindeki CR-10 satırına spec rev.8 referansı eklenebilir; bu, ayrı bir evidence
güncellemesidir ve analist brief'inin zorunlu parçası değildir.

## 4. Yapılmayacaklar

- `MIN_LOCAL_PART_LENGTH_FOR_CHECK` eşiğini değiştirme.
- 1–2 karakterli local-part için yeni bir substring kontrolü ekleme.
- 12–128 code point, NFC, 512 UTF-8 byte veya kompozisyon kuralı değişiklikleri.
- Tam e-posta/kullanıcı adı kontrollerini gevşetme.
- Yeni bir spec numarası açma; bu, 0004'ün revizyonudur.
- Kod, migration veya CI değişikliği yapma (davranış zaten mevcut).

## 5. Kabul kriterleri (spec düzeltmesi için)

- [ ] §2.2.1'de 1–2 karakterli local-part istisnası açıkça yazıldı.
- [ ] AC-1 parola maddesi aynı istisnayı ve test beklentisini içeriyor.
- [ ] §0 künyesinde rev.8 ve tarih güncellendi.
- [ ] Mevcut 12–128, NFC, tam e-posta ve kullanıcı adı kuralları değişmedi.
- [ ] Spec onayı Takım Yöneticisi'ne sunuldu.

## 6. Karar / onay

Bu brief, CR-10 bulgusunun **kod davranışını değiştirmeden** spec'e yansıtılmasını
önerir. Spec revizyonu Takım Yöneticisi tarafından onaylandıktan sonra geliştirici
yalnız AC-1'deki test beklentisini `PasswordPolicyTest`'e ekler; üretim kodu
değişmez.
