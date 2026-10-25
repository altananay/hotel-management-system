import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { Section } from "@/components/ui/section";
import { getCreditsContent, type CreditsEntry } from "@/lib/credits";
import { getDictionary, isValidLocale, type Locale } from "@/lib/i18n";
import { buildPageMetadata } from "@/lib/seo";

interface CreditsPageProps {
  params: Promise<{ locale: string }>;
}

export async function generateMetadata({
  params,
}: CreditsPageProps): Promise<Metadata> {
  const { locale } = await params;
  if (!isValidLocale(locale)) {
    return {};
  }
  return buildPageMetadata({
    locale: locale as Locale,
    route: "credits",
    dict: getDictionary(locale),
  });
}

function CreditList({ entries }: { entries: CreditsEntry[] }) {
  return (
    <ul className="space-y-4">
      {entries.map((entry) => (
        <li
          key={entry.text}
          className="border-b border-border pb-4 text-small leading-relaxed text-fg-muted last:border-0 last:pb-0"
        >
          {entry.segments.map((segment, index) =>
            segment.type === "link" && segment.href ? (
              <a
                key={`${segment.href}-${index}`}
                href={segment.href}
                target="_blank"
                rel="noreferrer"
                className="font-semibold text-accent-strong underline decoration-accent/35 underline-offset-4 transition-fast hover:text-accent hover:decoration-accent"
              >
                {segment.value}
              </a>
            ) : (
              <span key={`${segment.value}-${index}`}>{segment.value}</span>
            ),
          )}
        </li>
      ))}
    </ul>
  );
}

export default async function CreditsPage({ params }: CreditsPageProps) {
  const { locale } = await params;
  if (!isValidLocale(locale)) {
    notFound();
  }
  const dict = getDictionary(locale);
  const credits = getCreditsContent();

  return (
    <div data-testid="credits-page">
      <Section as="section" tone="raised" spacing="compact" className="overflow-hidden">
        <div className="grid gap-10 lg:grid-cols-12 lg:items-end">
          <div className="lg:col-span-8">
            <Badge tone="accent">{dict.siteTitle}</Badge>
            <h1 className="mt-6 max-w-3xl text-display-lg text-fg">
              {dict.credits.title}
            </h1>
            <p className="mt-6 max-w-2xl text-body leading-relaxed text-fg-muted">
              {dict.credits.intro}
            </p>
          </div>
          <div
            aria-hidden="true"
            className="relative hidden h-44 items-end justify-end gap-3 lg:col-span-4 lg:flex"
          >
            <div className="h-24 w-16 rounded-[999px_999px_0_0] border border-border-strong" />
            <div className="h-40 w-24 rounded-[999px_999px_0_0] border border-accent/45 bg-accent-soft/35" />
            <div className="h-28 w-16 rounded-[999px_999px_0_0] border border-border-strong" />
          </div>
        </div>
      </Section>

      <Section as="section" tone="paper" spacing="default">
        <div className="grid gap-10 lg:grid-cols-2 lg:gap-16">
          <Card className="rounded-3xl p-7 sm:p-9">
            <h2 className="text-heading text-fg">{dict.credits.mediaHeading}</h2>
            <div className="mt-6">
              <CreditList entries={credits.media} />
            </div>
          </Card>

          <Card className="rounded-3xl p-7 sm:p-9">
            <h2 className="text-heading text-fg">{dict.credits.typographyHeading}</h2>
            <div className="mt-6">
              <CreditList entries={credits.typography} />
            </div>
          </Card>
        </div>

        <div className="mt-10 border-l border-accent/45 pl-6">
          <h2 className="text-eyebrow font-semibold uppercase tracking-[0.18em] text-fg-muted">
            {dict.credits.contentNoteHeading}
          </h2>
          <p className="mt-3 max-w-2xl text-small leading-relaxed text-fg-muted">
            {dict.credits.contentNote}
          </p>
        </div>
      </Section>
    </div>
  );
}
