function parseDateTime(value) {
  if (!value) return null
  if (value instanceof Date && !Number.isNaN(value.getTime())) {
    return value
  }

  const text = String(value).trim()
  if (!text) return null

  const normalized = text.includes("T") ? text : text.replace(" ", "T")
  const parsed = new Date(normalized)
  if (!Number.isNaN(parsed.getTime())) {
    return parsed
  }

  const fallback = new Date(text.replace(/-/g, "/"))
  if (!Number.isNaN(fallback.getTime())) {
    return fallback
  }
  return null
}

export function getPaymentDeadlineDate(order) {
  return parseDateTime(order?.paymentExpireTime)
}

export function getPaymentDeadlineMs(order) {
  const date = getPaymentDeadlineDate(order)
  return date ? date.getTime() : null
}

export function isPaymentCountdownActive(order) {
  return order?.orderStatus === "PENDING_PAY" && order?.payStatus === "UNPAID"
}

export function getPaymentRemainingSeconds(order, nowMs = Date.now()) {
  if (!isPaymentCountdownActive(order)) return 0
  const deadlineMs = getPaymentDeadlineMs(order)
  if (deadlineMs == null) return 0
  return Math.max(Math.ceil((deadlineMs - nowMs) / 1000), 0)
}

export function isPaymentExpired(order, nowMs = Date.now()) {
  if (order?.paymentExpired === true) return true
  if (order?.payStatus !== "UNPAID") return false
  const deadlineMs = getPaymentDeadlineMs(order)
  return deadlineMs != null && deadlineMs <= nowMs
}

export function formatPaymentCountdown(totalSeconds) {
  const seconds = Math.max(Number(totalSeconds) || 0, 0)
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const remainSeconds = seconds % 60

  if (hours > 0) {
    return `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(remainSeconds).padStart(2, "0")}`
  }
  return `${String(minutes).padStart(2, "0")}:${String(remainSeconds).padStart(2, "0")}`
}

export function formatPaymentDeadlineTime(order) {
  const date = getPaymentDeadlineDate(order)
  if (!date) return "-"
  return date.toLocaleString("zh-CN", { hour12: false })
}
