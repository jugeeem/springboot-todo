# Application層 - usecase パッケージ（ユースケース）

## 📋 パッケージ概要

**場所**: `api/src/main/java/com/api/todos/application/usecase/`

**目的**: **ユースケース（Use Case）を Pure Java で実装**するパッケージです。UseCaseは、アプリケーション固有のビジネスルールを実装し、Domain層のエンティティとリポジトリをオーケストレーションして、具体的なユースケースを実現します。

**主要コンポーネント**:
- **Todo UseCases**: TODO関連のユースケース（作成、更新、削除、取得等）
- **User UseCases**: ユーザー関連のユースケース（登録、更新、削除等）
- **Auth UseCases**: 認証関連のユースケース（パスワード初期化、JWT生成等）

---

## 🏛️ クリーンアーキテクチャにおける位置づけ

```
┌─────────────────────────────────────────────────┐
│  Presentation層 (Controllers, REST API)         │
│     ↓ Commandオブジェクト渡す                   │
├─────────────────────────────────────────────────┤
│  Infrastructure層 (@Service + @Transactional)   │
│     ↓ UseCaseラッパー                           │
├─────────────────────────────────────────────────┤
│  Application層（Pure Java）                     │
│  ├── usecase/ ← ★ このパッケージ               │
│  │   - ビジネスロジックのオーケストレーション   │
│  │   - Domain層のエンティティとリポジトリ使用  │
│  │   - Command受取 → Result返却                 │
│  ├── command/ (入力オブジェクト)                │
│  └── dto/ (出力オブジェクト)                    │
├─────────────────────────────────────────────────┤
│  Domain層（Pure Java - 最内層）                 │
│  ├── model/ (Entity, Value Object)              │
│  ├── repository/ (Repository Interface)         │
│  └── service/ (Domain Service)                  │
└─────────────────────────────────────────────────┘
```

### データフロー

```
1. Presentation層: Request DTO受信
   ↓
2. Controller: DTO → Commandオブジェクト変換
   ↓
3. Infrastructure層: Service（@Transactional）
   ↓
4. Application層: UseCase実行（Pure Java）★このパッケージ
   - Commandオブジェクト受取
   - Repository Interface経由でデータ取得
   - Domain Entityのビジネスロジック実行
   - Domain Serviceで複雑なロジック実行
   - Repository Interface経由でデータ保存
   - Resultオブジェクト返却
   ↓
5. Controller: Result → Response DTO変換
   ↓
6. Presentation層: Response DTO返却
```

**重要**: UseCaseは **Application層のビジネスルール** を実装します。Domain層のビジネスルールとは異なり、アプリケーション固有のフローやオーケストレーションを担当します。

---

## 🎯 責務（Responsibilities）

### ✅ このパッケージが行うべきこと

1. **アプリケーション固有のビジネスルールの実装**
   - TODOの作成フロー（ユーザー存在確認 → TODO作成 → 保存）
   - ユーザー登録フロー（重複チェック → ユーザー作成 → 保存）
   - 認証フロー（ユーザー検証 → パスワード初期化 → JWT生成）

2. **ドメインオブジェクトのオーケストレーション**
   - Repository Interfaceを使ってデータ取得
   - Domain Entityのビジネスロジックメソッド呼び出し
   - Domain Serviceで複雑なビジネスロジック実行
   - Repository Interfaceを使ってデータ保存

3. **入出力の管理**
   - Commandオブジェクト（Application層専用）を受け取る
   - Resultオブジェクト（Application層専用）を返却
   - Presentation層のDTOには依存しない

4. **Pure Javaでの実装**
   - @Service、@Transactionalアノテーション使用禁止
   - フレームワークに依存しないビジネスロジック

### ❌ このパッケージが行ってはいけないこと

1. **トランザクション管理**
   - → Infrastructure層のServiceラッパーで実施
   - UseCaseは@Transactionalを使用しない

2. **Presentation層のDTOを使用**
   - → Application層専用のCommand/Resultオブジェクトを使用
   - Presentation層のDTOには依存しない

3. **Infrastructure層の実装詳細に依存**
   - → Domain層のRepository Interfaceに依存
   - JPA Repositoryや実装クラスに直接依存しない

4. **外部フレームワークへの依存**
   - → Pure Javaで実装（Spring/JPAアノテーション禁止）

---

## 🚨 絶対原則：Pure Java（フレームワーク依存禁止）

Application層のUseCaseは **Pure Java** で実装しなければなりません。以下のアノテーション・ライブラリは **絶対に使用禁止** です：

### ❌ 禁止されるアノテーション・ライブラリ

```java
// ❌ Spring Framework
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

// ❌ JPA/Hibernate（Repository Interfaceは使用可能）
import com.api.todos.infrastructure.persistence.repository.TodoJpaRepository;  // ❌ Infrastructure実装に依存
import jakarta.persistence.*;  // ❌ JPA依存

// ❌ Lombok（Application層では非推奨）
import lombok.RequiredArgsConstructor;

// ❌ Presentation層のDTO
import com.api.todos.presentation.dto.CreateTodoRequest;  // ❌ Presentation層に依存
import com.api.todos.presentation.dto.TodoResponse;  // ❌ Presentation層に依存
```

