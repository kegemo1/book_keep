# -*- coding: utf-8 -*-
"""
账时记 · 演示数据生成脚本

用途：为指定微信用户灌入一批"看起来像真人在用"的测试数据，用于验收图表可视化
（首页近 7 天工时折线图、记账页支出类别环形图、项目页收入/成本对比条形图）。

数据分布刻意对齐后端统计口径：
  - 首页折线图：/wx/project/dashboard → 近 7 天（today-6 ~ today）按 log_date 聚合，只算 log_status='1'
  - 记账环形图：/wx/finance/stat/category?recordType=1 → 不限日期，按 category_id 聚合
  - 项目条形图：/wx/project/profit → 按 owner_id 过滤，收入/成本/工时全时段聚合

用法：
  python seed-demo-data.py            # 默认 uid=9000000017，导入本机 book_keep
  python seed-demo-data.py 9000000018 # 指定用户
同时把等价 SQL 输出到 docs/seed-demo-data.sql，可直接在 Navicat / 服务器 mysql 客户端执行。

主键规则：@base := @uid * 1000，所有演示数据落在 [@base+1, @base+300]，
远超各表自增值，因此同一份 SQL 在开发库和生产库都不会与既有数据撞主键。
脚本先删该用户的旧演示数据再插入，可重复执行。
"""

import os
import sys
from datetime import date, datetime, timedelta

import pymysql

# ---------------- 连接配置（本机开发库） ----------------
DB = dict(host="127.0.0.1", port=3306, user="root", password=os.environ.get("DB_PASSWORD", ""),  # 从环境变量读取，避免硬编码,
          database="book_keep", charset="utf8mb4")

UID = int(sys.argv[1]) if len(sys.argv) > 1 else 9000000017
TODAY = date.today()


def d(offset):
    """相对今天的日期偏移"""
    return TODAY + timedelta(days=offset)


def dt(day, hh, mm=0):
    return datetime(day.year, day.month, day.day, hh, mm, 0)


# 主键表达式（SQL 里由 @base 计算，避免写死数字撞主键）
def pid(i):
    return "(@base + %d)" % (i + 1)


def tid(i):
    return "(@base + %d)" % (i + 101)


def lid(i):
    return "(@base + %d)" % (i + 201)


def rid(i):
    return "(@base + %d)" % (i + 301)


# ---------------- 项目 ----------------
# (code, name, customer, budget, hour_rate, start, plan_end)
PROJECTS = [
    ("P2026-001", "企业官网改版", "杭州云图科技有限公司", 80000, 180, d(-39), d(34)),
    ("P2026-002", "会员小程序开发", "宁波优选商贸有限公司", 45000, 150, d(-23), d(49)),
    ("P2026-003", "经营数据看板", "苏州智造精密机械", 30000, 200, d(-17), d(24)),
]

# ---------------- 任务 ----------------
# (项目序号, 名称, 状态, 优先级, 预估工时, 实际工时, 进度)
TASKS = [
    (0, "首页视觉稿设计", "2", "1", 16, 14.5, 100),
    (0, "响应式前端开发", "1", "0", 40, 22.0, 60),
    (0, "后台内容管理对接", "0", "2", 24, 0.0, 0),
    (1, "需求评审与原型", "2", "1", 12, 11.0, 100),
    (1, "会员等级模块", "1", "1", 32, 15.5, 45),
    (2, "数据源接入", "1", "0", 20, 8.0, 30),
    (2, "图表组件开发", "0", "2", 18, 0.0, 0),
]

