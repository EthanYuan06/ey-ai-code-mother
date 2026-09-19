#!/bin/bash
# =============================================
# ey-ai-code-mother 微服务 —— Linux 一键部署 / 运维脚本
# 用法: bash deploy.sh [init|update|stop|restart|logs|status|ps|down|build]
#
# 编排组件: higress(网关) / nacos(注册中心) / mysql / redis / user / app / screenshot / frontend
# 首次 init 后需在 Higress 控制台配置路由/域名/证书（详见部署文档第二阶段）。
# 请在本脚本所在目录（ey-ai-code-mother-microservice）执行。
# =============================================

set -e

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
err()  { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# 切换到脚本所在目录，保证相对路径（../sql、../ey-ai-code-mother-frontend、./docker）正确
cd "$(dirname "$0")"

# ---------- 前置检查 ----------
command -v docker >/dev/null 2>&1 || err "Docker 未安装，请先安装 Docker"
docker compose version >/dev/null 2>&1 || err "Docker Compose 未安装（需要 docker compose 插件）"

# ---------- 环境变量文件 ----------
if [ ! -f .env ]; then
    warn ".env 文件不存在。"
    if [ -f ../.env ]; then
        cp ../.env .env
        warn "已从上级目录 ../.env 复制模板，请核对/补充配置（尤其 DEPLOY_HOST）后重新运行。"
    else
        err "缺少 .env 文件（需包含 MYSQL_ROOT_PASSWORD、COS_*、DASHSCOPE_*、DEEPSEEK_* 等）"
    fi
    exit 0
fi

# DEPLOY_HOST 提示（生产环境应为 https 域名，否则部署作品链接会指向 localhost）
if ! grep -q '^DEPLOY_HOST=' .env; then
    warn ".env 未设置 DEPLOY_HOST，将默认 http://localhost:8080；生产环境请设为 https://你的域名"
fi

# 宿主机 IP（用于打印访问地址）
HOST_IP=$(hostname -I 2>/dev/null | awk '{print $1}')
[ -z "$HOST_IP" ] && HOST_IP="localhost"

# ---------- 部署动作 ----------
ACTION=${1:-update}

case $ACTION in
    init)
        log "===== 首次初始化部署（构建镜像 + 启动 + 初始化数据库）====="
        docker compose build --no-cache
        docker compose up -d
        log "等待 MySQL / Nacos 初始化..."
        sleep 20
        log "===== 部署完成 ====="
        log "网关入口:      http://${HOST_IP}:${HIGRESS_HTTP_PORT:-8080}  (生产为 https://你的域名)"
        log "Higress 控制台: http://${HOST_IP}:${HIGRESS_CONSOLE_PORT:-8001}  ← 首次需在此配路由/域名/证书"
        log "Nacos 控制台:   http://${HOST_IP}:${NACOS_HOST_PORT:-8848}/nacos  (默认 nacos/nacos)"
        log "后端 Swagger:   http://${HOST_IP}:${HIGRESS_HTTP_PORT:-8080}/api/doc.html  (经网关)"
        ;;
    update)
        log "===== 更新部署（重新构建 + 滚动更新）====="
        docker compose build
        docker compose up -d --remove-orphans
        log "===== 更新完成 ====="
        ;;
    build)
        log "===== 仅构建镜像 ====="
        docker compose build
        log "构建完成"
        ;;
    stop)
        log "===== 停止所有服务 ====="
        docker compose stop
        log "已停止"
        ;;
    down)
        log "===== 停止并移除容器（保留数据卷）====="
        docker compose down --remove-orphans
        log "已移除"
        ;;
    restart)
        log "===== 重启服务 ====="
        docker compose restart
        log "已重启"
        ;;
    logs)
        # 可选指定服务名: bash deploy.sh logs app
        docker compose logs -f --tail=100 ${2:-}
        ;;
    status|ps)
        docker compose ps
        ;;
    *)
        echo "用法: bash deploy.sh [init|update|build|stop|down|restart|logs|status]"
        echo "  init    - 首次部署（构建镜像 + 启动 + 初始化数据库）"
        echo "  update  - 更新部署（重新构建 + 滚动更新）默认"
        echo "  build   - 仅构建镜像，不启动"
        echo "  stop    - 停止所有服务（不移除容器）"
        echo "  down    - 停止并移除容器（保留数据卷）"
        echo "  restart - 重启所有服务"
        echo "  logs    - 查看日志，可选指定服务名: bash deploy.sh logs app"
        echo "            可选服务: nacos mysql redis user app screenshot frontend"
        echo "  status  - 查看服务状态"
        exit 0
        ;;
esac
