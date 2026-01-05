# Domain層 - service パッケージ（ドメインサービス）

## 📋 パッケージ概要

**場所**: `api/src/main/java/com/api/todos/domain/service/`

**目的**: **単一のエンティティに属さないビジネスロジック**や**複数エンティティを跨るドメインロジック**を Pure Java で実装するパッケージです。Domain Serviceは、Entityに含めるのが不自然なビジネスルールを実装します。

**主要コンポーネント**:
- **TodoDomainService**: TODO関連のドメインサービス
- **UserDomainService**: ユーザー関連のドメインサービス（必要に応じて）

---

## 🏛️ クリーンアーキテクチャにおける位置づけ

```
┌─────────────────────────────────────────────────┐
│  Presentation層 (Controllers, REST API)         │
├─────────────────────────────────────────────────┤
│  Infrastructure層 (Persistence, Security)       │
├─────────────────────────────────────────────────┤
│  Application層 (UseCases - Pure Java)           │
│     ↓ 使用                                      │
├─────────────────────────────────────────────────┤
│  Domain層（最内層）                             │
│  ├── model/ (Entity, Value Object)              │
│  ├── repository/ (Repository Interface)         │
│  └── service/ ← ★ このパッケージ               │
│      - Domain Service（Pure Java）              │
│      - 複数エンティティを跨るロジック           │
└─────────────────────────────────────────────────┘
```

### 依存関係の方向

```
Application層（UseCase）
    ↓ 使用
Domain層（Domain Service）
    ↓ 使用
Domain層（Entity, Repository Interface）
```

**重要**: Domain Serviceは **Pure Java** で実装し、Domain層の他のコンポーネント（Entity, Repository Interface）のみに依存します。Application層のUseCaseから呼び出されます。

---

## 🎯 責務（Responsibilities）

### ✅ このパッケージが行うべきこと

1. **単一のエンティティに属さないビジネスロジックの実装**
   - 複数のEntityを使用する処理
   - Entityのどれに属するか不明確なビジネスルール

2. **複数エンティティを跨るドメインロジックの実装**
   - Todo と User を組み合わせた所有権チェック
   - 複数のTodoを集計・フィルタリングするロジック

3. **ドメイン固有のビジネスポリシーの実装**
   - ビジネスルールの検証
   - ドメイン固有の計算ロジック

4. **Pure Javaでの実装**
   - Spring、JPA等のフレームワークアノテーション使用禁止
   - フレームワークに依存しないビジネスロジック

### ❌ このパッケージが行ってはいけないこと

1. **単一Entityのビジネスロジック**
   - → Entity内のメソッドとして実装すべき
   - 例: `todo.markAsCompleted()` はTodo Entityのメソッド

2. **データベースアクセス**
   - → Repository Interfaceを使用してApplication層で実行
   - Domain Serviceは直接データベースにアクセスしない

3. **Application層の責務（ユースケースのオーケストレーション）**
   - → UseCaseクラスで実装すべき
   - Domain Serviceは再利用可能なドメインロジックのみ

4. **外部フレームワークへの依存**
   - → Pure Javaで実装（Spring/JPAアノテーション禁止）

---

## 🤔 Domain Service vs Entity vs UseCase

### Domain Service、Entity、UseCaseの違い

| 観点 | Entity | Domain Service | UseCase（Application層） |
|------|--------|----------------|--------------------------|
| **場所** | domain/model/ | domain/service/ | application/usecase/ |
| **責務** | 単一エンティティのビジネスルール | 複数エンティティを跨るドメインロジック | ユースケースのオーケストレーション |
| **依存** | Pure Java | Pure Java（Entity, Repository Interface） | Pure Java（Domain層のみ） |
| **アノテーション** | なし | なし | なし（Infrastructure層でラップ） |
| **例** | `todo.markAsCompleted()` | `isOwner(todo, user)` | `createTodo(command)` |
| **データアクセス** | なし | なし（Repositoryは使わない） | Repository Interface経由 |
| **トランザクション** | なし | なし | Infrastructure層でラップして適用 |

### 使い分けの判断基準

```
ビジネスロジックをどこに配置すべきか？
  ↓
単一のEntityに属するか？
  YES → Entity内のメソッドとして実装
  NO  → 次へ
  ↓
複数のEntityを使用するか？
  YES → Domain Serviceとして実装
  NO  → 次へ
  ↓
データアクセス・トランザクション管理が必要か？
  YES → Application層のUseCaseとして実装
  NO  → Domain Serviceとして実装
```

### 具体例

#### ✅ Entity内に配置すべきロジック
```java
// ✅ 単一のTodo Entityに属するビジネスルール
public class Todo {
    public void markAsCompleted() {
        if (this.completed) {
            throw new IllegalStateException("TODO is already completed");
        }
        this.completed = true;
        this.updatedAt = LocalDateTime.now();
    }
}
```

