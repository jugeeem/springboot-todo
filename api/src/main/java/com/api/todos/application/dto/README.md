# Application層 - dto パッケージ（Resultオブジェクト）

## 📋 パッケージ概要

**場所**: `api/src/main/java/com/api/todos/application/dto/`

**目的**: **UseCaseの出力（Result）オブジェクトを Pure Java で実装**するパッケージです。Resultオブジェクトは、UseCaseの実行結果を表現し、Presentation層に返却されます。Presentation層のDTOとは分離された、Application層専用のデータ転送オブジェクトです。

**主要コンポーネント**:
- **TodoResult**: TODO結果オブジェクト（UseCaseの出力）
- **UserResult**: ユーザー結果オブジェクト（UseCaseの出力）

---

## 🏛️ クリーンアーキテクチャにおける位置づけ

```
┌─────────────────────────────────────────────────┐
│  Presentation層 (Controllers, REST API)         │
│     ↑ Resultオブジェクト受取                    │
│     ↓ Response DTO返却                          │
├─────────────────────────────────────────────────┤
│  Infrastructure層 (@Service + @Transactional)   │
│     ↑ Resultオブジェクト返却                    │
├─────────────────────────────────────────────────┤
│  Application層（Pure Java）                     │
│  ├── usecase/ (UseCase実行)                     │
│  │   ↓ Resultオブジェクト返却                   │
│  ├── dto/ ← ★ このパッケージ                   │
│  │   - Domain Entity → Result変換               │
│  │   - UseCaseの出力として使用                  │
│  │   - Presentation層のDTOとは分離              │
│  └── command/ (入力オブジェクト)                │
├─────────────────────────────────────────────────┤
│  Domain層（Pure Java - 最内層）                 │
│  └── model/ (Entity, Value Object)              │
│     ↑ Domain Entityから変換                     │
└─────────────────────────────────────────────────┘
```

### データフロー

```
1. UseCase: Repository経由でDomain Entity取得
   ↓
2. UseCase: Domain Entityのビジネスロジック実行
   ↓
3. UseCase: Domain Entity → Resultオブジェクト変換 ★このパッケージ
   ↓
4. UseCase: Resultオブジェクト返却
   ↓
5. Infrastructure層: Resultオブジェクトそのまま返却
   ↓
6. Controller: Result → Presentation層のResponse DTO変換
   ↓
7. Presentation層: Response DTO返却
```

**重要**: Resultオブジェクトは **Application層専用** であり、Presentation層のDTOとは分離されます。この分離により、Presentation層の変更がApplication層に影響しません。

---

## 🎯 責務（Responsibilities）

### ✅ このパッケージが行うべきこと

1. **UseCaseの出力表現**
   - Domain Entityの情報をResultオブジェクトに変換
   - UseCaseの実行結果を表現
   - Presentation層に返却する情報を保持

2. **Domain Entity → Result変換**
   - Domain EntityからResultオブジェクトへの変換メソッド（from()）
   - 必要な情報のみを抽出
   - 不変オブジェクトとして実装

3. **Application層専用のデータ転送**
   - Presentation層のDTOとは分離
   - UseCaseとControllerの境界で使用
   - ドメインロジックを含まない

4. **Pure Javaでの実装**
   - Spring、Lombokアノテーション使用禁止
   - フレームワークに依存しない不変オブジェクト

### ❌ このパッケージが行ってはいけないこと

1. **ビジネスロジックを含む**
   - → ビジネスロジックはDomain層の責務
   - Resultオブジェクトはデータ転送のみ

2. **Presentation層のDTOに依存**
   - → Application層とPresentation層は分離
   - Resultオブジェクトは独立して定義

3. **Domain Entityを直接返却**
   - → Domain EntityはApplication層内部で使用
   - 外部にはResultオブジェクトを返却

4. **可変オブジェクトとして実装**
   - → finalフィールド、setterなしの不変オブジェクト

---

## 🚨 絶対原則：Pure Java（フレームワーク依存禁止）

Application層のResultオブジェクトは **Pure Java** で実装しなければなりません。以下のアノテーション・ライブラリは **絶対に使用禁止** です：

### ❌ 禁止されるアノテーション・ライブラリ