# ---------------- 工时 ----------------
# (相对天数, 项目序号, 任务序号, 秒数, 类型0计时1补录, 起始小时|None)
# 近 7 天是折线图的数据源，今天必须有记录；-2 天故意留空，验证补零逻辑
LOGS = [
    # —— 近 7 天（首页折线图）——
    (-6, 0, 1, 7200, "0", 9),
    (-6, 1, 4, 5400, "1", None),
    (-5, 0, 1, 10800, "0", 9),
    (-5, 2, 5, 7200, "1", None),
    (-4, 0, 0, 14400, "0", 8),
    (-3, 1, 4, 3600, "1", None),
    (-1, 1, 4, 9000, "0", 9),
    (-1, 2, 5, 6300, "1", None),
    (0, 0, 1, 5400, "0", 9),
    (0, 1, 4, 3600, "1", None),
    (0, 2, 5, 1800, "1", None),
    # —— 本月早些时候（只影响本月工时/项目工时，不进折线图）——
    (-19, 0, 0, 10800, "1", None),
    (-18, 0, 0, 12600, "0", 9),
    (-15, 1, 3, 7200, "1", None),
    (-12, 0, 1, 9000, "0", 10),
    (-11, 2, 5, 5400, "1", None),
    (-8, 0, 1, 10800, "0", 9),
    (-7, 1, 4, 7200, "1", None),
]

# ---------------- 收支 ----------------
# (相对天数, 类型0收入1支出, 项目序号|None, 类别ID, 金额, 税额, 支付方式, 结算状态, 已结算, 对方, 备注)
RECORDS = [
    # 收入
    (-25, "0", 0, 101, 24000, 0, "2", "0", 24000, "杭州云图科技有限公司", "项目预付款 30%"),
    (-9, "0", 0, 100, 30000, 1698.11, "2", "1", 15000, "杭州云图科技有限公司", "一期进度款"),
    (-7, "0", 1, 101, 13500, 0, "0", "0", 13500, "宁波优选商贸有限公司", "小程序预付款"),
    (-3, "0", 2, 100, 12000, 679.25, "2", "1", 5000, "苏州智造精密机械", "看板首期款"),
    (-1, "0", None, 103, 1133, 67.21, "0", "0", 1133, "个人", "技术咨询答疑"),
    # 支出（7 个类别都有金额，环形图才有层次）
    (-24, "1", 0, 105, 1299, 0, "0", "0", 1299, "阿里云", "云服务器年付"),
    (-20, "1", 0, 104, 3200, 368.14, "2", "0", 3200, "京东", "4K 显示器"),
    (-17, "1", 0, 107, 6000, 0, "2", "1", 3000, "自由插画师·小林", "首页插画外包"),
    (-14, "1", 1, 106, 588, 0, "0", "0", 588, "Figma", "设计工具年费"),
    (-11, "1", 1, 105, 680, 0, "0", "0", 680, "腾讯云", "测试环境服务器"),
    (-8, "1", 2, 108, 460, 0, "1", "0", 460, "12306", "客户现场差旅"),
    (-6, "1", 0, 109, 2000, 113.21, "0", "0", 2000, "微信广告", "朋友圈推广"),
    (-4, "1", None, 106, 168, 0, "0", "0", 168, "七牛云", "图床会员续费"),
    (-2, "1", 2, 108, 320, 0, "1", "0", 320, "滴滴企业版", "往返客户现场"),
    (0, "1", None, 110, 260, 0, "0", "0", 260, "文具店", "打印耗材"),
]