#### ✅ Domain Serviceに配置すべきロジック
```java
// ✅ Todo と User を組み合わせた所有権チェック
public class TodoDomainService {
    public boolean isOwner(Todo todo, User user) {
        return todo.getUserId().equals(user.getId());
    }
}
```

#### ✅ UseCaseに配置すべきロジック
```java
// ✅ データアクセス・トランザクション管理を含むオーケストレーション
public class CreateTodoUseCase {
    public TodoResult execute(CreateTodoCommand command) {
        // 1. ユーザー存在確認（Repository使用）
        User user = userRepository.findById(command.getUserId())
            .orElseThrow(() -> new UserNotFoundException());
        
        // 2. Todo作成（Domain Entity使用）
        Todo todo = Todo.create(command.getTitle(), command.getDescriptions(), user.getId());
        
        // 3. 保存（Repository使用）
        Todo saved = todoRepository.save(todo);
        
        return TodoResult.from(saved);
    }
}
```

---

## 🚨 絶対原則：Pure Java（フレームワーク依存禁止）

Domain Serviceは **Pure Java** で実装しなければなりません。以下のアノテーション・ライブラリは **絶対に使用禁止** です：

### ❌ 禁止されるアノテーション・ライブラリ

```java
// ❌ Spring Framework
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

// ❌ JPA/Hibernate
import jakarta.persistence.*;

// ❌ Lombok（Domain層では非推奨）
import lombok.RequiredArgsConstructor;

// ❌ Bean Validation
import jakarta.validation.constraints.NotNull;
```

### ✅ 許可されるもの

```java
// ✅ Java標準ライブラリ
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// ✅ Domain層のEntity、Value Object
import com.api.todos.domain.model.Todo;
import com.api.todos.domain.model.User;
import com.api.todos.domain.model.UserRole;

// ✅ Pure Javaのコンストラクタ・メソッド
public class TodoDomainService {
    // Pure Javaのメソッド
    public boolean isOwner(Todo todo, User user) {
        return todo.getUserId().equals(user.getId());
    }
}
```

---

## 🚨 禁止パターン（Anti-Patterns）

### ❌ 1. Domain ServiceでSpringアノテーションを使用

**問題**: Domain Serviceがフレームワークに依存してしまい、Pure Javaの原則に違反します。

```java
// ❌ 絶対禁止: Domain ServiceでSpringアノテーションを使用
package com.api.todos.domain.service;

import org.springframework.stereotype.Service;
import com.api.todos.domain.model.Todo;
import com.api.todos.domain.model.User;

@Service  // ❌ Domain層でSpringアノテーション使用禁止
public class TodoDomainService {
    
    public boolean isOwner(Todo todo, User user) {
        return todo.getUserId().equals(user.getId());
    }
}
```

**なぜダメか**:
- Domain層がSpring Frameworkに依存してしまう
- Pure Javaの原則に違反
- テストが困難（Springコンテキスト必須）
- フレームワーク変更時に影響を受ける

**正しい実装**:
```java
// ✅ 正しい実装: Domain ServiceはPure Java
package com.api.todos.domain.service;

import com.api.todos.domain.model.Todo;
import com.api.todos.domain.model.User;

/**
 * TODOドメインサービス（Domain層）
 * 
 * Pure Javaで実装 - フレームワーク依存なし
 * アノテーションなし
 */
public class TodoDomainService {
    
    /**
     * ユーザーがTODOの所有者かチェック
     */
    public boolean isOwner(Todo todo, User user) {
        return todo.getUserId().equals(user.getId());
    }
}
```

```java
// ✅ Application層のUseCaseから使用
package com.api.todos.application.usecase.todo;

import com.api.todos.domain.service.TodoDomainService;

public class UpdateTodoUseCase {
    private final TodoRepository todoRepository;
    private final TodoDomainService todoDomainService;  // ✅ Pure Javaのインスタンス
    
    public UpdateTodoUseCase(TodoRepository todoRepository, TodoDomainService todoDomainService) {
        this.todoRepository = todoRepository;
        this.todoDomainService = todoDomainService;
    }
    
    public TodoResult execute(UpdateTodoCommand command) {
        Todo todo = todoRepository.findById(command.getTodoId())
            .orElseThrow(() -> new TodoNotFoundException());
        
        User user = userRepository.findById(command.getUserId())
            .orElseThrow(() -> new UserNotFoundException());
        
        // Domain Serviceを使用して所有権チェック
        if (!todoDomainService.isOwner(todo, user)) {
            throw new UnauthorizedException("Not the owner of this TODO");
        }
        
        todo.updateTitle(command.getTitle());
        return TodoResult.from(todoRepository.save(todo));
    }
}
```

---

### ❌ 2. Domain ServiceでRepositoryを直接使用

