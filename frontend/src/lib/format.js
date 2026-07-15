const usdFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

/** Crypto quantities need more precision than fiat - up to 8 places, trailing zeros trimmed. */
const cryptoFormatter = new Intl.NumberFormat('en-US', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 8,
})

export function formatUsd(value) {
  return usdFormatter.format(Number(value ?? 0))
}

export function formatQuantity(value) {
  return cryptoFormatter.format(Number(value ?? 0))
}

export function formatDateTime(isoString) {
  if (!isoString) return '—'
  return new Date(isoString).toLocaleString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  })
}
