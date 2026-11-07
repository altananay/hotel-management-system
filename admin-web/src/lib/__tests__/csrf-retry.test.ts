import { afterEach, describe, expect, it, vi } from "vitest";
import { AdminApiClientError } from "../api-client";
import { withCsrfRetry } from "../csrf-retry";

describe("withCsrfRetry (CR-2)", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    document.cookie = "XSRF-TOKEN=; Max-Age=0; path=/";
  });

  it("shouldRefreshTokenAndRetryOnce_whenCsrfIsInvalid", async () => {
    document.cookie = "XSRF-TOKEN=stale; path=/";
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation(() => {
        document.cookie = "XSRF-TOKEN=fresh; path=/";
        return Promise.resolve(
          new Response("{}", {
            status: 200,
            headers: { "Content-Type": "application/json" },
          }),
        );
      }),
    );
    const action = vi
      .fn()
      .mockRejectedValueOnce(
        new AdminApiClientError(403, "security.csrf_invalid"),
      )
      .mockResolvedValueOnce("ok");

    await expect(withCsrfRetry(action)).resolves.toBe("ok");

    expect(action).toHaveBeenNthCalledWith(1, "stale");
    expect(action).toHaveBeenNthCalledWith(2, "fresh");
  });

  it("shouldNotRetry_whenErrorIsNotCsrfInvalid", async () => {
    document.cookie = "XSRF-TOKEN=token; path=/";
    const action = vi
      .fn()
      .mockRejectedValueOnce(new AdminApiClientError(500, "system.internal_error"));

    await expect(withCsrfRetry(action)).rejects.toBeInstanceOf(
      AdminApiClientError,
    );
    expect(action).toHaveBeenCalledTimes(1);
  });
});
