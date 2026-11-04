#!/bin/bash
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=== Đang dừng hệ thống AI Interview Application ==="

stop_pid() {
    local pid_file="$1"
    local name="$2"
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if kill -0 "$pid" 2>/dev/null; then
            echo "-> Đang dừng $name (PID $pid)..."
            kill "$pid" 2>/dev/null || true
            sleep 1
            kill -9 "$pid" 2>/dev/null || true
        fi
        rm -f "$pid_file"
    fi
}

stop_pid "$ROOT_DIR/.frontend.pid" "Frontend"
stop_pid "$ROOT_DIR/.backend.pid" "Java Backend"
stop_pid "$ROOT_DIR/.ai-service.pid" "AI Service"

# Đảm bảo giải phóng các port nếu còn tiến trình sót
fuser -k 5173/tcp 2>/dev/null || true
fuser -k 8080/tcp 2>/dev/null || true
fuser -k 8000/tcp 2>/dev/null || true

echo "✅ Toàn bộ hệ thống đã được dừng và giải phóng cổng mạng!"

