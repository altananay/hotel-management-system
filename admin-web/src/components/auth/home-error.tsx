"use client";

import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";

export function AdminHomeError() {
  const router = useRouter();

  return (
    <div className="rounded-xl border border-red-200 bg-red-50 p-6" role="alert" data-testid="admin-home-error">
      <p className="font-medium text-red-900">Yönetim verileri şu anda yüklenemedi.</p>
      <p className="mt-1 text-sm text-red-700">Lütfen kısa bir süre sonra tekrar deneyin.</p>
      <Button className="mt-4" onClick={() => router.refresh()} data-testid="admin-home-retry">
        Tekrar Dene
      </Button>
    </div>
  );
}
