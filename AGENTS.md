# Spring Boot TODO API - AIエージェント向けコンテキストドキュメント

このドキュメントは、Spring Boot TODO APIプロジェクトで作業するAIエージェント（GitHub Copilot、Claude、その他のコーディングアシスタント）が効率的で一貫性のあるコード生成を行うための包括的なコンテキストと指針を提供します。

## 📋 プロジェクト概要

**プロジェクト名**: Spring Boot TODO API - タスク管理システム RESTful API  
**アーキテクチャ**: Java Spring Boot 4.0 + PostgreSQL / **純粋なクリーンアーキテクチャ（厳密版）**  
**現在のバージョン**: v0.0.1-SNAPSHOT (開発開始)  
**メインブランチ**: `main`  
**リポジトリ**: jugeeem/springboot-todo  

### 🎯 核心原則（絶対厳守）

1. **日本語コミュニケーション**: 開発者とのやり取りは **必ず日本語** で行う
2. **純粋なクリーンアーキテクチャ（厳密版）**: この設計原則は **如何なる理由があろうと破ることを許可しない**
3. **日本語コミットメッセージ**: Git コミットメッセージは **可能な限り日本語** で記述する
4. **GitHub操作優先順位**: 1. GitHub MCP → 2. GitHub CLI → 3. Markdown

### 📐 クリーンアーキテクチャ実装ポリシー

このプロジェクトは **厳密なクリーンアーキテクチャ** を実装しています：

#### ✅ 採用した厳密な原則

1. **Application層はPure Java**
   - Springアノテーション（`@Service`, `@Transactional`）は使用しない
   - フレームワークに一切依存しない純粋なビジネスロジック
   
2. **Application層はPresentation層に依存しない**
   - Presentation層のDTOは使用しない
   - 代わりにApplication層専用の`Command`/`Query`オブジェクトを使用
   - 結果もApplication層の`Result`オブジェクトで返却
   
3. **トランザクション管理はInfrastructure層**
   - UseCaseをラップする`@Service`クラスを作成
   - `@Transactional`アノテーションはこのラッパークラスで管理
   
4. **依存性注入（DI）設定はInfrastructure層**
   - `UseCaseConfig`でUseCaseインスタンスをBean登録
   - SpringのDIコンテナ管理はInfrastructure層の責務

#### 📁 実装パターン

```
Application層（Pure Java）:
├── command/          # Commandオブジェクト（入力）
│   ├── auth/
│   └── todo/
├── dto/              # Resultオブジェクト（出力）
└── usecase/          # Pure JavaのUseCase（@Serviceなし）
    ├── auth/
    └── todo/

Infrastructure層（Spring依存）:
├── config/
│   └── UseCaseConfig.java        # UseCaseのDI設定
└── service/                       # トランザクション管理ラッパー
    ├── InitializePasswordService.java
    ├── GenerateJwtTokenService.java
    └── CreateTodoService.java

Presentation層（REST API）:
└── rest/
    ├── AuthController.java       # DTO → Command変換
    └── TodoController.java       # Result → DTO変換
```

#### 🔄 データフロー

```
1. Client → Presentation層のDTO
   ↓
2. Controller: DTO → Application層のCommand変換
   ↓
3. Infrastructure層のService（@Transactional）
   ↓
4. Application層のUseCase実行（Pure Java）
   ↓
5. Domain層のビジネスロジック実行
   ↓
6. Application層のResult返却
   ↓
7. Controller: Result → Presentation層のDTO変換
   ↓
8. Presentation層のDTO → Client
```

## 🛠️ 技術スタック

### コア技術
```
言語: Java 21
フレームワーク: Spring Boot 4.0.1
ビルドツール: Gradle 8.x
セキュリティ: Spring Security + OAuth2
データベース: PostgreSQL 14+
```

