#!/usr/bin/env bash
# Smoke test: generate a draft to a local file (smoke-test-output.txt). NO email.
# Only needs ANTHROPIC_API_KEY — no Gmail setup required.
#
# Usage:
#   ./smoke-test.sh                 # generate the first channel
#   ./smoke-test.sh "LinkedIn"      # generate a specific channel
#   ./smoke-test.sh "Product Hunt"  # works for manual-only channels too
set -euo pipefail

# Auto-load the env file if the key isn't already exported. Override path with ENV_FILE=...
ENV_FILE="${ENV_FILE:-/etc/marketing-assistant/marketing-assistant.env}"
if [[ -z "${ANTHROPIC_API_KEY:-}" && -f "$ENV_FILE" ]]; then
  echo "Loading env from $ENV_FILE"
  set -a; . "$ENV_FILE"; set +a
fi

: "${ANTHROPIC_API_KEY:?set ANTHROPIC_API_KEY (export it or put it in $ENV_FILE)}"

cd "$(dirname "$0")"

JAR="target/reddit-marketing-assistant-1.0.0.jar"
if [[ ! -f "$JAR" ]]; then
  echo "Jar not found — building..."
  mvn -q clean package -DskipTests
fi

CHANNEL="${1:-}"
if [[ -n "$CHANNEL" ]]; then
  java -jar "$JAR" --dry-run --run-now="$CHANNEL"
else
  java -jar "$JAR" --dry-run
fi

echo
echo "Done. Draft written to: $(pwd)/smoke-test-output.txt"
