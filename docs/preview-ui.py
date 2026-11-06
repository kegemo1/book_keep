# -*- coding: utf-8 -*-
"""
把真实 WXSS 转成浏览器可看的静态预览（rpx 按 375px 视口折算 px）

用途：改版后先自查排版/配色，也方便直接发给非开发人员看效果。
注意：预览只做视觉近似，交互与真机差异以微信开发者工具为准。

用法：python preview-ui.py <小程序根目录> <输出 html> [只看第几屏，1 起，逗号分隔]
"""

import os
import re
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else "."
OUT = sys.argv[2] if len(sys.argv) > 2 else "ui-preview.html"
ONLY = sys.argv[3] if len(sys.argv) > 3 else ""
ZOOM = sys.argv[4] if len(sys.argv) > 4 else ""

CSS_FILES = [
    "app.wxss",
    "components/timer-card/timer-card.wxss",
    "pages/index/index.wxss",
    "pages/worklog/worklog.wxss",
    "pages/finance/finance.wxss",
    "pages/project/project.wxss",
    "pages/task/task.wxss",
    "pages/mine/mine.wxss",
]


def read(p):
    with open(p, encoding="utf-8") as f:
        return f.read()


def to_px(css):
    css = css.replace("env(safe-area-inset-bottom)", "0px")
    css = re.sub(r"(\d*\.?\d+)rpx", lambda m: "%gpx" % (float(m.group(1)) / 2), css)
    return css


def build_css():
    out = []
    for rel in CSS_FILES:
        p = os.path.join(ROOT, rel)
        if not os.path.exists(p):
            continue
        css = to_px(read(p))
        if rel == "app.wxss":
            # page 选择器在小程序里代表页面根节点，预览里换成手机框
            css = css.replace("page {", ".phone {", 1)
        out.append("/* %s */\n%s" % (rel, css))
    return "\n".join(out)


PHONES = []

TABS = [("home", "首页"), ("time", "工时"), ("wallet", "记账"), ("folder", "项目"), ("user", "我的")]


def tabbar(active):
    items = []
    for i, (icon, label) in enumerate(TABS):
        name = icon + ("-on" if i == active else "")
        items.append('<div class="tb-item%s"><img src="ui-assets/%s.png"><span>%s</span></div>'
                     % (" is-on" if i == active else "", name, label))
    return '<div class="tabbar">%s</div>' % "".join(items)


def phone(title, body, note="", active=0):
    PHONES.append('<div class="frame"><div class="phone"><div class="screen">%s</div>%s</div>'
                  '<div class="cap">%s%s</div></div>'
                  % (body, tabbar(active), title,
                     ('<span>%s</span>' % note) if note else ""))


