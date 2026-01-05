# Spring Boot TODO API

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

タスク管理システム RESTful API - **純粋なクリーンアーキテクチャ（厳密版）** による実装

## 📋 プロジェクト概要

Spring Boot TODO APIは、**純粋なクリーンアーキテクチャ（厳密版）** を採用したタスク管理システムのRESTful APIです。Application層をPure Javaで実装し、フレームワーク依存を排除することで、高いテスト容易性とメンテナンス性を実現しています。

### 🎯 核心原則

このプロジェクトは以下の原則を **絶対厳守** します：

1. **純粋なクリーンアーキテクチャ（厳密版）**
   - Application層は **Pure Java** で実装（`@Service`, `@Transactional` 禁止）
   - トランザクション管理はInfrastructure層のServiceラッパーで実施
   - Application層はPresentation層のDTOに依存しない（Command/Result使用）

2. **依存関係の方向**
   ```
   外側の層 → 内側の層 への依存のみ許可
   内側の層 → 外側の層 への依存は絶対禁止
   ```

3. **層別責務の明確化**
   - **Domain層**: ビジネスルール（Pure Java）
   - **Application層**: ユースケースのオーケストレーション（Pure Java）
   - **Infrastructure層**: フレームワーク依存の実装（JPA、トランザクション管理）
   - **Presentation層**: REST API、DTO変換

## 🏛️ アーキテクチャ

### クリーンアーキテクチャ構造

```
┌─────────────────────────────────────────────────┐
│  Frameworks & Drivers (Presentation層)          │
│  - Controllers, REST API, Web Framework         │
├─────────────────────────────────────────────────┤
│  Interface Adapters (Infrastructure層)          │
│  - Repositories実装, JPA, トランザクション管理   │
├─────────────────────────────────────────────────┤
│  Application Business Rules (Application層)     │
│  - Use Cases (Pure Java)                        │
├─────────────────────────────────────────────────┤
│  Enterprise Business Rules (Domain層)           │
│  - Entities, Value Objects (Pure Java)          │
└─────────────────────────────────────────────────┘
```

### データフロー

```
Client Request
    ↓
Presentation層: Request DTO
    ↓
Controller: DTO → Command 変換
    ↓
Infrastructure層: Service (@Transactional)
    ↓
Application層: UseCase (Pure Java)
    ↓
Domain層: Entity/Service (ビジネスルール)
    ↓
Application層: Result
    ↓
Infrastructure層: Service (トランザクションコミット)
    ↓
Controller: Result → Response DTO 変換
    ↓
Presentation層: Response DTO
    ↓
Client Response
```

## 🛠️ 技術スタック

### コア技術

- **言語**: Java 21
- **フレームワーク**: Spring Boot 4.0.1
- **ビルドツール**: Gradle 8.x
- **データベース**: PostgreSQL 14+
- **セキュリティ**: Spring Security + OAuth2
- **コンテナ**: Docker & Docker Compose

### 主要ライブラリ

```gradle
// Spring Boot
- spring-boot-starter-security
- spring-boot-starter-security-oauth2-authorization-server
- spring-boot-starter-security-oauth2-client
- spring-boot-starter-security-oauth2-resource-server
- spring-boot-starter-webmvc
- spring-boot-starter-jdbc

// データベース
- postgresql

// ユーティリティ
- lombok

// テスト
- spring-boot-starter-test
```

## 📁 プロジェクト構造

```
api/
├── src/main/java/com/api/todos/
│   ├── SpringbootTodosApplication.java    # アプリケーションエントリーポイント
│   │
│   ├── domain/                             # Domain層（Pure Java）
│   │   ├── model/                          # エンティティ・値オブジェクト
│   │   ├── repository/                     # リポジトリインターフェース
│   │   └── service/                        # ドメインサービス
│   │
│   ├── application/                        # Application層（Pure Java）
│   │   ├── command/                        # Commandオブジェクト（入力）
│   │   ├── dto/                            # Resultオブジェクト（出力）
│   │   └── usecase/                        # ユースケース
│   │
│   ├── infrastructure/                     # Infrastructure層（Spring依存）
│   │   ├── config/                         # 設定クラス（UseCaseConfig等）
│   │   ├── persistence/                    # データベースアクセス
│   │   │   ├── entity/                     # JPA Entity
│   │   │   └── repository/                 # Repository実装
│   │   ├── security/                       # セキュリティ設定
│   │   └── service/                        # トランザクション管理ラッパー
│   │
│   └── presentation/                       # Presentation層（REST API）
│       ├── dto/                            # Request/Response DTO
│       └── rest/                           # Controller
│
├── src/main/resources/
│   └── application.properties              # アプリケーション設定
│
├── config/
│   ├── checkstyle/                         # Checkstyle設定
│   └── spotless/                           # コードフォーマット設定
│
└── build.gradle                            # Gradle設定
```

## 🚀 セットアップ

### 前提条件

- **Java 21** (OpenJDK推奨)
- **Docker** & **Docker Compose**
- **Gradle 8.x**（Gradle Wrapper使用可）

### 1. リポジトリのクローン

```bash
git clone https://github.com/jugeeem/springboot-todo.git
cd springboot-todo
```

