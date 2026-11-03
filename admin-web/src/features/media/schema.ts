import { z } from "zod";

export const MediaStatusSchema = z.enum([
  "UPLOADED",
  "PROCESSING",
  "READY",
  "FAILED",
]);

export const MediaVariantSchema = z.object({
  variant: z.enum(["thumb", "card", "cover"]),
  format: z.enum(["webp", "jpeg"]),
  url: z.string(),
  width: z.number().int().positive(),
  height: z.number().int().positive(),
});

export const MediaItemSchema = z.object({
  id: z.string(),
  type: z.string(),
  status: MediaStatusSchema,
  roomTypeId: z.string().nullable().optional(),
  published: z.boolean(),
  cover: z.boolean(),
  displayOrder: z.number().int().nonnegative(),
  altTextTr: z.string().nullable().optional(),
  altTextEn: z.string().nullable().optional(),
  original: z.object({
    mimeType: z.string(),
    sizeBytes: z.number().int().positive(),
    width: z.number().int().positive().nullable().optional(),
    height: z.number().int().positive().nullable().optional(),
  }),
  variants: z.array(MediaVariantSchema),
  failureCode: z.string().nullable().optional(),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export const MediaPageSchema = z.object({
  items: z.array(MediaItemSchema),
  page: z.number().int().nonnegative(),
  size: z.number().int().positive(),
  total: z.number().int().nonnegative(),
});

export const RoomTypeOptionSchema = z.object({
  id: z.string(),
  nameTr: z.string(),
  nameEn: z.string(),
});

export type MediaStatus = z.infer<typeof MediaStatusSchema>;
export type MediaItem = z.infer<typeof MediaItemSchema>;
export type MediaPage = z.infer<typeof MediaPageSchema>;
export type RoomTypeOption = z.infer<typeof RoomTypeOptionSchema>;
