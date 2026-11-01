import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const push = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push, refresh: vi.fn() }),
}));

vi.mock("@/lib/csrf", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/csrf")>();
  return { ...actual, fetchCsrfToken: vi.fn() };
});

vi.mock("@/lib/api-client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api-client")>();
  return { ...actual, adminApiPost: vi.fn() };
});

import { LogoutButton } from "@/components/auth/logout-button";
import { AdminApiClientError, adminApiPost } from "@/lib/api-client";
import { fetchCsrfToken } from "@/lib/csrf";

const adminApiPostMock = vi.mocked(adminApiPost);
const fetchCsrfTokenMock = vi.mocked(fetchCsrfToken);

function clearCsrfCookie() {
  document.cookie = "XSRF-TOKEN=; Max-Age=0; path=/";
}

describe("LogoutButton (AC-6)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    clearCsrfCookie();
  });

  it("shouldLogoutWithSessionBoundToken_whenCsrfCookieExists", async () => {
    document.cookie = "XSRF-TOKEN=session-bound-token; path=/";
    adminApiPostMock.mockResolvedValue(undefined);

    render(<LogoutButton />);
    fireEvent.click(screen.getByTestId("admin-logout"));

    await waitFor(() => expect(push).toHaveBeenCalledWith("/login"));
    expect(adminApiPostMock).toHaveBeenCalledWith(
      "/admin-api/auth/logout",
      {},
      "session-bound-token",
    );
    expect(fetchCsrfTokenMock).not.toHaveBeenCalled();
  });

  it("shouldFetchCsrfToken_whenCookieIsMissing", async () => {
    adminApiPostMock.mockResolvedValue(undefined);
    fetchCsrfTokenMock.mockResolvedValueOnce("fetched-token");

    render(<LogoutButton />);
    fireEvent.click(screen.getByTestId("admin-logout"));

    await waitFor(() => expect(push).toHaveBeenCalledWith("/login"));
    expect(adminApiPostMock).toHaveBeenCalledWith(
      "/admin-api/auth/logout",
      {},
      "fetched-token",
    );
  });

  it("shouldRefreshTokenAndRetryOnce_whenCsrfIsRejected", async () => {
    document.cookie = "XSRF-TOKEN=stale-token; path=/";
    adminApiPostMock
      .mockRejectedValueOnce(new AdminApiClientError(403, "security.csrf_invalid"))
      .mockResolvedValueOnce(undefined);
    fetchCsrfTokenMock.mockResolvedValueOnce("fresh-token");

    render(<LogoutButton />);
    fireEvent.click(screen.getByTestId("admin-logout"));

    await waitFor(() => expect(push).toHaveBeenCalledWith("/login"));
    expect(adminApiPostMock).toHaveBeenCalledTimes(2);
    expect(adminApiPostMock).toHaveBeenLastCalledWith(
      "/admin-api/auth/logout",
      {},
      "fresh-token",
    );
  });

  it("shouldRedirectToLogin_whenSessionIsUnauthorized", async () => {
    document.cookie = "XSRF-TOKEN=session-token; path=/";
    adminApiPostMock.mockRejectedValueOnce(
      new AdminApiClientError(401, "security.unauthorized"),
    );

    render(<LogoutButton />);
    fireEvent.click(screen.getByTestId("admin-logout"));

    await waitFor(() => expect(push).toHaveBeenCalledWith("/login"));
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("shouldShowError_whenLogoutFails", async () => {
    document.cookie = "XSRF-TOKEN=session-token; path=/";
    adminApiPostMock.mockRejectedValueOnce(
      new AdminApiClientError(500, "system.internal_error"),
    );

    render(<LogoutButton />);
    fireEvent.click(screen.getByTestId("admin-logout"));

    await waitFor(() =>
      expect(screen.getByRole("alert")).toHaveTextContent("Çıkış yapılamadı."),
    );
    expect(push).not.toHaveBeenCalled();
  });
});