# ---------------------------------------------------------------- 首页
INDEX = """
<div class="container">
  <div class="page-head">
    <div class="hello">晚上好</div>
    <div class="hello-sub">10月27日 周二 · 今天也好好记录</div>
  </div>

  <div class="timer-hero">
    <div class="tc">
      <div class="tc-head">
        <span class="tc-kicker">计时器</span>
        <div class="tc-state is-on"><div class="tc-dot"></div><span>进行中</span></div>
      </div>
      <div class="tc-time">01:24:36</div>
      <div class="tc-meta">
        <div class="tc-meta-row"><span class="tc-k">项目</span><span class="tc-v">企业官网改版</span></div>
        <div class="tc-meta-row"><span class="tc-k">任务</span><span class="tc-v">响应式前端开发</span></div>
        <div class="tc-meta-row"><span class="tc-k">开始</span><span class="tc-v">09:12</span></div>
      </div>
      <div class="tc-btns">
        <div class="tc-btn is-primary">结束并记录</div>
        <div class="tc-btn is-ghost">丢弃</div>
      </div>
    </div>
  </div>

  <div class="card">
    <div class="card-title"><span>工时</span><span class="more">明细 ›</span></div>
    <div class="stat-line">
      <div class="stat-main">
        <div class="stat-main-num figure">3.0<span class="stat-unit">h</span></div>
        <div class="label-mono">今日已完成</div>
      </div>
      <div class="stat-aside">
        <div class="aside-cell"><span class="aside-num figure">23.5</span><span class="aside-label">本周</span></div>
        <div class="aside-cell"><span class="aside-num figure">38.3</span><span class="aside-label">本月</span></div>
      </div>
    </div>
  </div>

  <div class="card">
    <div class="card-title"><span>本月收支</span><span class="more">去记账 ›</span></div>
    <div class="money-pair">
      <div class="money-cell is-in">
        <div class="money-cap"><div class="dot"></div><span>收入</span></div>
        <div class="money-num figure">80,700.00</div>
      </div>
      <div class="money-divider"></div>
      <div class="money-cell is-out">
        <div class="money-cap"><div class="dot"></div><span>支出</span></div>
        <div class="money-num figure">14,975.00</div>
      </div>
    </div>
    <div class="hairline"></div>
    <div class="row-between">
      <div class="net-box"><span class="label-mono">净额</span><span class="net-num figure">¥65,725.00</span></div>
      <div class="chip-gold"><span class="chip-gold-k">待收</span><span class="figure">¥22,000.00</span></div>
    </div>
  </div>

  <div class="card">
    <div class="card-title"><span>近 7 天工时</span><span class="chart-total figure">21.8 h</span></div>
    <svg width="100%" height="90" viewBox="0 0 320 90" preserveAspectRatio="none">
      <defs>
        <linearGradient id="g1" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="rgba(36,53,46,0.12)"/>
          <stop offset="100%" stop-color="rgba(36,53,46,0)"/>
        </linearGradient>
      </defs>
      <line x1="0" y1="10" x2="320" y2="10" stroke="#efece5"/>
      <line x1="0" y1="42" x2="320" y2="42" stroke="#efece5"/>
      <line x1="0" y1="74" x2="320" y2="74" stroke="#efece5"/>
      <path d="M8,44 L58,31 L108,38 L158,67 L208,74 L258,41 L310,47 L310,74 L8,74 Z" fill="url(#g1)"/>
      <path d="M8,44 L58,31 L108,38 L158,67 L208,74 L258,41 L310,47" fill="none" stroke="#24352e" stroke-width="1.6" stroke-linejoin="round" stroke-linecap="round"/>
      <g fill="#24352e">
        <circle cx="8" cy="44" r="2.4"/><circle cx="58" cy="31" r="2.4"/><circle cx="108" cy="38" r="2.4"/>
        <circle cx="158" cy="67" r="2.4"/><circle cx="208" cy="74" r="2.4"/><circle cx="258" cy="41" r="2.4"/>
        <circle cx="310" cy="47" r="2.4"/>
      </g>
    </svg>
    <div class="chart-x"><span>10-21</span><span>10-22</span><span>10-23</span><span>10-24</span><span>10-25</span><span>10-26</span><span>10-27</span></div>
  </div>
</div>
"""

