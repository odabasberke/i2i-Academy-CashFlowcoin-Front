import { useMarket } from '../../context/MarketContext'
import PriceCell from './PriceCell'
import SkeletonRow from '../common/SkeletonRow'

export default function MarketTable({ onSelectAsset }) {
  const { prices, isLoading, error, refetch } = useMarket()
  const symbols = Object.keys(prices).sort()

  return (
    <section className="rounded-xl border border-hairline bg-panel">
      <div className="flex items-center justify-between border-b border-hairline px-4 py-3">
        <h2 className="font-display text-sm font-semibold uppercase tracking-wide text-text-muted">
          Market
        </h2>
        <button
          type="button"
          onClick={refetch}
          className="rounded-md border border-hairline px-2.5 py-1 text-xs text-text-muted transition hover:border-signal/50 hover:text-signal"
        >
          Refresh
        </button>
      </div>

      <table className="w-full text-left text-sm">
        <thead>
          <tr className="text-xs uppercase tracking-wide text-text-muted">
            <th className="px-4 py-2 font-normal">Asset</th>
            <th className="px-4 py-2 font-normal">Price</th>
            <th className="px-4 py-2 font-normal text-right">Trade</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-hairline">
          {isLoading &&
            symbols.length === 0 &&
            Array.from({ length: 2 }).map((_, i) => <SkeletonRow key={i} columns={3} />)}

          {symbols.length === 0 && !isLoading && (
            <tr>
              <td colSpan={3} className="px-4 py-6 text-center text-sm text-text-muted">
                No live prices yet. The market feed updates every few seconds.
              </td>
            </tr>
          )}

          {/* key={symbol}, not index - keeps React matching each row to the
              same DOM node across polls, which is what actually prevents
              rows from remounting (and therefore flickering) on refresh. */}
          {symbols.map((symbol) => (
            <tr key={symbol} className="transition-colors hover:bg-panel-raised/60">
              <td className="px-4 py-3 font-medium">{symbol}</td>
              <td className="px-4 py-3">
                <PriceCell price={prices[symbol].price} />
              </td>
              <td className="px-4 py-3 text-right">
                <button
                  type="button"
                  onClick={() => onSelectAsset(symbol)}
                  className="rounded-md bg-signal/10 px-3 py-1 text-xs font-medium text-signal transition hover:bg-signal/20"
                >
                  Trade
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {error && (
        <p className="border-t border-hairline px-4 py-2 text-xs text-sell">
          Couldn't refresh prices — showing the last known values.
        </p>
      )}
    </section>
  )
}
