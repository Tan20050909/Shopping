#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$ROOT_DIR/.run"

if [[ ! -d "$RUN_DIR" ]]; then
  echo "没有找到运行记录，无需停止。"
  exit 0
fi

shopt -s nullglob
pid_files=("$RUN_DIR"/*.pid)

if [[ ${#pid_files[@]} -eq 0 ]]; then
  echo "没有找到运行中的服务记录。"
  exit 0
fi

for pid_file in "${pid_files[@]}"; do
  name="$(basename "$pid_file" .pid)"
  pid="$(cat "$pid_file")"

  if kill -0 "${pid:-}" 2>/dev/null; then
    echo "停止 ${name:-unknown}，PID: ${pid:-?}"
    kill "${pid:-}" 2>/dev/null || true
  else
    echo "${name:-unknown} 未在运行，清理记录。"
  fi

  rm -f "$pid_file"
done

echo "停止完成。"
