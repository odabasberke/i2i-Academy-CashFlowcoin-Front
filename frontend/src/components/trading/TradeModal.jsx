import { useState } from 'react'
import { placeOrder } from '../../api/tradingApi'
import { useMarket } from '../../context/MarketContext'
import { usePortfolio } from '../../context/PortfolioContext'
import { useToast } from '../../context/ToastContext'
import { formatUsd, formatQuantity } from '../../lib/format'
import Spinner from '../common/Spinner'

const FIAT_CURRENCY = 'USD'

export default function TradeModal({ symbol, onClose }) {
  const { prices } = useMarket()
  const { walletFor, refetch: refetchPortfolio } = usePortfolio()
  const { push: pushToast } = useToast()

  const [side, setSide] = useState('BUY')
  const [quantity, setQuantity] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const price = prices[symbol]?.price ?? 0
  const ownedQuantity = walletFor(symbol)?.balance ?? 0
  const availableFiat = walletFor(FIAT_CURRENCY)?.balance ?? 0

  const parsedQuantity = Number(quantity) || 0
  const estimatedTotal = parsedQuantity * price

  const canSell = ownedQuantity > 0
  const insufficientFunds = side === 'BUY' && estimatedTotal > availableFiat
  const insufficientAsset = side === 'SELL' && parsedQuantity > ownedQuantity
  const canSubmit = parsedQuantity > 0 && !insufficientFunds && !insufficientAsset && !isSubmitting

  async function handleSubmit(event) {
    event.preventDefault()
    if (!canSubmit) {
      return
    }

    setIsSubmitting(true)
    try {
      const result = await placeOrder({ type: side, symbol, quantity: parsedQuantity })
      pushToast({
        type: 'success',
        message: `${side === 'BUY' ? 'Bought' : 'Sold'} ${formatQuantity(result.quantity)} ${symbol} at ${formatUsd(result.executionPrice)}.`,
      })
      await refetchPortfolio()
      onClose()
    } catch {
      // The axios interceptor already surfaced a toast with the backend's
      // specific message (e.g. "Insufficient funds: ...") - nothing more to do.
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div
      className="fixed inset-0 z-40 flex items-center justify-center bg-black/60 px-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="trade-modal-title"
      onClick={onClose}
    >
      <div
        className="w-full max-w-sm rounded-xl border border-hairline bg-panel p-5 shadow-2xl"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 id="trade-modal-title" className="font-display text-lg font-semibold">
            Trade {symbol}
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close"
            className="text-text-muted hover:text-text"
          >
            ✕
          </button>
        </div>

        <div className="mb-4 grid grid-cols-2 gap-2">
          <button
            type="button"
            onClick={() => setSide('BUY')}
            className={`rounded-md py-2 text-sm font-medium transition ${
              side === 'BUY' ? 'bg-buy/15 text-buy ring-1 ring-buy/40' : 'bg-panel-raised text-text-muted'
            }`}
          >
            Buy
          </button>
          <button
            type="button"
            onClick={() => setSide('SELL')}
            disabled={!canSell}
            title={canSell ? undefined : `You don't own any ${symbol} yet`}
            className={`rounded-md py-2 text-sm font-medium transition disabled:cursor-not-allowed disabled:opacity-40 ${
              side === 'SELL' ? 'bg-sell/15 text-sell ring-1 ring-sell/40' : 'bg-panel-raised text-text-muted'
            }`}
          >
            Sell
          </button>
        </div>

        <p className="mb-3 font-data text-xs text-text-muted">
          {side === 'BUY'
            ? `Available: ${formatUsd(availableFiat)}`
            : `You own: ${formatQuantity(ownedQuantity)} ${symbol}`}
        </p>

        <form onSubmit={handleSubmit}>
          <label htmlFor="quantity" className="mb-1 block text-xs text-text-muted">
            Quantity ({symbol})
          </label>
          <input
            id="quantity"
            type="number"
            step="any"
            min="0"
            inputMode="decimal"
            autoFocus
            value={quantity}
            onChange={(event) => setQuantity(event.target.value)}
            placeholder="0.00"
            className="w-full rounded-md border border-hairline bg-ink px-3 py-2 font-data tabular-nums outline-none focus:border-signal"
          />

          <div className="mt-3 flex items-center justify-between font-data text-sm">
            <span className="text-text-muted">Est. {side === 'BUY' ? 'cost' : 'proceeds'}</span>
            <span>{formatUsd(estimatedTotal)}</span>
          </div>

          {(insufficientFunds || insufficientAsset) && (
            <p className="mt-2 text-xs text-sell">
              {insufficientFunds
                ? 'Not enough available balance for this order.'
                : `You only own ${formatQuantity(ownedQuantity)} ${symbol}.`}
            </p>
          )}

          <button
            type="submit"
            disabled={!canSubmit}
            className={`mt-4 flex w-full items-center justify-center gap-2 rounded-md py-2.5 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-40 ${
              side === 'BUY' ? 'bg-buy text-ink hover:bg-buy/90' : 'bg-sell text-ink hover:bg-sell/90'
            }`}
          >
            {isSubmitting && <Spinner className="h-4 w-4" />}
            {side === 'BUY' ? 'Buy' : 'Sell'} {symbol}
          </button>
        </form>
      </div>
    </div>
  )
}
