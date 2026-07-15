import { apiClient } from './client'

/** @returns {Promise<{wallets: Array, recentTransactions: Array}>} */
export function getPortfolio() {
  return apiClient.get('/portfolio').then((res) => res.data)
}
