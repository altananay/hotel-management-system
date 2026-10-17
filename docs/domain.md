# Domain — Ubiquitous Language ve İş Kuralları

> Terimler kodda, dokümanda ve konuşmada **aynı anlamda** kullanılır. Yeni terim eklenirse buraya işlenir.

## 1. Sözlük

| Terim | Kod adı | Tanım |
|---|---|---|
| Otel Günü | `HotelDate` | İş günü Europe/Istanbul; "bugün" kavramının kaynağı |
| Konaklama Dönemi | `StayPeriod` | Check-in + check-out (`LocalDate`, çıkış günü hariç); gece sayısı taşır |
| Oda Tipi | `RoomType` | Satılabilir kategori (ör. Deluxe); kapasite ve olanaklar |
| Oda | `Room` | Fiziksel birim; tek oda tipine bağlı |
| Olanak | `Amenity` | Klima, jakuzi, manzara vb. |
| Oda Operasyonel Durumu | `RoomStatus` | `READY` / `DIRTY` / `OUT_OF_ORDER`; V1'de manuel yönetilir |
| Müsaitlik | Availability | Oda-gece bazında satılabilirlik; **otoritesi booking'dir** |
| Oda-Gece | `RoomNight` | Bir odanın bir gecesi; aynı oda+gece kombinezonu tekildir |
| Rezervasyon | `Reservation` | Bir odanın bir dönem için tutulması; statü makinesi §2'de |
| Tutma | Hold | Ödeme bekleyen rezervasyonun geçici kilidi (15 dk) |
| Rezervasyon Kodu | `ReservationCode` | Misafirin ödeme sonrası sorgulama anahtarı |
| Misafir | `Guest` | Rezervasyon kapsamındaki kişi bilgisi (hesap değildir) |
| Fiyat Teklifi | `Quote` | Dönem + oda tipi + misafir için hesaplanan fiyat; kur/buffer detayı içinde |
| Fiyat Anlık Görüntüsü | `PriceSnapshot` | Onaylanan fiyatın değişmez kaydı (kur, buffer, yuvarlama, kaynak) |
| Tarife Planı | `RatePlan` | Fiyat/iade koşulları seti |
| Sezon | `Season` | Tarife planı içindeki tarih aralığı |
| Promosyon Kodu | `PromoCode` | İndirim kodu + kullanım limiti |
| Baz Para Birimi | — | TRY (fiyatların girildiği para birimi) |
| Kur | `FxRate` | TCMB'den alınan USD/EUR kuru (satış) + kaynak zamanı |
| Buffer | `FxBuffer` | Otomatik çevrimde eklenen güvenlik payı (varsayılan %2) |
| Ödeme Niyeti | `PaymentIntent` | Sağlayıcı oturumu + tutar + para birimi |
| Ödeme Sağlayıcı | `PaymentProvider` | Port; `Fake` ve (sonra) `Iyzico` implementasyonları |
| Webhook | `ProviderWebhook` | Sağlayıcıdan gelen imzalı sonuç bildirimi |
| İade | `Refund` | Tahsil edilen tutarın iadesi (aynı para biriminde) |
| Mutabakat | `Reconciliation` | Sağlayıcı kayıtları ile iç kayıtların karşılaştırılması |
| Bildirim | `Notification` | E-posta gönderimi ve kaydı |
| Medya Varlığı | `MediaAsset` | Fotoğraf/panorama; işleme durumu (UPLOADED → PROCESSING → READY/FAILED) |
| Sahne | `Scene` | 360 turda bir panorama istasyonu; hotspot'ları taşır |
| Hotspot | `Hotspot` | Sahneler arası geçiş/etkileşim noktası |
| Tur | `Tour` | Sahnelerden oluşan gezinti; yayın durumu (draft/published) |
| Admin Oturumu | `AdminSession` | admin-api oturum kaydı (opak çerez) |
| Audit Kaydı | `AuditEntry` | "Kim, ne zaman, ne yaptı" |
| Rol | `AdminRole` | `SUPER_ADMIN` / `MANAGER` / `FRONT_DESK` |
| Projeksiyon | `Projection` | (V2) Event beslemeli okuma modeli (reporting) |

## 2. Durum Makineleri

### 2.1 Rezervasyon

