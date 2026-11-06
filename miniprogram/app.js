const { isLogin, login, getInfo } = require('./utils/auth')

App({
  globalData: {
    userInfo: null,
    // 全局共享的「进行中计时器」，避免每个 tab 页各查一次
    running: null,
    // 计时器变化的回调集合
    runningListeners: []
  },

  onLaunch() {
    this.globalData.userInfo = wx.getStorageSync('userInfo') || null
    // 静默登录：token 不存在时先拿一个，避免首屏接口全部 401
    if (!isLogin()) {
      login()
        .then(data => {
          this.globalData.userInfo = {
            userId: data.userId,
            userName: data.userName,
            nickName: data.nickName,
            avatar: data.avatar,
            deptId: data.deptId
          }
          this.emitLogin()
        })
        .catch(() => {})
    }
  },

  onShow() {
    // 从后台切回前台时校验登录态
    if (!isLogin()) {
      login().catch(() => {})
    }
  },

  /** 更新全局计时器并通知监听方 */
  setRunning(running) {
    this.globalData.running = running
    this.globalData.runningListeners.forEach(fn => {
      try {
        fn(running)
      } catch (e) {}
    })
  },

  onRunningChange(fn) {
    this.globalData.runningListeners.push(fn)
  },

  emitLogin() {
    const pages = getCurrentPages()
    const current = pages[pages.length - 1]
    if (current && typeof current.onLoginSuccess === 'function') {
      current.onLoginSuccess()
    }
  },

  /** 带登录态检查地执行业务方法 */
  ensureLogin() {
    if (isLogin()) {
      return Promise.resolve()
    }
    return login().then(() => {})
  }
})
