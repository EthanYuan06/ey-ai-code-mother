#!/bin/bash
# =============================================
# 宝塔 Linux 一键部署 / 更新脚本
# 用法: bash deploy.sh
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

# ---------- 前置检查 ----------
command -v docker >/dev/null 2>&1 || err "Docker 未安装，请先安装 Docker"
command -v docker compose >/dev/null 2>&1 || err "Docker Compose 未安装"

# ---------- 环境变量文件 ----------
if [ ! -f .env ]; then
    if [ -f .env.docker ]; then
        warn ".env 文件不存在，已从 .env.docker 复制模板"
        cp .env.docker .env
        warn "请编辑 .env 文件填写实际配置后重新运行: bash deploy.sh"
        exit 0
    else
        err "缺少 .env.docker 模板文件"
    fi
fi

# ---------- 部署动作 ----------
ACTION=${1:-update}

case $ACTION in
    init)
        log "===== 首次初始化部署 ====="
        docker compose build --no-cache
        docker compose up -d
        log "等待 MySQL 初始化..."
        sleep 15
        log "===== 部署完成 ====="
        log "前端访问: http://$(hostname -I | awk '{print $1}')"
        log "后端 API: http://$(hostname -I | awk '{print $1}')/api"
        log "Swagger:  http://$(hostname -I | awk '{print $1}')/api/doc.html"
        ;;
    update)
        log "===== 更新部署 ====="
        docker compose pull
        docker compose build
        docker compose up -d --remove-orphans
        log "===== 更新完成 ====="
        ;;
    stop)
        log "===== 停止所有服务 ====="
        docker compose down
        log "已停止"
        ;;
    restart)
        log "===== 重启服务 ====="
        docker compose restart
        log "已重启"
        ;;
    logs)
        docker compose logs -f --tail=100 ${2:-}
        ;;
    status)
        docker compose ps
        ;;
    *)
        echo "用法: bash deploy.sh [init|update|stop|restart|logs|status]"
        echo "  init    - 首次部署（构建镜像 + 启动 + 初始化数据库）"
        echo "  update  - 更新部署（拉取代码 + 重新构建 + 滚动更新）默认"
        echo "  stop    - 停止所有服务"
        echo "  restart - 重启所有服务"
        echo "  logs    - 查看日志，可选指定服务名: bash deploy.sh logs backend"
        echo "  status  - 查看服务状态"
        exit 0
        ;;
esac
