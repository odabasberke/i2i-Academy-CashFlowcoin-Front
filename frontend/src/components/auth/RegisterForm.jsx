import { useState } from 'react'
import Spinner from '../common/Spinner'

export default function RegisterForm({ onSubmit, isSubmitting }) {
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  function handleSubmit(event) {
    event.preventDefault()
    onSubmit({ username, email, password })
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label htmlFor="reg-username" className="mb-1 block text-xs text-text-muted">
          Username
        </label>
        <input
          id="reg-username"
          type="text"
          autoComplete="username"
          required
          minLength={3}
          maxLength={50}
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          className="w-full rounded-md border border-hairline bg-ink px-3 py-2 text-sm outline-none focus:border-signal"
        />
      </div>

      <div>
        <label htmlFor="reg-email" className="mb-1 block text-xs text-text-muted">
          Email
        </label>
        <input
          id="reg-email"
          type="email"
          autoComplete="email"
          required
          maxLength={255}
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          className="w-full rounded-md border border-hairline bg-ink px-3 py-2 text-sm outline-none focus:border-signal"
        />
      </div>

      <div>
        <label htmlFor="reg-password" className="mb-1 block text-xs text-text-muted">
          Password
        </label>
        <input
          id="reg-password"
          type="password"
          autoComplete="new-password"
          required
          minLength={8}
          maxLength={100}
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          className="w-full rounded-md border border-hairline bg-ink px-3 py-2 text-sm outline-none focus:border-signal"
        />
        <p className="mt-1 text-xs text-text-muted">At least 8 characters.</p>
      </div>

      <button
        type="submit"
        disabled={isSubmitting}
        className="flex w-full items-center justify-center gap-2 rounded-md bg-signal py-2.5 text-sm font-semibold text-ink transition hover:bg-signal/90 disabled:cursor-not-allowed disabled:opacity-50"
      >
        {isSubmitting && <Spinner className="h-4 w-4" />}
        Create account
      </button>
    </form>
  )
}
