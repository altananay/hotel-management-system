import { QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { delay, HttpResponse, http } from "msw";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll, describe, expect, it } from "vitest";
import { RoomShowcase } from "../room-showcase";
import { createAppQueryClient } from "@/lib/query-client";

const server = setupServer();

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

function renderWithQuery(locale: "tr" | "en") {
  const queryClient = createAppQueryClient();
  return render(
    <QueryClientProvider client={queryClient}>
      <RoomShowcase locale={locale} />
    </QueryClientProvider>,
  );
}

const SAMPLE_ROOMS_RESPONSE = {
  items: [
    {
      id: "0199f2c0-0000-7000-8000-000000000301",
      nameTr: "Kemerli Mağara Süit",
      nameEn: "Arched Cave Suite",
      descriptionTr: "Tarihi tüf kayalara oyulmuş yüksek kemerli mağara süit.",
      descriptionEn: "Carved into historic tuff rock with high vaulted ceilings.",
      maxGuests: 2,
      amenities: [
        {
          code: "cave_jacuzzi",
          nameTr: "Özel Taş Oyma Jakuzi",
          nameEn: "Private Carved-Stone Jacuzzi",
        },
        {
          code: "wifi",
          nameTr: "Yüksek Hızlı Kablosuz İnternet",
          nameEn: "High-Speed Wireless Internet",
        },
      ],
    },
    {
      id: "0199f2c0-0000-7000-8000-000000000302",
      nameTr: "Peri Bacası Manzaralı Deluxe Taş Oda",
      nameEn: "Fairy Chimney View Deluxe Stone Room",
      descriptionTr: "Özel terası ve taş şöminesiyle ferah taş oda.",
      descriptionEn: "Spacious stone room with private terrace and fireplace.",
      maxGuests: 3,
      amenities: [
        {
          code: "terrace_view",
          nameTr: "Panoramik Vadi ve Balon Terası",
          nameEn: "Panoramic Valley & Balloon Terrace",
        },
      ],
    },
  ],
  page: 0,
  size: 100,
  total: 2,
};

