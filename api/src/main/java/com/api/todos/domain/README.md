# Domain層（ドメイン層）

## 📋 パッケージ概要

**場所**: `api/src/main/java/com/api/todos/domain/`

**目的**: ビジネスロジックの中核を担う最も重要な層。企業のビジネスルール・ドメインロジックを Pure Java で実装し、**いかなる外部フレームワークにも依存しない**純粋なビジネスロジックを提供します。

**主要コンポーネント**:
- **model/**: エンティティ（Entity）・値オブジェクト（Value Object）
- **repository/**: リポジトリインターフェース（Repository Interface）
- **service/**: ドメインサービス（Domain Service）

---

## 🏛️ クリーンアーキテクチャにおける位置づけ

```
┌─────────────────────────────────────────────────┐
│  Presentation層 (Controllers, REST API)         │
├─────────────────────────────────────────────────┤
│  Infrastructure層 (Persistence, Security)       │
├─────────────────────────────────────────────────┤
│  Application層 (UseCases - Pure Java)           │
├─────────────────────────────────────────────────┤
│  Domain層 ← ★ このパッケージ（最内層）         │
│  - model/ (Entity, Value Object)                │
│  - repository/ (Repository Interface)           │
│  - service/ (Domain Service)                    │
│  ★ Pure Java - フレームワーク依存なし          │
└─────────────────────────────────────────────────┘
```

### 依存関係の方向（The Dependency Rule）

```
Domain層（最内層）
    ↑ 依存される
Application層
    ↑ 依存される
Infrastructure層
    ↑ 依存される
Presentation層
```

**重要**: Domain層は **いかなる外側の層にも依存しません**。すべての依存関係は Domain層に向かって流れます。これが **依存性逆転の原則（Dependency Inversion Principle）** の実現です。

---

## 🎯 責務（Responsibilities）

### ✅ このパッケージが行うべきこと

1. **ビジネスルール・ドメインロジックの実装**
   - エンティティ内にビジネスルールを実装
   - ビジネス不変条件（Invariants）の維持
   - ドメイン固有のバリデーション

2. **エンティティ（Entity）の定義**
   - ビジネスの中心概念を表現
   - 一意な識別子（ID）を持つオブジェクト
   - ライフサイクルを通じて同一性を維持

3. **値オブジェクト（Value Object）の定義**
   - 不変（Immutable）なオブジェクト
   - 属性の組み合わせで同一性を判定
   - ビジネスコンセプトを表現

4. **リポジトリインターフェース（Repository Interface）の定義**
   - データ永続化の抽象インターフェース
   - Infrastructure層で実装（依存性逆転）
   - ドメインオブジェクトの保存・取得を定義

5. **ドメインサービス（Domain Service）の実装**
   - 単一のエンティティに属さないビジネスロジック
   - 複数エンティティを跨るドメインロジック
   - Pure Javaで実装

### ❌ このパッケージが行ってはいけないこと

1. **外部フレームワークへの依存**
   - Spring、JPA、Hibernate等のアノテーション使用禁止
   - データベース固有の実装禁止
   - REST API固有の実装禁止

2. **Infrastructure層の実装詳細への依存**
   - JPA Entity への依存禁止
   - データベーステーブル構造への依存禁止
   - 外部サービスの実装詳細への依存禁止

3. **Presentation層のDTOへの依存**
   - REST APIのリクエスト・レスポンスDTOの使用禁止
   - HTTPリクエスト・レスポンスへの依存禁止

4. **Application層への依存**
   - UseCase、Command、Resultオブジェクトへの依存禁止
   - Application層のロジックをDomain層に記述しない

---

## 🚨 絶対原則：Pure Java（フレームワーク依存禁止）

Domain層は **Pure Java** で実装しなければなりません。以下のアノテーション・ライブラリは **絶対に使用禁止** です：

### ❌ 禁止されるアノテーション・ライブラリ

```java
// ❌ Spring Framework
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

// ❌ JPA/Hibernate
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Id;

// ❌ Lombok（Domain層では非推奨）
import lombok.Getter;
import lombok.Setter;
import lombok.Data;

// ❌ Jackson（JSON Serialization）
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

// ✅ Pure Javaのコンストラクタ、getter、equals、hashCode
public class Todo {
    private final UUID id;
    private String title;
    
    public Todo(UUID id, String title) {
        this.id = id;
        this.title = title;
    }
    
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    
    @Override
    public boolean equals(Object o) {
        // Pure Javaで実装
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

---

## 🚨 禁止パターン（Anti-Patterns）

### ❌ 1. Domain層のEntityにSpring/JPAアノテーションを付与

**問題**: Domain層がフレームワークに依存してしまい、Pure Javaの原則に違反します。

```java
// ❌ 絶対禁止: Domain層のEntityにJPAアノテーションを付与
package com.api.todos.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import java.util.UUID;

@Entity  // ❌ Domain層でJPA依存は禁止
@Table(name = "todos")  // ❌ データベーステーブルへの依存
public class Todo {
    @Id  // ❌ JPAアノテーション使用禁止
    @Column(name = "id")  // ❌ カラム名への依存
    private UUID id;
    
    @Column(name = "title", nullable = false, length = 32)  // ❌ DB制約への依存
    private String title;
}
```

**なぜダメか**:
- Domain層がJPA/Hibernateに依存してしまう
- データベーステーブル構造がドメインモデルに影響する
- ドメインモデルの変更がデータベーススキーマに縛られる
- テストが困難（JPAコンテキスト必須）
- Clean Architectureの「内側の層はフレームワークに依存しない」原則に違反

**正しい実装**:
```java
// ✅ 正しい実装: Domain層のEntityはPure Java
package com.api.todos.domain.model;

import java.util.UUID;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * TODOエンティティ（Domain層）
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
    
    // コンストラクタ（Pure Java）
    public Todo(UUID id, String title, String descriptions, boolean completed,
                UUID userId, LocalDateTime createdAt, LocalDateTime updatedAt, boolean deleted) {
        // バリデーション（ビジネスルール）
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
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
        this.deleted = deleted;
    }
    
    // ビジネスルール: TODO完了状態の変更
    public void markAsCompleted() {
        if (this.completed) {
            throw new IllegalStateException("TODO is already completed");
        }
        this.completed = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    // ビジネスルール: TODO未完了状態に戻す
    public void markAsIncomplete() {
        if (!this.completed) {
            throw new IllegalStateException("TODO is already incomplete");
        }
        this.completed = false;
        this.updatedAt = LocalDateTime.now();
    }
    
    // ビジネスルール: TODO削除（論理削除）
    public void delete() {
        if (this.deleted) {
            throw new IllegalStateException("TODO is already deleted");
        }
        this.deleted = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    // ビジネスルール: TODOタイトル更新
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
    
    // Getter（Pure Java）
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescriptions() { return descriptions; }
    public boolean isCompleted() { return completed; }
    public UUID getUserId() { return userId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public boolean isDeleted() { return deleted; }
    
    // equals & hashCode（Pure Java）
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
}
```

**JPA Entityは Infrastructure層で別途定義**:
```java
// ✅ Infrastructure層でJPA Entity定義（Domain Entityとは分離）
package com.api.todos.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.LocalDateTime;

@Entity
@Table(name = "todos", schema = "public")
public class TodoJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, length = 32)
    private String title;
    
    // ... 他のフィールド
    
    // getter, setter（Infrastructure層ではLombok使用OK）
}
```

---

### ❌ 2. Domain層のRepositoryに実装を含める

**問題**: Domain層にはインターフェースのみを定義し、実装はInfrastructure層で行うべきです。

```java
// ❌ 絶対禁止: Domain層のRepositoryに実装を含める
package com.api.todos.domain.repository;

import com.api.todos.domain.model.Todo;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Repository  // ❌ Domain層でSpringアノテーション使用禁止
public class TodoRepository {
    private final Map<UUID, Todo> database = new HashMap<>();  // ❌ 実装を含めている
    
    public Todo save(Todo todo) {
        database.put(todo.getId(), todo);  // ❌ 永続化実装をDomain層に記述
        return todo;
    }
    
    public List<Todo> findByUserId(UUID userId) {
        return database.values().stream()
            .filter(todo -> todo.getUserId().equals(userId))
            .toList();
    }
}
```

**なぜダメか**:
- Domain層が永続化の実装詳細に依存する
- テスト時にモックが困難
- データベース技術の変更がDomain層に影響する
- 依存性逆転の原則に違反

**正しい実装**:
```java
// ✅ 正しい実装: Domain層ではインターフェースのみ定義
package com.api.todos.domain.repository;

import com.api.todos.domain.model.Todo;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

/**
 * TODOリポジトリインターフェース（Domain層）
 * 
 * Domain層で定義、Infrastructure層で実装（依存性逆転の原則）
 * 永続化の実装詳細はInfrastructure層に隠蔽される
 */
