import { ArrowRight, Compass, Flame, Sparkles } from "lucide-react";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Card } from "@/components/ui/card";
import { getDictionary, isValidLocale } from "@/lib/i18n";

interface LocalePageProps {
  params: Promise<{ locale: string }>;
}

export default async function LocalePage({ params }: LocalePageProps) {
  const { locale } = await params;
  if (!isValidLocale(locale)) {
    notFound();
  }

  const dict = getDictionary(locale);

  return (
    <main className="mx-auto max-w-6xl px-6 py-10 sm:py-14" data-testid="web-home-page">
      <header className="mb-10 flex flex-col justify-between gap-4 border-b border-stone-200/90 pb-6 sm:flex-row sm:items-center">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-stone-900" data-testid="web-site-title">
            {dict.siteTitle}
          </h1>
          <p className="mt-1 text-sm text-stone-600" data-testid="web-site-subtitle">
            {dict.siteSubtitle}
          </p>
        </div>

        <nav
          aria-label={dict.languageLabel}
          className="flex items-center gap-2"
          data-testid="locale-switcher"
        >
          <span className="text-xs font-medium text-stone-500">{dict.languageLabel}:</span>
          <Link
            href="/tr"
            data-testid="locale-switch-tr"
            className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition-colors ${
              locale === "tr"
                ? "bg-stone-900 text-white"
                : "bg-stone-200/80 text-stone-700 hover:bg-stone-300"
            }`}
          >
            {dict.switchTr}
          </Link>
          <Link
            href="/en"
            data-testid="locale-switch-en"
            className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition-colors ${
              locale === "en"
                ? "bg-stone-900 text-white"
                : "bg-stone-200/80 text-stone-700 hover:bg-stone-300"
            }`}
          >
            {dict.switchEn}
          </Link>
        </nav>
      </header>

      {/* Hero Welcome & Room Showcase Transition */}
      <section className="relative overflow-hidden rounded-3xl border border-amber-900/20 bg-gradient-to-br from-stone-950 via-stone-900 to-amber-950 px-8 py-14 text-stone-100 shadow-xl sm:px-12 sm:py-20">
        <div className="relative z-10 max-w-2xl">
          <span className="inline-flex items-center gap-2 rounded-full border border-amber-300/30 bg-amber-500/15 px-3.5 py-1 text-xs font-semibold tracking-wider text-amber-200">
            <Sparkles className="h-3.5 w-3.5 text-amber-300" aria-hidden="true" />
            {dict.home.badge}
          </span>

          <h2 className="mt-5 text-3xl font-bold tracking-tight text-white sm:text-5xl sm:leading-tight">
            {dict.home.heroTitle}
          </h2>

          <p className="mt-5 text-base leading-relaxed text-stone-300 sm:text-lg">
            {dict.home.heroDescription}
          </p>

          <div className="mt-8">
            <Link
              href={`/${locale}/rooms`}
              data-testid="home-rooms-link"
              className="inline-flex items-center gap-2.5 rounded-xl bg-amber-500 px-6 py-3.5 text-sm font-semibold text-stone-950 shadow-md transition-all hover:bg-amber-400 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amber-300"
            >
              {dict.home.exploreRoomsCta}
              <ArrowRight className="h-4 w-4" aria-hidden="true" />
            </Link>
          </div>
        </div>
      </section>

      {/* Boutique Highlights */}
      <section className="mt-10 grid gap-6 sm:grid-cols-3">
        <Card className="rounded-2xl border border-stone-200/90 bg-white/95 p-6 shadow-xs">
          <div className="mb-3 inline-flex h-10 w-10 items-center justify-center rounded-xl bg-amber-100/80 text-amber-900">
            <Sparkles className="h-5 w-5" aria-hidden="true" />
          </div>
          <h3 className="text-base font-bold text-stone-900">
            {dict.home.highlights.architectureTitle}
          </h3>
          <p className="mt-2 text-sm leading-relaxed text-stone-600">
            {dict.home.highlights.architectureDesc}
          </p>
        </Card>

        <Card className="rounded-2xl border border-stone-200/90 bg-white/95 p-6 shadow-xs">
          <div className="mb-3 inline-flex h-10 w-10 items-center justify-center rounded-xl bg-amber-100/80 text-amber-900">
            <Compass className="h-5 w-5" aria-hidden="true" />
          </div>
          <h3 className="text-base font-bold text-stone-900">
            {dict.home.highlights.terraceTitle}
          </h3>
          <p className="mt-2 text-sm leading-relaxed text-stone-600">
            {dict.home.highlights.terraceDesc}
          </p>
        </Card>

        <Card className="rounded-2xl border border-stone-200/90 bg-white/95 p-6 shadow-xs">
          <div className="mb-3 inline-flex h-10 w-10 items-center justify-center rounded-xl bg-amber-100/80 text-amber-900">
            <Flame className="h-5 w-5" aria-hidden="true" />
          </div>
          <h3 className="text-base font-bold text-stone-900">
            {dict.home.highlights.hospitalityTitle}
          </h3>
          <p className="mt-2 text-sm leading-relaxed text-stone-600">
            {dict.home.highlights.hospitalityDesc}
          </p>
        </Card>
      </section>
    </main>
  );
}
