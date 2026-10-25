import { describe, expect, it } from "vitest";
import en from "../en.json";
import { resolveErrorMessage } from "../index";
import tr from "../tr.json";

function keysOf(value: Record<string, unknown>, prefix = ""): string[] {
  return Object.entries(value).flatMap(([key, child]) => {
    const path = prefix ? `${prefix}.${key}` : key;
    if (child && typeof child === "object" && !Array.isArray(child)) {
      return keysOf(child as Record<string, unknown>, path);
    }
    return [path];
  });
}

describe("resolveErrorMessage and i18n dictionary hygiene (CR-12, AC-4, QA-16)", () => {
  it("shouldReturnDefaultMessage_whenCodeIsAPrototypeProperty", () => {
    expect(resolveErrorMessage("tr", "constructor")).toBe(
      "Bağlantı sırasında beklenmeyen bir sorun oluştu. Lütfen tekrar deneyin.",
    );
    expect(resolveErrorMessage("tr", "toString")).toBe(
      "Bağlantı sırasında beklenmeyen bir sorun oluştu. Lütfen tekrar deneyin.",
    );
  });

  it("shouldReturnMappedMessage_whenCodeIsKnown", () => {
    expect(resolveErrorMessage("tr", "system.internal_error")).toBe(
      "Sistemde geçici bir sorun oluştu. Lütfen kısa süre sonra tekrar deneyin.",
    );
    expect(resolveErrorMessage("en", "system.not_found")).toBe(
      "The requested information was not found.",
    );
  });

  it("shouldReturnDefaultMessage_whenCodeIsUndefinedOrUnknown", () => {
    expect(resolveErrorMessage("tr", undefined)).toBe(
      "Bağlantı sırasında beklenmeyen bir sorun oluştu. Lütfen tekrar deneyin.",
    );
    expect(resolveErrorMessage("en", "booking.room_not_available")).toBe(
      "An unexpected connection problem occurred. Please try again.",
    );
  });

  it("shouldRemoveTemporaryScaffoldKeysAndPreserveSharedKeys_whenInspectingDictionaries", () => {
    const removedScaffoldKeys = [
      "statusSectionTitle",
      "loadingTitle",
      "loadingDescription",
      "emptyTitle",
      "emptyDescription",
      "errorTitle",
      "errorRetry",
    ];
    const preservedRootKeys = [
      "siteTitle",
      "siteSubtitle",
      "languageLabel",
      "switchTr",
      "switchEn",
      "errors",
    ];

    for (const key of removedScaffoldKeys) {
      expect(tr).not.toHaveProperty(key);
      expect(en).not.toHaveProperty(key);
    }

    for (const key of preservedRootKeys) {
      expect(tr).toHaveProperty(key);
      expect(en).toHaveProperty(key);
    }
  });

  it("shouldKeepTurkishAndEnglishKeyParityAndRemoveTheOldHighlights", () => {
    expect(keysOf(en).sort()).toEqual(keysOf(tr).sort());
    expect(tr.home).not.toHaveProperty("highlights");
    expect(en.home).not.toHaveProperty("highlights");
  });
});
