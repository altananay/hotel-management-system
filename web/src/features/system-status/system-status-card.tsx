"use client";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { ApiClientError } from "@/lib/api-client";
import { getDictionary, type Locale, resolveErrorMessage } from "@/lib/i18n";
import { useSampleItemsQuery } from "./use-sample-items-query";

interface SystemStatusCardProps {
  locale: Locale;
}

export function SystemStatusCard({ locale }: SystemStatusCardProps) {
  const dict = getDictionary(locale);
  const { data, isLoading, isError, error, refetch } = useSampleItemsQuery();

  return (
    <Card className="mx-auto max-w-2xl" data-testid="web-status-card">
      <h2
        className="mb-4 text-lg font-semibold text-stone-900"
        data-testid="web-status-heading"
      >
        {dict.statusSectionTitle}
      </h2>

      {isLoading && (
        <div
          className="space-y-3 py-4"
          role="status"
          aria-live="polite"
          data-testid="web-status-loading"
        >
          <p className="text-sm font-medium text-stone-700">{dict.loadingTitle}</p>
          <p className="text-xs text-stone-500">{dict.loadingDescription}</p>
          <Skeleton className="h-4 w-3/4" />
          <Skeleton className="h-4 w-1/2" />
        </div>
      )}

      {isError && (
        <div
          className="rounded-lg border border-red-200 bg-red-50 p-4 text-red-900"
          role="alert"
          data-testid="web-status-error"
        >
          <p className="font-medium" data-testid="web-status-error-title">
            {dict.errorTitle}
          </p>
          <p className="mt-1 text-sm text-red-700" data-testid="web-status-error-message">
            {resolveErrorMessage(
              locale,
              error instanceof ApiClientError ? error.code : undefined,
            )}
          </p>
          <Button
            className="mt-4 bg-red-800 hover:bg-red-700"
            onClick={() => void refetch()}
            data-testid="web-status-retry"
          >
            {dict.errorRetry}
          </Button>
        </div>
      )}

      {!isLoading && !isError && data && data.items.length === 0 && (
        <div
          className="rounded-lg border border-stone-200 bg-stone-50 p-6 text-center"
          data-testid="web-status-empty"
        >
          <p className="font-medium text-stone-800" data-testid="web-status-empty-title">
            {dict.emptyTitle}
          </p>
          <p className="mt-1 text-sm text-stone-600">{dict.emptyDescription}</p>
        </div>
      )}

      {!isLoading && !isError && data && data.items.length > 0 && (
        <ul className="divide-y divide-stone-200" data-testid="web-status-list">
          {data.items.map((item) => (
            <li key={item.id} className="py-2 text-sm text-stone-800">
              {item.name}
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}
