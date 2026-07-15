import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import RegisterForm from '../components/auth/RegisterForm'

export default function RegisterPage() {
  const { register } = useAuth()
  const { push: pushToast } = useToast()
  const navigate = useNavigate()
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(payload) {
    setIsSubmitting(true)
    try {
      await register(payload)
      pushToast({ type: 'success', message: 'Account created — log in to continue.' })
      navigate('/login', { replace: true })
    } catch {
      // The axios interceptor already toasted the specific error
      // (e.g. "Username already taken: ...").
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
          <p className="mt-1 text-sm text-text-muted">
            Create an account — you'll start with a random demo balance.
          </p>
        </div>

        <div className="rounded-xl border border-hairline bg-panel p-6">
          <RegisterForm onSubmit={handleSubmit} isSubmitting={isSubmitting} />
        </div>

        <p className="mt-4 text-center text-sm text-text-muted">
          Already have an account?{' '}
          <Link to="/login" className="text-signal hover:underline">
            Log in
          </Link>
        </p>
      </div>
    </div>
  )
}
