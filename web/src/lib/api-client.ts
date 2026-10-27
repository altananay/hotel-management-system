import { z } from "zod";
import { appConfig } from "./config";

export const DEFAULT_API_TIMEOUT_MS = 10_000;

// Bilinçli olarak `.passthrough()`: zorunlu alanlar katı şekilde doğrulanır, ileriye dönük
// ek alanlar (RFC 7807 uzantıları) yanıtı bozmaz (CR-9).
export const ProblemDetailSchema = z
  .object({
    type: z.string(),
    title: z.string(),
    status: z.number(),
    detail: z.string(),
    instance: z.string(),
    code: z.string(),
    requestId: z.string(),
  })
  .passthrough();

export class ApiClientError extends Error {
  readonly status: number;
  readonly code?: string;
  readonly requestId?: string;

  constructor(status: number, code?: string, requestId?: string) {
    super(`API error (${status})`);
    this.name = "ApiClientError";
    this.status = status;
    this.code = code;
    this.requestId = requestId;
  }
}

export interface ApiGetOptions {
  timeoutMs?: number;
}

export async function apiGet<T>(
  path: string,
  schema: z.ZodType<T>,
  options: ApiGetOptions = {},
): Promise<T> {
  const url = `${appConfig.apiBaseUrl}${path}`;
  const timeoutMs = options.timeoutMs ?? DEFAULT_API_TIMEOUT_MS;

  let timer: ReturnType<typeof setTimeout> | undefined;
  const timeout = new Promise<never>((_, reject) => {
    timer = setTimeout(() => {
      reject(new ApiClientError(0, "system.timeout"));
    }, timeoutMs);
  });

  const request = fetch(url, {
    method: "GET",
    headers: {
      Accept: "application/json, application/problem+json",
    },
  }).catch(() => {
    throw new ApiClientError(0, "system.network_error");
  });

  try {
    const response = await Promise.race([request, timeout]);
    const headerRequestId = response.headers.get("X-Request-Id") ?? undefined;

    if (!response.ok) {
      let code: string | undefined;
      let requestId = headerRequestId;
      try {
        const rawError: unknown = await Promise.race([response.json(), timeout]);
        const parsedProblem = ProblemDetailSchema.safeParse(rawError);
        if (parsedProblem.success) {
          code = parsedProblem.data.code;
          requestId = parsedProblem.data.requestId;
        }
      } catch (error) {
        if (error instanceof ApiClientError) {
          throw error;
        }
        // Bozuk/eksik zarf: makine kodu okunamaz; kullanıcı dostu genel mesaja düşülür (CR-9).
      }
      throw new ApiClientError(response.status, code, requestId);
    }

    let rawData: unknown;
    try {
      rawData = await Promise.race([response.json(), timeout]);
    } catch (error) {
      if (error instanceof ApiClientError) {
        throw error;
      }
      throw new ApiClientError(response.status, "system.invalid_response");
    }

    try {
      return schema.parse(rawData);
    } catch {
      throw new ApiClientError(response.status, "system.invalid_response");
    }
  } finally {
    if (timer) {
      clearTimeout(timer);
    }
  }
}
