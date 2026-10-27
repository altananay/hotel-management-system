import type { HTMLAttributes } from "react";
import { cn } from "./button";

export function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn(
        "rounded-2xl border border-border bg-surface-raised p-6 text-fg shadow-card",
        className,
      )}
      {...props}
    />
  );
}
