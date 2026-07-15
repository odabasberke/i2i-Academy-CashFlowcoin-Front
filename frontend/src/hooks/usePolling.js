import { useEffect, useRef, useState, useCallback } from 'react'

/**
 * Polls an async fetcher on an interval and exposes the latest result.
 * Only the very first call sets isLoading - every refetch after that
 * updates `data` in place without ever going back to a loading state, so
 * periodic updates can't blank or flicker whatever is rendering it.
 */
export function usePolling(fetcher, { intervalMs = 5000, enabled = true } = {}) {
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const fetcherRef = useRef(fetcher)
  fetcherRef.current = fetcher

  const runFetch = useCallback(async () => {
    try {
      const result = await fetcherRef.current()
      setData(result)
      setError(null)
    } catch (err) {
      setError(err)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    if (!enabled) {
      return undefined
    }
    runFetch()
    const id = setInterval(runFetch, intervalMs)
    return () => clearInterval(id)
  }, [enabled, intervalMs, runFetch])

  return { data, error, isLoading, refetch: runFetch }
}
