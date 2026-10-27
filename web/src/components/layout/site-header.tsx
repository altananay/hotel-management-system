"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import type { Dictionary } from "@/lib/i18n";
import { LocaleSwitcher } from "./locale-switcher";
import { MobileNav } from "./mobile-nav";

export interface SiteHeaderProps {
  locale: "tr" | "en";
  dict: Dictionary;
}

export function SiteHeader({ locale, dict }: SiteHeaderProps) {
  const pathname = usePathname();
  const isHome = pathname === `/${locale}`;
  const isRooms = pathname === `/${locale}/rooms`;

  return (
    <header
      data-testid="site-header"
      className="sticky top-0 z-40 border-b border-border/80 bg-surface/92 backdrop-blur-md"
    >
      <div className="site-container flex h-20 items-center justify-between gap-3 sm:gap-6">
        <Link
          href={`/${locale}`}
          data-testid="web-site-title"
          className="group flex min-w-0 items-center gap-3"
        >
          <span
            aria-hidden="true"
            className="hidden h-9 w-px bg-border-strong/70 sm:block"
          />
          <span className="font-display whitespace-nowrap text-[1rem] leading-none tracking-tight text-fg transition-fast group-hover:text-accent-strong sm:text-[1.35rem] lg:text-[1.55rem]">
            {dict.siteTitle}
          </span>
        </Link>

        <nav
          aria-label={dict.nav.ariaLabel}
          className="hidden items-center gap-9 lg:flex"
        >
          <Link
            href={`/${locale}`}
            data-testid="site-nav-home"
            aria-current={isHome ? "page" : undefined}
            className={
              isHome
                ? "border-b border-accent pb-1 text-small font-semibold text-fg"
                : "border-b border-transparent pb-1 text-small font-semibold text-fg-muted transition-fast hover:border-border-strong hover:text-fg"
            }
          >
            {dict.nav.home}
          </Link>
          <Link
            href={`/${locale}/rooms`} prefetch={false}
            data-testid="site-nav-rooms"
            aria-current={isRooms ? "page" : undefined}
            className={
              isRooms
                ? "border-b border-accent pb-1 text-small font-semibold text-fg"
                : "border-b border-transparent pb-1 text-small font-semibold text-fg-muted transition-fast hover:border-border-strong hover:text-fg"
            }
          >
            {dict.nav.rooms}
          </Link>
        </nav>

        <div className="flex items-center gap-2 sm:gap-3">
          <LocaleSwitcher
            locale={locale}
            pathname={pathname}
            label={dict.languageLabel}
            trLabel={dict.switchTr}
            enLabel={dict.switchEn}
          />
          <MobileNav locale={locale} pathname={pathname} dict={dict} />
        </div>
      </div>
    </header>
  );
}
