"use client";

import { useQuery } from "@tanstack/react-query";
import { apiGet } from "@/lib/api-client";
import { MediaPageResponseSchema, type MediaItemDto } from "./schema";

const MAX_IDS_PER_REQUEST = 50;

async function fetchMedia(roomTypeIds: string[]): Promise<MediaItemDto[]> {
  const chunks: string[][] = [];
  for (let index = 0; index < roomTypeIds.length; index += MAX_IDS_PER_REQUEST) {
    chunks.push(roomTypeIds.slice(index, index + MAX_IDS_PER_REQUEST));
  }
  const responses = await Promise.all(
    chunks.map((chunk) =>
      apiGet(
        `/api/media?roomTypeIds=${encodeURIComponent(chunk.join(","))}`,
        MediaPageResponseSchema,
      ),
    ),
  );
  return responses.flatMap((response) => response.items);
}

export function useRoomMediaQuery(roomTypeIds: string[]) {
  return useQuery({
    queryKey: ["catalog", "media", roomTypeIds],
    queryFn: () => fetchMedia(roomTypeIds),
    enabled: roomTypeIds.length > 0,
  });
}
