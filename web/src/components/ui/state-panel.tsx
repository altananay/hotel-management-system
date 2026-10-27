import type { ReactNode } from "react";
import { AlertCircle, Compass } from "lucide-react";
import { Card } from "./card";
import { cn } from "./button";

export type StatePanelState = "loading" | "empty" | "error";

export interface StatePanelProps {
  state: StatePanelState;
  title?: ReactNode;
  message?: ReactNode;
  action?: ReactNode;
  children?: ReactNode;
  icon?: ReactNode;
  testId?: string;
  titleTestId?: string;
  messageTestId?: string;
  className?: string;
}

export function StatePanel({
  state,
  title,
  message,
  action,
  children,
  icon,
  testId,
  titleTestId,
  messageTestId,
  className,
}: StatePanelProps) {
  const isLoading = state === "loading";
  const isError = state === "error";
  const defaultIcon = isError ? (
    <AlertCircle className="h-6 w-6" aria-hidden="true" />
  ) : (
    <Compass className="h-6 w-6" aria-hidden="true" />
  );

  return (
    <Card
      data-testid={testId}
      role={isError ? "alert" : isLoading ? "status" : undefined}
      aria-live={isLoading ? "polite" : undefined}
      className={cn(
        "mx-auto max-w-2xl rounded-3xl p-8 text-center sm:p-10",
        isError && "border-danger-border bg-danger-surface text-danger-fg",
        className,
      )}
    >
      {!isLoading ? (
        <div
          className={cn(
            "mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full",
            isError
              ? "bg-danger-border/40 text-danger-fg"
              : "bg-accent-soft/70 text-accent-strong",
          )}
        >
          {icon ?? defaultIcon}
        </div>
      ) : null}

      {title ? (
        <p
          data-testid={titleTestId}
          className={cn(
            "text-lg font-semibold sm:text-xl",
            isError ? "text-danger-fg" : "text-fg",
          )}
        >
          {title}
        </p>
      ) : null}

      {message ? (
        <p
          data-testid={messageTestId}
          className={cn(
            "mt-2 text-small leading-relaxed",
            isError ? "text-danger-fg/85" : "text-fg-muted",
          )}
        >
          {message}
        </p>
      ) : null}

      {action ? <div className="mt-6 flex justify-center">{action}</div> : null}
      {children}
    </Card>
  );
}
