import axios from 'axios'
import { tokenStorage } from '../lib/tokenStorage'
import { eventBus } from '../lib/eventBus'

/**
 * Single axios instance for the whole app. baseURL is relative ('/api')
 * so the same code works against the Vite dev proxy and a same-origin
 * production build alike - see vite.config.js and the Docker/Nginx setup
 * in the root README.
 */
export const apiClient = axios.create({
  // Eski hali: baseURL: '/api',
  // Yeni hali: .env dosyasındaki adresi okuyup sonuna /api ekliyoruz.
  baseURL: import.meta.env.VITE_API_URL + '/api',
  
  // AI queries are the slowest call in the app - GeminiClient's own read
  // timeout on the backend is 12s, so the client needs enough headroom on
  // top of that (plus network) rather than racing it.
  timeout: 20000,
})

apiClient.interceptors.request.use((config) => {
  const token = tokenStorage.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || fallbackMessage(status, error)

    if (status === 401) {
      tokenStorage.clear()
      eventBus.emit('unauthorized')
      eventBus.emit('toast', { type: 'error', message: 'Your session has expired. Please log in again.' })
    } else {
      eventBus.emit('toast', { type: 'error', message })
    }

    return Promise.reject(error)
  }
)

function fallbackMessage(status, error) {
  if (!error.response) {
    return 'Cannot reach the server. Check your connection and try again.'
  }
  if (status === 503) {
    return 'This service is temporarily unavailable. Please try again shortly.'
  }
  return 'Something went wrong. Please try again.'
}
