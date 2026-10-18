import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { delay, HttpResponse, http } from "msw";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll, describe, expect, it } from "vitest";
import { AdminSystemStatusCard } from "../admin-system-status-card";
import { createAdminQueryClient } from "@/lib/query-client";

const server = setupServer();

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

function renderAdminCard() {
  const queryClient = createAdminQueryClient();
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminSystemStatusCard />
    </QueryClientProvider>,
  );
}

describe("AdminSystemStatusCard (AC-8, AC-11)", () => {
  it("shouldRenderLoadingAndThenEmptyState_whenAdminApiReturnsEmptyItems", async () => {
    server.use(
      http.get("/admin-api/system/sample-items", async () => {
        await delay(50);
        return HttpResponse.json({ items: [] });
      }),
    );

    renderAdminCard();

    expect(screen.getByTestId("admin-status-loading")).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByTestId("admin-status-empty")).toBeInTheDocument();
    });
    expect(screen.getByTestId("admin-status-empty-title")).toHaveTextContent(
      "Henüz görüntülenecek operasyonel kayıt bulunmuyor",
    );
  });

  it("shouldRenderTurkishErrorMessageWithoutLeakingTechnicalDetail_whenAdminApiReturns500", async () => {
    server.use(
      http.get("/admin-api/system/sample-items", () => {
        return HttpResponse.json(
          {
            type: "about:blank",
            title: "Internal Server Error",
            status: 500,
            detail: "org.postgresql.util.PSQLException: internal stack trace",
            instance: "/admin-api/system/sample-items",
            code: "system.internal_error",
            requestId: "0199f2c0-0000-7000-8000-000000000002",
          },
          {
            status: 500,
            headers: { "Content-Type": "application/problem+json" },
          },
        );
      }),
    );

    renderAdminCard();

    await waitFor(() => {
      expect(screen.getByTestId("admin-status-error")).toBeInTheDocument();
    });

    expect(screen.getByTestId("admin-status-error-message")).toHaveTextContent(
      "Yönetim servisinde geçici bir sorun oluştu. Lütfen tekrar deneyin.",
    );
    expect(screen.queryByText(/PSQLException/i)).not.toBeInTheDocument();
    expect(screen.getByTestId("admin-status-retry")).toBeInTheDocument();
  });
});
