import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { delay, HttpResponse, http } from "msw";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll, describe, expect, it } from "vitest";
import { SystemStatusCard } from "../system-status-card";
import { createAppQueryClient } from "@/lib/query-client";

const server = setupServer();

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

function renderWithQuery(locale: "tr" | "en") {
  const queryClient = createAppQueryClient();
  return render(
    <QueryClientProvider client={queryClient}>
      <SystemStatusCard locale={locale} />
    </QueryClientProvider>,
  );
}

describe("SystemStatusCard (AC-7, AC-11)", () => {
  it("shouldRenderLoadingAndThenEmptyStateInTurkish_whenBackendReturnsEmptyItems", async () => {
    server.use(
      http.get("http://localhost:8081/api/system/sample-items", async () => {
        await delay(50);
        return HttpResponse.json({ items: [] });
      }),
    );

    renderWithQuery("tr");

    expect(screen.getByTestId("web-status-loading")).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByTestId("web-status-empty")).toBeInTheDocument();
    });
    expect(screen.getByTestId("web-status-empty-title")).toHaveTextContent(
      "Henüz yayınlanmış oda bulunmuyor",
    );
  });

  it("shouldRenderEmptyStateInEnglish_whenLocaleIsEn", async () => {
    server.use(
      http.get("http://localhost:8081/api/system/sample-items", () => {
        return HttpResponse.json({ items: [] });
      }),
    );

    renderWithQuery("en");

    await waitFor(() => {
      expect(screen.getByTestId("web-status-empty")).toBeInTheDocument();
    });
    expect(screen.getByTestId("web-status-empty-title")).toHaveTextContent(
      "No published rooms available yet",
    );
  });

  it("shouldRenderUserFriendlyErrorWithoutLeakingTechnicalDetail_whenBackendReturnsProblemJson500", async () => {
    server.use(
      http.get("http://localhost:8081/api/system/sample-items", () => {
        return HttpResponse.json(
          {
            type: "about:blank",
            title: "Internal Server Error",
            status: 500,
            detail: "java.sql.SQLException: internal stack trace detail",
            instance: "/api/system/sample-items",
            code: "system.internal_error",
            requestId: "0199f2c0-0000-7000-8000-000000000001",
          },
          {
            status: 500,
            headers: { "Content-Type": "application/problem+json" },
          },
        );
      }),
    );

    renderWithQuery("tr");

    await waitFor(() => {
      expect(screen.getByTestId("web-status-error")).toBeInTheDocument();
    });

    expect(screen.getByTestId("web-status-error-message")).toHaveTextContent(
      "Sistemde geçici bir sorun oluştu. Lütfen kısa süre sonra tekrar deneyin.",
    );
    expect(screen.queryByText(/SQLException/i)).not.toBeInTheDocument();
    expect(screen.getByTestId("web-status-retry")).toBeInTheDocument();
  });

  it("shouldFallBackToGenericMessage_whenProblemJsonIsMissingRequiredFields", async () => {
    server.use(
      http.get("http://localhost:8081/api/system/sample-items", () => {
        return HttpResponse.json(
          { title: "Bozuk zarf" },
          {
            status: 500,
            headers: { "Content-Type": "application/problem+json" },
          },
        );
      }),
    );

    renderWithQuery("tr");

    await waitFor(() => {
      expect(screen.getByTestId("web-status-error")).toBeInTheDocument();
    });

    // Zarf eksik olduğunda makine kodu okunamaz; kullanıcı dostu genel mesaja düşülür (CR-9).
    expect(screen.getByTestId("web-status-error-message")).toHaveTextContent(
      "Bağlantı sırasında beklenmeyen bir sorun oluştu. Lütfen tekrar deneyin.",
    );
  });

  it("shouldStillParseProblemJson_whenBackendAddsUnknownExtensionFields", async () => {
    server.use(
      http.get("http://localhost:8081/api/system/sample-items", () => {
        return HttpResponse.json(
          {
            type: "about:blank",
            title: "Internal Server Error",
            status: 500,
            detail: "Service unavailable",
            instance: "/api/system/sample-items",
            code: "system.internal_error",
            requestId: "0199f2c0-0000-7000-8000-000000000002",
            traceId: "extension-field",
          },
          {
            status: 500,
            headers: { "Content-Type": "application/problem+json" },
          },
        );
      }),
    );

    renderWithQuery("tr");

    await waitFor(() => {
      expect(screen.getByTestId("web-status-error")).toBeInTheDocument();
    });

    expect(screen.getByTestId("web-status-error-message")).toHaveTextContent(
      "Sistemde geçici bir sorun oluştu. Lütfen kısa süre sonra tekrar deneyin.",
    );
  });
});