**問題**: Domain ServiceがRepositoryに依存すると、データアクセスロジックがDomain層に漏れ出します。

```java
// ❌ 絶対禁止: Domain ServiceでRepositoryを直接使用
package com.api.todos.domain.service;

import com.api.todos.domain.model.Todo;
import com.api.todos.domain.repository.TodoRepository;
import java.util.List;
import java.util.UUID;

public class TodoDomainService {
    private final TodoRepository todoRepository;  // ❌ Repositoryを保持
    
    public TodoDomainService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }
    
    // ❌ データアクセスをDomain Service内で実行
    public List<Todo> findIncompleteTodosByUser(UUID userId) {
        List<Todo> todos = todoRepository.findByUserId(userId);  // ❌ Repository使用
        return todos.stream()
            .filter(todo -> !todo.isCompleted())
            .toList();
    }
}
```

**なぜダメか**:
- Domain ServiceがRepositoryに依存（データアクセス層への依存）
- データアクセスロジックとビジネスロジックが混在
- テストが困難（Repository実装が必要）
- Application層の責務をDomain層に含めてしまっている

**正しい実装**:
```java
// ✅ 正しい実装: Domain Serviceはデータアクセスしない
package com.api.todos.domain.service;

import com.api.todos.domain.model.Todo;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TODOドメインサービス
 * 
 * Pure Java実装 - Repositoryに依存しない
 * 引数として渡されたデータに対してビジネスロジックを実行
 */
public class TodoDomainService {
    
    /**
     * 未完了TODO一覧を取得
     * 
     * @param todos TODO一覧（引数として渡される）
     * @return 未完了TODO一覧
     */
    public List<Todo> filterIncompleteTodos(List<Todo> todos) {
        return todos.stream()
            .filter(todo -> !todo.isCompleted())
            .filter(todo -> !todo.isDeleted())
            .collect(Collectors.toList());
    }
}
```

```java
// ✅ Application層のUseCaseでRepositoryを使用
package com.api.todos.application.usecase.todo;

import com.api.todos.domain.service.TodoDomainService;

public class GetIncompleteTodosUseCase {
    private final TodoRepository todoRepository;
    private final TodoDomainService todoDomainService;
    
    public GetIncompleteTodosUseCase(TodoRepository todoRepository, TodoDomainService todoDomainService) {
        this.todoRepository = todoRepository;
        this.todoDomainService = todoDomainService;
    }
    
    public List<TodoResult> execute(UUID userId) {
        // 1. Repositoryでデータ取得（Application層の責務）
        List<Todo> todos = todoRepository.findByUserId(userId);
        
        // 2. Domain Serviceでフィルタリング（Domain層のビジネスロジック）
        List<Todo> incompleteTodos = todoDomainService.filterIncompleteTodos(todos);
        
        // 3. Result変換
        return incompleteTodos.stream()
            .map(TodoResult::from)
            .toList();
    }
}
```

---

### ❌ 3. 単一EntityのビジネスルールをDomain Serviceに実装

**問題**: 単一のEntityに属するビジネスルールは、Entity内のメソッドとして実装すべきです。

```java
// ❌ 絶対禁止: 単一EntityのビジネスルールをDomain Serviceに実装
package com.api.todos.domain.service;

import com.api.todos.domain.model.Todo;
import java.time.LocalDateTime;

public class TodoDomainService {
    
    // ❌ これはTodo Entityのメソッドとして実装すべき
    public void markTodoAsCompleted(Todo todo) {
        if (todo.isCompleted()) {
            throw new IllegalStateException("TODO is already completed");
        }
        // ❌ Entity内部状態を外部から変更（カプセル化違反）
        // todo.setCompleted(true);
        // todo.setUpdatedAt(LocalDateTime.now());
    }
    
    // ❌ これもTodo Entityのメソッドとして実装すべき
    public void updateTodoTitle(Todo todo, String newTitle) {
        if (newTitle == null || newTitle.isEmpty()) {
            throw new IllegalArgumentException("Title must not be empty");
        }
        // ❌ Entity内部状態を外部から変更
        // todo.setTitle(newTitle);
        // todo.setUpdatedAt(LocalDateTime.now());
    }
}
```

**なぜダメか**:
- 単一Entityのビジネスルールは Entity内に配置すべき
- カプセル化違反（Entity内部状態を外部から変更）
- Entityが貧血モデル（Anemic Domain Model）になる
- ビジネスロジックが分散して保守性が低下

