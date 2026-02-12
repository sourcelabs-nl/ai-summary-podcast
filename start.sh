#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="$SCRIPT_DIR/.app.pid"

if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
  echo "Application is already running (PID $(cat "$PID_FILE"))"
  exit 1
fi

echo "Starting ai-summary-podcast..."
cd "$SCRIPT_DIR"
if [ -f .env ]; then
  set -a
  source .env
  set +a
fi
./mvnw spring-boot:run > app.log 2>&1 &
APP_PID=$!
echo "$APP_PID" > "$PID_FILE"
echo "Application started (PID $APP_PID). Logs: app.log"
