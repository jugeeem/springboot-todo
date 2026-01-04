#!/bin/bash

# Spring Boot アプリケーション起動スクリプト
# Dev Container の postStartCommand から呼び出される

set -e

# ワークスペースに移動
cd /workspace/api

# ログディレクトリ作成
mkdir -p /tmp/logs

# Gradle メモリ最適化環境変数設定
export _JAVA_OPTIONS="-Xmx1024m"
export GRADLE_OPTS="-Xmx768m -Xms384m"

# アプリケーション起動
echo "Starting Spring Boot application..."
./gradlew bootRun > /tmp/logs/bootrun.log 2>&1 &

# PID を記録
echo $! > /tmp/app.pid

# 起動確認（最大60秒待機）
echo "Waiting for application to start..."
for i in {1..60}; do
  # ブートログから "Tomcat started" を確認
  if grep -q "Tomcat started on port 8080" /tmp/logs/bootrun.log 2>/dev/null; then
    echo "✅ Application started successfully"
    exit 0
  fi
  echo "Attempt $i/60..."
  sleep 1
done

echo "⚠️ Application may not have started. Check logs: docker exec springboot-todo-app tail -f /tmp/logs/bootrun.log"
exit 0
