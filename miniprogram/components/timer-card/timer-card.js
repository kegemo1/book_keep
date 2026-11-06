const { formatDuration } = require('../../utils/format')
const { maxTimerSeconds } = require('../../utils/config')

Component({
  properties: {
    // 进行中的计时器对象，无则传 null
    running: {
      type: Object,
      value: null
    }
  },

  data: {
    display: '00:00:00',
    overtime: false
  },

  lifetimes: {
    detached() {
      this.stopTick()
    }
  },

  observers: {
    'running': function (running) {
      this.stopTick()
      // 必须同时校验 logId：空对象 {} 是 truthy，只判 running 会误渲染成「进行中」
      if (running && running.logId && running.runningSeconds != null) {
        // 以服务端返回的秒数为基准，本地只做递增展示
        this._base = Number(running.runningSeconds) || 0
        this._startAt = Date.now()
        this.update()
        this.startTick()
      } else {
        this._base = 0
        this.setData({ display: '00:00:00', overtime: false })
      }
    }
  },

  methods: {
    startTick() {
      this._timer = setInterval(() => this.update(), 1000)
    },

    stopTick() {
      if (this._timer) {
        clearInterval(this._timer)
        this._timer = null
      }
    },

    update() {
      const seconds = this._base + Math.floor((Date.now() - this._startAt) / 1000)
      // 每秒只更新两个字段，避免大对象 setData 跨线程开销
      this.setData({
        display: formatDuration(seconds),
        overtime: seconds > maxTimerSeconds
      })
    },

    onStop() {
      this.triggerEvent('stop', { discard: false })
    },

    onDiscard() {
      this.triggerEvent('stop', { discard: true })
    }
  }
})