public interface TodoRepository {
    /**
     * TODOをIDで検索
     */
    Optional<Todo> findById(UUID id);
    
    /**
     * ユーザーIDでTODO一覧を検索
     */
    List<Todo> findByUserId(UUID userId);
    
    /**
     * TODOを保存（新規作成・更新）
     */
    Todo save(Todo todo);
    
    /**
     * TODOを削除
     */
    void delete(UUID id);
    
    /**
     * ユーザーIDとTODO IDで検索（アクセス制御用）
     */
    Optional<Todo> findByIdAndUserId(UUID id, UUID userId);
}
```

```java
// ✅ Infrastructure層で実装
package com.api.todos.infrastructure.persistence.repository;

import com.api.todos.domain.model.Todo;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.infrastructure.persistence.entity.TodoJpaEntity;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

@Repository
public class TodoRepositoryImpl implements TodoRepository {
    private final TodoJpaRepository jpaRepository;
    private final TodoMapper mapper;
    
    public TodoRepositoryImpl(TodoJpaRepository jpaRepository, TodoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Optional<Todo> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomainModel);
    }
    
    @Override
    public List<Todo> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
            .map(mapper::toDomainModel)
            .toList();
    }
    
    @Override
    public Todo save(Todo todo) {
        TodoJpaEntity entity = mapper.toJpaEntity(todo);
        TodoJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomainModel(saved);
    }
    
    @Override
    public void delete(UUID id) {
        jpaRepository.deleteById(id);
    }
    
    @Override
    public Optional<Todo> findByIdAndUserId(UUID id, UUID userId) {
        return jpaRepository.findByIdAndUserId(id, userId)
            .map(mapper::toDomainModel);
    }
}
```

---

### ❌ 3. Domain層のEntityがPresentation層のDTOを返却

**問題**: Domain層がPresentation層に依存してしまい、依存関係の方向が逆転します。

```java
// ❌ 絶対禁止: Domain層のEntityがPresentation層のDTOを返却
package com.api.todos.domain.model;

