# Test Stratejisi ve Kanıt Kuralları

> Bu dosya test ve kanıt kurallarının **tek kaynağıdır**.

## 1. İlke: Her Kriter Bir Test

- Spec'lerdeki her Acceptance Criteria (`AC-n`) **en az bir teste veya görsel kanıta** bağlanır.
- Test adı veya yorumu AC referansını içerir: ör. `shouldReturnRoomNotAvailable_whenRoomOccupied()` (AC-4).
- Bir AC test edilemiyorsa kriter yeterince kesin değildir → spec revize edilir.
- Kanıtsız tamamlanan iş yoktur (Altın Kural 6).

## 2. Backend: JUnit 5 + Mockito

- **Birim testler:** domain + uygulama servisleri. Dış bağımlılıklar (repository, port'lar — ör. `PaymentProvider`) mock'lanır; **domain nesneleri ve value object'ler asla mock'lanmaz.**
- **Adlandırma:** `should<BeklenenDavranış>_when<Kosul>()`; test sınıfı `<Sınıf>Test` (ör. `ReservationServiceTest`).
- **Entegrasyon testleri:** Spring Boot test + **Testcontainers** (PostgreSQL; Aşama 2'de Redpanda). Sınıf adı `<Konu>IT` (ör. `BookingFlowIT`).
- **Modül sınırları:** Spring Modulith doğrulama testi + ArchUnit kuralları zorunludur ve CI'da koşar; ihlal build'i kırar.
- **Olaylar:** publisher/tüketici testleri + idempotency testi (aynı event iki kez işlenir, sonuç değişmez).
- **Zaman/hold gibi zaman bağımlı akışlar:** enjekte edilen `Clock` üzerinden test edilir; gerçek bekleme yapılmaz.
- Framework kodu, getter/setter, konfigürasyon test edilmez; kapsam iş kuralına odaklanır.

## 3. Arayüz: Görsel Kanıt + Smoke

- Her mini-spec kriteri için ekran görüntüsü alınır: `specs/done/evidence/<####>/AC-n-<english-name>.png` (isimler İngilizce kebab-case; içerik Türkçe)
- **Kritik akışlara smoke test** yazılır (Playwright). Örnek zincir: "rezervasyon oluştur → fake ödeme → onay e-postası".
- Bileşen/birim testleri: **Vitest** (+ Testing Library); API mock'ları MSW ile.
- Her ekranda **yükleme / boş / hata** durumunun kanıtı zorunludur (bkz. `docs/frontend.md`).
- CI'da **lint + build** zorunludur; ayrıntılar `docs/frontend.md`.

## 4. Kanıtın Saklanması

- Görsel kanıtlar, spec `specs/done/`e taşınırken eklenir; PR'da link olarak verilir.
- Test çıktısı/raporu özeti PR açıklamasına eklenir.

## 5. SCORECARD

Her spec tamamlanınca `specs/TEMPLATE.md` §7'deki 5 satırlık SCORECARD doldurulur:

- Spec revizyon sayısı
- Düzeltme turu sayısı
- Bulgu gerçek/gürültü oranı
- Regresyon sayısı
- Kaçan hata (escaped bug)
