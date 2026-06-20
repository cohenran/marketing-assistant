#!/usr/bin/env bash
# Run the marketing assistant manually (dev/testing or non-systemd hosts).
# On a server, prefer the systemd unit in deploy/marketing-assistant.service.
set -euo pipefail

# Path to the env file holding secrets. Override: ENV_FILE=/path ./run.sh
ENV_FILE="${ENV_FILE:-/etc/marketing-assistant/marketing-assistant.env}"

if [[ -f "$ENV_FILE" ]]; then
  echo "Loading env from $ENV_FILE"
  set -a                 # export everything sourced
  # shellcheck disable=SC1090
  . "$ENV_FILE"
  set +a
else
  echo "WARN: $ENV_FILE not found — relying on already-exported environment vars"
fi

# Sanity check the required vars are present before starting.
: "${ANTHROPIC_API_KEY:?set ANTHROPIC_API_KEY}"
: "${MAIL_USERNAME:?set MAIL_USERNAME}"
: "${MAIL_PASSWORD:?set MAIL_PASSWORD}"

cd "$(dirname "$0")"

JAR="target/reddit-marketing-assistant-1.0.0.jar"
if [[ ! -f "$JAR" ]]; then
  echo "Jar not found — building with Maven..."
  mvn -q clean package -DskipTests
fi

echo "Starting marketing assistant..."
exec java -jar "$JAR" "$@"
