import { describe, expect, it } from "vitest";
import { resolveErrorMessage } from "../index";

describe("resolveErrorMessage (CR-12)", () => {
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
});
