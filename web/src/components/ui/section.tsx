import type { ElementType, HTMLAttributes, ReactNode } from "react";
import { cn } from "./button";

export type SectionTone = "paper" | "raised" | "inverse" | "transparent";
export type SectionSpacing = "default" | "compact" | "none";

export interface SectionProps extends HTMLAttributes<HTMLElement> {
  as?: ElementType;
  tone?: SectionTone;
  spacing?: SectionSpacing;
  container?: boolean;
  containerClassName?: string;
  children: ReactNode;
}

const toneClasses: Record<SectionTone, string> = {
  paper: "bg-surface text-fg",
  raised: "bg-surface-raised text-fg",
  inverse: "bg-surface-inverse text-fg-inverse",
  transparent: "bg-transparent text-fg",
};

const spacingClasses: Record<SectionSpacing, string> = {
  default: "py-section",
  compact: "py-section-sm",
  none: "py-0",
};

export function Section({
  as: Component = "section",
  tone = "paper",
  spacing = "default",
  container = true,
  containerClassName,
  className,
  children,
  ...props
}: SectionProps) {
  return (
    <Component
      className={cn("relative", toneClasses[tone], spacingClasses[spacing], className)}
      {...props}
    >
      {container ? (
        <div className={cn("site-container", containerClassName)}>{children}</div>
      ) : (
        children
      )}
    </Component>
  );
}
