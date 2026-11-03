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
};

const DEFAULT_ERROR_MESSAGE =
  "İşlem sırasında beklenmeyen bir hata oluştu. Lütfen tekrar deneyin.";

export function resolveAdminErrorMessage(code?: string): string {
  if (code && Object.hasOwn(ADMIN_ERROR_MESSAGES, code)) {
    return ADMIN_ERROR_MESSAGES[code];
  }
  return DEFAULT_ERROR_MESSAGE;
}
