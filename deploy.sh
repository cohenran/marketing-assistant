#!/usr/bin/env bash
# Build the app and (re)install it as a systemd service on this Ubuntu server.
# Run from the cloned repo dir, as root:  sudo ./deploy.sh
set -euo pipefail

APP_DIR=/opt/marketing-assistant
ENV_DIR=/etc/marketing-assistant
UNIT=marketing-assistant.service
JAR_NAME=reddit-marketing-assistant-1.0.0.jar

# Run from the repo root (where pom.xml lives).
cd "$(dirname "$0")"

echo "==> Pulling latest code"
git pull --ff-only || echo "WARN: git pull skipped (no upstream?) — building current checkout"

echo "==> Building jar"
mvn -q clean package -DskipTests

echo "==> Installing jar to $APP_DIR"
mkdir -p "$APP_DIR"
cp "target/$JAR_NAME" "$APP_DIR/"

echo "==> Ensuring env dir exists ($ENV_DIR)"
mkdir -p "$ENV_DIR"
if [[ ! -f "$ENV_DIR/$UNIT.env" && ! -f "$ENV_DIR/marketing-assistant.env" ]]; then
  echo "WARN: no env file at $ENV_DIR/marketing-assistant.env — create it (chmod 600) before the service will start"
fi

echo "==> Installing systemd unit"
cp "deploy/$UNIT" "/etc/systemd/system/$UNIT"
systemctl daemon-reload
systemctl enable "$UNIT"
systemctl restart "$UNIT"

echo "==> Done. Status:"
systemctl --no-pager status "$UNIT" || true
echo "Tail logs with:  journalctl -u $UNIT -f"