**正しい実装**:
```java
// ✅ 正しい実装: 単一Entityのビジネスルールは Entity内に実装
package com.api.todos.domain.model;

import java.util.UUID;
import java.time.LocalDateTime;

public class Todo {
    private final UUID id;
    private String title;
    private boolean completed;
    private LocalDateTime updatedAt;
    
    // ✅ Entity内にビジネスロジックを実装
    public void markAsCompleted() {
        if (this.completed) {
            throw new IllegalStateException("TODO is already completed");
        }
        this.completed = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    // ✅ Entity内にビジネスロジックを実装
    public void updateTitle(String newTitle) {
        if (newTitle == null || newTitle.isEmpty()) {
            throw new IllegalArgumentException("Title must not be empty");
        }
        if (newTitle.length() > 32) {
            throw new IllegalArgumentException("Title must not exceed 32 characters");
        }
        this.title = newTitle;
        this.updatedAt = LocalDateTime.now();
    }
}
```

```java
// ✅ Domain Serviceは複数Entityを跨るロジックのみ
package com.api.todos.domain.service;

import com.api.todos.domain.model.Todo;
import com.api.todos.domain.model.User;

public class TodoDomainService {
    
    // ✅ Todo と User を組み合わせたビジネスロジック
    public boolean isOwner(Todo todo, User user) {
        return todo.getUserId().equals(user.getId());
    }
    
    // ✅ 複数のTodoを集計するロジック
    public long countCompletedTodos(List<Todo> todos) {
        return todos.stream()
            .filter(Todo::isCompleted)
            .filter(todo -> !todo.isDeleted())
            .count();
    }
}
```

---

## ✅ 正しい実装パターン

### 1. TodoDomainService - 基本実装

**目的**: TODO関連の複数エンティティを跨るビジネスロジックを実装します。

**実装例**:
```java
// api/src/main/java/com/api/todos/domain/service/TodoDomainService.java
package com.api.todos.domain.service;

import com.api.todos.domain.model.Todo;
import com.api.todos.domain.model.User;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TODOドメインサービス（Domain層）
 * 
 * Pure Javaで実装 - フレームワーク依存なし
 * 複数エンティティを跨るドメインロジックを実装
 */
public class TodoDomainService {
    
    // ========================================
    // 所有権チェック
    // ========================================
    
    /**
     * ユーザーがTODOの所有者かチェック
     * 
     * @param todo TODO
     * @param user ユーザー
     * @return 所有者の場合true
     */
    public boolean isOwner(Todo todo, User user) {
        if (todo == null) {
            throw new IllegalArgumentException("Todo must not be null");
        }
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        return todo.getUserId().equals(user.getId());
    }
    
    // ========================================
    // フィルタリング
    // ========================================
    
    /**
     * 未完了TODO一覧を取得
     * 
     * @param todos TODO一覧
     * @return 未完了TODO一覧（削除済みは除外）
     */
    public List<Todo> filterIncompleteTodos(List<Todo> todos) {
        if (todos == null) {
            throw new IllegalArgumentException("Todos must not be null");
        }
        return todos.stream()
            .filter(todo -> !todo.isCompleted())
            .filter(todo -> !todo.isDeleted())
            .collect(Collectors.toList());
    }
    
    /**
     * 完了TODO一覧を取得
     * 
     * @param todos TODO一覧
     * @return 完了TODO一覧（削除済みは除外）
     */
    public List<Todo> filterCompletedTodos(List<Todo> todos) {
        if (todos == null) {
            throw new IllegalArgumentException("Todos must not be null");
        }
        return todos.stream()
            .filter(Todo::isCompleted)
            .filter(todo -> !todo.isDeleted())
            .collect(Collectors.toList());
    }
    
    /**
     * 削除されていないTODO一覧を取得
     * 
     * @param todos TODO一覧
     * @return アクティブなTODO一覧
     */
    public List<Todo> filterActiveTodos(List<Todo> todos) {
        if (todos == null) {
            throw new IllegalArgumentException("Todos must not be null");
        }
        return todos.stream()
            .filter(todo -> !todo.isDeleted())
            .collect(Collectors.toList());
    }
    
    // ========================================
    // 集計・統計
    // ========================================
    
    /**
     * 完了TODO数をカウント
     * 
     * @param todos TODO一覧
     * @return 完了TODO数
     */
    public long countCompletedTodos(List<Todo> todos) {
        if (todos == null) {
            throw new IllegalArgumentException("Todos must not be null");
        }
        return todos.stream()
            .filter(Todo::isCompleted)
            .filter(todo -> !todo.isDeleted())
            .count();
    }
    
    /**
     * 未完了TODO数をカウント
     * 
     * @param todos TODO一覧
     * @return 未完了TODO数
     */
    public long countIncompleteTodos(List<Todo> todos) {
        if (todos == null) {
            throw new IllegalArgumentException("Todos must not be null");
        }
        return todos.stream()
            .filter(todo -> !todo.isCompleted())
            .filter(todo -> !todo.isDeleted())
            .count();
    }
    
    /**
     * TODO完了率を計算
     * 
     * @param todos TODO一覧
     * @return 完了率（0.0～1.0）
     */
    public double calculateCompletionRate(List<Todo> todos) {
        if (todos == null) {
            throw new IllegalArgumentException("Todos must not be null");
        }
        
        List<Todo> activeTodos = filterActiveTodos(todos);
        if (activeTodos.isEmpty()) {
            return 0.0;
        }
        
        long completedCount = countCompletedTodos(activeTodos);
        return (double) completedCount / activeTodos.size();
    }
    
    // ========================================
    // ビジネスポリシー
    // ========================================
    
    /**
     * ユーザーがTODO一覧の所有者かチェック
     * 
     * @param todos TODO一覧
     * @param user ユーザー
     * @return すべてのTODOの所有者の場合true
     */
    public boolean isOwnerOfAllTodos(List<Todo> todos, User user) {
        if (todos == null) {
            throw new IllegalArgumentException("Todos must not be null");
        }
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        
        return todos.stream()
            .allMatch(todo -> isOwner(todo, user));
    }
    
    /**
     * ユーザーが指定されたTODOの少なくとも1つの所有者かチェック
     * 
     * @param todos TODO一覧
     * @param user ユーザー
     * @return 少なくとも1つのTODOの所有者の場合true
     */
    public boolean isOwnerOfAnyTodo(List<Todo> todos, User user) {
        if (todos == null) {
            throw new IllegalArgumentException("Todos must not be null");
        }
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        
        return todos.stream()
            .anyMatch(todo -> isOwner(todo, user));
    }
}
```

