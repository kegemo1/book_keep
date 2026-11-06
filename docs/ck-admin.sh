#!/bin/bash
#=============================================================
# ck-admin 后端服务管理脚本
# 用法：./ck-admin.sh {start|stop|restart|status|tail}
# 日志：/opt/app/logs/ck-admin-YYYYMMDD.log（按天一个文件，追加写入）
#       应用内部运行日志另由 logback 输出到工作目录 logs/ 下
# 安装：上传到 /opt/app/ 后 chmod +x ck-admin.sh
#=============================================================

APP_NAME="ck-admin"
APP_JAR="/opt/app/ck-admin.jar"          # ← 改成 jar 的实际绝对路径
LOG_DIR="/opt/app/logs"
JVM_OPTS="-Xms256m -Xmx512m -Dfile.encoding=UTF-8"
RUN_ARGS="--spring.profiles.active=prod"  # 不用 prod 配置就删掉这一行的值

mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/$APP_NAME-$(date +%Y%m%d).log"

get_pid() {
    pgrep -f "java.*$APP_JAR" || true
}

start() {
    if [ -n "$(get_pid)" ]; then
        echo "[$APP_NAME] 已在运行 (PID $(get_pid))，如需重启请执行 ./$0 restart"
        return
    fi
    echo "[$APP_NAME] 启动中..."
    nohup java $JVM_OPTS -jar "$APP_JAR" $RUN_ARGS >> "$LOG_FILE" 2>&1 &
    sleep 3
    if [ -n "$(get_pid)" ]; then
        echo "[$APP_NAME] 启动成功 (PID $(get_pid))"
        echo "  启动日志：$LOG_FILE"
        echo "  观察启动过程：./$0 tail"
    else
        echo "[$APP_NAME] 启动失败，查看日志：tail -100 $LOG_FILE"
    fi
}

stop() {
    PID=$(get_pid)
    if [ -z "$PID" ]; then
        echo "[$APP_NAME] 未在运行"
        return
    fi
    echo "[$APP_NAME] 停止中 (PID $PID)..."
    kill "$PID"
    # 优雅关闭：最多等 30 秒，超时强杀
    for i in $(seq 1 30); do
        [ -z "$(get_pid)" ] && break
        sleep 1
    done
    if [ -n "$(get_pid)" ]; then
        kill -9 "$(get_pid)"
        echo "[$APP_NAME] 已强制终止"
    else
        echo "[$APP_NAME] 已正常停止"
    fi
}

status() {
    PID=$(get_pid)
    if [ -n "$PID" ]; then
        echo "[$APP_NAME] 运行中 (PID $PID)"
        ps -o etime=,rss= -p "$PID" | awk '{printf "  运行时长: %s | 内存: %.0f MB\n", $1, $2/1024}'
    else
        echo "[$APP_NAME] 未在运行"
    fi
}

tail() {
    LATEST=$(ls -t "$LOG_DIR/$APP_NAME"-*.log 2>/dev/null | head -1)
    echo "跟踪日志：${LATEST:-$LOG_FILE}（Ctrl+C 退出）"
    tail -f "${LATEST:-$LOG_FILE}"
}

case "$1" in
    start)   start ;;
    stop)    stop ;;
    restart) stop; sleep 2; start ;;
    status)  status ;;
    tail)    tail ;;
    *) echo "用法: $0 {start|stop|restart|status|tail}"; exit 1 ;;
esac
