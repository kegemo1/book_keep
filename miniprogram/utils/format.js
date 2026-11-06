/**
 * 秒 → 时:分:秒
 */
function formatDuration(seconds) {
  const s = Math.max(0, Number(seconds || 0))
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  const pad = n => String(n).padStart(2, '0')
  return `${pad(h)}:${pad(m)}:${pad(sec)}`
}

/**
 * 秒 → 小时（保留 2 位）
 */
function toHours(seconds) {
  return (Math.max(0, Number(seconds || 0)) / 3600).toFixed(2)
}

/**
 * 金额格式化，千分位
 */
function money(value) {
  const num = Number(value || 0)
  return num.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

/**
 * 本地时区的今天，返回 yyyy-MM-dd
 * 注意：不能用 toISOString()，UTC 换算会导致跨天
 */
function todayStr(date) {
  const d = date || new Date()
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/**
 * 日期偏移（天）
 */
function offsetDays(date, days) {
  const d = new Date(date.getTime())
  d.setDate(d.getDate() + days)
  return todayStr(d)
}

/**
 * 是否在最近 N 天内
 */
function withinDays(dateStr, days) {
  const from = offsetDays(new Date(), -days)
  return dateStr >= from && dateStr <= todayStr()
}

/**
 * 截断长文本
 */
function ellipsis(text, len) {
  const t = text || ''
  return t.length > len ? t.slice(0, len) + '…' : t
}

module.exports = { formatDuration, toHours, money, todayStr, offsetDays, withinDays, ellipsis }
