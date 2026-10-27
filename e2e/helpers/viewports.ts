export const DESKTOP = { width: 1280, height: 800 } as const;
export const MOBILE = { width: 390, height: 844 } as const;
export const TABLET = { width: 768, height: 1024 } as const;

export const BOUNDARY_WIDTHS = [390, 767, 768, 1023, 1024, 1280] as const;

export const SIX_ROUTES = [
  "/tr",
  "/en",
  "/tr/rooms",
  "/en/rooms",
  "/tr/credits",
  "/en/credits",
] as const;
