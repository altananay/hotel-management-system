export function PricingForbidden() {
  return (
    <main
      data-testid="admin-pricing-page"
      className="mx-auto max-w-2xl px-6 py-12"
    >
      <div
        role="alert"
        data-testid="admin-pricing-forbidden"
        className="rounded-xl border border-amber-200 bg-amber-50 p-6 text-amber-900"
      >
        <h1 className="text-xl font-semibold">Yetkiniz yok</h1>
        <p className="mt-2 text-sm">
          Fiyat yönetimi yalnız SUPER_ADMIN ve MANAGER rollerine açıktır.
        </p>
      </div>
    </main>
  );
}
