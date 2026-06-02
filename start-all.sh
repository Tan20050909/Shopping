#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$ROOT_DIR/.run"
LOG_DIR="$ROOT_DIR/logs"

mkdir -p "$RUN_DIR" "$LOG_DIR"

# 列出监听指定端口的所有 PID
port_pids() {
  local port="$1"
  lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true
}

# 判断进程是否属于当前项目（命令行包含项目路径，或 cwd 在项目目录下）
process_belongs_to_project() {
  local pid="$1"
  local cmd cwd
  cmd="$(ps -p "$pid" -o command= 2>/dev/null || true)"
  cwd="$(lsof -a -p "$pid" -d cwd -Fn 2>/dev/null | sed -n 's/^n//p' | head -1 || true)"

  [[ "$cmd" == *"$ROOT_DIR"* ]] && return 0
  [[ "$cwd" == "$ROOT_DIR" || "$cwd" == "$ROOT_DIR"/* ]] && return 0
  return 1
}

# 清理端口上的本项目残留进程；非本项目进程则报错退出
stop_project_port() {
  local port="$1"
  local name="$2"
  local pids pid

  pids="$(port_pids "$port")"
  [[ -z "$pids" ]] && return 0

  for pid in $pids; do
    if process_belongs_to_project "$pid"; then
      echo "$name 端口 $port 被本项目残留进程占用，先停止 PID: $pid"
      kill "$pid" 2>/dev/null || true
    else
      echo "错误：$name 端口 $port 被非本项目进程占用，PID: $pid"
      echo "请手动确认后重试，不要误杀其他进程。"
      return 1
    fi
  done

  # 等待进程退出，未退出则强制停止
  sleep 1
  pids="$(port_pids "$port")"
  for pid in $pids; do
    if process_belongs_to_project "$pid"; then
      echo "$name 端口 $port 残留进程未退出，强制停止 PID: $pid"
      kill -9 "$pid" 2>/dev/null || true
    else
      echo "错误：$name 端口 $port 仍被非本项目进程占用，PID: $pid"
      return 1
    fi
  done
}

# 确保前端依赖已安装
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

# 启动单个服务
start_service() {
  local name="$1"
  local dir="$2"
  local port="$3"
  shift 3
  local pid_file="$RUN_DIR/$name.pid"
  local log_file="$LOG_DIR/$name.log"

  # 清理死 PID 文件
  if [[ -f "$pid_file" ]]; then
    local old_pid
    old_pid="$(cat "$pid_file")"
    if kill -0 "$old_pid" 2>/dev/null; then
      echo "$name 已在运行，PID: $old_pid"
      return 0
    else
      echo "$name 发现失效 PID 文件（进程已退出），清理 $pid_file"
      rm -f "$pid_file"
    fi
  fi

  # 清理端口上的本项目残留进程
  stop_project_port "$port" "$name" || return 1

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

# ---- 启动顺序 ----

prepare_frontend "Shopping/user/frontend"
prepare_frontend "Shopping/merchant/frontend"
prepare_frontend "Shopping/admin"

start_service "user-backend"     "Shopping/user"              8082 mvn spring-boot:run
start_service "merchant-backend" "Shopping/merchant/backend"  8081 mvn spring-boot:run
start_service "admin-backend"    "Shopping/admin"             8080 mvn spring-boot:run
start_service "user-frontend"    "Shopping/user/frontend"     5173 npm run dev -- --host 0.0.0.0
start_service "merchant-frontend" "Shopping/merchant/frontend" 3000 npm run dev -- --host 0.0.0.0
start_service "admin-frontend"   "Shopping/admin"             3002 npm run dev -- --host 0.0.0.0

echo
echo "全部启动命令已发出。常用地址："
echo "用户前端:     http://localhost:5173"
echo "商家前端:     http://localhost:3000"
echo "平台前端:     http://localhost:3002"
echo "用户后端:     http://localhost:8082"
echo "商家后端:     http://localhost:8081"
echo "平台后端:     http://localhost:8080"
echo
echo "停止全部服务：./stop-all.sh"
