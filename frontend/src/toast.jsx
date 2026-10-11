import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react'
import { CheckIcon, XIcon } from './icons'

const ToastContext = createContext(null)

export function ToastProvider({ children }) {
  const [items, setItems] = useState([])
  const id = useRef(0)

  const push = useCallback((kind, message) => {
    const key = ++id.current
    setItems((list) => [...list, { key, kind, message }])
    setTimeout(() => setItems((list) => list.filter((t) => t.key !== key)), kind === 'error' ? 5200 : 3200)
  }, [])

  const api = useMemo(
    () => ({ success: (m) => push('success', m), error: (m) => push('error', m) }),
    [push],
  )

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="toasts" role="status" aria-live="polite">
        {items.map((t) => (
          <div key={t.key} className={`toast ${t.kind}`}>
            <span className="dot">{t.kind === 'error' ? <XIcon width={13} height={13} /> : <CheckIcon width={13} height={13} />}</span>
            <span>{t.message}</span>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export const useToast = () => useContext(ToastContext)
