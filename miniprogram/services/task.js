const { get, post, put, del } = require('../utils/request')

/**
 * 任务服务
 * 任务 = 项目下的二级维度，让工时能回答「这 3 小时具体花在哪件事上」
 */

/** 任务列表（按 projectId 过滤） */
function list(query) {
  return get('/wx/task/list', query || {}).then(res => res.data || [])
}

/** 任务详情 */
function detail(taskId) {
  return get('/wx/task/' + taskId).then(res => res.data || null)
}

/** 新建任务 */
function add(data) {
  return post('/wx/task', data).then(res => res.data)
}

/** 修改任务 */
function update(data) {
  return put('/wx/task', data).then(res => res.data)
}

/** 完成任务 */
function done(taskId) {
  return put('/wx/task/done/' + taskId).then(res => res.data)
}

/** 删除任务 */
function remove(taskId) {
  return del('/wx/task/' + taskId).then(res => res.data)
}

/** 回写实际工时 */
function syncHours(taskId) {
  return put('/wx/task/syncHours/' + taskId).then(res => res.data)
}

module.exports = { list, detail, add, update, done, remove, syncHours }
