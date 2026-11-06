const { list, add, update, done, remove } = require('../../services/task')

const STATUS_TEXT = { '0': '待办', '1': '进行中', '2': '已完成', '3': '已取消' }
const PRIORITY_TEXT = { '0': '高', '1': '中', '2': '低' }
const PRIORITIES = ['高', '中', '低']

Page({
  data: {
    projectId: null,
    projectName: '',
    tasks: [],
    loading: false,

    showForm: false,
    isEdit: false,
    priorities: PRIORITIES,
    priorityIndex: 1,
    form: {
      taskId: undefined,
      taskName: '',
      priority: '2',
      estimateHours: '',
      progress: 0,
      remark: ''
    },
    submitting: false
  },

  onLoad(options) {
    this.setData({
      projectId: Number(options.projectId) || null,
      projectName: decodeURIComponent(options.projectName || '')
    })
    if (this.data.projectName) {
      wx.setNavigationBarTitle({ title: this.data.projectName + ' · 任务' })
    }
  },

  onShow() {
    this.load()
  },

  onPullDownRefresh() {
    this.load(() => wx.stopPullDownRefresh())
  },

  load(done) {
    if (!this.data.projectId) {
      if (done) done()
      return
    }
    this.setData({ loading: true })
    list({ projectId: this.data.projectId })
      .then(rows => {
        const tasks = (rows || []).map(normalizeTask)
        console.log('[task] 任务数 =', tasks.length)
        this.setData({ tasks })
      })
      .catch(() => {})
      .finally(() => {
        this.setData({ loading: false })
        if (done) done()
      })
  },

  // ----------------------------------------------------------------
  // 新建 / 编辑
  // ----------------------------------------------------------------

  openCreate() {
    this.setData({
      showForm: true,
      isEdit: false,
      priorityIndex: 1,
      form: {
        taskId: undefined,
        taskName: '',
        priority: '2',
        estimateHours: '',
        progress: 0,
        remark: ''
      }
    })
  },

  openEdit(e) {
    const id = e.currentTarget.dataset.id
    const task = this.data.tasks.find(t => t.taskId === id)
    if (!task) return
    this.setData({
      showForm: true,
      isEdit: true,
      priorityIndex: Math.max(0, PRIORITIES.indexOf(task.priorityText)),
      form: {
        taskId: task.taskId,
        taskName: task.taskName,
        priority: task.priority,
        estimateHours: task.estimateHours != null ? String(task.estimateHours) : '',
        progress: task.progress || 0,
        remark: task.remark || ''
      }
    })
  },

  closeForm() {
    this.setData({ showForm: false })
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ ['form.' + field]: e.detail.value })
  },

  onPriorityChange(e) {
    const index = Number(e.detail.value)
    this.setData({ priorityIndex: index, 'form.priority': String(index) })
  },

  onProgressChange(e) {
    this.setData({ 'form.progress': e.detail.value })
  },

  submitForm() {
    const form = this.data.form
    if (!form.taskName || !form.taskName.trim()) {
      wx.showToast({ title: '请输入任务名称', icon: 'none' })
      return
    }
    if (this.data.submitting) return

    const payload = {
      projectId: this.data.projectId,
      taskName: form.taskName.trim(),
      priority: form.priority,
      estimateHours: Number(form.estimateHours || 0),
      progress: Number(form.progress || 0),
      remark: form.remark
    }
    if (this.data.isEdit) {
      payload.taskId = form.taskId
    }

    this.setData({ submitting: true })
    wx.showLoading({ title: '提交中', mask: true })
    const task = this.data.isEdit ? update(payload) : add(payload)
    task
      .then(() => {
        wx.hideLoading()
        wx.showToast({ title: this.data.isEdit ? '已保存' : '任务已创建', icon: 'success' })
        this.setData({ showForm: false })
        this.load()
      })
      .catch(() => wx.hideLoading())
      .finally(() => this.setData({ submitting: false }))
  },

  /** 完成 / 重开 */
  onToggleDone(e) {
    const id = e.currentTarget.dataset.id
    const task = this.data.tasks.find(t => t.taskId === id)
    if (!task) return
    if (task.isDone) {
      // 重开
      update({ taskId: id, status: '0', progress: 0 })
        .then(() => this.load())
        .catch(() => {})
    } else {
      done(id)
        .then(() => {
          wx.showToast({ title: '任务完成', icon: 'success' })
          this.load()
        })
        .catch(() => {})
    }
  },

  onDelete(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '删除任务',
      content: '确认删除该任务？若已产生打卡记录，实际工时会一并失去归属。',
      success: r => {
        if (!r.confirm) return
        remove(id)
          .then(() => {
            wx.showToast({ title: '已删除', icon: 'success' })
            this.load()
          })
          .catch(() => {})
      }
    })
  }
})

/** 枚举转展示字段（判断一律放 JS，避免 WXML 字面量比较的类型坑） */
function normalizeTask(item) {
  const status = String(item.status)
  const priority = String(item.priority)
  return {
    ...item,
    statusText: STATUS_TEXT[status] || '待办',
    priorityText: PRIORITY_TEXT[priority] || '低',
    isDone: status === '2',
    isTodo: status === '0',
    percent: Math.min(Number(item.progress || 0), 100)
  }
}
