# AC-5 audit retention

- Hedef: audit kayıtları **90 gün** saklanır.
- Bu dilimde otomatik silme/arşivleme **yoktur**; fiziksel temizlik operasyonel
  runbook'a bağlıdır (spec §2.2.8).
- `admin_audit` tablosunda `actor_user_id` FK değildir; modüller arası FK yasaktır.
- Kayıt alanları: `occurred_at`, `event_type`, `actor_user_id`, `username_attempted`,
  `result`, `reason_code`, `ip_address`, `user_agent` (≤512), `request_id`, `metadata`.
- Parola, oturum kimliği, JWT veya istek gövdesi yazılmaz.