### 主要ライブラリ・依存関係
```gradle
// Spring Boot Starter
- spring-boot-starter-security (セキュリティ)
- spring-boot-starter-security-oauth2-authorization-server (OAuth2サーバー)
- spring-boot-starter-security-oauth2-client (OAuth2クライアント)
- spring-boot-starter-security-oauth2-resource-server (リソースサーバー)
- spring-boot-starter-webmvc (REST API)
- spring-boot-starter-jdbc (データベース)

// データベース
- postgresql (PostgreSQL JDBCドライバー)

// ユーティリティ
- lombok (ボイラープレートコード削減)

// テスト
- spring-boot-starter-test (統合テストフレームワーク)
```

### インフラストラクチャ  

```yaml
開発環境: VS Code
コンテナ: Docker & Docker Compose
データベース: PostgreSQL 14+
APIポート: 8080
Java Runtime: OpenJDK 21
```

## 🏛️ 純粋なクリーンアーキテクチャ（必須遵守）

### アーキテクチャの絶対原則

このプロジェクトは **純粋なクリーンアーキテクチャ** を採用しています。以下の原則は **如何なる理由があろうと破ることを許可しません**。

#### 依存関係の方向（The Dependency Rule）

```
外側の層 → 内側の層 への依存のみ許可
内側の層 → 外側の層 への依存は絶対禁止
```

```
┌─────────────────────────────────────────────────┐
│  Frameworks & Drivers (Presentation層)          │ 最外層
│  - Controllers, REST API, Web Framework         │
├─────────────────────────────────────────────────┤
│  Interface Adapters (Infrastructure層)          │ 
│  - Presenters, Gateways, Repositories実装       │
├─────────────────────────────────────────────────┤
│  Application Business Rules (Application層)     │
│  - Use Cases, Application Services              │
├─────────────────────────────────────────────────┤
│  Enterprise Business Rules (Domain層)           │ 最内層
│  - Entities, Value Objects, Domain Services     │
└─────────────────────────────────────────────────┘
```

### 🎯 層別責務定義

#### 1. Domain層（最内層・最も重要）

**場所**: `api/src/main/java/com/todos/domain/`

**責務**:
- ビジネスルール・ドメインロジックの集約
- エンティティ（Entity）の定義
- 値オブジェクト（Value Object）の定義
- ドメインサービス（Domain Service）の定義
- リポジトリインターフェース（Repository Interface）の定義

**依存関係**:
- ✅ 許可: **いかなる外部ライブラリにも依存しない** (Pure Java)
- ❌ 禁止: Spring、JPA、データベース、外部フレームワークへの依存

**実装例 - TODOエンティティ**:
```java
// api/src/main/java/com/todos/domain/model/Todo.java
package com.todos.domain.model;

import java.util.UUID;
import java.time.LocalDateTime;

/**
 * TODOエンティティ
 * Pure Javaで実装 - フレームワーク依存なし
 */
public class Todo {
    private final UUID id;
    private String title;
    private String descriptions;
    private boolean completed;
    private final UUID userId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;
    
    // ビジネスルール: TODO完了状態の変更
    public void markAsCompleted() {
        if (this.completed) {
            throw new IllegalStateException("既に完了済みのTODOです");
        }
        this.completed = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    // ビジネスルール: TODO未完了状態に戻す
    public void markAsIncomplete() {
        if (!this.completed) {
            throw new IllegalStateException("未完了のTODOです");
        }
        this.completed = false;
        this.updatedAt = LocalDateTime.now();
    }
    
    // ビジネスルール: TODO削除（論理削除）
    public void delete() {
        if (this.deleted) {
            throw new IllegalStateException("既に削除済みのTODOです");
        }
        this.deleted = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    // getter, constructor, equals, hashCode
}
```

