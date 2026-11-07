import { QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { delay, HttpResponse, http } from "msw";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll, describe, expect, it } from "vitest";
import { RoomShowcase } from "../room-showcase";
import { createAppQueryClient } from "@/lib/query-client";

const server = setupServer(
  http.get("http://localhost:8081/api/media", () =>
    HttpResponse.json({ items: [] }),
  ),
);

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

function renderWithQuery(locale: "tr" | "en") {
  const queryClient = createAppQueryClient();
  const result = render(
    <QueryClientProvider client={queryClient}>
      <RoomShowcase locale={locale} />
    </QueryClientProvider>,
  );
  return { ...result, queryClient };
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

  it("shouldRenderCoverImageWithSrcsetAndLocalizedAlt_whenMediaIsPublished", async () => {
    let capturedIds: string | null = null;

    server.use(
      http.get("http://localhost:8081/api/room-types", () =>
        HttpResponse.json(SAMPLE_ROOMS_RESPONSE),
      ),
      http.get("http://localhost:8081/api/media", ({ request }) => {
        capturedIds = new URL(request.url).searchParams.get("roomTypeIds");
        return HttpResponse.json({
          items: [
            {
              id: "0199f2c0-0000-7000-8000-000000000401",
              roomTypeId: "0199f2c0-0000-7000-8000-000000000301",
              cover: true,
              displayOrder: 0,
              altTextTr: "Kemerli mağara süit yatak odası",
              altTextEn: "Arched cave suite bedroom",
              variants: {
                thumb: {
                  url: "http://localhost:9000/hotel-media/public/thumb.webp",
                  width: 480,
                  height: 320,
                  format: "webp",
                },
                card: {
                  url: "http://localhost:9000/hotel-media/public/card.webp",
                  width: 1200,
                  height: 800,
                  format: "webp",
                },
                cover: {
                  url: "http://localhost:9000/hotel-media/public/cover.webp",
                  width: 1920,
                  height: 1280,
                  format: "webp",
                },
              },
            },
          ],
        });
      }),
    );

    renderWithQuery("tr");

    await waitFor(() => {
      expect(screen.getAllByTestId("room-card-image")).toHaveLength(1);
    });

    expect(capturedIds).toBe("0199f2c0-0000-7000-8000-000000000301,0199f2c0-0000-7000-8000-000000000302");

    const image = screen.getByTestId("room-card-image") as HTMLImageElement;
    expect(image.src).toBe("http://localhost:9000/hotel-media/public/card.webp");
    expect(image.alt).toBe("Kemerli mağara süit yatak odası");
    expect(image.width).toBe(1200);
    expect(image.height).toBe(800);
    expect(image.getAttribute("srcset")).toContain(
      "http://localhost:9000/hotel-media/public/card.webp 1200w",
    );
    expect(image.getAttribute("srcset")).not.toContain(
      "http://localhost:9000/hotel-media/public/cover.webp",
    );
    expect(image.getAttribute("loading")).toBe("lazy");

    const firstCard = within(screen.getAllByTestId("room-card")[0]);
    expect(firstCard.queryByTestId("room-card-placeholder")).not.toBeInTheDocument();
  });

  it("shouldUseFirstOrderedMediaAsCoverAndRoomNameAsAltFallback_whenNoExplicitCoverExists", async () => {
    server.use(
      http.get("http://localhost:8081/api/room-types", () =>
        HttpResponse.json(SAMPLE_ROOMS_RESPONSE),
      ),
      http.get("http://localhost:8081/api/media", () =>
        HttpResponse.json({
          items: [
            {
              id: "0199f2c0-0000-7000-8000-000000000402",
              roomTypeId: "0199f2c0-0000-7000-8000-000000000301",
              cover: false,
              displayOrder: 2,
              altTextTr: null,
              altTextEn: null,
              variants: {
                thumb: null,
                card: {
                  url: "http://localhost:9000/hotel-media/public/second.webp",
                  width: 1200,
                  height: 800,
                  format: "webp",
                },
                cover: null,
              },
            },
            {
              id: "0199f2c0-0000-7000-8000-000000000403",
              roomTypeId: "0199f2c0-0000-7000-8000-000000000301",
              cover: false,
              displayOrder: 1,
              altTextTr: null,
              altTextEn: null,
              variants: {
                thumb: null,
                card: {
                  url: "http://localhost:9000/hotel-media/public/first.webp",
                  width: 1200,
                  height: 800,
                  format: "webp",
                },
                cover: null,
              },
            },
          ],
        }),
      ),
    );

    renderWithQuery("en");

    await waitFor(() => {
      expect(screen.getAllByTestId("room-card-image")).toHaveLength(1);
    });

    const image = screen.getByTestId("room-card-image") as HTMLImageElement;
    expect(image.src).toBe("http://localhost:9000/hotel-media/public/first.webp");
    expect(image.alt).toBe("Arched Cave Suite");
  });

  it("shouldDropCachedMediaAndShowPlaceholder_whenRefreshFails", async () => {
    server.use(
      http.get("http://localhost:8081/api/room-types", () =>
        HttpResponse.json(SAMPLE_ROOMS_RESPONSE),
      ),
      http.get("http://localhost:8081/api/media", () =>
        HttpResponse.json({
          items: [
            {
              id: "0199f2c0-0000-7000-8000-000000000405",
              roomTypeId: "0199f2c0-0000-7000-8000-000000000301",
              cover: true,
              displayOrder: 0,
              altTextTr: "Kapak",
              altTextEn: "Cover",
              variants: {
                thumb: null,
                card: {
                  url: "http://localhost:9000/hotel-media/public/card.webp",
                  width: 1200,
                  height: 800,
                  format: "webp",
                },
                cover: null,
              },
            },
          ],
        }),
      ),
    );

    const { queryClient } = renderWithQuery("tr");

    await waitFor(() => {
      expect(screen.getAllByTestId("room-card-image")).toHaveLength(1);
    });

    server.use(
      http.get("http://localhost:8081/api/media", () =>
        HttpResponse.json(
          {
            type: "about:blank",
            title: "Service Unavailable",
            status: 503,
            code: "media.storage_unavailable",
            instance: "/api/media",
          },
          { status: 503, headers: { "Content-Type": "application/problem+json" } },
        ),
      ),
    );
    await queryClient.refetchQueries({ queryKey: ["catalog", "media"] });

    await waitFor(() => {
      expect(screen.queryAllByTestId("room-card-image")).toHaveLength(0);
    });
    expect(screen.getAllByTestId("room-card-placeholder")).toHaveLength(2);
  });

  it("shouldKeepPlaceholder_whenMediaServiceFails", async () => {
    server.use(
      http.get("http://localhost:8081/api/room-types", () =>
        HttpResponse.json(SAMPLE_ROOMS_RESPONSE),
      ),
      http.get("http://localhost:8081/api/media", () =>
        HttpResponse.json(
          {
            type: "about:blank",
            title: "Service Unavailable",
            status: 503,
            code: "media.storage_unavailable",
            instance: "/api/media",
          },
          { status: 503, headers: { "Content-Type": "application/problem+json" } },
        ),
      ),
    );

    renderWithQuery("tr");

    await waitFor(() => {
      expect(screen.getByTestId("rooms-list")).toBeInTheDocument();
    });

    expect(screen.queryAllByTestId("room-card-image")).toHaveLength(0);
    expect(screen.getAllByTestId("room-card-placeholder")).toHaveLength(2);
  });
});
