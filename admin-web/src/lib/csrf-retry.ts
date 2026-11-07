import {
  AdminApiClientError,
} from "./api-client";
import { fetchCsrfToken, readCsrfToken } from "./csrf";

/**
 * Runs a state-changing admin request with the 0004 CSRF policy:
 * use the cookie token; on `security.csrf_invalid`, refresh once and retry.
 */
export async function withCsrfRetry<T>(
  action: (csrfToken: string) => Promise<T>,
): Promise<T> {
  let token = readCsrfToken() ?? (await fetchCsrfToken());
  try {
    return await action(token);
  } catch (error) {
    if (
      error instanceof AdminApiClientError &&
      error.code === "security.csrf_invalid"
    ) {
      token = await fetchCsrfToken();
      return action(token);
    }
    throw error;
  }
}
