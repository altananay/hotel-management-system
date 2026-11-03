import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";
import type { ButtonHTMLAttributes } from "react";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

type ButtonVariant = "primary" | "secondary";
type ButtonSize = "md" | "sm";

export function Button({
  className,
  variant = "primary",
  size = "md",
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  size?: ButtonSize;
}) {
  return (
    <button
      type="button"
      className={cn(
        "inline-flex items-center justify-center gap-1.5 rounded-md text-sm font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-900 disabled:pointer-events-none disabled:opacity-50",
        variant === "primary"
          ? "bg-slate-900 text-slate-50 hover:bg-slate-800"
          : "border border-slate-300 bg-white text-slate-900 hover:bg-slate-50",
        size === "sm" ? "px-3 py-1.5 text-xs" : "px-4 py-2",
        className,
      )}
      {...props}
    />
  );
}
