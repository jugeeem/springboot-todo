# Application層 - command パッケージ（Commandオブジェクト）

## 📋 パッケージ概要

**場所**: `api/src/main/java/com/api/todos/application/command/`

**目的**: **UseCaseの入力（Command）オブジェクトを Pure Java で実装**するパッケージです。Commandオブジェクトは、UseCaseに渡される入力パラメータを表現し、Presentation層のDTOから変換されます。Presentation層のDTOとは分離された、Application層専用の入力オブジェクトです。

**主要コンポーネント**:
- **CreateTodoCommand**: TODO作成コマンド（UseCaseの入力）
- **UpdateTodoCommand**: TODO更新コマンド（UseCaseの入力）
- **InitializePasswordCommand**: パスワード初期化コマンド（UseCaseの入力）

---

## 🏛️ クリーンアーキテクチャにおける位置づけ

```
┌─────────────────────────────────────────────────┐
│  Presentation層 (Controllers, REST API)         │
│     ↓ Request DTO受取                           │
│     ↓ Commandオブジェクト変換                   │
├─────────────────────────────────────────────────┤
│  Infrastructure層 (@Service + @Transactional)   │
│     ↓ Commandオブジェクト渡す                   │
├─────────────────────────────────────────────────┤
│  Application層（Pure Java）                     │
│  ├── usecase/ (UseCase実行)                     │
│  │   ↑ Commandオブジェクト受取                  │
│  ├── command/ ← ★ このパッケージ                │
│  │   - Request DTO → Command変換                │
│  │   - UseCaseの入力として使用                  │
│  │   - Presentation層のDTOとは分離              │
│  └── dto/ (出力オブジェクト)                    │
├─────────────────────────────────────────────────┤
│  Domain層（Pure Java - 最内層）                 │
│  └── model/ (Entity, Value Object)              │
│     ↑ Commandからデータ取得してEntity作成       │
└─────────────────────────────────────────────────┘
```

### データフロー

```
1. Client: HTTPリクエスト送信
   ↓
2. Controller: Request DTO受取
   ↓
3. Controller: Request DTO → Commandオブジェクト変換 ★このパッケージ
   ↓
4. Controller: Infrastructure層のService呼び出し
   ↓
5. Infrastructure Service: Commandオブジェクトをそのまま渡す
   ↓
6. UseCase: Commandオブジェクト受取
   ↓
7. UseCase: Commandからデータ取得
   ↓
8. UseCase: Domain Entityビジネスロジック実行
   ↓
9. UseCase: Resultオブジェクト返却
```

**重要**: Commandオブジェクトは **Application層専用** であり、Presentation層のDTOとは分離されます。この分離により、Presentation層の変更がApplication層に影響しません。

---

## 🎯 責務（Responsibilities）

### ✅ このパッケージが行うべきこと

1. **UseCaseの入力表現**
   - UseCaseに必要なパラメータを保持
   - Presentation層のRequest DTOから変換
   - アプリケーション層専用の入力オブジェクト

2. **入力データの検証**
   - コンストラクタでの基本的な検証
   - null チェック、必須パラメータの確認
   - ビジネスルールではなく、入力の妥当性検証

3. **不変オブジェクトとしての実装**
   - finalフィールド、getterのみ
   - setterなしの不変性保証
   - スレッドセーフ

4. **Pure Javaでの実装**
   - Spring、Lombokアノテーション使用禁止
   - フレームワークに依存しない

### ❌ このパッケージが行ってはいけないこと

1. **ビジネスロジックを含む**
   - → ビジネスロジックはDomain層の責務
   - Commandオブジェクトはデータ保持のみ

2. **Presentation層のDTOに依存**
   - → Application層とPresentation層は分離
   - Commandオブジェクトは独立して定義

3. **Domain Entityを直接保持**
   - → Commandはプリミティブなデータのみ保持
   - Entity作成はUseCase内で実施

4. **可変オブジェクトとして実装**
   - → finalフィールド、setterなしの不変オブジェクト

---

## 🚨 絶対原則：Pure Java（フレームワーク依存禁止）

Application層のCommandオブジェクトは **Pure Java** で実装しなければなりません。以下のアノテーション・ライブラリは **絶対に使用禁止** です：

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
import lombok.AllArgsConstructor;

// ❌ Bean Validation（Commandオブジェクトでは非推奨）
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
```

**注意**: Bean Validationは **Presentation層のDTOで使用**し、Commandオブジェクトではコンストラクタで検証します。

### ✅ 許可されるもの

```java
// ✅ Java標準ライブラリ
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.Objects;

