import { apiClient } from './client'

/** @returns {Promise<{publicId, username, email, role}>} */
export function register({ username, email, password }) {
  return apiClient.post('/auth/register', { username, email, password }).then((res) => res.data)
}

/** @returns {Promise<{accessToken, tokenType, expiresInSeconds, userPublicId, username, role}>} */
export function login({ username, password }) {
  return apiClient.post('/auth/login', { username, password }).then((res) => res.data)
}