### ✅ 許可されるもの

```java
// ✅ Java標準ライブラリ
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// ✅ Domain層の依存
import com.api.todos.domain.model.Todo;
import com.api.todos.domain.model.User;
import com.api.todos.domain.repository.TodoRepository;  // ✅ Repository Interface
import com.api.todos.domain.repository.UserRepository;  // ✅ Repository Interface
import com.api.todos.domain.service.TodoDomainService;  // ✅ Domain Service

// ✅ Application層の依存
import com.api.todos.application.command.todo.CreateTodoCommand;  // ✅ Command
import com.api.todos.application.dto.TodoResult;  // ✅ Result

// ✅ Pure Javaのコンストラクタ
public class CreateTodoUseCase {
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    
    // Pure Javaのコンストラクタインジェクション
    public CreateTodoUseCase(
        TodoRepository todoRepository,
        UserRepository userRepository
    ) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
    }
}
```

---

## 🚨 禁止パターン（Anti-Patterns）

### ❌ 1. UseCaseに@Service、@Transactionalアノテーションを使用

**問題**: UseCaseがSpring Frameworkに依存してしまい、Pure Javaの原則に違反します。

```java
// ❌ 絶対禁止: UseCaseでSpringアノテーションを使用
package com.api.todos.application.usecase.todo;

import org.springframework.stereotype.Service;  // ❌ Spring依存
import org.springframework.transaction.annotation.Transactional;  // ❌ Spring依存
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.application.command.todo.CreateTodoCommand;
import com.api.todos.application.dto.TodoResult;

@Service  // ❌ Application層でSpringアノテーション使用禁止
public class CreateTodoUseCase {
    
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    
    public CreateTodoUseCase(
        TodoRepository todoRepository,
        UserRepository userRepository
    ) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
    }
    
    @Transactional  // ❌ トランザクション管理はInfrastructure層の責務
    public TodoResult execute(CreateTodoCommand command) {
        // ユーザーの存在確認
        User user = userRepository.findById(command.getUserId())
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        // TODO作成
        Todo todo = Todo.create(
            command.getTitle(),
            command.getDescriptions(),
            command.getUserId()
        );
        
        // 保存
        Todo savedTodo = todoRepository.save(todo);
        
        // Result返却
        return TodoResult.from(savedTodo);
    }
}
```

**なぜダメか**:
- Application層がSpring Frameworkに依存
- Pure Javaの原則に違反
- フレームワーク変更時にApplication層も変更が必要
- テストが困難（Spring実装が必要）
- トランザクション管理の責務がApplication層に漏れ出している

**正しい実装**:
```java
// ✅ 正しい実装: Pure JavaのUseCase（Application層）
package com.api.todos.application.usecase.todo;

import com.api.todos.domain.model.Todo;
import com.api.todos.domain.model.User;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.domain.repository.UserRepository;
import com.api.todos.application.command.todo.CreateTodoCommand;
import com.api.todos.application.dto.TodoResult;

/**
 * TODO作成ユースケース（Application層 - Pure Java）
 * 
 * 注意: @Service、@Transactionalアノテーションは使用しない
 * トランザクション管理はInfrastructure層のServiceラッパーで実施
 */
public class CreateTodoUseCase {
    
    // Repository Interface（Domain層）に依存
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    
    // Pure Javaのコンストラクタ
    public CreateTodoUseCase(
        TodoRepository todoRepository,
        UserRepository userRepository
    ) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * TODO作成を実行
     * 
     * ビジネスフロー:
     * 1. ユーザーの存在確認
     * 2. TODO作成
     * 3. TODO保存
     * 4. Result返却
     * 
     * @param command TODO作成コマンド（Application層専用）
     * @return TODO結果オブジェクト（Application層専用）
     * @throws UserNotFoundException ユーザーが存在しない
     */
    public TodoResult execute(CreateTodoCommand command) {
        // 1. ユーザーの存在確認
        User user = userRepository.findById(command.getUserId())
            .orElseThrow(() -> new UserNotFoundException(
                "User not found: " + command.getUserId()
            ));
        
        // 2. Domain Entityの作成（Domain層のFactoryメソッド使用）
        Todo todo = Todo.create(
            command.getTitle(),
            command.getDescriptions(),
            command.getUserId()
        );
        
        // 3. Repository Interface経由で保存
        Todo savedTodo = todoRepository.save(todo);
        
        // 4. Resultオブジェクトに変換して返却
        return TodoResult.from(savedTodo);
    }
}
```

