#!/bin/bash
#===============================================================
# bookkeepingWhiz 服务器环境一键安装
# 组件：MySQL 8.0 + Redis + Nginx（含安全加固与建库）
# 适用：Alibaba Cloud Linux 4 LTS（RHEL 9 生态，dnf/yum）
# 用法：以 root 执行   bash install-server.sh
# 凭据：安装完成后统一写入 /root/server-credentials.txt（权限 600）
#===============================================================
set -euo pipefail

#---------------- 可配置项 ----------------
DB_NAME="book_keep"
APP_DB_USER="bookkeep"                 # 应用专用账号（不用 root 连库）
APP_PORT=9066                          # RuoYi 后端端口
CRED_FILE="/root/server-credentials.txt"
SQL_DIR="/opt/bookkeeping/sql"         # 可选：把表结构 SQL 放这里，脚本会自动导入
# MYSQL_ROOT_PW / APP_DB_PW / REDIS_PASSWORD 三个环境变量可预置，留空则自动生成
MYSQL_ROOT_PW="${MYSQL_ROOT_PW:-}"
APP_DB_PW="${APP_DB_PW:-}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
#------------------------------------------

RED='\033[31m'; GREEN='\033[32m'; YELLOW='\033[33m'; CYAN='\033[36m'; NC='\033[0m'
info()  { echo -e "${CYAN}[INFO]${NC}  $*"; }
ok()    { echo -e "${GREEN}[ OK ]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
die()   { echo -e "${RED}[FAIL]${NC}  $*"; exit 1; }

# 密码生成：字母数字打底 + 固定混入特殊字符，绕开 MySQL 密码复杂度校验又便于转义
gen_pw() { echo "$(openssl rand -hex 12)Bk#$(openssl rand -hex 4)"; }

[ "$(id -u)" -eq 0 ] || die "请以 root 执行"
command -v dnf >/dev/null || die "未检测到 dnf，仅支持 Alibaba Cloud Linux / Anolis / RHEL 系"

info "系统信息：$(cat /etc/os-release | grep PRETTY_NAME | cut -d'"' -f2)"

#---------------- 0. 生成凭据 ----------------
[ -z "$MYSQL_ROOT_PW" ]  && MYSQL_ROOT_PW=$(gen_pw)
[ -z "$APP_DB_PW" ]      && APP_DB_PW=$(gen_pw)
[ -z "$REDIS_PASSWORD" ] && REDIS_PASSWORD=$(gen_pw)
info "凭据已生成（稍后统一写入 $CRED_FILE）"

#---------------- 1. 基础工具 ----------------
info "安装基础工具..."
dnf -y install openssl curl wget tar >/dev/null 2>&1 || true
ok "基础工具就绪"

#---------------- 2. MySQL 8.0 ----------------
install_mysql() {
    if command -v mysqld >/dev/null 2>&1; then
        warn "检测到已安装 MySQL，跳过安装步骤"
        return
    fi
    # 优先系统源（Alibaba Cloud Linux 自带 mysql-server）
    if dnf -y install mysql-server >/dev/null 2>&1 \
       && mysqld --version 2>/dev/null | grep -q "8\."; then
        ok "MySQL 已通过系统源安装：$(mysqld --version)"
        return
    fi
    # 系统源不满足则手写 MySQL 8.0 仓库文件。
    # 注意：阿里云镜像没有 mysql yum 目录（repo rpm 404），改用中科大镜像（已验证可用）；
    # module_hotfixes=1 用于绕过 Anolis/AL4 的 dnf 模块流对非模块包的拦截
    warn "系统源无 MySQL 8.0，改用社区 8.0 仓库（中科大镜像）..."
    # 清理任何来源的旧 mysql repo —— 服务器上若残留官方 release rpm 装的
    # mysql80-community.repo（el8 baseurl + 不存在的 gpgkey 文件），
    # 会抢先于本脚本生成的 repo 生效，导致装错 el8 包且 GPG 校验失败
    rm -f /etc/yum.repos.d/mysql80-community.repo /etc/yum.repos.d/mysql-community*.repo
    dnf -y remove mysql-server >/dev/null 2>&1 || true
    cat > /etc/yum.repos.d/mysql80-community.repo <<'REPO'
[mysql80-community]
name=MySQL 8.0 Community Server (USTC)
baseurl=https://mirrors.ustc.edu.cn/mysql-repo/yum/mysql-8.0-community/el/9/x86_64/
enabled=1
gpgcheck=0
module_hotfixes=1
REPO
    dnf clean all >/dev/null 2>&1 || true
    # --nogpgcheck 双保险：官方 2023 起换新签名 key，与其纠结 key 导入不如信任国内镜像源
    dnf -y --nogpgcheck install mysql-community-server
    ok "MySQL 已通过社区仓库安装：$(mysqld --version)"
}

info "安装 MySQL 8.0..."
install_mysql

info "配置 MySQL（utf8mb4 / 东八区 / 仅本机监听）..."
cat > /etc/my.cnf.d/bookkeeping.cnf <<EOF
[mysqld]
character-set-server = utf8mb4
collation-server     = utf8mb4_general_ci
default-time-zone    = '+08:00'
max_connections      = 500
innodb_buffer_pool_size = 256M
# 应用与数据库同机部署，只监听本机最安全；
# 如需远程用 Navicat 管理，改为 0.0.0.0 并仅对可信 IP 放行安全组
bind-address         = 127.0.0.1
lower_case_table_names = 0
EOF

systemctl enable --now mysqld
sleep 3
systemctl is-active mysqld >/dev/null || die "mysqld 启动失败，请查看 journalctl -u mysqld"

# 首次登录：兼容「临时密码」与「无密码」两种初始化方式
TMP_PW=$(grep 'temporary password' /var/log/mysqld.log 2>/dev/null | tail -1 | awk '{print $NF}' || true)
if [ -n "$TMP_PW" ]; then
    mysql --connect-expired-password -uroot -p"$TMP_PW" \
      -e "UNINSTALL COMPONENT 'file://validate_password';" 2>/dev/null || true
    mysql --connect-expired-password -uroot -p"$TMP_PW" \
      -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '$MYSQL_ROOT_PW';"
else
    mysql -uroot \
      -e "UNINSTALL COMPONENT 'file://validate_password';" 2>/dev/null || true
    mysql -uroot -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '$MYSQL_ROOT_PW';" 2>/dev/null \
      || mysql -uroot -p"$MYSQL_ROOT_PW" -e "SELECT 1;"
fi
ok "root 密码已设置"

info "建库建用户（$DB_NAME / $APP_DB_USER，仅限本机连接）..."
mysql -uroot -p"$MYSQL_ROOT_PW" <<SQL
CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER IF NOT EXISTS '$APP_DB_USER'@'localhost' IDENTIFIED BY '$APP_DB_PW';
GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$APP_DB_USER'@'localhost';
FLUSH PRIVILEGES;
SQL
ok "数据库与账号已创建"

# 可选：导入 /opt/bookkeeping/sql 下的表结构
if [ -d "$SQL_DIR" ] && ls "$SQL_DIR"/*.sql >/dev/null 2>&1; then
    info "发现 $SQL_DIR 下的 SQL 文件，开始导入..."
    for f in $(ls "$SQL_DIR"/*.sql | sort); do
        info "  导入 $(basename $f)"
        mysql -uroot -p"$MYSQL_ROOT_PW" "$DB_NAME" < "$f"
    done
    ok "表结构导入完成"
else
    warn "未发现 $SQL_DIR/*.sql，跳过建表。请把 sys 表结构与 biz 表结构 SQL 上传到该目录后手动导入："
    warn "  mysql -uroot -p $DB_NAME < /opt/bookkeeping/sql/xxx.sql"
fi

#---------------- 3. Redis ----------------
info "安装并配置 Redis..."
dnf -y install redis >/dev/null 2>&1 || die "redis 安装失败"

sed -i "s/^bind .*/bind 127.0.0.1/" /etc/redis/redis.conf 2>/dev/null || true
if grep -q "^requirepass" /etc/redis/redis.conf; then
    sed -i "s/^requirepass .*/requirepass $REDIS_PASSWORD/" /etc/redis/redis.conf
else
    echo "requirepass $REDIS_PASSWORD" >> /etc/redis/redis.conf
fi
systemctl enable --now redis
systemctl is-active redis >/dev/null || die "redis 启动失败"
# 验证密码生效
redis-cli -a "$REDIS_PASSWORD" ping 2>/dev/null | grep -q PONG || die "redis 密码验证失败"
ok "Redis 就绪（仅本机监听 + 密码）"

#---------------- 4. Nginx ----------------
info "安装并配置 Nginx..."
dnf -y install nginx >/dev/null 2>&1 || die "nginx 安装失败"

cat > /etc/nginx/conf.d/bookkeeping.conf <<EOF
# 小程序 / 管理端 API 入口
server {
    listen 80;
    server_name _;

    # 头像、支付凭证上传上限
    client_max_body_size 20m;

    # 整站反代到 RuoYi 后端（小程序只走 API，无需配静态目录）
    location / {
        proxy_pass http://127.0.0.1:$APP_PORT;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 120s;
    }
}

# HTTPS 预留：小程序上线必须 HTTPS。
# 申请证书（阿里云免费证书或 certbot）后，把证书路径填入并取消注释：
# server {
#     listen 443 ssl;
#     server_name your-domain.com;
#     ssl_certificate     /etc/nginx/cert/your-domain.pem;
#     ssl_certificate_key /etc/nginx/cert/your-domain.key;
#     client_max_body_size 20m;
#     location / {
#         proxy_pass http://127.0.0.1:$APP_PORT;
#         proxy_set_header Host \$host;
#         proxy_set_header X-Real-IP \$remote_addr;
#         proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
#         proxy_set_header X-Forwarded-Proto \$scheme;
#     }
# }
EOF

systemctl enable --now nginx
nginx -t >/dev/null 2>&1 || die "nginx 配置校验失败"
ok "Nginx 就绪（80 → $APP_PORT 反向代理）"

#---------------- 5. 防火墙 ----------------
if systemctl is-active firewalld >/dev/null 2>&1; then
    info "配置 firewalld（放行 80/443，3306/6379 不对外）..."
    firewall-cmd --permanent --add-service=http  >/dev/null
    firewall-cmd --permanent --add-service=https >/dev/null
    firewall-cmd --reload >/dev/null
    ok "防火墙已配置"
else
    warn "firewalld 未启用；请确认阿里云安全组已放行 80/443，且不要放行 3306/6379"
fi

#---------------- 6. 凭据落盘 ----------------
umask 077
cat > "$CRED_FILE" <<EOF
# bookkeepingWhiz 服务器凭据（生成于 $(date '+%F %T')）
# 请妥善保管，泄露后立即在服务器上改密
[MySQL]
host     = 127.0.0.1
database = $DB_NAME
root密码 = $MYSQL_ROOT_PW
应用账号 = $APP_DB_USER
应用密码 = $APP_DB_PW

[Redis]
host     = 127.0.0.1
password = $REDIS_PASSWORD

[Nginx]
监听 80，反向代理 -> 127.0.0.1:$APP_PORT
配置文件 = /etc/nginx/conf.d/bookkeeping.conf
EOF
chmod 600 "$CRED_FILE"
ok "凭据已写入 $CRED_FILE（权限 600）"

#---------------- 7. 汇总 ----------------
echo ""
echo -e "${GREEN}================= 安装完成 =================${NC}"
echo "MySQL 8.0 : $(mysqld --version 2>/dev/null || echo '?')"
echo "Redis     : $(redis-server --version 2>/dev/null | awk '{print $3}' || echo '?')  (127.0.0.1, 已设密码)"
echo "Nginx     : $(nginx -v 2>&1 | cut -d'/' -f2)  (80 -> 127.0.0.1:$APP_PORT)"
echo ""
echo -e "${YELLOW}接下来你要做的：${NC}"
echo "1. 把凭据抄进 application.yml：datasource 与 redis 的密码"
echo "2. 上传表结构 SQL 到 $SQL_DIR 并导入（如上面未自动执行）"
echo "3. 阿里云控制台安全组放行 80/443（3306/6379 保持关闭）"
echo "4. 申请域名证书并启用脚本里预留的 HTTPS 配置（小程序上线必需）"
echo "5. 上传后端 jar：nohup java -jar ruoyi-admin.jar --spring.profiles.active=prod &"
echo -e "${GREEN}=============================================${NC}"
