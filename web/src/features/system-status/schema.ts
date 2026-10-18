import { z } from "zod";

export const SampleItemSchema = z.object({
  id: z.string(),
  name: z.string(),
});

export const SampleItemsResponseSchema = z.object({
  items: z.array(SampleItemSchema),
});

export type SampleItem = z.infer<typeof SampleItemSchema>;
export type SampleItemsResponse = z.infer<typeof SampleItemsResponseSchema>;