```java
// ✅ Infrastructure層でトランザクション管理ラッパーを作成
package com.api.todos.infrastructure.service;

import org.springframework.stereotype.Service;  // ✅ Infrastructure層ではSpring使用可能
import org.springframework.transaction.annotation.Transactional;
import com.api.todos.application.usecase.todo.CreateTodoUseCase;
import com.api.todos.application.command.todo.CreateTodoCommand;
import com.api.todos.application.dto.TodoResult;

/**
 * TODO作成サービス（Infrastructure層 - トランザクション管理ラッパー）
 * 
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

```java
// ✅ Infrastructure層でUseCaseをBean登録
package com.api.todos.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.api.todos.application.usecase.todo.CreateTodoUseCase;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.domain.repository.UserRepository;

/**
 * UseCase設定クラス（Infrastructure層）
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

---

### ❌ 2. UseCaseがPresentation層のDTOを使用

**問題**: UseCaseがPresentation層に依存し、依存方向が逆になります。

```java
// ❌ 絶対禁止: UseCaseがPresentation層のDTOを使用
package com.api.todos.application.usecase.todo;

import com.api.todos.presentation.dto.CreateTodoRequest;  // ❌ Presentation層に依存
import com.api.todos.presentation.dto.TodoResponse;  // ❌ Presentation層に依存
import com.api.todos.domain.repository.TodoRepository;

public class CreateTodoUseCase {
    
    private final TodoRepository todoRepository;
    
    public CreateTodoUseCase(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }
    
    // ❌ Presentation層のDTOを直接受け取る
    public TodoResponse execute(CreateTodoRequest request) {
        // TODO作成
        Todo todo = Todo.create(
            request.getTitle(),
            request.getDescriptions(),
            request.getUserId()
        );
        
        Todo savedTodo = todoRepository.save(todo);
        
        // ❌ Presentation層のDTOを返却
        TodoResponse response = new TodoResponse();
        response.setId(savedTodo.getId());
        response.setTitle(savedTodo.getTitle());
        // ...
        return response;
    }
}
```

**なぜダメか**:
- Application層がPresentation層に依存（依存方向が逆）
- クリーンアーキテクチャの原則に違反
- Presentation層の変更がApplication層に影響
- UseCaseが外側の層を知ってしまう
- テストが困難（Presentation層のDTOが必要）

**正しい実装**:
```java
// ✅ 正しい実装: Application層専用のCommandオブジェクト
package com.api.todos.application.command.todo;

import java.util.UUID;

/**
 * TODO作成コマンド（Application層専用）
 * 
 * Presentation層のDTOとは分離
 * UseCaseの入力として使用
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

```java
// ✅ 正しい実装: Application層専用のResultオブジェクト
package com.api.todos.application.dto;

import com.api.todos.domain.model.Todo;
import java.util.UUID;
import java.time.LocalDateTime;

/**
 * TODO結果オブジェクト（Application層専用）
 * 
 * Presentation層のDTOとは分離
 * UseCaseの出力として使用
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
    
    private TodoResult(
        UUID id, String title, String descriptions, boolean completed,
        UUID userId, LocalDateTime createdAt, LocalDateTime updatedAt, boolean deleted
    ) {
        this.id = id;
        this.title = title;
        this.descriptions = descriptions;
        this.completed = completed;
        this.userId = userId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
    }
    
    /**
     * Domain EntityからResultオブジェクトへの変換
     */
    public static TodoResult from(Todo todo) {
        return new TodoResult(
            todo.getId(),
            todo.getTitle(),
            todo.getDescriptions(),
            todo.isCompleted(),
            todo.getUserId(),
            todo.getCreatedAt(),
            todo.getUpdatedAt(),
            todo.isDeleted()
        );
    }
    
    // Getter
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescriptions() { return descriptions; }
    public boolean isCompleted() { return completed; }
    public UUID getUserId() { return userId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public boolean isDeleted() { return deleted; }
}
```

```java
// ✅ 正しい実装: Pure JavaのUseCase
package com.api.todos.application.usecase.todo;

import com.api.todos.application.command.todo.CreateTodoCommand;  // ✅ Application層のCommand
import com.api.todos.application.dto.TodoResult;  // ✅ Application層のResult
import com.api.todos.domain.model.Todo;
import com.api.todos.domain.repository.TodoRepository;

/**
 * TODO作成ユースケース
 * Application層専用のCommand/Resultを使用
 */
public class CreateTodoUseCase {
    
    private final TodoRepository todoRepository;
    
    public CreateTodoUseCase(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }
    
    // ✅ Application層のCommandを受け取り、Resultを返却
    public TodoResult execute(CreateTodoCommand command) {
        Todo todo = Todo.create(
            command.getTitle(),
            command.getDescriptions(),
            command.getUserId()
        );
        
        Todo savedTodo = todoRepository.save(todo);
        
        // ✅ Application層のResultオブジェクトに変換
        return TodoResult.from(savedTodo);
    }
}
```

```java
// ✅ Presentation層でDTO ⇔ Command/Result変換
package com.api.todos.presentation.rest;

