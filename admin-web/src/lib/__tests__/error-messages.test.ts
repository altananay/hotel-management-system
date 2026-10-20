import { describe, expect, it } from "vitest";
import { resolveAdminErrorMessage } from "../error-messages";

describe("resolveAdminErrorMessage (CR-12)", () => {
  it("shouldReturnDefaultMessage_whenCodeIsAPrototypeProperty", () => {
    expect(resolveAdminErrorMessage("constructor")).toBe(
      "İşlem sırasında beklenmeyen bir hata oluştu. Lütfen tekrar deneyin.",
    );
    expect(resolveAdminErrorMessage("hasOwnProperty")).toBe(
      "İşlem sırasında beklenmeyen bir hata oluştu. Lütfen tekrar deneyin.",
    );
  });

  it("shouldReturnMappedMessage_whenCodeIsKnown", () => {
    expect(resolveAdminErrorMessage("system.internal_error")).toBe(
      "Yönetim servisinde geçici bir sorun oluştu. Lütfen tekrar deneyin.",
    );
  });

  it("shouldReturnDefaultMessage_whenCodeIsUndefinedOrUnknown", () => {
    expect(resolveAdminErrorMessage(undefined)).toBe(
      "İşlem sırasında beklenmeyen bir hata oluştu. Lütfen tekrar deneyin.",
    );
  });
});
