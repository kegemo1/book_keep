const { get, post, put } = require('../utils/request')

/**
 * 工时打卡服务
 * 说明：所有时长由服务端根据起止时间计算，前端只负责展示递增，不参与计时结果
 */

/** 当前进行中的计时器（无则返回 null） */
function getRunning() {
  return get('/wx/worklog/running').then(res => {
    const data = res.data
    // 后端无进行中记录时成功响应里没有 data 字段，request 层会补成 {}，
    // 这里按 logId 判断，必须还原成 null —— 否则界面会渲染成「进行中」的空卡片
    return (data && data.logId) ? data : null
  })
}

/** 开始计时 */
function start(data) {
  return post('/wx/worklog/start', data).then(res => res.data)
}

/** 结束计时 */
function stop(data) {
  return post('/wx/worklog/stop', data).then(res => res.data)
}

/** 手动补录：hours 为小时数，startTime/endTime 可选 */
function addManual(data) {
  return post('/wx/worklog/manual', data).then(res => res.data)
}

/** 我的打卡记录 */
function list(query) {
  return get('/wx/worklog/list', query).then(res => ({
    rows: (res.data && res.data.rows) || (res.data || []),
    total: (res.data && res.data.total) || res.total || 0
  }))
}

/** 今日清单 + 今日合计（一次请求拿全，减少小程序端请求数） */
function today() {
  return get('/wx/worklog/today').then(res => res.data || {})
}

/** 我的工时趋势 */
function statDaily(query) {
  return get('/wx/worklog/stat/daily', query).then(res => res.data || [])
}

/** 作废一条记录 */
function cancel(logId) {
  return post('/wx/worklog/cancel/' + logId).then(res => res.data)
}

module.exports = { getRunning, start, stop, addManual, list, today, statDaily, cancel }