import com.api.todos.application.command.todo.CreateTodoCommand;
import com.api.todos.application.dto.TodoResult;
import com.api.todos.infrastructure.service.CreateTodoService;
import com.api.todos.presentation.dto.CreateTodoRequest;
import com.api.todos.presentation.dto.TodoResponse;

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
        // 1. Presentation層のDTO → Application層のCommand変換
        CreateTodoCommand command = new CreateTodoCommand(
            request.getTitle(),
            request.getDescriptions(),
            userId
        );
        
        // 2. UseCase実行（Infrastructure層のServiceラッパー経由）
        TodoResult result = createTodoService.execute(command);
        
        // 3. Application層のResult → Presentation層のDTO変換
        TodoResponse response = TodoResponse.from(result);
        
        return ResponseEntity.ok(response);
    }
}
```

---

### ❌ 3. UseCaseがInfrastructure層の実装に依存

**問題**: UseCaseがInfrastructure層の実装詳細に依存し、依存方向が逆になります。

```java
// ❌ 絶対禁止: UseCaseがInfrastructure層の実装に依存
package com.api.todos.application.usecase.todo;

import com.api.todos.infrastructure.persistence.repository.TodoJpaRepository;  // ❌ Infrastructure実装に依存
import com.api.todos.infrastructure.persistence.entity.TodoJpaEntity;  // ❌ JPA Entityに依存
import com.api.todos.application.command.todo.CreateTodoCommand;
import com.api.todos.application.dto.TodoResult;

public class CreateTodoUseCase {
    
    // ❌ JPA Repositoryに直接依存
    private final TodoJpaRepository jpaRepository;
    
    public CreateTodoUseCase(TodoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    public TodoResult execute(CreateTodoCommand command) {
        // ❌ JPA Entityを直接操作
        TodoJpaEntity entity = new TodoJpaEntity();
        entity.setTitle(command.getTitle());
        entity.setDescriptions(command.getDescriptions());
        entity.setUserId(command.getUserId());
        entity.setCompleted(false);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setDeleted(false);
        
        // ❌ JPA Repository経由で保存
        TodoJpaEntity savedEntity = jpaRepository.save(entity);
        
        // ❌ JPA EntityからResultに変換（変換ロジックがUseCaseに漏れ出す）
        return new TodoResult(
            savedEntity.getId(),
            savedEntity.getTitle(),
            savedEntity.getDescriptions(),
            savedEntity.isCompleted(),
            savedEntity.getUserId(),
            savedEntity.getCreatedAt(),
            savedEntity.getUpdatedAt(),
            savedEntity.isDeleted()
        );
    }
}
```

**なぜダメか**:
- Application層がInfrastructure層に依存（依存方向が逆）
- クリーンアーキテクチャの原則に違反
- Infrastructure層の変更がApplication層に影響
- Domain層のビジネスロジックを使用していない
- テストが困難（JPA実装が必要）
- JPA Entity ⇔ Domain Entityの変換ロジックがUseCaseに漏れ出す

**正しい実装**:
```java
// ✅ 正しい実装: UseCaseはDomain層のRepository Interfaceに依存
package com.api.todos.application.usecase.todo;

import com.api.todos.domain.model.Todo;  // ✅ Domain Entity
import com.api.todos.domain.repository.TodoRepository;  // ✅ Repository Interface
import com.api.todos.application.command.todo.CreateTodoCommand;
import com.api.todos.application.dto.TodoResult;

/**
 * TODO作成ユースケース
 * Domain層のRepository Interfaceに依存（依存性逆転の原則）
 */
public class CreateTodoUseCase {
    
    // ✅ Repository Interface（Domain層）に依存
    private final TodoRepository todoRepository;
    
    public CreateTodoUseCase(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }
    
    public TodoResult execute(CreateTodoCommand command) {
        // ✅ Domain EntityのFactoryメソッドで作成
        // ビジネスルールとバリデーションはDomain層で実施
        Todo todo = Todo.create(
            command.getTitle(),
            command.getDescriptions(),
            command.getUserId()
        );
        
        // ✅ Repository Interface経由で保存
        // 実装詳細（JPA Entity変換）はInfrastructure層が担当
        Todo savedTodo = todoRepository.save(todo);
        
        // ✅ Domain EntityからResultオブジェクトに変換
        return TodoResult.from(savedTodo);
    }
}
```

```java
// ✅ Infrastructure層でRepository Interfaceを実装
package com.api.todos.infrastructure.persistence.repository;

import com.api.todos.domain.model.Todo;
import com.api.todos.domain.repository.TodoRepository;  // ✅ Domain層のInterface
import com.api.todos.infrastructure.persistence.entity.TodoJpaEntity;
import org.springframework.stereotype.Repository;

@Repository
public class TodoRepositoryImpl implements TodoRepository {
    
    private final TodoJpaRepository jpaRepository;
    private final TodoMapper mapper;
    
