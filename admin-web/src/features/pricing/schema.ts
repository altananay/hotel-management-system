import { z } from "zod";

export const SeasonSchema = z.object({
  id: z.string(),
  ratePlanId: z.string(),
  code: z.string(),
  nameTr: z.string(),
  nameEn: z.string(),
  startDate: z.string(),
  endDate: z.string(),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export const RatePlanSchema = z.object({
  id: z.string(),
  code: z.string(),
  nameTr: z.string(),
  nameEn: z.string(),
  active: z.boolean(),
  defaultPlan: z.boolean(),
  createdAt: z.string(),
  updatedAt: z.string(),
  seasons: z.array(SeasonSchema),
});

export const MatrixRoomTypeSchema = z.object({
  id: z.string(),
  nameTr: z.string(),
  nameEn: z.string(),
  maxGuests: z.number(),
  active: z.boolean(),
});

export const PriceRowSchema = z.object({
  id: z.string(),
  roomTypeId: z.string(),
  guests: z.number(),
  amountTry: z.string(),
  updatedAt: z.string(),
});

export const MissingPriceSchema = z.object({
  roomTypeId: z.string(),
  guests: z.number(),
});

export const PriceMatrixSchema = z.object({
  roomTypes: z.array(MatrixRoomTypeSchema),
  rows: z.array(PriceRowSchema),
  missing: z.array(MissingPriceSchema),
});

export const ManualPriceSchema = z.object({
  id: z.string(),
  ratePlanId: z.string(),
  roomTypeId: z.string(),
  roomTypeNameTr: z.string().nullable(),
  roomTypeNameEn: z.string().nullable(),
  guests: z.number(),
  startDate: z.string(),
  endDate: z.string(),
  amountTry: z.string(),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export const FxRateSchema = z.object({
  id: z.string(),
  currency: z.string(),
  source: z.string(),
  rateDate: z.string(),
  rate: z.string(),
  capturedAt: z.string(),
  updatedAt: z.string(),
});

export const FxRateHistorySchema = z.object({
  latest: z.array(FxRateSchema),
  items: z.array(FxRateSchema),
  page: z.number(),
  size: z.number(),
  total: z.number(),
  stale: z.boolean(),
  staleCurrencies: z.array(z.string()),
});

export const SettingsSchema = z.object({
  bufferPercent: z.string(),
  roundingStep: z.string(),
  updatedAt: z.string(),
});

export const RoomTypeOptionSchema = z.object({
  id: z.string(),
  nameTr: z.string(),
  nameEn: z.string(),
  maxGuests: z.number(),
});

export type Season = z.infer<typeof SeasonSchema>;
export type RatePlan = z.infer<typeof RatePlanSchema>;
export type PriceMatrix = z.infer<typeof PriceMatrixSchema>;
export type ManualPrice = z.infer<typeof ManualPriceSchema>;
export type FxRate = z.infer<typeof FxRateSchema>;
export type FxRateHistory = z.infer<typeof FxRateHistorySchema>;
export type Settings = z.infer<typeof SettingsSchema>;
export type RoomTypeOption = z.infer<typeof RoomTypeOptionSchema>;