# ---------------------------------------------------------------- 工时
WORKLOG = """
<div class="container">
  <div class="today-card">
    <div class="today-left">
      <div class="label-mono">今日已完成</div>
      <div class="today-num figure">3.0<span class="today-unit">h</span></div>
      <div class="today-meta">3 次打卡</div>
    </div>
    <div class="today-action"><span class="today-plus">＋</span><span>补录</span></div>
  </div>

  <div class="timer-hero">
    <div class="tc">
      <div class="tc-head">
        <span class="tc-kicker">计时器</span>
        <div class="tc-state"><div class="tc-dot"></div><span>待开始</span></div>
      </div>
      <div class="tc-time is-idle">00:00:00</div>
      <div class="tc-hint">还没有进行中的计时，选好项目即可开始</div>
    </div>
  </div>

  <div class="card">
    <div class="card-title"><span>打卡记录</span><span class="count-text">18 条</span></div>

    <div class="rec">
      <div class="rec-date"><span class="rec-day figure">10-28</span></div>
      <div class="rec-body">
        <div class="rec-line"><span class="rec-proj">会员小程序开发</span><span class="rec-hours figure">0.6<span class="rec-hours-unit">h</span></span></div>
        <div class="rec-sub">会员等级模块</div>
        <div class="rec-time">14:20 – 计时中</div>
        <div class="rec-foot">
          <div class="rec-meta">
            <span class="rec-src">计时器</span>
            <div class="rec-state is-running"><div class="rec-state-dot"></div><span>进行中</span></div>
          </div>
          <span class="rec-op is-in">结束并记录</span>
        </div>
        <div class="rec-note">这条计时一直没结束，结束后才计入统计</div>
      </div>
    </div>

    <div class="rec">
      <div class="rec-date"><span class="rec-day figure">10-27</span></div>
      <div class="rec-body">
        <div class="rec-line"><span class="rec-proj">企业官网改版</span><span class="rec-hours figure">1.5<span class="rec-hours-unit">h</span></span></div>
        <div class="rec-sub">响应式前端开发</div>
        <div class="rec-time">09:12 – 10:42</div>
        <div class="rec-content">首页骨架与栅格，先跑通 750 断点</div>
        <div class="rec-foot">
          <div class="rec-meta">
            <span class="rec-src">计时器</span>
            <div class="rec-state is-billable"><div class="rec-state-dot"></div><span>计费</span></div>
          </div>
          <span class="rec-op">作废</span>
        </div>
      </div>
    </div>

    <div class="rec">
      <div class="rec-date"><span class="rec-day figure">10-26</span></div>
      <div class="rec-body">
        <div class="rec-line"><span class="rec-proj">会员小程序开发</span><span class="rec-hours figure">2.5<span class="rec-hours-unit">h</span></span></div>
        <div class="rec-sub">会员等级模块</div>
        <div class="rec-time">手动补录</div>
        <div class="rec-foot">
          <div class="rec-meta">
            <span class="rec-src">补录</span>
            <div class="rec-state is-free"><div class="rec-state-dot"></div><span>不计费</span></div>
          </div>
          <span class="rec-op">作废</span>
        </div>
      </div>
    </div>

    <div class="rec is-void">
      <div class="rec-date"><span class="rec-day figure">10-24</span></div>
      <div class="rec-body">
        <div class="rec-line"><span class="rec-proj">经营数据看板</span><span class="rec-hours figure">1.0<span class="rec-hours-unit">h</span></span></div>
        <div class="rec-sub">经营指标口径梳理</div>
        <div class="rec-time">14:20 – 15:20</div>
        <div class="rec-foot">
          <div class="rec-meta">
            <span class="rec-src">补录</span>
            <div class="rec-state is-void"><div class="rec-state-dot"></div><span>已作废</span></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
"""