```
PENDING_PAYMENT ──ödeme başarılı──► CONFIRMED ──giriş günü──► CHECKED_IN ──çıkış──► CHECKED_OUT
       │                                │
       ├──hold süresi doldu──► EXPIRED  ├──iptal──► CANCELLED
       └──ödeme başarısız──► CANCELLED └──gelmedi──► NO_SHOW
```

- `CONFIRMED` yalnızca `PaymentSucceeded` sonrası oluşur.
- `EXPIRED` yalnızca hold süresi dolduğunda ve ödeme alınmamışken oluşur.
- `CHECKED_IN` yalnızca `CONFIRMED` rezervasyona ve giriş gününde yapılır.

### 2.2 Ödeme

```
INITIATED ─► PENDING ─► SUCCEEDED | FAILED | EXPIRED
SUCCEEDED ─► REFUND_PENDING ─► REFUNDED          (V1: tam iade)
```

Webhook gecikirse veya kaybolursa günlük mutabakat işi durumu düzeltir.

### 2.3 Oda Operasyonel Durumu (V1 manuel)

```
READY ⇄ DIRTY ;  OUT_OF_ORDER (müsaitlikte bloklu; elle READY'ye alınır)
```

## 3. İş Kuralları

1. **Müsaitlik otoritesi booking'dir.** Oda-gece ataması tekil olur; hiçbir koşulda oversell (çift satış) yapılmaz.
2. **Hold 15 dakikadır** (konfigüre edilebilir). Süre dolunca rezervasyon `EXPIRED`, ödeme niyeti iptal edilir.
3. **1 rezervasyon = 1 oda** (V1). Çok odalı/grup rezervasyonu kapsam dışıdır.
4. **Fiyat snapshot'ı değişmez.** Onaylanan fiyat; kur, buffer ve yuvarlama bilgisiyle birlikte saklanır.
5. **Çoklu para birimi:** Gösterim para birimi = tahsilat para birimi. Otomatik çevrim TCMB satış kuruyla; öncelik **elle fiyat > elle kur > TCMB**. TCMB erişilemezse son bilinen kur kullanılır + admin ekranında uyarı gösterilir; akış bloke edilmez. Ödeme anında çevrim yoktur; iade aynı para biriminde yapılır.
6. **Varsayılan kur parametreleri:** buffer %2, yuvarlama en yakın 5'e yukarı. İkisi de admin tarafından yapılandırılır.
7. **İptal/iade (V1):** Girişten **X gün** öncesine kadar tam iade; sonrasında iade yok. X varsayılan **7**, admin yapılandırır. Politika motoru V2'dir.
8. **Müşteri hesabı yoktur.** Rezervasyon sorgulama: rezervasyon kodu + e-posta.
9. **Bildirimler e-posta ile** yapılır; voucher/onay özeti e-posta içeriğidir (PDF V2).
10. **`OUT_OF_ORDER` odalar** müsaitlik hesabında satılamaz kabul edilir; blok çakışması yaşanırsa admin rezervasyonu elle taşır (V1).
11. **Check-in** yalnız `CONFIRMED` rezervasyona, **check-out** yalnız `CHECKED_IN` rezervasyona uygulanır; ikisi de admin işlemi.
12. **Tur yalnız yayınlandığında (`TourPublished`) public'te görünür.**
13. **Fiyatlar KDV dahildir** (V1: tek tutar). Ayrıştırılmış vergi/fatura hesabı V2'dir.
14. **Dil ve para birimi bağımsızdır** (EN + TRY geçerli bir kombinasyondur). Fiyat gösterimi seçilen para biriminde yapılır.
15. **Roller (kaba):** aşağıdaki tablo. Yetki zorlaması her zaman **hotel-api/admin-api tarafındadır**; arayüz gizlemesi güvenlik sayılmaz.

| İşlev | SUPER_ADMIN | MANAGER | FRONT_DESK |
|---|---|---|---|
| Admin kullanıcı/rol yönetimi | ✔ | — | — |
| Fiyat, tarife, promosyon yönetimi | ✔ | ✔ | — |
| Medya/tur yönetimi | ✔ | ✔ | — |
| Rezervasyon görüntüleme | ✔ | ✔ | ✔ |
| Rezervasyon iptal/iade | ✔ | ✔ | — |
| Check-in / check-out | ✔ | ✔ | ✔ |
| Oda operasyonel durumu | ✔ | ✔ | ✔ |
| Audit görüntüleme | ✔ | ✔ | — |