**実装例 - Userエンティティ**:
```java
// api/src/main/java/com/todos/domain/model/User.java
package com.todos.domain.model;

import java.util.UUID;
import java.time.LocalDateTime;

/**
 * ユーザーエンティティ
 * Pure Javaで実装
 */
public class User {
    private final UUID id;
    private String username;
    private String email;
    private String hashedPassword;
    private String firstName;
    private String lastName;
    private int role;
    private boolean passwordInitialized;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;
    
    // ビジネスルール: パスワード初期化確認
    public boolean isPasswordInitialized() {
        return this.passwordInitialized;
    }
    
    // getter, constructor, equals, hashCode
}
```

**実装例 - リポジトリインターフェース**:
```java
// api/src/main/java/com/todos/domain/repository/TodoRepository.java
package com.todos.domain.repository;

import com.todos.domain.model.Todo;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

/**
 * TODOリポジトリインターフェース
 * Domain層で定義、Infrastructure層で実装（依存性逆転の原則）
 */
public interface TodoRepository {
    Optional<Todo> findById(UUID id);
    List<Todo> findByUserId(UUID userId);
    Todo save(Todo todo);
    void delete(UUID id);
}
```

#### 2. Application層（ユースケース層）

**場所**: `api/src/main/java/com/todos/application/`

**責務**:
- ユースケース（Use Case）の実装（Pure Java）
- アプリケーション層専用のCommand/Queryオブジェクト定義
- アプリケーション層専用のResultオブジェクト定義
- ドメインオブジェクトのオーケストレーション

**依存関係**:
- ✅ 許可: Domain層のみ
- ❌ 禁止: Infrastructure層、Presentation層への依存
- ❌ 禁止: Spring、JPA等のフレームワークアノテーション

**実装例 - Commandオブジェクト**:
```java
// api/src/main/java/com/todos/application/command/todo/CreateTodoCommand.java
package com.todos.application.command.todo;

import java.util.UUID;

/**
 * TODO作成コマンド（Application層専用）
 */
public class CreateTodoCommand {
    private final String title;
    private final String descriptions;
    private final UUID userId;

    public CreateTodoCommand(String title, String descriptions, UUID userId) {
        this.title = title;
        this.descriptions = descriptions;
        this.userId = userId;
    }

    public String getTitle() { return title; }
    public String getDescriptions() { return descriptions; }
    public UUID getUserId() { return userId; }
}
```

**実装例 - Resultオブジェクト**:
```java
// api/src/main/java/com/todos/application/dto/TodoResult.java
package com.todos.application.dto;

import java.util.UUID;
import java.time.LocalDateTime;

/**
 * TODO結果オブジェクト（Application層専用）
 */
public class TodoResult {
    private final UUID id;
    private final String title;
    private final String descriptions;
    private final boolean completed;
    private final UUID userId;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final boolean deleted;

    public TodoResult(UUID id, String title, String descriptions, boolean completed,
                     UUID userId, LocalDateTime createdAt, LocalDateTime updatedAt, boolean deleted) {
        this.id = id;
        this.title = title;
        this.descriptions = descriptions;
        this.completed = completed;
        this.userId = userId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
    }

    // getters
}
```

**実装例 - UseCase（Pure Java）**:
```java
// api/src/main/java/com/todos/application/usecase/todo/CreateTodoUseCase.java
package com.todos.application.usecase.todo;

import com.todos.application.command.todo.CreateTodoCommand;
import com.todos.application.dto.TodoResult;
import com.todos.domain.model.Todo;
import com.todos.domain.repository.TodoRepository;
import com.todos.domain.repository.UserRepository;

/**
 * TODO作成ユースケース（Pure Java）
 * 注意: @Service、@Transactionalアノテーションは使用しない
 */
public class CreateTodoUseCase {
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;

    public CreateTodoUseCase(TodoRepository todoRepository, UserRepository userRepository) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
    }

    /**
     * 新しいTODOを作成する
     * @param command TODO作成コマンド（Application層専用）
     * @return TODO結果オブジェクト（Application層専用）
     */
    public TodoResult execute(CreateTodoCommand command) {
        // ユーザーの存在確認
        userRepository.findById(command.getUserId())
                .orElseThrow(() -> new UserNotFoundException("ユーザーが見つかりません"));

        // Todoドメインモデルの作成
        Todo todo = new Todo(
            command.getTitle(),
            command.getDescriptions(),
            command.getUserId()
        );

        // リポジトリに保存
        Todo savedTodo = todoRepository.save(todo);

        // Resultオブジェクトに変換して返却
        return new TodoResult(
            savedTodo.getId(),
            savedTodo.getTitle(),
            savedTodo.getDescriptions(),
            savedTodo.isCompleted(),
            savedTodo.getUserId(),
            savedTodo.getCreatedAt(),
            savedTodo.getUpdatedAt(),
            savedTodo.isDeleted()
        );
    }
}
```

