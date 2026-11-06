const app = getApp()
const { getRunning, start, stop } = require('../../services/worklog')
const { list: listProject, dashboard } = require('../../services/project')
const { money } = require('../../utils/format')

Page({
  data: {
    running: null,
    dash: {},
    greeting: '你好',
    dateLine: '',
    chartLabels: [],
    chartValues: [],
    chartTotal: '0.0',
    chartHasData: false,
    projects: [],
    projectNames: [],
    projectIndex: -1,
    projectId: null,
    tasks: [],
    taskNames: [],
    taskIndex: -1,
    taskId: null,
    content: '',
    billable: '0',
    submitting: false
  },

  onLoad() {
    this.setData({ greeting: greetingOf(new Date().getHours()), dateLine: dateLine(new Date()) })
    this.loadProjects()
  },

  onShow() {
    this.refresh()
    // 首次进入时静默登录是异步的，onLoad 里加载项目可能还没拿到 token 而失败；
    // 这里补一次，保证项目选择器不会一直空白
    if (this.data.projects.length === 0) {
      this.loadProjects()
    }
  },

  onPullDownRefresh() {
    this.refresh(() => wx.stopPullDownRefresh())
  },

  /** 刷新计时器与概览：两个接口并发，减少白屏等待 */
  refresh(done) {
    Promise.all([getRunning().catch(() => null), dashboard().catch(() => ({}))])
      .then(([running, dash]) => {
        // 趋势图：后端只返回「有记录」的日期（无记录的当天不出现），
        // 端上补齐近 7 天，否则休息日会被跳过、横轴日期不连续
        const raw = (dash && dash.recentDailyHours) || []
        const byDate = {}
        raw.forEach(i => {
          byDate[i.statDate] = Number(i.totalHours || 0)
        })
        const chartLabels = []
        const chartValues = []
        const now = new Date()
        for (let i = 6; i >= 0; i--) {
          const d = new Date(now.getTime() - i * 86400000)
          const m = d.getMonth() + 1
          const day = d.getDate()
          const key = `${d.getFullYear()}-${m < 10 ? '0' + m : m}-${day < 10 ? '0' + day : day}`
          chartLabels.push(key.substring(5))
          chartValues.push(byDate[key] || 0)
        }
        const chartTotal = chartValues.reduce((s, v) => s + v, 0).toFixed(1)
        const chartHasData = chartValues.some(v => v > 0)
        app.setRunning(running)
        this.setData({ running, dash, chartLabels, chartValues, chartTotal, chartHasData })
      })
      .finally(() => {
        if (done) done()
      })
  },

  loadProjects() {
    listProject({}).then(rows => {
      const projects = rows || []
      this.setData({
        projects,
        projectNames: projects.map(item => item.projectName)
      })
    })
  },

  onProjectChange(e) {
    const index = Number(e.detail.value)
    const project = this.data.projects[index]
    this.setData({
      projectIndex: index,
      projectId: project ? project.projectId : null,
      taskId: null,
      taskIndex: -1,
      tasks: [],
      taskNames: []
    })
    if (project) {
      require('../../services/project').taskOptions(project.projectId).then(tasks => {
        this.setData({
          tasks: tasks || [],
          taskNames: (tasks || []).map(item => item.taskName)
        })
      })
    }
  },

  onTaskChange(e) {
    const index = Number(e.detail.value)
    const task = this.data.tasks[index]
    this.setData({ taskIndex: index, taskId: task ? task.taskId : null })
  },

  onContentInput(e) {
    this.setData({ content: e.detail.value })
  },

  onBillableChange(e) {
    this.setData({ billable: e.detail.value ? '0' : '1' })
  },

  /** 开始计时 */
  onStart() {
    if (!this.data.projectId) {
      wx.showToast({ title: '请先选择项目', icon: 'none' })
      return
    }
    if (this.data.submitting) return
    this.setData({ submitting: true })
    start({
      projectId: this.data.projectId,
      taskId: this.data.taskId,
      content: this.data.content,
      billable: this.data.billable,
      // 小程序端切换项目是高频操作，允许自动结束上一条
      autoSwitch: true
    })
      .then(() => {
        wx.showToast({ title: '计时已开始', icon: 'success' })
        this.setData({ content: '' })
        this.refresh()
      })
      .catch(() => {})
      .finally(() => this.setData({ submitting: false }))
  },

  /** 结束计时（来自 timer-card 组件的 stop 事件） */
  onStop(e) {
    const discard = e.detail.discard
    const running = this.data.running
    if (!running) return

    const doStop = () => {
      wx.showLoading({ title: '处理中', mask: true })
      stop({ logId: running.logId, discard })
        .then(res => {
          wx.hideLoading()
          if (discard) {
            wx.showToast({ title: '已丢弃本次计时', icon: 'success' })
          } else {
            const hours = (res && res.totalHours) || 0
            wx.showToast({
              title: `记 ${hours} 小时${res && res.crossDay ? '（跨天已拆分）' : ''}`,
              icon: 'success',
              duration: 2500
            })
          }
          this.refresh()
        })
        .catch(() => wx.hideLoading())
    }

    if (discard) {
      wx.showModal({
        title: '丢弃本次计时',
        content: '记录会保留但标记为作废，不参与统计。确定吗？',
        success: r => { if (r.confirm) doStop() }
      })
    } else {
      doStop()
    }
  },

  /** 快捷跳转 */
  goFinance() {
    wx.switchTab({ url: '/pages/finance/finance' })
  },

  goWorklog() {
    wx.switchTab({ url: '/pages/worklog/worklog' })
  },

  formatMoney(value) {
    return money(value)
  },

  onShareAppMessage() {
    return {
      title: '账时记 · 工时打卡与项目收支',
      path: '/pages/index/index'
    }
  }
})

/** 按时间段给一句问候，替代冷冰冰的页面标题 */
function greetingOf(hour) {
  if (hour < 5) return '夜深了'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
}

/** 页头第二行：10月27日 周二 */
function dateLine(d) {
  const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
  return `${d.getMonth() + 1}月${d.getDate()}日 ${week}`
}
