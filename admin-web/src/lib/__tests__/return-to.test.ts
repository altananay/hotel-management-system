import { describe, expect, it } from "vitest";
import { safeReturnTo } from "../return-to";

describe("safeReturnTo", () => {
  it("shouldAcceptRelativePaths", () => {
    expect(safeReturnTo("/rooms")).toBe("/rooms");
    expect(safeReturnTo("/")).toBe("/");
    expect(safeReturnTo("/rooms?checkIn=2026-11-01")).toBe("/rooms?checkIn=2026-11-01");
  });

  it("shouldRejectOpenRedirectsAndFallbackToRoot", () => {
    expect(safeReturnTo("//evil.example")).toBe("/");
    expect(safeReturnTo("https://evil.example")).toBe("/");
    expect(safeReturnTo("\\\\evil.example")).toBe("/");
    expect(safeReturnTo(undefined)).toBe("/");
  });

  it("shouldRejectEncodedOpenRedirectVariants", () => {
    expect(safeReturnTo("/%2F%2Fevil.example")).toBe("/");
    expect(safeReturnTo("/%5Cevil.example")).toBe("/");
    expect(safeReturnTo("/%252F%252Fevil.example")).toBe("/");
    expect(safeReturnTo("/%2f%2fevil.example")).toBe("/");
    expect(safeReturnTo("/%00evil")).toBe("/");
  });

  it("shouldRejectMalformedEncoding", () => {
    expect(safeReturnTo("/%E0%A4%A")).toBe("/");
  });
});