### 2. データベースの起動

```bash
docker-compose up -d
```

PostgreSQL 14がポート5432で起動します。

### 3. データベース初期化

```bash
# DDLスクリプトの実行
docker exec -i postgres-db psql -U postgres -d todos_db < db/ddl.sql
```

### 4. アプリケーションのビルド

```bash
cd api
./gradlew build
```

### 5. アプリケーションの起動

```bash
./gradlew bootRun
```

アプリケーションは `http://localhost:8080` で起動します。

## 🧪 開発

### コード品質チェック

コードを追加・変更した後は、**必ず以下のコマンドを実行**してください：

```bash
./gradlew quality
```

このコマンドは以下を実行します：
- `spotlessCheck` - コードフォーマットのチェック
- `checkstyleMain` - メインコードの静的解析
- `checkstyleTest` - テストコードの静的解析

### コードフォーマット

フォーマットエラーが検出された場合は、以下で自動修正できます：

```bash
./gradlew spotlessApply
```

### テストの実行

```bash
# 全テスト実行
./gradlew test

# 特定のテストクラスを実行
./gradlew test --tests "クラス名"
```

### ビルド

```bash
# クリーンビルド
./gradlew clean build

# テストをスキップしてビルド
./gradlew build -x test
```

## 📚 APIドキュメント

詳細なAPI仕様は以下を参照してください：

- [認証API](.docs/api/AUTH.md) - ユーザー登録、ログイン、ログアウト
- [ユーザー管理API](.docs/api/USERS.md) - ユーザー一覧、詳細取得、更新、削除
- [TODO管理API](.docs/api/TODOS.md) - TODO一覧、作成、詳細取得、更新、削除

## 🏗️ 実装パターン

### トランザクション管理パターン

このプロジェクトでは、Application層をPure Javaに保つため、トランザクション管理をInfrastructure層で実施します：

```java
// Application層: Pure Java UseCase
public class CreateTodoUseCase {
    private final TodoRepository todoRepository;
    
    public CreateTodoUseCase(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }
    
    public TodoResult execute(CreateTodoCommand command) {
        // ビジネスフロー実装（@Transactionalなし）
    }
}

// Infrastructure層: トランザクション管理ラッパー
@Service
public class CreateTodoService {
    private final CreateTodoUseCase useCase;
    
    @Transactional
    public TodoResult execute(CreateTodoCommand command) {
        return useCase.execute(command);
    }
}
```

### Command/Resultパターン

Application層はPresentation層のDTOに依存せず、独自のCommand/Resultオブジェクトを使用します：

```java
// Presentation層: DTO
public class CreateTodoRequest {
    private String title;
    private String descriptions;
}

// Application層: Command
public class CreateTodoCommand {
    private final String title;
    private final String descriptions;
    private final UUID userId;
}

// Application層: Result
public class TodoResult {
    private final UUID id;
    private final String title;
    private final String descriptions;
    // ...
}

// Controller: DTO ⇔ Command/Result 変換
@PostMapping
public ResponseEntity<TodoResponse> createTodo(
    @RequestBody CreateTodoRequest request,
    @RequestHeader("x-user-id") UUID userId
) {
    // DTO → Command
    CreateTodoCommand command = new CreateTodoCommand(
        request.getTitle(),
        request.getDescriptions(),
        userId
    );
    
    // UseCase実行
    TodoResult result = createTodoService.execute(command);
    
    // Result → DTO
    TodoResponse response = TodoResponse.from(result);
    return ResponseEntity.ok(response);
}
```

## 🔒 セキュリティ

- **JWT認証**: Spring Security + JWT
- **ロールベースアクセス制御**: ADMIN、MANAGER、USER
- **パスワードハッシュ化**: BCrypt
- **SQLインジェクション対策**: パラメータバインディング

## 📖 ドキュメント

詳細なアーキテクチャドキュメントとAIエージェント向けガイドラインは以下を参照：

- [AGENTS.md](AGENTS.md) - AIエージェント向け包括的コンテキストドキュメント

## 🤝 コントリビューション

1. このリポジトリをフォーク
2. フィーチャーブランチを作成 (`git checkout -b feature/amazing-feature`)
3. 変更をコミット (`git commit -m 'feat: 素晴らしい機能を追加'`)
4. ブランチにプッシュ (`git push origin feature/amazing-feature`)
5. プルリクエストを作成

### コミットメッセージ規約

Conventional Commits形式（日本語）を使用：

```
<type>: <subject>

type:
- feat: 新機能
- fix: バグ修正
- docs: ドキュメント
- style: コードフォーマット
- refactor: リファクタリング
- test: テスト追加・修正
- chore: ビルド・補助ツール
```

## 📝 ライセンス

このプロジェクトはMITライセンスの下で公開されています。

## 👥 Authors

- **jugeeem** - [GitHub Profile](https://github.com/jugeeem)

## 🙏 謝辞

このプロジェクトは以下の原則に基づいて設計されています：

- [Clean Architecture (Robert C. Martin)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Spring Boot Best Practices](https://spring.io/projects/spring-boot)

---

**Version**: v0.0.1-SNAPSHOT  
**Last Updated**: 2026年1月5日