/**
 * Minimal event bus so the axios interceptor (which runs outside the
 * React tree) can talk to AuthContext and ToastContext without a direct
 * import cycle between the API layer and the context layer. Two events
 * are used: 'toast' (show a notification) and 'unauthorized' (the
 * backend rejected the session - log out).
 */
function createEventBus() {
  const listeners = new Map()

  return {
    on(event, handler) {
      const handlers = listeners.get(event) ?? new Set()
      handlers.add(handler)
      listeners.set(event, handlers)
      return () => handlers.delete(handler)
    },
    emit(event, payload) {
      listeners.get(event)?.forEach((handler) => handler(payload))
    },
  }
}

export const eventBus = createEventBus()
