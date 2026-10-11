import { useEffect, useId, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { CheckIcon } from '../icons'

const Chevron = ({ open }) => (
  <svg className="dd-chevron" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
    strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"
    style={{ transform: open ? 'rotate(180deg)' : 'none' }}>
    <path d="m6 9 6 6 6-6" />
  </svg>
)

/**
 * Custom dropdown that replaces the native <select>.
 * options: [{ value, label, hint?, hue? }]  (hue draws a small coloured dot)
 * Keyboard: Arrow keys, Home/End, Enter/Space, Escape, and type-to-jump.
 */
export default function Dropdown({
  value, onChange, options, placeholder = 'Select', ariaLabel, searchable = false,
  disabled = false, className = '', style, menuMinWidth = 220,
}) {
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(0)
  const [query, setQuery] = useState('')
  const [pos, setPos] = useState(null)
  const btnRef = useRef(null)
  const menuRef = useRef(null)
  const searchRef = useRef(null)
  const typed = useRef({ text: '', t: 0 })
  const uid = useId()

  const shown = searchable && query
    ? options.filter((o) => o.label.toLowerCase().includes(query.toLowerCase()))
    : options
  const selected = options.find((o) => o.value === value)

  function openMenu() {
    if (disabled) return
    const r = btnRef.current.getBoundingClientRect()
    const below = window.innerHeight - r.bottom - 12
    const above = r.top - 12
    const up = below < 240 && above > below
    const maxHeight = Math.max(160, Math.min(340, up ? above : below))
    const width = Math.max(r.width, menuMinWidth)
    const left = Math.min(Math.max(8, r.left), window.innerWidth - width - 8)
    setPos({ left, width, maxHeight, up, top: up ? undefined : r.bottom + 6, bottom: up ? window.innerHeight - r.top + 6 : undefined })
    const i = options.findIndex((o) => o.value === value)
    setActive(Math.max(0, i))
    setQuery('')
    setOpen(true)
  }

  function close(focusBtn = true) {
    setOpen(false)
    if (focusBtn) btnRef.current?.focus()
  }

  function choose(opt) {
    if (!opt) return
    onChange(opt.value)
    close()
  }

  // Close on outside click, scroll elsewhere, or resize.
  useEffect(() => {
    if (!open) return
    const down = (e) => {
      if (!menuRef.current?.contains(e.target) && !btnRef.current?.contains(e.target)) close(false)
    }
    const scroll = (e) => { if (!menuRef.current?.contains(e.target)) close(false) }
    document.addEventListener('mousedown', down)
    window.addEventListener('scroll', scroll, true)
    window.addEventListener('resize', close)
    return () => {
      document.removeEventListener('mousedown', down)
      window.removeEventListener('scroll', scroll, true)
      window.removeEventListener('resize', close)
    }
  }, [open]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (open && searchable) searchRef.current?.focus()
  }, [open, searchable])

  // Keep the highlighted option visible.
  useEffect(() => {
    if (!open) return
    menuRef.current?.querySelector(`[data-i="${active}"]`)?.scrollIntoView({ block: 'nearest' })
  }, [active, open, shown.length])

  function onKeyDown(e) {
    if (!open) {
      if (['ArrowDown', 'ArrowUp', 'Enter', ' '].includes(e.key)) { e.preventDefault(); openMenu() }
      return
    }
    switch (e.key) {
      case 'ArrowDown': e.preventDefault(); setActive((a) => Math.min(shown.length - 1, a + 1)); break
      case 'ArrowUp': e.preventDefault(); setActive((a) => Math.max(0, a - 1)); break
      case 'Home': e.preventDefault(); setActive(0); break
      case 'End': e.preventDefault(); setActive(shown.length - 1); break
      case 'Enter': e.preventDefault(); choose(shown[active]); break
      case ' ': if (!searchable) { e.preventDefault(); choose(shown[active]) } break
      case 'Escape': e.preventDefault(); close(); break
      case 'Tab': close(false); break
      default:
        // Type-to-jump for non-searchable menus.
        if (!searchable && e.key.length === 1 && !e.ctrlKey && !e.metaKey) {
          const now = Date.now()
          typed.current.text = now - typed.current.t > 600 ? e.key : typed.current.text + e.key
          typed.current.t = now
          const i = shown.findIndex((o) => o.label.toLowerCase().startsWith(typed.current.text.toLowerCase()))
          if (i >= 0) setActive(i)
        }
    }
  }

  return (
    <>
      <button
        ref={btnRef} type="button" className={`dd ${open ? 'is-open' : ''} ${className}`} style={style}
        disabled={disabled} aria-haspopup="listbox" aria-expanded={open} aria-controls={`${uid}-list`}
        aria-label={ariaLabel} onClick={() => (open ? close() : openMenu())} onKeyDown={onKeyDown}
      >
        <span className={`dd-value ${selected ? '' : 'is-placeholder'}`}>
          {selected?.hue != null && <i className="dd-dot" style={{ '--h': selected.hue }} />}
          <span className="dd-text">{selected ? selected.label : placeholder}</span>
        </span>
        <Chevron open={open} />
      </button>

      {open && pos && createPortal(
        <div
          ref={menuRef} className={`dd-menu ${pos.up ? 'is-up' : ''}`}
          style={{ left: pos.left, width: pos.width, maxHeight: pos.maxHeight, top: pos.top, bottom: pos.bottom }}
        >
          {searchable && (
            <input ref={searchRef} className="dd-search" placeholder="Search" value={query}
              onChange={(e) => { setQuery(e.target.value); setActive(0) }} onKeyDown={onKeyDown}
              aria-label="Search options" />
          )}
          <ul id={`${uid}-list`} role="listbox" aria-label={ariaLabel} className="dd-list">
            {shown.length === 0 && <li className="dd-empty">Nothing matches</li>}
            {shown.map((o, i) => (
              <li
                key={String(o.value)} role="option" data-i={i} aria-selected={o.value === value}
                className={`dd-opt ${i === active ? 'is-active' : ''} ${o.value === value ? 'is-selected' : ''}`}
                onMouseEnter={() => setActive(i)} onMouseDown={(e) => e.preventDefault()} onClick={() => choose(o)}
              >
                {o.hue != null && <i className="dd-dot" style={{ '--h': o.hue }} />}
                <span className="dd-opt-main">
                  <span className="dd-opt-label">{o.label}</span>
                  {o.hint && <span className="dd-opt-hint">{o.hint}</span>}
                </span>
                {o.value === value && <CheckIcon width={16} height={16} />}
              </li>
            ))}
          </ul>
        </div>,
        document.body,
      )}
    </>
  )
}
