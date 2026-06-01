#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$ROOT_DIR/.run"
LOG_DIR="$ROOT_DIR/logs"

mkdir -p "$RUN_DIR" "$LOG_DIR"

prepare_frontend() {
  local dir="$1"

  echo "检查前端依赖: $dir"
  (
    cd "$ROOT_DIR/$dir"
    if [[ ! -d node_modules ]] || [[ ! -e node_modules/vite/bin/vite.js ]]; then
      npm install
    fi
    chmod +x node_modules/.bin/vite node_modules/vite/bin/vite.js 2>/dev/null || true
  )
}

start_service() {
  local name="$1"
  local dir="$2"
  shift 2
  local pid_file="$RUN_DIR/$name.pid"
  local log_file="$LOG_DIR/$name.log"

  if [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
    echo "$name 已在运行，PID: $(cat "$pid_file")"
    return
  fi

  echo "启动 $name..."
  (
    cd "$ROOT_DIR/$dir"
    "$@"
  ) >"$log_file" 2>&1 &

  echo $! >"$pid_file"
  sleep 2
  if ! kill -0 "$(cat "$pid_file")" 2>/dev/null; then
    echo "$name 启动失败，最近日志："
    tail -40 "$log_file" || true
    rm -f "$pid_file"
    return 1
  fi

  echo "$name PID: $(cat "$pid_file")，日志: $log_file"
}

prepare_frontend "Shopping/user/frontend"
prepare_frontend "Shopping/merchant/frontend"
prepare_frontend "Shopping/homepage-common"
prepare_frontend "Shopping/admin"

start_service "user-backend" "Shopping/user" mvn spring-boot:run
start_service "merchant-backend" "Shopping/merchant/backend" mvn spring-boot:run
start_service "admin-backend" "Shopping/admin" mvn spring-boot:run
start_service "user-frontend" "Shopping/user/frontend" npm run dev -- --host 0.0.0.0
start_service "merchant-frontend" "Shopping/merchant/frontend" npm run dev -- --host 0.0.0.0
start_service "admin-frontend" "Shopping/admin" npm run dev -- --host 0.0.0.0
start_service "homepage-common" "Shopping/homepage-common" npm run dev -- --host 0.0.0.0

echo
echo "全部启动命令已发出。常用地址："
echo "用户前端:     http://localhost:5173"
echo "商家前端:     http://localhost:3000"
echo "平台前端:     http://localhost:3002"
echo "公共首页:     http://localhost:3001"
echo "用户后端:     http://localhost:8082"
echo "商家后端:     http://localhost:8081"
echo "平台后端:     http://localhost:8080"
echo
echo "停止全部服务：./stop-all.sh"
