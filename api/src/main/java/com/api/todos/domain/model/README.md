# Domain層 - model パッケージ（エンティティ・値オブジェクト）

## 📋 パッケージ概要

**場所**: `api/src/main/java/com/api/todos/domain/model/`

**目的**: **ビジネスルールとドメインロジックの中核**を Pure Java で実装するパッケージです。Entityと Value Object は、アプリケーションの最も重要なビジネスロジックを保持し、フレームワークに一切依存しない Pure Java で実装されます。

**主要コンポーネント**:
- **Todo Entity**: TODOのビジネスロジックとビジネスルール
- **User Entity**: ユーザーのビジネスロジックと認証ロジック
- **UserRole Value Object**: ユーザーロールの定義

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
│  Domain層（最内層・最も重要）                   │
│  ├── model/ ← ★ このパッケージ                 │
│  │   - Entity（Pure Java）                      │
│  │   - Value Object（Pure Java）                │
│  │   - ビジネスルール・ドメインロジック         │
│  ├── repository/ (Repository Interface)         │
│  └── service/ (Domain Service)                  │
└─────────────────────────────────────────────────┘
```

### 依存関係の方向

```
すべての層がDomain層のEntityに依存
    ↓
Domain層のEntity（Pure Java）
    - フレームワークに依存しない
    - ビジネスルールを保持
    - どこからでも使用可能
```

**重要**: Domain層のEntityは **クリーンアーキテクチャの最内層（最も重要な層）** であり、いかなる外部ライブラリ・フレームワークにも依存してはいけません。

---

## 🎯 責務（Responsibilities）

### ✅ このパッケージが行うべきこと

1. **ビジネスルールの実装**
   - TODOの完了状態変更ロジック
   - パスワード初期化ロジック
   - データの整合性チェック

2. **ドメインロジックの集約**
   - 単一のエンティティに属するビジネスロジック
   - エンティティの状態変更メソッド
   - ドメイン固有の計算ロジック

3. **不変条件（Invariant）の保証**
   - コンストラクタでバリデーション
   - メソッド内でビジネスルールチェック
   - 不正な状態遷移の防止

4. **Pure Javaでの実装**
   - Spring、JPA等のフレームワークアノテーション使用禁止
   - フレームワークに依存しないビジネスロジック

### ❌ このパッケージが行ってはいけないこと

1. **データベースアクセス**
   - → Repository Interfaceを使用（Application層で実行）
   - Entityはデータアクセスしない

2. **外部サービス連携**
   - → Infrastructure層の責務
   - Entityは外部システムに依存しない

3. **トランザクション管理**
   - → Infrastructure層の責務
   - Entityはトランザクションに関知しない

4. **外部フレームワークへの依存**
   - → Pure Javaで実装（Spring/JPAアノテーション禁止）

---

## 🚨 絶対原則：Pure Java（フレームワーク依存禁止）

Domain層のEntityとValue Objectは **Pure Java** で実装しなければなりません。以下のアノテーション・ライブラリは **絶対に使用禁止** です：

### ❌ 禁止されるアノテーション・ライブラリ

```java
// ❌ Spring Framework
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

// ❌ JPA/Hibernate
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;

// ❌ Lombok（Domain層では非推奨）
import lombok.Getter;
import lombok.Setter;
import lombok.Data;
import lombok.RequiredArgsConstructor;

// ❌ Jackson
import com.fasterxml.jackson.annotation.JsonProperty;

// ❌ Bean Validation
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
```

### ✅ 許可されるもの

```java
// ✅ Java標準ライブラリ
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

// ✅ Pure Javaのコンストラクタ・メソッド
public class Todo {
    private final UUID id;
    private String title;
    
    // Pure Javaのコンストラクタ
    public Todo(UUID id, String title) {
        this.id = id;
        this.title = title;
    }
    
    // Pure Javaのgetter
    public UUID getId() {
        return id;
    }
}
```

---

## 🚨 禁止パターン（Anti-Patterns）

### ❌ 1. Domain EntityにJPA/Springアノテーションを使用

**問題**: Domain Entityがフレームワークに依存してしまい、Pure Javaの原則に違反します。

```java
// ❌ 絶対禁止: Domain EntityでJPAアノテーションを使用
package com.api.todos.domain.model;

import jakarta.persistence.*;  // ❌ JPA依存
import java.util.UUID;
import java.time.LocalDateTime;

@Entity  // ❌ Domain層でJPAアノテーション使用禁止
@Table(name = "todos")  // ❌ Infrastructure層の関心事
public class Todo {
    