import com.api.todos.presentation.dto.TodoResponse;  // ❌ Presentation層への依存
import java.util.UUID;

public class Todo {
    private UUID id;
    private String title;
    
    // ❌ Presentation層のDTOに変換するメソッドをDomain層に実装
    public TodoResponse toResponse() {
        TodoResponse response = new TodoResponse();
        response.setId(this.id);
        response.setTitle(this.title);
        return response;
    }
}
```

**なぜダメか**:
- Domain層がPresentation層に依存（依存関係の逆転）
- REST API仕様の変更がDomain層に影響する
- Domain層がHTTP/REST固有の実装に縛られる
- Clean Architectureの依存関係の方向に違反

**正しい実装**:
```java
// ✅ 正しい実装: Domain層のEntityはPure Java
package com.api.todos.domain.model;

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
    
    // Pure Javaのコンストラクタ、getter、ビジネスロジック
    // DTOへの変換ロジックは含めない
}
```

```java
// ✅ Presentation層でDTO変換
package com.api.todos.presentation.dto;

import com.api.todos.application.dto.TodoResult;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;
import java.time.LocalDateTime;

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
    
    // ✅ Presentation層でApplication層のResultから変換
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

---

## ✅ 正しい実装パターン

### 1. Entity（エンティティ）- TODOエンティティ

