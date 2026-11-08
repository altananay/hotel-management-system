import { QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from "vitest";
import { createAdminQueryClient } from "@/lib/query-client";
import { PricingManager } from "../pricing-manager";

const PLAN_ID = "0199f2c0-0000-7000-8000-000000000701";
const SEASON_ID = "0199f2c0-0000-7000-8000-000000000702";
const ROOM_TYPE_ID = "0199f2c0-0000-7000-8000-000000000301";

const plan = {
  id: PLAN_ID,
  code: "STANDARD",
  nameTr: "Standart",
  nameEn: "Standard",
  active: true,
  defaultPlan: true,
  createdAt: "2026-11-08T10:00:00Z",
  updatedAt: "2026-11-08T10:00:00Z",
  seasons: [
    {
      id: SEASON_ID,
      ratePlanId: PLAN_ID,
      code: "HIGH",
      nameTr: "Yüksek Sezon",
      nameEn: "High Season",
      startDate: "2026-11-01",
      endDate: "2026-12-31",
      createdAt: "2026-11-08T10:00:00Z",
      updatedAt: "2026-11-08T10:00:00Z",
    },
  ],
};

const settings = {
  bufferPercent: "2.00",
  roundingStep: "5.00",
  updatedAt: "2026-11-08T10:00:00Z",
};

const roomTypes = [
  {
    id: ROOM_TYPE_ID,
    nameTr: "Kemerli Mağara Süit",
    nameEn: "Arched Cave Suite",
    maxGuests: 2,
  },
];

const matrix = {
  roomTypes: [
    {
      id: ROOM_TYPE_ID,
      nameTr: "Kemerli Mağara Süit",
      nameEn: "Arched Cave Suite",
      maxGuests: 2,
      active: true,
    },
  ],
  rows: [
    {
      id: "row-1",
      roomTypeId: ROOM_TYPE_ID,
      guests: 1,
      amountTry: "100.00",
      updatedAt: "2026-11-08T10:00:00Z",
    },
  ],
  missing: [{ roomTypeId: ROOM_TYPE_ID, guests: 2 }],
};

const fxHistory = {
  latest: [
    {
      id: "fx-1",
      currency: "EUR",
      source: "TCMB",
      rateDate: "2026-11-07",
      rate: "42.500000",
      capturedAt: "2026-11-07T12:30:00Z",
      updatedAt: "2026-11-07T12:30:00Z",
    },
  ],
  items: [
    {
      id: "fx-1",
      currency: "EUR",
      source: "TCMB",
      rateDate: "2026-11-07",
      rate: "42.500000",
      capturedAt: "2026-11-07T12:30:00Z",
      updatedAt: "2026-11-07T12:30:00Z",
    },
  ],
  page: 0,
  size: 50,
  total: 1,
  stale: true,
  staleCurrencies: ["EUR", "USD"],
};

const server = setupServer(
  http.get("*/admin-api/pricing/rate-plans", () => HttpResponse.json([plan])),
  http.get("*/admin-api/pricing/settings", () => HttpResponse.json(settings)),
  http.get("*/admin-api/pricing/fx-rates*", () => HttpResponse.json(fxHistory)),
  http.get("*/admin-api/pricing/room-types", () => HttpResponse.json(roomTypes)),
  http.get(`*/admin-api/pricing/rate-plans/${PLAN_ID}/prices`, () =>
    HttpResponse.json(matrix),
  ),
  http.get(`*/admin-api/pricing/seasons/${SEASON_ID}/prices`, () =>
    HttpResponse.json(matrix),
  ),
  http.get("*/admin-api/pricing/manual-prices*", () => HttpResponse.json([])),
  http.get("*/admin-api/pricing/manual-fx-rates*", () =>
    HttpResponse.json({ items: [], page: 0, size: 50, total: 0 }),
  ),
  http.get("*/admin-api/pricing/rate-plans/null/prices", () =>
    HttpResponse.json(matrix),
  ),
  http.get("*/admin-api/pricing/seasons/null/prices", () =>
    HttpResponse.json(matrix),
  ),
);

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
beforeEach(() => {
  document.cookie = "XSRF-TOKEN=test-csrf; path=/";
});
afterEach(() => {
  server.resetHandlers();
  vi.restoreAllMocks();
  document.cookie = "XSRF-TOKEN=; Max-Age=0; path=/";
});
afterAll(() => server.close());

function renderManager() {
  return render(
    <QueryClientProvider client={createAdminQueryClient()}>
      <PricingManager />
    </QueryClientProvider>,
  );
}

describe("PricingManager (AC-2)", () => {
  it("shouldRenderLoadingThenEmpty_whenNoPlansExist", async () => {
    server.use(
      http.get("*/admin-api/pricing/rate-plans", async () => {
        await new Promise((resolve) => setTimeout(resolve, 20));
        return HttpResponse.json([]);
      }),
    );

    renderManager();

    expect(screen.getByTestId("admin-pricing-loading")).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getByTestId("admin-pricing-empty")).toBeInTheDocument(),
    );
    expect(screen.getByTestId("admin-pricing-page")).toBeInTheDocument();
    expect(screen.getByTestId("admin-pricing-manual-fx")).toBeInTheDocument();
  });

  it("shouldRenderErrorAndRecoverAfterRetry_whenRatePlansFail", async () => {
    server.use(
      http.get("*/admin-api/pricing/rate-plans", () =>
        HttpResponse.json(
          {
            type: "about:blank",
            title: "Internal Server Error",
            status: 500,
            detail: "failed",
            instance: "/admin-api/pricing/rate-plans",
            code: "system.internal_error",
            requestId: "req-1",
          },
          { status: 500 },
        ),
      ),
    );

    renderManager();
    await waitFor(() =>
      expect(screen.getByTestId("admin-pricing-error")).toBeInTheDocument(),
    );

    server.use(
      http.get("*/admin-api/pricing/rate-plans", () => HttpResponse.json([])),
    );
    fireEvent.click(screen.getByTestId("admin-pricing-retry"));
    await waitFor(() =>
      expect(screen.getByTestId("admin-pricing-empty")).toBeInTheDocument(),
    );
  });

  it("shouldUpdatePlanName_whenEditFormSubmitted", async () => {
    let currentPlan = { ...plan };
    server.use(
      http.get("*/admin-api/pricing/rate-plans", () =>
        HttpResponse.json([currentPlan]),
      ),
      http.patch(
        `*/admin-api/pricing/rate-plans/${PLAN_ID}`,
        async ({ request }) => {
          const body = (await request.json()) as {
            nameTr?: string;
            nameEn?: string;
          };
          currentPlan = { ...currentPlan, ...body };
          return HttpResponse.json(currentPlan);
        },
      ),
    );

    renderManager();
    const row = await screen.findByTestId("admin-pricing-rate-plan-row");
    fireEvent.click(within(row).getByRole("button", { name: "Düzenle" }));
    fireEvent.change(screen.getByLabelText("Plan düzenleme TR adı"), {
      target: { value: "Güncel Ad" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Planı Güncelle" }));

    await waitFor(() =>
      expect(screen.getByText(/Güncel Ad/)).toBeInTheDocument(),
    );
  });

  it("shouldRenderFilledStateAndStaleFxWarning_whenDataExists", async () => {
    renderManager();

    await waitFor(() =>
      expect(screen.getByTestId("admin-pricing-rate-plans")).toBeInTheDocument(),
    );
    expect(screen.getByTestId("admin-pricing-filled")).toBeInTheDocument();
    expect(screen.getByTestId("admin-pricing-rate-plan-row")).toBeInTheDocument();
    expect(
      screen.getByTestId("admin-pricing-rate-plan-default"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("admin-pricing-fx-warning")).toBeInTheDocument();
    expect(
      await screen.findByTestId("admin-pricing-prices"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("admin-pricing-season-row")).toBeInTheDocument();
    expect(screen.getAllByTestId("admin-pricing-save").length).toBeGreaterThan(0);
    expect(screen.getByText(/mevcut fiyat/i)).toBeInTheDocument();
  });
});
