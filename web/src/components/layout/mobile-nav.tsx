"use client";

import * as Dialog from "@radix-ui/react-dialog";
import { ArrowUpRight, Menu, X } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import type { Dictionary } from "@/lib/i18n";
import { LocaleSwitcher } from "./locale-switcher";

export interface MobileNavProps {
  locale: "tr" | "en";
  pathname: string;
  dict: Dictionary;
}

export function MobileNav({ locale, pathname, dict }: MobileNavProps) {
  const [open, setOpen] = useState(false);
  const isHome = pathname === `/${locale}`;
  const isRooms = pathname === `/${locale}/rooms`;

  useEffect(() => {
    setOpen(false);
  }, [pathname]);

  useEffect(() => {
    const media = window.matchMedia("(min-width: 1024px)");
    const handleChange = () => {
      if (media.matches) {
        setOpen(false);
      }
    };
    media.addEventListener("change", handleChange);
    return () => media.removeEventListener("change", handleChange);
  }, []);

  return (
    <Dialog.Root open={open} onOpenChange={setOpen}>
      <Dialog.Trigger asChild>
        <button
          type="button"
          data-testid="mobile-nav-toggle"
          aria-label={dict.mobileMenu.open}
          className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-border bg-surface-raised/70 text-fg transition-fast hover:border-border-strong hover:text-accent-strong lg:hidden"
        >
          <Menu className="h-5 w-5" aria-hidden="true" />
        </button>
      </Dialog.Trigger>

      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-overlay lg:hidden" />
        <Dialog.Content
          data-testid="mobile-nav"
          role="dialog"
          aria-modal="true"
          aria-describedby={undefined}
          className="fixed inset-0 z-50 flex flex-col overflow-y-auto bg-surface-inverse text-fg-inverse lg:hidden"
        >
          <Dialog.Title className="sr-only">{dict.nav.ariaLabel}</Dialog.Title>

          <div className="site-container flex h-20 shrink-0 items-center justify-between border-b border-fg-inverse/15">
            <span className="font-display text-xl leading-none tracking-tight text-fg-inverse">
              {dict.siteTitle}
            </span>
            <Dialog.Close asChild>
              <button
                type="button"
                data-testid="mobile-nav-close"
                aria-label={dict.mobileMenu.close}
                className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-fg-inverse/20 text-fg-inverse transition-fast hover:border-fg-inverse/50 hover:bg-fg-inverse/10"
              >
                <X className="h-5 w-5" aria-hidden="true" />
              </button>
            </Dialog.Close>
          </div>

          <nav
            aria-label={dict.nav.ariaLabel}
            className="site-container flex flex-1 flex-col justify-center py-12"
          >
            <Dialog.Close asChild>
              <Link
                href={`/${locale}`}
                data-testid="mobile-nav-home"
                aria-current={isHome ? "page" : undefined}
                className="group flex items-baseline justify-between gap-6 border-b border-fg-inverse/15 py-6"
              >
                <span className="flex items-baseline gap-4">
                  <span className="text-eyebrow font-semibold text-accent-soft/80">01</span>
                  <span className="font-display text-5xl leading-none tracking-tight text-fg-inverse sm:text-6xl">
                    {dict.nav.home}
                  </span>
                </span>
                <ArrowUpRight
                  className="h-5 w-5 text-fg-inverse-muted transition-fast group-hover:text-accent-soft"
                  aria-hidden="true"
                />
              </Link>
            </Dialog.Close>

            <Dialog.Close asChild>
              <Link
                href={`/${locale}/rooms`} prefetch={false}
                data-testid="mobile-nav-rooms"
                aria-current={isRooms ? "page" : undefined}
                className="group flex items-baseline justify-between gap-6 border-b border-fg-inverse/15 py-6"
              >
                <span className="flex items-baseline gap-4">
                  <span className="text-eyebrow font-semibold text-accent-soft/80">02</span>
                  <span className="font-display text-5xl leading-none tracking-tight text-fg-inverse sm:text-6xl">
                    {dict.nav.rooms}
                  </span>
                </span>
                <ArrowUpRight
                  className="h-5 w-5 text-fg-inverse-muted transition-fast group-hover:text-accent-soft"
                  aria-hidden="true"
                />
              </Link>
            </Dialog.Close>

            <div className="mt-10 flex flex-col gap-6 sm:flex-row sm:items-center sm:justify-between">
              <p className="max-w-sm text-small leading-relaxed text-fg-inverse-muted">
                {dict.footer.tagline}
              </p>
              <LocaleSwitcher
                locale={locale}
                pathname={pathname}
                label={dict.languageLabel}
                trLabel={dict.switchTr}
                enLabel={dict.switchEn}
                tone="dark"
                testIdPrefix="mobile-"
              />
            </div>
          </nav>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