**目的**: ビジネスの中心概念を表現し、ビジネスルールを実装します。

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
 * ビジネスルール・ドメインロジックを実装
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
    // コンストラクタ
    // ========================================
    
    /**
     * 新規TODO作成用コンストラクタ
     */
    public static Todo create(String title, String descriptions, UUID userId) {
        return new Todo(
            UUID.randomUUID(),
            title,
            descriptions,
            false,
            userId,
            LocalDateTime.now(),
            LocalDateTime.now(),
            false
        );
    }
    
    /**
     * 既存TODO再構築用コンストラクタ（Repository層から呼ばれる）
     */
    public Todo(UUID id, String title, String descriptions, boolean completed,
                UUID userId, LocalDateTime createdAt, LocalDateTime updatedAt, boolean deleted) {
        // バリデーション（ビジネスルール）
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
    
    // ========================================
    // ビジネスロジック（ドメインロジック）
    // ========================================
    
    /**
     * TODO完了状態に変更
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
     * TODO未完了状態に戻す
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
     * TODO削除（論理削除）
     */
    public void delete() {
        if (this.deleted) {
            throw new IllegalStateException("TODO is already deleted");
        }
        this.deleted = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * TODOタイトル更新
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
     * TODO説明更新
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
     * 指定されたユーザーがこのTODOの所有者かチェック
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
    // equals & hashCode（IDで同一性判定）
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
                ", userId=" + userId +
                ", deleted=" + deleted +
                '}';
    }
}
```

---

### 2. Entity（エンティティ）- Userエンティティ

**目的**: ユーザーのビジネスルール・認証情報を管理します。

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
 * 認証・認可に関するビジネスルールを実装
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
    
    /**
     * 新規ユーザー作成用コンストラクタ
     */
    public static User create(String username, String email, String firstName, 
                             String lastName, UserRole role) {
        return new User(
            UUID.randomUUID(),
            username,
            email,
            null,  // パスワードは後で初期化
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
     * 既存ユーザー再構築用コンストラクタ（Repository層から呼ばれる）
     */
    public User(UUID id, String username, String email, String hashedPassword,
                String firstName, String lastName, UserRole role, boolean passwordInitialized,
                LocalDateTime createdAt, LocalDateTime updatedAt, boolean deleted) {
        // バリデーション
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
    
    // ========================================
    // ビジネスロジック
    // ========================================
    
    /**
     * パスワード初期化
     */
    public void initializePassword(String hashedPassword) {
        if (hashedPassword == null || hashedPassword.isEmpty()) {
            throw new IllegalArgumentException("Hashed password must not be empty");
        }
        if (this.passwordInitialized) {
            throw new IllegalStateException("Password is already initialized");
        }
        if (this.deleted) {
            throw new IllegalStateException("Cannot initialize password for deleted user");
        }
        this.hashedPassword = hashedPassword;
        this.passwordInitialized = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * パスワード変更
     */
    public void changePassword(String newHashedPassword) {
        if (newHashedPassword == null || newHashedPassword.isEmpty()) {
            throw new IllegalArgumentException("New hashed password must not be empty");
        }
        if (!this.passwordInitialized) {
            throw new IllegalStateException("Cannot change password before initialization");
        }
        if (this.deleted) {
            throw new IllegalStateException("Cannot change password for deleted user");
        }
        this.hashedPassword = newHashedPassword;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * ユーザー情報更新
     */
    public void updateProfile(String email, String firstName, String lastName) {
        if (email != null && !email.isEmpty()) {
            this.email = email;
        }
        if (this.deleted) {
            throw new IllegalStateException("Cannot update deleted user");
        }
        this.firstName = firstName;
        this.lastName = lastName;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * ロール変更（管理者のみ実行可能）
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
     * ユーザー削除（論理削除）
     */
    public void delete() {
        if (this.deleted) {
            throw new IllegalStateException("User is already deleted");
        }
        this.deleted = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * パスワード初期化済みかチェック
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
    public boolean isPasswordInitialized() { return passwordInitialized; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public boolean isDeleted() { return deleted; }
    
    // ========================================
    // equals & hashCode
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
                ", email='" + email + '\'' +
                ", role=" + role +
                ", passwordInitialized=" + passwordInitialized +
                ", deleted=" + deleted +
                '}';
    }
}
```

---

### 3. Value Object（値オブジェクト）- UserRole

**目的**: ユーザーのロールを表現する不変オブジェクト。

**実装例**:
```java
// api/src/main/java/com/api/todos/domain/model/UserRole.java
package com.api.todos.domain.model;

/**
 * ユーザーロール（Value Object）
 * 
 * 0: ADMIN（管理者）
 * 1: MANAGER（マネージャー）
 * 2: USER（一般ユーザー）
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
     */
    public static UserRole fromCode(int code) {
        for (UserRole role : UserRole.values()) {
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

### 4. Repository Interface（リポジトリインターフェース）- TodoRepository

**目的**: データ永続化の抽象インターフェースを定義し、Infrastructure層で実装します。

**実装例**:
```java
// api/src/main/java/com/api/todos/domain/repository/TodoRepository.java
package com.api.todos.domain.repository;