---

### 2. UserDomainService - ユーザー関連ドメインサービス（オプション）

**目的**: ユーザー関連の複数エンティティを跨るビジネスロジックを実装します。

**実装例**:
```java
// api/src/main/java/com/api/todos/domain/service/UserDomainService.java
package com.api.todos.domain.service;

import com.api.todos.domain.model.User;
import com.api.todos.domain.model.UserRole;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ユーザードメインサービス（Domain層）
 * 
 * Pure Javaで実装 - フレームワーク依存なし
 * ユーザー関連の複数エンティティを跨るドメインロジックを実装
 */
public class UserDomainService {
    
    /**
     * 管理者権限を持つユーザー一覧を取得
     * 
     * @param users ユーザー一覧
     * @return 管理者ユーザー一覧
     */
    public List<User> filterAdminUsers(List<User> users) {
        if (users == null) {
            throw new IllegalArgumentException("Users must not be null");
        }
        return users.stream()
            .filter(User::isAdmin)
            .filter(user -> !user.isDeleted())
            .collect(Collectors.toList());
    }
    
    /**
     * マネージャー権限を持つユーザー一覧を取得
     * 
     * @param users ユーザー一覧
     * @return マネージャーユーザー一覧
     */
    public List<User> filterManagerUsers(List<User> users) {
        if (users == null) {
            throw new IllegalArgumentException("Users must not be null");
        }
        return users.stream()
            .filter(User::isManager)
            .filter(user -> !user.isDeleted())
            .collect(Collectors.toList());
    }
    
    /**
     * ユーザーが管理者またはマネージャー権限を持つかチェック
     * 
     * @param user ユーザー
     * @return 管理者またはマネージャーの場合true
     */
    public boolean hasManagementPrivilege(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        return user.isAdmin() || user.isManager();
    }
}
```

---

### 3. Application層UseCaseでDomain Serviceを使用

**目的**: Application層のUseCaseからDomain Serviceを呼び出します。

**実装例**:
```java
// api/src/main/java/com/api/todos/application/usecase/todo/GetTodoStatisticsUseCase.java
package com.api.todos.application.usecase.todo;

import com.api.todos.application.dto.TodoStatisticsResult;
import com.api.todos.domain.model.Todo;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.domain.service.TodoDomainService;
import java.util.UUID;
import java.util.List;

/**
 * TODO統計取得ユースケース（Application層）
 * 
 * Pure Java実装 - Domain Serviceを使用
 */
public class GetTodoStatisticsUseCase {
    private final TodoRepository todoRepository;
    private final TodoDomainService todoDomainService;
    
    public GetTodoStatisticsUseCase(
        TodoRepository todoRepository,
        TodoDomainService todoDomainService
    ) {
        this.todoRepository = todoRepository;
        this.todoDomainService = todoDomainService;
    }
    
    /**
     * TODO統計を取得する
     * 
     * @param userId ユーザーID
     * @return TODO統計結果
     */
    public TodoStatisticsResult execute(UUID userId) {
        // 1. Repositoryでデータ取得（Application層の責務）
        List<Todo> todos = todoRepository.findByUserId(userId);
        
        // 2. Domain Serviceでビジネスロジック実行
        long totalCount = todoDomainService.filterActiveTodos(todos).size();
        long completedCount = todoDomainService.countCompletedTodos(todos);
        long incompleteCount = todoDomainService.countIncompleteTodos(todos);
        double completionRate = todoDomainService.calculateCompletionRate(todos);
        
        // 3. Result変換して返却
        return new TodoStatisticsResult(
            totalCount,
            completedCount,
            incompleteCount,
            completionRate
        );
    }
}
```

