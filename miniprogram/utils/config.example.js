// 注意：复制为 config.js 再填你自己的域名（真实 config.js 已被 .gitignore 排除）
// ============================================================
// 环境配置 —— 发布上线只改本文件
// ============================================================
// 1. USE_DEV 改为 false，并把 ENV.prod 换成你的正式域名
// 2. 域名要求：已 ICP 备案 + HTTPS，且已在微信公众平台登记
// 3. DEBUG 控制调试日志输出，发布时设为 false
const ENV = {
  // 本地联调：开发者工具勾选「不校验合法域名」后可直连后端
  dev: 'http://127.0.0.1:9066',
  // 生产环境：填你的备案域名（HTTPS）
  prod: 'https://your-domain.com'
}

// 上线前改为 false
const USE_DEV = false
// 调试日志开关（发布版建议 false）
const DEBUG = false

module.exports = {
  baseUrl: USE_DEV ? ENV.dev : ENV.prod,
  DEBUG,
  // 单次计时上限（秒），与后端 BizConstants.MAX_TIMER_DURATION_SECONDS 保持一致
  maxTimerSeconds: 12 * 3600,
  // 手动补录单次上限（秒）
  maxManualSeconds: 24 * 3600,
  // 允许补录的回溯天数
  backfillMaxDays: 31
}