```java
// ❌ Spring Framework
import org.springframework.stereotype.Component;

// ❌ JPA/Hibernate
import jakarta.persistence.Entity;

// ❌ Lombok（Application層では非推奨）
import lombok.Getter;
import lombok.Setter;
import lombok.Data;
import lombok.Builder;

// ❌ Jackson（Application層では非推奨）
import com.fasterxml.jackson.annotation.JsonProperty;

// ❌ Bean Validation
import jakarta.validation.constraints.NotNull;
```

### ✅ 許可されるもの

```java
// ✅ Java標準ライブラリ
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;

// ✅ Domain層の依存
import com.api.todos.domain.model.Todo;
import com.api.todos.domain.model.User;

// ✅ Pure Javaのコンストラクタ・メソッド
public class TodoResult {
    // finalフィールド（不変）
    private final UUID id;
    private final String title;
    
    // Pure Javaのコンストラクタ
    private TodoResult(UUID id, String title) {
        this.id = id;
        this.title = title;
    }
    
    // Pure Javaのgetter
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    
    // Domain Entity → Result変換
    public static TodoResult from(Todo todo) {
        return new TodoResult(todo.getId(), todo.getTitle());
    }
}
```

---

## 🚨 禁止パターン（Anti-Patterns）

### ❌ 1. Domain Entityを直接返却

**問題**: Domain EntityをUseCaseの出力として直接返却すると、Domain層の実装詳細が外部に漏れ出します。

```java
// ❌ 絶対禁止: Domain Entityを直接返却
package com.api.todos.application.usecase.todo;

import com.api.todos.domain.model.Todo;  // ❌ Domain Entityを返却
import com.api.todos.domain.repository.TodoRepository;
import java.util.UUID;

public class GetTodoUseCase {
    
    private final TodoRepository todoRepository;
    
    public GetTodoUseCase(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }
    
    // ❌ Domain Entityを直接返却
    public Todo execute(UUID todoId) {
        return todoRepository.findById(todoId)
            .orElseThrow(() -> new TodoNotFoundException("TODO not found"));
    }
}
```

**なぜダメか**:
- Domain Entityの実装詳細が外部に漏れる
- Domain層の変更がPresentation層に影響
- Domain Entityにビジネスロジックメソッドが含まれる
- セキュリティリスク（意図しない情報の漏洩）

**正しい実装**:
```java
// ✅ 正しい実装: Resultオブジェクトに変換して返却
package com.api.todos.application.usecase.todo;

import com.api.todos.domain.model.Todo;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.application.dto.TodoResult;  // ✅ Resultオブジェクト
import java.util.UUID;

public class GetTodoUseCase {
    
    private final TodoRepository todoRepository;
    
    public GetTodoUseCase(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }
    
    // ✅ Resultオブジェクトを返却
    public TodoResult execute(UUID todoId) {
        Todo todo = todoRepository.findById(todoId)
            .orElseThrow(() -> new TodoNotFoundException("TODO not found"));
        
        // Domain Entity → Resultオブジェクト変換
        return TodoResult.from(todo);
    }
}
```

```java
// ✅ Resultオブジェクト定義（Pure Java）
package com.api.todos.application.dto;

import com.api.todos.domain.model.Todo;
import java.util.UUID;
import java.time.LocalDateTime;

/**
 * TODO結果オブジェクト（Application層専用）
 * 
 * Pure Javaの不変オブジェクト
 * Domain EntityからResultオブジェクトへの変換
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
        UUID id,
        String title,
        String descriptions,
        boolean completed,
        UUID userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
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

---

### ❌ 2. Presentation層のDTOを返却

**問題**: UseCaseがPresentation層のDTOを返却すると、Application層がPresentation層に依存してしまいます。

```java
// ❌ 絶対禁止: Presentation層のDTOを返却
package com.api.todos.application.usecase.todo;

import com.api.todos.presentation.dto.TodoResponse;  // ❌ Presentation層に依存
import com.api.todos.domain.repository.TodoRepository;
import java.util.UUID;

public class GetTodoUseCase {
    
    private final TodoRepository todoRepository;
    
    public GetTodoUseCase(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }
    
