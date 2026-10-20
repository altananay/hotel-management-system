import { z } from "zod";
import { appConfig } from "./config";

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

export async function apiGet<T>(path: string, schema: z.ZodType<T>): Promise<T> {
  const url = `${appConfig.apiBaseUrl}${path}`;
  let response: Response;
  try {
    response = await fetch(url, {
      method: "GET",
      headers: {
        Accept: "application/json, application/problem+json",
      },
    });
  } catch {
    throw new ApiClientError(0, "system.network_error");
  }

  const headerRequestId = response.headers.get("X-Request-Id") ?? undefined;

  if (!response.ok) {
    let code: string | undefined;
    let requestId = headerRequestId;
    try {
      const rawError: unknown = await response.json();
      const parsedProblem = ProblemDetailSchema.safeParse(rawError);
      if (parsedProblem.success) {
        code = parsedProblem.data.code;
        requestId = parsedProblem.data.requestId;
      }
    } catch {
      // Bozuk/eksik zarf: makine kodu okunamaz; kullanıcı dostu genel mesaja düşülür (CR-9).
    }
    throw new ApiClientError(response.status, code, requestId);
  }

  const rawData: unknown = await response.json();
  return schema.parse(rawData);
}
