import { QueryClientProvider } from "@tanstack/react-query";
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { HttpResponse, http } from "msw";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from "vitest";
import { createAdminQueryClient } from "@/lib/query-client";
import { MediaManager } from "../media-manager";

const ROOM_TYPE_ID = "0199f2c0-0000-7000-8000-000000000301";

const roomTypes = [
  { id: ROOM_TYPE_ID, nameTr: "Kemerli Mağara Süit", nameEn: "Arched Cave Suite" },
];

function mediaItem(overrides: Record<string, unknown> = {}) {
  return {
    id: "0199f2c0-0000-7000-8000-000000000401",
    type: "PHOTO",
    status: "READY",
    roomTypeId: ROOM_TYPE_ID,
    published: false,
    cover: false,
    displayOrder: 0,
    altTextTr: "Mağara süit",
    altTextEn: "Cave suite",
    original: {
      mimeType: "image/jpeg",
      sizeBytes: 204800,
      width: 1200,
      height: 800,
    },
    variants: [
      {
        variant: "thumb",
        format: "webp",
        url: "http://localhost:9000/hotel-media/private/401/thumb.webp?signature=x",
        width: 480,
        height: 320,
      },
      {
        variant: "card",
        format: "webp",
        url: "http://localhost:9000/hotel-media/private/401/card.webp?signature=x",
        width: 1200,
        height: 800,
      },
    ],
    failureCode: null,
    createdAt: "2026-11-03T10:00:00Z",
    updatedAt: "2026-11-03T10:00:00Z",
    ...overrides,
  };
}

function page(items: unknown[]) {
  return { items, page: 0, size: 20, total: items.length };
}

const server = setupServer(
  http.get("*/admin-api/media/room-types", () => HttpResponse.json(roomTypes)),
  http.get("*/admin-api/media", () => HttpResponse.json(page([]))),
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
      <MediaManager />
    </QueryClientProvider>,
  );
}

