import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <header className="border-b border-hairline bg-panel/80 backdrop-blur-sm">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-3 sm:px-6">
        <div className="flex items-center gap-2">
          <span className="inline-block h-2 w-2 rounded-full bg-signal shadow-[0_0_8px_2px_rgba(94,234,212,0.6)]" />
          <span className="font-display text-lg font-semibold tracking-tight">CryptoPal</span>
          <span className="hidden font-data text-xs text-text-muted sm:inline">SIMULATED TERMINAL</span>
        </div>

        {user && (
          <div className="flex items-center gap-4">
            <span className="font-data text-sm text-text-muted">
              {user.username} <span className="text-signal-dim">· {user.role}</span>
            </span>
            <button
              type="button"
              onClick={handleLogout}
              className="rounded-md border border-hairline px-3 py-1.5 text-sm text-text-muted transition hover:border-sell/50 hover:text-sell"
            >
              Log out
            </button>
          </div>
        )}
      </div>
    </header>
  )
}
