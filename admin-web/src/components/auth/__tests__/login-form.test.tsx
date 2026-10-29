import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const push = vi.fn();
const get = vi.fn().mockReturnValue("/");

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push, refresh: vi.fn() }),
  useSearchParams: () => ({ get }),
}));

vi.mock("@/lib/csrf", () => ({
  fetchCsrfToken: vi.fn(),
}));

vi.mock("@/lib/api-client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api-client")>();
  return { ...actual, adminApiPost: vi.fn() };
});

import { LoginForm } from "@/components/auth/login-form";
import { adminApiPost, AdminApiClientError } from "@/lib/api-client";
import { fetchCsrfToken } from "@/lib/csrf";

const fetchCsrfTokenMock = vi.mocked(fetchCsrfToken);
const adminApiPostMock = vi.mocked(adminApiPost);

async function renderReadyForm() {
  fetchCsrfTokenMock.mockResolvedValue("csrf-token");
  render(<LoginForm />);
  await waitFor(() => expect(screen.getByTestId("admin-login-form")).toBeInTheDocument());
}

describe("LoginForm (AC-6)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    get.mockReturnValue("/");
  });

  it("shouldShowLoadingThenLoginForm_whenCsrfIsFetched", async () => {
    let resolve!: (value: string) => void;
    fetchCsrfTokenMock.mockReturnValue(new Promise((done) => (resolve = done)));
    render(<LoginForm />);
    expect(screen.getByTestId("admin-login-loading")).toBeInTheDocument();
    resolve("csrf-token");
    await waitFor(() => expect(screen.getByTestId("admin-login-form")).toBeInTheDocument());
  });

  it("shouldShowRetry_whenCsrfFetchFails", async () => {
    fetchCsrfTokenMock.mockRejectedValueOnce(new Error("down"));
    render(<LoginForm />);
    await waitFor(() => expect(screen.getByTestId("admin-login-error")).toBeInTheDocument());
    expect(screen.getByTestId("admin-login-retry")).toBeInTheDocument();
  });

  it("shouldShowGenericError_whenCredentialsAreInvalid", async () => {
    await renderReadyForm();
    adminApiPostMock.mockRejectedValue(
      new AdminApiClientError(401, "security.invalid_credentials"),
    );
    fireEvent.change(screen.getByTestId("admin-login-identifier"), {
      target: { value: "superadmin" },
    });
    fireEvent.change(screen.getByTestId("admin-login-password"), {
      target: { value: "wrong" },
    });
    fireEvent.click(screen.getByTestId("admin-login-submit"));
    await waitFor(() =>
      expect(screen.getByTestId("admin-login-error")).toHaveTextContent(
        "Kullanıcı adı veya parola hatalı.",
      ),
    );
    expect(screen.queryByText(/wrong/i)).not.toBeInTheDocument();
  });

  it("shouldRetryOnceWithFreshCsrfToken_whenCsrfIsRejected", async () => {
    await renderReadyForm();
    adminApiPostMock
      .mockRejectedValueOnce(new AdminApiClientError(403, "security.csrf_invalid"))
      .mockResolvedValueOnce(undefined);
    fetchCsrfTokenMock.mockResolvedValueOnce("fresh-token");
    fireEvent.change(screen.getByTestId("admin-login-identifier"), {
      target: { value: "superadmin" },
    });
    fireEvent.change(screen.getByTestId("admin-login-password"), {
      target: { value: "LocalOnly-Cappadocia-2026!" },
    });
    fireEvent.click(screen.getByTestId("admin-login-submit"));

    await waitFor(() => expect(push).toHaveBeenCalledWith("/"));
    expect(adminApiPostMock).toHaveBeenCalledTimes(2);
    expect(adminApiPostMock).toHaveBeenLastCalledWith(
      "/admin-api/auth/login",
      expect.any(Object),
      "fresh-token",
    );
  });
});
