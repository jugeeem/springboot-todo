# Java 21開発環境イメージ
FROM eclipse-temurin:21-jdk-jammy

# 開発ツールとユーティリティをインストール
RUN apt-get update && apt-get install -y \
    curl \
    git \
    build-essential \
    postgresql-client \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# 開発用ユーザーを作成
RUN useradd -m -s /bin/bash vscode

# ワークスペースディレクトリを作成し、vsCodeユーザーに所有権を付与
RUN mkdir -p /workspace && \
    chown -R vscode:vscode /workspace

# vscode ユーザーのホームディレクトリとGradleキャッシュを準備
RUN mkdir -p /home/vscode/.gradle && \
    chown -R vscode:vscode /home/vscode

# Gradle 最適化設定ファイルを作成（VS Code Server との共存対応）
RUN mkdir -p /home/vscode/.gradle && \
    echo "org.gradle.jvmargs=-Xmx768m -Xms384m" >> /home/vscode/.gradle/gradle.properties && \
    echo "org.gradle.daemon=true" >> /home/vscode/.gradle/gradle.properties && \
    echo "org.gradle.caching=true" >> /home/vscode/.gradle/gradle.properties && \
    echo "org.gradle.parallel=false" >> /home/vscode/.gradle/gradle.properties && \
    echo "org.gradle.workers.max=2" >> /home/vscode/.gradle/gradle.properties && \
    echo "org.gradle.daemon.performance.enable-monitoring=false" >> /home/vscode/.gradle/gradle.properties && \
    chown -R vscode:vscode /home/vscode/.gradle

# vsCodeユーザーに切り替え
USER vscode

# JAVA_HOMEを設定（eclipse-temurin イメージの正しいパス）
ENV JAVA_HOME=/opt/java/openjdk \
    PATH="${JAVA_HOME}/bin:${PATH}"

# Java インストール位置を検証
RUN java -version && test -d "${JAVA_HOME}" && echo "JAVA_HOME is correctly set to ${JAVA_HOME}"

WORKDIR /workspace

EXPOSE 8080 5005

# Dev Containerではインタラクティブシェルを実行し続ける
CMD ["/bin/bash"]
