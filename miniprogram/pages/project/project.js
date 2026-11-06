const { profit: fetchProfit, detail, add, update, remove } = require('../../services/project')
const { todayStr, money } = require('../../utils/format')

const PROJECT_TYPES = [
  { label: '自研产品', value: '1' },
  { label: '客户外包', value: '2' },
  { label: '内部事务', value: '3' }
]

const STATUS_MAP = { '0': '进行中', '1': '已完成', '2': '已暂停', '3': '已归档' }

Page({
  data: {
    list: [],
    loading: false,
    totalIncome: 0,
    totalCost: 0,
    totalNet: 0,
    totalNetNegative: false,

    showForm: false,
    isEdit: false,
    types: PROJECT_TYPES,
    typeNames: ['自研产品', '客户外包', '内部事务'],
    typeIndex: 0,
    form: {
      projectId: undefined,
      projectCode: '',
      projectName: '',
      projectType: '1',
      customerName: '',
      budgetAmount: '',
      hourRate: '',
      startDate: '',
      planEndDate: '',
      remark: ''
    },
    submitting: false
  },

  onShow() {
    this.load()
  },

  onPullDownRefresh() {
    this.load(() => wx.stopPullDownRefresh())
  },

  load(done) {
    this.setData({ loading: true })
    fetchProfit({})
      .then(rows => {
        // 图表条形宽度基准：取所有项目 income/cost 的最大值
        const maxBar = Math.max(1, ...(rows || []).map(r =>
          Math.max(Number(r.incomeTotal || 0), Number(r.costTotal || 0))))
        const list = (rows || []).map(item => ({
          ...item,
          statusText: STATUS_MAP[item.status] || '进行中',
          netNegative: Number(item.netProfit || 0) < 0,
          budgetPercent: Math.min(Number(item.budgetUsedRate || 0), 100),
          // 展示用金额统一千分位，模板里只负责排版
          incomeText: money(item.incomeTotal),
          costText: money(item.costTotal),
          grossText: money(item.grossProfit),
          laborText: money(item.laborCost),
          netText: money(item.netProfit),
          receivableText: money(item.receivable),
          budgetText: money(item.budgetAmount),
          // 图表条形百分比
          incomeBar: Math.min(100, Math.round(Number(item.incomeTotal || 0) / maxBar * 100)),
          costBar: Math.min(100, Math.round(Number(item.costTotal || 0) / maxBar * 100)),
          // 收入/成本/工时全为 0 说明该项目尚未产生任何数据，给出提示而不是一片 0
          hasData: Number(item.incomeTotal || 0) !== 0
            || Number(item.costTotal || 0) !== 0
            || Number(item.workHours || 0) !== 0
        }))
        let income = 0
        let cost = 0
        let net = 0
        list.forEach(item => {
          income += Number(item.incomeTotal || 0)
          cost += Number(item.costTotal || 0)
          net += Number(item.netProfit || 0)
        })
        this.setData({
          list,
          totalIncome: money(income),
          totalCost: money(cost),
          totalNet: money(net),
          totalNetNegative: net < 0
        })
      })
      .catch(err => {
        console.error('[project] 盈亏接口失败', err)
        wx.showToast({ title: '项目数据加载失败', icon: 'none' })
      })
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
      typeIndex: 0,
      form: {
        projectId: undefined,
        projectCode: '',
        projectName: '',
        projectType: '1',
        customerName: '',
        budgetAmount: '',
        hourRate: '',
        startDate: todayStr(),
        planEndDate: '',
        remark: ''
      }
    })
  },

  openEdit(e) {
    const id = e.currentTarget.dataset.id
    wx.showLoading({ title: '加载中', mask: true })
    detail(id)
      .then(project => {
        wx.hideLoading()
        if (!project) {
          wx.showToast({ title: '项目不存在', icon: 'none' })
          return
        }
        const typeIndex = PROJECT_TYPES.findIndex(t => t.value === project.projectType)
        this.setData({
          showForm: true,
          isEdit: true,
          typeIndex: typeIndex < 0 ? 0 : typeIndex,
          form: {
            projectId: project.projectId,
            projectCode: project.projectCode || '',
            projectName: project.projectName || '',
            projectType: project.projectType || '1',
            customerName: project.customerName || '',
            budgetAmount: project.budgetAmount != null ? String(project.budgetAmount) : '',
            hourRate: project.hourRate != null ? String(project.hourRate) : '',
            startDate: project.startDate || '',
            planEndDate: project.planEndDate || '',
            remark: project.remark || ''
          }
        })
      })
      .catch(() => wx.hideLoading())
  },

  closeForm() {
    this.setData({ showForm: false })
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ ['form.' + field]: e.detail.value })
  },

  onTypeChange(e) {
    const index = Number(e.detail.value)
    this.setData({ typeIndex: index, 'form.projectType': PROJECT_TYPES[index].value })
  },

  onDateChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ ['form.' + field]: e.detail.value })
  },

  submitForm() {
    const form = this.data.form
    if (!form.projectName || !form.projectName.trim()) {
      wx.showToast({ title: '请输入项目名称', icon: 'none' })
      return
    }
    if (!this.data.isEdit && (!form.projectCode || !form.projectCode.trim())) {
      wx.showToast({ title: '请输入项目编号', icon: 'none' })
      return
    }
    if (this.data.submitting) return

    const payload = {
      projectName: form.projectName.trim(),
      projectType: form.projectType,
      customerName: form.customerName,
      budgetAmount: Number(form.budgetAmount || 0),
      hourRate: Number(form.hourRate || 0),
      startDate: form.startDate || undefined,
      planEndDate: form.planEndDate || undefined,
      remark: form.remark
    }
    if (this.data.isEdit) {
      payload.projectId = form.projectId
    } else {
      payload.projectCode = form.projectCode.trim()
    }

    this.setData({ submitting: true })
    wx.showLoading({ title: '提交中', mask: true })
    const task = this.data.isEdit ? update(payload) : add(payload)
    task
      .then(() => {
        wx.hideLoading()
        wx.showToast({ title: this.data.isEdit ? '已保存' : '项目已创建', icon: 'success' })
        this.setData({ showForm: false })
        this.load()
      })
      .catch(() => wx.hideLoading())
      .finally(() => this.setData({ submitting: false }))
  },

  /** 进入项目任务管理 */
  goTask(e) {
    const id = e.currentTarget.dataset.id
    const name = encodeURIComponent(e.currentTarget.dataset.name || '')
    wx.navigateTo({ url: '/pages/task/task?projectId=' + id + '&projectName=' + name })
  },

  onDelete(e) {
    const id = e.currentTarget.dataset.id
    const name = e.currentTarget.dataset.name
    wx.showModal({
      title: '删除项目',
      content: `确认删除「${name}」？已有工时或流水的项目不能删除，建议改为归档。`,
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