def build_sql():
    """生成可移植 SQL（开发库 / 生产库通用）"""
    q = []
    q.append("-- 账时记 演示数据 uid=%d  生成时间 %s" % (UID, datetime.now().strftime("%Y-%m-%d %H:%M:%S")))
    q.append("-- 可重复执行：先清理该用户的演示数据，再重新写入")
    q.append("-- 主键由 @base 派生（uid*1000），不会与各表自增主键冲突")
    q.append("SET @uid := %d;" % UID)
    q.append("SET @by := CONCAT('wx_', @uid);")
    q.append("SET @base := @uid * 1000;")
    q.append("")
    q.append("DELETE FROM biz_finance_record WHERE user_id = @uid;")
    q.append("DELETE FROM biz_work_log WHERE user_id = @uid;")
    q.append("DELETE FROM biz_task WHERE assignee_id = @uid;")
    q.append("DELETE FROM biz_project WHERE owner_id = @uid;")
    q.append("")

    q.append("-- 1) 项目（owner_id = 当前用户，状态进行中）")
    for i, (code, name, cust, budget, rate, start, plan) in enumerate(PROJECTS):
        q.append(
            "INSERT INTO biz_project (project_id, project_code, project_name, owner_id, project_type, "
            "customer_name, status, start_date, plan_end_date, budget_amount, hour_rate, order_num, "
            "del_flag, create_by, create_time, remark) VALUES "
            "(%s, '%s', '%s', @uid, '1', '%s', '0', '%s', '%s', %d.00, %d.00, 0, '0', @by, NOW(), '');"
            % (pid(i), code, name, cust, start, plan, budget, rate)
        )
    q.append("")

    q.append("-- 2) 任务（assignee_id = 当前用户，保证首页待办数不为 0）")
    for i, (pidx, name, st, pri, est, act, prog) in enumerate(TASKS):
        q.append(
            "INSERT INTO biz_task (task_id, project_id, parent_id, ancestors, task_name, assignee_id, "
            "priority, status, plan_start_time, plan_end_time, estimate_hours, actual_hours, progress, "
            "order_num, del_flag, create_by, create_time) VALUES "
            "(%s, %s, 0, '0', '%s', @uid, '%s', '%s', NULL, NULL, %.2f, %.2f, %d, 0, '0', @by, NOW());"
            % (tid(i), pid(pidx), name, pri, st, est, act, prog)
        )
    q.append("")

    q.append("-- 3) 工时打卡（近 7 天构成折线图；log_status='1' 只算已完成）")
    for i, (off, pidx, tidx, secs, ltype, hour) in enumerate(LOGS):
        day = d(off)
        if hour is None:
            st, et, content = "NULL", "NULL", "手动补录"
        else:
            st = "'%s'" % dt(day, hour)
            et = "'%s'" % (dt(day, hour) + timedelta(seconds=secs))
            content = "计时打卡"
        billable = "1" if i % 7 == 3 else "0"   # 少数不计费，计费工时口径才有差异
        q.append(
            "INSERT INTO biz_work_log (log_id, user_id, project_id, task_id, log_date, start_time, end_time, "
            "duration, log_type, log_status, billable, content, source, del_flag, create_by, create_time) VALUES "
            "(%s, @uid, %s, %s, '%s', %s, %s, %d, '%s', '1', '%s', '%s', '1', '0', @by, '%s');"
            % (lid(i), pid(pidx), tid(tidx), day, st, et, secs, ltype, billable, content, dt(day, 21))
        )
    q.append("")

    q.append("-- 4) 收支流水（收入撑起项目盈利，支出覆盖 7 个类别撑起环形图）")
    for i, (off, rtype, pidx, cat, amt, tax, pay, settle, settled, party, note) in enumerate(RECORDS):
        day = d(off)
        no = "BK%s%04d" % (day.strftime("%Y%m%d"), i + 1)
        q.append(
            "INSERT INTO biz_finance_record (record_id, record_no, user_id, project_id, category_id, record_type, "
            "amount, tax_amount, currency, record_date, pay_method, settle_status, settled_amount, counterparty, "
            "source, del_flag, create_by, create_time, remark) VALUES "
            "(%s, '%s', @uid, %s, %d, '%s', %.2f, %.2f, 'CNY', '%s', '%s', '%s', %.2f, '%s', '1', '0', @by, NOW(), '%s');"
            % (rid(i), no, "NULL" if pidx is None else pid(pidx), cat, rtype, amt, tax, day, pay, settle,
               settled, party, note)
        )
    q.append("")
    return q