#### 3. Infrastructure層（インフラストラクチャ層）

**場所**: `api/src/main/java/com/todos/infrastructure/`

**責務**:
- データベースアクセスの実装（JPA Entity、Repository実装）
- 外部サービス連携
- フレームワーク固有の実装
- リポジトリインターフェースの具象クラス
- セキュリティインフラ（JWT認証、暗号化等）

**依存関係**:
- ✅ 許可: Domain層、Application層への依存
- ✅ 許可: Spring、JPA、PostgreSQL等のフレームワーク依存
- ❌ 禁止: Presentation層への依存

**実装例 - JPA Entity**:
```java
// api/src/main/java/com/todos/infrastructure/persistence/entity/TodoJpaEntity.java
package com.todos.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.LocalDateTime;

/**
 * JPA用TODOエンティティ（永続化専用）
 * Domain層のTodoエンティティとは分離
 */
@Entity
@Table(name = "todos", schema = "public")
public class TodoJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, length = 32)
    private String title;
    
    @Column(length = 128)
    private String descriptions;
    
    @Column(nullable = false)
    private boolean completed;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(nullable = false)
    private boolean deleted;
    
    // getter, setter, constructor
}
```

**実装例 - Repository実装**:
```java
// api/src/main/java/com/todos/infrastructure/persistence/repository/TodoRepositoryImpl.java
package com.todos.infrastructure.persistence.repository;

import com.todos.domain.model.Todo;
import com.todos.domain.repository.TodoRepository;
import com.todos.infrastructure.persistence.entity.TodoJpaEntity;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TodoRepositoryインターフェースの実装
 * Domain層で定義されたインターフェースをInfrastructure層で実装
 */
@Repository
public class TodoRepositoryImpl implements TodoRepository {
    private final TodoJpaRepository jpaRepository;
    
    public TodoRepositoryImpl(TodoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public Optional<Todo> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(this::toDomainModel);
    }
    
    @Override
    public List<Todo> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
            .map(this::toDomainModel)
            .collect(Collectors.toList());
    }
    
    @Override
    public Todo save(Todo todo) {
        TodoJpaEntity entity = toJpaEntity(todo);
        TodoJpaEntity saved = jpaRepository.save(entity);
        return toDomainModel(saved);
    }
    
    @Override
    public void delete(UUID id) {
        jpaRepository.deleteById(id);
    }
    
    // JPA Entity ⇔ Domain Model の変換
    private Todo toDomainModel(TodoJpaEntity entity) {
        // 変換ロジック実装
        return new Todo(/* 引数 */);
    }
    
    private TodoJpaEntity toJpaEntity(Todo todo) {
        // 変換ロジック実装
        TodoJpaEntity entity = new TodoJpaEntity();
        // マッピング
        return entity;
    }
}
```

**実装例 - UseCase設定**:
```java
// api/src/main/java/com/todos/infrastructure/config/UseCaseConfig.java
package com.todos.infrastructure.config;

import com.todos.application.usecase.todo.CreateTodoUseCase;
import com.todos.domain.repository.TodoRepository;
import com.todos.domain.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application層のUseCaseをDI管理する設定クラス
 * Pure JavaのUseCaseをSpring DIコンテナに登録
 */
@Configuration
public class UseCaseConfig {
    @Bean
    public CreateTodoUseCase createTodoUseCase(
            TodoRepository todoRepository,
            UserRepository userRepository
    ) {
        return new CreateTodoUseCase(todoRepository, userRepository);
    }
}
```