    public TodoRepositoryImpl(TodoJpaRepository jpaRepository, TodoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Todo save(Todo todo) {
        // Infrastructure層でDomain Entity → JPA Entity変換
        TodoJpaEntity entity = mapper.toJpaEntity(todo);
        TodoJpaEntity savedEntity = jpaRepository.save(entity);
        // Infrastructure層でJPA Entity → Domain Entity変換
        return mapper.toDomainModel(savedEntity);
    }
}
```

---

## ✅ 正しい実装パターン

### 1. CreateTodoUseCase - TODO作成

**目的**: 新しいTODOを作成します。

**実装例**:
```java
// api/src/main/java/com/api/todos/application/usecase/todo/CreateTodoUseCase.java
package com.api.todos.application.usecase.todo;

import com.api.todos.domain.model.Todo;
import com.api.todos.domain.model.User;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.domain.repository.UserRepository;
import com.api.todos.application.command.todo.CreateTodoCommand;
import com.api.todos.application.dto.TodoResult;

/**
 * TODO作成ユースケース（Application層 - Pure Java）
 * 
 * ビジネスフロー:
 * 1. ユーザーの存在確認
 * 2. TODO作成
 * 3. TODO保存
 * 4. Result返却
 */
public class CreateTodoUseCase {
    
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    
    public CreateTodoUseCase(
        TodoRepository todoRepository,
        UserRepository userRepository
    ) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * TODO作成を実行
     * 
     * @param command TODO作成コマンド
     * @return TODO結果オブジェクト
     * @throws UserNotFoundException ユーザーが存在しない
     */
    public TodoResult execute(CreateTodoCommand command) {
        // 1. ユーザーの存在確認
        User user = userRepository.findById(command.getUserId())
            .orElseThrow(() -> new UserNotFoundException(
                "User not found: " + command.getUserId()
            ));
        
        // 2. Domain EntityのFactoryメソッドで作成
        // バリデーションとビジネスルールはDomain層で実施
        Todo todo = Todo.create(
            command.getTitle(),
            command.getDescriptions(),
            command.getUserId()
        );
        
        // 3. Repository Interface経由で保存
        Todo savedTodo = todoRepository.save(todo);
        
        // 4. Resultオブジェクトに変換して返却
        return TodoResult.from(savedTodo);
    }
}
```

---

### 2. UpdateTodoUseCase - TODO更新

**目的**: 既存のTODOを更新します。アクセス制御とビジネスロジックを含みます。

**実装例**:
```java
// api/src/main/java/com/api/todos/application/usecase/todo/UpdateTodoUseCase.java
package com.api.todos.application.usecase.todo;

import com.api.todos.domain.model.Todo;
import com.api.todos.domain.model.User;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.domain.service.TodoDomainService;
import com.api.todos.application.command.todo.UpdateTodoCommand;
import com.api.todos.application.dto.TodoResult;
import java.util.UUID;

/**
 * TODO更新ユースケース（Application層 - Pure Java）
 * 
 * ビジネスフロー:
 * 1. TODO取得
 * 2. 所有権チェック（Domain Service使用）
 * 3. TODOのビジネスロジック実行（Domain Entityのメソッド使用）
 * 4. TODO保存
 * 5. Result返却
 */
public class UpdateTodoUseCase {
    
    private final TodoRepository todoRepository;
    private final TodoDomainService todoDomainService;
    
    public UpdateTodoUseCase(
        TodoRepository todoRepository,
        TodoDomainService todoDomainService
    ) {
        this.todoRepository = todoRepository;
        this.todoDomainService = todoDomainService;
    }
    
    /**
     * TODO更新を実行
     * 
     * @param todoId TODO ID
     * @param userId ユーザーID
     * @param command 更新コマンド
     * @return TODO結果オブジェクト
     * @throws TodoNotFoundException TODOが存在しない
     * @throws AccessDeniedException 所有者でない
     */
    public TodoResult execute(UUID todoId, UUID userId, UpdateTodoCommand command) {
        // 1. TODO取得
        Todo todo = todoRepository.findById(todoId)
            .orElseThrow(() -> new TodoNotFoundException(
                "TODO not found: " + todoId
            ));
        
        // 2. 所有権チェック（Domain Service使用）
        User user = User.reconstruct(userId, null, null, null, null, null, null, false, null, null, false);
        if (!todoDomainService.isOwner(todo, user)) {
            throw new AccessDeniedException(
                "User " + userId + " is not the owner of TODO " + todoId
            );
        }
        
        // 3. Domain Entityのビジネスロジックメソッド呼び出し
        // バリデーションとビジネスルールはDomain層で実施
        if (command.getTitle() != null) {
            todo.updateTitle(command.getTitle());
        }
        if (command.getDescriptions() != null) {
            todo.updateDescriptions(command.getDescriptions());
        }
        
        // 4. Repository Interface経由で保存
        Todo updatedTodo = todoRepository.save(todo);
        
        // 5. Resultオブジェクトに変換して返却
        return TodoResult.from(updatedTodo);
    }
}
```

---

### 3. CompleteTodoUseCase - TODO完了

**目的**: TODOを完了状態にします。Domain Entityのビジネスロジックを使用します。

**実装例**:
```java
// api/src/main/java/com/api/todos/application/usecase/todo/CompleteTodoUseCase.java
package com.api.todos.application.usecase.todo;

import com.api.todos.domain.model.Todo;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.application.dto.TodoResult;
import java.util.UUID;

/**
 * TODO完了ユースケース（Application層 - Pure Java）
 * 
 * ビジネスフロー:
 * 1. TODO取得
 * 2. 所有権チェック
 * 3. Domain Entityのビジネスロジック実行（markAsCompleted）
 * 4. TODO保存
 * 5. Result返却
 */
public class CompleteTodoUseCase {
    
