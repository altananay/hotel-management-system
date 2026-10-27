import { renderHook, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { useMotionPreferences } from "../use-motion-preference";

function setMatchMedia({ desktop, reduced }: { desktop: boolean; reduced: boolean }) {
  window.matchMedia = vi.fn((query: string) => {
    const matches = query.includes("min-width: 1024px")
      ? desktop
      : query.includes("prefers-reduced-motion")
        ? reduced
        : false;
    return {
      matches,
      media: query,
      onchange: null,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      addListener: vi.fn(),
      removeListener: vi.fn(),
      dispatchEvent: vi.fn(),
    };
  }) as unknown as typeof window.matchMedia;
}

describe("useMotionPreferences (AC-5, B-12)", () => {
  afterEach(() => {
    delete (navigator as Navigator & { connection?: unknown }).connection;
    vi.restoreAllMocks();
  });

  it("shouldStartStaticAndResolveTheDesktopAndReducedMotionPreferences", async () => {
    setMatchMedia({ desktop: true, reduced: false });
    const { result } = renderHook(() => useMotionPreferences());

    await waitFor(() => {
      expect(result.current.isDesktop).toBe(true);
      expect(result.current.reduceMotion).toBe(false);
    });
  });

  it("shouldReadSaveDataFromTheConnection", async () => {
    setMatchMedia({ desktop: false, reduced: false });
    Object.defineProperty(navigator, "connection", {
      configurable: true,
      value: { saveData: true },
    });

    const { result } = renderHook(() => useMotionPreferences());
    await waitFor(() => {
      expect(result.current.saveData).toBe(true);
    });
  });
});