**実装例 - Service（トランザクション管理ラッパー）**:
```java
// api/src/main/java/com/todos/infrastructure/service/CreateTodoService.java
package com.todos.infrastructure.service;

import com.todos.application.command.todo.CreateTodoCommand;
import com.todos.application.dto.TodoResult;
import com.todos.application.usecase.todo.CreateTodoUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TODO作成サービス（Infrastructure層のトランザクション管理ラッパー）
 * Pure JavaのUseCaseをラップし、Springのトランザクション管理を適用
 */
@Service
public class CreateTodoService {
    private final CreateTodoUseCase useCase;

    public CreateTodoService(CreateTodoUseCase useCase) {
        this.useCase = useCase;
    }

    @Transactional
    public TodoResult execute(CreateTodoCommand command) {
        return useCase.execute(command);
    }
}
```

#### 4. Presentation層（プレゼンテーション層）

**場所**: `api/src/main/java/com/todos/presentation/`

**責務**:
- REST APIエンドポイントの公開（Controller）
- HTTPリクエスト・レスポンスのハンドリング
- DTOの変換（Domain Model ⇔ DTO）
- バリデーション
- 認証・認可の適用

**依存関係**:
- ✅ 許可: Domain層、Application層、Infrastructure層への依存
- ✅ 許可: Spring Web、Jackson等のフレームワーク依存

**実装例 - リクエストDTO**:
```java
// api/src/main/java/com/todos/presentation/dto/CreateTodoRequest.java
package com.todos.presentation.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * TODO作成リクエストDTO（Presentation層）
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateTodoRequest {
    private String title;
    private String descriptions;
}
```

**実装例 - レスポンスDTO**:
```java
// api/src/main/java/com/todos/presentation/dto/TodoResponse.java
package com.todos.presentation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import com.todos.application.dto.TodoResult;
import java.util.UUID;
import java.time.LocalDateTime;

/**
 * TODOレスポンスDTO（Presentation層）
 */
@Getter
@NoArgsConstructor
public class TodoResponse {
    private UUID id;
    private String title;
    private String descriptions;
    private boolean completed;
    private UUID userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;

    public static TodoResponse from(TodoResult result) {
        TodoResponse response = new TodoResponse();
        response.id = result.getId();
        response.title = result.getTitle();
        response.descriptions = result.getDescriptions();
        response.completed = result.isCompleted();
        response.userId = result.getUserId();
        response.createdAt = result.getCreatedAt();
        response.updatedAt = result.getUpdatedAt();
        response.deleted = result.isDeleted();
        return response;
    }
}
```

**実装例 - Controller**:
```java
// api/src/main/java/com/todos/presentation/rest/TodoController.java
package com.todos.presentation.rest;

import com.todos.application.command.todo.CreateTodoCommand;
import com.todos.application.dto.TodoResult;
import com.todos.infrastructure.service.CreateTodoService;
import com.todos.presentation.dto.CreateTodoRequest;
import com.todos.presentation.dto.TodoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/**
 * TODOコントローラー
 * 
 * クリーンアーキテクチャの原則に従い：
 * 1. Presentation層のDTOを受け取る
 * 2. Application層のCommandに変換する
 * 3. Infrastructure層のサービス経由でUseCaseを実行（トランザクション管理）
 * 4. Application層の結果をPresentation層のDTOに変換して返却
 */
@RestController
@RequestMapping("/api/todos")
public class TodoController {
    private final CreateTodoService createTodoService;
    
    public TodoController(CreateTodoService createTodoService) {
        this.createTodoService = createTodoService;
    }
    
    @PostMapping
    public ResponseEntity<TodoResponse> createTodo(
        @RequestBody CreateTodoRequest request,
        @RequestHeader("x-user-id") UUID userId
    ) {
        // 1. Presentation層のDTOをApplication層のCommandに変換
        CreateTodoCommand command = new CreateTodoCommand(
            request.getTitle(),
            request.getDescriptions(),
            userId
        );
        
        // 2. Infrastructure層のサービス経由でUseCase実行
        TodoResult result = createTodoService.execute(command);
        
        // 3. Application層の結果をPresentation層のDTOに変換
        TodoResponse response = TodoResponse.from(result);
        
        return ResponseEntity.ok(response);
    }
}
```

