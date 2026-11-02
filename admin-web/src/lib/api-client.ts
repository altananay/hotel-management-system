import { z } from "zod";

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

export class AdminApiClientError extends Error {
  readonly status: number;
  readonly code?: string;
  readonly requestId?: string;

  constructor(status: number, code?: string, requestId?: string) {
    super(`Admin API error (${status})`);
    this.name = "AdminApiClientError";
    this.status = status;
    this.code = code;
    this.requestId = requestId;
  }
}

export interface AdminApiRequestOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  csrfToken?: string;
}

export async function adminApiRequest<T>(
  path: string,
  schema: z.ZodType<T>,
  options: AdminApiRequestOptions = {},
): Promise<T> {
  const response = await send(path, options);
  if (response.status === 204) {
    return undefined as T;
  }
  return schema.parse(await response.json());
}

export async function adminApiGet<T>(path: string, schema: z.ZodType<T>): Promise<T> {
  return adminApiRequest(path, schema, { method: "GET" });
}

export async function adminApiPost(
  path: string,
  body: unknown,
  csrfToken: string,
): Promise<void> {
  await send(path, { method: "POST", body, csrfToken });
}

async function send(path: string, options: AdminApiRequestOptions): Promise<Response> {
  const headers: Record<string, string> = {
    Accept: "application/json, application/problem+json",
  };
  if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  if (options.csrfToken) {
    headers["X-XSRF-TOKEN"] = options.csrfToken;
  }

  let response: Response;
  try {
    response = await fetch(path, {
      method: options.method ?? "GET",
      headers,
      credentials: "same-origin",
      cache: "no-store",
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    });
  } catch {
    throw new AdminApiClientError(0, "system.network_error");
  }

  if (!response.ok) {
    let code: string | undefined;
    let requestId = response.headers.get("X-Request-Id") ?? undefined;
    try {
      const parsed = ProblemDetailSchema.safeParse(await response.json());
      if (parsed.success) {
        code = parsed.data.code;
        requestId = parsed.data.requestId;
      }
    } catch {
      // Generic message is safer than leaking a malformed envelope.
    }
    throw new AdminApiClientError(response.status, code, requestId);
  }
  return response;
}
