"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createElement, type ReactNode, useState } from "react";

export function createAdminQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        refetchOnWindowFocus: false,
      },
    },
  });
}

export function AdminQueryProvider({ children }: { children: ReactNode }) {
  const [queryClient] = useState(() => createAdminQueryClient());
  return createElement(QueryClientProvider, { client: queryClient }, children);
}
