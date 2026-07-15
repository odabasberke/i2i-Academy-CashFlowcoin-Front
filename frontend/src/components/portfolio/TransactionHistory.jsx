import { usePortfolio } from '../../context/PortfolioContext'
import { formatQuantity, formatUsd, formatDateTime } from '../../lib/format'

export default function TransactionHistory() {
  const { transactions, isLoading } = usePortfolio()

  return (
    <section className="rounded-xl border border-hairline bg-panel">
      <div className="border-b border-hairline px-4 py-3">
        <h2 className="font-display text-sm font-semibold uppercase tracking-wide text-text-muted">
          Recent activity
        </h2>
      </div>

      <ul className="max-h-80 divide-y divide-hairline overflow-y-auto">
        {isLoading && transactions.length === 0 && (
          <li className="animate-pulse px-4 py-3">
            <div className="h-4 w-3/4 rounded bg-panel-raised" />
          </li>
        )}

        {!isLoading && transactions.length === 0 && (
          <li className="px-4 py-6 text-center text-sm text-text-muted">
            No trades yet — buy something from the market list to get started.
          </li>
        )}

        {transactions.map((tx) => (
          <li key={tx.transactionId} className="px-4 py-3 text-sm">
            <div className="flex items-center justify-between">
              <span className={tx.type === 'BUY' ? 'font-medium text-buy' : 'font-medium text-sell'}>
                {tx.type} {tx.currencyPair}
              </span>
              <span className="font-data tabular-nums">{formatQuantity(tx.amount)}</span>
            </div>
            <div className="mt-0.5 flex items-center justify-between text-xs text-text-muted">
              <span>{formatDateTime(tx.executedAt)}</span>
              <span className="font-data tabular-nums">@ {formatUsd(tx.price)}</span>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}
