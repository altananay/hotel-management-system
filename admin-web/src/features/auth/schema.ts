import { z } from "zod";

export const LoginResponseSchema = z.object({}).passthrough();
export const MeSchema = z.object({
  id: z.string(),
  username: z.string(),
  email: z.string(),
  role: z.string(),
});
export type Me = z.infer<typeof MeSchema>;
