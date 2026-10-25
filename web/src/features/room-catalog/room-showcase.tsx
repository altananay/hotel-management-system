"use client";

import { RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { RoomCard } from "@/components/ui/room-card";
import { Skeleton } from "@/components/ui/skeleton";
import { StatePanel } from "@/components/ui/state-panel";
import { getDictionary, type Locale } from "@/lib/i18n";
import type { RoomTypeDto } from "./schema";
import { useRoomTypesQuery } from "./use-room-types-query";

interface RoomShowcaseProps {
  locale: Locale;
}

function LoadingShowcase({ label }: { label: string }) {
  return (
    <StatePanel
      state="loading"
      testId="rooms-loading"
      title={label}
      className="max-w-none border-0 bg-transparent p-0 text-left shadow-none"
    >
      <div className="mt-6 space-y-6">
        {[0, 1].map((index) => (
          <Card
            key={index}
            className="rounded-3xl border-border bg-surface-raised/80 p-6 sm:p-8"
          >
            <div className="grid gap-6 lg:grid-cols-12">
              <Skeleton className="h-56 w-full rounded-[1.5rem_1.5rem_0.5rem_0.5rem] lg:col-span-5" />
              <div className="space-y-4 lg:col-span-7">
                <div className="flex justify-between gap-4">
                  <Skeleton className="h-8 w-2/3" />
                  <Skeleton className="h-8 w-28 rounded-full" />
                </div>
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-5/6" />
                <Skeleton className="h-4 w-2/3" />
                <div className="flex gap-2 pt-4">
                  <Skeleton className="h-8 w-28 rounded-full" />
                  <Skeleton className="h-8 w-32 rounded-full" />
                  <Skeleton className="h-8 w-24 rounded-full" />
                </div>
              </div>
            </div>
          </Card>
        ))}
      </div>
    </StatePanel>
  );
}

export function RoomShowcase({ locale }: RoomShowcaseProps) {
  const dict = getDictionary(locale);
  const { data, isLoading, isError, refetch } = useRoomTypesQuery();

  if (isLoading) {
    return <LoadingShowcase label={dict.rooms.loadingText} />;
  }

  if (isError) {
    return (
      <StatePanel
        state="error"
        testId="rooms-error"
        messageTestId="rooms-error-message"
        message={dict.rooms.errorMessage}
        action={
          <Button
            data-testid="rooms-retry"
            variant="secondary"
            onClick={() => void refetch()}
            className="border-danger-border bg-danger-fg text-accent-contrast hover:border-danger-fg hover:bg-danger-fg hover:text-accent-contrast"
          >
            <RefreshCw className="h-4 w-4" aria-hidden="true" />
            {dict.rooms.retryButton}
          </Button>
        }
      />
    );
  }

  if (!data || data.items.length === 0) {
    return (
      <StatePanel
        state="empty"
        testId="rooms-empty"
        titleTestId="rooms-empty-title"
        title={dict.rooms.emptyTitle}
        message={dict.rooms.emptySubtitle}
      />
    );
  }

  return (
    <div data-testid="rooms-list" className="space-y-6">
      {data.items.map((room: RoomTypeDto) => (
        <RoomCard
          key={room.id}
          room={room}
          locale={locale}
          labels={{
            placeholderBadge: dict.rooms.placeholderBadge,
            placeholderCaption: dict.rooms.placeholderCaption,
            capacityTemplate: dict.rooms.capacityBadge,
            amenitiesHeading: dict.rooms.amenitiesHeading,
          }}
        />
      ))}
    </div>
  );
}