// ✅ Pure Javaのコンストラクタ・メソッド
public class CreateTodoCommand {
    // finalフィールド（不変）
    private final String title;
    private final String descriptions;
    private final UUID userId;
    
    // Pure Javaのコンストラクタ（検証付き）
    public CreateTodoCommand(String title, String descriptions, UUID userId) {
        // 入力検証
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be null or blank");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        
        this.title = title;
        this.descriptions = descriptions;
        this.userId = userId;
    }
    
    // Pure Javaのgetter
    public String getTitle() { return title; }
    public String getDescriptions() { return descriptions; }
    public UUID getUserId() { return userId; }
}
```

---

## 🚨 禁止パターン（Anti-Patterns）

### ❌ 1. Presentation層のDTOをUseCaseに渡す

**問題**: UseCaseがPresentation層のDTOを直接受け取ると、Application層がPresentation層に依存してしまいます。

```java
// ❌ 絶対禁止: Presentation層のDTOをUseCaseに渡す
package com.api.todos.application.usecase.todo;

import com.api.todos.presentation.dto.CreateTodoRequest;  // ❌ Presentation層に依存
import com.api.todos.domain.repository.TodoRepository;

public class CreateTodoUseCase {
    
    private final TodoRepository todoRepository;
    
    public CreateTodoUseCase(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }
    
    // ❌ Presentation層のDTOを直接受け取る
    public TodoResult execute(CreateTodoRequest request) {
        // ❌ Presentation層のDTOに依存
        Todo todo = Todo.create(
            request.getTitle(),
            request.getDescriptions(),
            request.getUserId()
        );
        
        return TodoResult.from(todoRepository.save(todo));
    }
}
```

```java
// ❌ Controller側も問題
@RestController
@RequestMapping("/api/todos")
public class TodoController {
    
    private final CreateTodoService createTodoService;
    
    @PostMapping
    public ResponseEntity<TodoResponse> createTodo(
        @RequestBody CreateTodoRequest request,
        @RequestHeader("x-user-id") UUID userId
    ) {
        // ❌ Presentation層のDTOをそのままUseCaseに渡す
        request.setUserId(userId);  // DTOにsetterが必要
        TodoResult result = createTodoService.execute(request);
        
        return ResponseEntity.ok(TodoResponse.from(result));
    }
}
```

**なぜダメか**:
- Application層がPresentation層に依存（依存方向が逆）
- クリーンアーキテクチャの原則に違反
- Presentation層の変更がApplication層に影響
- DTOのフィールド追加がUseCaseに影響

**正しい実装**:
```java
// ✅ 正しい実装: Application層専用のCommandオブジェクト
package com.api.todos.application.command.todo;

import java.util.UUID;

/**
 * TODO作成コマンド（Application層専用）
 * Pure Javaの不変オブジェクト
 */
public class CreateTodoCommand {
    
    private final String title;
    private final String descriptions;
    private final UUID userId;
    
    public CreateTodoCommand(String title, String descriptions, UUID userId) {
        // 入力検証
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be null or blank");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        
        this.title = title;
        this.descriptions = descriptions;
        this.userId = userId;
    }
    
    // Getter
    public String getTitle() { return title; }
    public String getDescriptions() { return descriptions; }
    public UUID getUserId() { return userId; }
}
```

```java
// ✅ UseCase: Application層のCommandを受け取る
package com.api.todos.application.usecase.todo;

import com.api.todos.application.command.todo.CreateTodoCommand;  // ✅ Application層
import com.api.todos.application.dto.TodoResult;
import com.api.todos.domain.repository.TodoRepository;

public class CreateTodoUseCase {
    
    private final TodoRepository todoRepository;
    
    public CreateTodoUseCase(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }
    
    // ✅ Application層のCommandを受け取る
    public TodoResult execute(CreateTodoCommand command) {
        Todo todo = Todo.create(
            command.getTitle(),
            command.getDescriptions(),
            command.getUserId()
        );
        
        return TodoResult.from(todoRepository.save(todo));
    }
}
```

```java
// ✅ Controller: DTO → Command変換
package com.api.todos.presentation.rest;

import com.api.todos.presentation.dto.CreateTodoRequest;
import com.api.todos.presentation.dto.TodoResponse;
import com.api.todos.application.command.todo.CreateTodoCommand;  // ✅ Command
import com.api.todos.application.dto.TodoResult;
import com.api.todos.infrastructure.service.CreateTodoService;

