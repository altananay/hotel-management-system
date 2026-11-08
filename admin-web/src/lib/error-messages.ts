const ADMIN_ERROR_MESSAGES: Record<string, string> = {
  "security.unauthorized": "Bu işlem için oturum açmanız gerekiyor.",
  "security.invalid_credentials": "Kullanıcı adı veya parola hatalı.",
  "security.csrf_invalid":
    "Oturum doğrulaması yenilenemedi. Lütfen tekrar deneyin.",
  "system.network_error": "Sunucuya ulaşılamadı. Lütfen tekrar deneyin.",
  "system.internal_error":
    "Yönetim servisinde geçici bir sorun oluştu. Lütfen tekrar deneyin.",
  "system.not_found": "İstenen kayıt bulunamadı.",
  "media.not_found": "Medya kaydı bulunamadı.",
  "media.invalid_content_type": "Yalnız JPEG, PNG veya WebP yükleyebilirsiniz.",
  "media.file_too_large": "Dosya en fazla 15 MB olabilir.",
  "media.image_too_large": "Görsel çözünürlüğü izin verilen sınırı aşıyor.",
  "media.invalid_image": "Dosya geçerli bir görsel değil.",
  "media.room_type_not_found": "Seçilen oda tipi aktif değil.",
  "media.not_ready": "Bu işlem için medya hazır değil.",
  "media.not_published": "Bu işlem için medya yayında olmalı.",
  "media.order_conflict": "Medya sırası çakıştı; listeyi yenileyip tekrar deneyin.",
  "media.processing_failed": "Görsel işlenemedi. Yeniden deneyin.",
  "media.alt_text_invalid": "Alt metin en fazla 200 düz metin karakteri olabilir.",
  "media.upload_expired": "Yükleme süresi doldu; dosyayı yeniden yükleyin.",
  "media.already_archived": "Bu medya zaten arşivlenmiş.",
  "media.cover_conflict": "Kapak seçimi çakıştı; listeyi yenileyip tekrar deneyin.",
  "media.storage_unavailable": "Medya deposuna şu anda ulaşılamıyor.",
  "pricing.invalid_period": "Tarih aralığı geçersiz.",
  "pricing.invalid_guests": "Misafir sayısı oda tipi kapasitesine uygun değil.",
  "pricing.invalid_currency": "Yalnız TRY, EUR veya USD kullanılabilir.",
  "pricing.invalid_amount": "Tutar sıfırdan büyük ve en fazla iki ondalıklı olmalıdır.",
  "pricing.invalid_buffer": "Buffer yüzdesi 0 ile 100 arasında olmalıdır.",
  "pricing.invalid_rounding_step": "Yuvarlama adımı sıfırdan büyük olmalıdır.",
  "pricing.invalid_fx_rate": "Kur sıfırdan büyük ve en fazla altı ondalıklı olmalıdır.",
  "pricing.room_type_not_found": "Seçilen oda tipi aktif değil.",
  "pricing.price_not_configured": "Bu oda tipi ve misafir sayısı için fiyat tanımlı değil.",
  "pricing.default_rate_plan_required": "Varsayılan aktif tarife planı bulunamadı.",
  "pricing.default_rate_plan_incomplete":
    "Varsayılan plan için aktif oda tiplerinin baz fiyatları eksik.",
  "pricing.rate_plan_not_found": "Tarife planı bulunamadı.",
  "pricing.rate_plan_duplicate": "Bu plan kodu zaten kullanılıyor.",
  "pricing.rate_plan_in_use": "Varsayılan plan pasifleştirilemez.",
  "pricing.season_not_found": "Sezon bulunamadı.",
  "pricing.season_duplicate": "Bu sezon kodu planda zaten kullanılıyor.",
  "pricing.season_overlap": "Sezon tarihleri aynı plandaki başka bir sezonla çakışıyor.",
  "pricing.manual_price_overlap": "Manuel fiyat aralığı başka bir manuel fiyatla çakışıyor.",
  "pricing.manual_price_not_found": "Manuel fiyat bulunamadı.",
  "pricing.manual_fx_duplicate": "Bu para birimi ve tarih için manuel kur zaten var.",
  "pricing.manual_fx_not_found": "Manuel kur bulunamadı.",
  "pricing.fx_rate_unavailable": "EUR/USD için kullanılabilir kur yok.",
};

const DEFAULT_ERROR_MESSAGE =
  "İşlem sırasında beklenmeyen bir hata oluştu. Lütfen tekrar deneyin.";

export function resolveAdminErrorMessage(code?: string): string {
  if (code && Object.hasOwn(ADMIN_ERROR_MESSAGES, code)) {
    return ADMIN_ERROR_MESSAGES[code];
  }
  return DEFAULT_ERROR_MESSAGE;
}
