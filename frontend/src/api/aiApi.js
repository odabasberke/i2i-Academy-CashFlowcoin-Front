import { apiClient } from './client'

/** @returns {Promise<{answer: string, answeredAt: string}>} */
export function askAi(question) {
  return apiClient.post('/ai/query', { question }).then((res) => res.data)
}
