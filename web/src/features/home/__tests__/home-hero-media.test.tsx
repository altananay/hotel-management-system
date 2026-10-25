import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { HomeHeroMedia } from "../home-hero-media";

interface MediaPreferences {
  desktop: boolean;
  reduced: boolean;
}

function setMatchMedia({ desktop, reduced }: MediaPreferences) {
  window.matchMedia = vi.fn((query: string) => {
    const matches = query.includes("min-width: 768px")
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

function setPlay(implementation: () => Promise<void>) {
  Object.defineProperty(HTMLMediaElement.prototype, "play", {
    configurable: true,
    writable: true,
    value: vi.fn(implementation),
  });
}

describe("HomeHeroMedia (AC-2, B-05, B-13)", () => {
  beforeEach(() => {
    setPlay(() => Promise.resolve());
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: true }));
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    delete (navigator as Navigator & { connection?: unknown }).connection;
  });

  it("shouldAlwaysRenderThePosterAndMountAVideoOnlyAtDesktopWidth", async () => {
    setMatchMedia({ desktop: true, reduced: false });
    render(<HomeHeroMedia />);

    expect(screen.getByTestId("home-hero-poster")).toBeInTheDocument();
    const video = await screen.findByTestId("home-hero-video");

    expect(video).toHaveAttribute("preload", "none");
    expect(video).toHaveAttribute("aria-hidden", "true");
    expect((video as HTMLVideoElement).muted).toBe(true);
    expect((video as HTMLVideoElement).loop).toBe(true);
    expect((video as HTMLVideoElement).playsInline).toBe(true);

    fireEvent.canPlay(video);
    await waitFor(() => {
      expect(video.className).toContain("opacity-100");
    });
  });

  it("shouldKeepThePosterAndRemoveTheVideoWhenAutoplayIsRejected", async () => {
    setMatchMedia({ desktop: true, reduced: false });
    setPlay(() => Promise.reject(new Error("autoplay blocked")));
    render(<HomeHeroMedia />);

    const video = await screen.findByTestId("home-hero-video");
    fireEvent.canPlay(video);

    await waitFor(() => {
      expect(screen.queryByTestId("home-hero-video")).not.toBeInTheDocument();
    });
    expect(screen.getByTestId("home-hero-poster")).toBeInTheDocument();
  });

  it("shouldNotMountVideoAtMobileWidthOrUnderReducedMotionOrSaveData", async () => {
    setMatchMedia({ desktop: false, reduced: false });
    const { unmount } = render(<HomeHeroMedia />);
    await waitFor(() => {
      expect(screen.queryByTestId("home-hero-video")).not.toBeInTheDocument();
    });
    expect(fetch).not.toHaveBeenCalled();
    unmount();

    setMatchMedia({ desktop: true, reduced: true });
    const reduced = render(<HomeHeroMedia />);
    await waitFor(() => {
      expect(screen.queryByTestId("home-hero-video")).not.toBeInTheDocument();
    });
    reduced.unmount();

    setMatchMedia({ desktop: true, reduced: false });
    Object.defineProperty(navigator, "connection", {
      configurable: true,
      value: { saveData: true },
    });
    render(<HomeHeroMedia />);
    await waitFor(() => {
      expect(screen.queryByTestId("home-hero-video")).not.toBeInTheDocument();
    });
  });
});