    // ❌ Presentation層のDTOを返却
    public TodoResponse execute(UUID todoId) {
        Todo todo = todoRepository.findById(todoId)
            .orElseThrow(() -> new TodoNotFoundException("TODO not found"));
        
        // ❌ Presentation層のDTOに変換
        TodoResponse response = new TodoResponse();
        response.setId(todo.getId());
        response.setTitle(todo.getTitle());
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

**正しい実装**:
```java
// ✅ 正しい実装: Application層専用のResultオブジェクトを返却
package com.api.todos.application.usecase.todo;

import com.api.todos.application.dto.TodoResult;  // ✅ Application層のResult
import com.api.todos.domain.repository.TodoRepository;
import java.util.UUID;

public class GetTodoUseCase {
    
    private final TodoRepository todoRepository;
    
    public GetTodoUseCase(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }
    
    // ✅ Application層のResultオブジェクトを返却
    public TodoResult execute(UUID todoId) {
        Todo todo = todoRepository.findById(todoId)
            .orElseThrow(() -> new TodoNotFoundException("TODO not found"));
        
        // ✅ Application層のResultオブジェクトに変換
        return TodoResult.from(todo);
    }
}
```

```java
// ✅ Presentation層でResult → Response DTO変換
package com.api.todos.presentation.rest;

import com.api.todos.application.dto.TodoResult;
import com.api.todos.presentation.dto.TodoResponse;
import com.api.todos.infrastructure.service.GetTodoService;

@RestController
@RequestMapping("/api/todos")
public class TodoController {
    
    private final GetTodoService getTodoService;
    
    public TodoController(GetTodoService getTodoService) {
        this.getTodoService = getTodoService;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TodoResponse> getTodo(@PathVariable UUID id) {
        // 1. UseCase実行（Application層のResult取得）
        TodoResult result = getTodoService.execute(id);
        
        // 2. Result → Response DTO変換
        TodoResponse response = TodoResponse.from(result);
        
        return ResponseEntity.ok(response);
    }
}
```

---

### ❌ 3. 可変オブジェクトとして実装

**問題**: Resultオブジェクトにsetterを提供すると、意図しない変更が発生する可能性があります。

```java
// ❌ 絶対禁止: 可変オブジェクトとして実装
package com.api.todos.application.dto;

import java.util.UUID;
import java.time.LocalDateTime;

/**
 * ❌ 可変オブジェクト - setterで変更可能
 */
public class TodoResult {
    // ❌ finalなしの可変フィールド
    private UUID id;
    private String title;
    private String descriptions;
    private boolean completed;
    
    // ❌ デフォルトコンストラクタ
    public TodoResult() {
    }
    
    // ❌ setter（可変）
    public void setId(UUID id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescriptions(String descriptions) { this.descriptions = descriptions; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    
    // getter
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescriptions() { return descriptions; }
    public boolean isCompleted() { return completed; }
}
```

**なぜダメか**:
- 意図しない変更が発生する可能性
- スレッドセーフでない
- 不変性の保証がない
- デバッグが困難（いつ変更されたか不明）

**正しい実装**:
```java
// ✅ 正しい実装: 不変オブジェクトとして実装
package com.api.todos.application.dto;

import com.api.todos.domain.model.Todo;
import java.util.UUID;
import java.time.LocalDateTime;

/**
 * TODO結果オブジェクト（Application層専用）
 * 
 * 不変オブジェクト - finalフィールド、setterなし
 */
public class TodoResult {
    // ✅ finalフィールド（不変）
    private final UUID id;
    private final String title;
    private final String descriptions;
    private final boolean completed;
    private final UUID userId;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final boolean deleted;
    
    // ✅ プライベートコンストラクタ
    private TodoResult(
        UUID id,
        String title,
        String descriptions,
        boolean completed,
        UUID userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
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
     * ✅ Factoryメソッド: Domain Entity → Result変換
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
    
    // ✅ Getterのみ（setterなし）
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

---

## ✅ 正しい実装パターン

### 1. TodoResult - TODO結果オブジェクト

**目的**: TODO情報をUseCaseの出力として表現します。

**実装例**:
```java
// api/src/main/java/com/api/todos/application/dto/TodoResult.java
package com.api.todos.application.dto;

import com.api.todos.domain.model.Todo;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * TODO結果オブジェクト（Application層専用）
 * 
 * Pure Javaの不変オブジェクト
 * Domain EntityからResultオブジェクトへの変換
 * UseCaseの出力として使用
 */
public class TodoResult {
    
    // ========================================
    // フィールド（すべてfinal - 不変）
    // ========================================
    
    private final UUID id;
    private final String title;
    private final String descriptions;
    private final boolean completed;
    private final UUID userId;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final boolean deleted;
    
    // ========================================
    // コンストラクタ（プライベート）
    // ========================================
    
    /**
     * プライベートコンストラクタ
     * Factoryメソッド（from）経由でのみ生成可能
     */
    private TodoResult(
        UUID id,
        String title,
        String descriptions,
        boolean completed,
        UUID userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
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
    
    // ========================================
    // Factoryメソッド（Domain Entity → Result変換）
    // ========================================
    
    /**
     * Domain EntityからResultオブジェクトへの変換
     * 
     * @param todo TODO Domain Entity
     * @return TodoResult
     */
    public static TodoResult from(Todo todo) {
        if (todo == null) {
            throw new IllegalArgumentException("Todo must not be null");
        }
        
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
    
    // ========================================
    // Getter（Pure Java）
    // ========================================
    
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescriptions() { return descriptions; }
    public boolean isCompleted() { return completed; }
    public UUID getUserId() { return userId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public boolean isDeleted() { return deleted; }
    
    // ========================================
    // equals, hashCode, toString
    // ========================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TodoResult that = (TodoResult) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "TodoResult{" +
            "id=" + id +
            ", title='" + title + '\'' +
            ", completed=" + completed +
            ", deleted=" + deleted +
            '}';
    }
}
```

**使用例（UseCase内）**:
```java
// GetTodoUseCase.java
public class GetTodoUseCase {
    public TodoResult execute(UUID todoId) {
        Todo todo = todoRepository.findById(todoId)
            .orElseThrow(() -> new TodoNotFoundException("TODO not found"));
        
        // Domain Entity → Resultオブジェクト変換
        return TodoResult.from(todo);
    }
}
```

---

### 2. UserResult - ユーザー結果オブジェクト

**目的**: ユーザー情報をUseCaseの出力として表現します。

**実装例**:
```java
// api/src/main/java/com/api/todos/application/dto/UserResult.java
package com.api.todos.application.dto;

import com.api.todos.domain.model.User;
import com.api.todos.domain.model.UserRole;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * ユーザー結果オブジェクト（Application層専用）
 * 
 * Pure Javaの不変オブジェクト
 * Domain EntityからResultオブジェクトへの変換
 * UseCaseの出力として使用
 * 
 * 注意: パスワードハッシュは含まない（セキュリティ）
 */
public class UserResult {
    
    // ========================================
    // フィールド（すべてfinal - 不変）
    // ========================================
    
    private final UUID id;
    private final String username;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final UserRole role;
    private final boolean passwordInitialized;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final boolean deleted;
    
    // ========================================
    // コンストラクタ（プライベート）
    // ========================================
    
    /**
     * プライベートコンストラクタ
     * Factoryメソッド（from）経由でのみ生成可能
     */
    private UserResult(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        boolean passwordInitialized,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
    ) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.passwordInitialized = passwordInitialized;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
    }
    
    // ========================================
    // Factoryメソッド（Domain Entity → Result変換）
    // ========================================
    
    /**
     * Domain EntityからResultオブジェクトへの変換
     * 
     * 注意: パスワードハッシュは含まない
     * 
     * @param user User Domain Entity
     * @return UserResult
     */
    public static UserResult from(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        
        return new UserResult(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole(),
            user.isPasswordInitialized(),
            user.getCreatedAt(),
            user.getUpdatedAt(),
            user.isDeleted()
        );
    }
    
    // ========================================
    // Getter（Pure Java）
    // ========================================
    
    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public UserRole getRole() { return role; }
    public boolean isPasswordInitialized() { return passwordInitialized; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public boolean isDeleted() { return deleted; }
    
    // ========================================
    // equals, hashCode, toString
    // ========================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserResult that = (UserResult) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "UserResult{" +
            "id=" + id +
            ", username='" + username + '\'' +
            ", role=" + role +
            ", deleted=" + deleted +
            '}';
    }
}
```

---

### 3. リスト結果パターン - List<TodoResult>

**目的**: TODO一覧をUseCaseの出力として表現します。

**実装例（UseCase内）**:
```java
// api/src/main/java/com/api/todos/application/usecase/todo/GetTodoListUseCase.java
package com.api.todos.application.usecase.todo;

import com.api.todos.domain.model.Todo;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.application.dto.TodoResult;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TODO一覧取得ユースケース
 */
public class GetTodoListUseCase {
    
    private final TodoRepository todoRepository;
    
    public GetTodoListUseCase(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }
    
    /**
     * ユーザーのTODO一覧を取得
     * 
     * @param userId ユーザーID
     * @return TODO結果オブジェクトリスト
     */
    public List<TodoResult> execute(UUID userId) {
        // 1. Repository経由でDomain Entity一覧取得
        List<Todo> todos = todoRepository.findByUserId(userId);
        
        // 2. Domain Entity → Resultオブジェクトリストに変換
        return todos.stream()
            .map(TodoResult::from)
            .collect(Collectors.toList());
    }
}
```

**Presentation層での使用例**:
```java
// api/src/main/java/com/api/todos/presentation/rest/TodoController.java
@RestController
@RequestMapping("/api/todos")
public class TodoController {
    
    private final GetTodoListService getTodoListService;
    
    @GetMapping
    public ResponseEntity<List<TodoResponse>> getTodoList(
        @RequestHeader("x-user-id") UUID userId
    ) {
        // 1. UseCase実行（List<TodoResult>取得）
        List<TodoResult> results = getTodoListService.execute(userId);
        
        // 2. List<TodoResult> → List<TodoResponse>変換
        List<TodoResponse> responses = results.stream()
            .map(TodoResponse::from)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
}
```

---

### 4. 部分的な情報を含むResultパターン

**目的**: セキュリティやパフォーマンスの観点から、必要な情報のみを含むResultオブジェクトを定義します。

**実装例**:
```java
// api/src/main/java/com/api/todos/application/dto/TodoSummaryResult.java
package com.api.todos.application.dto;

import com.api.todos.domain.model.Todo;
import java.util.UUID;

/**
 * TODOサマリー結果オブジェクト（Application層専用）
 * 
 * 必要最小限の情報のみを含む
 * 一覧表示用など、詳細情報が不要な場合に使用
 */
public class TodoSummaryResult {
    
    private final UUID id;
    private final String title;
    private final boolean completed;
    
    private TodoSummaryResult(UUID id, String title, boolean completed) {
        this.id = id;
        this.title = title;
        this.completed = completed;
    }
    
    /**
     * Domain EntityからResultオブジェクトへの変換
     * 必要最小限の情報のみを抽出
     */
    public static TodoSummaryResult from(Todo todo) {
        if (todo == null) {
            throw new IllegalArgumentException("Todo must not be null");
        }
        
        return new TodoSummaryResult(
            todo.getId(),
            todo.getTitle(),
            todo.isCompleted()
        );
    }
    
    // Getter
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public boolean isCompleted() { return completed; }
}
```

---

## 🧪 テスト戦略

### Result変換テスト（Pure Javaユニットテスト）

**目的**: Domain Entity → Result変換が正しく動作するかテストします。

**実装例**:
```java
package com.api.todos.application.dto;

import com.api.todos.domain.model.Todo;
import com.api.todos.domain.model.UserRole;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * TodoResult テスト
 * Pure Javaのユニットテスト - モック不要、Springコンテキスト不要
 */
class TodoResultTest {
    
    @Test
    void Domain_EntityからResultオブジェクトに変換できること() {
        // Given
        UUID userId = UUID.randomUUID();
        Todo todo = Todo.create("Test TODO", "Test Description", userId);
        
        // When
        TodoResult result = TodoResult.from(todo);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(todo.getId());
        assertThat(result.getTitle()).isEqualTo("Test TODO");
        assertThat(result.getDescriptions()).isEqualTo("Test Description");
        assertThat(result.isCompleted()).isFalse();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.isDeleted()).isFalse();
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }
    
    @Test
    void nullのDomain_Entityから変換しようとすると例外をスローすること() {
        // When & Then
        assertThatThrownBy(() -> TodoResult.from(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Todo must not be null");
    }
    
    @Test
    void 完了状態のTODOをResultオブジェクトに変換できること() {
        // Given
        UUID userId = UUID.randomUUID();
        Todo todo = Todo.create("Test TODO", "Description", userId);
        todo.markAsCompleted();
        
        // When
        TodoResult result = TodoResult.from(todo);
        
        // Then
        assertThat(result.isCompleted()).isTrue();
    }
    
    @Test
    void equalsとhashCodeが正しく動作すること() {
        // Given
        UUID userId = UUID.randomUUID();
        Todo todo1 = Todo.create("TODO 1", "Description 1", userId);
        Todo todo2 = Todo.create("TODO 2", "Description 2", userId);
        
        TodoResult result1a = TodoResult.from(todo1);
        TodoResult result1b = TodoResult.from(todo1);
        TodoResult result2 = TodoResult.from(todo2);
        
        // When & Then
        assertThat(result1a).isEqualTo(result1b);
        assertThat(result1a).isNotEqualTo(result2);
        assertThat(result1a.hashCode()).isEqualTo(result1b.hashCode());
    }
}
```

```java
package com.api.todos.application.dto;

import com.api.todos.domain.model.User;
import com.api.todos.domain.model.UserRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * UserResult テスト
 */
class UserResultTest {
    
    @Test
    void Domain_EntityからResultオブジェクトに変換できること() {
        // Given
        User user = User.create(
            "testuser",
            "test@example.com",
            "John",
            "Doe",
            UserRole.USER
        );
        
        // When
        UserResult result = UserResult.from(user);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(user.getId());
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getRole()).isEqualTo(UserRole.USER);
        assertThat(result.isPasswordInitialized()).isFalse();
        assertThat(result.isDeleted()).isFalse();
    }
    
    @Test
    void パスワードハッシュは含まれないこと() {
        // Given
        User user = User.create("testuser", "test@example.com", "John", "Doe", UserRole.USER);
        user.initializePassword("hashedPassword123");
        
        // When
        UserResult result = UserResult.from(user);
        
        // Then
        // UserResultにはパスワードハッシュのgetterが存在しない
        assertThat(result.isPasswordInitialized()).isTrue();
        // パスワードハッシュ自体はResultに含まれない（セキュリティ）
    }
}
```

---

## ✅ 実装チェックリスト

### Result実装時

- [ ] **Pure Java**で実装（Spring/Lombokアノテーション使用禁止）
- [ ] **不変オブジェクト**として実装（finalフィールド、setterなし）
- [ ] **プライベートコンストラクタ**でFactoryメソッド経由でのみ生成
- [ ] **from()メソッド**でDomain Entity → Result変換
- [ ] **Getterのみ提供**（setterは提供しない）
- [ ] **equals/hashCode**をIDのみで実装
- [ ] **toString**でデバッグ情報を提供
- [ ] **セキュリティ考慮**（パスワードハッシュ等の機密情報を含まない）
- [ ] **Pure Javaユニットテスト**を実装

### 対応する他のコンポーネント

- [ ] **Application層**: UseCase（Resultを返却）が存在
- [ ] **Domain層**: Entity（Resultに変換される）が存在
- [ ] **Presentation層**: Response DTO（Resultから変換）が存在
- [ ] **テスト**: Result変換テストが実装済み

---

## 📚 参考資料

### プロジェクト内ドキュメント
- **[AGENTS.md](../../../../../AGENTS.md)** - AIエージェント向けコンテキストドキュメント
- **[Application層 README](../README.md)** - Application層全体の概要
- **[Application層 usecase README](../usecase/README.md)** - UseCase実装パターン
- **[Domain層 model README](../../domain/model/README.md)** - Domain Entity実装パターン
- **[Presentation層 dto README](../../presentation/dto/README.md)** - Response DTO実装パターン

### 外部参考資料
- [Clean Architecture (Robert C. Martin)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Effective Java (Joshua Bloch)](https://www.oreilly.com/library/view/effective-java/9780134686097/)

---

## 🎯 まとめ

Application層のResultオブジェクトは、**UseCaseの出力を表現する不変オブジェクト**であり、Application層とPresentation層の境界で使用されます。

### 重要ポイント

1. **Pure Java（最重要）**
   - Spring、Lombokアノテーション使用禁止
   - フレームワークに一切依存しない
   - Pure Javaの不変オブジェクト

2. **不変性の保証**
   - finalフィールド、setterなし
   - プライベートコンストラクタ + Factoryメソッド
   - スレッドセーフ

3. **Application層専用**
   - Presentation層のDTOとは分離
   - Domain Entityから変換
   - UseCaseの出力として使用

4. **セキュリティ考慮**
   - パスワードハッシュ等の機密情報を含まない
   - 必要な情報のみを含む
   - 部分的な情報を含むResultパターン

### 禁止事項

- ❌ Domain Entityを直接返却
- ❌ Presentation層のDTOを返却
- ❌ 可変オブジェクトとして実装（setterの提供）

このREADMEを参考に、**Pure Javaで実装された高品質なResultオブジェクト**を構築してください。Resultオブジェクトは、Application層とPresentation層の境界を明確にし、クリーンアーキテクチャの原則を実現する重要なコンポーネントです。

---

**作成日**: 2026年1月5日  
**対象バージョン**: Spring Boot TODO API v0.0.1-SNAPSHOT  
**対象パッケージ**: `com.api.todos.application.dto`
