import { apiClient } from './client'

/** @returns {Promise<Record<string, {symbol, price, timestamp}>>} */
export function getLatestPrices() {
  return apiClient.get('/market/prices').then((res) => res.data)
}
