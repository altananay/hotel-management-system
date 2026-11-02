import { redirect } from "next/navigation";
import { AdminHomeError } from "@/components/auth/home-error";
import { LogoutButton } from "@/components/auth/logout-button";
import { AdminAuthError, getAdminSession } from "@/lib/admin-auth";

export const dynamic = "force-dynamic";

export default async function AdminHomePage() {
  let session;
  try {
    session = await getAdminSession();
  } catch (error) {
    if (error instanceof AdminAuthError) {
      return (
        <main className="mx-auto max-w-4xl px-6 py-12" data-testid="admin-home-page">
          <AdminHomeError />
        </main>
      );
    }
    throw error;
  }

  if (!session) {
    redirect("/login?returnTo=/");
  }

  return (
    <main className="mx-auto max-w-4xl px-6 py-12" data-testid="admin-home-page">
      <header className="flex flex-wrap items-start justify-between gap-4 border-b border-slate-200 pb-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-900" data-testid="admin-site-title">
            Kapadokya Otel Yönetim Paneli
          </h1>
          <p className="mt-1 text-sm text-slate-600" data-testid="admin-site-subtitle">
            Operasyon, Oda, Fiyat ve Rezervasyon Yönetimi
          </p>
        </div>
        <div className="flex items-center gap-3">
          <span className="text-sm text-slate-600">
            {session.username} · {session.role}
          </span>
          <LogoutButton />
        </div>
      </header>

      <section className="mt-8 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-slate-900">Yönetim Paneli</h2>
        <p className="mt-2 text-sm text-slate-600">
          Oturumunuz doğrulandı. Yönetim modülleri bir sonraki dilimlerde etkinleştirilecek.
        </p>
      </section>
    </main>
  );
}
