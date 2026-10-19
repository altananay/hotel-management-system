import type { HTMLAttributes } from "react";
import { cn } from "./button";

export function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn(
        "rounded-xl border border-stone-200 bg-white p-6 text-stone-950 shadow-sm",
        className,
      )}
      {...props}
    />
  );
}
