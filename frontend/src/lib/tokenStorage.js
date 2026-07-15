const TOKEN_KEY = 'cryptopal.accessToken'

/**
 * Centralizes localStorage access for the token so nothing else in the
 * app touches the storage key directly. localStorage (not an httpOnly
 * cookie) matches how the backend was built - a stateless bearer token in
 * the Authorization header, no cookie-based session, which is also why
 * the backend disables CSRF. The trade-off is that a token here is
 * readable by any script on the page (XSS risk) - acceptable for this
 * project, but worth knowing if this ever needs to harden further.
 */
export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}
