export function FlagTr({ className = "" }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 24 16"
      aria-hidden="true"
      className={`mr-1 inline-block h-3 w-[1.125rem] shrink-0 rounded-[2px] align-[-1px] ${className}`}
    >
      <rect width="24" height="16" fill="#E30A17" />
      <circle cx="9" cy="8" r="5" fill="#FFFFFF" />
      <circle cx="10.5" cy="8" r="4" fill="#E30A17" />
      <path
        d="M15.4 5.2l.72 1.94h2.05l-1.66 1.2.64 1.94-1.75-1.15-1.75 1.15.64-1.94-1.66-1.2h2.05z"
        fill="#FFFFFF"
      />
    </svg>
  );
}

export function FlagGb({ className = "" }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 24 16"
      aria-hidden="true"
      className={`mr-1 inline-block h-3 w-[1.125rem] shrink-0 rounded-[2px] align-[-1px] ${className}`}
    >
      <rect width="24" height="16" fill="#012169" />
      <path d="M0 0l24 16M24 0L0 16" stroke="#FFFFFF" strokeWidth="3" />
      <path d="M0 0l24 16M24 0L0 16" stroke="#C8102E" strokeWidth="1.5" />
      <path d="M12 0v16M0 8h24" stroke="#FFFFFF" strokeWidth="5" />
      <path d="M12 0v16M0 8h24" stroke="#C8102E" strokeWidth="3" />
    </svg>
  );
}