```java
// api/src/main/java/com/api/todos/application/usecase/todo/UpdateTodoUseCase.java
package com.api.todos.application.usecase.todo;

import com.api.todos.application.command.todo.UpdateTodoCommand;
import com.api.todos.application.dto.TodoResult;
import com.api.todos.domain.model.Todo;
import com.api.todos.domain.model.User;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.domain.repository.UserRepository;
import com.api.todos.domain.service.TodoDomainService;

/**
 * TODO更新ユースケース（Application層）
 * 
 * Pure Java実装 - Domain Serviceで所有権チェック
 */
public class UpdateTodoUseCase {
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    private final TodoDomainService todoDomainService;
    
    public UpdateTodoUseCase(
        TodoRepository todoRepository,
        UserRepository userRepository,
        TodoDomainService todoDomainService
    ) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
        this.todoDomainService = todoDomainService;
    }
    
    public TodoResult execute(UpdateTodoCommand command) {
        // 1. データ取得
        Todo todo = todoRepository.findById(command.getTodoId())
            .orElseThrow(() -> new TodoNotFoundException());
        
        User user = userRepository.findById(command.getUserId())
            .orElseThrow(() -> new UserNotFoundException());
        
        // 2. Domain Serviceで所有権チェック（ビジネスルール）
        if (!todoDomainService.isOwner(todo, user)) {
            throw new UnauthorizedException("Not the owner of this TODO");
        }
        
        // 3. Entityのビジネスロジック実行
        todo.updateTitle(command.getTitle());
        todo.updateDescriptions(command.getDescriptions());
        
        // 4. 保存して返却
        Todo updated = todoRepository.save(todo);
        return TodoResult.from(updated);
    }
}
```

---

### 4. Infrastructure層でDomain ServiceをBean登録

**目的**: Domain ServiceをSpring DIコンテナに登録します。

**実装例**:
```java
// api/src/main/java/com/api/todos/infrastructure/config/DomainServiceConfig.java
package com.api.todos.infrastructure.config;

import com.api.todos.domain.service.TodoDomainService;
import com.api.todos.domain.service.UserDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Domain ServiceのDI設定クラス（Infrastructure層）
 * 
 * Pure JavaのDomain ServiceをSpring DIコンテナに登録
 */
@Configuration
public class DomainServiceConfig {
    
    /**
     * TodoDomainService Bean登録
     */
    @Bean
    public TodoDomainService todoDomainService() {
        return new TodoDomainService();
    }
    
    /**
     * UserDomainService Bean登録
     */
    @Bean
    public UserDomainService userDomainService() {
        return new UserDomainService();
    }
}
```

---

## 🔄 データフロー - Domain Serviceの役割

```
┌────────────────────────────────────────────────────────────────┐
│ Presentation層 (Controller)                                    │
│  - HTTPリクエスト受信                                           │
└────────────────────────────────────────────────────────────────┘
                            ↓
┌────────────────────────────────────────────────────────────────┐
│ Infrastructure層 (Service - トランザクション管理)               │
│  - @Transactional適用                                          │
└────────────────────────────────────────────────────────────────┘
                            ↓
┌────────────────────────────────────────────────────────────────┐
│ Application層 (UseCase)                                        │
│  - ビジネスフローのオーケストレーション                         │
│  - Repositoryでデータ取得                                      │
│  - Domain Serviceを使用（複数Entityを跨るロジック）            │
│  - Entityのビジネスロジック実行                                │
└────────────────────────────────────────────────────────────────┘
                            ↓
┌────────────────────────────────────────────────────────────────┐
│ Domain層                                                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Domain Service ← ★ このパッケージ                        │  │
│  │  - filterIncompleteTodos(todos)                          │  │
│  │  - isOwner(todo, user)                                   │  │
│  │  - calculateCompletionRate(todos)                        │  │
│  └──────────────────────────────────────────────────────────┘  │
│                            ↓ 使用                               │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Entity (Todo, User)                                      │  │
│  │  - markAsCompleted(), updateTitle()...                   │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
```

---

## 🧪 テスト戦略

### Domain Service テスト（Pure Java ユニットテスト）

**目的**: Domain Serviceのビジネスロジックが正しく動作するかテストします。

