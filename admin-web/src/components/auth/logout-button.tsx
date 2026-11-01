"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { AdminApiClientError, adminApiPost } from "@/lib/api-client";
import { fetchCsrfToken, readCsrfToken } from "@/lib/csrf";

async function sessionCsrfToken(): Promise<string> {
  return readCsrfToken() ?? (await fetchCsrfToken());
}

export function LogoutButton() {
  const router = useRouter();
  const [submitting, setSubmitting] = useState(false);
  const [failed, setFailed] = useState(false);

  const sendLogout = async (csrfToken: string): Promise<"sent" | "redirected"> => {
    try {
      await adminApiPost("/admin-api/auth/logout", {}, csrfToken);
      return "sent";
    } catch (error) {
      if (error instanceof AdminApiClientError && error.status === 401) {
        router.push("/login");
        return "redirected";
      }
      throw error;
    }
  };

  const logout = async () => {
    setSubmitting(true);
    setFailed(false);
    try {
      try {
        const first = await sendLogout(await sessionCsrfToken());
        if (first === "redirected") {
          return;
        }
      } catch (error) {
        if (
          !(
            error instanceof AdminApiClientError &&
            error.code === "security.csrf_invalid"
          )
        ) {
          throw error;
        }
        const retry = await sendLogout(await fetchCsrfToken());
        if (retry === "redirected") {
          return;
        }
      }
      router.push("/login");
    } catch {
      setFailed(true);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex items-center gap-3" data-testid="admin-user-menu">
      {failed && (
        <span className="text-xs text-red-700" role="alert">
          Çıkış yapılamadı.
        </span>
      )}
      <Button onClick={() => void logout()} disabled={submitting} data-testid="admin-logout">
        Çıkış Yap
      </Button>
    </div>
  );
}
