import { ArrowRight } from "lucide-react";
import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import type { Dictionary, Locale } from "@/lib/i18n";
import { HomeHeroMedia } from "./home-hero-media";

export function HomeHero({ locale, dict }: { locale: Locale; dict: Dictionary }) {
  return (
    <section
      id="home-hero"
      data-testid="home-hero"
      data-motion-scene="home-hero"
      data-motion="static"
      className="relative isolate flex min-h-[calc(100svh-5rem)] items-end overflow-hidden bg-surface-inverse text-fg-inverse"
    >
      <HomeHeroMedia />
      <div className="hero-vignette absolute inset-0" aria-hidden="true" />
      <div
        aria-hidden="true"
        className="absolute inset-x-0 top-0 h-40 bg-gradient-to-b from-surface-inverse/65 to-transparent"
      />

      <div className="site-container relative z-10 pb-14 pt-32 sm:pb-20 lg:pb-24">
        <div className="max-w-4xl">
          <Badge tone="inverse" className="backdrop-blur-sm">
            {dict.home.badge}
          </Badge>

          <h1 className="mt-7 max-w-4xl text-display-xl text-fg-inverse">
            {dict.home.heroTitle}
          </h1>

          <p className="mt-7 max-w-2xl text-body leading-relaxed text-fg-inverse-muted sm:text-lg">
            {dict.home.heroDescription}
          </p>

          <div className="mt-9 flex flex-wrap items-center gap-5">
            <Link
              href={`/${locale}/rooms`} prefetch={false}
              className="inline-flex items-center gap-3 rounded-full bg-accent px-7 py-4 text-small font-semibold text-accent-contrast transition-fast hover:bg-accent-strong"
            >
              {dict.home.exploreRoomsCta}
              <ArrowRight className="h-4 w-4" aria-hidden="true" />
            </Link>
          </div>
        </div>
      </div>

      <div className="pointer-events-none absolute bottom-8 right-gutter z-10 hidden items-center gap-4 lg:flex">
        <span className="h-px w-16 bg-fg-inverse/35" aria-hidden="true" />
        <span className="text-eyebrow font-semibold uppercase tracking-[0.18em] text-fg-inverse-muted">
          {dict.home.scrollHint}
        </span>
      </div>

      <div
        aria-hidden="true"
        className="absolute inset-x-0 bottom-0 h-px bg-gradient-to-r from-transparent via-accent-soft/50 to-transparent"
      />
    </section>
  );
}
