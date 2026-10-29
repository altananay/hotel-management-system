import { describe, expect, it } from "vitest";
import { safeReturnTo } from "../return-to";

describe("safeReturnTo", () => {
  it("shouldAcceptRelativePaths", () => {
    expect(safeReturnTo("/rooms")).toBe("/rooms");
    expect(safeReturnTo("/")).toBe("/");
  });

  it("shouldRejectOpenRedirectsAndFallbackToRoot", () => {
    expect(safeReturnTo("//evil.example")).toBe("/");
    expect(safeReturnTo("https://evil.example")).toBe("/");
    expect(safeReturnTo("\\\\evil.example")).toBe("/");
    expect(safeReturnTo(undefined)).toBe("/");
  });
});
