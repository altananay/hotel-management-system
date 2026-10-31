"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { AdminApiClientError, adminApiPost } from "@/lib/api-client";
import { fetchCsrfToken } from "@/lib/csrf";

export function LogoutButton() {
  const router = useRouter();
  const [submitting, setSubmitting] = useState(false);
  const [failed, setFailed] = useState(false);

  const logout = async () => {
    setSubmitting(true);
    setFailed(false);
    try {
      try {
        await adminApiPost("/admin-api/auth/logout", {}, await fetchCsrfToken());
      } catch (error) {
        if (error instanceof AdminApiClientError && error.code === "security.csrf_invalid") {
          await adminApiPost("/admin-api/auth/logout", {}, await fetchCsrfToken());
        } else if (error instanceof AdminApiClientError && error.status === 401) {
          router.push("/login");
          return;
        } else {
          throw error;
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
