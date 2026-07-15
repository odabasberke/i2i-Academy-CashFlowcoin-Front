import { usePortfolio } from '../../context/PortfolioContext'
import { formatUsd, formatQuantity } from '../../lib/format'

const FIAT_CURRENCY = 'USD'

export default function WalletList() {
  const { wallets, isLoading } = usePortfolio()

  return (
    <section className="rounded-xl border border-hairline bg-panel">
      <div className="border-b border-hairline px-4 py-3">
        <h2 className="font-display text-sm font-semibold uppercase tracking-wide text-text-muted">
          Portfolio
        </h2>
      </div>

      <ul className="divide-y divide-hairline">
        {isLoading && wallets.length === 0 && (
          <li className="animate-pulse px-4 py-3">
            <div className="h-4 w-2/3 rounded bg-panel-raised" />
          </li>
        )}

        {!isLoading && wallets.length === 0 && (
          <li className="px-4 py-6 text-center text-sm text-text-muted">No wallets yet.</li>
        )}

        {wallets.map((wallet) => (
          <li key={wallet.currencyCode} className="flex items-center justify-between px-4 py-3">
            <span className="font-medium">{wallet.currencyCode}</span>
            <span className="font-data tabular-nums">
              {wallet.currencyCode === FIAT_CURRENCY
                ? formatUsd(wallet.balance)
                : formatQuantity(wallet.balance)}
            </span>
          </li>
        ))}
      </ul>
    </section>
  )
}