# ---------------------------------------------------------------- 记账
FINANCE = """
<div class="container">
  <div class="sum-card">
    <div class="row-between sum-head">
      <span class="label-mono">本月收支</span>
      <div class="chip-gold"><span class="chip-gold-k">待收</span><span class="figure">¥22,000.00</span></div>
    </div>
    <div class="money-pair sum-money">
      <div class="money-cell is-in">
        <div class="money-cap"><div class="dot"></div><span>收入</span></div>
        <div class="money-num figure">80,700.00</div>
      </div>
      <div class="money-divider"></div>
      <div class="money-cell is-out">
        <div class="money-cap"><div class="dot"></div><span>支出</span></div>
        <div class="money-num figure">14,975.00</div>
      </div>
    </div>
    <div class="add-btn"><span class="add-plus">＋</span><span>记一笔</span></div>
  </div>

  <div class="card">
    <div class="card-title">支出构成</div>
    <div class="ring-wrap">
      <div class="ring-chart" style="background: conic-gradient(#24352e 0 40.1%, #6f8f7c 40.1% 61.5%, #b4864a 61.5% 74.9%, #ae5f49 74.9% 88.1%, #7e8a9b 88.1% 93.3%, #c7b49a 93.3% 98.3%, #4f6b5f 98.3% 100%)">
        <div class="ring-center"><span class="ring-cny">¥</span><span class="ring-total figure">14,975</span></div>
      </div>
      <div class="ring-legend">
        <div class="legend-item"><div class="legend-dot" style="background:#24352e"></div><span class="legend-name">外包/人力</span><span class="legend-pct figure">40.1%</span></div>
        <div class="legend-item"><div class="legend-dot" style="background:#6f8f7c"></div><span class="legend-name">硬件设备</span><span class="legend-pct figure">21.4%</span></div>
        <div class="legend-item"><div class="legend-dot" style="background:#b4864a"></div><span class="legend-name">推广营销</span><span class="legend-pct figure">13.4%</span></div>
        <div class="legend-item"><div class="legend-dot" style="background:#ae5f49"></div><span class="legend-name">云服务/服务器</span><span class="legend-pct figure">13.2%</span></div>
        <div class="legend-item"><div class="legend-dot" style="background:#7e8a9b"></div><span class="legend-name">差旅交通</span><span class="legend-pct figure">5.2%</span></div>
        <div class="legend-item"><div class="legend-dot" style="background:#c7b49a"></div><span class="legend-name">软件与订阅</span><span class="legend-pct figure">5.0%</span></div>
      </div>
    </div>
  </div>

  <div class="seg">
    <div class="seg-item active">全部</div>
    <div class="seg-item">收入</div>
    <div class="seg-item">支出</div>
  </div>

  <div class="card">
    <div class="bill">
      <div class="bill-tile is-in">项</div>
      <div class="bill-body">
        <div class="bill-line"><span class="bill-cat">项目收款</span><span class="bill-amt is-in figure">+30,000.00</span></div>
        <div class="bill-sub">企业官网改版 · 杭州云图科技有限公司</div>
        <div class="bill-foot"><span class="bill-date figure">2026-10-18</span><span class="tag tag-gray">已结 15,000.00</span></div>
        <div class="bill-remark">一期进度款</div>
      </div>
    </div>

    <div class="bill">
      <div class="bill-tile is-out">外</div>
      <div class="bill-body">
        <div class="bill-line"><span class="bill-cat">外包/人力</span><span class="bill-amt is-out figure">−6,000.00</span></div>
        <div class="bill-sub">企业官网改版 · 自由插画师·小林</div>
        <div class="bill-foot"><span class="bill-date figure">2026-10-10</span><span class="tag tag-gray">已结 3,000.00</span></div>
      </div>
    </div>

    <div class="bill">
      <div class="bill-tile is-out">云</div>
      <div class="bill-body">
        <div class="bill-line"><span class="bill-cat">云服务/服务器</span><span class="bill-amt is-out figure">−1,299.00</span></div>
        <div class="bill-sub">企业官网改版 · 阿里云</div>
        <div class="bill-foot"><span class="bill-date figure">2026-10-03</span></div>
      </div>
    </div>
  </div>
</div>
"""

# ---------------------------------------------------------------- 项目
PROJECT = """
<div class="container">
  <div class="summary">
    <div class="row-between">
      <span class="label-mono">项目总览</span>
      <span class="summary-count">3 个在管</span>
    </div>
    <div class="summary-net"><span class="net-k">净利润</span><span class="net-v figure">¥100,203.00</span></div>
    <div class="summary-pair">
      <div class="pair-cell">
        <div class="pair-cap"><div class="dot"></div><span>收入</span></div>
        <span class="pair-v money-in figure">¥79,500.00</span>
      </div>
      <div class="money-divider"></div>
      <div class="pair-cell">
        <div class="pair-cap is-out"><div class="dot"></div><span>成本</span></div>
        <span class="pair-v money-out figure">¥14,547.00</span>
      </div>
    </div>
  </div>

  <div class="proj">
    <div class="proj-head">
      <div class="proj-titles">
        <div class="proj-name">企业官网改版</div>
        <div class="proj-sub"><span>杭州云图科技有限公司</span><span class="proj-code">P2026-001</span></div>
      </div>
      <div class="status-pill is-on">进行中</div>
    </div>

    <div class="proj-meta"><span>预算 ¥80,000.00</span><span>时薪 ¥180/h</span></div>

    <div class="bar-chart">
      <div class="bar-row"><span class="bar-k">收</span><div class="bar-track"><div class="bar-fill is-in" style="width:100%"></div></div><span class="bar-amt is-in figure">54,000.00</span></div>
      <div class="bar-row"><span class="bar-k">支</span><div class="bar-track"><div class="bar-fill is-out" style="width:23%"></div></div><span class="bar-amt is-out figure">12,499.00</span></div>
    </div>

    <div class="metrics">
      <div class="metric"><span class="m-k">收入</span><span class="m-v money-in figure">54,000.00</span></div>
      <div class="metric"><span class="m-k">直接成本</span><span class="m-v money-out figure">12,499.00</span></div>
      <div class="metric"><span class="m-k">毛利</span><span class="m-v figure">41,501.00</span></div>
      <div class="metric"><span class="m-k">工时</span><span class="m-v figure">22.5<span class="m-u">h</span></span></div>
      <div class="metric"><span class="m-k">人力成本</span><span class="m-v figure">4,050.00</span></div>
      <div class="metric"><span class="m-k">净利润</span><span class="m-v money-in figure">37,451.00</span></div>
    </div>

    <div class="budget">
      <div class="row-between budget-head"><span class="budget-k">预算使用 27.4%</span></div>
      <div class="budget-track"><div class="budget-fill" style="width:27.4%"></div></div>
    </div>

    <div class="proj-foot"><span class="foot-rate">毛利率 76.9% · 净利率 69.4%</span><span class="foot-receivable">应收 15,000.00</span></div>

    <div class="proj-ops"><span class="op">任务</span><span class="op">编辑</span><span class="op is-danger">删除</span></div>
  </div>
</div>
"""

