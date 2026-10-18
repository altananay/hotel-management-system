import { z } from "zod";

export const ProblemDetailSchema = z.object({
  type: z.string().optional(),
  title: z.string().optional(),
  status: z.number().optional(),
  detail: z.string().optional(),
  instance: z.string().optional(),
  code: z.string().optional(),
  requestId: z.string().optional(),
});

export class AdminApiClientError extends Error {
  readonly status: number;
  readonly code: string;
  readonly requestId?: string;

  constructor(status: number, code: string, requestId?: string) {
    super(`Admin API error (${status}): ${code}`);
    this.name = "AdminApiClientError";
    this.status = status;
    this.code = code;
    this.requestId = requestId;
  }
}

export async function adminApiGet<T>(
  path: string,
  schema: z.ZodType<T>,
): Promise<T> {
  let response: Response;
  try {
    response = await fetch(path, {
      method: "GET",
      headers: {
        Accept: "application/json, application/problem+json",
      },
      credentials: "same-origin",
    });
  } catch {
    throw new AdminApiClientError(0, "system.network_error");
  }

  const headerRequestId = response.headers.get("X-Request-Id") ?? undefined;

  if (!response.ok) {
    let code = "system.internal_error";
    let requestId = headerRequestId;
    try {
      const rawError: unknown = await response.json();
      const parsedProblem = ProblemDetailSchema.safeParse(rawError);
      if (parsedProblem.success) {
        code = parsedProblem.data.code ?? code;
        requestId = parsedProblem.data.requestId ?? requestId;
      }
    } catch {
      // Fall back to default machine code without leaking raw body
    }
    throw new AdminApiClientError(response.status, code, requestId);
  }

  const rawData: unknown = await response.json();
  return schema.parse(rawData);
}
