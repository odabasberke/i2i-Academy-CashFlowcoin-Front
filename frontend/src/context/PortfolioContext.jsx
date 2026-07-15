import { createContext, useContext, useCallback, useEffect, useState } from 'react'
import { getPortfolio } from '../api/portfolioApi'
import { useAuth } from './AuthContext'

const PortfolioContext = createContext(null)

export function PortfolioProvider({ children }) {
  const { isAuthenticated } = useAuth()
  const [portfolio, setPortfolio] = useState(null)
  const [isLoading, setIsLoading] = useState(true)

  const refetch = useCallback(async () => {
    try {
      const data = await getPortfolio()
      setPortfolio(data)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    if (isAuthenticated) {
      refetch()
    }
  }, [isAuthenticated, refetch])

  const walletFor = useCallback(
    (currencyCode) => portfolio?.wallets.find((w) => w.currencyCode === currencyCode) ?? null,
    [portfolio]
  )

  const value = {
    wallets: portfolio?.wallets ?? [],
    transactions: portfolio?.recentTransactions ?? [],
    isLoading,
    refetch,
    walletFor,
  }

  return <PortfolioContext.Provider value={value}>{children}</PortfolioContext.Provider>
}

export function usePortfolio() {
  const ctx = useContext(PortfolioContext)
  if (!ctx) {
    throw new Error('usePortfolio must be used within PortfolioProvider')
  }
  return ctx
}
