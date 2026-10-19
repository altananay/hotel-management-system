import { AdminSystemStatusCard } from "@/features/system-status/admin-system-status-card";

export default function AdminHomePage() {
  return (
    <main className="mx-auto max-w-4xl px-6 py-12" data-testid="admin-home-page">
      <header className="mb-10 border-b border-slate-200 pb-6">
        <h1 className="text-2xl font-bold text-slate-900" data-testid="admin-site-title">
          Kapadokya Otel Yönetim Paneli
        </h1>
        <p className="mt-1 text-sm text-slate-600" data-testid="admin-site-subtitle">
          Operasyon, Oda, Fiyat ve Rezervasyon Yönetimi
        </p>
      </header>

      <AdminSystemStatusCard />
    </main>
  );
}
