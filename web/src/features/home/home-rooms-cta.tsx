import { ArrowRight } from "lucide-react";
import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { Section } from "@/components/ui/section";
import type { Dictionary, Locale } from "@/lib/i18n";

export function HomeRoomsCta({
  locale,
  dict,
}: {
  locale: Locale;
  dict: Dictionary;
}) {
  const copy = dict.home.roomsCta;

  return (
    <Section
      as="section"
      id="home-rooms-cta"
      data-testid="home-rooms-cta"
      data-motion-scene="home-rooms-cta"
      data-motion="static"
      tone="paper"
      spacing="compact"
      container={false}
      className="overflow-hidden"
    >
      <div className="site-container">
        <div className="relative overflow-hidden rounded-[2rem] bg-surface-inverse px-6 py-14 text-fg-inverse sm:px-10 lg:px-16 lg:py-20">
          <div
            aria-hidden="true"
            className="paper-grain absolute inset-0 opacity-35"
          />
          <div
            aria-hidden="true"
            className="absolute -right-24 -top-24 h-80 w-80 rounded-full bg-accent/20 blur-3xl"
          />

          <div className="relative z-10 grid gap-12 lg:grid-cols-12 lg:items-end">
            <div className="lg:col-span-7" data-reveal>
              <Badge tone="inverse">{copy.eyebrow}</Badge>
              <h2 className="mt-6 max-w-2xl text-display-lg text-fg-inverse">
                {copy.title}
              </h2>
              <p className="mt-6 max-w-xl text-body leading-relaxed text-fg-inverse-muted">
                {copy.body}
              </p>
              <div className="mt-9">
                <Link
                  href={`/${locale}/rooms`} prefetch={false}
                  data-testid="home-rooms-link"
                  className="inline-flex items-center gap-3 rounded-full bg-accent-contrast px-7 py-4 text-small font-semibold text-surface-inverse transition-fast hover:bg-accent-soft"
                >
                  {copy.button}
                  <ArrowRight className="h-4 w-4" aria-hidden="true" />
                </Link>
              </div>
            </div>

            <div
              aria-hidden="true"
              className="relative hidden h-64 items-end justify-center gap-4 lg:col-span-5 lg:flex"
            >
              <div className="h-36 w-24 rounded-[999px_999px_0_0] border border-fg-inverse/20" />
              <div className="h-52 w-32 rounded-[999px_999px_0_0] border border-accent-soft/45 bg-accent/10" />
              <div className="h-40 w-24 rounded-[999px_999px_0_0] border border-fg-inverse/20" />
              <div className="absolute bottom-0 left-1/2 h-px w-64 -translate-x-1/2 bg-gradient-to-r from-transparent via-accent-soft/50 to-transparent" />
            </div>
          </div>
        </div>
      </div>
    </Section>
  );
}