### 🚨 絶対禁止事項

以下のパターンは **絶対に実装してはいけません**：

#### ❌ 1. Domain層からFrameworkへの依存
```java
// ❌ 絶対禁止: Domain層でSpringアノテーションを使用
package com.todos.domain.model;

import org.springframework.stereotype.Component;

@Component // ❌ Domain層でSpring依存は禁止
public class Todo {
    // ...
}
```

#### ❌ 2. Domain層でJPAアノテーションを使用
```java
// ❌ 絶対禁止: Domain層でJPAアノテーションを使用
package com.todos.domain.model;

import jakarta.persistence.Entity;

@Entity // ❌ Domain層でJPA依存は禁止
public class Todo {
    // ...
}
```

#### ❌ 3. Infrastructure層の実装詳細をApplication層で参照
```java
// ❌ 絶対禁止: UseCaseでJPA Repositoryを直接使用
package com.todos.application.usecase;

import com.todos.infrastructure.persistence.repository.TodoJpaRepository;

public class CreateTodoUseCase {
    private final TodoJpaRepository jpaRepository; // ❌ Infrastructure層の実装に依存
    
    // 正しくはDomain層のTodoRepositoryインターフェースに依存すべき
}
```

#### ❌ 4. ドメインモデルを直接レスポンスとして返却
```java
// ❌ 絶対禁止: ドメインモデルを直接REST APIで返却
@GetMapping("/{id}")
public ResponseEntity<Todo> getTodo(@PathVariable UUID id) {
    Todo todo = getTodoUseCase.execute(id);
    return ResponseEntity.ok(todo); // ❌ ドメインモデルを直接返却
}

// ✅ 正しい実装: DTOに変換して返却
@GetMapping("/{id}")
public ResponseEntity<TodoResponse> getTodo(@PathVariable UUID id) {
    TodoResult result = getTodoUseCase.execute(id);
    TodoResponse response = TodoResponse.from(result);
    return ResponseEntity.ok(response); // ✅ DTOに変換して返却
}
```

## 📚 API仕様

### 提供されるAPIドキュメント

以下のドキュメントを参照してください：

1. **[AUTH.md](.docs/api/AUTH.md)** - 認証関連API（ユーザー登録、ログイン、ログアウト）
2. **[USERS.md](.docs/api/USERS.md)** - ユーザー管理API（ユーザー一覧、詳細取得、更新、削除）
3. **[TODOS.md](.docs/api/TODOS.md)** - TODO管理API（一覧、作成、詳細取得、更新、削除）

### API設計原則

- **RESTful**: 標準的なHTTP メソッドとステータスコードを使用
- **JSON**: リクエスト・レスポンスはJSON形式
- **認証**: JWTトークンによる認証
- **ページネーション**: TODO一覧など大量データには対応
- **エラーハンドリング**: 統一されたエラーレスポンス形式

## 🔒 セキュリティ要件

### 認証・認可

- **JWT認証**: Spring Security + JWT
- **ロールベースアクセス制御（RBAC）**: ユーザーロール管理
  - ADMIN（管理者）
  - MANAGER（マネージャー）
  - USER（一般ユーザー）

### パスワード管理

```java
// ✅ 正しい実装: BCryptでハッシュ化
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hashedPassword = encoder.encode(rawPassword);
```

### SQLインジェクション対策