# ---------------------------------------------------------------- 任务
TASK = """
<div class="container">
  <div class="task">
    <div class="task-strip is-high"></div>
    <div class="task-main">
      <div class="task-head">
        <span class="task-name">首页视觉稿设计</span>
        <div class="task-status is-done">已完成</div>
      </div>
      <div class="task-meta">
        <span class="prio is-high">高优先</span>
        <span class="meta-k">预估 <span class="figure">16</span>h</span>
        <span class="meta-k">实际 <span class="figure">14.5</span>h</span>
      </div>
      <div class="progress"><div class="track"><div class="fill is-done" style="width:100%"></div></div><span class="pct figure">100%</span></div>
      <div class="task-remark">配色定稿，交付给前端切图</div>
      <div class="task-ops"><span class="op">重开</span><span class="op">编辑</span><span class="op is-danger">删除</span></div>
    </div>
  </div>

  <div class="task">
    <div class="task-strip is-mid"></div>
    <div class="task-main">
      <div class="task-head">
        <span class="task-name">响应式前端开发</span>
        <div class="task-status is-doing">进行中</div>
      </div>
      <div class="task-meta">
        <span class="prio is-mid">中优先</span>
        <span class="meta-k">预估 <span class="figure">40</span>h</span>
        <span class="meta-k">实际 <span class="figure">22</span>h</span>
      </div>
      <div class="progress"><div class="track"><div class="fill" style="width:60%"></div></div><span class="pct figure">60%</span></div>
      <div class="task-ops"><span class="op">完成</span><span class="op">编辑</span><span class="op is-danger">删除</span></div>
    </div>
  </div>

  <div class="task">
    <div class="task-strip is-low"></div>
    <div class="task-main">
      <div class="task-head">
        <span class="task-name">后台内容管理对接</span>
        <div class="task-status is-todo">待处理</div>
      </div>
      <div class="task-meta">
        <span class="prio">低优先</span>
        <span class="meta-k">预估 <span class="figure">24</span>h</span>
        <span class="meta-k">实际 <span class="figure">0</span>h</span>
      </div>
      <div class="progress"><div class="track"><div class="fill" style="width:0%"></div></div><span class="pct figure">0%</span></div>
      <div class="task-ops"><span class="op">完成</span><span class="op">编辑</span><span class="op is-danger">删除</span></div>
    </div>
  </div>
</div>
"""