    private final TodoRepository todoRepository;
    
    public CompleteTodoUseCase(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }
    
    /**
     * TODO完了を実行
     * 
     * @param todoId TODO ID
     * @param userId ユーザーID
     * @return TODO結果オブジェクト
     * @throws TodoNotFoundException TODOが存在しない
     * @throws AccessDeniedException 所有者でない
     * @throws IllegalStateException 既に完了済みまたは削除済み
     */
    public TodoResult execute(UUID todoId, UUID userId) {
        // 1. TODO取得（所有者のみ取得）
        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
            .orElseThrow(() -> new TodoNotFoundException(
                "TODO not found or access denied: " + todoId
            ));
        
        // 2. Domain Entityのビジネスロジックメソッド呼び出し
        // ビジネスルール（既に完了済み、削除済みチェック）はDomain層で実施
        todo.markAsCompleted();
        
        // 3. Repository Interface経由で保存
        Todo completedTodo = todoRepository.save(todo);
        
        // 4. Resultオブジェクトに変換して返却
        return TodoResult.from(completedTodo);
    }
}
```

---

### 4. GetTodoListUseCase - TODO一覧取得

**目的**: ユーザーのTODO一覧を取得します。Domain Serviceで高度なフィルタリングを実施します。

**実装例**:
```java
// api/src/main/java/com/api/todos/application/usecase/todo/GetTodoListUseCase.java
package com.api.todos.application.usecase.todo;

import com.api.todos.domain.model.Todo;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.domain.service.TodoDomainService;
import com.api.todos.application.dto.TodoResult;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TODO一覧取得ユースケース（Application層 - Pure Java）
 * 
 * ビジネスフロー:
 * 1. ユーザーのTODO一覧取得
 * 2. Domain Serviceでフィルタリング（任意）
 * 3. Resultリストに変換して返却
 */
public class GetTodoListUseCase {
    
    private final TodoRepository todoRepository;
    private final TodoDomainService todoDomainService;
    
    public GetTodoListUseCase(
        TodoRepository todoRepository,
        TodoDomainService todoDomainService
    ) {
        this.todoRepository = todoRepository;
        this.todoDomainService = todoDomainService;
    }
    
    /**
     * 全TODO一覧取得
     * 
     * @param userId ユーザーID
     * @return TODO結果オブジェクトリスト
     */
    public List<TodoResult> execute(UUID userId) {
        // 1. Repository Interface経由でTODO一覧取得
        List<Todo> todos = todoRepository.findByUserId(userId);
        
        // 2. Resultオブジェクトリストに変換
        return todos.stream()
            .map(TodoResult::from)
            .collect(Collectors.toList());
    }
    
    /**
     * 未完了TODO一覧取得
     * 
     * @param userId ユーザーID
     * @return 未完了TODO結果オブジェクトリスト
     */
    public List<TodoResult> getIncompleteTodos(UUID userId) {
        // 1. Repository Interface経由でTODO一覧取得
        List<Todo> todos = todoRepository.findByUserId(userId);
        
        // 2. Domain Serviceでフィルタリング
        List<Todo> incompleteTodos = todoDomainService.filterIncompleteTodos(todos);
        
        // 3. Resultオブジェクトリストに変換
        return incompleteTodos.stream()
            .map(TodoResult::from)
            .collect(Collectors.toList());
    }
    
    /**
     * 完了TODO一覧取得
     * 
     * @param userId ユーザーID
     * @return 完了TODO結果オブジェクトリスト
     */
    public List<TodoResult> getCompletedTodos(UUID userId) {
        // 1. Repository Interface経由でTODO一覧取得
        List<Todo> todos = todoRepository.findByUserId(userId);
        
        // 2. Domain Serviceでフィルタリング
        List<Todo> completedTodos = todoDomainService.filterCompletedTodos(todos);
        
        // 3. Resultオブジェクトリストに変換
        return completedTodos.stream()
            .map(TodoResult::from)
            .collect(Collectors.toList());
    }
}
```

---

### 5. InitializePasswordUseCase - パスワード初期化

**目的**: ユーザーの初期パスワードを設定します。認証フローの例です。

**実装例**:
```java
// api/src/main/java/com/api/todos/application/usecase/auth/InitializePasswordUseCase.java
package com.api.todos.application.usecase.auth;

import com.api.todos.domain.model.User;
import com.api.todos.domain.repository.UserRepository;
import com.api.todos.application.command.auth.InitializePasswordCommand;
import com.api.todos.application.dto.UserResult;
import java.util.UUID;

/**
 * パスワード初期化ユースケース（Application層 - Pure Java）
 * 
 * ビジネスフロー:
 * 1. ユーザー取得
 * 2. パスワード初期化状態チェック
 * 3. Domain Entityのビジネスロジック実行（initializePassword）
 * 4. ユーザー保存
 * 5. Result返却
 * 
 * 注意: パスワードのハッシュ化はInfrastructure層で実施
 */
public class InitializePasswordUseCase {
    