describe("RoomShowcase (AC-3, QA-10, QA-11, QA-15)", () => {
  it("shouldRequestPage0Size100AndRenderLoadingThenEmptyState_whenBackendReturnsNoRooms", async () => {
    let capturedPage: string | null = null;
    let capturedSize: string | null = null;

    server.use(
      http.get("http://localhost:8081/api/room-types", async ({ request }) => {
        const url = new URL(request.url);
        capturedPage = url.searchParams.get("page");
        capturedSize = url.searchParams.get("size");
        await delay(50);
        return HttpResponse.json({
          items: [],
          page: 0,
          size: 100,
          total: 0,
        });
      }),
    );

    renderWithQuery("tr");

    expect(screen.getByTestId("rooms-loading")).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByTestId("rooms-empty")).toBeInTheDocument();
    });

    expect(capturedPage).toBe("0");
    expect(capturedSize).toBe("100");
    expect(screen.getByTestId("rooms-empty-title")).toHaveTextContent(
      "Henüz yayınlanmış oda bulunmuyor.",
    );
  });

  it("shouldRenderEmptyStateInEnglish_whenLocaleIsEnAndNoRoomsAvailable", async () => {
    server.use(
      http.get("http://localhost:8081/api/room-types", () => {
        return HttpResponse.json({
          items: [],
          page: 0,
          size: 100,
          total: 0,
        });
      }),
    );

    renderWithQuery("en");

    await waitFor(() => {
      expect(screen.getByTestId("rooms-empty")).toBeInTheDocument();
    });

    expect(screen.getByTestId("rooms-empty-title")).toHaveTextContent(
      "No published rooms are available yet.",
    );
  });

  it("shouldRenderTurkishErrorAndRetryWithoutLeakingTechnicalDetail_whenBackendReturns500", async () => {
    let attempts = 0;

    server.use(
      http.get("http://localhost:8081/api/room-types", () => {
        attempts += 1;
        if (attempts === 1) {
          return HttpResponse.json(
            {
              type: "about:blank",
              title: "Internal Server Error",
              status: 500,
              detail: "org.postgresql.util.PSQLException: sensitive internal detail",
              instance: "/api/room-types",
              code: "system.internal_error",
              requestId: "0199f2c0-0000-7000-8000-000000000001",
            },
            {
              status: 500,
              headers: { "Content-Type": "application/problem+json" },
            },
          );
        }
        return HttpResponse.json(SAMPLE_ROOMS_RESPONSE);
      }),
    );

    renderWithQuery("tr");

    await waitFor(() => {
      expect(screen.getByTestId("rooms-error")).toBeInTheDocument();
    });

    expect(screen.getByTestId("rooms-error-message")).toHaveTextContent(
      "Oda bilgileri şu anda yüklenemedi. Lütfen tekrar deneyin.",
    );
    expect(screen.queryByText(/PSQLException/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/system\.internal_error/i)).not.toBeInTheDocument();

    const retryButton = screen.getByTestId("rooms-retry");
    expect(retryButton).toHaveTextContent("Tekrar Dene");

    fireEvent.click(retryButton);

    await waitFor(() => {
      expect(screen.getByTestId("rooms-list")).toBeInTheDocument();
    });
    expect(screen.getAllByTestId("room-card")).toHaveLength(2);
  });

  it("shouldRenderEnglishErrorState_whenLocaleIsEnAndBackendFails", async () => {
    server.use(
      http.get("http://localhost:8081/api/room-types", () => {
        return HttpResponse.json(
          {
            type: "about:blank",
            title: "Internal Server Error",
            status: 500,
            detail: "Sensitive stack trace",
            instance: "/api/room-types",
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

    renderWithQuery("en");

    await waitFor(() => {
      expect(screen.getByTestId("rooms-error")).toBeInTheDocument();
    });

    expect(screen.getByTestId("rooms-error-message")).toHaveTextContent(
      "Room information could not be loaded right now. Please try again.",
    );
    expect(screen.getByTestId("rooms-retry")).toHaveTextContent("Try Again");
  });

  it("shouldRenderTurkishAndEnglishRoomCardsWithNoPriceSymbolsOrTestIds_whenRoomsReturned", async () => {
    server.use(
      http.get("http://localhost:8081/api/room-types", () => {
        return HttpResponse.json(SAMPLE_ROOMS_RESPONSE);
      }),
    );

    const { unmount } = renderWithQuery("tr");

    await waitFor(() => {
      expect(screen.getByTestId("rooms-list")).toBeInTheDocument();
    });

    const trCards = screen.getAllByTestId("room-card");
    expect(trCards).toHaveLength(2);

    const firstTrCard = within(trCards[0]);
    expect(firstTrCard.getByTestId("room-card-name")).toHaveTextContent(
      "Kemerli Mağara Süit",
    );
    expect(firstTrCard.getByTestId("room-card-description")).toHaveTextContent(
      "Tarihi tüf kayalara oyulmuş yüksek kemerli mağara süit.",
    );
    expect(firstTrCard.getByTestId("room-card-capacity")).toHaveTextContent(
      "En fazla 2 misafir",
    );
    expect(firstTrCard.getByTestId("room-card-placeholder")).toBeInTheDocument();
    expect(firstTrCard.getByTestId("room-card-amenities")).toBeInTheDocument();

    const trAmenities = firstTrCard.getAllByTestId("room-card-amenity");
    expect(trAmenities).toHaveLength(2);
    expect(trAmenities[0]).toHaveTextContent("Özel Taş Oyma Jakuzi");
    expect(trAmenities[1]).toHaveTextContent("Yüksek Hızlı Kablosuz İnternet");

    // No price testid or currency symbol in any card (AC-3, QA-10)
    for (const card of trCards) {
      expect(card.querySelectorAll('[data-testid^="price"]')).toHaveLength(0);
      expect(card.textContent).not.toMatch(/[₺€$]/);
    }

    unmount();

    // Render in English
    renderWithQuery("en");

    await waitFor(() => {
      expect(screen.getByTestId("rooms-list")).toBeInTheDocument();
    });

    const enCards = screen.getAllByTestId("room-card");
    const firstEnCard = within(enCards[0]);
    expect(firstEnCard.getByTestId("room-card-name")).toHaveTextContent(
      "Arched Cave Suite",
    );
    expect(firstEnCard.getByTestId("room-card-description")).toHaveTextContent(
      "Carved into historic tuff rock with high vaulted ceilings.",
    );
    expect(firstEnCard.getByTestId("room-card-capacity")).toHaveTextContent(
      "Up to 2 guests",
    );
    const enAmenities = firstEnCard.getAllByTestId("room-card-amenity");
    expect(enAmenities[0]).toHaveTextContent("Private Carved-Stone Jacuzzi");
    expect(enAmenities[1]).toHaveTextContent("High-Speed Wireless Internet");
  });
});
