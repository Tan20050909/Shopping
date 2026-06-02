#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$ROOT_DIR/.run"

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

# 按 PID 停止单个进程
stop_pid() {
  local name="$1"
  local pid="$2"

  if [[ -z "${pid:-}" ]]; then
    echo "${name:-unknown} PID 记录为空，清理记录。"
    return 0
  fi

  if kill -0 "$pid" 2>/dev/null; then
    echo "停止 ${name:-unknown}，PID: ${pid}"
    kill "$pid" 2>/dev/null || true
  else
    echo "${name:-unknown} 未在运行（PID: ${pid}），清理记录。"
  fi
}

# 第一步：按 PID 文件停止所有服务
if [[ ! -d "$RUN_DIR" ]]; then
  echo "没有找到运行记录，跳过 PID 文件清理。"
else
  shopt -s nullglob
  pid_files=("$RUN_DIR"/*.pid)

  if [[ ${#pid_files[@]} -eq 0 ]]; then
    echo "没有 PID 文件，跳过 PID 文件清理。"
  else
    for pid_file in "${pid_files[@]}"; do
      name="$(basename "$pid_file" .pid)"
      pid="$(cat "$pid_file")"
      stop_pid "${name:-unknown}" "${pid:-}"
      rm -f "$pid_file"
    done
  fi
fi

# 第二步：扫描固定端口，清理本项目残留进程
echo "扫描端口，清理残留进程..."

for port in 8082 8081 8080 5173 3000 3002; do
  pids="$(port_pids "$port")"
  [[ -z "$pids" ]] && continue

  for pid in $pids; do
    if process_belongs_to_project "$pid"; then
      echo "停止占用项目端口 ${port} 的残留进程，PID: ${pid}"
      kill "$pid" 2>/dev/null || true
    else
      echo "端口 ${port} 被非本项目进程占用，跳过，PID: ${pid}"
    fi
  done
done

# 等待进程退出，未退出则强制停止
sleep 1

for port in 8082 8081 8080 5173 3000 3002; do
  pids="$(port_pids "$port")"
  [[ -z "$pids" ]] && continue

  for pid in $pids; do
    if process_belongs_to_project "$pid"; then
      echo "强制停止占用项目端口 ${port} 的残留进程，PID: ${pid}"
      kill -9 "$pid" 2>/dev/null || true
    fi
  done
done

echo "停止完成。"
