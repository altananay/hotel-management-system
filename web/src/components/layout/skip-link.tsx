export interface SkipLinkProps {
  label: string;
}

export function SkipLink({ label }: SkipLinkProps) {
  return (
    <a
      href="#main-content"
      data-testid="skip-to-content"
      className="sr-only focus:not-sr-only focus:fixed focus:left-3 focus:top-3 focus:z-[100] focus:rounded-sm focus:bg-surface-inverse focus:px-4 focus:py-3 focus:text-small focus:font-semibold focus:text-fg-inverse"
    >
      {label}
    </a>
  );
}
