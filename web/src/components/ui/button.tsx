import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";
import type { ButtonHTMLAttributes } from "react";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export type ButtonVariant = "primary" | "secondary" | "ghost";
export type ButtonSize = "md" | "lg";

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
}

const variantClasses: Record<ButtonVariant, string> = {
  primary:
    "border border-accent bg-accent text-accent-contrast hover:border-accent-strong hover:bg-accent-strong",
  secondary:
    "border border-border-strong bg-transparent text-fg hover:border-accent hover:bg-surface-muted hover:text-accent-strong",
  ghost:
    "border border-transparent bg-transparent text-accent hover:bg-accent-soft/50 hover:text-accent-strong",
};

const sizeClasses: Record<ButtonSize, string> = {
  md: "px-5 py-2.5 text-small",
  lg: "px-7 py-3.5 text-body",
};

export function Button({
  className,
  variant = "primary",
  size = "md",
  type = "button",
  ...props
}: ButtonProps) {
  return (
    <button
      type={type}
      className={cn(
        "inline-flex items-center justify-center gap-2.5 rounded-full font-semibold tracking-wide transition-fast disabled:pointer-events-none disabled:opacity-50",
        variantClasses[variant],
        sizeClasses[size],
        className,
      )}
      {...props}
    />
  );
}