# ---------------------------------------------------------------- 我的
MINE = """
<div class="container">
  <div class="profile">
    <div class="profile-main">
      <div class="avatar-wrap">
        <div class="avatar-inner">
          <div class="avatar" style="background:linear-gradient(140deg,#cfd8cf,#8fa396)"></div>
        </div>
        <button class="avatar-hit" open-type="chooseAvatar"></button>
        <div class="avatar-badge">改</div>
      </div>
      <div class="profile-info">
        <div class="nick-row"><span class="nick-name">微信用户77493</span><span class="edit-icon">编辑</span></div>
        <div class="profile-sub">2026-10-27 加入</div>
      </div>
    </div>
    <div class="profile-stats">
      <div class="ps-cell"><span class="ps-num figure">38.3</span><span class="ps-label">累计工时</span></div>
      <div class="ps-cell"><span class="ps-num figure">18</span><span class="ps-label">打卡次数</span></div>
      <div class="ps-cell"><span class="ps-num figure">3</span><span class="ps-label">进行中项目</span></div>
    </div>
  </div>

  <div class="card">
    <div class="card-title"><span>本月</span><span class="text-mute">38.3 小时</span></div>
    <div class="month-row">
      <div class="month-cell">
        <div class="money-cap"><div class="dot"></div><span>收入</span></div>
        <div class="month-num money-in figure">80,700.00</div>
      </div>
      <div class="money-divider"></div>
      <div class="month-cell">
        <div class="money-cap is-out"><div class="dot"></div><span>支出</span></div>
        <div class="month-num money-out figure">14,975.00</div>
      </div>
    </div>
  </div>

  <div class="card">
    <div class="card-title">账号</div>
    <div class="list-item row-between"><span class="text-sub">身份类型</span><span class="tag tag-blue">微信独立账号</span></div>
    <div class="list-item row-between"><span class="text-sub">账号状态</span><span class="tag tag-green">正常</span></div>
    <div class="list-item row-between"><span class="text-sub">最近登录</span><span class="text-mute figure">2026-10-27 21:01</span></div>
  </div>

  <div class="card">
    <div class="menu-item"><div class="glyph is-pine">导</div><span class="menu-text">数据导出</span><div class="arrow"></div></div>
    <div class="menu-item"><div class="glyph is-clay">销</div><span class="menu-text">账号注销</span><div class="arrow"></div></div>
    <div class="menu-item"><div class="glyph">退</div><span class="menu-text">退出登录</span><div class="arrow"></div></div>
  </div>
</div>
"""

phone("首页", INDEX, "", 0)
phone("工时", WORKLOG, "", 1)
phone("记账", FINANCE, "", 2)
phone("项目", PROJECT, "含支出分类环形图", 3)
phone("任务", TASK, "从项目卡进入", 3)
phone("我的", MINE, "", 4)

# 同一页的极端态：没设头像 + 超长昵称，用来验证首字兜底与「编辑」不竖排
MINE_EDGE = MINE.replace(
    '<div class="avatar" style="background:linear-gradient(140deg,#cfd8cf,#8fa396)"></div>',
    '<div class="avatar-fallback">微</div>'
).replace("微信用户77493", "微信用户7749312456789")
phone("我的（未设头像）", MINE_EDGE, "昵称首字兜底 · 长昵称省略", 4)

# 编辑昵称态：验证输入框与保存/取消的间距、热区高度
MINE_EDIT = MINE.replace(
    """        <div class="nick-row"><span class="nick-name">微信用户77493</span><span class="edit-icon">编辑</span></div>
        <div class="profile-sub">2026-10-27 加入</div>""",
    """        <input class="nick-input" value="微信用户77493">
        <div class="edit-actions"><div class="edit-btn is-save">保存</div><div class="edit-btn is-cancel">取消</div></div>"""
)
phone("我的（编辑昵称）", MINE_EDIT, "保存/取消为独立热区", 4)

# 只渲染指定屏：方便单屏放大截图逐像素核对（1 起编号）
if ONLY:
    picked = set()
    for token in ONLY.replace("，", ",").split(","):
        token = token.strip()
        if token.isdigit():
            picked.add(int(token))
    shown = [h for i, h in enumerate(PHONES, 1) if i in picked]
    if not shown:
        shown = PHONES
else:
    shown = PHONES

EXTRA = """
/* 预览页面自身的外框，不参与小程序样式 */
body { margin: 0; background: #e9e5dd; font-family: -apple-system, "PingFang SC", sans-serif; }
.stage { display: flex; gap: 28px; padding: 36px 36px 60px; align-items: flex-start; overflow-x: auto; }
.frame { flex: 0 0 auto; }
.phone {
  position: relative;
  width: 375px; height: 780px; display: flex; flex-direction: column;
  border-radius: 26px; overflow: hidden;
  box-shadow: 0 24px 50px -22px rgba(28,33,31,.45), 0 0 0 1px rgba(28,33,31,.06);
  background: #f6f4ef;
}
.screen { position: relative; flex: 1; overflow-y: auto; overflow-x: hidden; }
.phone .container { min-height: 0 !important; padding-bottom: 28px !important; }
.phone .fab { position: absolute; bottom: 24px; }
.tabbar {
  flex-shrink: 0; height: 58px; display: flex; align-items: center;
  background: #fff; border-top: 1px solid #f1ede5; padding-bottom: 4px;
}
.tb-item { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; color: #a3a69f; font-size: 10px; letter-spacing: .5px; }
.tb-item img { width: 23px; height: 23px; display: block; }
.tb-item.is-on { color: #24352e; }
.cap { margin-top: 14px; font-size: 13px; color: #6b6e68; letter-spacing: .5px; }
.cap span { color: #a3a69f; margin-left: 8px; font-size: 12px; }
.chart-x { display: flex; justify-content: space-between; padding: 8px 4px 0; font-size: 9px; color: #a3a69f; }
h1 { font-size: 15px; font-weight: 600; color: #1c211f; margin: 0 0 4px 36px; letter-spacing: .5px; }
"""

