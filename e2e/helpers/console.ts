import type { Page } from "@playwright/test";

export interface ConsoleCollector {
  errors: string[];
  appErrors: () => string[];
  clear: () => void;
}

const RESOURCE_ERROR = /Failed to load resource|net::ERR_/i;

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
    appErrors: () => errors.filter((message) => !RESOURCE_ERROR.test(message)),
    clear: () => {
      errors.length = 0;
    },
  };
}
