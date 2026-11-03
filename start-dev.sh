#!/bin/bash
ROOT="/home/zenos/Downloads/Do An/kiro-srs-ai-interview-config"
LOGDIR="/tmp/interview-logs"
mkdir -p "$LOGDIR"
get_env() { grep "^$1=" "$ROOT/.env" | head -1 | cut -d= -f2-; }
echo "[1/3] Python AI Service..."
cd "$ROOT/ai-service"
nohup .venv/bin/uvicorn main:app --reload > "$LOGDIR/ai-service.log" 2>&1 &
echo "  PID: $!"
echo "[2/3] Java Backend..."
cd "$ROOT/backend"
RAW_URL=$(get_env DATABASE_URL)
DB_HOST=$(echo "$RAW_URL" | sed 's/.*@//;s|/.*||')
DB_NAME=$(echo "$RAW_URL" | sed 's|.*/||;s|?.*||')
export DATABASE_URL="jdbc:postgresql://${DB_HOST}/${DB_NAME}?sslmode=require"
export DB_USERNAME=$(get_env DB_USERNAME)
export DB_PASSWORD=$(get_env DB_PASSWORD)
export JWT_SECRET=$(get_env JWT_SECRET)
export INTERNAL_API_KEY=$(get_env INTERNAL_API_KEY)
export AI_SERVICE_BASE_URL=$(get_env AI_SERVICE_BASE_URL)
export AI_SERVICE_TIMEOUT_SECONDS=$(get_env AI_SERVICE_TIMEOUT_SECONDS)
export ALLOWED_ORIGINS=$(get_env ALLOWED_ORIGINS)
export ADMIN_EMAIL=$(get_env ADMIN_EMAIL)
export ADMIN_PASSWORD=$(get_env ADMIN_PASSWORD)
nohup mvn spring-boot:run > "$LOGDIR/backend.log" 2>&1 &
echo "  PID: $!"
echo "[3/3] Frontend..."
cd "$ROOT/frontend"
nohup npm run dev > "$LOGDIR/frontend.log" 2>&1 &
echo "  PID: $!"
echo "Done. Logs: $LOGDIR/"