# 放大自查：局部细节（头像环、徽标、文字基线）看不准时用
if ZOOM:
    EXTRA += "\nbody { zoom: %s; }\n" % ZOOM

# 展开整屏：手机框不再固定 780px 高、内部不再滚动，
# 长页面（工时记录列表等）能一次性截全，不用手动滚
if os.environ.get("PREVIEW_FULL"):
    EXTRA += """
.phone { height: auto !important; }
.screen { overflow: visible !important; }
"""

# 裸机模式：只渲染手机本体，不带标题/说明文字/外框留白，
# 供小红书图文等外部场景当「手机截图」素材用（配 3 倍缩放出高清图）
if os.environ.get("PREVIEW_BARE"):
    EXTRA += """
body { background: #f6f4ef !important; }
h1, .cap { display: none !important; }
.stage { padding: 0 !important; gap: 0 !important; }
.phone { height: auto !important; box-shadow: none !important; border-radius: 0 !important; }
.screen { overflow: visible !important; }
"""

# 注入微信内置的 button 默认样式：浏览器预览没有这套默认值，
# 不注入就无法复现「button 被撑大 / 位置跑掉」这类真机问题。
# 用法：PREVIEW_WX_BUTTON=1 python preview-ui.py ...
if os.environ.get("PREVIEW_WX_BUTTON"):
    EXTRA += """
/* 微信内置 button 默认样式（基础库 wxss 摘录） */
button {
  position: relative; display: block;
  margin-left: auto; margin-right: auto;
  padding-left: 14px; padding-right: 14px;
  box-sizing: border-box; font-size: 18px; text-align: center;
  text-decoration: none; line-height: 2.55555556; border-radius: 5px;
  -webkit-tap-highlight-color: transparent; overflow: hidden;
  color: #000; background-color: #f8f8f8;
}
button::after {
  content: " "; width: 200%; height: 200%; position: absolute;
  top: -50%; left: -50%; border: 1px solid rgba(0,0,0,.2);
  transform: scale(.5); transform-origin: 0 0; box-sizing: border-box;
  border-radius: 10px;
}
/* 预览里把按钮画出来，方便看它到底占了多大 */
.avatar-hit { outline: 2px dashed #d33 !important; opacity: .25 !important; background: rgba(221,51,51,.25) !important; }

/* 微信 input 自带 1.4rem 高度，字号一大就会被边线拦腰截断，
   注入后才能在预览里复现。只设 height，让 app.wxss 里的 min-height 兜底生效 */
input { height: 1.4rem; }
"""

html = """<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<title>账时记 · 界面预览</title>
<style>
%s
%s
</style>
</head>
<body>
<h1>账时记 · 界面预览</h1>
<div class="stage">
%s
</div>
</body>
</html>
""" % (build_css(), EXTRA, "\n".join(shown))

PHONES_RESULT = PHONES

with open(OUT, "w", encoding="utf-8") as f:
    f.write(html)

# 图标拷到预览同目录，避免跨盘 file:// 引用失败
import shutil
icon_dir = os.path.join(os.path.dirname(os.path.abspath(OUT)), "ui-assets")
os.makedirs(icon_dir, exist_ok=True)
src_icon_dir = os.path.join(ROOT, "assets", "tabbar")
if os.path.isdir(src_icon_dir):
    for fn in os.listdir(src_icon_dir):
        if fn.endswith(".png"):
            shutil.copy(os.path.join(src_icon_dir, fn), os.path.join(icon_dir, fn))

print("已生成 %s" % OUT)
