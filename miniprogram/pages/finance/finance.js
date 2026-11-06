const { categories, statByCategory, add, list, summary } = require('../../services/finance')
const { list: listProject } = require('../../services/project')
const { todayStr, money } = require('../../utils/format')

const PAY_METHODS = ['微信', '支付宝', '银行转账', '现金', '其他']

Page({
  data: {
    summary: {},
    records: [],
    pageNum: 1,
    pageSize: 10,
    total: 0,
    loading: false,
    noMore: false,
    filterType: '',
    // 环形图
    ringSegments: '',
    ringTotal: '0',
    hasRingData: false,
    ringItems: [],

    // 记一笔表单
    showForm: false,
    recordType: '1',
    projects: [],
    projectNames: [],
    projectIndex: -1,
    projectId: null,
    categoryList: [],
    categoryNames: [],
    categoryIndex: -1,
    categoryId: null,
    payMethods: PAY_METHODS,
    payMethodIndex: 0,
    amount: '',
    recordDate: '',
    settledAmount: '',
    counterparty: '',
    taxAmount: '',
    tradeNo: '',
    invoiceNo: '',
    voucherUrl: '',
    remark: '',
    submitting: false
  },

  onLoad() {
    this.setData({ recordDate: todayStr() })
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
    summary().then(data => this.setData({ summary: data })).catch(() => {})
    this.loadChart()
    this.setData({ pageNum: 1, records: [], noMore: false })
    this.loadMore(done)
  },

  /** 加载支出环形图（按类别占比） */
  loadChart() {
    // 一组同色系的克制色，避免红绿蓝紫乱跳
    const colors = ['#24352e', '#6f8f7c', '#b4864a', '#ae5f49', '#7e8a9b', '#c7b49a', '#4f6b5f', '#d9c4a9']
    statByCategory('1').then(rows => {
      if (!rows || rows.length === 0) {
        this.setData({ hasRingData: false, ringSegments: '', ringItems: [] })
        return
      }
      const total = rows.reduce((s, r) => s + Number(r.totalAmount || 0), 0)
      let acc = 0
      const segments = rows.map((r, i) => {
        const pct = total > 0 ? (Number(r.totalAmount || 0) / total) * 100 : 0
        const seg = `${colors[i % colors.length]} ${acc.toFixed(1)}% ${(acc + pct).toFixed(1)}%`
        acc += pct
        return seg
      }).join(', ')
      this.setData({
        ringSegments: segments,
        ringTotal: total.toFixed(2),
        hasRingData: true,
        ringItems: rows.map((r, i) => ({
          categoryName: r.categoryName,
          totalAmount: Number(r.totalAmount || 0).toFixed(2),
          ratio: Number(r.ratio || 0).toFixed(1),
          color: colors[i % colors.length]
        }))
      })
    }).catch(() => {})
  },

  loadMore(done) {
    if (this.data.loading) {
      if (done) done()
      return
    }
    this.setData({ loading: true })
    const { pageNum, pageSize, filterType } = this.data
    const query = { pageNum, pageSize }
    if (filterType) query.recordType = filterType
    list(query)
      .then(res => {
        const rows = (res.rows || []).map(decorate)
        const records = pageNum === 1 ? rows : this.data.records.concat(rows)
        const total = res.total || 0
        this.setData({ records, total, loading: false, noMore: records.length >= total })
      })
      .catch(() => this.setData({ loading: false }))
      .finally(() => {
        if (done) done()
      })
  },

  /** 切换收支筛选 */
  onFilterChange(e) {
    const type = e.currentTarget.dataset.type
    this.setData({ filterType: type, pageNum: 1, records: [], noMore: false }, () => this.loadMore())
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
  // 记一笔
  // ----------------------------------------------------------------

  openForm() {
    this.setData({
      showForm: true,
      recordType: '1',
      amount: '',
      settledAmount: '',
      counterparty: '',
      taxAmount: '',
      tradeNo: '',
      invoiceNo: '',
      voucherUrl: '',
      remark: '',
      recordDate: todayStr(),
      projectIndex: -1,
      projectId: null,
      categoryIndex: -1,
      categoryId: null,
      payMethodIndex: 0
    })
    this.loadCategories('1')
  },

  closeForm() {
    this.setData({ showForm: false })
  },

  loadCategories(type) {
    categories(type).then(rows => {
      this.setData({
        categoryList: rows || [],
        categoryNames: (rows || []).map(item => item.categoryName)
      })
    })
  },

  onTypeChange(e) {
    const recordType = e.currentTarget.dataset.type
    this.setData({ recordType, categoryIndex: -1, categoryId: null }, () => this.loadCategories(recordType))
  },

  onProjectChange(e) {
    const index = Number(e.detail.value)
    const project = this.data.projects[index]
    this.setData({ projectIndex: index, projectId: project ? project.projectId : null })
  },

  onCategoryChange(e) {
    const index = Number(e.detail.value)
    const category = this.data.categoryList[index]
    this.setData({ categoryIndex: index, categoryId: category ? category.categoryId : null })
  },

  onPayMethodChange(e) {
    this.setData({ payMethodIndex: Number(e.detail.value) })
  },

  onDateChange(e) {
    this.setData({ recordDate: e.detail.value })
  },

  onAmountInput(e) {
    this.setData({ amount: e.detail.value })
  },

  onSettledInput(e) {
    this.setData({ settledAmount: e.detail.value })
  },

  onCounterpartyInput(e) {
    this.setData({ counterparty: e.detail.value })
  },

  /** 通用输入（税额/交易单号/发票号，靠 data-field 区分） */
  onInput(e) {
    const field = e.currentTarget.dataset.field
    if (field) {
      this.setData({ [field]: e.detail.value })
    }
  },

  /** 选择并上传支付凭证截图 */
  onChooseVoucher() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sizeType: ['compressed'],
      success: res => {
        const file = res.tempFiles && res.tempFiles[0]
        if (!file) return
        wx.showLoading({ title: '上传中', mask: true })
        require('../../utils/auth').uploadAvatar(file.tempFilePath)
          .then(result => {
            wx.hideLoading()
            if (result && result.ok) {
              this.setData({ voucherUrl: result.url })
              wx.showToast({ title: '凭证已上传', icon: 'success' })
            } else {
              wx.showModal({
                title: '凭证上传失败',
                content: (result && result.msg) || '未知原因',
                showCancel: false
              })
            }
          })
          .catch(() => wx.hideLoading())
      }
    })
  },

  onRemoveVoucher() {
    this.setData({ voucherUrl: '' })
  },

  onRemarkInput(e) {
    this.setData({ remark: e.detail.value })
  },

  submitRecord() {
    const amount = Number(this.data.amount)
    if (!amount || amount <= 0) {
      wx.showToast({ title: '请输入金额', icon: 'none' })
      return
    }
    if (Number(this.data.settledAmount || 0) > amount) {
      wx.showToast({ title: '已结算金额不能超过金额', icon: 'none' })
      return
    }
    if (this.data.submitting) return

    this.setData({ submitting: true })
    wx.showLoading({ title: '提交中', mask: true })
    add({
      projectId: this.data.projectId,
      categoryId: this.data.categoryId,
      recordType: this.data.recordType,
      amount: amount,
      recordDate: this.data.recordDate,
      payMethod: String(this.data.payMethodIndex),
      settledAmount: Number(this.data.settledAmount || 0),
      counterparty: this.data.counterparty,
      taxAmount: Number(this.data.taxAmount || 0),
      tradeNo: this.data.tradeNo,
      invoiceNo: this.data.invoiceNo,
      voucherUrl: this.data.voucherUrl,
      remark: this.data.remark
    })
      .then(() => {
        wx.hideLoading()
        wx.showToast({ title: '记账成功', icon: 'success' })
        this.setData({ showForm: false })
        this.reload()
      })
      .catch(() => wx.hideLoading())
      .finally(() => this.setData({ submitting: false }))
  }
})

/**
 * 账单行展示字段：类别首字做色块、金额千分位、结算金额格式化
 * 放在 JS 里算，WXML 只负责排版
 */
function decorate(row) {
  const isIncome = String(row.recordType) === '0'
  const categoryText = row.categoryName || (isIncome ? '收入' : '支出')
  return {
    ...row,
    isIncome,
    initial: categoryText.charAt(0),
    categoryText,
    amountText: money(row.amount),
    settledText: money(row.settledAmount || 0)
  }
}

