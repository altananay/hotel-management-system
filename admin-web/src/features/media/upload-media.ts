"use client";

import { z } from "zod";
import { adminApiRequest, adminApiSend } from "@/lib/api-client";
import { withCsrfRetry } from "@/lib/csrf-retry";

const MediaUploadSchema = z.object({
  mediaId: z.string(),
  uploadUrl: z.string(),
  expiresAt: z.string(),
});

export async function uploadMedia(file: File): Promise<void> {
  const contentType = file.type || "application/octet-stream";
  const start = await withCsrfRetry((csrfToken) =>
    adminApiRequest("/admin-api/media/uploads", MediaUploadSchema, {
      method: "POST",
      body: {
        fileName: file.name,
        contentType,
        sizeBytes: file.size,
      },
      csrfToken,
    }),
  );

  const upload = await fetch(start.uploadUrl, {
    method: "PUT",
    headers: { "Content-Type": contentType },
    body: file,
  });
  if (!upload.ok) {
    throw new Error("media_upload_failed");
  }

  await withCsrfRetry((csrfToken) =>
    adminApiSend(`/admin-api/media/${start.mediaId}/complete`, {
      method: "POST",
      csrfToken,
    }),
  );
}
