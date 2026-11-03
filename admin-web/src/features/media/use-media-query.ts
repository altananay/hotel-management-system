"use client";

import { useQuery } from "@tanstack/react-query";
import { adminApiGet } from "@/lib/api-client";
import { MediaPageSchema, RoomTypeOptionSchema } from "./schema";
import { z } from "zod";

async function fetchAllMedia() {
  const pageSize = 100;
  const first = await adminApiGet(
    `/admin-api/media?page=0&size=${pageSize}`,
    MediaPageSchema,
  );
  const items = [...first.items];
  const pageCount = Math.ceil(first.total / pageSize);
  for (let page = 1; page < pageCount; page += 1) {
    const next = await adminApiGet(
      `/admin-api/media?page=${page}&size=${pageSize}`,
      MediaPageSchema,
    );
    items.push(...next.items);
  }
  return { ...first, items };
}

export function useMediaQuery() {
  return useQuery({
    queryKey: ["media", "list"],
    // Reorder requires the complete non-archived list; fetch all pages.
    queryFn: fetchAllMedia,
    refetchInterval: (query) =>
      query.state.data?.items.some((item) => item.status === "PROCESSING")
        ? 3000
        : false,
  });
}

export function useRoomTypeOptionsQuery() {
  return useQuery({
    queryKey: ["media", "room-types"],
    queryFn: () =>
      adminApiGet(
        "/admin-api/media/room-types",
        z.array(RoomTypeOptionSchema),
      ),
  });
}
