import { render, screen } from "@testing-library/react";
import type { ReactNode } from "react";
import { describe, expect, it, vi } from "vitest";
import { SiteHeader } from "../site-header";
import tr from "@/lib/i18n/tr.json";

vi.mock("next/navigation", () => ({
  usePathname: () => "/tr/rooms",
}));

vi.mock("next/link", () => ({
  default: ({
    href,
    children,
    prefetch,
    ...props
  }: {
    href: string;
    children: ReactNode;
    prefetch?: boolean;
  }) => {
    void prefetch;
    return (
      <a href={href} {...props}>
        {children}
      </a>
    );
  },
}));

describe("SiteHeader (AC-2, AC-4)", () => {
  it("shouldRenderOneWordmarkLinkDesktopNavLocaleSwitchAndMobileToggle", () => {
    render(<SiteHeader locale="tr" dict={tr} />);

    const wordmarks = screen.getAllByTestId("web-site-title");
    expect(wordmarks).toHaveLength(1);
    expect(wordmarks[0].tagName).toBe("A");
    expect(wordmarks[0]).toHaveTextContent("Kapadokya Taş Konak Otel");
    expect(wordmarks[0]).toHaveAttribute("href", "/tr");

    expect(screen.getByTestId("site-header").tagName).toBe("HEADER");
    expect(screen.getByTestId("site-nav-home")).not.toHaveAttribute("aria-current");
    expect(screen.getByTestId("site-nav-rooms")).toHaveAttribute("aria-current", "page");
    expect(screen.getByTestId("locale-switch-en")).toHaveAttribute("href", "/en/rooms");
    expect(screen.getByTestId("mobile-nav-toggle")).toBeInTheDocument();
  });
});
