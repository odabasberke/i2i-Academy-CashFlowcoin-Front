const TYPE_STYLES = {
  error: 'border-sell/40 bg-sell/10 text-sell',
  success: 'border-buy/40 bg-buy/10 text-buy',
  info: 'border-signal/40 bg-signal/10 text-signal',
}

export default function ToastViewport({ toasts, onDismiss }) {
  if (toasts.length === 0) {
    return null
  }

  return (
    <div
      className="fixed top-4 right-4 z-50 flex w-full max-w-sm flex-col gap-2"
      role="region"
      aria-label="Notifications"
    >
      {toasts.map((toast) => (
        <div
          key={toast.id}
          role="alert"
          className={`flex items-start justify-between gap-3 rounded-lg border px-4 py-3 shadow-lg backdrop-blur-sm ${TYPE_STYLES[toast.type] ?? TYPE_STYLES.info}`}
        >
          <p className="text-sm leading-snug">{toast.message}</p>
          <button
            type="button"
            onClick={() => onDismiss(toast.id)}
            aria-label="Dismiss notification"
            className="shrink-0 text-current/70 hover:text-current"
          >
            ✕
          </button>
        </div>
      ))}
    </div>
  )
}
