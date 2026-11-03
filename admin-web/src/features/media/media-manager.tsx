"use client";

import { RefreshCw, Upload } from "lucide-react";
import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useMediaMutations } from "./use-media-mutations";
import { useMediaQuery, useRoomTypeOptionsQuery } from "./use-media-query";
import { uploadMedia } from "./upload-media";
import type { MediaItem } from "./schema";

function statusLabel(status: MediaItem["status"]) {
  switch (status) {
    case "UPLOADED":
      return "Yüklendi";
    case "PROCESSING":
      return "İşleniyor";
    case "READY":
      return "Hazır";
    case "FAILED":
      return "Başarısız";
  }
}

export function MediaManager() {
  const queryClient = useQueryClient();
  const mediaQuery = useMediaQuery();
  const roomTypesQuery = useRoomTypeOptionsQuery();
  const mutations = useMediaMutations();
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState(false);
  const [actionError, setActionError] = useState(false);

  const run = async (action: () => Promise<unknown>) => {
    setActionError(false);
    try {
      await action();
    } catch {
      setActionError(true);
    }
  };

  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ["media"] });
  };

  const submitUpload = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!file) {
      return;
    }
    setUploading(true);
    setUploadError(false);
    try {
      await uploadMedia(file);
      setFile(null);
      await refresh();
    } catch {
      setUploadError(true);
    } finally {
      setUploading(false);
    }
  };

  const move = (item: MediaItem, direction: -1 | 1) => {
    const roomTypeId = item.roomTypeId;
    if (!roomTypeId) {
      return;
    }
    const siblings = (mediaQuery.data?.items ?? [])
      .filter((candidate) => candidate.roomTypeId === roomTypeId)
      .sort((a, b) => a.displayOrder - b.displayOrder);
    const index = siblings.findIndex((candidate) => candidate.id === item.id);
    const target = index + direction;
    if (index < 0 || target < 0 || target >= siblings.length) {
      return;
    }
    const reordered = [...siblings];
    [reordered[index], reordered[target]] = [reordered[target], reordered[index]];
    void run(() =>
      mutations.reorder.mutateAsync({
        roomTypeId,
        mediaIds: reordered.map((candidate) => candidate.id),
      }),
    );
  };

  if (mediaQuery.isLoading) {
    return (
      <div data-testid="admin-media-page" className="mx-auto max-w-6xl px-6 py-12">
        <div data-testid="admin-media-loading" className="space-y-4">
          <Skeleton className="h-10 w-64" />
          <Skeleton className="h-40 w-full" />
        </div>
      </div>
    );
  }

  if (mediaQuery.isError) {
    return (
      <div data-testid="admin-media-page" className="mx-auto max-w-6xl px-6 py-12">
        <div role="alert" data-testid="admin-media-error" className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800">
          Medya listesi yüklenemedi. Lütfen tekrar deneyin.
        </div>
        <Button
          className="mt-4"
          data-testid="admin-media-retry"
          onClick={() => void mediaQuery.refetch()}
        >
          <RefreshCw className="h-4 w-4" aria-hidden="true" />
          Tekrar Dene
        </Button>
      </div>
    );
  }

  const items = mediaQuery.data?.items ?? [];
  const roomTypes = roomTypesQuery.data ?? [];

  return (
    <main data-testid="admin-media-page" className="mx-auto max-w-6xl px-6 py-12">
      <header className="flex flex-wrap items-end justify-between gap-4 border-b border-slate-200 pb-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Medya Yönetimi</h1>
          <p className="mt-1 text-sm text-slate-600">
            Oda fotoğraflarını yükleyin, sıralayın ve yayınlayın.
          </p>
        </div>
        <Button onClick={() => void mediaQuery.refetch()}>Yenile</Button>
      </header>

      {actionError && (
        <div
          role="alert"
          data-testid="admin-media-action-error"
          className="mt-6 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800"
        >
          İşlem tamamlanamadı. Listeyi yenileyip tekrar deneyin.
        </div>
      )}

      {roomTypesQuery.isError && (
        <div
          role="alert"
          data-testid="admin-media-room-types-error"
          className="mt-6 flex flex-wrap items-center justify-between gap-3 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800"
        >
          <span>Oda tipleri yüklenemedi; atama yapılamıyor.</span>
          <Button
            size="sm"
            variant="secondary"
            data-testid="admin-media-room-types-retry"
            onClick={() => void roomTypesQuery.refetch()}
          >
            Tekrar Dene
          </Button>
        </div>
      )}

      <Card className="mt-6 p-6">
        <form data-testid="admin-media-upload" onSubmit={(event) => void submitUpload(event)}>
          <label className="text-sm font-medium text-slate-700" htmlFor="media-file">
            Fotoğraf (JPEG, PNG veya WebP · en fazla 15 MB)
          </label>
          <input
            id="media-file"
            data-testid="admin-media-file-input"
            className="mt-2 block w-full rounded-md border border-slate-300 p-2 text-sm"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            onChange={(event) => setFile(event.target.files?.[0] ?? null)}
          />
          {uploading && (
            <p className="mt-2 text-sm text-slate-600" role="status">
              Yükleniyor ve işleme alınıyor...
            </p>
          )}
          {uploadError && (
            <p className="mt-2 text-sm text-red-700" role="alert">
              Fotoğraf yüklenemedi. Türü ve boyutu kontrol edin.
            </p>
          )}
          <Button
            className="mt-4"
            type="submit"
            disabled={!file || uploading}
            data-testid="admin-media-upload-submit"
          >
            <Upload className="h-4 w-4" aria-hidden="true" />
            Yükle
          </Button>
        </form>
      </Card>

      {items.length === 0 ? (
        <div className="mt-8 rounded-lg border border-slate-200 bg-white p-8 text-sm text-slate-600" data-testid="admin-media-empty">
          Henüz medya yüklenmedi.
        </div>
      ) : (
        <div className="mt-8 space-y-4" data-testid="admin-media-list">
          {items.map((item) => (
            <MediaRow
              key={item.id}
              item={item}
              roomTypes={roomTypes}
              roomTypesDisabled={
                roomTypesQuery.isLoading || roomTypesQuery.isError
              }
              mutations={mutations}
              onMove={move}
              run={run}
            />
          ))}
        </div>
      )}
    </main>
  );
}