**実装例**:
```java
package com.api.todos.domain.service;

import com.api.todos.domain.model.Todo;
import com.api.todos.domain.model.User;
import com.api.todos.domain.model.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * TodoDomainService テスト
 * Pure Javaのユニットテスト - モック不要
 */
class TodoDomainServiceTest {
    
    private TodoDomainService todoDomainService;
    private UUID userId;
    private User user;
    
    @BeforeEach
    void setUp() {
        todoDomainService = new TodoDomainService();
        userId = UUID.randomUUID();
        user = User.create("testuser", "test@example.com", "Test", "User", UserRole.USER);
    }
    
    // ========================================
    // 所有権チェック
    // ========================================
    
    @Test
    void ユーザーがTODOの所有者の場合trueを返すこと() {
        Todo todo = Todo.create("Test TODO", "Description", userId);
        
        boolean result = todoDomainService.isOwner(todo, user);
        
        assertThat(result).isTrue();
    }
    
    @Test
    void ユーザーがTODOの所有者でない場合falseを返すこと() {
        Todo todo = Todo.create("Test TODO", "Description", UUID.randomUUID());
        
        boolean result = todoDomainService.isOwner(todo, user);
        
        assertThat(result).isFalse();
    }
    
    @Test
    void TODOがnullの場合例外をスローすること() {
        assertThatThrownBy(() -> todoDomainService.isOwner(null, user))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Todo must not be null");
    }
    
    @Test
    void ユーザーがnullの場合例外をスローすること() {
        Todo todo = Todo.create("Test TODO", "Description", userId);
        
        assertThatThrownBy(() -> todoDomainService.isOwner(todo, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User must not be null");
    }
    
    // ========================================
    // フィルタリング
    // ========================================
    
    @Test
    void 未完了TODO一覧を取得できること() {
        Todo todo1 = Todo.create("TODO 1", "Description", userId);
        Todo todo2 = Todo.create("TODO 2", "Description", userId);
        todo2.markAsCompleted();
        Todo todo3 = Todo.create("TODO 3", "Description", userId);
        
        List<Todo> todos = List.of(todo1, todo2, todo3);
        List<Todo> result = todoDomainService.filterIncompleteTodos(todos);
        
        assertThat(result).hasSize(2);
        assertThat(result).contains(todo1, todo3);
    }
    
    @Test
    void 完了TODO一覧を取得できること() {
        Todo todo1 = Todo.create("TODO 1", "Description", userId);
        Todo todo2 = Todo.create("TODO 2", "Description", userId);
        todo2.markAsCompleted();
        Todo todo3 = Todo.create("TODO 3", "Description", userId);
        todo3.markAsCompleted();
        
        List<Todo> todos = List.of(todo1, todo2, todo3);
        List<Todo> result = todoDomainService.filterCompletedTodos(todos);
        
        assertThat(result).hasSize(2);
        assertThat(result).contains(todo2, todo3);
    }
    
    @Test
    void 削除済みTODOはフィルタリング結果から除外されること() {
        Todo todo1 = Todo.create("TODO 1", "Description", userId);
        Todo todo2 = Todo.create("TODO 2", "Description", userId);
        todo2.delete();
        
        List<Todo> todos = List.of(todo1, todo2);
        List<Todo> result = todoDomainService.filterIncompleteTodos(todos);
        
        assertThat(result).hasSize(1);
        assertThat(result).contains(todo1);
    }
    
    // ========================================
    // 集計・統計
    // ========================================
    
    @Test
    void 完了TODO数をカウントできること() {
        Todo todo1 = Todo.create("TODO 1", "Description", userId);
        Todo todo2 = Todo.create("TODO 2", "Description", userId);
        todo2.markAsCompleted();
        Todo todo3 = Todo.create("TODO 3", "Description", userId);
        todo3.markAsCompleted();
        
        List<Todo> todos = List.of(todo1, todo2, todo3);
        long count = todoDomainService.countCompletedTodos(todos);
        
        assertThat(count).isEqualTo(2);
    }
    
    @Test
    void 未完了TODO数をカウントできること() {
        Todo todo1 = Todo.create("TODO 1", "Description", userId);
        Todo todo2 = Todo.create("TODO 2", "Description", userId);
        todo2.markAsCompleted();
        
        List<Todo> todos = List.of(todo1, todo2);
        long count = todoDomainService.countIncompleteTodos(todos);
        
        assertThat(count).isEqualTo(1);
    }
    
    @Test
    void TODO完了率を計算できること() {
        Todo todo1 = Todo.create("TODO 1", "Description", userId);
        Todo todo2 = Todo.create("TODO 2", "Description", userId);
        todo2.markAsCompleted();
        Todo todo3 = Todo.create("TODO 3", "Description", userId);
        todo3.markAsCompleted();
        
        List<Todo> todos = List.of(todo1, todo2, todo3);
        double rate = todoDomainService.calculateCompletionRate(todos);
        
        assertThat(rate).isEqualTo(2.0 / 3.0);
    }
    
    @Test
    void TODOが空の場合完了率は0になること() {
        List<Todo> todos = List.of();
        
        double rate = todoDomainService.calculateCompletionRate(todos);
        
        assertThat(rate).isEqualTo(0.0);
    }
    
    // ========================================
    // ビジネスポリシー
    // ========================================
    
    @Test
    void ユーザーがすべてのTODOの所有者の場合trueを返すこと() {
        Todo todo1 = Todo.create("TODO 1", "Description", userId);
        Todo todo2 = Todo.create("TODO 2", "Description", userId);
        
        List<Todo> todos = List.of(todo1, todo2);
        boolean result = todoDomainService.isOwnerOfAllTodos(todos, user);
        
        assertThat(result).isTrue();
    }
    
    @Test
    void ユーザーが一部のTODOの所有者でない場合falseを返すこと() {
        Todo todo1 = Todo.create("TODO 1", "Description", userId);
        Todo todo2 = Todo.create("TODO 2", "Description", UUID.randomUUID());
        
        List<Todo> todos = List.of(todo1, todo2);
        boolean result = todoDomainService.isOwnerOfAllTodos(todos, user);
        
        assertThat(result).isFalse();
    }
}
```

