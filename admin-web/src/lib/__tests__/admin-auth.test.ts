import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/headers", () => ({
  cookies: vi.fn(),
}));

import { cookies } from "next/headers";
import { AdminAuthError, getAdminSession } from "../admin-auth";

const cookiesMock = vi.mocked(cookies);

function stubCookieHeader(value: string) {
  cookiesMock.mockResolvedValue({
    toString: () => value,
  } as Awaited<ReturnType<typeof cookies>>);
}

describe("getAdminSession (AC-6)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    stubCookieHeader("ADMIN_SESSION=session-token");
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shouldReturnSession_whenMeEndpointReturnsValidPayload", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          id: "0199-1",
          username: "superadmin",
          email: "superadmin@example.com",
          role: "SUPER_ADMIN",
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(getAdminSession()).resolves.toEqual({
      id: "0199-1",
      username: "superadmin",
      email: "superadmin@example.com",
      role: "SUPER_ADMIN",
    });
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8082/admin-api/auth/me",
      expect.objectContaining({
        headers: expect.objectContaining({
          Cookie: "ADMIN_SESSION=session-token",
        }),
        cache: "no-store",
      }),
    );
  });

  it("shouldReturnNull_whenSessionIsUnauthorized", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("", { status: 401 })));

    await expect(getAdminSession()).resolves.toBeNull();
  });

  it("shouldThrowAdminAuthError_whenMeEndpointFails", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("", { status: 503 })));

    await expect(getAdminSession()).rejects.toMatchObject({
      name: "AdminAuthError",
      status: 503,
    });
  });

  it("shouldThrowAdminAuthErrorWithZeroStatus_whenNetworkFails", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("connection refused")));

    await expect(getAdminSession()).rejects.toBeInstanceOf(AdminAuthError);
    await expect(getAdminSession()).rejects.toMatchObject({ status: 0 });
  });
});
