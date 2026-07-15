export default function SkeletonRow({ columns = 3 }) {
  return (
    <tr className="animate-pulse">
      {Array.from({ length: columns }).map((_, i) => (
        <td key={i} className="px-4 py-3">
          <div className="h-4 rounded bg-panel-raised" style={{ width: `${50 + (i % 3) * 15}%` }} />
        </td>
      ))}
    </tr>
  )
}