import com.api.todos.domain.model.Todo;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

/**
 * TODOリポジトリインターフェース（Domain層）
 * 
 * Domain層で定義、Infrastructure層で実装（依存性逆転の原則）
 * 永続化の実装詳細はInfrastructure層に隠蔽される
 */
public interface TodoRepository {
    /**
     * TODOをIDで検索
     * 
     * @param id TODO ID
     * @return TODOエンティティ（存在しない場合はEmpty）
     */
    Optional<Todo> findById(UUID id);
    
    /**
     * ユーザーIDでTODO一覧を検索
     * 
     * @param userId ユーザーID
     * @return TODO一覧（削除済みTODOは除外）
     */
    List<Todo> findByUserId(UUID userId);
    
    /**
     * TODOを保存（新規作成・更新）
     * 
     * @param todo TODOエンティティ
     * @return 保存されたTODOエンティティ
     */
    Todo save(Todo todo);
    
    /**
     * TODOを削除
     * 
     * @param id TODO ID
     */
    void delete(UUID id);
    
    /**
     * ユーザーIDとTODO IDで検索（アクセス制御用）
     * 
     * @param id TODO ID
     * @param userId ユーザーID
     * @return TODOエンティティ（存在しない場合はEmpty）
     */
    Optional<Todo> findByIdAndUserId(UUID id, UUID userId);
    
    /**
     * タイトルで検索（部分一致）
     * 
     * @param userId ユーザーID
     * @param titleKeyword タイトルキーワード
     * @return TODO一覧
     */
    List<Todo> searchByTitle(UUID userId, String titleKeyword);
}
```

---

### 5. Repository Interface（リポジトリインターフェース）- UserRepository

**目的**: ユーザーデータの永続化インターフェースを定義します。

**実装例**:
```java
// api/src/main/java/com/api/todos/domain/repository/UserRepository.java
package com.api.todos.domain.repository;

import com.api.todos.domain.model.User;
import com.api.todos.domain.model.UserRole;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

/**
 * ユーザーリポジトリインターフェース（Domain層）
 * 
 * Domain層で定義、Infrastructure層で実装（依存性逆転の原則）
 */
public interface UserRepository {
    /**
     * ユーザーをIDで検索
     * 
     * @param id ユーザーID
     * @return ユーザーエンティティ（存在しない場合はEmpty）
     */
    Optional<User> findById(UUID id);
    
    /**
     * ユーザー名でユーザーを検索
     * 
     * @param username ユーザー名
     * @return ユーザーエンティティ（存在しない場合はEmpty）
     */
    Optional<User> findByUsername(String username);
    
    /**
     * メールアドレスでユーザーを検索
     * 
     * @param email メールアドレス
     * @return ユーザーエンティティ（存在しない場合はEmpty）
     */
    Optional<User> findByEmail(String email);
    
    /**
     * ユーザーを保存（新規作成・更新）
     * 
     * @param user ユーザーエンティティ
     * @return 保存されたユーザーエンティティ
     */
    User save(User user);
    
    /**
     * ユーザーを削除
     * 
     * @param id ユーザーID
     */
    void delete(UUID id);
    
    /**
     * 全ユーザー一覧を取得
     * 
     * @return ユーザー一覧（削除済みユーザーは除外）
     */
    List<User> findAll();
    
    /**
     * ロール別ユーザー一覧を取得
     * 
     * @param role ユーザーロール
     * @return ユーザー一覧
     */
    List<User> findByRole(UserRole role);
    
    /**
     * ユーザー名が存在するかチェック
     * 
     * @param username ユーザー名
     * @return 存在する場合true
     */
    boolean existsByUsername(String username);
    
