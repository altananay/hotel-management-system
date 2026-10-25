import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { describe, expect, it, vi } from "vitest";
import { MobileNav } from "../mobile-nav";
import tr from "@/lib/i18n/tr.json";

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

describe("MobileNav (AC-4, B-08)", () => {
  it("shouldOpenAModalDialogAndReturnFocusOnEscape", async () => {
    render(<MobileNav locale="tr" pathname="/tr" dict={tr} />);

    const toggle = screen.getByTestId("mobile-nav-toggle");
    fireEvent.click(toggle);

    const dialog = await screen.findByTestId("mobile-nav");
    expect(dialog).toHaveAttribute("role", "dialog");
    expect(dialog).toHaveAttribute("aria-modal", "true");
    expect(screen.getByTestId("mobile-nav-home")).toHaveAttribute("aria-current", "page");

    fireEvent.keyDown(document, { key: "Escape", code: "Escape" });

    await waitFor(() => {
      expect(screen.queryByTestId("mobile-nav")).not.toBeInTheDocument();
    });
    expect(toggle).toHaveFocus();
  });

  it("shouldCloseWhenTheRouteChanges", async () => {
    const { rerender } = render(<MobileNav locale="tr" pathname="/tr" dict={tr} />);
    fireEvent.click(screen.getByTestId("mobile-nav-toggle"));
    expect(await screen.findByTestId("mobile-nav")).toBeInTheDocument();

    rerender(<MobileNav locale="tr" pathname="/tr/rooms" dict={tr} />);

    await waitFor(() => {
      expect(screen.queryByTestId("mobile-nav")).not.toBeInTheDocument();
    });
  });
});