    private final UserRepository userRepository;
    
    public InitializePasswordUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * パスワード初期化を実行
     * 
     * @param userId ユーザーID
     * @param command パスワード初期化コマンド（ハッシュ化済み）
     * @return ユーザー結果オブジェクト
     * @throws UserNotFoundException ユーザーが存在しない
     * @throws IllegalStateException 既にパスワード初期化済み
     */
    public UserResult execute(UUID userId, InitializePasswordCommand command) {
        // 1. ユーザー取得
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(
                "User not found: " + userId
            ));
        
        // 2. Domain Entityのビジネスロジックメソッド呼び出し
        // ビジネスルール（既に初期化済みチェック）はDomain層で実施
        user.initializePassword(command.getHashedPassword());
        
        // 3. Repository Interface経由で保存
        User updatedUser = userRepository.save(user);
        
        // 4. Resultオブジェクトに変換して返却
        return UserResult.from(updatedUser);
    }
}
```

---

## 🧪 テスト戦略

### UseCase テスト（モック使用のユニットテスト）

**目的**: UseCaseのビジネスフローが正しく動作するかテストします。

**実装例**:
```java
package com.api.todos.application.usecase.todo;

import com.api.todos.domain.model.Todo;
import com.api.todos.domain.model.User;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.domain.repository.UserRepository;
import com.api.todos.application.command.todo.CreateTodoCommand;
import com.api.todos.application.dto.TodoResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CreateTodoUseCase テスト
 * Mockitoを使用したユニットテスト - Springコンテキスト不要
 */
@ExtendWith(MockitoExtension.class)
class CreateTodoUseCaseTest {
    
    @Mock
    private TodoRepository todoRepository;
    
    @Mock
    private UserRepository userRepository;
    
    private CreateTodoUseCase useCase;
    
    private UUID userId;
    private User user;
    
    @BeforeEach
    void setUp() {
        useCase = new CreateTodoUseCase(todoRepository, userRepository);
        
        userId = UUID.randomUUID();
        user = User.create("testuser", "test@example.com", "John", "Doe", UserRole.USER);
    }
    
    // ========================================
    // 正常系テスト
    // ========================================
    
    @Test
    void 新規TODOを作成できること() {
        // Given
        CreateTodoCommand command = new CreateTodoCommand(
            "Test TODO",
            "Test Description",
            userId
        );
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        TodoResult result = useCase.execute(command);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test TODO");
        assertThat(result.getDescriptions()).isEqualTo("Test Description");
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.isCompleted()).isFalse();
        assertThat(result.isDeleted()).isFalse();
        
        // Repository呼び出し確認
        verify(userRepository).findById(userId);
        verify(todoRepository).save(any(Todo.class));
    }
    
    @Test
    void Repository_saveに正しいTodoが渡されること() {
        // Given
        CreateTodoCommand command = new CreateTodoCommand(
            "Test TODO",
            "Test Description",
            userId
        );
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // ArgumentCaptorで保存されるTodoをキャプチャ
        ArgumentCaptor<Todo> todoCaptor = ArgumentCaptor.forClass(Todo.class);
        
        // When
        useCase.execute(command);
        
        // Then
        verify(todoRepository).save(todoCaptor.capture());
        Todo savedTodo = todoCaptor.getValue();
        
        assertThat(savedTodo.getTitle()).isEqualTo("Test TODO");
        assertThat(savedTodo.getDescriptions()).isEqualTo("Test Description");
        assertThat(savedTodo.getUserId()).isEqualTo(userId);
        assertThat(savedTodo.isCompleted()).isFalse();
        assertThat(savedTodo.isDeleted()).isFalse();
    }
    
    // ========================================
    // 異常系テスト
    // ========================================
    
    @Test
    void ユーザーが存在しない場合例外をスローすること() {
        // Given
        CreateTodoCommand command = new CreateTodoCommand(
            "Test TODO",
            "Test Description",
            userId
        );
        
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessageContaining("User not found");
        
        // Repository呼び出し確認（saveは呼ばれない）
        verify(userRepository).findById(userId);
        verify(todoRepository, never()).save(any(Todo.class));
    }
}
```

```java
package com.api.todos.application.usecase.todo;

import com.api.todos.domain.model.Todo;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.application.dto.TodoResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CompleteTodoUseCase テスト
 */
