import { afterEach, describe, expect, it, vi } from "vitest";
import { fetchCsrfToken, readCookie, readCsrfToken } from "../csrf";

describe("csrf cookie helpers", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    document.cookie = "XSRF-TOKEN=; Max-Age=0; path=/";
  });

  it("shouldReadUrlEncodedCookieValue", () => {
    document.cookie = "XSRF-TOKEN=abc%2D123; path=/";
    expect(readCookie("XSRF-TOKEN")).toBe("abc-123");
    expect(readCsrfToken()).toBe("abc-123");
  });

  it("shouldFetchTokenAndReadCookie_whenCsrfEndpointSucceeds", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response("{}", {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );
    document.cookie = "XSRF-TOKEN=cookie-token; path=/";

    await expect(fetchCsrfToken()).resolves.toBe("cookie-token");
  });

  it("shouldReject_whenCsrfEndpointFails", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("", { status: 500 })));

    await expect(fetchCsrfToken()).rejects.toThrow("csrf_fetch_failed");
  });
});
