import en from "./en.json";
import tr from "./tr.json";

export const SUPPORTED_LOCALES = ["tr", "en"] as const;
export type Locale = (typeof SUPPORTED_LOCALES)[number];
export const DEFAULT_LOCALE: Locale = "tr";

export type Dictionary = typeof tr;

const dictionaries: Record<Locale, Dictionary> = {
  tr,
  en,
};

export function isValidLocale(value: string): value is Locale {
  return SUPPORTED_LOCALES.includes(value as Locale);
}

export function getDictionary(locale: Locale): Dictionary {
  return dictionaries[locale] ?? dictionaries[DEFAULT_LOCALE];
}

export function resolveErrorMessage(locale: Locale, code?: string): string {
  const dict = getDictionary(locale);
  const errorMap = dict.errors as Record<string, string>;
  if (code && Object.hasOwn(errorMap, code)) {
    return errorMap[code];
  }
  return dict.errors.default;
}