@ExtendWith(MockitoExtension.class)
class CompleteTodoUseCaseTest {
    
    @Mock
    private TodoRepository todoRepository;
    
    private CompleteTodoUseCase useCase;
    
    private UUID todoId;
    private UUID userId;
    private Todo todo;
    
    @BeforeEach
    void setUp() {
        useCase = new CompleteTodoUseCase(todoRepository);
        
        todoId = UUID.randomUUID();
        userId = UUID.randomUUID();
        todo = Todo.create("Test TODO", "Description", userId);
    }
    
    @Test
    void TODOを完了状態にできること() {
        // Given
        when(todoRepository.findByIdAndUserId(todoId, userId)).thenReturn(Optional.of(todo));
        when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        TodoResult result = useCase.execute(todoId, userId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isCompleted()).isTrue();
        
        // Repository呼び出し確認
        verify(todoRepository).findByIdAndUserId(todoId, userId);
        verify(todoRepository).save(todo);
    }
    
    @Test
    void TODOが存在しない場合例外をスローすること() {
        // Given
        when(todoRepository.findByIdAndUserId(todoId, userId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> useCase.execute(todoId, userId))
            .isInstanceOf(TodoNotFoundException.class)
            .hasMessageContaining("TODO not found or access denied");
        
        // Repository呼び出し確認（saveは呼ばれない）
        verify(todoRepository).findByIdAndUserId(todoId, userId);
        verify(todoRepository, never()).save(any(Todo.class));
    }
}
```

---

## ✅ 実装チェックリスト

### UseCase実装時

- [ ] **Pure Java**で実装（@Service、@Transactionalアノテーション使用禁止）
- [ ] **Domain層のRepository Interface**に依存（Infrastructure実装に依存しない）
- [ ] **Application層のCommand/Result**を使用（Presentation層のDTOに依存しない）
- [ ] **Domain Entityのビジネスロジック**を呼び出す（UseCase内でビジネスルール実装しない）
- [ ] **Domain Service**で複雑なビジネスロジック実行
- [ ] **コンストラクタインジェクション**でRepository受け取る
- [ ] **JavaDoc**で責務とビジネスフローを明記
- [ ] **例外処理**を適切に実施

### 対応する他のコンポーネント

- [ ] **Application層**: Command（入力）が存在
- [ ] **Application層**: Result（出力）が存在
- [ ] **Infrastructure層**: Serviceラッパー（@Transactional）が存在
- [ ] **Infrastructure層**: UseCaseConfig（DI設定）にBean登録済み
- [ ] **Presentation層**: Controller（DTO変換）が存在
- [ ] **テスト**: Mock使用のユニットテストが実装済み

---

## 📚 参考資料

### プロジェクト内ドキュメント
- **[AGENTS.md](../../../../../AGENTS.md)** - AIエージェント向けコンテキストドキュメント
- **[Application層 README](../README.md)** - Application層全体の概要
- **[Domain層 model README](../../domain/model/README.md)** - Domain Entity実装パターン
- **[Domain層 repository README](../../domain/repository/README.md)** - Repository Interface定義
- **[Infrastructure層 service README](../../infrastructure/service/README.md)** - トランザクション管理ラッパー

### 外部参考資料
- [Clean Architecture (Robert C. Martin)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Domain-Driven Design (Eric Evans)](https://www.domainlanguage.com/ddd/)

---

## 🎯 まとめ

Application層のUseCaseは、**アプリケーション固有のビジネスルールとドメインオブジェクトのオーケストレーション**を担う重要なコンポーネントです。

### 重要ポイント

1. **Pure Java（最重要）**
   - @Service、@Transactionalアノテーション使用禁止
   - フレームワークに一切依存しない
   - トランザクション管理はInfrastructure層のServiceラッパーで実施

2. **依存方向**
   - Domain層のRepository Interfaceに依存
   - Application層のCommand/Resultを使用
   - Infrastructure層、Presentation層に依存しない

3. **ビジネスロジックの配置**
   - Domain Entityのビジネスロジックメソッド呼び出し
   - Domain Serviceで複雑なビジネスロジック実行
   - UseCase内でビジネスルール実装しない

4. **オーケストレーション**
   - Repository Interface経由でデータ取得
   - Domain Entityのビジネスロジック実行
   - Repository Interface経由でデータ保存
   - Resultオブジェクトに変換して返却

### 禁止事項

- ❌ UseCaseに@Service、@Transactionalアノテーション使用
- ❌ UseCaseがPresentation層のDTOを使用
- ❌ UseCaseがInfrastructure層の実装に依存

このREADMEを参考に、**Pure Javaで実装された高品質なUseCase**を構築してください。UseCaseは、アプリケーションの価値を実現する中核コンポーネントです。

---

**作成日**: 2026年1月5日  
**対象バージョン**: Spring Boot TODO API v0.0.1-SNAPSHOT  
**対象パッケージ**: `com.api.todos.application.usecase`
