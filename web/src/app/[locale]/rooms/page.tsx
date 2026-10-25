import type { Metadata } from "next";
import { ArrowLeft } from "lucide-react";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Badge } from "@/components/ui/badge";
import { Section } from "@/components/ui/section";
import { RoomShowcase } from "@/features/room-catalog/room-showcase";
import { getDictionary, isValidLocale, type Locale } from "@/lib/i18n";
import { buildPageMetadata } from "@/lib/seo";

interface RoomsPageProps {
  params: Promise<{ locale: string }>;
}

export async function generateMetadata({
  params,
}: RoomsPageProps): Promise<Metadata> {
  const { locale } = await params;
  if (!isValidLocale(locale)) {
    return {};
  }
  return buildPageMetadata({
    locale: locale as Locale,
    route: "rooms",
    dict: getDictionary(locale),
  });
}

export default async function RoomsPage({ params }: RoomsPageProps) {
  const { locale } = await params;
  if (!isValidLocale(locale)) {
    notFound();
  }
  const dict = getDictionary(locale);

  return (
    <div data-testid="rooms-page">
      <Section
        as="section"
        tone="raised"
        spacing="compact"
        className="overflow-hidden"
      >
        <div className="grid gap-10 lg:grid-cols-12 lg:items-end">
          <div className="lg:col-span-8">
            <Link
              href={`/${locale}`}
              className="mb-8 inline-flex items-center gap-1.5 text-small font-semibold text-fg-muted transition-fast hover:text-accent-strong"
            >
              <ArrowLeft className="h-3.5 w-3.5" aria-hidden="true" />
              {dict.rooms.backToHome}
            </Link>
            <div>
              <Badge tone="accent">{dict.rooms.collectionBadge}</Badge>
            </div>
            <h1 className="mt-6 max-w-3xl text-display-lg text-fg">
              {dict.rooms.pageTitle}
            </h1>
            <p className="mt-6 max-w-2xl text-body leading-relaxed text-fg-muted">
              {dict.rooms.pageSubtitle}
            </p>
          </div>

          <div
            aria-hidden="true"
            className="relative hidden h-56 items-end justify-center gap-3 lg:col-span-4 lg:flex"
          >
            <div className="h-28 w-16 rounded-[999px_999px_0_0] border border-border-strong" />
            <div className="h-48 w-24 rounded-[999px_999px_0_0] border border-accent/45 bg-accent-soft/35" />
            <div className="h-36 w-20 rounded-[999px_999px_0_0] border border-border-strong" />
          </div>
        </div>
      </Section>

      <Section as="section" tone="paper" spacing="default">
        <RoomShowcase locale={locale} />
      </Section>
    </div>
  );
}
