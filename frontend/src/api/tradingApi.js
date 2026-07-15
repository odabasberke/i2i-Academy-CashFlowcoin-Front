import { apiClient } from './client'

/**
 * @param {{type: 'BUY'|'SELL', symbol: string, quantity: number}} order
 * @returns {Promise<{transactionId, type, symbol, quantity, executionPrice, totalValue, newFiatBalance, newAssetBalance, executedAt}>}
 */
export function placeOrder(order) {
  return apiClient.post('/trading/order', order).then((res) => res.data)
}
