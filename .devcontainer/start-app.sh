#!/bin/bash

# Spring Boot アプリケーション起動スクリプト（非推奨）
# 注意：このスクリプトはもはや使用されていません。
# 代わりに docker-compose.yml で直接 gradlew bootRun を実行してください。
#
# 理由：
# 1. バックグラウンド実行によりコンテナが終了してしまう
# 2. ログがコンテナコンソールに出力されない
# 3. 起動確認の信頼性が低い
#
# 新しい起動方法：
# docker-compose.yml の app サービスで直接実行：
# command: bash -c "cd /workspace/api && ./gradlew bootRun"

echo "Warning: This script is deprecated. Use 'docker-compose up' to start the app."
exit 0
