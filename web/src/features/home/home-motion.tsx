"use client";

import { useEffect, useLayoutEffect } from "react";
import { useMotionPreferences } from "@/lib/motion/use-motion-preference";

const useIsomorphicLayoutEffect =
  typeof window === "undefined" ? useEffect : useLayoutEffect;

interface MotionFlags {
  motion: boolean;
  gsap: boolean;
  lenis: boolean;
}

type MotionWindow = Window & {
  __hotelMotionLoaded?: MotionFlags;
};

function setFlags(flags: MotionFlags) {
  (window as MotionWindow).__hotelMotionLoaded = flags;
}

function setScenes(pinned: boolean) {
  const scenes = document.querySelectorAll<HTMLElement>("[data-motion-scene]");
  scenes.forEach((scene) => {
    scene.dataset.motion = "static";
  });
  document.querySelectorAll<HTMLElement>("[data-motion-track]").forEach((track) => {
    const isPinnedTrack =
      pinned && ["home-story", "home-experience"].includes(track.dataset.motionTrack ?? "");
    if (isPinnedTrack) {
      track.dataset.pinned = "true";
    } else {
      delete track.dataset.pinned;
    }
  });
  if (!pinned) {
    return;
  }
  for (const id of ["home-story", "home-experience"]) {
    const scene = document.getElementById(id);
    if (scene) {
      scene.dataset.motion = "pinned";
    }
  }
}

function clearReveals() {
  document.querySelectorAll<HTMLElement>("[data-reveal]").forEach((element) => {
    element.style.removeProperty("opacity");
    element.style.removeProperty("transform");
    element.style.removeProperty("will-change");
  });
}

function clearIconDraws() {
  document.querySelectorAll<SVGElement>("[data-icon-draw] *").forEach((shape) => {
    const element = shape as SVGGeometryElement;
    element.style.removeProperty("stroke-dasharray");
    element.style.removeProperty("stroke-dashoffset");
    element.removeAttribute("pathLength");
  });
}

function clearScrollTransforms() {
  document.querySelectorAll<HTMLElement>("[data-parallax], [data-slow-zoom]").forEach((element) => {
    element.style.removeProperty("transform");
  });
}