    /**
     * メールアドレスが存在するかチェック
     * 
     * @param email メールアドレス
     * @return 存在する場合true
     */
    boolean existsByEmail(String email);
}
```

---

### 6. Domain Service（ドメインサービス）- TodoDomainService（オプション）

**目的**: 単一のエンティティに属さないドメインロジック、複数エンティティを跨るロジックを実装します。

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
    
    /**
     * ユーザーがTODOの所有者かチェック
     * 
     * @param todo TODO
     * @param user ユーザー
     * @return 所有者の場合true
     */
    public boolean isOwner(Todo todo, User user) {
        return todo.getUserId().equals(user.getId());
    }
    
    /**
     * 未完了TODO一覧を取得
     * 
     * @param todos TODO一覧
     * @return 未完了TODO一覧
     */
    public List<Todo> filterIncompleteTodos(List<Todo> todos) {
        return todos.stream()
            .filter(todo -> !todo.isCompleted())
            .filter(todo -> !todo.isDeleted())
            .collect(Collectors.toList());
    }
    
    /**
     * 完了TODO一覧を取得
     * 
     * @param todos TODO一覧
     * @return 完了TODO一覧
     */
    public List<Todo> filterCompletedTodos(List<Todo> todos) {
        return todos.stream()
            .filter(Todo::isCompleted)
            .filter(todo -> !todo.isDeleted())
            .collect(Collectors.toList());
    }
}
```

---

## 🔄 データフロー - Domain層の役割

```
┌────────────────────────────────────────────────────────────────┐
│ Presentation層 (Controller)                                    │
│  - HTTPリクエスト受信                                           │
│  - Presentation層DTOをApplication層Commandに変換                │
└────────────────────────────────────────────────────────────────┘
                            ↓
┌────────────────────────────────────────────────────────────────┐
│ Infrastructure層 (Service - トランザクション管理)               │
│  - @Transactional適用                                          │
│  - UseCaseを実行                                               │
└────────────────────────────────────────────────────────────────┘
                            ↓
┌────────────────────────────────────────────────────────────────┐
│ Application層 (UseCase - Pure Java)                            │
│  - ビジネスフローのオーケストレーション                         │
│  - Domain層のRepositoryを使用してデータ取得                     │
│  - Domain層のEntityのビジネスロジックを実行                     │
└────────────────────────────────────────────────────────────────┘
                            ↓
┌────────────────────────────────────────────────────────────────┐
│ Domain層 ← ★ このパッケージ                                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Repository Interface (TodoRepository, UserRepository)    │  │
│  │  - データ永続化の抽象インターフェース                     │  │
│  └──────────────────────────────────────────────────────────┘  │
│                            ↓ 実装                               │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Entity (Todo, User)                                      │  │
│  │  - ビジネスルール・ドメインロジック実装                   │  │
│  │  - markAsCompleted(), delete(), updateTitle()...         │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
                            ↑ 実装
┌────────────────────────────────────────────────────────────────┐
│ Infrastructure層 (Repository実装)                               │
│  - TodoRepositoryImpl (Domain層インターフェースを実装)          │
│  - Domain Entity ⇔ JPA Entity 変換（Mapper）                   │
│  - JPA Repositoryを使用してデータベースアクセス                 │
└────────────────────────────────────────────────────────────────┘
                            ↓
┌────────────────────────────────────────────────────────────────┐
│ Database (PostgreSQL)                                          │
└────────────────────────────────────────────────────────────────┘
```

**重要ポイント**:
1. Domain層は **他の層に依存しない**（Pure Java）
2. Domain層の Repository Interface を Infrastructure層で実装（依存性逆転）
3. Domain層の Entity にビジネスロジックを集約
4. Application層が Domain層のビジネスロジックをオーケストレーション

---

## 🧪 テスト戦略

### 1. Entity テスト（Pure Java ユニットテスト）

**目的**: Entityのビジネスロジックが正しく動作するかテストします。

