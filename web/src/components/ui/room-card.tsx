import { Sparkles, Users } from "lucide-react";
import { Badge } from "./badge";
import { Card } from "./card";
import type { Locale } from "@/lib/i18n";

export interface RoomCardRoom {
  id: string;
  nameTr: string;
  nameEn: string;
  descriptionTr: string;
  descriptionEn: string;
  maxGuests: number;
  amenities: Array<{
    code: string;
    nameTr: string;
    nameEn: string;
  }>;
}

export interface RoomCardLabels {
  placeholderBadge: string;
  placeholderCaption: string;
  capacityTemplate: string;
  amenitiesHeading: string;
}

export interface RoomCardProps {
  room: RoomCardRoom;
  locale: Locale;
  labels: RoomCardLabels;
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
      className="relative flex min-h-64 flex-col justify-between overflow-hidden rounded-[1.5rem_1.5rem_0.5rem_0.5rem] bg-surface-inverse p-6 text-fg-inverse"
    >
      <div className="paper-grain absolute inset-0 opacity-60" aria-hidden="true" />
      <div className="fine-grid absolute inset-0 opacity-25" aria-hidden="true" />
      <div
        className="absolute -top-16 right-[-4rem] h-56 w-56 rounded-full bg-accent/25 blur-3xl"
        aria-hidden="true"
      />

      <svg
        aria-hidden="true"
        viewBox="0 0 400 280"
        preserveAspectRatio="xMidYMax slice"
        className="pointer-events-none absolute inset-0 h-full w-full"
      >
        <path
          d="M0 280V0h400v280h-68V150c0-66-264-66-264 0v130Z"
          className="fill-surface"
          opacity="0.28"
        />
        <path
          d="M48 280l48-76 28 42 38-58 40 62 44-54 44 48 32 36Z"
          className="fill-accent-soft"
          opacity="0.14"
        />
        <circle cx="286" cy="84" r="24" className="fill-accent" opacity="0.6" />
        <rect
          x="281"
          y="110"
          width="10"
          height="7"
          rx="1.5"
          className="fill-accent-contrast"
          opacity="0.65"
        />
        <circle cx="132" cy="102" r="12" className="fill-accent-soft" opacity="0.45" />
        <path
          d="M112 280V166c0-38 88-38 88 0v114"
          fill="none"
          stroke="currentColor"
          strokeWidth="1"
          className="text-fg-inverse"
          opacity="0.18"
        />
      </svg>

      <div className="relative z-10 flex items-center justify-between gap-2">
        <Badge tone="inverse">
          <Sparkles className="h-3.5 w-3.5" aria-hidden="true" />
          {badge}
        </Badge>
      </div>

      <div className="relative z-10 mt-14">
        <div className="h-px w-12 bg-accent-contrast/50" />
        <p className="mt-3 max-w-xs text-small leading-relaxed text-fg-inverse-muted">
          {caption}
        </p>
      </div>
    </div>
  );
}

function formatCapacity(template: string, count: number): string {
  return template.replace("{count}", String(count));
}

export function RoomCard({ room, locale, labels }: RoomCardProps) {
  const roomName = locale === "tr" ? room.nameTr : room.nameEn;
  const roomDescription = locale === "tr" ? room.descriptionTr : room.descriptionEn;

  return (
    <Card
      data-testid="room-card"
      className="overflow-hidden rounded-3xl p-0 hover:border-border-strong hover:shadow-elevated"
    >
      <div className="grid gap-0 lg:grid-cols-12 lg:items-stretch">
        <div className="p-4 lg:col-span-5 lg:p-6">
          <RoomVisualPlaceholder
            badge={labels.placeholderBadge}
            caption={labels.placeholderCaption}
          />
        </div>

        <div className="flex flex-col justify-between p-6 lg:col-span-7 lg:p-8">
          <div>
            <div className="flex flex-wrap items-start justify-between gap-3">
              <h2
                data-testid="room-card-name"
                className="text-display-md text-fg"
              >
                {roomName}
              </h2>

              <span
                data-testid="room-card-capacity"
                className="inline-flex items-center gap-1.5 rounded-full border border-border-strong bg-surface-muted px-3.5 py-1.5 text-xs font-semibold text-fg"
              >
                <Users className="h-3.5 w-3.5 text-accent-strong" aria-hidden="true" />
                {formatCapacity(labels.capacityTemplate, room.maxGuests)}
              </span>
            </div>

            <p
              data-testid="room-card-description"
              className="mt-4 max-w-2xl text-body leading-relaxed text-fg-muted"
            >
              {roomDescription}
            </p>
          </div>

          <div className="mt-7 border-t border-border pt-5">
            <p className="mb-3 text-eyebrow font-semibold uppercase text-fg-muted">
              {labels.amenitiesHeading}
            </p>
            <ul data-testid="room-card-amenities" className="flex flex-wrap gap-2">
              {room.amenities.map((amenity) => {
                const amenityLabel = locale === "tr" ? amenity.nameTr : amenity.nameEn;
                return (
                  <li
                    key={amenity.code}
                    data-testid="room-card-amenity"
                    className="inline-flex items-center rounded-full border border-border bg-surface px-3 py-1.5 text-xs font-semibold text-fg-muted"
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

