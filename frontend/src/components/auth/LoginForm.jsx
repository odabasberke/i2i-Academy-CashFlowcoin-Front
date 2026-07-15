import { useState } from 'react'
import Spinner from '../common/Spinner'

export default function LoginForm({ onSubmit, isSubmitting }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')

  function handleSubmit(event) {
    event.preventDefault()
    onSubmit({ username, password })
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label htmlFor="username" className="mb-1 block text-xs text-text-muted">
          Username
        </label>
        <input
          id="username"
          type="text"
          autoComplete="username"
          required
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          className="w-full rounded-md border border-hairline bg-ink px-3 py-2 text-sm outline-none focus:border-signal"
        />
      </div>

      <div>
        <label htmlFor="password" className="mb-1 block text-xs text-text-muted">
          Password
        </label>
        <input
          id="password"
          type="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          className="w-full rounded-md border border-hairline bg-ink px-3 py-2 text-sm outline-none focus:border-signal"
        />
      </div>

      <button
        type="submit"
        disabled={isSubmitting}
        className="flex w-full items-center justify-center gap-2 rounded-md bg-signal py-2.5 text-sm font-semibold text-ink transition hover:bg-signal/90 disabled:cursor-not-allowed disabled:opacity-50"
      >
        {isSubmitting && <Spinner className="h-4 w-4" />}
        Log in
      </button>
    </form>
  )
}