export function HomeMotion() {
  const { isDesktop, reduceMotion } = useMotionPreferences();

  useIsomorphicLayoutEffect(() => {
    let cancelled = false;
    let revealCleanup: (() => void) | undefined;
    let pinCleanup: (() => void) | undefined;
    let frame = 0;
    let delay = 0;

    setFlags({ motion: false, gsap: false, lenis: false });
    setScenes(false);
    clearReveals();

    if (reduceMotion) {
      return () => {
        setScenes(false);
        clearReveals();
        clearIconDraws();
        clearScrollTransforms();
        setFlags({ motion: false, gsap: false, lenis: false });
      };
    }

    const initialise = async () => {
      const { animate } = await import("motion");
      if (cancelled) {
        return;
      }

      setFlags({ motion: true, gsap: false, lenis: false });
      const animations = new Set<{ stop: () => void }>();
      const observer = new IntersectionObserver(
        (entries) => {
          for (const entry of entries) {
            if (!entry.isIntersecting) {
              continue;
            }
            const element = entry.target as HTMLElement;
            observer.unobserve(element);
            const animation = animate(
              element,
              { opacity: [0, 1], y: [24, 0] },
              { duration: 0.75, ease: [0.22, 1, 0.36, 1] },
            );
            animations.add(animation);
          }
        },
        { threshold: 0.12, rootMargin: "0px 0px -8% 0px" },
      );

      document.querySelectorAll<HTMLElement>("[data-reveal]").forEach((element) => {
        const rect = element.getBoundingClientRect();
        if (rect.top < window.innerHeight * 0.92) {
          return;
        }
        element.style.opacity = "0";
        element.style.transform = "translateY(24px)";
        element.style.willChange = "opacity, transform";
        observer.observe(element);
      });

      const iconObserver = new IntersectionObserver(
        (entries) => {
          for (const entry of entries) {
            if (!entry.isIntersecting) {
              continue;
            }
            const icon = entry.target as SVGElement;
            iconObserver.unobserve(icon);
            icon
              .querySelectorAll<SVGGeometryElement>("path, circle, line, rect, polygon")
              .forEach((shape) => {
                shape.setAttribute("pathLength", "1");
                shape.style.strokeDasharray = "1";
                shape.style.strokeDashoffset = "1";
                const animation = animate(
                  shape,
                  { strokeDashoffset: [1, 0] },
                  { duration: 0.9, ease: [0.22, 1, 0.36, 1] },
                );
                animations.add(animation);
              });
          }
        },
        { threshold: 0.4 },
      );

      document.querySelectorAll<SVGElement>("[data-icon-draw]").forEach((icon) => {
        iconObserver.observe(icon);
      });

      revealCleanup = () => {
        observer.disconnect();
        iconObserver.disconnect();
        animations.forEach((animation) => animation.stop());
        animations.clear();
        clearReveals();
        clearIconDraws();
      };

      if (!isDesktop) {
        return;
      }

      const [{ gsap }, { ScrollTrigger }, { default: Lenis }] = await Promise.all([
        import("gsap"),
        import("gsap/ScrollTrigger"),
        import("lenis"),
      ]);
      if (cancelled) {
        return;
      }

      gsap.registerPlugin(ScrollTrigger);
      setFlags({ motion: true, gsap: true, lenis: false });
      setScenes(true);
      ScrollTrigger.refresh();

      const triggers: Array<{ kill: () => void }> = [];
      document.querySelectorAll<HTMLElement>("[data-parallax]").forEach((layer) => {
        const track = layer.closest("[data-motion-track]") ?? layer.closest("section");
        if (!track) {
          return;
        }
        const tween = gsap.fromTo(
          layer,
          { y: -Number(layer.dataset.parallaxAmplitude ?? 40) },
          {
            y: Number(layer.dataset.parallaxAmplitude ?? 40),
            ease: "none",
            scrollTrigger: {
              trigger: track,
              start: "top bottom",
              end: "bottom top",
              scrub: true,
            },
          },
        );
        triggers.push({
          kill: () => {
            tween.scrollTrigger?.kill();
            tween.kill();
          },
        });
      });

      document.querySelectorAll<HTMLElement>("[data-slow-zoom]").forEach((layer) => {
        const track = layer.closest("[data-motion-track]") ?? layer.closest("section");
        if (!track) {
          return;
        }
        const tween = gsap.fromTo(
          layer,
          { scale: 1 },
          {
            scale: 1.045,
            ease: "none",
            scrollTrigger: {
              trigger: track,
              start: "top bottom",
              end: "bottom top",
              scrub: true,
            },
          },
        );
        triggers.push({
          kill: () => {
            tween.scrollTrigger?.kill();
            tween.kill();
          },
        });
      });

      ScrollTrigger.refresh();

      const lenis = new Lenis({ duration: 0.9, smoothWheel: true });
      lenis.on("scroll", ScrollTrigger.update);
      const tick = (time: number) => {
        lenis.raf(time * 1000);
      };
      gsap.ticker.add(tick);
      gsap.ticker.lagSmoothing(0);
      setFlags({ motion: true, gsap: true, lenis: true });

      const refreshOnFonts = () => ScrollTrigger.refresh();
      void document.fonts?.ready.then(() => {
        if (!cancelled) {
          refreshOnFonts();
        }
      });

      pinCleanup = () => {
        gsap.ticker.remove(tick);
        lenis.destroy();
        triggers.forEach((trigger) => trigger.kill());
        clearScrollTransforms();
        setScenes(false);
        setFlags({ motion: false, gsap: false, lenis: false });
      };
    };

    delay = window.setTimeout(() => {
      frame = window.requestAnimationFrame(() => {
        void initialise();
      });
    }, 2000);

    return () => {
      cancelled = true;
      window.clearTimeout(delay);
      window.cancelAnimationFrame(frame);
      revealCleanup?.();
      pinCleanup?.();
      setScenes(false);
      clearReveals();
      clearIconDraws();
      clearScrollTransforms();
      setFlags({ motion: false, gsap: false, lenis: false });
    };
  }, [isDesktop, reduceMotion]);

  return null;
}
