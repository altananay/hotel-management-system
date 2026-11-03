# AC-6 — Code Review

Reviewer: Code Reviewer (independent agent review, 2026-11-04)
Scope: `feature/0005-media` vs `origin/integration`, Spec 0005

0005 Medya ve Fotoğraf Yönetimi dilimi `feature/0005-media` üzerinde incelendi. B1–B7 kapsamındaki
stale entity merge, no-upscaling, arşiv/yayın invariant'ları, retry/status sözleşmesi, oda bazlı lock
ve deadlock→409 davranışı, DB lease/fencing ve presigned `Content-Length`/worker boyut koruması
doğrulandı. V9 migration yalnız `media` şemasına `processing_lease_id` ekliyor; `media → catalog`
public port sınırı, public/private depolama ayrımı ve rol kontrolleri korunuyor. Local doğrulama
173 unit + 56 IT (MediaIT 9/9), web 42 ve admin-web 32 test; `@media` E2E başarılı.

Kabul edilen riskler:
- Public JSON için kısa cache yerine `no-store` tercih edildi.
- Presigned TTL penceresinde aynı boyutlu private nesnenin değiştirilebilme olasılığı vardır.
- S3/DB publish-unpublish-archive işlemlerinde outbox/otomatik kompanzasyon yoktur; operasyonel retry'a bağlıdır.
- Medya audit'i spec gereği kapsam dışıdır; uygulama log/request-id ile izlenir.

Karar: **APPROVE**.
