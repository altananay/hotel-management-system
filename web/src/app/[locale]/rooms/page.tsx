import { ArrowLeft, Sparkles } from "lucide-react";
import Link from "next/link";
import { notFound } from "next/navigation";
import { RoomShowcase } from "@/features/room-catalog/room-showcase";
import { getDictionary, isValidLocale } from "@/lib/i18n";

interface RoomsPageProps {
  params: Promise<{ locale: string }>;
}

export default async function RoomsPage({ params }: RoomsPageProps) {
  const { locale } = await params;
  if (!isValidLocale(locale)) {
    notFound();
  }

  const dict = getDictionary(locale);

  return (
    <main
      className="mx-auto max-w-6xl px-6 py-10 sm:py-14"
      data-testid="rooms-page"
    >
      <header className="mb-10 flex flex-col justify-between gap-4 border-b border-stone-200/90 pb-6 sm:flex-row sm:items-center">
        <div>
          <Link
            href={`/${locale}`}
            className="mb-2 inline-flex items-center gap-1.5 text-xs font-semibold text-amber-900 transition-colors hover:text-amber-700"
          >
            <ArrowLeft className="h-3.5 w-3.5" aria-hidden="true" />
            {dict.rooms.backToHome}
          </Link>
          <p className="text-lg font-bold tracking-tight text-stone-900" data-testid="web-site-title">
            {dict.siteTitle}
          </p>
          <p className="text-xs text-stone-600" data-testid="web-site-subtitle">
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
            href="/tr/rooms"
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
            href="/en/rooms"
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

      <section className="mb-10 rounded-3xl border border-amber-900/15 bg-gradient-to-r from-stone-900 via-stone-800 to-amber-950 px-8 py-10 text-stone-100 shadow-lg sm:px-10 sm:py-12">
        <span className="inline-flex items-center gap-1.5 rounded-full border border-amber-300/30 bg-amber-500/15 px-3 py-1 text-xs font-semibold tracking-wider text-amber-200">
          <Sparkles className="h-3.5 w-3.5 text-amber-300" aria-hidden="true" />
          {dict.rooms.collectionBadge}
        </span>
        <h1 className="mt-3 text-2xl font-bold tracking-tight text-white sm:text-4xl">
          {dict.rooms.pageTitle}
        </h1>
        <p className="mt-2 max-w-2xl text-sm leading-relaxed text-stone-300 sm:text-base">
          {dict.rooms.pageSubtitle}
        </p>
      </section>

      <RoomShowcase locale={locale} />
    </main>
  );
}
