const HAS_SCHEME = /^[a-z][a-z0-9+.-]*:/i;
const RESIDUAL_ENCODED_SEPARATOR = /%2f|%5c/i;

export function safeReturnTo(value: string | null | undefined): string {
  if (!value) {
    return "/";
  }
  let decoded: string;
  try {
    decoded = decodeURIComponent(value);
  } catch {
    return "/";
  }
  if (!decoded.startsWith("/") || decoded.startsWith("//")) {
    return "/";
  }
  if (decoded.includes("\\") || HAS_SCHEME.test(decoded)) {
    return "/";
  }
  if (RESIDUAL_ENCODED_SEPARATOR.test(decoded)) {
    return "/";
  }
  if (/[\u0000-\u001f\u007f]/.test(decoded)) {
    return "/";
  }
  return decoded;
}
