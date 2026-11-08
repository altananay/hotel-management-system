"use client";

import { useQuery } from "@tanstack/react-query";
import { adminApiGet } from "@/lib/api-client";
import {
  FxRateHistorySchema,
  ManualPriceSchema,
  PriceMatrixSchema,
  RatePlanSchema,
  RoomTypeOptionSchema,
  SettingsSchema,
} from "./schema";
import { z } from "zod";

export function useRatePlansQuery() {
  return useQuery({
    queryKey: ["pricing", "rate-plans"],
    queryFn: () =>
      adminApiGet("/admin-api/pricing/rate-plans", z.array(RatePlanSchema)),
  });
}

export function useSettingsQuery() {
  return useQuery({
    queryKey: ["pricing", "settings"],
    queryFn: () => adminApiGet("/admin-api/pricing/settings", SettingsSchema),
  });
}

export function useFxRatesQuery() {
  return useQuery({
    queryKey: ["pricing", "fx-rates"],
    queryFn: () =>
      adminApiGet(
        "/admin-api/pricing/fx-rates?page=0&size=50",
        FxRateHistorySchema,
      ),
  });
}

export function useRoomTypesQuery() {
  return useQuery({
    queryKey: ["pricing", "room-types"],
    queryFn: () =>
      adminApiGet(
        "/admin-api/pricing/room-types",
        z.array(RoomTypeOptionSchema),
      ),
  });
}

export function useBasePricesQuery(ratePlanId: string | null) {
  return useQuery({
    queryKey: ["pricing", "base-prices", ratePlanId],
    queryFn: () =>
      adminApiGet(
        `/admin-api/pricing/rate-plans/${ratePlanId}/prices`,
        PriceMatrixSchema,
      ),
    enabled: Boolean(ratePlanId),
  });
}

export function useSeasonPricesQuery(seasonId: string | null) {
  return useQuery({
    queryKey: ["pricing", "season-prices", seasonId],
    queryFn: () =>
      adminApiGet(
        `/admin-api/pricing/seasons/${seasonId}/prices`,
        PriceMatrixSchema,
      ),
    enabled: Boolean(seasonId),
  });
}

export function useManualPricesQuery(ratePlanId: string | null) {
  return useQuery({
    queryKey: ["pricing", "manual-prices", ratePlanId],
    queryFn: () =>
      adminApiGet(
        `/admin-api/pricing/manual-prices?ratePlanId=${ratePlanId}`,
        z.array(ManualPriceSchema),
      ),
    enabled: Boolean(ratePlanId),
  });
}

export function useManualFxRatesQuery() {
  return useQuery({
    queryKey: ["pricing", "manual-fx"],
    queryFn: () =>
      adminApiGet(
        "/admin-api/pricing/manual-fx-rates?page=0&size=50",
        z.object({
          items: z.array(
            z.object({
              id: z.string(),
              currency: z.string(),
              source: z.string(),
              rateDate: z.string(),
              rate: z.string(),
              capturedAt: z.string(),
              updatedAt: z.string(),
            }),
          ),
          page: z.number(),
          size: z.number(),
          total: z.number(),
        }),
      ),
  });
}
