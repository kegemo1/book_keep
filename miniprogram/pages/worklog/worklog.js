const app = getApp()
const { getRunning, list, addManual, cancel } = require('../../services/worklog')
const { list: listProject } = require('../../services/project')
const { todayStr, offsetDays, withinDays } = require('../../utils/format')
const { backfillMaxDays, maxManualSeconds } = require('../../utils/config')

Page({
  data: {
    running: null,
    records: [],
    pageNum: 1,
    pageSize: 10,
    total: 0,
    loading: false,
    noMore: false,
    todayHours: 0,
    todayCount: 0,

    // 补录表单
    showForm: false,
    projects: [],
    projectNames: [],
    projectIndex: -1,
    projectId: null,
    taskNames: [],
    taskIndex: -1,
    taskId: null,
    logDate: '',
    minDate: '',
    hours: '2',
    content: '',
    billable: '0',
    submitting: false
  },

  onLoad() {
    this.setData({
      logDate: todayStr(),
      minDate: offsetDays(new Date(), -backfillMaxDays)
    })
    this.loadProjects()
  },

  onShow() {
    this.reload()
    // 静默登录是异步的，onLoad 时 token 可能未就绪导致项目加载失败；补一次
    if (this.data.projects.length === 0) {
      this.loadProjects()
    }
  },

  onPullDownRefresh() {
    this.reload(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (this.data.noMore || this.data.loading) return
    this.setData({ pageNum: this.data.pageNum + 1 })
    this.loadMore()
  },

  reload(done) {
    getRunning()
      .then(running => {
        app.setRunning(running)
        this.setData({ running })
      })
      .catch(() => {})

    this.setData({ pageNum: 1, records: [], noMore: false })
    this.loadMore(done)
  },

  loadMore(done) {
    if (this.data.loading) {
      if (done) done()
      return
    }
    this.setData({ loading: true })
    const { pageNum, pageSize } = this.data
    list({ pageNum, pageSize })
      .then(res => {
        const rows = (res.rows || []).map(normalizeRecord)
        const records = pageNum === 1 ? rows : this.data.records.concat(rows)
        const total = res.total || 0
        this.setData({
          records,
          total,
          loading: false,
          noMore: records.length >= total
        })
        this.calcToday(records)
      })
      .catch(() => this.setData({ loading: false }))
      .finally(() => {
        if (done) done()
      })
  },

  /** 今日合计在端上算，省一次请求 */
  calcToday(records) {
    const today = todayStr()
    let seconds = 0
    let count = 0
    records.forEach(item => {
      if (item.logDate === today && item.logStatus === '1') {
        seconds += Number(item.duration || 0)
        count += 1
      }
    })
    this.setData({ todayHours: (seconds / 3600).toFixed(2), todayCount: count })
  },

  loadProjects() {
    listProject({}).then(rows => {
      this.setData({
        projects: rows || [],
        projectNames: (rows || []).map(item => item.projectName)
      })
    })
  },

  // ----------------------------------------------------------------
  // 手动补录
  // ----------------------------------------------------------------

  openForm() {
    this.setData({
      showForm: true,
      logDate: todayStr(),
      hours: '2',
      content: '',
      projectIndex: -1,
      projectId: null,
      taskId: null,
      taskIndex: -1,
      taskNames: []
    })
  },

  closeForm() {
    this.setData({ showForm: false })
  },

  onProjectChange(e) {
    const index = Number(e.detail.value)
    const project = this.data.projects[index]
    this.setData({
      projectIndex: index,
      projectId: project ? project.projectId : null,
      taskId: null,
      taskIndex: -1,
      taskNames: []
    })
    if (project) {
      require('../../services/project').taskOptions(project.projectId).then(tasks => {
        this.setData({ tasks: tasks || [], taskNames: (tasks || []).map(t => t.taskName) })
      })
    }
  },

  onTaskChange(e) {
    const index = Number(e.detail.value)
    const task = (this.data.tasks || [])[index]
    this.setData({ taskIndex: index, taskId: task ? task.taskId : null })
  },

  onDateChange(e) {
    this.setData({ logDate: e.detail.value })
  },

  onHoursInput(e) {
    this.setData({ hours: e.detail.value })
  },

  onContentInput(e) {
    this.setData({ content: e.detail.value })
  },

  onBillableChange(e) {
    this.setData({ billable: e.detail.value ? '0' : '1' })
  },

  submitManual() {
    const hours = Number(this.data.hours)
    if (!this.data.projectId) {
      wx.showToast({ title: '请选择项目', icon: 'none' })
      return
    }
    if (!hours || hours <= 0) {
      wx.showToast({ title: '工时必须大于 0', icon: 'none' })
      return
    }
    const seconds = Math.round(hours * 3600)
    if (seconds > maxManualSeconds) {
      wx.showToast({ title: '单条不能超过 24 小时', icon: 'none' })
      return
    }
    if (!withinDays(this.data.logDate, backfillMaxDays)) {
      wx.showToast({ title: `只能补录近 ${backfillMaxDays} 天`, icon: 'none' })
      return
    }
    if (this.data.submitting) return

    this.setData({ submitting: true })
    wx.showLoading({ title: '提交中', mask: true })
    addManual({
      projectId: this.data.projectId,
      taskId: this.data.taskId,
      logDate: this.data.logDate,
      hours: hours,
      content: this.data.content,
      billable: this.data.billable
    })
      .then(() => {
        wx.hideLoading()
        wx.showToast({ title: '补录成功', icon: 'success' })
        this.setData({ showForm: false })
        this.reload()
      })
      .catch(() => wx.hideLoading())
      .finally(() => this.setData({ submitting: false }))
  },

  /** 作废记录 */
  onCancel(e) {
    const logId = e.currentTarget.dataset.id
    wx.showModal({
      title: '作废该记录',
      content: '记录会保留但不参与统计，可在后台恢复。',
      success: r => {
        if (!r.confirm) return
        cancel(logId)
          .then(() => {
            wx.showToast({ title: '已作废', icon: 'success' })
            this.reload()
          })
          .catch(() => {})
      }
    })
  },

  /** 就地结束一条进行中的计时记录（处理僵尸记录） */
  onStopRecord(e) {
    const logId = e.currentTarget.dataset.id
    require('../../services/worklog').stop({ logId: logId, discard: false })
      .then(result => {
        const hours = (result && result.totalHours) || 0
        wx.showToast({ title: `已记录 ${hours} 小时`, icon: 'success', duration: 2500 })
        this.reload()
      })
      .catch(() => {})
  },

  onStop(e) {
    const discard = e.detail.discard
    const running = this.data.running
    if (!running) return
    require('../../services/worklog').stop({ logId: running.logId, discard })
      .then(() => {
        wx.showToast({ title: discard ? '已丢弃' : '计时已结束', icon: 'success' })
        this.reload()
      })
      .catch(() => {})
  }
})

/**
 * 把枚举字段转成展示文本。
 * 放在 JS 里做而不写在 WXML 里，是为了避免：
 * ① 后端返回数字 0 而 WXML 里比较 '0' 字符串；
 * ② 不同基础库对 WXML 表达式中 === 的解析差异。
 * 统一用 String() 转换后判断，两种类型都能正确识别。
 */
function normalizeRecord(item) {
  const logType = String(item.logType)
  const logStatus = String(item.logStatus)
  const billable = String(item.billable)

  // 底部只保留一枚状态，按优先级互斥：进行中 > 已作废 > 计费/不计费。
  // 之前「计费」和「已作废」是同一枚标签的两个分支，作废后计费信息就丢了，
  // 同时「补录」「已作废」两个色块并排也互相打架
  let stateText = '不计费'
  let stateClass = 'is-free'
  if (logStatus === '0') {
    stateText = '进行中'
    stateClass = 'is-running'
  } else if (logStatus === '2') {
    stateText = '已作废'
    stateClass = 'is-void'
  } else if (billable === '0') {
    stateText = '计费'
    stateClass = 'is-billable'
  }

  return {
    ...item,
    // 日期栏只显示 MM-DD，年份在小程序场景里是噪音
    dateLabel: String(item.logDate || '').substring(5),
    logTypeText: logType === '0' ? '计时器' : '补录',
    isRunning: logStatus === '0',
    isVoid: logStatus === '2',
    // 只有已完成的才能作废：进行中要先结束，已作废不必再作废
    canCancel: logStatus === '1',
    stateText: stateText,
    stateClass: stateClass
  }
}
