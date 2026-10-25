import { ArrowUpRight } from "lucide-react";
import Link from "next/link";
import type { Dictionary } from "@/lib/i18n";

export interface SiteFooterProps {
  locale: "tr" | "en";
  dict: Dictionary;
}

export function SiteFooter({ locale, dict }: SiteFooterProps) {
  const { contact } = dict.home;

  return (
    <footer
      data-testid="site-footer"
      className="relative overflow-hidden bg-surface-inverse text-fg-inverse"
    >
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-accent-soft/45 to-transparent"
      />
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -right-24 top-10 h-72 w-72 rounded-full bg-accent/10 blur-3xl"
      />

      <div className="site-container relative py-section-sm">
        <div className="grid gap-12 lg:grid-cols-12 lg:gap-8">
          <div className="lg:col-span-5">
            <Link
              href={`/${locale}`}
              className="inline-flex items-baseline gap-3 transition-fast hover:text-accent-soft"
            >
              <span
                aria-hidden="true"
                className="h-8 w-px bg-accent-soft/50"
              />
              <span className="font-display text-3xl leading-none tracking-tight sm:text-4xl">
                {dict.siteTitle}
              </span>
            </Link>
            <p className="mt-5 max-w-sm text-small leading-relaxed text-fg-inverse-muted">
              {dict.footer.tagline}
            </p>
          </div>

          <div className="lg:col-span-4">
            <h2 className="text-eyebrow font-semibold uppercase tracking-[0.18em] text-fg-inverse-muted">
              {dict.footer.contactLabel}
            </h2>
            <address className="mt-5 space-y-3 text-small not-italic text-fg-inverse-muted">
              <p data-testid="footer-address">{contact.address}</p>
              <p>
                <a
                  data-testid="footer-phone"
                  href={`tel:${contact.phone.replace(/\s+/g, "")}`}
                  className="underline decoration-fg-inverse/25 underline-offset-4 transition-fast hover:text-fg-inverse hover:decoration-accent-soft"
                >
                  {contact.phone}
                </a>
              </p>
              <p>
                <a
                  data-testid="footer-email"
                  href={`mailto:${contact.email}`}
                  className="underline decoration-fg-inverse/25 underline-offset-4 transition-fast hover:text-fg-inverse hover:decoration-accent-soft"
                >
                  {contact.email}
                </a>
              </p>
            </address>
          </div>

          <div className="lg:col-span-3 lg:text-right">
            <Link
              href={`/${locale}/credits`}
              data-testid="footer-credits-link"
              className="inline-flex items-center gap-2 border-b border-fg-inverse/25 pb-1 text-small font-semibold text-fg-inverse transition-fast hover:border-accent-soft hover:text-accent-soft"
            >
              {dict.footer.creditsLabel}
              <ArrowUpRight className="h-4 w-4" aria-hidden="true" />
            </Link>
          </div>
        </div>

        <div className="mt-14 flex flex-col gap-3 border-t border-fg-inverse/15 pt-6 text-xs text-fg-inverse-muted sm:flex-row sm:items-center sm:justify-between">
          <p data-testid="footer-copyright">{dict.footer.copyright}</p>
          <p>{dict.siteSubtitle}</p>
        </div>
      </div>
    </footer>
  );
}
