#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="$SCRIPT_DIR/.app.pid"
JAR_FILE="$SCRIPT_DIR/target/ai-summary-podcast-0.0.1-SNAPSHOT.jar"

if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
  echo "Application is already running (PID $(cat "$PID_FILE"))"
  exit 1
fi

echo "Building ai-summary-podcast..."
cd "$SCRIPT_DIR"
./mvnw -q package -DskipTests

if [ -f .env ]; then
  set -a
  source .env
  set +a
fi

echo "Starting ai-summary-podcast..."
java --enable-native-access=ALL-UNNAMED -jar "$JAR_FILE" > app.log 2>&1 &
APP_PID=$!
echo "$APP_PID" > "$PID_FILE"
echo "Application started (PID $APP_PID). Logs: app.log"
