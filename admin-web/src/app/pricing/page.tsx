import { redirect } from "next/navigation";
import { AdminHomeError } from "@/components/auth/home-error";
import { PricingForbidden } from "@/features/pricing/components/pricing-forbidden";
import { PricingManager } from "@/features/pricing/pricing-manager";
import { AdminAuthError, getAdminSession } from "@/lib/admin-auth";

export const dynamic = "force-dynamic";

export default async function PricingPage() {
  let session;
  try {
    session = await getAdminSession();
  } catch (error) {
    if (error instanceof AdminAuthError) {
      return <AdminHomeError />;
    }
    throw error;
  }
  if (!session) {
    redirect("/login?returnTo=/pricing");
  }
  if (session.role === "FRONT_DESK") {
    return <PricingForbidden />;
  }
  return <PricingManager />;
}