```java
// ✅ 正しい実装: JPQLでパラメータバインディング
@Query("SELECT t FROM TodoJpaEntity t WHERE t.userId = :userId")
List<TodoJpaEntity> findByUserId(@Param("userId") UUID userId);

// ❌ 絶対禁止: 文字列連結によるクエリ構築
@Query("SELECT t FROM TodoJpaEntity t WHERE t.userId = '" + userId + "'") // SQL Injection脆弱性
```

## 📝 コーディング規約

### Java命名規則

```java
// クラス名: PascalCase
public class TodoService { }

// メソッド名: camelCase
public TodoResult createTodo() { }

// 変数名: camelCase
private String title;

// 定数: UPPER_SNAKE_CASE
public static final String API_VERSION = "v1";

// パッケージ名: 小文字、ドット区切り
package com.todos.domain.model;
```

### パッケージ構成

```
com/todos/
├── domain/                      # Domain層
│   ├── model/                   # エンティティ・値オブジェクト
│   ├── repository/              # リポジトリインターフェース
│   └── service/                 # ドメインサービス
├── application/                 # Application層
│   ├── command/                 # Commandオブジェクト（入力）
│   ├── dto/                     # Resultオブジェクト（出力）
│   └── usecase/                 # ユースケース（Pure Java）
├── infrastructure/              # Infrastructure層
│   ├── persistence/             # データベースアクセス
│   │   ├── entity/             # JPA Entity
│   │   ├── repository/         # Repository実装
│   │   └── mapper/             # Domain Model ⇔ JPA Entity 変換
│   ├── config/                  # 設定クラス
│   ├── security/                # セキュリティインフラ（JWT等）
│   └── service/                 # トランザクション管理ラッパー
└── presentation/                # Presentation層
    ├── rest/                    # REST Controller
    ├── dto/                     # Data Transfer Object
    └── exception/               # 例外ハンドラー
```

### Lombok使用ガイドライン

```java
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// Presentation層での使用は推奨
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TodoResponse {
    private UUID id;
    private String title;
    private String descriptions;
}

// Domain層では極力使用しない（Pure Javaを優先）
```

## 🧪 テスト戦略

### テストの層別方針

```java
// Domain層: Pure Javaのユニットテスト
class TodoTest {
    @Test
    void shouldMarkTodoAsCompletedSuccessfully() {
        Todo todo = new Todo("Test Todo", "Description", userId);
        todo.markAsCompleted();
        assertTrue(todo.isCompleted());
    }
}

// Application層: UseCase単体テスト（モック使用）
@ExtendWith(MockitoExtension.class)
class CreateTodoUseCaseTest {
    @Mock
    private TodoRepository todoRepository;
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private CreateTodoUseCase useCase;
    
    @Test
    void shouldCreateTodoSuccessfully() {
        // モックを使ったテスト
    }
}

// Presentation層: Controller統合テスト
@WebMvcTest(TodoController.class)
class TodoControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CreateTodoService createTodoService;
    
    @Test
    void shouldReturnCreatedTodo() throws Exception {
        mockMvc.perform(post("/api/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Test\",\"descriptions\":\"test\"}")
            .header("x-user-id", "user-123"))
            .andExpect(status().isOk());
    }
}
```

## 🐙 GitHub運用ルール

### GitHub操作の優先順位

1. **GitHub MCP（最優先）**: Model Context Protocol経由でのGitHub操作
2. **GitHub CLI（代替）**: `gh` コマンドによる操作
3. **Markdownファイル生成（最終手段）**: 手動コピー&ペースト用

### Git コミットメッセージ規約

**原則**: 可能な限り日本語で記述

```bash
# ✅ 推奨: 日本語でのコミットメッセージ
git commit -m "feat: ユーザー認証機能を追加"
git commit -m "fix: TODO削除エラーを修正"
git commit -m "docs: APIドキュメントを更新"

# Conventional Commits形式（日本語）
<type>: <subject>

# type一覧
feat: 新機能
fix: バグ修正
docs: ドキュメント
style: コードフォーマット
refactor: リファクタリング
test: テスト追加・修正
chore: ビルド・補助ツール
```

