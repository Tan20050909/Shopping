#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$ROOT_DIR/.run"

port_pids() {
  local port="$1"
  lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true
}

process_belongs_to_project() {
  local pid="$1"
  local cmd cwd
  cmd="$(ps -p "$pid" -o command= 2>/dev/null || true)"
  cwd="$(lsof -a -p "$pid" -d cwd -Fn 2>/dev/null | sed -n 's/^n//p' | head -1 || true)"

  [[ "$cmd" == *"$ROOT_DIR"* || "$cwd" == "$ROOT_DIR"* ]]
}

stop_pid() {
  local name="$1"
  local pid="$2"

  if kill -0 "${pid:-}" 2>/dev/null; then
    echo "停止 ${name:-unknown}，PID: ${pid:-?}"
    kill "${pid:-}" 2>/dev/null || true
  else
    echo "${name:-unknown} 未在运行，清理记录。"
  fi
}

if [[ ! -d "$RUN_DIR" ]]; then
  echo "没有找到运行记录，无需停止。"
else
  shopt -s nullglob
  pid_files=("$RUN_DIR"/*.pid)

  for pid_file in "${pid_files[@]}"; do
    name="$(basename "$pid_file" .pid)"
    pid="$(cat "$pid_file")"

    stop_pid "${name:-unknown}" "${pid:-}"
    rm -f "$pid_file"
  done
fi

for port in 8082 8081 8080 5173 3000 3002 3001; do
  pids="$(port_pids "$port")"
  [[ -z "$pids" ]] && continue

  for pid in $pids; do
    if process_belongs_to_project "$pid"; then
      echo "停止占用项目端口 $port 的残留进程，PID: $pid"
      kill "$pid" 2>/dev/null || true
    else
      echo "端口 $port 被非本项目进程占用，跳过，PID: $pid"
    fi
  done
done

sleep 1

for port in 8082 8081 8080 5173 3000 3002 3001; do
  pids="$(port_pids "$port")"
  [[ -z "$pids" ]] && continue

  for pid in $pids; do
    if process_belongs_to_project "$pid"; then
      echo "强制停止占用项目端口 $port 的残留进程，PID: $pid"
      kill -9 "$pid" 2>/dev/null || true
    fi
  done
done

echo "停止完成。"
