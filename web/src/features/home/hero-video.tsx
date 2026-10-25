"use client";

import { useEffect, useRef, useState } from "react";

const HERO_WEBM = "/media/hero/home-hero.webm";
const HERO_MP4 = "/media/hero/home-hero.mp4";

interface NavigatorWithConnection extends Navigator {
  connection?: { saveData?: boolean };
}

function canUseVideo(media: MediaQueryList, reduced: MediaQueryList): boolean {
  const connection = (navigator as NavigatorWithConnection).connection;
  return media.matches && !reduced.matches && !connection?.saveData;
}

export function HomeHeroVideo() {
  const [mounted, setMounted] = useState(false);
  const [visible, setVisible] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    if (typeof window.matchMedia !== "function") {
      return;
    }

    let cancelled = false;
    const desktop = window.matchMedia("(min-width: 768px)");
    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)");

    const preflight = async () => {
      if (!canUseVideo(desktop, reduced)) {
        return;
      }
      try {
        const response = await fetch(HERO_MP4, {
          method: "HEAD",
          cache: "no-store",
        });
        if (!cancelled && response.ok && canUseVideo(desktop, reduced)) {
          setMounted(true);
        }
      } catch {
        // The poster is the permanent, silent fallback.
      }
    };

    const frame = window.requestAnimationFrame(() => {
      void preflight();
    });

    const handleChange = () => {
      if (canUseVideo(desktop, reduced)) {
        void preflight();
      } else {
        setMounted(false);
        setVisible(false);
      }
    };

    desktop.addEventListener("change", handleChange);
    reduced.addEventListener("change", handleChange);

    return () => {
      cancelled = true;
      window.cancelAnimationFrame(frame);
      desktop.removeEventListener("change", handleChange);
      reduced.removeEventListener("change", handleChange);
    };
  }, []);

  const handleCanPlay = () => {
    const video = videoRef.current;
    if (!video) {
      return;
    }
    void video
      .play()
      .then(() => setVisible(true))
      .catch(() => {
        setMounted(false);
        setVisible(false);
      });
  };

  const handleError = () => {
    setMounted(false);
    setVisible(false);
  };

  if (!mounted) {
    return null;
  }

  return (
    <video
      data-testid="home-hero-video"
      ref={videoRef}
      aria-hidden="true"
      muted
      loop
      playsInline
      autoPlay
      preload="none"
      tabIndex={-1}
      onCanPlay={handleCanPlay}
      onError={handleError}
      className={
        visible
          ? "absolute inset-0 h-full w-full object-cover object-center opacity-100 transition-opacity"
          : "absolute inset-0 h-full w-full object-cover object-center opacity-0 transition-opacity"
      }
      style={{ transitionDuration: "var(--duration-slow)" }}
    >
      <source src={HERO_WEBM} type="video/webm" />
      <source src={HERO_MP4} type="video/mp4" />
    </video>
  );
}
