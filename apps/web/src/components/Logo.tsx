interface Props {
  size?: number
}

/** Seekker AI's own mark: a magnifier on a rounded square. */
export default function Logo({ size = 28 }: Props) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true">
      <rect width="32" height="32" rx="8" fill="var(--accent)" />
      <circle cx="14" cy="14" r="6" fill="none" stroke="var(--accent-contrast)" strokeWidth="2.5" />
      <path d="M18.6 18.6 24 24" stroke="var(--accent-contrast)" strokeWidth="2.5" strokeLinecap="round" />
    </svg>
  )
}
