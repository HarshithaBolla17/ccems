import { useEffect, useRef, useState } from 'react'

/** Seconds left until `until` (ISO string). Calls onExpire once when it reaches zero. */
export function useCountdown(until, onExpire) {
  const calc = () => (until ? Math.max(0, Math.ceil((new Date(until).getTime() - Date.now()) / 1000)) : 0)
  const [left, setLeft] = useState(calc)
  const fired = useRef(false)
  const cb = useRef(onExpire)
  cb.current = onExpire

  useEffect(() => {
    fired.current = false
    setLeft(calc())
    if (!until) return undefined
    const id = setInterval(() => {
      const s = calc()
      setLeft(s)
      if (s === 0 && !fired.current) {
        fired.current = true
        cb.current?.()
      }
    }, 1000)
    return () => clearInterval(id)
  }, [until]) // eslint-disable-line react-hooks/exhaustive-deps

  return left
}

export const clock = (s) => `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`

export default function HoldTimer({ until, onExpire }) {
  const left = useCountdown(until, onExpire)
  return <span className="tabnum">{clock(left)}</span>
}
