Component({
  properties: {
    labels: { type: Array, value: [] },
    values: { type: Array, value: [] },
    height: { type: Number, value: 180 }
  },
  observers: {
    'labels, values': function () { this.draw() }
  },
  lifetimes: {
    ready() { this.draw() }
  },
  methods: {
    draw() {
      const labels = this.data.labels || []
      const values = (this.data.values || []).map(Number)
      if (values.length === 0) return
      const query = this.createSelectorQuery()
      query.select('#lc')
        .fields({ node: true, size: true })
        .exec(res => {
          if (!res[0] || !res[0].node) return
          const canvas = res[0].node
          const ctx = canvas.getContext('2d')
          const dpr = wx.getSystemInfoSync().pixelRatio
          const w = res[0].width
          const h = res[0].height
          canvas.width = w * dpr
          canvas.height = h * dpr
          ctx.scale(dpr, dpr)

          const pad = { l: 14, r: 12, t: 20, b: 24 }
          const cw = w - pad.l - pad.r
          const ch = h - pad.t - pad.b
          const max = Math.max.apply(null, values.concat([1]))
          const step = values.length > 1 ? cw / (values.length - 1) : 0
          const baseY = pad.t + ch

          const pts = values.map((v, i) => ({
            x: pad.l + step * i,
            y: baseY - (v / max) * ch * 0.94
          }))

          // 横向发丝线：只用极浅的暖灰，不抢数据
          ctx.strokeStyle = '#efece5'
          ctx.lineWidth = 1
          for (let i = 0; i <= 2; i++) {
            const y = pad.t + (ch / 2) * i
            ctx.beginPath()
            ctx.moveTo(pad.l, y)
            ctx.lineTo(pad.l + cw, y)
            ctx.stroke()
          }

          // 平滑路径：用中点做控制点，避免折线太"硬"
          const trace = () => {
            ctx.beginPath()
            ctx.moveTo(pts[0].x, pts[0].y)
            for (let i = 1; i < pts.length; i++) {
              const p0 = pts[i - 1]
              const p1 = pts[i]
              const mx = (p0.x + p1.x) / 2
              ctx.bezierCurveTo(mx, p0.y, mx, p1.y, p1.x, p1.y)
            }
          }

          // 面积渐变
          trace()
          ctx.lineTo(pts[pts.length - 1].x, baseY)
          ctx.lineTo(pts[0].x, baseY)
          ctx.closePath()
          const grad = ctx.createLinearGradient(0, pad.t, 0, baseY)
          grad.addColorStop(0, 'rgba(36,53,46,0.12)')
          grad.addColorStop(1, 'rgba(36,53,46,0)')
          ctx.fillStyle = grad
          ctx.fill()

          // 描线
          trace()
          ctx.strokeStyle = '#24352e'
          ctx.lineWidth = 2.5
          ctx.lineJoin = 'round'
          ctx.lineCap = 'round'
          ctx.stroke()

          // 数据点：白色外圈垫底，小圆点更精致
          pts.forEach(p => {
            ctx.beginPath()
            ctx.arc(p.x, p.y, 5, 0, 2 * Math.PI)
            ctx.fillStyle = '#ffffff'
            ctx.fill()
            ctx.beginPath()
            ctx.arc(p.x, p.y, 3, 0, 2 * Math.PI)
            ctx.fillStyle = '#24352e'
            ctx.fill()
          })

          // x 轴标签
          ctx.fillStyle = '#a3a69f'
          ctx.font = '10px -apple-system, sans-serif'
          ctx.textAlign = 'center'
          const every = Math.ceil(values.length / 7)
          labels.forEach((label, i) => {
            if (i % every === 0 || i === values.length - 1) {
              ctx.fillText(label, pts[i].x, baseY + 17)
            }
          })
        })
    }
  }
})
