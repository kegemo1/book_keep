const { get, post, put, del } = require('../utils/request')

/**
 * 项目与统计服务
 * 小程序是微信用户创建/维护项目的唯一入口（与后台彻底分离）
 */

/** 我的项目列表（status 为空时后端默认返回进行中的项目） */
function list(query) {
  return get('/wx/project/list', query || {}).then(res => res.data || [])
}

/** 项目详情 */
function detail(projectId) {
  return get('/wx/project/' + projectId).then(res => res.data || null)
}

/** 新建项目 */
function add(data) {
  return post('/wx/project', data).then(res => res.data)
}

/** 修改项目 */
function update(data) {
  return put('/wx/project', data).then(res => res.data)
}

/** 删除项目 */
function remove(projectId) {
  return del('/wx/project/' + projectId).then(res => res.data)
}

/** 项目下的任务下拉 */
function taskOptions(projectId) {
  return get('/wx/project/taskOptions/' + projectId).then(res => res.data || [])
}

/** 项目盈亏清单 */
function profit(query) {
  return get('/wx/project/profit', query || {}).then(res => {
    const rows = res.data || []
    return rows
  })
}

/** 首页概览 */
function dashboard() {
  return get('/wx/project/dashboard').then(res => res.data || {})
}

module.exports = { list, detail, add, update, remove, taskOptions, profit, dashboard }
