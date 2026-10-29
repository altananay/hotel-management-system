export function safeReturnTo(value: string | null | undefined): string {
  if (!value || !value.startsWith("/") || value.startsWith("//")) {
    return "/";
  }
  if (value.includes("://") || value.includes("\\")) {
    return "/";
  }
  return value;
}
