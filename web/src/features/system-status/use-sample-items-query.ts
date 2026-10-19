"use client";

import { useQuery } from "@tanstack/react-query";
import { apiGet } from "@/lib/api-client";
import { SampleItemsResponseSchema } from "./schema";

export function useSampleItemsQuery() {
  return useQuery({
    queryKey: ["system", "sample-items"],
    queryFn: () => apiGet("/api/system/sample-items", SampleItemsResponseSchema),
  });
}
