import { z } from "zod";

export const AdminSampleItemSchema = z.object({
  id: z.string(),
  name: z.string(),
});

export const AdminSampleItemsResponseSchema = z.object({
  items: z.array(AdminSampleItemSchema),
});

export type AdminSampleItem = z.infer<typeof AdminSampleItemSchema>;
export type AdminSampleItemsResponse = z.infer<typeof AdminSampleItemsResponseSchema>;
