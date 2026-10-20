"use client";

import { useQuery } from "@tanstack/react-query";
import { apiGet } from "@/lib/api-client";
import { RoomTypePageResponseSchema } from "./schema";

export function useRoomTypesQuery() {
  return useQuery({
    queryKey: ["catalog", "room-types", { page: 0, size: 100 }],
    queryFn: () => apiGet("/api/room-types?page=0&size=100", RoomTypePageResponseSchema),
  });
}
