import type { Page } from "@playwright/test";

export interface ConsoleCollector {
  errors: string[];
  clear: () => void;
}

export function collectConsole(page: Page): ConsoleCollector {
  const errors: string[] = [];

  page.on("console", (message) => {
    if (message.type() === "error") {
      errors.push(message.text());
    }
  });
  page.on("pageerror", (error) => {
    errors.push(`pageerror: ${error.message}`);
  });

  return {
    errors,
    clear: () => {
      errors.length = 0;
    },
  };
}
