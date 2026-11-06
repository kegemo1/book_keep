const { post, get, put, getToken } = require('./request')
const { baseUrl } = require('./config')

/**
 * 微信身份说明：
 * 微信用户与后台账号（sys_user）彻底分离、不做任何绑定。
 * 小程序侧只有一套身份 —— biz_wx_user，登录后拿到的是 RuoYi 标准令牌。
 */

/**
 * 调用 wx.login 获取临时凭证 code
 */
function getCode() {
  return new Promise((resolve, reject) => {
    wx.login({
      timeout: 10000,
      success: res => {
        if (res.code) {
          resolve(res.code)
        } else {
          reject(new Error('wx.login 失败：' + res.errMsg))
        }
      },
      fail: reject
    })
  })
}

/**
 * 微信一键登录：code 换系统令牌
 */
function login(nickName, avatarUrl) {
  return getCode()
    .then(code => post('/wx/auth/login', { code, nickName, avatarUrl }))
    .then(res => {
      const data = res.data || {}
      if (data.token) {
        wx.setStorageSync('token', data.token)
        wx.setStorageSync('userInfo', {
          userId: data.userId,
          userType: data.userType,
          userName: data.userName,
          nickName: data.nickName,
          avatar: data.avatar
        })
      }
      return data
    })
}

/**
 * 静默登录（供请求层 401 时自动重试使用）
 */
function silentLogin() {
  return getCode()
    .then(code => post('/wx/auth/login', { code }))
    .then(res => {
      const data = res.data || {}
      if (data.token) {
        wx.setStorageSync('token', data.token)
      }
      return data
    })
}

/**
 * 个人资料 + 概览统计（「我的」页一次拿全）
 */
function getInfo() {
  return get('/wx/auth/info').then(res => res.data || {})
}

/**
 * 上传头像到 RuoYi 通用上传接口
 *
 * 注意两点：
 * 1. chooseAvatar 拿到的是本地临时路径，不上传的话后端存了也无法访问
 * 2. RuoYi 的 /common/upload 把 url 放在**响应顶层**（ajax.put("url", ...)），
 *    不是 data.url，因此这里两种结构都要兼容，否则会误判为上传失败
 *
 * @return {Promise<{ok:Boolean, url:String, msg:String}>} 结构化结果，便于定位问题
 */
function uploadAvatar(filePath) {
  return new Promise(resolve => {
    const url = baseUrl + '/common/upload'
    console.log('[upload] 开始上传 →', url, 'filePath=', filePath)
    wx.uploadFile({
      url: url,
      filePath: filePath,
      name: 'file',
      header: { 'Authorization': 'Bearer ' + getToken() },
      success(res) {
        console.log('[upload] statusCode=', res.statusCode, ' 原始响应=', res.data)
        if (res.statusCode < 200 || res.statusCode >= 300) {
          resolve({ ok: false, url: '', msg: 'HTTP ' + res.statusCode + '（' + url + '）' })
          return
        }
        let body = null
        try {
          body = JSON.parse(res.data)
        } catch (e) {
          resolve({ ok: false, url: '', msg: '响应不是合法 JSON：' + String(res.data).slice(0, 200) })
          return
        }
        // 兼容两种返回结构：顶层 url（RuoYi 原生）与 data.url（部分封装）
        const fileUrl = (body && (body.url || (body.data && body.data.url))) || ''
        if (body && body.code !== undefined && body.code !== 200) {
          resolve({ ok: false, url: '', msg: body.msg || ('上传接口返回 code=' + body.code) })
          return
        }
        resolve({
          ok: !!fileUrl,
          url: fileUrl,
          msg: fileUrl ? '' : ((body && body.msg) || '接口未返回文件地址')
        })
      },
      fail(err) {
        console.error('[upload] 失败', err)
        resolve({
          ok: false,
          url: '',
          msg: ((err && err.errMsg) || '网络异常') + '（' + url + '）'
        })
      }
    })
  })
}

/**
 * 修改个人资料（微信昵称 / 头像）
 * @param {String} nickName 昵称，不修改传 undefined
 * @param {String} avatarUrl 已上传的 http 头像地址
 */
function updateProfile(nickName, avatarUrl) {
  return put('/wx/auth/profile', { nickName, avatarUrl }).then(res => res.data)
}

/**
 * 是否已登录
 */
function isLogin() {
  return !!wx.getStorageSync('token')
}

/**
 * 退出登录（服务端无状态，主要清除本地 token）
 */
function logout() {
  post('/wx/auth/logout').catch(() => {})
  wx.removeStorageSync('token')
  wx.removeStorageSync('userInfo')
}

/**
 * 导出个人数据（REQ-002）：生成 Excel（打卡+流水两 Sheet）与 JSON 备份
 * @param {Object} params {scope:'all'|'year'|'custom', beginDate, endDate}
 * @return {Promise<{xlsxUrl, jsonUrl, logCount, recordCount, exportTime}>}
 */
function exportData(params) {
  return post('/wx/export', params).then(res => res.data)
}

/**
 * 注销前检查（REQ-001）：是否有进行中计时、应收款金额
 * @return {Promise<{canCancel:Boolean, hasRunning:Boolean, receivable:Number}>}
 */
function cancelCheck() {
  return get('/wx/auth/cancel/check').then(res => res.data || {})
}

/**
 * 账号注销：biz_wx_user 档案脱敏（openid 置空），30 天后物理清理；
 * 注销成功后服务端令牌立即失效，调用方需清除本地登录态
 * @param {Boolean} confirmed 有应收款时的二次确认标记
 */
function cancelAccount(confirmed) {
  return post('/wx/auth/cancel', { confirmed }).then(res => res.data)
}

module.exports = { getCode, login, silentLogin, getInfo, uploadAvatar, updateProfile, isLogin, logout, exportData, cancelCheck, cancelAccount }
