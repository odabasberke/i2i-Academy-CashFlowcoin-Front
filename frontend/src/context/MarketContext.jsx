import { createContext, useContext } from 'react'
import { usePolling } from '../hooks/usePolling'
import { getLatestPrices } from '../api/marketApi'

const MarketContext = createContext(null)
const POLL_INTERVAL_MS = 5000

/**
 * One poll of /api/market/prices, shared by MarketTicker, MarketTable, and
 * TradeModal - not three independent ones. Keeps every price shown on
 * screen at any moment perfectly in sync with every other, and halves
 * (really, thirds) the request volume for no cost.
 */
export function MarketProvider({ children }) {
  const { data, isLoading, error, refetch } = usePolling(getLatestPrices, {
    intervalMs: POLL_INTERVAL_MS,
  })

  const value = { prices: data ?? {}, isLoading, error, refetch }

  return <MarketContext.Provider value={value}>{children}</MarketContext.Provider>
}

export function useMarket() {
  const ctx = useContext(MarketContext)
  if (!ctx) {
    throw new Error('useMarket must be used within MarketProvider')
  }
  return ctx
}
