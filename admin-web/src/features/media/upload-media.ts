"use client";

import { z } from "zod";
import {
  AdminApiClientError,
  adminApiRequest,
  adminApiSend,
} from "@/lib/api-client";
import { fetchCsrfToken, readCsrfToken } from "@/lib/csrf";

const MediaUploadSchema = z.object({
  mediaId: z.string(),
  uploadUrl: z.string(),
  expiresAt: z.string(),
});

async function withCsrf(action: (token: string) => Promise<void>) {
  let token = readCsrfToken() ?? (await fetchCsrfToken());
  try {
    await action(token);
  } catch (error) {
    if (
      error instanceof AdminApiClientError &&
      error.code === "security.csrf_invalid"
    ) {
      token = await fetchCsrfToken();
      await action(token);
      return;
    }
    throw error;
  }
}

export async function uploadMedia(file: File): Promise<void> {
  const contentType = file.type || "application/octet-stream";
  const start = await adminApiRequest(
    "/admin-api/media/uploads",
    MediaUploadSchema,
    {
      method: "POST",
      body: {
        fileName: file.name,
        contentType,
        sizeBytes: file.size,
      },
      csrfToken: readCsrfToken() ?? (await fetchCsrfToken()),
    },
  );

  const upload = await fetch(start.uploadUrl, {
    method: "PUT",
    headers: { "Content-Type": contentType },
    body: file,
  });
  if (!upload.ok) {
    throw new Error("media_upload_failed");
  }

  await withCsrf((csrfToken) =>
    adminApiSend(`/admin-api/media/${start.mediaId}/complete`, {
      method: "POST",
      csrfToken,
    }),
  );
}
