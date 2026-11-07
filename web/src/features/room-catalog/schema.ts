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

export const MediaVariantSchema = z.object({
  url: z.string(),
  width: z.number().int().positive(),
  height: z.number().int().positive(),
  format: z.string(),
});

export const MediaItemSchema = z.object({
  id: z.string(),
  roomTypeId: z.string(),
  cover: z.boolean(),
  displayOrder: z.number().int().nonnegative(),
  altTextTr: z.string().nullable().optional(),
  altTextEn: z.string().nullable().optional(),
  variants: z.object({
    thumb: MediaVariantSchema.nullable(),
    card: MediaVariantSchema.nullable(),
    cover: MediaVariantSchema.nullable(),
  }),
});

export const MediaPageResponseSchema = z.object({
  items: z.array(MediaItemSchema),
});

export type AmenityDto = z.infer<typeof AmenitySchema>;
export type RoomTypeDto = z.infer<typeof RoomTypeSchema>;
export type RoomTypePageResponse = z.infer<typeof RoomTypePageResponseSchema>;
export type MediaItemDto = z.infer<typeof MediaItemSchema>;
