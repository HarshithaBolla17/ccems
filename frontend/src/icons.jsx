const base = {
  width: 18,
  height: 18,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.9,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
}
const mk = (children) => (props) => (
  <svg {...base} {...props}>
    {children}
  </svg>
)

export const SearchIcon = mk(<><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></>)
export const CalendarIcon = mk(<><rect x="3.5" y="5" width="17" height="15.5" rx="3" /><path d="M8 3v4M16 3v4M3.5 10h17" /></>)
export const PinIcon = mk(<><path d="M12 21s-7-6.2-7-11.2A7 7 0 0 1 19 9.8C19 14.8 12 21 12 21z" /><circle cx="12" cy="9.8" r="2.4" /></>)
export const UsersIcon = mk(<><circle cx="9" cy="8" r="3.4" /><path d="M2.5 20c.6-3.6 3.2-5.5 6.5-5.5s5.9 1.9 6.5 5.5" /><path d="M16 4.8a3.2 3.2 0 0 1 0 6.2M18 14.8c1.9.6 3.1 2.3 3.5 5.2" /></>)
export const CheckIcon = mk(<path d="m5 12.5 4.5 4.5L19 7.5" />)
export const XIcon = mk(<path d="M6 6l12 12M18 6 6 18" />)
export const SunIcon = mk(<><circle cx="12" cy="12" r="4" /><path d="M12 2.5v2.2M12 19.3v2.2M2.5 12h2.2M19.3 12h2.2M5.3 5.3l1.6 1.6M17.1 17.1l1.6 1.6M18.7 5.3l-1.6 1.6M6.9 17.1l-1.6 1.6" /></>)
export const MoonIcon = mk(<path d="M20 14.5A8.5 8.5 0 0 1 9.5 4a8.5 8.5 0 1 0 10.5 10.5z" />)
export const MenuIcon = mk(<path d="M4 7h16M4 12h16M4 17h16" />)
export const PlusIcon = mk(<path d="M12 5v14M5 12h14" />)
export const DownloadIcon = mk(<><path d="M12 4v11M7.5 11 12 15.5 16.5 11" /><path d="M4.5 19.5h15" /></>)
export const PrintIcon = mk(<><path d="M7 9V3.5h10V9" /><rect x="3.5" y="9" width="17" height="8" rx="2.5" /><path d="M7 14h10v6.5H7z" /></>)
export const ClockIcon = mk(<><circle cx="12" cy="12" r="8.5" /><path d="M12 7.5V12l3 2" /></>)
export const LogoutIcon = mk(<><path d="M14 4.5h4.5a1.5 1.5 0 0 1 1.5 1.5v12a1.5 1.5 0 0 1-1.5 1.5H14" /><path d="M10 8l-4 4 4 4M6 12h10" /></>)
export const AwardIcon = mk(<><circle cx="12" cy="9" r="5.5" /><path d="m8.8 13.6-1.3 6.9 4.5-2.5 4.5 2.5-1.3-6.9" /></>)
export const TicketIcon = mk(<><path d="M3.5 8.5A2 2 0 0 1 5.5 6.5h13a2 2 0 0 1 2 2V10a2 2 0 0 0 0 4v1.5a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2V14a2 2 0 0 0 0-4z" /><path d="M14.5 7v10" strokeDasharray="1.5 2.5" /></>)
export const FolderIcon = mk(<path d="M3.5 7.5a2 2 0 0 1 2-2H10l2 2.5h6.5a2 2 0 0 1 2 2v7.5a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2z" />)

export function BrandMark(props) {
  return (
    <svg viewBox="0 0 32 32" aria-hidden="true" {...props}>
      <rect width="32" height="32" rx="9" fill="currentColor" />
      <path
        d="M8 11.5A2.5 2.5 0 0 1 10.5 9h11a2.5 2.5 0 0 1 2.5 2.5V13a2 2 0 0 0 0 4v3.5a2.5 2.5 0 0 1-2.5 2.5h-11A2.5 2.5 0 0 1 8 20.5V17a2 2 0 0 0 0-4z"
        fill="#fff"
      />
      <path d="M17 10v12" stroke="currentColor" strokeWidth="1.6" strokeDasharray="2 2.2" />
    </svg>
  )
}

/** Gold seal used on certificates. */
export function Seal({ className }) {
  return (
    <svg viewBox="0 0 120 120" className={className} aria-hidden="true">
      <defs>
        <radialGradient id="sealg" cx="35%" cy="30%" r="80%">
          <stop offset="0" stopColor="#ffe08a" />
          <stop offset="0.6" stopColor="#e0a52a" />
          <stop offset="1" stopColor="#b57d12" />
        </radialGradient>
      </defs>
      <path
        d="M60 4l9.5 7.6 12-2.2 5.3 11 12 3.4-.2 12.2 8.7 8.6-5.9 10.7 4.4 11.4-10.3 6.5-2.5 12-12 .6-8.1 9-11-5.2-11 5.2-8.1-9-12-.6-2.5-12L2.4 91.3l4.4-11.4-5.9-10.7 8.7-8.6-.2-12.2 12-3.4 5.3-11 12 2.2z"
        fill="url(#sealg)"
      />
      <circle cx="60" cy="60" r="38" fill="none" stroke="#fff6d6" strokeWidth="1.6" opacity="0.8" />
      <circle cx="60" cy="60" r="33" fill="none" stroke="#8a5c08" strokeWidth="0.8" opacity="0.5" />
      <path d="M44 60.5l11 11 21-23" fill="none" stroke="#fff8e0" strokeWidth="6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

export function CornerOrnament({ className }) {
  return (
    <svg viewBox="0 0 60 60" className={className} aria-hidden="true" fill="none" stroke="currentColor" strokeWidth="1.4">
      <path d="M3 57V14C3 7.4 7.4 3 14 3h43" />
      <path d="M11 57V20c0-5.5 3.5-9 9-9h37" opacity="0.6" />
      <circle cx="14" cy="14" r="3" fill="currentColor" stroke="none" />
    </svg>
  )
}