describe("MediaManager (AC-4)", () => {
  it("shouldRenderLoadingThenEmptyState_whenNoMediaExists", async () => {
    server.use(
      http.get("*/admin-api/media", async () => {
        await new Promise((resolve) => setTimeout(resolve, 20));
        return HttpResponse.json(page([]));
      }),
    );

    renderManager();

    expect(screen.getByTestId("admin-media-loading")).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getByTestId("admin-media-empty")).toBeInTheDocument(),
    );
    expect(screen.getByTestId("admin-media-upload")).toBeInTheDocument();
  });

  it("shouldRenderStatusActionsAndAltWarning_forEachMediaState", async () => {
    server.use(
      http.get("*/admin-api/media", () =>
        HttpResponse.json(
          page([
            mediaItem({
              id: "0199f2c0-0000-7000-8000-000000000401",
              published: true,
              cover: true,
              altTextEn: null,
            }),
            mediaItem({
              id: "0199f2c0-0000-7000-8000-000000000402",
              roomTypeId: null,
              displayOrder: 0,
              altTextTr: null,
              altTextEn: null,
              variants: [],
              original: {
                mimeType: "image/png",
                sizeBytes: 1000,
                width: null,
                height: null,
              },
            }),
            mediaItem({
              id: "0199f2c0-0000-7000-8000-000000000403",
              status: "FAILED",
              roomTypeId: null,
              failureCode: "media.invalid_image",
              variants: [],
            }),
            mediaItem({
              id: "0199f2c0-0000-7000-8000-000000000404",
              status: "PROCESSING",
              displayOrder: 1,
              variants: [],
            }),
          ]),
        ),
      ),
    );

    renderManager();

    await waitFor(() =>
      expect(screen.getAllByTestId("admin-media-item")).toHaveLength(4),
    );

    const items = screen.getAllByTestId("admin-media-item");
    const published = within(items[0]);
    const unassigned = within(items[1]);
    const failed = within(items[2]);
    const processing = within(items[3]);

    expect(published.getByTestId("admin-media-status")).toHaveTextContent("Hazır");
    expect(published.getByTestId("admin-media-cover-badge")).toBeInTheDocument();
    expect(
      published.queryByTestId("admin-media-cover"),
    ).not.toBeInTheDocument();
    expect(published.getByTestId("admin-media-unpublish")).toBeInTheDocument();
    expect(published.getByTestId("admin-media-archive")).toBeInTheDocument();
    expect(published.getByTestId("admin-media-alt-warning")).toHaveTextContent(
      "eksik alt metin",
    );

    expect(unassigned.getByTestId("admin-media-status")).toHaveTextContent(
      "Hazır",
    );
    expect(
      unassigned.queryByTestId("admin-media-publish"),
    ).not.toBeInTheDocument();
    expect(unassigned.getByTestId("admin-media-room-type")).toHaveValue("");

    expect(failed.getByTestId("admin-media-status")).toHaveTextContent(
      "Başarısız",
    );
    expect(failed.getByTestId("admin-media-retry-processing")).toBeInTheDocument();

    expect(processing.getByTestId("admin-media-status")).toHaveTextContent(
      "İşleniyor",
    );
    expect(
      processing.queryByTestId("admin-media-publish"),
    ).not.toBeInTheDocument();
    expect(processing.getByTestId("admin-media-archive")).toBeInTheDocument();
  });

  it("shouldRetryLoad_genericErrorWithoutLeakingDetails_whenBackendFails", async () => {
    let attempts = 0;
    server.use(
      http.get("*/admin-api/media", () => {
        attempts += 1;
        if (attempts === 1) {
          return HttpResponse.json(
            {
              type: "about:blank",
              title: "Internal Server Error",
              status: 500,
              detail: "PSQLException: sensitive detail",
              instance: "/admin-api/media",
              code: "system.internal_error",
              requestId: "0199f2c0-0000-7000-8000-000000000001",
            },
            { status: 500, headers: { "Content-Type": "application/problem+json" } },
          );
        }
        return HttpResponse.json(page([mediaItem()]));
      }),
    );

    renderManager();

    await waitFor(() =>
      expect(screen.getByTestId("admin-media-error")).toBeInTheDocument(),
    );
    expect(screen.queryByText(/PSQLException/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/system\.internal_error/i)).not.toBeInTheDocument();

    fireEvent.click(screen.getByTestId("admin-media-retry"));

    await waitFor(() =>
      expect(screen.getAllByTestId("admin-media-item")).toHaveLength(1),
    );
  });

  it("shouldShowPermissionError_whenFrontDeskGets403", async () => {
    server.use(
      http.get("*/admin-api/media", () =>
        HttpResponse.json(
          {
            type: "about:blank",
            title: "Forbidden",
            status: 403,
            detail: "Access is denied.",
            instance: "/admin-api/media",
            code: "security.access_denied",
            requestId: "0199f2c0-0000-7000-8000-000000000002",
          },
          { status: 403, headers: { "Content-Type": "application/problem+json" } },
        ),
      ),
    );

    renderManager();

    await waitFor(() =>
      expect(screen.getByTestId("admin-media-error")).toBeInTheDocument(),
    );
    expect(screen.queryByTestId("admin-media-list")).not.toBeInTheDocument();
  });

  it("shouldUploadThroughPresignedUrlAndComplete_thenRefreshList", async () => {
    let startedBody: Record<string, unknown> | null = null;
    let uploadContentType: string | null = null;
    let completed = false;

    server.use(
      http.get("*/admin-api/media", () =>
        HttpResponse.json(
          page(
            completed
              ? [mediaItem({ status: "PROCESSING", variants: [] })]
              : [],
          ),
        ),
      ),
      http.post("*/admin-api/media/uploads", async ({ request }) => {
        startedBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json(
          {
            mediaId: "0199f2c0-0000-7000-8000-000000000401",
            uploadUrl: "http://localhost:9000/hotel-media/private/upload-target",
            expiresAt: "2026-11-03T10:10:00Z",
          },
          { status: 201 },
        );
      }),
      http.put("http://localhost:9000/hotel-media/private/upload-target", ({ request }) => {
        uploadContentType = request.headers.get("Content-Type");
        return new HttpResponse(null, { status: 200 });
      }),
      http.post("*/admin-api/media/0199f2c0-0000-7000-8000-000000000401/complete", () => {
        completed = true;
        return HttpResponse.json(
          {
            mediaId: "0199f2c0-0000-7000-8000-000000000401",
            status: "PROCESSING",
          },
          { status: 202 },
        );
      }),
    );

    renderManager();
    await waitFor(() =>
      expect(screen.getByTestId("admin-media-empty")).toBeInTheDocument(),
    );

    const file = new File(["image-bytes"], "room.jpg", { type: "image/jpeg" });
    fireEvent.change(screen.getByTestId("admin-media-file-input"), {
      target: { files: [file] },
    });
    fireEvent.click(screen.getByTestId("admin-media-upload-submit"));

    await waitFor(() =>
      expect(screen.getAllByTestId("admin-media-item")).toHaveLength(1),
    );
    expect(startedBody).toEqual({
      fileName: "room.jpg",
      contentType: "image/jpeg",
      sizeBytes: file.size,
    });
    expect(uploadContentType).toBe("image/jpeg");
    expect(completed).toBe(true);
    expect(screen.getByTestId("admin-media-status")).toHaveTextContent(
      "İşleniyor",
    );
  });

  it("shouldSaveAltTextAndSendCsrf_whenFormSubmitted", async () => {
    server.use(
      http.get("*/admin-api/media", () => HttpResponse.json(page([mediaItem()]))),
      http.patch("*/admin-api/media/0199f2c0-0000-7000-8000-000000000401", async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>;
        expect(body).toEqual({
          roomTypeId: ROOM_TYPE_ID,
          altTextTr: "Yeni TR",
          altTextEn: "New EN",
        });
        expect(request.headers.get("X-XSRF-TOKEN")).toBe("test-csrf");
        return HttpResponse.json(
          mediaItem({ altTextTr: "Yeni TR", altTextEn: "New EN" }),
        );
      }),
    );

    renderManager();
    await waitFor(() =>
      expect(screen.getByTestId("admin-media-item")).toBeInTheDocument(),
    );

    fireEvent.change(screen.getByTestId("admin-media-alt-tr"), {
      target: { value: "Yeni TR" },
    });
    fireEvent.change(screen.getByTestId("admin-media-alt-en"), {
      target: { value: "New EN" },
    });
    fireEvent.click(screen.getByText("Alt metni kaydet"));

    await waitFor(() =>
      expect(screen.getByTestId("admin-media-alt-tr")).toHaveValue("Yeni TR"),
    );
  });

  it("shouldSendCompleteOrderedList_whenMovingMedia", async () => {
    const first = mediaItem({
      id: "0199f2c0-0000-7000-8000-000000000401",
      displayOrder: 0,
    });
    const second = mediaItem({
      id: "0199f2c0-0000-7000-8000-000000000402",
      displayOrder: 1,
    });

    server.use(
      http.get("*/admin-api/media", () =>
        HttpResponse.json(page([first, second])),
      ),
      http.put("*/admin-api/media/order", async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>;
        expect(body).toEqual({
          roomTypeId: ROOM_TYPE_ID,
          mediaIds: [second.id, first.id],
        });
        expect(request.headers.get("X-XSRF-TOKEN")).toBe("test-csrf");
        return new HttpResponse(null, { status: 200 });
      }),
    );

    renderManager();
    await waitFor(() =>
      expect(screen.getAllByTestId("admin-media-item")).toHaveLength(2),
    );

    const items = screen.getAllByTestId("admin-media-item");
    fireEvent.click(within(items[0]).getByTestId("admin-media-order-down"));

    await waitFor(() =>
      expect(
        screen.getAllByTestId("admin-media-item").length,
      ).toBe(2),
    );
  });

  it("shouldConfirmBeforeArchivingAndSkipRequest_whenCancelled", async () => {
    let archiveCalls = 0;
    server.use(
      http.get("*/admin-api/media", () => HttpResponse.json(page([mediaItem()]))),
      http.delete("*/admin-api/media/0199f2c0-0000-7000-8000-000000000401", () => {
        archiveCalls += 1;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);

    renderManager();
    await waitFor(() =>
      expect(screen.getByTestId("admin-media-item")).toBeInTheDocument(),
    );

    fireEvent.click(screen.getByTestId("admin-media-archive"));
    expect(confirm).toHaveBeenCalled();
    expect(archiveCalls).toBe(0);

    confirm.mockReturnValue(true);
    fireEvent.click(screen.getByTestId("admin-media-archive"));
    await waitFor(() => expect(archiveCalls).toBe(1));
  });
});