---

## ✅ 実装チェックリスト

### Domain Service実装時

- [ ] **Pure Java**で実装（Spring/JPAアノテーション使用禁止）
- [ ] **複数エンティティを跨るロジック**を実装
- [ ] **単一Entityのビジネスルール**はEntityメソッドとして実装（Domain Serviceに含めない）
- [ ] **Repositoryを直接使用しない**（引数として渡されたデータに対して処理）
- [ ] **バリデーション**をメソッド内で実施
- [ ] **Pure Javaユニットテスト**を実装

### 使い分けチェック

- [ ] 単一Entityに属するロジックはEntity内に実装（Domain Serviceに含めない）
- [ ] データアクセスが必要なロジックはApplication層のUseCaseに実装
- [ ] 複数Entityを組み合わせたビジネスポリシーのみDomain Serviceに実装

### 禁止事項チェック

- [ ] Domain Serviceで**Springアノテーション**を使用していない
- [ ] Domain Serviceで**Repositoryを直接使用**していない
- [ ] 単一Entityのビジネスルールを**Domain Serviceに実装**していない
- [ ] Domain Serviceが**Infrastructure層・Presentation層**に依存していない

### 対応する他のコンポーネント

- [ ] **Domain層**: Entity（ビジネスロジック実装）が存在
- [ ] **Application層**: UseCase（Domain Service使用）が存在
- [ ] **Infrastructure層**: DomainServiceConfig（Bean登録）が存在
- [ ] **テスト**: Pure Javaユニットテストが実装済み

---

## 📚 参考資料

### プロジェクト内ドキュメント
- **[AGENTS.md](../../../../../AGENTS.md)** - AIエージェント向けコンテキストドキュメント
- **[Domain層 README](../README.md)** - Domain層全体の概要
- **[Domain層 model README](../model/README.md)** - Entity実装パターン
- **[Application層 README](../../application/README.md)** - UseCase実装パターン

### 外部参考資料
- [Domain-Driven Design (Eric Evans)](https://www.domainlanguage.com/ddd/)
- [Clean Architecture (Robert C. Martin)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)

---

## 🎯 まとめ

Domain Serviceは、**複数エンティティを跨るビジネスロジックを Pure Java で実装する**ためのパッケージです。

### 重要ポイント

1. **Pure Java（最重要）**
   - Spring、JPA、Lombokアノテーション使用禁止
   - フレームワークに一切依存しない
   - Pure Javaユニットテストで高速にテスト

2. **責務の明確化**
   - 単一Entityのビジネスルール → **Entity内のメソッド**
   - 複数Entityを跨るロジック → **Domain Service**
   - データアクセス・トランザクション → **Application層UseCase**

3. **Repositoryを使用しない**
   - Domain Serviceはデータアクセスしない
   - 引数として渡されたデータに対してビジネスロジックを実行
   - データ取得はApplication層の責務

4. **Application層から使用**
   - UseCaseからDomain Serviceを呼び出す
   - Domain Serviceの結果を使ってビジネスフローをオーケストレーション

### 禁止事項

- ❌ Domain ServiceでSpringアノテーション使用
- ❌ Domain ServiceでRepositoryを直接使用
- ❌ 単一EntityのビジネスルールをDomain Serviceに実装

このREADMEを参考に、**Pure Javaで実装された高品質なDomain Service**を構築してください。Domain Serviceは、複数エンティティを跨るビジネスロジックを集約し、コードの再利用性と保守性を向上させます。

---

**作成日**: 2026年1月5日  
**対象バージョン**: Spring Boot TODO API v0.0.1-SNAPSHOT  
**対象パッケージ**: `com.api.todos.domain.service`