@RestController
@RequestMapping("/api/todos")
public class TodoController {
    
    private final CreateTodoService createTodoService;
    
    @PostMapping
    public ResponseEntity<TodoResponse> createTodo(
        @RequestBody CreateTodoRequest request,
        @RequestHeader("x-user-id") UUID userId
    ) {
        // ✅ DTO → Command変換（Presentation層の責務）
        CreateTodoCommand command = new CreateTodoCommand(
            request.getTitle(),
            request.getDescriptions(),
            userId
        );
        
        // ✅ CommandをUseCaseに渡す
        TodoResult result = createTodoService.execute(command);
        
        // ✅ Result → Response DTO変換
        return ResponseEntity.ok(TodoResponse.from(result));
    }
}
```

---

### ❌ 2. 可変オブジェクトとして実装（setterの提供）

**問題**: Commandオブジェクトにsetterを提供すると、意図しない変更が発生する可能性があります。

```java
// ❌ 絶対禁止: 可変オブジェクトとして実装
package com.api.todos.application.command.todo;

import java.util.UUID;

/**
 * ❌ 可変オブジェクト - setterで変更可能
 */
public class CreateTodoCommand {
    // ❌ finalなしの可変フィールド
    private String title;
    private String descriptions;
    private UUID userId;
    
    // ❌ デフォルトコンストラクタ
    public CreateTodoCommand() {
    }
    
