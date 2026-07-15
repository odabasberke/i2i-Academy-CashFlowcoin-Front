import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import LoginForm from '../components/auth/LoginForm'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(credentials) {
    setIsSubmitting(true)
    try {
      await login(credentials)
      navigate(location.state?.from ?? '/', { replace: true })
    } catch {
      // The axios interceptor already toasted "Invalid username or password".
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <span className="mx-auto mb-2 inline-block h-2 w-2 rounded-full bg-signal shadow-[0_0_8px_2px_rgba(94,234,212,0.6)]" />
          <h1 className="font-display text-2xl font-semibold">CryptoPal</h1>
          <p className="mt-1 text-sm text-text-muted">Log in to your simulated portfolio</p>
        </div>

        <div className="rounded-xl border border-hairline bg-panel p-6">
          <LoginForm onSubmit={handleSubmit} isSubmitting={isSubmitting} />
        </div>

        <p className="mt-4 text-center text-sm text-text-muted">
          No account yet?{' '}
          <Link to="/register" className="text-signal hover:underline">
            Create one
          </Link>
        </p>
      </div>
    </div>
  )
}
