import { createContext, useContext, useCallback, useState, useEffect, useRef } from 'react'
import { eventBus } from '../lib/eventBus'
import ToastViewport from '../components/common/ToastViewport'

const ToastContext = createContext(null)
const AUTO_DISMISS_MS = 5000

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])
  const nextId = useRef(0)

  const dismiss = useCallback((id) => {
    setToasts((current) => current.filter((toast) => toast.id !== id))
  }, [])

  const push = useCallback(
    (toast) => {
      const id = ++nextId.current
      setToasts((current) => [...current, { id, type: 'info', ...toast }])
      setTimeout(() => dismiss(id), AUTO_DISMISS_MS)
    },
    [dismiss]
  )

  useEffect(() => {
    // The axios interceptor (client.js) emits 'toast' on every 400/401/503
    // it sees - this is the one place that turns those into visible UI.
    return eventBus.on('toast', push)
  }, [push])

  return (
    <ToastContext.Provider value={{ push, dismiss }}>
      {children}
      <ToastViewport toasts={toasts} onDismiss={dismiss} />
    </ToastContext.Provider>
  )
}

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) {
    throw new Error('useToast must be used within ToastProvider')
  }
  return ctx
}
