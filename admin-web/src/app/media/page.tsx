import { redirect } from "next/navigation";
import { AdminHomeError } from "@/components/auth/home-error";
import { MediaManager } from "@/features/media/media-manager";
import { AdminAuthError, getAdminSession } from "@/lib/admin-auth";

export const dynamic = "force-dynamic";

export default async function MediaPage() {
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
    redirect("/login?returnTo=/media");
  }
  return <MediaManager />;
}
