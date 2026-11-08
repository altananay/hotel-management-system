"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { adminApiRequest, adminApiSend } from "@/lib/api-client";
import { withCsrfRetry } from "@/lib/csrf-retry";
import {
  ManualPriceSchema,
  PriceMatrixSchema,
  RatePlanSchema,
  SeasonSchema,
  SettingsSchema,
} from "./schema";
import { z } from "zod";

const ManualFxSchema = z.object({
  id: z.string(),
  currency: z.string(),
  source: z.string(),
  rateDate: z.string(),
  rate: z.string(),
  capturedAt: z.string(),
  updatedAt: z.string(),
});

export function usePricingMutations() {
  const queryClient = useQueryClient();
  const invalidateAll = () =>
    queryClient.invalidateQueries({ queryKey: ["pricing"] });

  const createRatePlan = useMutation({
    mutationFn: (input: {
      code: string;
      nameTr: string;
      nameEn: string;
      active: boolean;
    }) =>
      withCsrfRetry((csrfToken) =>
        adminApiRequest("/admin-api/pricing/rate-plans", RatePlanSchema, {
          method: "POST",
          body: input,
          csrfToken,
        }),
      ),
    onSuccess: invalidateAll,
  });

  const updateRatePlan = useMutation({
    mutationFn: (input: {
      id: string;
      nameTr?: string;
      nameEn?: string;
      active?: boolean;
    }) =>
      withCsrfRetry((csrfToken) =>
        adminApiRequest(
          `/admin-api/pricing/rate-plans/${input.id}`,
          RatePlanSchema,
          {
            method: "PATCH",
            body: {
              nameTr: input.nameTr,
              nameEn: input.nameEn,
              active: input.active,
            },
            csrfToken,
          },
        ),
      ),
    onSuccess: invalidateAll,
  });

  const setDefault = useMutation({
    mutationFn: (input: {
      id: string;
      expectedDefaultPlanId: string | null;
    }) =>
      withCsrfRetry((csrfToken) =>
        adminApiRequest(
          `/admin-api/pricing/rate-plans/${input.id}/default`,
          RatePlanSchema,
          {
            method: "PUT",
            body: { expectedDefaultPlanId: input.expectedDefaultPlanId },
            csrfToken,
          },
        ),
      ),
    onSuccess: invalidateAll,
  });

  const createSeason = useMutation({
    mutationFn: (input: {
      ratePlanId: string;
      code: string;
      nameTr: string;
      nameEn: string;
      startDate: string;
      endDate: string;
    }) =>
      withCsrfRetry((csrfToken) =>
        adminApiRequest(
          `/admin-api/pricing/rate-plans/${input.ratePlanId}/seasons`,
          SeasonSchema,
          {
            method: "POST",
            body: {
              code: input.code,
              nameTr: input.nameTr,
              nameEn: input.nameEn,
              startDate: input.startDate,
              endDate: input.endDate,
            },
            csrfToken,
          },
        ),
      ),
    onSuccess: invalidateAll,
  });

  const updateSeason = useMutation({
    mutationFn: (input: {
      id: string;
      nameTr?: string;
      nameEn?: string;
      startDate?: string;
      endDate?: string;
    }) =>
      withCsrfRetry((csrfToken) =>
        adminApiRequest(
          `/admin-api/pricing/seasons/${input.id}`,
          SeasonSchema,
          {
            method: "PATCH",
            body: {
              nameTr: input.nameTr,
              nameEn: input.nameEn,
              startDate: input.startDate,
              endDate: input.endDate,
            },
            csrfToken,
          },
        ),
      ),
    onSuccess: invalidateAll,
  });

  const deleteSeason = useMutation({
    mutationFn: (id: string) =>
      withCsrfRetry((csrfToken) =>
        adminApiSend(`/admin-api/pricing/seasons/${id}`, {
          method: "DELETE",
          csrfToken,
        }),
      ),
    onSuccess: invalidateAll,
  });

  const saveBasePrices = useMutation({
    mutationFn: (input: {
      ratePlanId: string;
      rows: Array<{ roomTypeId: string; guests: number; amountTry: string }>;
    }) =>
      withCsrfRetry((csrfToken) =>
        adminApiRequest(
          `/admin-api/pricing/rate-plans/${input.ratePlanId}/prices`,
          PriceMatrixSchema,
          {
            method: "PUT",
            body: { rows: input.rows },
            csrfToken,
          },
        ),
      ),
    onSuccess: invalidateAll,
  });

  const saveSeasonPrices = useMutation({
    mutationFn: (input: {
      seasonId: string;
      rows: Array<{ roomTypeId: string; guests: number; amountTry: string }>;
    }) =>
      withCsrfRetry((csrfToken) =>
        adminApiRequest(
          `/admin-api/pricing/seasons/${input.seasonId}/prices`,
          PriceMatrixSchema,
          {
            method: "PUT",
            body: { rows: input.rows },
            csrfToken,
          },
        ),
      ),
    onSuccess: invalidateAll,
  });

  const deleteBasePrice = useMutation({
    mutationFn: (input: { ratePlanId: string; priceRowId: string }) =>
      withCsrfRetry((csrfToken) =>
        adminApiSend(
          `/admin-api/pricing/rate-plans/${input.ratePlanId}/prices/${input.priceRowId}`,
          { method: "DELETE", csrfToken },
        ),
      ),
    onSuccess: invalidateAll,
  });

  const deleteSeasonPrice = useMutation({
    mutationFn: (input: { seasonId: string; priceRowId: string }) =>
      withCsrfRetry((csrfToken) =>
        adminApiSend(
          `/admin-api/pricing/seasons/${input.seasonId}/prices/${input.priceRowId}`,
          { method: "DELETE", csrfToken },
        ),
      ),
    onSuccess: invalidateAll,
  });

  const createManualPrice = useMutation({
    mutationFn: (input: {
      ratePlanId: string;
      roomTypeId: string;
      guests: number;
      startDate: string;
      endDate: string;
      amountTry: string;
    }) =>
      withCsrfRetry((csrfToken) =>
        adminApiRequest(
          "/admin-api/pricing/manual-prices",
          ManualPriceSchema,
          { method: "POST", body: input, csrfToken },
        ),
      ),
    onSuccess: invalidateAll,
  });

  const updateManualPrice = useMutation({
    mutationFn: (input: {
      id: string;
      roomTypeId?: string;
      guests?: number;
      startDate?: string;
      endDate?: string;
      amountTry?: string;
    }) =>
      withCsrfRetry((csrfToken) =>
        adminApiRequest(
          `/admin-api/pricing/manual-prices/${input.id}`,
          ManualPriceSchema,
          {
            method: "PATCH",
            body: {
              roomTypeId: input.roomTypeId,
              guests: input.guests,
              startDate: input.startDate,
              endDate: input.endDate,
              amountTry: input.amountTry,
            },
            csrfToken,
          },
        ),
      ),
    onSuccess: invalidateAll,
  });

  const deleteManualPrice = useMutation({
    mutationFn: (id: string) =>
      withCsrfRetry((csrfToken) =>
        adminApiSend(`/admin-api/pricing/manual-prices/${id}`, {
          method: "DELETE",
          csrfToken,
        }),
      ),
    onSuccess: invalidateAll,
  });

  const createManualFxRate = useMutation({
    mutationFn: (input: {
      currency: string;
      rateDate: string;
      rate: string;
    }) =>
      withCsrfRetry((csrfToken) =>
        adminApiRequest(
          "/admin-api/pricing/manual-fx-rates",
          ManualFxSchema,
          { method: "POST", body: input, csrfToken },
        ),
      ),
    onSuccess: invalidateAll,
  });

  const deleteManualFxRate = useMutation({
    mutationFn: (id: string) =>
      withCsrfRetry((csrfToken) =>
        adminApiSend(`/admin-api/pricing/manual-fx-rates/${id}`, {
          method: "DELETE",
          csrfToken,
        }),
      ),
    onSuccess: invalidateAll,
  });

  const updateSettings = useMutation({
    mutationFn: (input: { bufferPercent: string; roundingStep: string }) =>
      withCsrfRetry((csrfToken) =>
        adminApiRequest("/admin-api/pricing/settings", SettingsSchema, {
          method: "PUT",
          body: input,
          csrfToken,
        }),
      ),
    onSuccess: invalidateAll,
  });

  return {
    createRatePlan,
    updateRatePlan,
    setDefault,
    createSeason,
    updateSeason,
    deleteSeason,
    saveBasePrices,
    saveSeasonPrices,
    deleteBasePrice,
    deleteSeasonPrice,
    createManualPrice,
    updateManualPrice,
    deleteManualPrice,
    createManualFxRate,
    deleteManualFxRate,
    updateSettings,
  };
}
