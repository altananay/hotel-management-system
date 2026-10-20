"use client";

import { AlertCircle, Compass, RefreshCw, Sparkles, Users } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { ApiClientError } from "@/lib/api-client";
import { getDictionary, type Locale, resolveErrorMessage } from "@/lib/i18n";
import type { RoomTypeDto } from "./schema";
import { useRoomTypesQuery } from "./use-room-types-query";

interface RoomShowcaseProps {
  locale: Locale;
}

function formatCapacity(template: string, count: number): string {
  return template.replace("{count}", String(count));
}

function RoomVisualPlaceholder({
  badge,
  caption,
}: {
  badge: string;
  caption: string;
}) {
  return (
    <div
      data-testid="room-card-placeholder"
      className="relative flex min-h-60 flex-col justify-between overflow-hidden rounded-2xl border border-amber-900/15 bg-gradient-to-br from-stone-900 via-stone-800 to-amber-950 p-6 text-stone-100 shadow-inner"
    >
      {/* Decorative Cappadocia arch & hot-air balloon silhouette */}
      <svg
        aria-hidden="true"
        viewBox="0 0 400 240"
        className="pointer-events-none absolute inset-0 h-full w-full object-cover opacity-25"
      >
        <defs>
          <radialGradient id="sunGlow" cx="75%" cy="25%" r="55%">
            <stop offset="0%" stopColor="#f59e0b" stopOpacity="0.65" />
            <stop offset="100%" stopColor="#1c1917" stopOpacity="0" />
          </radialGradient>
        </defs>
        <rect width="400" height="240" fill="url(#sunGlow)" />
        {/* Archway silhouette */}
        <path
          d="M0,240 L0,0 L400,0 L400,240 L330,240 L330,115 C330,45 70,45 70,115 L70,240 Z"
          fill="#0c0a09"
          opacity="0.55"
        />
        {/* Distant fairy chimneys & valley */}
        <path
          d="M50,240 L95,165 L125,210 L165,145 L205,215 L255,155 L305,205 L355,240 Z"
          fill="#d6d3d1"
          opacity="0.28"
        />
        {/* Hot-air balloon */}
        <circle cx="285" cy="78" r="22" fill="#f59e0b" opacity="0.7" />
        <rect x="280" y="104" width="10" height="7" rx="1.5" fill="#fde68a" opacity="0.65" />
        <circle cx="135" cy="96" r="12" fill="#fbbf24" opacity="0.5" />
      </svg>

      <div className="relative z-10 flex items-center justify-between gap-2">
        <span className="inline-flex items-center gap-1.5 rounded-full border border-amber-300/30 bg-amber-500/15 px-3 py-1 text-xs font-medium tracking-wide text-amber-200 backdrop-blur-xs">
          <Sparkles className="h-3.5 w-3.5 text-amber-300" aria-hidden="true" />
          {badge}
        </span>
      </div>

      <div className="relative z-10 mt-10">
        <div className="h-px w-12 bg-amber-400/50" />
        <p className="mt-2.5 text-xs leading-relaxed text-stone-300/90">{caption}</p>
      </div>
    </div>
  );
}

function RoomTypeCard({ room, locale }: { room: RoomTypeDto; locale: Locale }) {
  const dict = getDictionary(locale);
  const roomName = locale === "tr" ? room.nameTr : room.nameEn;
  const roomDescription = locale === "tr" ? room.descriptionTr : room.descriptionEn;

  return (
    <Card
      data-testid="room-card"
      className="group overflow-hidden rounded-3xl border border-stone-200/90 bg-white/95 p-6 shadow-md transition-all duration-300 hover:-translate-y-0.5 hover:border-amber-800/30 hover:shadow-xl sm:p-8"
    >
      <div className="grid gap-6 lg:grid-cols-12 lg:items-stretch">
        <div className="lg:col-span-5">
          <RoomVisualPlaceholder
            badge={dict.rooms.placeholderBadge}
            caption={dict.rooms.placeholderCaption}
          />
        </div>

        <div className="flex flex-col justify-between lg:col-span-7">
          <div>
            <div className="flex flex-wrap items-start justify-between gap-3">
              <h2
                data-testid="room-card-name"
                className="text-xl font-bold tracking-tight text-stone-900 sm:text-2xl"
              >
                {roomName}
              </h2>

              <span
                data-testid="room-card-capacity"
                className="inline-flex items-center gap-1.5 rounded-full border border-amber-900/20 bg-amber-50 px-3.5 py-1.5 text-xs font-semibold text-amber-950"
              >
                <Users className="h-3.5 w-3.5 text-amber-800" aria-hidden="true" />
                {formatCapacity(dict.rooms.capacityBadge, room.maxGuests)}
              </span>
            </div>

            <p
              data-testid="room-card-description"
              className="mt-4 text-sm leading-relaxed text-stone-600 sm:text-base"
            >
              {roomDescription}
            </p>
          </div>

          <div className="mt-6 border-t border-stone-200/80 pt-5">
            <p className="mb-3 text-xs font-semibold tracking-wider text-stone-500 uppercase">
              {dict.rooms.amenitiesHeading}
            </p>
            <ul
              data-testid="room-card-amenities"
              className="flex flex-wrap gap-2"
            >
              {room.amenities.map((amenity) => {
                const amenityLabel = locale === "tr" ? amenity.nameTr : amenity.nameEn;
                return (
                  <li
                    key={amenity.code}
                    data-testid="room-card-amenity"
                    className="inline-flex items-center rounded-lg border border-stone-200 bg-stone-100/90 px-3 py-1.5 text-xs font-medium text-stone-800 transition-colors group-hover:border-amber-900/20 group-hover:bg-amber-50/60"
                  >
                    {amenityLabel}
                  </li>
                );
              })}
            </ul>
          </div>
        </div>
      </div>
    </Card>
  );
}

