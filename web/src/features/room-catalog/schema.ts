import { z } from "zod";

export const AmenitySchema = z.object({
  code: z.string(),
  nameTr: z.string(),
  nameEn: z.string(),
});

export const RoomTypeSchema = z.object({
  id: z.string(),
  nameTr: z.string(),
  nameEn: z.string(),
  descriptionTr: z.string(),
  descriptionEn: z.string(),
  maxGuests: z.number().int().min(1).max(20),
  amenities: z.array(AmenitySchema),
});

export const RoomTypePageResponseSchema = z.object({
  items: z.array(RoomTypeSchema),
  page: z.number().int().nonnegative(),
  size: z.number().int().positive(),
  total: z.number().int().nonnegative(),
});

export type AmenityDto = z.infer<typeof AmenitySchema>;
export type RoomTypeDto = z.infer<typeof RoomTypeSchema>;
export type RoomTypePageResponse = z.infer<typeof RoomTypePageResponseSchema>;
