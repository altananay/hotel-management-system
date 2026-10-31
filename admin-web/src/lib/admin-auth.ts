import { cookies } from "next/headers";
import { z } from "zod";

const AdminMeSchema = z.object({
  id: z.string(),
  username: z.string(),
  email: z.string(),
  role: z.string(),
});

export type AdminMe = z.infer<typeof AdminMeSchema>;

export class AdminAuthError extends Error {
  readonly status: number;

  constructor(status: number) {
    super(`Admin auth error (${status})`);
    this.name = "AdminAuthError";
    this.status = status;
  }
}

export async function getAdminSession(): Promise<AdminMe | null> {
  const cookieStore = await cookies();
  const baseUrl = process.env.ADMIN_API_INTERNAL_URL ?? "http://localhost:8082";
  let response: Response;
  try {
    response = await fetch(`${baseUrl}/admin-api/auth/me`, {
      headers: {
        Accept: "application/json, application/problem+json",
        Cookie: cookieStore.toString(),
      },
      cache: "no-store",
    });
  } catch {
    throw new AdminAuthError(0);
  }

  if (response.status === 401) {
    return null;
  }
  if (!response.ok) {
    throw new AdminAuthError(response.status);
  }
  return AdminMeSchema.parse(await response.json());
}
