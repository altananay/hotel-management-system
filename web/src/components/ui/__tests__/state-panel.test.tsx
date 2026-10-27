import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { StatePanel } from "../state-panel";

describe("StatePanel (AC-1, AC-3)", () => {
  it("shouldExposeLoadingAsStatusAndErrorAsAlert", () => {
    const { rerender } = render(
      <StatePanel state="loading" testId="loading" title="Loading" />,
    );
    expect(screen.getByTestId("loading")).toHaveAttribute("role", "status");

    rerender(
      <StatePanel
        state="error"
        testId="error"
        messageTestId="error-message"
        message="Something went wrong"
        action={<button type="button">Retry</button>}
      />,
    );
    expect(screen.getByTestId("error")).toHaveAttribute("role", "alert");
    expect(screen.getByTestId("error-message")).toHaveTextContent("Something went wrong");

    rerender(
      <StatePanel
        state="empty"
        testId="empty"
        titleTestId="empty-title"
        title="Nothing here"
      />,
    );
    expect(screen.getByTestId("empty-title")).toHaveTextContent("Nothing here");
  });
});
