const { get, post, put } = require('../utils/request')

/**
 * 收支记账服务
 * 说明：金额恒为正数，收入/支出由 recordType 决定（0收入 1支出）
 */

/** 类别下拉：categoryType 为 '0'(收入) 或 '1'(支出) */
function categories(categoryType) {
  return get('/wx/finance/categories', { categoryType }).then(res => res.data || [])
}

/** 按类别统计金额（环形图用） */
function statByCategory(recordType) {
  return get('/wx/finance/stat/category', { recordType }).then(res => res.data || [])
}

/** 记一笔 */
function add(data) {
  return post('/wx/finance/add', data).then(res => res.data)
}

/** 我的账单 */
function list(query) {
  return get('/wx/finance/list', query).then(res => ({
    rows: (res.data && res.data.rows) || (res.data || []),
    total: (res.data && res.data.total) || res.total || 0
  }))
}

/** 本月收支汇总 */
function summary() {
  return get('/wx/finance/summary').then(res => res.data || {})
}

/** 登记收付款：settledAmount 传本次结算金额 */
function settle(recordId, settledAmount) {
  return put('/wx/finance/settle', { recordId, settledAmount }).then(res => res.data)
}

module.exports = { categories, statByCategory, add, list, summary, settle }