    @Id  // ❌ JPA依存
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, length = 32)  // ❌ データベース制約をDomain層に持ち込み
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
    
    // getter, setter...
}
```

**なぜダメか**:
- Domain層がJPA/Hibernateに依存してしまう
- Pure Javaの原則に違反
- フレームワーク変更時にDomain層も変更が必要
- テストが困難（JPA実装が必要）
- ビジネスロジックとデータベース制約が混在

**正しい実装**:
```java
// ✅ 正しい実装: Pure JavaのDomain Entity
package com.api.todos.domain.model;

import java.util.UUID;
import java.time.LocalDateTime;

/**
 * TODOエンティティ（Domain層）
 * 
 * Pure Javaで実装 - フレームワーク依存なし
 * ビジネスルールとドメインロジックを保持
 */
public class Todo {
    // final: 不変フィールド（変更不可）
    private final UUID id;
    private String title;
    private String descriptions;
    private boolean completed;
    private final UUID userId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;
    
    // ========================================
    // コンストラクタ（バリデーション込み）
    // ========================================
    
    /**
     * プライベートコンストラクタ
     * Factoryメソッド経由でのみ生成可能
     */
    private Todo(
        UUID id,
        String title,
        String descriptions,
        boolean completed,
        UUID userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
    ) {
        // バリデーション
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("Title must not be empty");
        }
        if (title.length() > 32) {
            throw new IllegalArgumentException("Title must not exceed 32 characters");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        
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
     * Factoryメソッド: 新規TODO作成
     */
    public static Todo create(String title, String descriptions, UUID userId) {
        return new Todo(
            UUID.randomUUID(),
            title,
            descriptions,
            false,  // 初期状態: 未完了
            userId,
            LocalDateTime.now(),
            LocalDateTime.now(),
            false  // 初期状態: 未削除
        );
    }
    
    // ========================================
    // ビジネスロジック
    // ========================================
    
    /**
     * TODOを完了状態にする
     */
    public void markAsCompleted() {
        if (this.completed) {
            throw new IllegalStateException("TODO is already completed");
        }
        if (this.deleted) {
            throw new IllegalStateException("Cannot complete deleted TODO");
        }
        this.completed = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * TODOを未完了状態に戻す
     */
    public void markAsIncomplete() {
        if (!this.completed) {
            throw new IllegalStateException("TODO is already incomplete");
        }
        if (this.deleted) {
            throw new IllegalStateException("Cannot mark deleted TODO as incomplete");
        }
        this.completed = false;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * TODOを削除（論理削除）
     */
    public void delete() {
        if (this.deleted) {
            throw new IllegalStateException("TODO is already deleted");
        }
        this.deleted = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * タイトルを更新
     */
    public void updateTitle(String newTitle) {
        if (newTitle == null || newTitle.isEmpty()) {
            throw new IllegalArgumentException("Title must not be empty");
        }
        if (newTitle.length() > 32) {
            throw new IllegalArgumentException("Title must not exceed 32 characters");
        }
        if (this.deleted) {
            throw new IllegalStateException("Cannot update deleted TODO");
        }
        this.title = newTitle;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 説明を更新
     */
    public void updateDescriptions(String newDescriptions) {
        if (newDescriptions != null && newDescriptions.length() > 128) {
            throw new IllegalArgumentException("Descriptions must not exceed 128 characters");
        }
        if (this.deleted) {
            throw new IllegalStateException("Cannot update deleted TODO");
        }
        this.descriptions = newDescriptions;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * ユーザーがこのTODOの所有者かチェック
     */
    public boolean isOwnedBy(UUID userId) {
        return this.userId.equals(userId);
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
    // equals, hashCode（IDのみで判定）
    // ========================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Todo todo = (Todo) o;
        return Objects.equals(id, todo.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Todo{" +
            "id=" + id +
            ", title='" + title + '\'' +
            ", completed=" + completed +
            ", deleted=" + deleted +
            '}';
    }
}
```

```java
// ✅ Infrastructure層でJPA Entityを別途作成
package com.api.todos.infrastructure.persistence.entity;

import jakarta.persistence.*;  // ✅ Infrastructure層ではJPA使用可能

@Entity
@Table(name = "todos")
public class TodoJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, length = 32)
    private String title;
    
    // ... その他のフィールド
    
    // getter, setter
}
```

---

### ❌ 2. Domain EntityがPresentation層のDTOを返却

**問題**: Domain EntityがPresentation層のDTOに依存し、依存方向が逆になります。

```java
// ❌ 絶対禁止: Domain EntityがPresentation層のDTOを返却
package com.api.todos.domain.model;

import com.api.todos.presentation.dto.TodoResponse;  // ❌ Presentation層に依存
import java.util.UUID;
import java.time.LocalDateTime;

public class Todo {
    private final UUID id;
    private String title;
    private String descriptions;
    private boolean completed;
    private final UUID userId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;
    
    // ❌ Domain EntityがPresentation層のDTOに依存
    public TodoResponse toResponse() {
        TodoResponse response = new TodoResponse();
        response.setId(this.id);
        response.setTitle(this.title);
        response.setDescriptions(this.descriptions);
        response.setCompleted(this.completed);
        response.setUserId(this.userId);
        response.setCreatedAt(this.createdAt);
        response.setUpdatedAt(this.updatedAt);
        response.setDeleted(this.deleted);
        return response;
    }
}
```

**なぜダメか**:
- Domain層がPresentation層に依存（依存方向が逆）
- クリーンアーキテクチャの原則に違反
- Presentation層の変更がDomain層に影響
- Domain Entityが外側の層を知ってしまう

**正しい実装**:
```java
// ✅ 正しい実装: Domain EntityはPure Java（Domain層）
package com.api.todos.domain.model;

import java.util.UUID;
import java.time.LocalDateTime;

/**
 * TODOエンティティ
 * Pure Java - Presentation層に依存しない
 */
public class Todo {
    private final UUID id;
    private String title;
    // ... その他のフィールド
    
    // Pure Javaのgetter
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    // ...
}
```

```java
// ✅ Application層でResultオブジェクトに変換
package com.api.todos.application.dto;

import com.api.todos.domain.model.Todo;

public class TodoResult {
    private final UUID id;
    private final String title;
    // ... その他のフィールド
    
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
}
```

```java
// ✅ Presentation層でResponseDTOに変換
package com.api.todos.presentation.dto;

import com.api.todos.application.dto.TodoResult;

public class TodoResponse {
    private UUID id;
    private String title;
    // ... その他のフィールド
    
    /**
     * Application層のResultからPresentation層のDTOへの変換
     */
    public static TodoResponse from(TodoResult result) {
        TodoResponse response = new TodoResponse();
        response.setId(result.getId());
        response.setTitle(result.getTitle());
        // ...
        return response;
    }
}
```

---

### ❌ 3. Domain Entityがビジネスロジックを持たない（貧血モデル）

**問題**: Entityがgetterとsetterしか持たず、ビジネスロジックが外部に散在します。

```java
// ❌ 絶対禁止: 貧血モデル（Anemic Domain Model）
package com.api.todos.domain.model;

import java.util.UUID;
import java.time.LocalDateTime;

/**
 * ❌ ビジネスロジックを持たない貧血モデル
 */
public class Todo {
    private UUID id;
    private String title;
    private String descriptions;
    private boolean completed;
    private UUID userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;
    
    // ❌ ビジネスロジックなし（getter/setterのみ）
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }  // ❌ バリデーションなし
    
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }  // ❌ ビジネスルールなし
    
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }  // ❌ ビジネスルールなし
    
    // ... その他のgetter/setter
}
```

```java
// ❌ ビジネスロジックがUseCase層に散在
package com.api.todos.application.usecase.todo;

public class CompleteTodoUseCase {
    public TodoResult execute(UUID todoId) {
        Todo todo = todoRepository.findById(todoId).orElseThrow();
        
        // ❌ ビジネスロジックがUseCase層に漏れ出している
        if (todo.isCompleted()) {
            throw new IllegalStateException("TODO is already completed");
        }
        if (todo.isDeleted()) {
            throw new IllegalStateException("Cannot complete deleted TODO");
        }
        todo.setCompleted(true);  // ❌ ビジネスルール外でsetterを直接操作
        todo.setUpdatedAt(LocalDateTime.now());
        
        return TodoResult.from(todoRepository.save(todo));
    }
}
```

**なぜダメか**:
- ビジネスロジックがDomain層に集約されていない
- 同じビジネスルールが複数箇所に重複
- バグが発生しやすい（setter直接操作）
- テストが困難（ビジネスロジックが分散）
- ドメインモデルの価値がない

**正しい実装**:
```java
// ✅ 正しい実装: リッチドメインモデル（Rich Domain Model）
package com.api.todos.domain.model;

import java.util.UUID;
import java.time.LocalDateTime;

/**
 * TODOエンティティ
 * ビジネスロジックを保持するリッチドメインモデル
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
    
    // ========================================
    // ビジネスロジック（最重要）
    // ========================================
    
    /**
     * TODOを完了状態にする
     * ビジネスルール: 既に完了済み・削除済みの場合はエラー
     */
    public void markAsCompleted() {
        if (this.completed) {
            throw new IllegalStateException("TODO is already completed");
        }
        if (this.deleted) {
            throw new IllegalStateException("Cannot complete deleted TODO");
        }
        this.completed = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * TODOを削除（論理削除）
     * ビジネスルール: 既に削除済みの場合はエラー
     */
    public void delete() {
        if (this.deleted) {
            throw new IllegalStateException("TODO is already deleted");
        }
        this.deleted = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * タイトルを更新
     * ビジネスルール: 空・32文字超・削除済みの場合はエラー
     */
    public void updateTitle(String newTitle) {
        if (newTitle == null || newTitle.isEmpty()) {
            throw new IllegalArgumentException("Title must not be empty");
        }
        if (newTitle.length() > 32) {
            throw new IllegalArgumentException("Title must not exceed 32 characters");
        }
        if (this.deleted) {
            throw new IllegalStateException("Cannot update deleted TODO");
        }
        this.title = newTitle;
        this.updatedAt = LocalDateTime.now();
    }
    
    // ========================================
    // Getter（setterは提供しない）
    // ========================================
    
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public boolean isCompleted() { return completed; }
    public boolean isDeleted() { return deleted; }
    // ...
}
```

```java
// ✅ UseCase層はEntityのビジネスロジックを呼び出すだけ
package com.api.todos.application.usecase.todo;

public class CompleteTodoUseCase {
    public TodoResult execute(UUID todoId) {
        Todo todo = todoRepository.findById(todoId).orElseThrow();
        
        // ✅ Entityのビジネスロジックを呼び出す
        todo.markAsCompleted();
        
        return TodoResult.from(todoRepository.save(todo));
    }
}
```

---

## ✅ 正しい実装パターン

### 1. Todo Entity - リッチドメインモデル

**目的**: TODOのビジネスルールとドメインロジックを保持します。

**実装例**:
```java
// api/src/main/java/com/api/todos/domain/model/Todo.java
package com.api.todos.domain.model;

import java.util.UUID;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * TODOエンティティ（Domain層）
 * 
 * Pure Javaで実装 - フレームワーク依存なし
 * ビジネスルールとドメインロジックを保持
 */
public class Todo {
    
    // ========================================
    // フィールド（不変フィールドはfinal）
    // ========================================
    
    private final UUID id;
    private String title;
    private String descriptions;
    private boolean completed;
    private final UUID userId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;
    
    // ========================================
    // コンストラクタ（バリデーション込み）
    // ========================================
    
    /**
     * プライベートコンストラクタ
     * Factoryメソッド経由でのみ生成可能
     */
    private Todo(
        UUID id,
        String title,
        String descriptions,
        boolean completed,
        UUID userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
    ) {
        // バリデーション
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("Title must not be empty");
        }
        if (title.length() > 32) {
            throw new IllegalArgumentException("Title must not exceed 32 characters");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Created at must not be null");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("Updated at must not be null");
        }
        
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
     * Factoryメソッド: 新規TODO作成
     * 
     * @param title タイトル（必須、32文字以内）
     * @param descriptions 説明（任意、128文字以内）
     * @param userId ユーザーID（必須）
     * @return 新規TODO
     */
    public static Todo create(String title, String descriptions, UUID userId) {
        if (descriptions != null && descriptions.length() > 128) {
            throw new IllegalArgumentException("Descriptions must not exceed 128 characters");
        }
        
        return new Todo(
            UUID.randomUUID(),
            title,
            descriptions,
            false,  // 初期状態: 未完了
            userId,
            LocalDateTime.now(),
            LocalDateTime.now(),
            false  // 初期状態: 未削除
        );
    }
    
    /**
     * Factoryメソッド: 既存TODOの復元（Repository実装から呼び出される）
     */
    public static Todo reconstruct(
        UUID id,
        String title,
        String descriptions,
        boolean completed,
        UUID userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
    ) {
        return new Todo(id, title, descriptions, completed, userId, createdAt, updatedAt, deleted);
    }
    
    // ========================================
    // ビジネスロジック
    // ========================================
    
    /**
     * TODOを完了状態にする
     * 
     * ビジネスルール:
     * - 既に完了済みの場合はエラー
     * - 削除済みの場合はエラー
     * 
     * @throws IllegalStateException ビジネスルール違反
     */
    public void markAsCompleted() {
        if (this.completed) {
            throw new IllegalStateException("TODO is already completed");
        }
        if (this.deleted) {
            throw new IllegalStateException("Cannot complete deleted TODO");
        }
        this.completed = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * TODOを未完了状態に戻す
     * 
     * ビジネスルール:
     * - 既に未完了の場合はエラー
     * - 削除済みの場合はエラー
     * 
     * @throws IllegalStateException ビジネスルール違反
     */
    public void markAsIncomplete() {
        if (!this.completed) {
            throw new IllegalStateException("TODO is already incomplete");
        }
        if (this.deleted) {
            throw new IllegalStateException("Cannot mark deleted TODO as incomplete");
        }
        this.completed = false;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * TODOを削除（論理削除）
     * 
     * ビジネスルール:
     * - 既に削除済みの場合はエラー
     * 
     * @throws IllegalStateException ビジネスルール違反
     */
    public void delete() {
        if (this.deleted) {
            throw new IllegalStateException("TODO is already deleted");
        }
        this.deleted = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * タイトルを更新
     * 
     * ビジネスルール:
     * - 空のタイトルは不可
     * - 32文字を超えるタイトルは不可
     * - 削除済みの場合は更新不可
     * 
     * @param newTitle 新しいタイトル
     * @throws IllegalArgumentException タイトルが不正
     * @throws IllegalStateException 削除済みTODO
     */
    public void updateTitle(String newTitle) {
        if (newTitle == null || newTitle.isEmpty()) {
            throw new IllegalArgumentException("Title must not be empty");
        }
        if (newTitle.length() > 32) {
            throw new IllegalArgumentException("Title must not exceed 32 characters");
        }
        if (this.deleted) {
            throw new IllegalStateException("Cannot update deleted TODO");
        }
        this.title = newTitle;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 説明を更新
     * 
     * ビジネスルール:
     * - 128文字を超える説明は不可
     * - 削除済みの場合は更新不可
     * 
     * @param newDescriptions 新しい説明
     * @throws IllegalArgumentException 説明が不正
     * @throws IllegalStateException 削除済みTODO
     */
    public void updateDescriptions(String newDescriptions) {
        if (newDescriptions != null && newDescriptions.length() > 128) {
            throw new IllegalArgumentException("Descriptions must not exceed 128 characters");
        }
        if (this.deleted) {
            throw new IllegalStateException("Cannot update deleted TODO");
        }
        this.descriptions = newDescriptions;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * ユーザーがこのTODOの所有者かチェック
     * アクセス制御に使用
     * 
     * @param userId ユーザーID
     * @return 所有者の場合true
     */
    public boolean isOwnedBy(UUID userId) {
        if (userId == null) {
            return false;
        }
        return this.userId.equals(userId);
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
    // equals, hashCode（IDのみで判定）
    // ========================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Todo todo = (Todo) o;
        return Objects.equals(id, todo.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Todo{" +
            "id=" + id +
            ", title='" + title + '\'' +
            ", completed=" + completed +
            ", deleted=" + deleted +
            '}';
    }
}
```

---

### 2. User Entity - 認証・認可エンティティ

**目的**: ユーザーのビジネスルールと認証ロジックを保持します。

**実装例**:
```java
// api/src/main/java/com/api/todos/domain/model/User.java
package com.api.todos.domain.model;

import java.util.UUID;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * ユーザーエンティティ（Domain層）
 * 
 * Pure Javaで実装 - フレームワーク依存なし
 * 認証・認可のビジネスロジックを保持
 */
public class User {
    
    // ========================================
    // フィールド
    // ========================================
    
    private final UUID id;
    private String username;
    private String email;
    private String hashedPassword;
    private String firstName;
    private String lastName;
    private UserRole role;
    private boolean passwordInitialized;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;
    
    // ========================================
    // コンストラクタ
    // ========================================
    
    private User(
        UUID id,
        String username,
        String email,
        String hashedPassword,
        String firstName,
        String lastName,
        UserRole role,
        boolean passwordInitialized,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
    ) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username must not be empty");
        }
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email must not be empty");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role must not be null");
        }
        
        this.id = id;
        this.username = username;
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.passwordInitialized = passwordInitialized;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
    }
    
    /**
     * Factoryメソッド: 新規ユーザー作成
     */
    public static User create(
        String username,
        String email,
        String firstName,
        String lastName,
        UserRole role
    ) {
        return new User(
            UUID.randomUUID(),
            username,
            email,
            null,  // パスワードは未設定
            firstName,
            lastName,
            role,
            false,  // パスワード未初期化
            LocalDateTime.now(),
            LocalDateTime.now(),
            false
        );
    }
    
    /**
     * Factoryメソッド: 既存ユーザーの復元
     */
    public static User reconstruct(
        UUID id,
        String username,
        String email,
        String hashedPassword,
        String firstName,
        String lastName,
        UserRole role,
        boolean passwordInitialized,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
    ) {
        return new User(
            id, username, email, hashedPassword, firstName, lastName,
            role, passwordInitialized, createdAt, updatedAt, deleted
        );
    }
    
    // ========================================
    // ビジネスロジック - 認証
    // ========================================
    
    /**
     * パスワードを初期化
     * 
     * ビジネスルール:
     * - 既にパスワード初期化済みの場合はエラー
     * - 削除済みユーザーの場合はエラー
     */
    public void initializePassword(String hashedPassword) {
        if (this.passwordInitialized) {
            throw new IllegalStateException("Password is already initialized");
        }
        if (this.deleted) {
            throw new IllegalStateException("Cannot initialize password for deleted user");
        }
        if (hashedPassword == null || hashedPassword.isEmpty()) {
            throw new IllegalArgumentException("Hashed password must not be empty");
        }
        
        this.hashedPassword = hashedPassword;
        this.passwordInitialized = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * パスワードを変更
     * 
     * ビジネスルール:
     * - パスワード未初期化の場合はエラー
     * - 削除済みユーザーの場合はエラー
     */
    public void changePassword(String newHashedPassword) {
        if (!this.passwordInitialized) {
            throw new IllegalStateException("Password is not initialized yet");
        }
        if (this.deleted) {
            throw new IllegalStateException("Cannot change password for deleted user");
        }
        if (newHashedPassword == null || newHashedPassword.isEmpty()) {
            throw new IllegalArgumentException("Hashed password must not be empty");
        }
        
        this.hashedPassword = newHashedPassword;
        this.updatedAt = LocalDateTime.now();
    }
    
    // ========================================
    // ビジネスロジック - プロフィール
    // ========================================
    
    /**
     * プロフィール情報を更新
     */
    public void updateProfile(String email, String firstName, String lastName) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email must not be empty");
        }
        if (this.deleted) {
            throw new IllegalStateException("Cannot update profile for deleted user");
        }
        
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * ロールを変更
     * 
     * ビジネスルール:
     * - 削除済みユーザーの場合はエラー
     */
    public void changeRole(UserRole newRole) {
        if (newRole == null) {
            throw new IllegalArgumentException("Role must not be null");
        }
        if (this.deleted) {
            throw new IllegalStateException("Cannot change role for deleted user");
        }
        
        this.role = newRole;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * ユーザーを削除（論理削除）
     */
    public void delete() {
        if (this.deleted) {
            throw new IllegalStateException("User is already deleted");
        }
        this.deleted = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    // ========================================
    // ビジネスロジック - 認可
    // ========================================
    
    /**
     * パスワードが初期化済みかチェック
     */
    public boolean isPasswordInitialized() {
        return this.passwordInitialized;
    }
    
    /**
     * 管理者権限を持つかチェック
     */
    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }
    
    /**
     * マネージャー権限を持つかチェック
     */
    public boolean isManager() {
        return this.role == UserRole.MANAGER;
    }
    
    // ========================================
    // Getter
    // ========================================
    
    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getHashedPassword() { return hashedPassword; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public UserRole getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public boolean isDeleted() { return deleted; }
    
    // ========================================
    // equals, hashCode
    // ========================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "User{" +
            "id=" + id +
            ", username='" + username + '\'' +
            ", role=" + role +
            ", deleted=" + deleted +
            '}';
    }
}
```

---

### 3. UserRole Value Object - ユーザーロール

**目的**: ユーザーロールを不変なValue Objectとして定義します。

**実装例**:
```java
// api/src/main/java/com/api/todos/domain/model/UserRole.java
package com.api.todos.domain.model;

/**
 * ユーザーロール Value Object（Domain層）
 * 
 * Pure Javaで実装 - フレームワーク依存なし
 * 不変オブジェクト
 */
public enum UserRole {
    ADMIN(0, "管理者"),
    MANAGER(1, "マネージャー"),
    USER(2, "一般ユーザー");
    
    private final int code;
    private final String displayName;
    
    UserRole(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * コードからUserRoleを取得
     * 
     * @param code ロールコード
     * @return UserRole
     * @throws IllegalArgumentException 無効なコード
     */
    public static UserRole fromCode(int code) {
        for (UserRole role : values()) {
            if (role.code == code) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid role code: " + code);
    }
    
    /**
     * 管理者権限を持つかチェック
     */
    public boolean hasAdminPrivilege() {
        return this == ADMIN;
    }
    
    /**
     * マネージャー権限を持つかチェック
     */
    public boolean hasManagerPrivilege() {
        return this == ADMIN || this == MANAGER;
    }
}
```

---

## 🧪 テスト戦略

### Domain Entity テスト（Pure Java ユニットテスト）

**目的**: Domain Entityのビジネスロジックが正しく動作するかテストします。

**実装例**:
```java
package com.api.todos.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Todo エンティティテスト
 * Pure Javaのユニットテスト - モック不要、Springコンテキスト不要
 */
class TodoTest {
    
    private UUID userId;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }
    
    // ========================================
    // 作成テスト
    // ========================================
    
    @Test
    void 新規TODOを作成できること() {
        Todo todo = Todo.create("Test TODO", "Description", userId);
        
        assertThat(todo).isNotNull();
        assertThat(todo.getId()).isNotNull();
        assertThat(todo.getTitle()).isEqualTo("Test TODO");
        assertThat(todo.getDescriptions()).isEqualTo("Description");
        assertThat(todo.isCompleted()).isFalse();
        assertThat(todo.isDeleted()).isFalse();
        assertThat(todo.getUserId()).isEqualTo(userId);
        assertThat(todo.getCreatedAt()).isNotNull();
        assertThat(todo.getUpdatedAt()).isNotNull();
    }
    
    @Test
    void タイトルが空の場合例外をスローすること() {
        assertThatThrownBy(() -> Todo.create("", "Description", userId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Title must not be empty");
    }
    
    @Test
    void タイトルが32文字を超える場合例外をスローすること() {
        String longTitle = "a".repeat(33);
        
        assertThatThrownBy(() -> Todo.create(longTitle, "Description", userId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Title must not exceed 32 characters");
    }
    
    // ========================================
    // 完了状態テスト
    // ========================================
    
    @Test
    void TODOを完了状態にできること() {
        Todo todo = Todo.create("Test TODO", "Description", userId);
        
        todo.markAsCompleted();
        
        assertThat(todo.isCompleted()).isTrue();
    }
    
    @Test
    void 既に完了済みのTODOを完了状態にしようとすると例外をスローすること() {
        Todo todo = Todo.create("Test TODO", "Description", userId);
        todo.markAsCompleted();
        
        assertThatThrownBy(() -> todo.markAsCompleted())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("TODO is already completed");
    }
    
    @Test
    void TODOを未完了状態に戻せること() {
        Todo todo = Todo.create("Test TODO", "Description", userId);
        todo.markAsCompleted();
        
        todo.markAsIncomplete();
        
        assertThat(todo.isCompleted()).isFalse();
    }
    
    // ========================================
    // 削除テスト
    // ========================================
    
    @Test
    void TODOを論理削除できること() {
        Todo todo = Todo.create("Test TODO", "Description", userId);
        
        todo.delete();
        
        assertThat(todo.isDeleted()).isTrue();
    }
    
    @Test
    void 削除済みTODOを完了状態にしようとすると例外をスローすること() {
        Todo todo = Todo.create("Test TODO", "Description", userId);
        todo.delete();
        
        assertThatThrownBy(() -> todo.markAsCompleted())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot complete deleted TODO");
    }
    
    // ========================================
    // 更新テスト
    // ========================================
    
    @Test
    void タイトルを更新できること() {
        Todo todo = Todo.create("Test TODO", "Description", userId);
        
        todo.updateTitle("Updated TODO");
        
        assertThat(todo.getTitle()).isEqualTo("Updated TODO");
    }
    
    @Test
    void 空のタイトルに更新しようとすると例外をスローすること() {
        Todo todo = Todo.create("Test TODO", "Description", userId);
        
        assertThatThrownBy(() -> todo.updateTitle(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Title must not be empty");
    }
    
    @Test
    void 削除済みTODOのタイトルを更新しようとすると例外をスローすること() {
        Todo todo = Todo.create("Test TODO", "Description", userId);
        todo.delete();
        
        assertThatThrownBy(() -> todo.updateTitle("New Title"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot update deleted TODO");
    }
    
    // ========================================
    // 所有権テスト
    // ========================================
    
    @Test
    void ユーザーがTODOの所有者の場合trueを返すこと() {
        Todo todo = Todo.create("Test TODO", "Description", userId);
        
        boolean isOwner = todo.isOwnedBy(userId);
        
        assertThat(isOwner).isTrue();
    }
    
    @Test
    void ユーザーがTODOの所有者でない場合falseを返すこと() {
        Todo todo = Todo.create("Test TODO", "Description", userId);
        UUID anotherUserId = UUID.randomUUID();
        
        boolean isOwner = todo.isOwnedBy(anotherUserId);
        
        assertThat(isOwner).isFalse();
    }
}
```

```java
package com.api.todos.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/**
 * UserRole Value Object テスト
 */
class UserRoleTest {
    
    @Test
    void コードからUserRoleを取得できること() {
        assertThat(UserRole.fromCode(0)).isEqualTo(UserRole.ADMIN);
        assertThat(UserRole.fromCode(1)).isEqualTo(UserRole.MANAGER);
        assertThat(UserRole.fromCode(2)).isEqualTo(UserRole.USER);
    }
    
    @Test
    void 無効なコードの場合例外をスローすること() {
        assertThatThrownBy(() -> UserRole.fromCode(99))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid role code");
    }
    
    @Test
    void 管理者権限チェックが正しく動作すること() {
        assertThat(UserRole.ADMIN.hasAdminPrivilege()).isTrue();
        assertThat(UserRole.MANAGER.hasAdminPrivilege()).isFalse();
        assertThat(UserRole.USER.hasAdminPrivilege()).isFalse();
    }
    
    @Test
    void マネージャー権限チェックが正しく動作すること() {
        assertThat(UserRole.ADMIN.hasManagerPrivilege()).isTrue();
        assertThat(UserRole.MANAGER.hasManagerPrivilege()).isTrue();
        assertThat(UserRole.USER.hasManagerPrivilege()).isFalse();
    }
}
```

---

## ✅ 実装チェックリスト

### Entity実装時

- [ ] **Pure Java**で実装（Spring/JPAアノテーション使用禁止）
- [ ] **ビジネスロジック**をEntityメソッドとして実装
- [ ] **finalフィールド**で不変性を保証（ID、createdAt等）
- [ ] **コンストラクタでバリデーション**を実施
- [ ] **Factoryメソッド**で生成（コンストラクタはprivate）
- [ ] **setterを提供しない**（ビジネスロジックメソッドのみ）
- [ ] **equals/hashCode**をIDのみで実装
- [ ] **toString**でデバッグ情報を提供
- [ ] **Pure Javaユニットテスト**を実装

### Value Object実装時

- [ ] **不変オブジェクト**として実装（finalフィールド）
- [ ] **equals/hashCode**をすべてのフィールドで実装
- [ ] **setterを提供しない**
- [ ] **バリデーション**をコンストラクタで実施

### 対応する他のコンポーネント

- [ ] **Application層**: Command/Result（Domain Entityを使用）が存在
- [ ] **Infrastructure層**: JPA Entity（Domain Entityと分離）が存在
- [ ] **Infrastructure層**: Mapper（Domain Entity ⇔ JPA Entity変換）が存在
- [ ] **テスト**: Pure Javaユニットテストが実装済み

---

## 📚 参考資料

### プロジェクト内ドキュメント
- **[AGENTS.md](../../../../../AGENTS.md)** - AIエージェント向けコンテキストドキュメント
- **[Domain層 README](../README.md)** - Domain層全体の概要
- **[Infrastructure層 persistence/entity README](../../infrastructure/persistence/entity/README.md)** - JPA Entity実装パターン

### 外部参考資料
- [Domain-Driven Design (Eric Evans)](https://www.domainlanguage.com/ddd/)
- [Clean Architecture (Robert C. Martin)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)

---

## 🎯 まとめ

Domain層のEntityとValue Objectは、**ビジネスルールとドメインロジックの中核**を担う最も重要なコンポーネントです。

### 重要ポイント

1. **Pure Java（最重要）**
   - Spring、JPA、Lombokアノテーション使用禁止
   - フレームワークに一切依存しない
   - Pure Javaユニットテストで高速にテスト

2. **リッチドメインモデル**
   - ビジネスロジックをEntityメソッドとして実装
   - setterを提供せず、ビジネスルールを強制
   - 貧血モデル（Anemic Domain Model）を避ける

3. **不変性の保証**
   - finalフィールドで不変性を保証
   - Factoryメソッドで生成
   - コンストラクタでバリデーション

4. **依存方向**
   - すべての層がDomain Entityに依存
   - Domain EntityはPresentation/Infrastructure層に依存しない
   - クリーンアーキテクチャの最内層

### 禁止事項

- ❌ Domain EntityにJPA/Springアノテーション使用
- ❌ Domain EntityがPresentation層のDTOを返却
- ❌ ビジネスロジックを持たない貧血モデル

このREADMEを参考に、**Pure Javaで実装された高品質なDomain Entity**を構築してください。Domain Entityは、アプリケーションの価値の源泉であり、最も保護すべきコンポーネントです。

---

**作成日**: 2026年1月5日  
**対象バージョン**: Spring Boot TODO API v0.0.1-SNAPSHOT  
**対象パッケージ**: `com.api.todos.domain.model`