## 🎯 AIエージェントへの指針

### コード生成時の必須チェックリスト

#### 1. クリーンアーキテクチャ遵守
- [ ] 適切な層（domain/application/infrastructure/presentation）にコード配置
- [ ] 依存関係の方向が正しい（外→内のみ）
- [ ] Domain層にフレームワーク依存がない（Pure Java）
- [ ] Infrastructure層で依存性逆転の原則を実装

#### 2. Domain層実装
- [ ] エンティティはPure Javaで実装
- [ ] ビジネスルールはドメインモデル内に実装
- [ ] リポジトリはインターフェースのみ定義
- [ ] Spring/JPA/Lombokアノテーションを使用していない

#### 3. Infrastructure層実装
- [ ] JPA Entityはドメインモデルと分離
- [ ] Repository実装でドメインインターフェースを実装
- [ ] Domain Model ⇔ JPA Entity の変換ロジック実装
- [ ] トランザクション管理は適切な境界で実施

#### 4. API設計
- [ ] ドメインモデルを直接レスポンスしない
- [ ] DTOに変換してからレスポンス
- [ ] 適切なHTTPステータスコード
- [ ] バリデーション実装

#### 5. セキュリティ
- [ ] SQLインジェクション対策（パラメータバインディング）
- [ ] パスワードのハッシュ化（BCrypt）
- [ ] 認証・認可の実装
- [ ] 機密情報のログ出力除外

### 推奨する実装順序

1. **Domain層**: Entity → Value Object → Repository Interface → Domain Service
2. **Application層**: Command/Query → Use Case → Result/DTO
3. **Infrastructure層**: JPA Entity → Repository Implementation → Mapper → UseCase設定
4. **Presentation層**: DTO → Controller
5. **テスト**: Domain Test → Use Case Test → Controller Test

### 質問対応ガイドライン

開発者から質問があった場合：

1. **必ず日本語で回答する**
2. **クリーンアーキテクチャの原則に照らして回答する**
3. **具体的なコード例を提示する**
4. **依存関係の方向を明示する**
5. **なぜそうすべきかの理由を説明する**

## 🔗 参考ドキュメント

### プロジェクト内ドキュメント
1. **[README.md](./README.md)** - プロジェクト全体概要
2. **[AUTH.md](.docs/api/AUTH.md)** - 認証API仕様
3. **[USERS.md](.docs/api/USERS.md)** - ユーザー管理API仕様
4. **[TODOS.md](.docs/api/TODOS.md)** - TODO管理API仕様

### 外部参考資料
- [Clean Architecture (Robert C. Martin)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Spring Boot 公式ドキュメント](https://spring.io/projects/spring-boot)
- [Spring Data JDBC リファレンス](https://spring.io/projects/spring-data-jdbc)
- [Spring Security 公式ドキュメント](https://spring.io/projects/spring-security)

## 📞 開発支援

開発中に不明な点がある場合：

1. **アーキテクチャ原則**: このドキュメントの「純粋なクリーンアーキテクチャ」セクション参照
2. **API仕様**: `.docs/api/` ディレクトリ内のドキュメント参照
3. **実装パターン**: このドキュメントの「正しい実装パターン」参照
4. **GitHub運用**: このドキュメントの「GitHub運用ルール」参照

---

**作成日**: 2025年1月4日  
**対象バージョン**: Spring Boot TODO API v0.0.1-SNAPSHOT  
**対象ブランチ**: `main`（メインブランチ）  

このドキュメントは、AIエージェントがSpring Boot TODO APIプロジェクトで **純粋なクリーンアーキテクチャの原則を厳格に遵守** した高品質なコードを生成するための包括的なガイドです。依存関係の方向、層別責務、実装パターンを必ず遵守し、一貫性のある実装を行ってください。