export function RoomShowcase({ locale }: RoomShowcaseProps) {
  const dict = getDictionary(locale);
  const { data, isLoading, isError, error, refetch } = useRoomTypesQuery();

  if (isLoading) {
    return (
      <section
        data-testid="rooms-loading"
        role="status"
        aria-live="polite"
        className="space-y-6"
      >
        <p className="text-sm font-medium text-stone-600">{dict.rooms.loadingText}</p>
        {[0, 1].map((idx) => (
          <Card
            key={idx}
            className="rounded-3xl border border-stone-200/80 bg-white/90 p-6 sm:p-8"
          >
            <div className="grid gap-6 lg:grid-cols-12">
              <Skeleton className="h-56 w-full rounded-2xl lg:col-span-5" />
              <div className="space-y-4 lg:col-span-7">
                <div className="flex justify-between gap-4">
                  <Skeleton className="h-7 w-2/3" />
                  <Skeleton className="h-7 w-28 rounded-full" />
                </div>
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-5/6" />
                <Skeleton className="h-4 w-2/3" />
                <div className="flex gap-2 pt-4">
                  <Skeleton className="h-7 w-28 rounded-lg" />
                  <Skeleton className="h-7 w-32 rounded-lg" />
                  <Skeleton className="h-7 w-24 rounded-lg" />
                </div>
              </div>
            </div>
          </Card>
        ))}
      </section>
    );
  }

  if (isError) {
    const detailedHint = resolveErrorMessage(
      locale,
      error instanceof ApiClientError ? error.code : undefined,
    );

    return (
      <Card
        data-testid="rooms-error"
        role="alert"
        className="mx-auto max-w-2xl rounded-3xl border border-red-200 bg-red-50/90 p-8 text-center text-red-950 shadow-sm"
      >
        <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-red-100 text-red-800">
          <AlertCircle className="h-6 w-6" aria-hidden="true" />
        </div>
        <p
          data-testid="rooms-error-message"
          className="text-base font-semibold text-red-950 sm:text-lg"
        >
          {dict.rooms.errorMessage}
        </p>
        <p className="mt-2 text-sm text-red-800/90">{detailedHint}</p>
        <div className="mt-6">
          <Button
            data-testid="rooms-retry"
            onClick={() => void refetch()}
            className="inline-flex items-center gap-2 rounded-xl bg-red-900 px-5 py-2.5 text-sm font-semibold text-white hover:bg-red-800"
          >
            <RefreshCw className="h-4 w-4" aria-hidden="true" />
            {dict.rooms.retryButton}
          </Button>
        </div>
      </Card>
    );
  }

  if (!data || data.items.length === 0) {
    return (
      <Card
        data-testid="rooms-empty"
        className="mx-auto max-w-2xl rounded-3xl border border-stone-200/90 bg-white/95 p-10 text-center shadow-sm"
      >
        <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-amber-100/80 text-amber-900">
          <Compass className="h-6 w-6" aria-hidden="true" />
        </div>
        <h2
          data-testid="rooms-empty-title"
          className="text-lg font-bold text-stone-900 sm:text-xl"
        >
          {dict.rooms.emptyTitle}
        </h2>
        <p className="mt-2 text-sm text-stone-600">{dict.rooms.emptySubtitle}</p>
      </Card>
    );
  }

  return (
    <div data-testid="rooms-list" className="space-y-6">
      {data.items.map((room) => (
        <RoomTypeCard key={room.id} room={room} locale={locale} />
      ))}
    </div>
  );
}
