import { useEffect, useRef, useState } from 'react'
import { formatUsd } from '../../lib/format'

/**
 * Renders one price value and briefly flashes green/up or rose/down when
 * it changes - direction only affects a background-color animation, never
 * size or position, so polling updates can never shift the layout.
 */
export default function PriceCell({ price }) {
  const previousPrice = useRef(price)
  const [flash, setFlash] = useState(null)

  useEffect(() => {
    if (previousPrice.current !== null && price !== previousPrice.current) {
      setFlash(price > previousPrice.current ? 'up' : 'down')
      previousPrice.current = price
      const timeout = setTimeout(() => setFlash(null), 900)
      return () => clearTimeout(timeout)
    }
    previousPrice.current = price
    return undefined
  }, [price])

  const flashClass = flash === 'up' ? 'price-flash-up' : flash === 'down' ? 'price-flash-down' : ''

  return (
    <span className={`inline-block rounded px-2 py-0.5 font-data tabular-nums ${flashClass}`}>
      {formatUsd(price)}
    </span>
  )
}
