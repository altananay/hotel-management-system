import type { HTMLAttributes, ReactNode } from "react";
import { cn } from "./button";

export type BadgeTone = "accent" | "outline" | "inverse" | "muted";

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  tone?: BadgeTone;
  children: ReactNode;
}

const toneClasses: Record<BadgeTone, string> = {
  accent: "border-accent/30 bg-accent-soft/55 text-accent-strong",
  outline: "border-border-strong bg-transparent text-fg-muted",
  inverse: "border-fg-inverse/25 bg-fg-inverse/10 text-fg-inverse",
  muted: "border-border bg-surface-muted text-fg-muted",
};

export function Badge({ tone = "outline", className, children, ...props }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-eyebrow font-semibold uppercase",
        toneClasses[tone],
        className,
      )}
      {...props}
    >
      {children}
    </span>
  );
}
