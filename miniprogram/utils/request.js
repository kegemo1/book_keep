const { baseUrl } = require('./config')

// 登录态失效时，等待中的请求排队，避免并发触发多次重新登录
let refreshing = false
let waiters = []

function getToken() {
  return wx.getStorageSync('token') || ''
}

function onTokenRefreshed(token) {
  waiters.forEach(fn => fn(token))
  waiters = []
}

function waitToken() {
  return new Promise(resolve => waiters.push(resolve))
}

/**
 * 统一请求封装
 * 约定：后端返回 { code: 200, msg, data }，非 200 视为业务失败
 */
function request(options) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    wx.request({
      url: baseUrl + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? 'Bearer ' + token : '',
        ...(options.header || {})
      },
      timeout: 15000,
      success(res) {
        const body = res.data || {}
        // HTTP 401 或 业务码 401：登录态失效
        if (res.statusCode === 401 || body.code === 401) {
          wx.removeStorageSync('token')
          if (!refreshing) {
            refreshing = true
            require('./auth').silentLogin()
              .then(() => {
                refreshing = false
                onTokenRefreshed(getToken())
              })
              .catch(() => {
                refreshing = false
                onTokenRefreshed('')
              })
          }
          return waitToken().then(newToken => {
            if (!newToken) {
              reject(body)
              return
            }
            // 带上新 token 重试一次
            request(options).then(resolve).catch(reject)
          })
        }
        if (res.statusCode >= 200 && res.statusCode < 300) {
          if (body.code === 200 || body.code === undefined) {
            resolve(normalize(body))
          } else {
            wx.showToast({ title: body.msg || '请求失败', icon: 'none' })
            reject(body)
          }
          return
        }
        wx.showToast({ title: body.msg || ('请求异常 ' + res.statusCode), icon: 'none' })
        reject(body)
      },
      fail(err) {
        wx.showToast({ title: '网络异常，请检查网络', icon: 'none' })
        reject(err)
      }
    })
  })
}

/**
 * 统一响应结构，保证业务侧始终能用 res.data 取到业务数据。
 *
 * 背景：RuoYi 的 AjaxResult 有两种用法
 *   1) AjaxResult.success(data)            → {"code":200,"msg":"...","data":{...}}
 *   2) AjaxResult.success(); ajax.put(k,v) → {"code":200,"msg":"...","k":v}   ← 没有 data 层
 * 本项目 /wx/auth/info、/wx/worklog/today、/wx/finance/summary 用的是第 2 种，
 * 业务侧若直接读 res.data 会拿到 undefined。这里补一层：把除 code/msg 外的
 * 顶层字段收集到 data 上（浅拷贝，避免自引用）。
 */
function normalize(body) {
  if (!body || typeof body !== 'object') {
    return body
  }
  if (body.data !== undefined) {
    return body
  }
  const flat = {}
  Object.keys(body).forEach(key => {
    if (key !== 'code' && key !== 'msg') {
      flat[key] = body[key]
    }
  })
  body.data = flat
  return body
}

function get(url, data) {
  return request({ url, method: 'GET', data })
}

function post(url, data) {
  return request({ url, method: 'POST', data })
}

function put(url, data) {
  return request({ url, method: 'PUT', data })
}

function del(url, data) {
  return request({ url, method: 'DELETE', data })
}

module.exports = { request, get, post, put, del, getToken }
