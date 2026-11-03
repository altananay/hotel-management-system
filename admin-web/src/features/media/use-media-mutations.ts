"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  AdminApiClientError,
  adminApiRequest,
  adminApiSend,
} from "@/lib/api-client";
import { fetchCsrfToken, readCsrfToken } from "@/lib/csrf";
import { MediaItemSchema } from "./schema";

async function withCsrf<T>(
  action: (csrfToken: string) => Promise<T>,
): Promise<T> {
  let token = readCsrfToken() ?? (await fetchCsrfToken());
  try {
    return await action(token);
  } catch (error) {
    if (
      error instanceof AdminApiClientError &&
      error.code === "security.csrf_invalid"
    ) {
      token = await fetchCsrfToken();
      return action(token);
    }
    throw error;
  }
}

export function useMediaMutations() {
  const queryClient = useQueryClient();
  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ["media"] });

  const update = useMutation({
    mutationFn: (input: {
      id: string;
      roomTypeId?: string | null;
      altTextTr?: string;
      altTextEn?: string;
    }) =>
      withCsrf((csrfToken) =>
        adminApiRequest(`/admin-api/media/${input.id}`, MediaItemSchema, {
          method: "PATCH",
          body: {
            roomTypeId: input.roomTypeId ?? undefined,
            altTextTr: input.altTextTr,
            altTextEn: input.altTextEn,
          },
          csrfToken,
        }),
      ),
    onSuccess: invalidate,
  });

  const reorder = useMutation({
    mutationFn: (input: { roomTypeId: string; mediaIds: string[] }) =>
      withCsrf((csrfToken) =>
        adminApiSend("/admin-api/media/order", {
          method: "PUT",
          body: { roomTypeId: input.roomTypeId, mediaIds: input.mediaIds },
          csrfToken,
        }),
      ),
    onSuccess: invalidate,
  });

  const cover = useMutation({
    mutationFn: (id: string) =>
      withCsrf((csrfToken) =>
        adminApiRequest(`/admin-api/media/${id}/cover`, MediaItemSchema, {
          method: "PUT",
          csrfToken,
        }),
      ),
    onSuccess: invalidate,
  });

  const publish = useMutation({
    mutationFn: (id: string) =>
      withCsrf((csrfToken) =>
        adminApiSend(`/admin-api/media/${id}/publish`, {
          method: "POST",
          csrfToken,
        }),
      ),
    onSuccess: invalidate,
  });

  const unpublish = useMutation({
    mutationFn: (id: string) =>
      withCsrf((csrfToken) =>
        adminApiSend(`/admin-api/media/${id}/unpublish`, {
          method: "POST",
          csrfToken,
        }),
      ),
    onSuccess: invalidate,
  });

  const retry = useMutation({
    mutationFn: (id: string) =>
      withCsrf((csrfToken) =>
        adminApiSend(`/admin-api/media/${id}/retry`, {
          method: "POST",
          csrfToken,
        }),
      ),
    onSuccess: invalidate,
  });

  const archive = useMutation({
    mutationFn: (id: string) =>
      withCsrf((csrfToken) =>
        adminApiSend(`/admin-api/media/${id}`, {
          method: "DELETE",
          csrfToken,
        }),
      ),
    onSuccess: invalidate,
  });

  return { update, reorder, cover, publish, unpublish, retry, archive };
}
