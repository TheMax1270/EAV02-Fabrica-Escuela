import { useEffect, useState } from 'react'
import { NavLink } from 'react-router-dom'
import { Bell } from 'lucide-react'
import { getUnreadCount, NOTIFICATIONS_CHANGED_EVENT } from '../api/notification'
import { useAuth } from '../context/useAuth'

const POLL_INTERVAL_MS = 30_000

export default function NotificationBell() {
  const { user } = useAuth()
  const [count, setCount] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    const refresh = () => getUnreadCount(controller.signal).then((value) => {
      if (!controller.signal.aborted) setCount(value)
    }).catch(() => undefined)
    void refresh()
    const timer = window.setInterval(() => {
      if (document.visibilityState === 'visible') void refresh()
    }, POLL_INTERVAL_MS)
    window.addEventListener(NOTIFICATIONS_CHANGED_EVENT, refresh)
    return () => {
      window.clearInterval(timer)
      window.removeEventListener(NOTIFICATIONS_CHANGED_EVENT, refresh)
      controller.abort()
    }
  }, [user?.id])

  return (
    <NavLink to="/notifications" className="notification-link"
      aria-label={count > 0 ? `Notificaciones, ${count} sin leer` : 'Notificaciones'}>
      <Bell size={18} strokeWidth={1.8} aria-hidden="true" />
      {count > 0 && <span className="notification-badge">{count > 99 ? '99+' : count}</span>}
    </NavLink>
  )
}