function MediaRow({
  item,
  roomTypes,
  roomTypesDisabled,
  mutations,
  onMove,
  run,
}: {
  item: MediaItem;
  roomTypes: Array<{ id: string; nameTr: string; nameEn: string }>;
  roomTypesDisabled: boolean;
  mutations: ReturnType<typeof useMediaMutations>;
  onMove: (item: MediaItem, direction: -1 | 1) => void;
  run: (action: () => Promise<unknown>) => Promise<void>;
}) {
  const [altTr, setAltTr] = useState(item.altTextTr ?? "");
  const [altEn, setAltEn] = useState(item.altTextEn ?? "");

  return (
    <Card className="p-4" data-testid="admin-media-item">
      <div className="grid gap-4 lg:grid-cols-12 lg:items-center">
        <div className="lg:col-span-3">
          {item.variants[0]?.url ? (
            // Presigned admin preview; not a public/unpublished URL.
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={item.variants.find((variant) => variant.variant === "thumb")?.url ?? item.variants[0].url}
              alt=""
              className="h-24 w-full rounded object-cover"
            />
          ) : (
            <div className="flex h-24 items-center justify-center rounded bg-slate-100 text-xs text-slate-500">
              Önizleme yok
            </div>
          )}
        </div>
        <div className="lg:col-span-4">
          <div className="flex flex-wrap items-center gap-2">
            <span
              className="rounded-full bg-slate-100 px-2 py-1 text-xs font-semibold"
              data-testid="admin-media-status"
            >
              {statusLabel(item.status)}
            </span>
            {item.cover && (
              <span
                className="text-xs font-semibold text-amber-700"
                data-testid="admin-media-cover-badge"
              >
                Kapak
              </span>
            )}
            {item.published && <span className="text-xs text-emerald-700">Yayında</span>}
          </div>
          {item.failureCode && (
            <p className="mt-2 text-xs text-red-700">{item.failureCode}</p>
          )}
          <p className="mt-2 text-xs text-slate-500">
            {item.original.mimeType} · {Math.round(item.original.sizeBytes / 1024)} KB
          </p>
          <label className="mt-3 block text-xs text-slate-600">
            Oda tipi
            <select
              className="mt-1 w-full rounded border border-slate-300 p-1.5 text-sm"
              data-testid="admin-media-room-type"
              value={item.roomTypeId ?? ""}
              disabled={item.published || roomTypesDisabled}
              onChange={(event) => {
                if (!event.target.value) {
                  return;
                }
                void run(() =>
                  mutations.update.mutateAsync({
                    id: item.id,
                    roomTypeId: event.target.value,
                    altTextTr: altTr,
                    altTextEn: altEn,
                  }),
                );
              }}
            >
              <option value="" disabled>
                Atanmadı
              </option>
              {roomTypes.map((room) => (
                <option key={room.id} value={room.id}>
                  {room.nameTr}
                </option>
              ))}
            </select>
            {item.published && (
              <p
                className="mt-1 text-xs text-amber-700"
                data-testid="admin-media-room-type-locked"
              >
                Yayındaki medyanın oda tipi değiştirilemez; önce yayından
                kaldırın.
              </p>
            )}
          </label>
        </div>
        <div className="lg:col-span-3">
          <label className="block text-xs text-slate-600">
            TR alt metin
            <input
              className="mt-1 w-full rounded border border-slate-300 p-1.5 text-sm"
              data-testid="admin-media-alt-tr"
              maxLength={200}
              value={altTr}
              onChange={(event) => setAltTr(event.target.value)}
            />
          </label>
          <label className="mt-2 block text-xs text-slate-600">
            EN alt metin
            <input
              className="mt-1 w-full rounded border border-slate-300 p-1.5 text-sm"
              data-testid="admin-media-alt-en"
              maxLength={200}
              value={altEn}
              onChange={(event) => setAltEn(event.target.value)}
            />
          </label>
          {item.published && (!item.altTextTr || !item.altTextEn) && (
            <p
              className="mt-2 text-xs text-amber-700"
              data-testid="admin-media-alt-warning"
              role="status"
            >
              Yayındaki görselde eksik alt metin var; public sitede oda tipi adı
              kullanılacak.
            </p>
          )}
          <Button
            className="mt-2"
            variant="secondary"
            onClick={() =>
              void run(() =>
                mutations.update.mutateAsync({
                  id: item.id,
                  roomTypeId: item.roomTypeId ?? undefined,
                  altTextTr: altTr,
                  altTextEn: altEn,
                }),
              )
            }
          >
            Alt metni kaydet
          </Button>
        </div>
        <div className="flex flex-wrap gap-2 lg:col-span-2">
          <Button
            size="sm"
            variant="secondary"
            data-testid="admin-media-order-up"
            disabled={!item.roomTypeId}
            onClick={() => onMove(item, -1)}
          >
            ↑
          </Button>
          <Button
            size="sm"
            variant="secondary"
            data-testid="admin-media-order-down"
            disabled={!item.roomTypeId}
            onClick={() => onMove(item, 1)}
          >
            ↓
          </Button>
          {item.status === "READY" && item.published && !item.cover && (
            <Button
              size="sm"
              variant="secondary"
              data-testid="admin-media-cover"
              onClick={() => void run(() => mutations.cover.mutateAsync(item.id))}
            >
              Kapak yap
            </Button>
          )}
          {item.status === "READY" && !item.published && item.roomTypeId && (
            <Button
              size="sm"
              data-testid="admin-media-publish"
              onClick={() => void run(() => mutations.publish.mutateAsync(item.id))}
            >
              Yayınla
            </Button>
          )}
          {item.published && (
            <Button
              size="sm"
              variant="secondary"
              data-testid="admin-media-unpublish"
              onClick={() => void run(() => mutations.unpublish.mutateAsync(item.id))}
            >
              Yayından kaldır
            </Button>
          )}
          {item.status === "FAILED" && (
            <Button
              size="sm"
              data-testid="admin-media-retry-processing"
              onClick={() => void run(() => mutations.retry.mutateAsync(item.id))}
            >
              Yeniden dene
            </Button>
          )}
          <Button
            size="sm"
            variant="secondary"
            data-testid="admin-media-archive"
            onClick={() => {
              if (
                window.confirm(
                  "Arşivlenen medya geri alınamaz ve public siteden kaldırılır. Devam edilsin mi?",
                )
              ) {
                void run(() => mutations.archive.mutateAsync(item.id));
              }
            }}
          >
            Arşivle
          </Button>
        </div>
      </div>
    </Card>
  );
}