def main():
    lines = build_sql()
    sql_text = "\n".join(lines)

    out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "seed-demo-data.sql")
    with open(out, "w", encoding="utf-8") as f:
        f.write(sql_text + "\n")
    print("SQL 已生成：%s" % out)

    conn = pymysql.connect(**DB)
    conn.autocommit(False)
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT wx_user_id, nick_name FROM biz_wx_user WHERE wx_user_id=%s", (UID,))
            row = cur.fetchone()
            if not row:
                raise SystemExit("用户 %d 不存在于 biz_wx_user，请先在开发者工具登录一次" % UID)
            print("目标用户：%s %s" % row)

            # 会话变量（后续 INSERT 用 @uid/@by/@base 引用）
            cur.execute("SET @uid := %s", (UID,))
            cur.execute("SET @by := CONCAT('wx_', @uid)")
            cur.execute("SET @base := @uid * 1000")
            for t, col in (("biz_finance_record", "user_id"), ("biz_work_log", "user_id"),
                           ("biz_task", "assignee_id"), ("biz_project", "owner_id")):
                cur.execute("DELETE FROM %s WHERE %s = %s" % (t, col, UID))
            for stmt in lines:
                s = stmt.strip()
                if not s or s.startswith("--") or s.upper().startswith(("SET ", "DELETE ")):
                    continue
                cur.execute(s)
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()

    # ---------------- 校验：按三个图表接口的口径重算 ----------------
    conn = pymysql.connect(**DB, cursorclass=pymysql.cursors.DictCursor)
    cur = conn.cursor()
    print("\n===== 校验（模拟三个图表接口）=====")

    cur.execute("""SELECT log_date, ROUND(SUM(duration)/3600, 2) AS h, COUNT(*) c
                   FROM biz_work_log WHERE user_id=%s AND del_flag='0' AND log_status='1'
                     AND log_date BETWEEN %s AND %s
                   GROUP BY log_date ORDER BY log_date""",
                (UID, TODAY - timedelta(days=6), TODAY))
    print("[首页折线图] 近 7 天：")
    for r in cur.fetchall():
        print("   %s  %sh  (%d 条)" % (r["log_date"], r["h"], r["c"]))

    cur.execute("""SELECT c.category_name, SUM(f.amount) amt, COUNT(*) c
                   FROM biz_finance_record f LEFT JOIN biz_finance_category c ON c.category_id=f.category_id
                   WHERE f.user_id=%s AND f.del_flag='0' AND f.record_type='1'
                   GROUP BY f.category_id, c.category_name ORDER BY amt DESC""", (UID,))
    rows = cur.fetchall()
    total = sum(float(r["amt"]) for r in rows)
    print("[记账环形图] 支出合计 %.2f 元，%d 个类别：" % (total, len(rows)))
    for r in rows:
        print("   %-10s %8.2f  %5.1f%%" % (r["category_name"], r["amt"], float(r["amt"]) / total * 100))

    cur.execute("""SELECT p.project_name,
                     IFNULL(SUM(CASE WHEN f.record_type='0' THEN f.amount END),0) income,
                     IFNULL(SUM(CASE WHEN f.record_type='1' THEN f.amount END),0) cost
                   FROM biz_project p LEFT JOIN biz_finance_record f
                     ON f.project_id=p.project_id AND f.del_flag='0'
                   WHERE p.owner_id=%s AND p.del_flag='0'
                   GROUP BY p.project_id, p.project_name ORDER BY p.project_id""", (UID,))
    print("[项目条形图] 收支对比：")
    for r in cur.fetchall():
        print("   %-12s 收入 %9.2f  成本 %9.2f" % (r["project_name"], r["income"], r["cost"]))

    cur.execute("""SELECT
        (SELECT ROUND(SUM(duration)/3600,1) FROM biz_work_log WHERE user_id=%s AND del_flag='0' AND log_status='1' AND log_date=CURDATE()) today,
        (SELECT ROUND(SUM(duration)/3600,1) FROM biz_work_log WHERE user_id=%s AND del_flag='0' AND log_status='1'
            AND log_date BETWEEN DATE_FORMAT(CURDATE(),'%%Y-%%m-01') AND CURDATE()) month,
        (SELECT COUNT(*) FROM biz_task WHERE assignee_id=%s AND status IN ('0','1') AND del_flag='0') todo,
        (SELECT IFNULL(SUM(amount-settled_amount),0) FROM biz_finance_record WHERE user_id=%s AND record_type='0'
            AND settle_status<>'0' AND del_flag='0') receivable""", (UID, UID, UID, UID))
    print("[首页概览] %s" % cur.fetchone())
    conn.close()


if __name__ == "__main__":
    main()
