"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { AdminApiClientError, adminApiPost } from "@/lib/api-client";
import { fetchCsrfToken } from "@/lib/csrf";
import { resolveAdminErrorMessage } from "@/lib/error-messages";
import { safeReturnTo } from "@/lib/return-to";

export function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [csrfToken, setCsrfToken] = useState<string | null>(null);
  const [csrfState, setCsrfState] = useState<"loading" | "ready" | "error">("loading");
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [errorCode, setErrorCode] = useState<string | null>(null);

  const loadCsrf = async () => {
    setCsrfState("loading");
    setErrorCode(null);
    try {
      setCsrfToken(await fetchCsrfToken());
      setCsrfState("ready");
    } catch {
      setCsrfToken(null);
      setCsrfState("error");
    }
  };

  useEffect(() => {
    void loadCsrf();
  }, []);

  const submit = async (token: string) => {
    await adminApiPost(
      "/admin-api/auth/login",
      { identifier, password },
      token,
    );
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!csrfToken || submitting) {
      return;
    }
    setSubmitting(true);
    setErrorCode(null);
    try {
      try {
        await submit(csrfToken);
      } catch (error) {
        if (error instanceof AdminApiClientError && error.code === "security.csrf_invalid") {
          const freshToken = await fetchCsrfToken();
          await submit(freshToken);
        } else {
          throw error;
        }
      }
      router.push(safeReturnTo(searchParams.get("returnTo")));
    } catch (error) {
      setErrorCode(
        error instanceof AdminApiClientError
          ? (error.code ?? "system.network_error")
          : "system.network_error",
      );
    } finally {
      setSubmitting(false);
    }
  };

  if (csrfState === "loading") {
    return (
      <div
        className="text-sm text-slate-600"
        role="status"
        aria-live="polite"
        data-testid="admin-login-loading"
      >
        Giriş hazırlanıyor...
      </div>
    );
  }

  if (csrfState === "error") {
    return (
      <div className="rounded-lg border border-red-200 bg-red-50 p-4" role="alert" data-testid="admin-login-error">
        <p className="text-sm text-red-800">Giriş formu hazırlanamadı.</p>
        <Button className="mt-3" onClick={() => void loadCsrf()} data-testid="admin-login-retry">
          Tekrar Dene
        </Button>
      </div>
    );
  }

  return (
    <Card className="mx-auto w-full max-w-md">
      <h1 className="text-xl font-semibold text-slate-950">Yönetim Paneli Girişi</h1>
      <p className="mt-1 text-sm text-slate-600">Devam etmek için hesabınızla giriş yapın.</p>
      <form className="mt-6 space-y-4" onSubmit={handleSubmit} data-testid="admin-login-form">
        <label className="block text-sm font-medium text-slate-700">
          Kullanıcı adı veya e-posta
          <input
            className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-slate-900"
            value={identifier}
            onChange={(event) => setIdentifier(event.target.value)}
            autoComplete="username"
            required
            data-testid="admin-login-identifier"
          />
        </label>
        <label className="block text-sm font-medium text-slate-700">
          Parola
          <input
            className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-slate-900"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
            required
            data-testid="admin-login-password"
          />
        </label>
        {errorCode && (
          <p className="text-sm text-red-700" role="alert" data-testid="admin-login-error">
            {resolveAdminErrorMessage(errorCode)}
          </p>
        )}
        {submitting && (
          <p className="text-sm text-slate-600" role="status" data-testid="admin-login-loading">
            Giriş yapılıyor...
          </p>
        )}
        <Button type="submit" className="w-full" disabled={submitting} data-testid="admin-login-submit">
          Giriş Yap
        </Button>
      </form>
    </Card>
  );
}
