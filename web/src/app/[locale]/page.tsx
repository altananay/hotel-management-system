import Link from "next/link";
import { notFound } from "next/navigation";
import { SystemStatusCard } from "@/features/system-status/system-status-card";
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
    <main className="mx-auto max-w-4xl px-6 py-12" data-testid="web-home-page">
      <header className="mb-10 flex flex-col justify-between gap-4 border-b border-stone-200 pb-6 sm:flex-row sm:items-center">
        <div>
          <h1 className="text-2xl font-bold text-stone-900" data-testid="web-site-title">
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
            className={`rounded px-3 py-1 text-xs font-semibold ${
              locale === "tr"
                ? "bg-stone-900 text-white"
                : "bg-stone-200 text-stone-700 hover:bg-stone-300"
            }`}
          >
            {dict.switchTr}
          </Link>
          <Link
            href="/en"
            data-testid="locale-switch-en"
            className={`rounded px-3 py-1 text-xs font-semibold ${
              locale === "en"
                ? "bg-stone-900 text-white"
                : "bg-stone-200 text-stone-700 hover:bg-stone-300"
            }`}
          >
            {dict.switchEn}
          </Link>
        </nav>
      </header>

      <SystemStatusCard locale={locale} />
    </main>
  );
}
