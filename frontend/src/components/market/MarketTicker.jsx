import { useMarket } from '../../context/MarketContext'
import { formatUsd } from '../../lib/format'

export default function MarketTicker() {
  const { prices } = useMarket()
  const symbols = Object.keys(prices).sort()

  if (symbols.length === 0) {
    return <div className="h-9 border-b border-hairline bg-panel" />
  }

  // Duplicated so the CSS translateX(-50%) loop has no visible seam.
  const items = [...symbols, ...symbols]

  return (
    <div className="overflow-hidden border-b border-hairline bg-panel">
      <div className="flex w-max animate-ticker gap-8 py-2">
        {items.map((symbol, i) => (
          <span
            key={`${symbol}-${i}`}
            className="flex items-center gap-2 whitespace-nowrap px-2 font-data text-xs"
          >
            <span className="text-text-muted">{symbol}</span>
            <span className="tabular-nums text-signal">{formatUsd(prices[symbol].price)}</span>
          </span>
        ))}
      </div>
    </div>
  )
}
