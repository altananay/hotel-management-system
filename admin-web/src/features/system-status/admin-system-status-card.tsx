"use client";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { AdminApiClientError } from "@/lib/api-client";
import { resolveAdminErrorMessage } from "@/lib/error-messages";
import { useAdminSampleItemsQuery } from "./use-admin-sample-items-query";

export function AdminSystemStatusCard() {
  const { data, isLoading, isError, error, refetch } = useAdminSampleItemsQuery();

  return (
    <Card className="mx-auto max-w-2xl" data-testid="admin-status-card">
      <h2
        className="mb-4 text-lg font-semibold text-slate-900"
        data-testid="admin-status-heading"
      >
        Yönetim Paneli Sistem Durumu
      </h2>

      {isLoading && (
        <div
          className="space-y-3 py-4"
          role="status"
          aria-live="polite"
          data-testid="admin-status-loading"
        >
          <p className="text-sm font-medium text-slate-700">
            Yönetim verileri yükleniyor...
          </p>
          <p className="text-xs text-slate-500">
            Operasyonel kayıtlar ve özet bilgiler hazırlanıyor.
          </p>
          <Skeleton className="h-4 w-3/4" />
          <Skeleton className="h-4 w-1/2" />
        </div>
      )}

      {isError && (
        <div
          className="rounded-lg border border-red-200 bg-red-50 p-4 text-red-900"
          role="alert"
          data-testid="admin-status-error"
        >
          <p className="font-medium" data-testid="admin-status-error-title">
            Yönetim verileri alınamadı
          </p>
          <p className="mt-1 text-sm text-red-700" data-testid="admin-status-error-message">
            {resolveAdminErrorMessage(
              error instanceof AdminApiClientError ? error.code : undefined,
            )}
          </p>
          <Button
            className="mt-4 bg-red-800 hover:bg-red-700"
            onClick={() => void refetch()}
            data-testid="admin-status-retry"
          >
            Tekrar Dene
          </Button>
        </div>
      )}

      {!isLoading && !isError && data && data.items.length === 0 && (
        <div
          className="rounded-lg border border-slate-200 bg-slate-50 p-6 text-center"
          data-testid="admin-status-empty"
        >
          <p className="font-medium text-slate-800" data-testid="admin-status-empty-title">
            Henüz görüntülenecek operasyonel kayıt bulunmuyor
          </p>
          <p className="mt-1 text-sm text-slate-600">
            Yönetim modülleri etkinleştirildiğinde özet veriler burada listelenecektir.
          </p>
        </div>
      )}

      {!isLoading && !isError && data && data.items.length > 0 && (
        <ul className="divide-y divide-slate-200" data-testid="admin-status-list">
          {data.items.map((item) => (
            <li key={item.id} className="py-2 text-sm text-slate-800">
              {item.name}
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}
