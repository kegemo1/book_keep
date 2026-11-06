const { isLogin, login, getInfo, logout, uploadAvatar, updateProfile, exportData, cancelCheck, cancelAccount } = require('../../utils/auth')
const { baseUrl } = require('../../utils/config')

Page({
  data: {
    login: false,
    wxUser: null,
    nickInitial: '账',
    stat: {},
    editing: false,
    nickNameInput: '',
    uploading: false,
    // 账号状态由 JS 预先算好：兼容后端返回字符串 '0' 或数字 0，
    // 避免在 WXML 里做字面量比较（类型不一致时会误判为「已停用」）
    statusOk: false,
    statusText: '未知',
    // 数据导出（REQ-002）
    exportShow: false,
    scopeNames: ['全部', '本年', '自定义'],
    scopeIndex: 0,
    exportBegin: '',
    exportEnd: '',
    exporting: false,
    exportResult: null,
    envText: baseUrl.indexOf('https') === 0 ? '生产环境' : '开发环境'
  },

  onLoad() {
    console.log('[mine] 接口地址 =', baseUrl)
  },

  onShow() {
    this.refresh()
  },

  onPullDownRefresh() {
    this.refresh(() => wx.stopPullDownRefresh())
  },

  refresh(done) {
    if (!isLogin()) {
      this.setData({ login: false, wxUser: null, stat: {} })
      if (done) done()
      return
    }
    getInfo()
      .then(info => {
        const wxUser = info.wxUser || null
        // 存量头像可能是 http:// 存的（Nginx 301 到 https，image 组件不跟随会回显失败），统一替换
        if (wxUser && wxUser.avatarUrl) {
          wxUser.avatarUrl = String(wxUser.avatarUrl).replace(/^http:\/\//, 'https://')
        }
        console.log('[mine] info =', info)
        if (!wxUser) {
          // 拿到了响应却没有账号信息：多半是旧 token（userId 与当前账号体系不匹配）
          // 或后端未更新，此时引导重新登录，而不是展示一个空壳页面
          this.setData({ login: true, wxUser: null, stat: {}, statusOk: false, statusText: '未知' })
          wx.showModal({
            title: '登录态已失效',
            content: '未获取到微信账号信息，请重新登录后再试。若仍失败，请确认后端已使用最新代码重启。',
            confirmText: '重新登录',
            success: r => {
              if (r.confirm) {
                logout()
                this.onLogin()
              }
            }
          })
          return
        }

        this.setData({
          login: true,
          wxUser: wxUser,
          nickInitial: initialOf(wxUser.nickName),
          stat: info.stat || {},
          nickNameInput: wxUser.nickName || '',
          ...resolveStatus(wxUser)
        })
      })
      .catch(err => {
        console.error('[mine] 获取资料失败', err)
      })
      .finally(() => {
        if (done) done()
      })
  },

  onLogin() {
    wx.showLoading({ title: '登录中', mask: true })
    login()
      .then(() => {
        wx.hideLoading()
        wx.showToast({ title: '登录成功', icon: 'success' })
        this.refresh()
      })
      .catch(() => {
        wx.hideLoading()
        wx.showToast({ title: '登录失败', icon: 'none' })
      })
  },

  /**
   * 更换头像：两阶段处理
   * ① chooseAvatar 得到本地临时路径 → 上传到 /common/upload 换取 http 地址
   * ② 调用 /wx/auth/profile 保存到账号
   * 两阶段分别提示，避免「明明上传成功却只说失败」的误判
   */
  onChooseAvatar(e) {
    const filePath = e.detail && e.detail.avatarUrl
    if (!filePath) return
    if (this.data.uploading) return

    this.setData({ uploading: true })
    wx.showLoading({ title: '上传中', mask: true })

    uploadAvatar(filePath)
      .then(result => {
        console.log('[mine] 上传结果 =', result)
        if (!result || !result.ok) {
          wx.hideLoading()
          this.setData({ uploading: false })
          this.showUploadError('头像文件上传失败', (result && result.msg) || '未知原因')
          return
        }

        // 阶段 ① 成功，进入阶段 ②
        return updateProfile(undefined, result.url)
          .then(() => {
            wx.hideLoading()
            this.setData({ uploading: false, 'wxUser.avatarUrl': result.url })
            const cached = wx.getStorageSync('userInfo') || {}
            cached.avatar = result.url
            wx.setStorageSync('userInfo', cached)
            wx.showToast({ title: '头像已更新', icon: 'success' })
          })
          .catch(err => {
            wx.hideLoading()
            this.setData({ uploading: false })
            console.error('[mine] 保存头像失败', err)
            this.showUploadError(
              '头像文件已上传，但保存失败',
              (err && err.msg) || '接口 /wx/auth/profile 调用异常，请确认后端已更新并重启'
            )
          })
      })
      .catch(err => {
        wx.hideLoading()
        this.setData({ uploading: false })
        console.error('[mine] 上传异常', err)
        this.showUploadError('头像上传失败', (err && err.msg) || '未知异常')
      })
  },

  showUploadError(title, detail) {
    wx.showModal({
      title: title,
      content: detail,
      showCancel: false,
      confirmText: '知道了'
    })
  },

  onToggleEdit() {
    this.setData({ editing: !this.data.editing })
  },

  onNicknameInput(e) {
    this.setData({ nickNameInput: e.detail.value })
  },

  saveNickName() {
    const nick = (this.data.nickNameInput || '').trim()
    if (!nick) {
      wx.showToast({ title: '昵称不能为空', icon: 'none' })
      return
    }
    // 先收起键盘：type=nickname 的输入框在卸载/失焦的瞬间，微信会再拉起一次
    // 「头像昵称填写」浮层，用户会以为「点保存弹出了改头像」
    wx.hideKeyboard()
    wx.showLoading({ title: '保存中', mask: true })
    updateProfile(nick, undefined)
      .then(() => {
        wx.hideLoading()
        this.setData({ 'wxUser.nickName': nick, nickInitial: initialOf(nick), editing: false })
        const cached = wx.getStorageSync('userInfo') || {}
        cached.nickName = nick
        wx.setStorageSync('userInfo', cached)
        wx.showToast({ title: '已保存', icon: 'success' })
      })
      .catch(err => {
        wx.hideLoading()
        console.error('[mine] 保存昵称失败', err)
        this.showUploadError('昵称保存失败', (err && err.msg) || '请确认后端已更新并重启')
      })
  },

  cancelEdit() {
    wx.hideKeyboard()
    this.setData({ editing: false, nickNameInput: (this.data.wxUser && this.data.wxUser.nickName) || '' })
  },

  // ------------------------------------------------------------------
  // 数据导出（REQ-002）
  // ------------------------------------------------------------------

  openExport() {
    this.setData({
      exportShow: true,
      exportResult: null,
      exportBegin: '',
      exportEnd: '',
      scopeIndex: 0
    })
  },

  closeExport() {
    if (this.data.exporting) {
      wx.showToast({ title: '正在生成，请稍候', icon: 'none' })
      return
    }
    this.setData({ exportShow: false })
  },

  onScopeChange(e) {
    this.setData({ scopeIndex: Number(e.detail.value) })
  },

  onBeginChange(e) {
    this.setData({ exportBegin: e.detail.value })
  },

  onEndChange(e) {
    this.setData({ exportEnd: e.detail.value })
  },

  doExport() {
    if (this.data.exporting) return
    const scope = ['all', 'year', 'custom'][this.data.scopeIndex]
    const { exportBegin, exportEnd } = this.data
    if (scope === 'custom' && (!exportBegin || !exportEnd)) {
      wx.showToast({ title: '请选择起止日期', icon: 'none' })
      return
    }
    if (scope === 'custom' && exportBegin > exportEnd) {
      wx.showToast({ title: '开始日期不能晚于结束日期', icon: 'none' })
      return
    }

    this.setData({ exporting: true })
    wx.showLoading({ title: '生成中', mask: true })
    exportData({ scope, beginDate: exportBegin, endDate: exportEnd })
      .then(res => {
        wx.hideLoading()
        // 注意：auth.js 里已经取过一次 res.data（业务数据），这里不能再取
        const d = res || {}
        this.setData({ exportResult: d, exporting: false })
        wx.showToast({ title: '导出成功', icon: 'success' })
      })
      .catch(err => {
        wx.hideLoading()
        this.setData({ exporting: false })
        wx.showModal({
          title: '导出失败',
          content: (err && err.msg) || '请稍后再试',
          showCancel: false
        })
      })
  },

  /** 下载并打开 Excel（wx.openDocument 支持在线预览，右上角可另存） */
  openExcel() {
    const url = this.data.exportResult && this.data.exportResult.xlsxUrl
    if (!url) return
    wx.showLoading({ title: '下载中', mask: true })
    wx.downloadFile({
      url,
      success: res => {
        wx.hideLoading()
        wx.openDocument({
          filePath: res.tempFilePath,
          fileType: 'xlsx',
          showMenu: true,
          fail: () => wx.showToast({ title: '打开失败', icon: 'none' })
        })
      },
      fail: () => {
        wx.hideLoading()
        wx.showToast({ title: '下载失败', icon: 'none' })
      }
    })
  },

  /** JSON 备份文件用电脑浏览器打开链接下载（手机端不解析 json） */
  copyJson() {
    const url = this.data.exportResult && this.data.exportResult.jsonUrl
    if (!url) return
    wx.setClipboardData({
      data: url,
      success: () => wx.showToast({ title: '链接已复制', icon: 'success' })
    })
  },

  onLogout() {
    wx.showModal({
      title: '退出登录',
      content: '确定退出当前微信账号？',
      success: r => {
        if (!r.confirm) return
        logout()
        this.setData({ login: false, wxUser: null, stat: {}, editing: false })
        wx.showToast({ title: '已退出', icon: 'success' })
      }
    })
  },

  /**
   * 账号注销（REQ-001）：check → 分级确认 → 注销 → 清理本地登录态
   * 分级：进行中计时 → 阻断；有应收款 → 强提醒；其余 → 普通确认
   */
  onCancelAccount() {
    cancelCheck()
      .then(res => {
        const d = res.data || {}
        if (d.hasRunning) {
          wx.showModal({
            title: '无法注销',
            content: '存在进行中的计时，请先结束或丢弃后再注销。',
            showCancel: false
          })
          return
        }
        const receivable = Number(d.receivable || 0)
        if (receivable > 0) {
          wx.showModal({
            title: '注销前请确认',
            content: `您还有 ¥${receivable} 应收款未收回，注销后无法追讨。确定继续注销吗？`,
            confirmColor: '#ae5f49',
            success: r => { if (r.confirm) this.doCancel() }
          })
        } else {
          wx.showModal({
            title: '注销账号',
            content: '注销后您的所有工时、项目、记账数据将无法恢复，确定继续吗？',
            confirmColor: '#ae5f49',
            success: r => { if (r.confirm) this.doCancel() }
          })
        }
      })
      .catch(() => wx.showToast({ title: '检查失败，请稍后再试', icon: 'none' }))
  },

  doCancel() {
    wx.showLoading({ title: '处理中', mask: true })
    cancelAccount(true)
      .then(() => {
        wx.hideLoading()
        logout()
        this.setData({ login: false, wxUser: null, stat: {}, editing: false })
        wx.showToast({ title: '账号已注销', icon: 'success', duration: 2500 })
      })
      .catch(() => {
        wx.hideLoading()
        wx.showToast({ title: '注销失败，请稍后再试', icon: 'none' })
      })
  }
})

/**
 * 取昵称首字符：没有头像时用它做兜底占位，比灰色破图或默认剪影更体面
 * 用 Array.from 而非 charAt，避免 emoji / 生僻字被截成半个码元
 */
function initialOf(nickName) {
  const name = String(nickName || '').trim()
  if (!name) return '账'
  const first = Array.from(name)[0]
  return first || '账'
}

/**
 * 解析账号状态：兼容字符串 '0' 与数字 0，字段缺失时显示「未知」
 */
function resolveStatus(wxUser) {
  const raw = wxUser ? wxUser.status : undefined
  if (raw === undefined || raw === null) {
    return { statusOk: false, statusText: '未知' }
  }
  const ok = String(raw) === '0'
  return { statusOk: ok, statusText: ok ? '正常' : '已停用' }
}