    // ❌ setter（可変）
    public void setTitle(String title) { this.title = title; }
    public void setDescriptions(String descriptions) { this.descriptions = descriptions; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    // getter
    public String getTitle() { return title; }
    public String getDescriptions() { return descriptions; }
    public UUID getUserId() { return userId; }
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
package com.api.todos.application.command.todo;

import java.util.UUID;

/**
 * TODO作成コマンド（Application層専用）
 * 
 * 不変オブジェクト - finalフィールド、setterなし
 */
public class CreateTodoCommand {
    // ✅ finalフィールド（不変）
    private final String title;
    private final String descriptions;
    private final UUID userId;
    
    // ✅ コンストラクタで初期化（検証付き）
    public CreateTodoCommand(String title, String descriptions, UUID userId) {
        // 入力検証
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be null or blank");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        
        this.title = title;
        this.descriptions = descriptions;
        this.userId = userId;
    }
    
    // ✅ Getterのみ（setterなし）
    public String getTitle() { return title; }
    public String getDescriptions() { return descriptions; }
    public UUID getUserId() { return userId; }
}
```

---

### ❌ 3. Domain Entityを直接保持

**問題**: CommandオブジェクトがDomain Entityを直接保持すると、責務が曖昧になります。

```java
// ❌ 絶対禁止: Domain Entityを直接保持
package com.api.todos.application.command.todo;

import com.api.todos.domain.model.Todo;  // ❌ Domain Entityを保持

/**
 * ❌ Domain Entityを直接保持
 */
public class UpdateTodoCommand {
    // ❌ Domain Entityを保持
    private final Todo todo;
    
    public UpdateTodoCommand(Todo todo) {
        this.todo = todo;
    }
    
    public Todo getTodo() { return todo; }
}
```

```java
// ❌ UseCase側も問題
public class UpdateTodoUseCase {
    public TodoResult execute(UpdateTodoCommand command) {
        // ❌ CommandからDomain Entityを直接取得
        Todo todo = command.getTodo();
        
        // ビジネスロジック実行
        todo.updateTitle("New Title");
        
        return TodoResult.from(todoRepository.save(todo));
    }
}
```

**なぜダメか**:
- Commandの責務が曖昧（データ保持 or Entity保持？）
- UseCaseでEntity取得の責務が不明確
- Domain Entityの生成タイミングが不明
- テストが困難（Entityのモックが必要）

**正しい実装**:
```java
// ✅ 正しい実装: プリミティブなデータのみ保持
package com.api.todos.application.command.todo;

import java.util.UUID;

/**
 * TODO更新コマンド（Application層専用）
 * 
 * プリミティブなデータのみ保持
 * Domain Entityは保持しない
 */
public class UpdateTodoCommand {
    // ✅ プリミティブなデータのみ
    private final UUID todoId;
    private final String title;
    private final String descriptions;
    private final UUID userId;
    
    public UpdateTodoCommand(UUID todoId, String title, String descriptions, UUID userId) {
        if (todoId == null) {
            throw new IllegalArgumentException("Todo ID must not be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        
        this.todoId = todoId;
        this.title = title;
        this.descriptions = descriptions;
        this.userId = userId;
    }
    
    // Getter
    public UUID getTodoId() { return todoId; }
    public String getTitle() { return title; }
    public String getDescriptions() { return descriptions; }
    public UUID getUserId() { return userId; }
}
```

```java
// ✅ UseCase: Repository経由でEntity取得
public class UpdateTodoUseCase {
    
    private final TodoRepository todoRepository;
    
    public TodoResult execute(UpdateTodoCommand command) {
        // ✅ Repository経由でEntity取得（UseCase内でEntity取得）
        Todo todo = todoRepository.findById(command.getTodoId())
            .orElseThrow(() -> new TodoNotFoundException("TODO not found"));
        
        // ✅ Commandからデータ取得してビジネスロジック実行
        todo.updateTitle(command.getTitle());
        todo.updateDescriptions(command.getDescriptions());
        
        return TodoResult.from(todoRepository.save(todo));
    }
}
```

---

## ✅ 正しい実装パターン

### 1. CreateTodoCommand - TODO作成コマンド

**目的**: TODO作成に必要なパラメータを保持します。

**実装例**:
```java
// api/src/main/java/com/api/todos/application/command/todo/CreateTodoCommand.java
package com.api.todos.application.command.todo;

import java.util.UUID;
import java.util.Objects;

/**
 * TODO作成コマンド（Application層専用）
 * 
 * Pure Javaの不変オブジェクト
 * UseCaseの入力として使用
 * Presentation層のDTOとは分離
 */
public class CreateTodoCommand {
    
    // ========================================
    // フィールド（すべてfinal - 不変）
    // ========================================
    
    private final String title;
    private final String descriptions;
    private final UUID userId;
    
    // ========================================
    // コンストラクタ（検証付き）
    // ========================================
    
    /**
     * コンストラクタ
     * 
     * @param title TODOタイトル（必須）
     * @param descriptions TODO説明（任意）
     * @param userId ユーザーID（必須）
     * @throws IllegalArgumentException タイトルまたはユーザーIDがnullの場合
     */
    public CreateTodoCommand(String title, String descriptions, UUID userId) {
        // 入力検証
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be null or blank");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        
        this.title = title;
        this.descriptions = descriptions;
        this.userId = userId;
    }
    
    // ========================================
    // Getter（Pure Java）
    // ========================================
    
    public String getTitle() { return title; }
    public String getDescriptions() { return descriptions; }
    public UUID getUserId() { return userId; }
    
    // ========================================
    // equals, hashCode, toString
    // ========================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateTodoCommand that = (CreateTodoCommand) o;
        return Objects.equals(title, that.title) &&
               Objects.equals(descriptions, that.descriptions) &&
               Objects.equals(userId, that.userId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(title, descriptions, userId);
    }
    
    @Override
    public String toString() {
        return "CreateTodoCommand{" +
            "title='" + title + '\'' +
            ", descriptions='" + descriptions + '\'' +
            ", userId=" + userId +
            '}';
    }
}
```

**使用例（Controller内）**:
```java
// TodoController.java
@PostMapping
public ResponseEntity<TodoResponse> createTodo(
    @RequestBody CreateTodoRequest request,
    @RequestHeader("x-user-id") UUID userId
) {
    // Presentation層のDTO → Application層のCommand変換
    CreateTodoCommand command = new CreateTodoCommand(
        request.getTitle(),
        request.getDescriptions(),
        userId
    );
    
    // UseCase実行
    TodoResult result = createTodoService.execute(command);
    
    // Result → Response DTO変換
    return ResponseEntity.ok(TodoResponse.from(result));
}
```

---

### 2. UpdateTodoCommand - TODO更新コマンド

**目的**: TODO更新に必要なパラメータを保持します。

**実装例**:
```java
// api/src/main/java/com/api/todos/application/command/todo/UpdateTodoCommand.java
package com.api.todos.application.command.todo;

import java.util.UUID;
import java.util.Objects;

/**
 * TODO更新コマンド（Application層専用）
 * 
 * Pure Javaの不変オブジェクト
 * UseCaseの入力として使用
 */
public class UpdateTodoCommand {
    
    // ========================================
    // フィールド（すべてfinal - 不変）
    // ========================================
    
    private final UUID todoId;
    private final String title;
    private final String descriptions;
    private final UUID userId;
    
    // ========================================
    // コンストラクタ（検証付き）
    // ========================================
    
    /**
     * コンストラクタ
     * 
     * @param todoId TODO ID（必須）
     * @param title TODOタイトル（任意、nullの場合は更新しない）
     * @param descriptions TODO説明（任意、nullの場合は更新しない）
     * @param userId ユーザーID（必須、アクセス制御用）
     * @throws IllegalArgumentException TODO IDまたはユーザーIDがnullの場合
     */
    public UpdateTodoCommand(UUID todoId, String title, String descriptions, UUID userId) {
        // 入力検証
        if (todoId == null) {
            throw new IllegalArgumentException("Todo ID must not be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        
        this.todoId = todoId;
        this.title = title;
        this.descriptions = descriptions;
        this.userId = userId;
    }
    
    // ========================================
    // Getter（Pure Java）
    // ========================================
    
    public UUID getTodoId() { return todoId; }
    public String getTitle() { return title; }
    public String getDescriptions() { return descriptions; }
    public UUID getUserId() { return userId; }
    
    // ========================================
    // ヘルパーメソッド
    // ========================================
    
    /**
     * タイトル更新が必要か判定
     */
    public boolean shouldUpdateTitle() {
        return title != null && !title.isBlank();
    }
    
    /**
     * 説明更新が必要か判定
     */
    public boolean shouldUpdateDescriptions() {
        return descriptions != null;
    }
    
    // ========================================
    // equals, hashCode, toString
    // ========================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateTodoCommand that = (UpdateTodoCommand) o;
        return Objects.equals(todoId, that.todoId) &&
               Objects.equals(title, that.title) &&
               Objects.equals(descriptions, that.descriptions) &&
               Objects.equals(userId, that.userId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(todoId, title, descriptions, userId);
    }
    
    @Override
    public String toString() {
        return "UpdateTodoCommand{" +
            "todoId=" + todoId +
            ", title='" + title + '\'' +
            ", descriptions='" + descriptions + '\'' +
            ", userId=" + userId +
            '}';
    }
}
```

**使用例（UseCase内）**:
```java
// UpdateTodoUseCase.java
public class UpdateTodoUseCase {
    
    public TodoResult execute(UpdateTodoCommand command) {
        // 1. Repository経由でEntity取得
        Todo todo = todoRepository.findById(command.getTodoId())
            .orElseThrow(() -> new TodoNotFoundException("TODO not found"));
        
        // 2. アクセス制御（所有者チェック）
        User user = userRepository.findById(command.getUserId())
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        if (!todoDomainService.isOwner(todo, user)) {
            throw new AccessDeniedException("You are not the owner of this TODO");
        }
        
        // 3. Commandのヘルパーメソッドを使用して条件分岐
        if (command.shouldUpdateTitle()) {
            todo.updateTitle(command.getTitle());
        }
        
        if (command.shouldUpdateDescriptions()) {
            todo.updateDescriptions(command.getDescriptions());
        }
        
        // 4. 保存して結果返却
        return TodoResult.from(todoRepository.save(todo));
    }
}
```

---

### 3. InitializePasswordCommand - パスワード初期化コマンド

**目的**: ユーザーのパスワード初期化に必要なパラメータを保持します。

**実装例**:
```java
// api/src/main/java/com/api/todos/application/command/auth/InitializePasswordCommand.java
package com.api.todos.application.command.auth;

import java.util.UUID;
import java.util.Objects;

/**
 * パスワード初期化コマンド（Application層専用）
 * 
 * Pure Javaの不変オブジェクト
 * UseCaseの入力として使用
 * 
 * 注意: パスワードは既にハッシュ化済みの状態で渡される
 *       ハッシュ化はInfrastructure層（Controller or Service）の責務
 */
public class InitializePasswordCommand {
    
    // ========================================
    // フィールド（すべてfinal - 不変）
    // ========================================
    
    private final UUID userId;
    private final String hashedPassword;
    
    // ========================================
    // コンストラクタ（検証付き）
    // ========================================
    
    /**
     * コンストラクタ
     * 
     * @param userId ユーザーID（必須）
     * @param hashedPassword ハッシュ化済みパスワード（必須）
     * @throws IllegalArgumentException ユーザーIDまたはハッシュ化パスワードがnullの場合
     */
    public InitializePasswordCommand(UUID userId, String hashedPassword) {
        // 入力検証
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (hashedPassword == null || hashedPassword.isBlank()) {
            throw new IllegalArgumentException("Hashed password must not be null or blank");
        }
        
        this.userId = userId;
        this.hashedPassword = hashedPassword;
    }
    
    // ========================================
    // Getter（Pure Java）
    // ========================================
    
    public UUID getUserId() { return userId; }
    public String getHashedPassword() { return hashedPassword; }
    
    // ========================================
    // equals, hashCode, toString
    // ========================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InitializePasswordCommand that = (InitializePasswordCommand) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(hashedPassword, that.hashedPassword);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId, hashedPassword);
    }
    
    @Override
    public String toString() {
        return "InitializePasswordCommand{" +
            "userId=" + userId +
            ", hashedPassword='[PROTECTED]'" +  // パスワードは出力しない
            '}';
    }
}
```

**使用例（Controller内）**:
```java
// AuthController.java
@PostMapping("/initialize-password")
public ResponseEntity<UserResponse> initializePassword(
    @RequestBody InitializePasswordRequest request,
    @RequestHeader("x-user-id") UUID userId
) {
    // 1. パスワードをハッシュ化（Infrastructure層の責務）
    String hashedPassword = passwordEncoder.encode(request.getPassword());
    
    // 2. DTO → Command変換
    InitializePasswordCommand command = new InitializePasswordCommand(
        userId,
        hashedPassword
    );
    
    // 3. UseCase実行
    UserResult result = initializePasswordService.execute(command);
    
    // 4. Result → Response DTO変換
    return ResponseEntity.ok(UserResponse.from(result));
}
```

---

### 4. 複合的なCommand - TodoQueryCommand（検索条件）

**目的**: TODO検索に必要な複数の条件を保持します。

**実装例**:
```java
// api/src/main/java/com/api/todos/application/command/todo/TodoQueryCommand.java
package com.api.todos.application.command.todo;

import java.util.UUID;
import java.util.Objects;

/**
 * TODO検索クエリコマンド（Application層専用）
 * 
 * Pure Javaの不変オブジェクト
 * 複数の検索条件を保持
 */
public class TodoQueryCommand {
    
    // ========================================
    // フィールド（すべてfinal - 不変）
    // ========================================
    
    private final UUID userId;
    private final Boolean completed;  // nullの場合は全件
    private final Boolean deleted;    // nullの場合は削除済み除外
    private final String titleKeyword; // 部分一致検索用
    
    // ========================================
    // コンストラクタ（検証付き）
    // ========================================
    
    /**
     * コンストラクタ
     * 
     * @param userId ユーザーID（必須）
     * @param completed 完了状態フィルタ（null=全件、true=完了のみ、false=未完了のみ）
     * @param deleted 削除状態フィルタ（null=削除済み除外、true=削除済みのみ、false=未削除のみ）
     * @param titleKeyword タイトル検索キーワード（null=検索なし）
     * @throws IllegalArgumentException ユーザーIDがnullの場合
     */
    public TodoQueryCommand(UUID userId, Boolean completed, Boolean deleted, String titleKeyword) {
        // 入力検証
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        
        this.userId = userId;
        this.completed = completed;
        this.deleted = deleted != null ? deleted : false;  // デフォルト: 削除済み除外
        this.titleKeyword = titleKeyword;
    }
    
    // ========================================
    // Getter（Pure Java）
    // ========================================
    
    public UUID getUserId() { return userId; }
    public Boolean getCompleted() { return completed; }
    public Boolean getDeleted() { return deleted; }
    public String getTitleKeyword() { return titleKeyword; }
    
    // ========================================
    // ヘルパーメソッド
    // ========================================
    
    /**
     * 完了状態フィルタが必要か判定
     */
    public boolean shouldFilterByCompleted() {
        return completed != null;
    }
    
    /**
     * タイトル検索が必要か判定
     */
    public boolean shouldSearchByTitle() {
        return titleKeyword != null && !titleKeyword.isBlank();
    }
    
    // ========================================
    // equals, hashCode, toString
    // ========================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TodoQueryCommand that = (TodoQueryCommand) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(completed, that.completed) &&
               Objects.equals(deleted, that.deleted) &&
               Objects.equals(titleKeyword, that.titleKeyword);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId, completed, deleted, titleKeyword);
    }
    
    @Override
    public String toString() {
        return "TodoQueryCommand{" +
            "userId=" + userId +
            ", completed=" + completed +
            ", deleted=" + deleted +
            ", titleKeyword='" + titleKeyword + '\'' +
            '}';
    }
}
```

---

## 🧪 テスト戦略

### Command検証テスト（Pure Javaユニットテスト）

**目的**: Commandオブジェクトの入力検証が正しく動作するかテストします。

**実装例**:
```java
package com.api.todos.application.command.todo;

import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * CreateTodoCommand テスト
 * Pure Javaのユニットテスト - モック不要、Springコンテキスト不要
 */
class CreateTodoCommandTest {
    
    @Test
    void 有効なパラメータでCommandを作成できること() {
        // Given
        String title = "Test TODO";
        String descriptions = "Test Description";
        UUID userId = UUID.randomUUID();
        
        // When
        CreateTodoCommand command = new CreateTodoCommand(title, descriptions, userId);
        
        // Then
        assertThat(command).isNotNull();
        assertThat(command.getTitle()).isEqualTo(title);
        assertThat(command.getDescriptions()).isEqualTo(descriptions);
        assertThat(command.getUserId()).isEqualTo(userId);
    }
    
    @Test
    void タイトルがnullの場合は例外をスローすること() {
        // Given
        UUID userId = UUID.randomUUID();
        
        // When & Then
        assertThatThrownBy(() -> new CreateTodoCommand(null, "Description", userId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Title must not be null or blank");
    }
    
    @Test
    void タイトルが空文字の場合は例外をスローすること() {
        // Given
        UUID userId = UUID.randomUUID();
        
        // When & Then
        assertThatThrownBy(() -> new CreateTodoCommand("", "Description", userId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Title must not be null or blank");
    }
    
    @Test
    void タイトルがブランクの場合は例外をスローすること() {
        // Given
        UUID userId = UUID.randomUUID();
        
        // When & Then
        assertThatThrownBy(() -> new CreateTodoCommand("   ", "Description", userId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Title must not be null or blank");
    }
    
    @Test
    void ユーザーIDがnullの場合は例外をスローすること() {
        // When & Then
        assertThatThrownBy(() -> new CreateTodoCommand("Test TODO", "Description", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User ID must not be null");
    }
    
    @Test
    void 説明がnullでもCommandを作成できること() {
        // Given
        String title = "Test TODO";
        UUID userId = UUID.randomUUID();
        
        // When
        CreateTodoCommand command = new CreateTodoCommand(title, null, userId);
        
        // Then
        assertThat(command).isNotNull();
        assertThat(command.getTitle()).isEqualTo(title);
        assertThat(command.getDescriptions()).isNull();
        assertThat(command.getUserId()).isEqualTo(userId);
    }
    
    @Test
    void equalsとhashCodeが正しく動作すること() {
        // Given
        UUID userId = UUID.randomUUID();
        CreateTodoCommand command1 = new CreateTodoCommand("TODO", "Desc", userId);
        CreateTodoCommand command2 = new CreateTodoCommand("TODO", "Desc", userId);
        CreateTodoCommand command3 = new CreateTodoCommand("TODO2", "Desc2", userId);
        
        // When & Then
        assertThat(command1).isEqualTo(command2);
        assertThat(command1).isNotEqualTo(command3);
        assertThat(command1.hashCode()).isEqualTo(command2.hashCode());
    }
}
```

```java
package com.api.todos.application.command.todo;

import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * UpdateTodoCommand テスト
 */
class UpdateTodoCommandTest {
    
    @Test
    void 有効なパラメータでCommandを作成できること() {
        // Given
        UUID todoId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        // When
        UpdateTodoCommand command = new UpdateTodoCommand(
            todoId, "New Title", "New Description", userId
        );
        
        // Then
        assertThat(command).isNotNull();
        assertThat(command.getTodoId()).isEqualTo(todoId);
        assertThat(command.getTitle()).isEqualTo("New Title");
        assertThat(command.getDescriptions()).isEqualTo("New Description");
        assertThat(command.getUserId()).isEqualTo(userId);
    }
    
    @Test
    void TODO_IDがnullの場合は例外をスローすること() {
        // Given
        UUID userId = UUID.randomUUID();
        
        // When & Then
        assertThatThrownBy(() -> 
            new UpdateTodoCommand(null, "Title", "Description", userId)
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Todo ID must not be null");
    }
    
    @Test
    void shouldUpdateTitleメソッドが正しく動作すること() {
        // Given
        UUID todoId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        // When: タイトルあり
        UpdateTodoCommand commandWithTitle = new UpdateTodoCommand(
            todoId, "New Title", "Description", userId
        );
        
        // Then
        assertThat(commandWithTitle.shouldUpdateTitle()).isTrue();
        
        // When: タイトルnull
        UpdateTodoCommand commandWithoutTitle = new UpdateTodoCommand(
            todoId, null, "Description", userId
        );
        
        // Then
        assertThat(commandWithoutTitle.shouldUpdateTitle()).isFalse();
    }
    
    @Test
    void shouldUpdateDescriptionsメソッドが正しく動作すること() {
        // Given
        UUID todoId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        // When: 説明あり
        UpdateTodoCommand commandWithDesc = new UpdateTodoCommand(
            todoId, "Title", "New Description", userId
        );
        
        // Then
        assertThat(commandWithDesc.shouldUpdateDescriptions()).isTrue();
        
        // When: 説明null
        UpdateTodoCommand commandWithoutDesc = new UpdateTodoCommand(
            todoId, "Title", null, userId
        );
        
        // Then
        assertThat(commandWithoutDesc.shouldUpdateDescriptions()).isFalse();
    }
}
```

---

## ✅ 実装チェックリスト

### Command実装時

- [ ] **Pure Java**で実装（Spring/Lombokアノテーション使用禁止）
- [ ] **不変オブジェクト**として実装（finalフィールド、setterなし）
- [ ] **コンストラクタで検証**（null チェック、必須パラメータ確認）
- [ ] **Getterのみ提供**（setterは提供しない）
- [ ] **プリミティブなデータのみ保持**（Domain Entityは保持しない）
- [ ] **equals/hashCode**を実装
- [ ] **toString**でデバッグ情報を提供（機密情報を除く）
- [ ] **ヘルパーメソッド**で条件判定を提供（shouldUpdateTitle等）
- [ ] **Pure Javaユニットテスト**を実装

### 対応する他のコンポーネント

- [ ] **Presentation層**: Request DTO（Commandに変換される）が存在
- [ ] **Presentation層**: Controller（DTO→Command変換）が実装済み
- [ ] **Application層**: UseCase（Commandを受け取る）が存在
- [ ] **Infrastructure層**: Service（Commandをそのまま渡す）が存在
- [ ] **テスト**: Command検証テストが実装済み

---

## 📚 参考資料

### プロジェクト内ドキュメント
- **[AGENTS.md](../../../../../AGENTS.md)** - AIエージェント向けコンテキストドキュメント
- **[Application層 README](../README.md)** - Application層全体の概要
- **[Application層 usecase README](../usecase/README.md)** - UseCase実装パターン
- **[Application層 dto README](../dto/README.md)** - Result実装パターン
- **[Presentation層 dto README](../../presentation/dto/README.md)** - Request/Response DTO実装パターン

### 外部参考資料
- [Clean Architecture (Robert C. Martin)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Effective Java (Joshua Bloch)](https://www.oreilly.com/library/view/effective-java/9780134686097/)

---

## 🎯 まとめ

Application層のCommandオブジェクトは、**UseCaseの入力を表現する不変オブジェクト**であり、Presentation層とApplication層の境界で使用されます。

### 重要ポイント

1. **Pure Java（最重要）**
   - Spring、Lombokアノテーション使用禁止
   - フレームワークに一切依存しない
   - Pure Javaの不変オブジェクト

2. **不変性の保証**
   - finalフィールド、setterなし
   - コンストラクタで初期化
   - スレッドセーフ

3. **Application層専用**
   - Presentation層のDTOとは分離
   - UseCaseの入力として使用
   - プリミティブなデータのみ保持

4. **入力検証**
   - コンストラクタでの基本的な検証
   - null チェック、必須パラメータ確認
   - ビジネスルールではなく、入力の妥当性検証

### 禁止事項

- ❌ Presentation層のDTOをUseCaseに渡す
- ❌ 可変オブジェクトとして実装（setterの提供）
- ❌ Domain Entityを直接保持

このREADMEを参考に、**Pure Javaで実装された高品質なCommandオブジェクト**を構築してください。Commandオブジェクトは、Presentation層とApplication層の境界を明確にし、クリーンアーキテクチャの原則を実現する重要なコンポーネントです。

---

**作成日**: 2026年1月5日  
**対象バージョン**: Spring Boot TODO API v0.0.1-SNAPSHOT  
**対象パッケージ**: `com.api.todos.application.command`
