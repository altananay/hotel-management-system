const ADMIN_ERROR_MESSAGES: Record<string, string> = {
  "security.unauthorized": "Bu işlem için oturum açmanız gerekiyor.",
  "security.invalid_credentials": "Kullanıcı adı veya parola hatalı.",
  "security.csrf_invalid":
    "Oturum doğrulaması yenilenemedi. Lütfen tekrar deneyin.",
  "system.network_error": "Sunucuya ulaşılamadı. Lütfen tekrar deneyin.",
  "system.internal_error":
    "Yönetim servisinde geçici bir sorun oluştu. Lütfen tekrar deneyin.",
  "system.not_found": "İstenen kayıt bulunamadı.",
};

const DEFAULT_ERROR_MESSAGE =
  "İşlem sırasında beklenmeyen bir hata oluştu. Lütfen tekrar deneyin.";

export function resolveAdminErrorMessage(code?: string): string {
  if (code && Object.hasOwn(ADMIN_ERROR_MESSAGES, code)) {
    return ADMIN_ERROR_MESSAGES[code];
  }
  return DEFAULT_ERROR_MESSAGE;
}
