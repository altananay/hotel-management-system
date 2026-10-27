"use client";

import { useEffect, useState } from "react";

export interface MotionPreferences {
  isDesktop: boolean;
  reduceMotion: boolean;
  saveData: boolean;
}

const INITIAL: MotionPreferences = {
  isDesktop: false,
  reduceMotion: true,
  saveData: false,
};

interface NavigatorWithConnection extends Navigator {
  connection?: {
    saveData?: boolean;
    addEventListener?: (type: "change", listener: () => void) => void;
    removeEventListener?: (type: "change", listener: () => void) => void;
  };
}

function readPreferences(): MotionPreferences {
  if (typeof window === "undefined" || typeof window.matchMedia !== "function") {
    return INITIAL;
  }
  const connection = (navigator as NavigatorWithConnection).connection;
  return {
    isDesktop: window.matchMedia("(min-width: 1024px)").matches,
    reduceMotion: window.matchMedia("(prefers-reduced-motion: reduce)").matches,
    saveData: connection?.saveData === true,
  };
}

export function useMotionPreferences(): MotionPreferences {
  const [preferences, setPreferences] = useState<MotionPreferences>(INITIAL);

  useEffect(() => {
    const desktop = window.matchMedia("(min-width: 1024px)");
    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)");
    const connection = (navigator as NavigatorWithConnection).connection;
    const update = () => setPreferences(readPreferences());

    update();
    desktop.addEventListener("change", update);
    reduced.addEventListener("change", update);
    connection?.addEventListener?.("change", update);

    return () => {
      desktop.removeEventListener("change", update);
      reduced.removeEventListener("change", update);
      connection?.removeEventListener?.("change", update);
    };
  }, []);

  return preferences;
}
