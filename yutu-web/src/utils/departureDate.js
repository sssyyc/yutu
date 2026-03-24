const DAY_MS = 24 * 60 * 60 * 1000

function toLocalMidnight(date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

export function parseLocalDate(value) {
  if (!value) return null
  if (value instanceof Date && !Number.isNaN(value.getTime())) {
    return toLocalMidnight(value)
  }

  const text = String(value).trim()
  const match = text.match(/^(\d{4})-(\d{1,2})-(\d{1,2})$/)
  if (match) {
    return new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]))
  }

  const parsed = new Date(text)
  if (Number.isNaN(parsed.getTime())) {
    return null
  }
  return toLocalMidnight(parsed)
}

export function isUpcomingDeparture(value, baseDate = new Date()) {
  const target = parseLocalDate(value)
  if (!target) return false
  return target.getTime() >= toLocalMidnight(baseDate).getTime()
}

export function filterUpcomingDepartures(rows, baseDate = new Date()) {
  return (Array.isArray(rows) ? rows : []).filter((item) => isUpcomingDeparture(item?.departDate, baseDate))
}

export function getDaysUntilDeparture(value, baseDate = new Date()) {
  const target = parseLocalDate(value)
  if (!target) return null
  const diff = target.getTime() - toLocalMidnight(baseDate).getTime()
  return Math.round(diff / DAY_MS)
}
