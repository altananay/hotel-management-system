import Link from "next/link";
import { FlagGb, FlagTr } from "./flags";

export interface LocaleSwitcherProps {
  locale: "tr" | "en";
  pathname: string;
  label: string;
  trLabel: string;
  enLabel: string;
  tone?: "light" | "dark";
  testIdPrefix?: string;
}

function localeHref(pathname: string, target: "tr" | "en"): string {
  const segments = pathname.split("/").filter(Boolean);
  if (segments[0] === "tr" || segments[0] === "en") {
    segments[0] = target;
  } else {
    segments.unshift(target);
  }
  return `/${segments.join("/")}`;
}

export function LocaleSwitcher({
  locale,
  pathname,
  label,
  trLabel,
  enLabel,
  tone = "light",
  testIdPrefix = "",
}: LocaleSwitcherProps) {
  const isDark = tone === "dark";
  return (
    <nav
      aria-label={label}
      data-testid={`${testIdPrefix}locale-switcher`}
      className={
        isDark
          ? "flex items-center gap-3 text-fg-inverse"
          : "flex items-center gap-2 text-fg"
      }
    >
      <span
        className={
          isDark
            ? "hidden text-eyebrow font-semibold uppercase tracking-[0.18em] text-fg-inverse-muted sm:inline"
            : "hidden text-eyebrow font-semibold uppercase tracking-[0.18em] text-fg-muted sm:inline"
        }
      >
        {label}
      </span>
      <span
        className={
          isDark
            ? "inline-flex items-center rounded-full border border-fg-inverse/20 p-0.5"
            : "inline-flex items-center rounded-full border border-border bg-surface-raised/70 p-0.5"
        }
      >
        <Link
          href={localeHref(pathname, "tr")}
          data-testid={`${testIdPrefix}locale-switch-tr`}
          aria-current={locale === "tr" ? "true" : undefined}
          className={
            locale === "tr"
              ? isDark
                ? "rounded-full bg-fg-inverse px-2 py-1 sm:px-2.5 text-eyebrow font-semibold text-surface-inverse"
                : "rounded-full bg-surface-inverse px-2 py-1 sm:px-2.5 text-eyebrow font-semibold text-fg-inverse"
              : isDark
                ? "rounded-full px-2 py-1 sm:px-2.5 text-eyebrow font-semibold text-fg-inverse-muted hover:text-fg-inverse"
                : "rounded-full px-2 py-1 sm:px-2.5 text-eyebrow font-semibold text-fg-muted hover:text-fg"
          }
        >
          <FlagTr />
          <span>{trLabel}</span>
        </Link>
        <Link
          href={localeHref(pathname, "en")}
          data-testid={`${testIdPrefix}locale-switch-en`}
          aria-current={locale === "en" ? "true" : undefined}
          className={
            locale === "en"
              ? isDark
                ? "rounded-full bg-fg-inverse px-2 py-1 sm:px-2.5 text-eyebrow font-semibold text-surface-inverse"
                : "rounded-full bg-surface-inverse px-2 py-1 sm:px-2.5 text-eyebrow font-semibold text-fg-inverse"
              : isDark
                ? "rounded-full px-2 py-1 sm:px-2.5 text-eyebrow font-semibold text-fg-inverse-muted hover:text-fg-inverse"
                : "rounded-full px-2 py-1 sm:px-2.5 text-eyebrow font-semibold text-fg-muted hover:text-fg"
          }
        >
          <FlagGb />
          <span>{enLabel}</span>
        </Link>
      </span>
    </nav>
  );
}

