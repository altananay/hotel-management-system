const CSRF_COOKIE = "XSRF-TOKEN";

export function readCookie(name: string): string | undefined {
  if (typeof document === "undefined") {
    return undefined;
  }
  const prefix = `${name}=`;
  for (const part of document.cookie.split(";")) {
    const value = part.trim();
    if (value.startsWith(prefix)) {
      return decodeURIComponent(value.slice(prefix.length));
    }
  }
  return undefined;
}

export function readCsrfToken(): string | undefined {
  return readCookie(CSRF_COOKIE);
}

export async function fetchCsrfToken(): Promise<string> {
  const response = await fetch("/admin-api/auth/csrf", {
    method: "GET",
    credentials: "same-origin",
    cache: "no-store",
  });
  if (!response.ok) {
    throw new Error("csrf_fetch_failed");
  }
  const token = readCsrfToken();
  if (!token) {
    throw new Error("csrf_cookie_missing");
  }
  return token;
}
