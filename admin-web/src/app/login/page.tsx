import { Suspense } from "react";
import { LoginForm } from "@/components/auth/login-form";

export default function LoginPage() {
  return (
    <main className="flex min-h-screen items-center justify-center px-6" data-testid="admin-login-page">
      <Suspense
        fallback={
          <p className="text-sm text-slate-600" role="status" data-testid="admin-login-loading">
            Giriş hazırlanıyor...
          </p>
        }
      >
        <LoginForm />
      </Suspense>
    </main>
  );
}
