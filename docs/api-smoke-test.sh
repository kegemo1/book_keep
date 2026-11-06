#!/bin/bash
#=============================================================
# 账时记 · 生产环境全链路只读测试
# 模拟体验版真实用户旅程：登录 → 首页 → 项目 → 任务 → 工时 → 记账 → 导出
# 安全性：全部为只读接口（export 生成的文件 3 天自动清理）
#=============================================================
BASE="${API_BASE:-http://127.0.0.1:9066}"  # 默认本地，线上环境: API_BASE=https://your-domain.com ./api-smoke-test.sh
PASS=0; FAIL=0
# 外部注入 token 用法（可选）：API_TOKEN='Bearer后那串' bash docs/api-smoke-test.sh
TOKEN="${API_TOKEN:-}"

# ---------- 1. 登录（openid 硬编码调试模式，命中已有档案，不产生新数据） ----------
LOGIN=$(curl -sk -X POST "$BASE/wx/auth/login" -H "Content-Type: application/json" \
  -d '{"code":"apitest2026"}' -m 15)
TOKEN=$(echo "$LOGIN" | python -c "import sys,json;print(json.load(sys.stdin).get('token',''))" 2>/dev/null)
AUTH="Authorization: Bearer $TOKEN"

t() { # t <label> <method> <path> [body]
  local label="$1" m="$2" p="$3" b="$4"
  if [ -n "$b" ]; then
    RESP=$(curl -sk -X "$m" -H "Content-Type: application/json" -H "$AUTH" -d "$b" -m 20 "$BASE$p")
  else
    RESP=$(curl -sk -X "$m" -H "$AUTH" -m 20 "$BASE$p")
  fi
  CODE=$(echo "$RESP" | python -c "import sys,json;print(json.load(sys.stdin).get('code','?'))" 2>/dev/null || echo "ERR")
  if [ "$CODE" = "200" ]; then
    echo "  OK   $label"; PASS=$((PASS+1))
  else
    echo "  FAIL $label (code=$CODE)"; echo "       $RESP" | head -c 200; echo; FAIL=$((FAIL+1))
  fi
  LAST="$RESP"
}

echo "===== 1. 登录链路 ====="
if [ -z "$TOKEN" ]; then
  LOGIN=$(curl -sk -X POST "$BASE/wx/auth/login" -H "Content-Type: application/json" \
    -d '{"code":"apitest2026"}' -m 15)
  TOKEN=$(echo "$LOGIN" | python -c "import sys,json;print(json.load(sys.stdin).get('token',''))" 2>/dev/null)
fi
AUTH="Authorization: Bearer $TOKEN"

t() { # t <label> <method> <path> [body]
  local label="$1" m="$2" p="$3" b="$4"
  if [ -n "$b" ]; then
    RESP=$(curl -sk -X "$m" -H "Content-Type: application/json" -H "$AUTH" -d "$b" -m 20 "$BASE$p")
  else
    RESP=$(curl -sk -X "$m" -H "$AUTH" -m 20 "$BASE$p")
  fi
  CODE=$(echo "$RESP" | python -c "import sys,json;print(json.load(sys.stdin).get('code','?'))" 2>/dev/null || echo "ERR")
  if [ "$CODE" = "200" ]; then
    echo "  OK   $label"; PASS=$((PASS+1))
  else
    echo "  FAIL $label (code=$CODE)"; echo "       $RESP" | head -c 200; echo; FAIL=$((FAIL+1))
  fi
  LAST="$RESP"
}

if [ -n "$TOKEN" ]; then
  echo "  OK   使用外部注入的 token（跳过 login 测试）"; PASS=$((PASS+1))
else
  if [ -n "$TOKEN" ]; then
    echo "  OK   POST /wx/auth/login (token 获取成功, ${#TOKEN} 字符)"; PASS=$((PASS+1))
  else
    echo "  FAIL POST /wx/auth/login（真实登录需 wx.login 真实 code，外部无法模拟）"
    echo "  $LOGIN" | head -c 200; echo; FAIL=$((FAIL+1)); exit 1
  fi
fi

t  "GET  /wx/auth/info          个人资料" GET  "/wx/auth/info"

echo "===== 2. 项目链路 ====="
t  "GET  /wx/project/list       项目下拉（应仅本人项目）" GET  "/wx/project/list"
t  "GET  /wx/project/profit     项目盈亏" GET  "/wx/project/profit"
t  "GET  /wx/project/dashboard  首页统计" GET  "/wx/project/dashboard"

PROJECT_ID=$(echo "$LAST" | python -c "
import sys,json
try:
    d=json.load(sys.stdin); rows=d.get('data') or []
    print(rows[0]['projectId'] if rows else 100)
except Exception: print(100)" 2>/dev/null)

echo "===== 3. 任务链路（projectId=$PROJECT_ID）====="
t  "GET  /wx/task/list          任务列表" GET  "/wx/task/list?projectId=$PROJECT_ID"
t  "GET  /wx/task/taskOptions   任务选项" GET  "/wx/task/taskOptions/$PROJECT_ID"

echo "===== 4. 工时链路 ====="
t  "GET  /wx/worklog/running    进行中计时" GET  "/wx/worklog/running"
t  "GET  /wx/worklog/today      今日工时" GET  "/wx/worklog/today"
t  "GET  /wx/worklog/stat/daily 近7天趋势" GET  "/wx/worklog/stat/daily?days=7"

echo "===== 5. 记账链路 ====="
t  "GET  /wx/finance/summary    收支汇总" GET  "/wx/finance/summary"
t  "GET  /wx/finance/categories 类别统计" GET  "/wx/finance/categories?recordType=1"
t  "GET  /wx/finance/list       流水分页" GET  "/wx/finance/list?pageNum=1&pageSize=5"

echo "===== 6. 注销检查（只读，不真注销）====="
t  "GET  /wx/auth/cancel/check  注销前检查" GET  "/wx/auth/cancel/check"

echo "===== 7. 数据导出（REQ-002，生成文件 3 天后自动清理）====="
t  "POST /wx/export             年度数据导出" POST "/wx/export" '{"scope":"year"}'
XLSX_URL=$(echo "$LAST" | python -c "import sys,json;print(json.load(sys.stdin).get('data',{}).get('xlsxUrl',''))" 2>/dev/null)
if [ -n "$XLSX_URL" ]; then
  XCODE=$(curl -sk -o /dev/null -w "%{http_code}" -m 20 "$XLSX_URL")
  if [ "$XCODE" = "200" ]; then echo "  OK   下载 Excel 文件 ($XCODE)"; PASS=$((PASS+1));
  else echo "  FAIL 下载 Excel 文件 ($XCODE)"; FAIL=$((FAIL+1)); fi
fi

echo "===== 8. 清理测试令牌 ====="
t  "POST /wx/auth/logout        退出登录" POST "/wx/auth/logout"

echo ""
echo "==================== 结果 ===================="
echo "通过: $PASS   失败: $FAIL"