**実装例**:
```java
package com.api.todos.domain.model;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Todoエンティティ テスト
 * Pure Javaのユニットテスト - モック不要
 */
class TodoTest {
    
    @Test
    void TODOを新規作成できること() {
        UUID userId = UUID.randomUUID();
        
        Todo todo = Todo.create("Test TODO", "Test Description", userId);
        
        assertThat(todo.getId()).isNotNull();
        assertThat(todo.getTitle()).isEqualTo("Test TODO");
        assertThat(todo.getDescriptions()).isEqualTo("Test Description");
        assertThat(todo.isCompleted()).isFalse();
        assertThat(todo.getUserId()).isEqualTo(userId);
        assertThat(todo.isDeleted()).isFalse();
    }
    
    @Test
    void TODOを完了状態に変更できること() {
        Todo todo = Todo.create("Test TODO", "Description", UUID.randomUUID());
        
        todo.markAsCompleted();
        
        assertThat(todo.isCompleted()).isTrue();
    }
    
    @Test
    void 完了済みTODOを再度完了にできないこと() {
        Todo todo = Todo.create("Test TODO", "Description", UUID.randomUUID());
        todo.markAsCompleted();
        
        assertThatThrownBy(() -> todo.markAsCompleted())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already completed");
    }
    
    @Test
    void TODOを未完了状態に戻せること() {
        Todo todo = Todo.create("Test TODO", "Description", UUID.randomUUID());
        todo.markAsCompleted();
        
        todo.markAsIncomplete();
        
        assertThat(todo.isCompleted()).isFalse();
    }
    
    @Test
    void TODOを論理削除できること() {
        Todo todo = Todo.create("Test TODO", "Description", UUID.randomUUID());
        
        todo.delete();
        
        assertThat(todo.isDeleted()).isTrue();
    }
    
    @Test
    void 削除済みTODOを完了にできないこと() {
        Todo todo = Todo.create("Test TODO", "Description", UUID.randomUUID());
        todo.delete();
        
        assertThatThrownBy(() -> todo.markAsCompleted())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("deleted TODO");
    }
    
    @Test
    void TODOタイトルを更新できること() {
        Todo todo = Todo.create("Old Title", "Description", UUID.randomUUID());
        
        todo.updateTitle("New Title");
        
        assertThat(todo.getTitle()).isEqualTo("New Title");
    }
    
    @Test
    void タイトルが空の場合エラーになること() {
        assertThatThrownBy(() -> Todo.create("", "Description", UUID.randomUUID()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Title must not be empty");
    }
    
    @Test
    void タイトルが32文字を超える場合エラーになること() {
        String longTitle = "a".repeat(33);
        
        assertThatThrownBy(() -> Todo.create(longTitle, "Description", UUID.randomUUID()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not exceed 32 characters");
    }
    
    @Test
    void 指定されたユーザーがTODOの所有者かチェックできること() {
        UUID userId = UUID.randomUUID();
        Todo todo = Todo.create("Test TODO", "Description", userId);
        
        assertThat(todo.isOwnedBy(userId)).isTrue();
        assertThat(todo.isOwnedBy(UUID.randomUUID())).isFalse();
    }
}
```

---

### 2. Value Object テスト

**実装例**:
```java
package com.api.todos.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * UserRole テスト
 */
class UserRoleTest {
    
    @Test
    void コードからUserRoleを取得できること() {
        assertThat(UserRole.fromCode(0)).isEqualTo(UserRole.ADMIN);
        assertThat(UserRole.fromCode(1)).isEqualTo(UserRole.MANAGER);
        assertThat(UserRole.fromCode(2)).isEqualTo(UserRole.USER);
    }
    
    @Test
    void 無効なコードの場合エラーになること() {
        assertThatThrownBy(() -> UserRole.fromCode(999))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid role code");
    }
    
    @Test
    void 管理者権限を持つかチェックできること() {
        assertThat(UserRole.ADMIN.hasAdminPrivilege()).isTrue();
        assertThat(UserRole.MANAGER.hasAdminPrivilege()).isFalse();
        assertThat(UserRole.USER.hasAdminPrivilege()).isFalse();
    }
    
    @Test
    void マネージャー権限を持つかチェックできること() {
        assertThat(UserRole.ADMIN.hasManagerPrivilege()).isTrue();
        assertThat(UserRole.MANAGER.hasManagerPrivilege()).isTrue();
        assertThat(UserRole.USER.hasManagerPrivilege()).isFalse();
    }
}
```

---

## ✅ 実装チェックリスト

### Entity実装時

- [ ] **Pure Java**で実装（Spring/JPA/Lombokアノテーション使用禁止）
- [ ] **ビジネスルール**をEntityメソッドとして実装
- [ ] **不変フィールド**（ID、作成日時等）は`final`で定義
- [ ] **バリデーション**をコンストラクタ・メソッドで実施
- [ ] **equals & hashCode**をIDで実装
- [ ] **ビジネス例外**を適切にスロー（`IllegalArgumentException`, `IllegalStateException`）
- [ ] **テスト**をPure Javaユニットテストで実装

