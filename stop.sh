#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="$SCRIPT_DIR/.app.pid"

if [ ! -f "$PID_FILE" ]; then
  echo "No PID file found. Application may not be running."
  exit 1
fi

APP_PID=$(cat "$PID_FILE")

if kill -0 "$APP_PID" 2>/dev/null; then
  echo "Stopping ai-summary-podcast (PID $APP_PID)..."
  kill "$APP_PID"
  # Wait up to 10 seconds for graceful shutdown
  for i in $(seq 1 10); do
    if ! kill -0 "$APP_PID" 2>/dev/null; then
      break
    fi
    sleep 1
  done
  # Force kill if still running
  if kill -0 "$APP_PID" 2>/dev/null; then
    echo "Graceful shutdown timed out, force killing..."
    kill -9 "$APP_PID"
  fi
  echo "Application stopped."
else
  echo "Process $APP_PID is not running."
fi

rm -f "$PID_FILE"
