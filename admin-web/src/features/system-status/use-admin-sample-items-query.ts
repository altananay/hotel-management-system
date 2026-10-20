"use client";

import { useQuery } from "@tanstack/react-query";
import { adminApiGet } from "@/lib/api-client";
import { AdminSampleItemsResponseSchema } from "./schema";

export function useAdminSampleItemsQuery() {
  return useQuery({
    queryKey: ["admin", "system", "sample-items"],
    queryFn: () =>
      adminApiGet("/admin-api/system/sample-items", AdminSampleItemsResponseSchema),
  });
}