### Repository Interface実装時

- [ ] **インターフェースのみ**を定義（実装はInfrastructure層）
- [ ] **Domain Entity**を返却型・引数型として使用
- [ ] **JPA Entity**やデータベース固有の型を使用しない
- [ ] **メソッドのシグネチャ**がビジネス要求を表現
- [ ] **JavaDocコメント**でメソッドの責務を明記

### Value Object実装時

- [ ] **不変（Immutable）**で実装（全フィールドfinal）
- [ ] **equals & hashCode**を全フィールドで実装
- [ ] **バリデーション**をコンストラクタで実施
- [ ] **Pure Java**で実装

### Domain Service実装時（オプション）

- [ ] **Pure Java**で実装
- [ ] **複数エンティティを跨るロジック**を実装
- [ ] **単一エンティティのビジネスルール**はEntityに実装（Serviceに記述しない）
- [ ] **Repositoryインターフェース**への依存は最小限に

### 禁止事項チェック

- [ ] Domain層で**Springアノテーション**を使用していない
- [ ] Domain層で**JPAアノテーション**を使用していない
- [ ] Domain層で**Lombokアノテーション**を使用していない
- [ ] Domain層が**Infrastructure層**に依存していない
- [ ] Domain層が**Presentation層**に依存していない
- [ ] Domain層が**Application層**に依存していない

### 対応する他のコンポーネント

- [ ] **Application層**: UseCase（Pure Java）でDomain Entityを使用
- [ ] **Infrastructure層**: Repository実装（TodoRepositoryImpl等）が存在
- [ ] **Infrastructure層**: Mapper（Domain Entity ⇔ JPA Entity変換）が存在
- [ ] **Infrastructure層**: JPA Entity（TodoJpaEntity等）が存在
- [ ] **テスト**: Pure Javaユニットテストが実装済み

---

## 📚 参考資料

### プロジェクト内ドキュメント
- **[AGENTS.md](../../../../AGENTS.md)** - AIエージェント向けコンテキストドキュメント
- **[Application層 README](../application/README.md)** - UseCase実装パターン
- **[Infrastructure層 persistence README](../infrastructure/persistence/README.md)** - Repository実装パターン
- **[Infrastructure層 config README](../infrastructure/config/README.md)** - UseCase Bean登録パターン

### 外部参考資料
- [Clean Architecture (Robert C. Martin)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Domain-Driven Design (Eric Evans)](https://www.domainlanguage.com/ddd/)
- [Effective Java (Joshua Bloch)](https://www.oreilly.com/library/view/effective-java/9780134686097/)

---

## 🎯 まとめ

Domain層は、**クリーンアーキテクチャの最内層として、ビジネスロジックの中核を担う最も重要な層**です。

### 重要ポイント

1. **Pure Java（最重要）**
   - Spring、JPA、Lombokアノテーション使用禁止
   - いかなる外部フレームワークにも依存しない
   - ビジネスロジックをフレームワークから独立させる

2. **依存関係の方向**
   - Domain層は**他の層に依存しない**
   - すべての依存関係は**Domain層に向かって流れる**
   - Repository Interfaceを定義し、Infrastructure層で実装（依存性逆転）

3. **ビジネスロジック実装**
   - Entityメソッドとしてビジネスルールを実装
   - ビジネス不変条件（Invariants）を維持
   - ドメイン固有のバリデーションを実装

4. **テスタビリティ**
   - Pure Javaユニットテストで高速にテスト
   - モック不要（フレームワーク依存なし）
   - ビジネスロジックの品質を保証

### 禁止事項

- ❌ Domain層でSpring/JPA/Lombokアノテーション使用
- ❌ Repository Interfaceに実装を含める
- ❌ Domain EntityがPresentation層DTOを返却
- ❌ Infrastructure層・Application層・Presentation層への依存

このREADMEを参考に、**Pure Javaで実装された高品質なDomain層**を構築してください。Domain層の品質が、アプリケーション全体の品質を決定します。

---

**作成日**: 2026年1月5日  
**対象バージョン**: Spring Boot TODO API v0.0.1-SNAPSHOT  
**対象パッケージ**: `com.api.todos.domain`
