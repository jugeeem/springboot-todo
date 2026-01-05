# Domain層 - repository パッケージ（リポジトリインターフェース）

## 📋 パッケージ概要

**場所**: `api/src/main/java/com/api/todos/domain/repository/`

**目的**: **データアクセスの抽象化**を Pure Java インターフェースで定義するパッケージです。Repository Interfaceは、Domain層でビジネスロジックが必要とするデータアクセスの契約を定義し、Infrastructure層で具体的な実装を行います（**依存性逆転の原則**）。

**主要コンポーネント**:
- **TodoRepository**: TODO関連のデータアクセスインターフェース
- **UserRepository**: ユーザー関連のデータアクセスインターフェース

---

## 🏛️ クリーンアーキテクチャにおける位置づけ

```
┌─────────────────────────────────────────────────┐
│  Presentation層 (Controllers, REST API)         │
├─────────────────────────────────────────────────┤
│  Infrastructure層 (Repository実装)               │
│     implements ↓                                │
├─────────────────────────────────────────────────┤
│  Application層 (UseCases - Pure Java)           │
│     uses ↓                                      │
├─────────────────────────────────────────────────┤
│  Domain層（最内層）                             │
│  ├── model/ (Entity, Value Object)              │
│  ├── repository/ ← ★ このパッケージ             │
│  │   - Repository Interface（Pure Java）        │
│  │   - データアクセスの抽象化                   │
│  └── service/ (Domain Service)                  │
└─────────────────────────────────────────────────┘
```

### 依存性逆転の原則（Dependency Inversion Principle）

```
┌────────────────────────────────────────────┐
│ Application層（UseCase）                   │
│   ↓ 依存（インターフェース）                │
├────────────────────────────────────────────┤
│ Domain層（Repository Interface）← 定義     │
├────────────────────────────────────────────┤
│ Infrastructure層（Repository Impl）← 実装   │
│   implements ↑                             │
└────────────────────────────────────────────┘
```

**重要**: Repository Interfaceは **Domain層で定義** し、**Infrastructure層で実装** します。これにより、Domain層がInfrastructure層に依存しない設計を実現します。

---

## 🎯 責務（Responsibilities）

### ✅ このパッケージが行うべきこと

1. **データアクセスメソッドのインターフェース定義**
   - CRUD操作（Create, Read, Update, Delete）
   - 検索メソッド（findById, findByUserId等）
   - 存在確認メソッド（existsByUsername等）

2. **Domain Entityを型として使用**
   - Repository Interfaceの引数・戻り値は **Domain Entity**
   - JPA Entity等のInfrastructure層の型は使用しない

3. **Pure Javaインターフェースとして定義**
   - 実装を含まない（interfaceのみ）
   - Springアノテーション等のフレームワーク依存なし

4. **ビジネスロジックに必要な操作のみ定義**
   - 不要なメソッドを定義しない
   - ビジネス要件に基づいてメソッドを設計

### ❌ このパッケージが行ってはいけないこと

1. **実装を含める**
   - → Infrastructure層で実装すべき
   - Repository Interfaceは契約のみ定義

2. **JPA/Spring Data JPAのアノテーションを使用**
   - → Infrastructure層の責務
   - Domain層はフレームワークに依存しない

3. **JPA Entity等のInfrastructure層の型を使用**
   - → Domain Entityのみ使用
   - 依存性逆転の原則に違反

4. **具体的なSQL/JPQLを含める**
   - → Infrastructure層で定義
   - Repository Interfaceはデータアクセスの抽象化

---

## 🚨 絶対原則：Pure Java（フレームワーク依存禁止）

Repository Interfaceは **Pure Java** で実装しなければなりません。以下のアノテーション・ライブラリは **絶対に使用禁止** です：

### ❌ 禁止されるアノテーション・ライブラリ

```java
// ❌ Spring Data JPA
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

// ❌ Spring Framework
import org.springframework.stereotype.Repository;

// ❌ JPA/Hibernate
import jakarta.persistence.*;
```

### ✅ 許可されるもの

```java
// ✅ Java標準ライブラリ
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// ✅ Domain層のEntity、Value Object
import com.api.todos.domain.model.Todo;
import com.api.todos.domain.model.User;
import com.api.todos.domain.model.UserRole;

// ✅ Pure Javaのinterface定義
public interface TodoRepository {
    Optional<Todo> findById(UUID id);
    Todo save(Todo todo);
}
```

---

## 🚨 禁止パターン（Anti-Patterns）

### ❌ 1. Repository InterfaceにSpring Data JPAを継承

**問題**: Domain層がSpring Data JPAに依存してしまい、Pure Javaの原則に違反します。

```java
// ❌ 絶対禁止: Domain層でSpring Data JPAを継承
package com.api.todos.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.api.todos.domain.model.Todo;
import java.util.UUID;

// ❌ JpaRepositoryを継承（Spring依存）
public interface TodoRepository extends JpaRepository<Todo, UUID> {
    // ❌ Domain層がSpring Data JPAに依存
}
```

**なぜダメか**:
- Domain層がSpring Frameworkに依存してしまう
- Pure Javaの原則に違反
- フレームワーク変更時に影響を受ける
- テストが困難（Springコンテキスト必須）

**正しい実装**:
```java
// ✅ 正しい実装: Pure Javaのインターフェース（Domain層）
package com.api.todos.domain.repository;

import com.api.todos.domain.model.Todo;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

/**
 * TODOリポジトリインターフェース（Domain層）
 * 
 * Pure Javaで実装 - フレームワーク依存なし
 * Infrastructure層で実装
 */
public interface TodoRepository {
    
    /**
     * IDでTODOを検索
     */
    Optional<Todo> findById(UUID id);
    
    /**
     * ユーザーIDでTODO一覧を取得
     */
    List<Todo> findByUserId(UUID userId);
    
    /**
     * TODOを保存
     */
    Todo save(Todo todo);
    
    /**
     * TODOを削除
     */
    void delete(UUID id);
}
```

```java
// ✅ Infrastructure層でSpring Data JPAを使用
package com.api.todos.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.api.todos.infrastructure.persistence.entity.TodoJpaEntity;
import java.util.UUID;

/**
 * Spring Data JPA Repository（Infrastructure層）
 */
public interface TodoJpaRepository extends JpaRepository<TodoJpaEntity, UUID> {
    // JPA Entityを使用
    List<TodoJpaEntity> findByUserId(UUID userId);
}
```

```java
// ✅ Infrastructure層でDomain Repository Interfaceを実装
package com.api.todos.infrastructure.persistence.repository;

import com.api.todos.domain.model.Todo;
import com.api.todos.domain.repository.TodoRepository;
import org.springframework.stereotype.Repository;

@Repository
public class TodoRepositoryImpl implements TodoRepository {
    private final TodoJpaRepository jpaRepository;
    private final TodoMapper todoMapper;
    
    public TodoRepositoryImpl(TodoJpaRepository jpaRepository, TodoMapper todoMapper) {
        this.jpaRepository = jpaRepository;
        this.todoMapper = todoMapper;
    }
    
    @Override
    public Optional<Todo> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(todoMapper::toDomainModel);
    }
    
    @Override
    public List<Todo> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
            .map(todoMapper::toDomainModel)
            .toList();
    }
    
    @Override
    public Todo save(Todo todo) {
        TodoJpaEntity entity = todoMapper.toJpaEntity(todo);
        TodoJpaEntity saved = jpaRepository.save(entity);
        return todoMapper.toDomainModel(saved);
    }
    
    @Override
    public void delete(UUID id) {
        jpaRepository.deleteById(id);
    }
}
```

---

### ❌ 2. Repository InterfaceにJPA Entityを使用

**問題**: Repository InterfaceがInfrastructure層のJPA Entityに依存し、依存性逆転の原則に違反します。

```java
// ❌ 絶対禁止: Domain層のRepository InterfaceでJPA Entityを使用
package com.api.todos.domain.repository;

import com.api.todos.infrastructure.persistence.entity.TodoJpaEntity;  // ❌ Infrastructure層に依存
import java.util.UUID;
import java.util.Optional;

/**
 * ❌ JPA EntityをDomain層で使用（依存方向が逆）
 */
public interface TodoRepository {
    
    // ❌ Infrastructure層のJPA Entityを使用
    Optional<TodoJpaEntity> findById(UUID id);
    
    // ❌ Infrastructure層のJPA Entityを使用
    TodoJpaEntity save(TodoJpaEntity entity);
}
```

**なぜダメか**:
- Domain層がInfrastructure層に依存（依存方向が逆）
- クリーンアーキテクチャの原則に違反
- Domain層がJPA実装に依存してしまう
- データベース変更時にDomain層も変更が必要

**正しい実装**:
```java
// ✅ 正しい実装: Domain Entityを使用（Domain層）
package com.api.todos.domain.repository;

import com.api.todos.domain.model.Todo;  // ✅ Domain層のEntity
import java.util.UUID;
import java.util.Optional;
import java.util.List;

/**
 * TODOリポジトリインターフェース（Domain層）
 * 
 * Domain Entityを使用 - Infrastructure層に依存しない
 */
public interface TodoRepository {
    
    /**
     * IDでTODOを検索
     * @return Domain Entity（Todo）
     */
    Optional<Todo> findById(UUID id);
    
    /**
     * TODOを保存
     * @param todo Domain Entity
     * @return 保存されたDomain Entity
     */
    Todo save(Todo todo);
}
```

---

### ❌ 3. Repository Interfaceに実装を含める

**問題**: Interfaceに実装を含めると、Domain層がデータアクセスの実装詳細を持ってしまいます。

```java
// ❌ 絶対禁止: Repository Interfaceに実装を含める
package com.api.todos.domain.repository;

import com.api.todos.domain.model.Todo;
import java.util.*;

/**
 * ❌ Interfaceなのに実装を持つ
 */
public interface TodoRepository {
    
    // ❌ データベースの実装詳細をDomain層に含める
    private static final Map<UUID, Todo> database = new HashMap<>();
    
    // ❌ default methodで実装を提供
    default Optional<Todo> findById(UUID id) {
        return Optional.ofNullable(database.get(id));
    }
    
    // ❌ default methodで実装を提供
    default Todo save(Todo todo) {
        database.put(todo.getId(), todo);
        return todo;
    }
    
    // ❌ default methodで実装を提供
    default List<Todo> findByUserId(UUID userId) {
        return database.values().stream()
            .filter(todo -> todo.getUserId().equals(userId))
            .toList();
    }
}
```

**なぜダメか**:
- Repository Interfaceは契約のみ定義すべき
- データアクセスの実装詳細はInfrastructure層の責務
- Domain層がデータベース実装に依存してしまう
- テストが困難（実装を変更できない）

**正しい実装**:
```java
// ✅ 正しい実装: Repository Interfaceは契約のみ定義（Domain層）
package com.api.todos.domain.repository;

import com.api.todos.domain.model.Todo;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

/**
 * TODOリポジトリインターフェース（Domain層）
 * 
 * Pure Javaのinterface - 実装を含まない
 * Infrastructure層で実装を提供
 */
public interface TodoRepository {
    
    /**
     * IDでTODOを検索
     * 
     * @param id TODO ID
     * @return TODO（存在しない場合はOptional.empty()）
     */
    Optional<Todo> findById(UUID id);
    
    /**
     * ユーザーIDでTODO一覧を取得（削除済みを除く）
     * 
     * @param userId ユーザーID
     * @return TODO一覧
     */
    List<Todo> findByUserId(UUID userId);
    
    /**
     * TODOを保存（新規作成・更新）
     * 
     * @param todo TODO
     * @return 保存されたTODO
     */
    Todo save(Todo todo);
    
    /**
     * TODOを削除
     * 
     * @param id TODO ID
     */
    void delete(UUID id);
}
```

```java
// ✅ Infrastructure層で実装を提供
package com.api.todos.infrastructure.persistence.repository;

import com.api.todos.domain.model.Todo;
import com.api.todos.domain.repository.TodoRepository;
import org.springframework.stereotype.Repository;

/**
 * TodoRepositoryの実装（Infrastructure層）
 */
@Repository
public class TodoRepositoryImpl implements TodoRepository {
    private final TodoJpaRepository jpaRepository;
    private final TodoMapper todoMapper;
    
    public TodoRepositoryImpl(TodoJpaRepository jpaRepository, TodoMapper todoMapper) {
        this.jpaRepository = jpaRepository;
        this.todoMapper = todoMapper;
    }
    
    @Override
    public Optional<Todo> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(todoMapper::toDomainModel);
    }
    
    @Override
    public List<Todo> findByUserId(UUID userId) {
        return jpaRepository.findByUserIdAndDeletedFalse(userId).stream()
            .map(todoMapper::toDomainModel)
            .toList();
    }
    
    @Override
    public Todo save(Todo todo) {
        TodoJpaEntity entity = todoMapper.toJpaEntity(todo);
        TodoJpaEntity saved = jpaRepository.save(entity);
        return todoMapper.toDomainModel(saved);
    }
    
    @Override
    public void delete(UUID id) {
        jpaRepository.deleteById(id);
    }
}
```

---

## ✅ 正しい実装パターン

### 1. TodoRepository - TODO管理インターフェース

**目的**: TODO関連のデータアクセス操作を定義します。

**実装例**:
```java
// api/src/main/java/com/api/todos/domain/repository/TodoRepository.java
package com.api.todos.domain.repository;

import com.api.todos.domain.model.Todo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * TODOリポジトリインターフェース（Domain層）
 * 
 * Pure Javaで実装 - フレームワーク依存なし
 * Infrastructure層で実装
 */
public interface TodoRepository {
    
    // ========================================
    // 検索系メソッド
    // ========================================
    
    /**
     * IDでTODOを検索
     * 
     * @param id TODO ID
     * @return TODO（存在しない場合はOptional.empty()）
     */
    Optional<Todo> findById(UUID id);
    
    /**
     * ユーザーIDでTODO一覧を取得
     * 削除済みTODOは除外される
     * 
     * @param userId ユーザーID
     * @return TODO一覧（空の場合は空リスト）
     */
    List<Todo> findByUserId(UUID userId);
    
    /**
     * ユーザーIDとTODO IDでTODOを検索
     * アクセス制御に使用（ユーザーが所有するTODOのみ取得）
     * 
     * @param id TODO ID
     * @param userId ユーザーID
     * @return TODO（存在しないか所有者でない場合はOptional.empty()）
     */
    Optional<Todo> findByIdAndUserId(UUID id, UUID userId);
    
    /**
     * タイトルでTODOを検索
     * 部分一致検索（LIKE検索）
     * 
     * @param userId ユーザーID
     * @param titleKeyword タイトルキーワード
     * @return TODO一覧
     */
    List<Todo> searchByTitle(UUID userId, String titleKeyword);
    
    /**
     * 完了状態でTODOをフィルタリング
     * 
     * @param userId ユーザーID
     * @param completed 完了状態（true: 完了のみ、false: 未完了のみ）
     * @return TODO一覧
     */
    List<Todo> findByUserIdAndCompleted(UUID userId, boolean completed);
    
    // ========================================
    // 保存・更新系メソッド
    // ========================================
    
    /**
     * TODOを保存（新規作成・更新）
     * 
     * IDがnullの場合は新規作成、
     * IDが存在する場合は更新
     * 
     * @param todo TODO
     * @return 保存されたTODO（IDが付与される）
     */
    Todo save(Todo todo);
    
    // ========================================
    // 削除系メソッド
    // ========================================
    
    /**
     * TODOを削除
     * 物理削除または論理削除はInfrastructure層の実装に依存
     * 
     * @param id TODO ID
     */
    void delete(UUID id);
    
    /**
     * ユーザーの全TODOを削除
     * ユーザー削除時に使用
     * 
     * @param userId ユーザーID
     */
    void deleteByUserId(UUID userId);
    
    // ========================================
    // 存在確認系メソッド
    // ========================================
    
    /**
     * TODOが存在するかチェック
     * 
     * @param id TODO ID
     * @return 存在する場合true
     */
    boolean existsById(UUID id);
    
    /**
     * ユーザーがTODOを所有しているかチェック
     * 
     * @param id TODO ID
     * @param userId ユーザーID
     * @return 所有している場合true
     */
    boolean existsByIdAndUserId(UUID id, UUID userId);
    
    // ========================================
    // 集計系メソッド
    // ========================================
    
    /**
     * ユーザーのTODO総数をカウント
     * 削除済みTODOは除外
     * 
     * @param userId ユーザーID
     * @return TODO総数
     */
    long countByUserId(UUID userId);
    
    /**
     * ユーザーの完了TODO数をカウント
     * 
     * @param userId ユーザーID
     * @return 完了TODO数
     */
    long countByUserIdAndCompleted(UUID userId, boolean completed);
}
```

---

### 2. UserRepository - ユーザー管理インターフェース

**目的**: ユーザー関連のデータアクセス操作を定義します。

**実装例**:
```java
// api/src/main/java/com/api/todos/domain/repository/UserRepository.java
package com.api.todos.domain.repository;

import com.api.todos.domain.model.User;
import com.api.todos.domain.model.UserRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ユーザーリポジトリインターフェース（Domain層）
 * 
 * Pure Javaで実装 - フレームワーク依存なし
 * Infrastructure層で実装
 */
public interface UserRepository {
    
    // ========================================
    // 検索系メソッド
    // ========================================
    
    /**
     * IDでユーザーを検索
     * 
     * @param id ユーザーID
     * @return ユーザー（存在しない場合はOptional.empty()）
     */
    Optional<User> findById(UUID id);
    
    /**
     * ユーザー名でユーザーを検索
     * ログイン認証に使用
     * 
     * @param username ユーザー名
     * @return ユーザー（存在しない場合はOptional.empty()）
     */
    Optional<User> findByUsername(String username);
    
    /**
     * メールアドレスでユーザーを検索
     * 
     * @param email メールアドレス
     * @return ユーザー（存在しない場合はOptional.empty()）
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 全ユーザーを取得
     * 管理者機能で使用
     * 
     * @return ユーザー一覧
     */
    List<User> findAll();
    
    /**
     * ロールでユーザーをフィルタリング
     * 
     * @param role ユーザーロール
     * @return ユーザー一覧
     */
    List<User> findByRole(UserRole role);
    
    /**
     * パスワード未初期化のユーザー一覧を取得
     * 
     * @return ユーザー一覧
     */
    List<User> findByPasswordInitializedFalse();
    
    // ========================================
    // 保存・更新系メソッド
    // ========================================
    
    /**
     * ユーザーを保存（新規作成・更新）
     * 
     * IDがnullの場合は新規作成、
     * IDが存在する場合は更新
     * 
     * @param user ユーザー
     * @return 保存されたユーザー（IDが付与される）
     */
    User save(User user);
    
    // ========================================
    // 削除系メソッド
    // ========================================
    
    /**
     * ユーザーを削除
     * 物理削除または論理削除はInfrastructure層の実装に依存
     * 
     * @param id ユーザーID
     */
    void delete(UUID id);
    
    // ========================================
    // 存在確認系メソッド
    // ========================================
    
    /**
     * ユーザーが存在するかチェック
     * 
     * @param id ユーザーID
     * @return 存在する場合true
     */
    boolean existsById(UUID id);
    
    /**
     * ユーザー名が既に使用されているかチェック
     * ユーザー登録時の重複チェックに使用
     * 
     * @param username ユーザー名
     * @return 既に使用されている場合true
     */
    boolean existsByUsername(String username);
    
    /**
     * メールアドレスが既に使用されているかチェック
     * ユーザー登録時の重複チェックに使用
     * 
     * @param email メールアドレス
     * @return 既に使用されている場合true
     */
    boolean existsByEmail(String email);
    
    // ========================================
    // 集計系メソッド
    // ========================================
    
    /**
     * 全ユーザー数をカウント
     * 
     * @return ユーザー数
     */
    long count();
    
    /**
     * ロール別のユーザー数をカウント
     * 
     * @param role ユーザーロール
     * @return ユーザー数
     */
    long countByRole(UserRole role);
}
```

---

### 3. Application層UseCaseでRepository Interfaceを使用

**目的**: Application層のUseCaseからRepository Interfaceを使用します。

**実装例**:
```java
// api/src/main/java/com/api/todos/application/usecase/todo/GetTodoUseCase.java
package com.api.todos.application.usecase.todo;

import com.api.todos.application.dto.TodoResult;
import com.api.todos.domain.model.Todo;
import com.api.todos.domain.repository.TodoRepository;
import java.util.UUID;

/**
 * TODO取得ユースケース（Application層）
 * 
 * Pure Java実装 - Domain層のRepository Interfaceを使用
 */
public class GetTodoUseCase {
    private final TodoRepository todoRepository;  // ✅ Domain層のインターフェースに依存
    
    public GetTodoUseCase(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }
    
    /**
     * TODOを取得する
     * 
     * @param id TODO ID
     * @return TODO結果
     * @throws TodoNotFoundException TODOが見つからない場合
     */
    public TodoResult execute(UUID id) {
        // Repository Interfaceを使用してデータ取得
        Todo todo = todoRepository.findById(id)
            .orElseThrow(() -> new TodoNotFoundException("TODO not found: " + id));
        
        return TodoResult.from(todo);
    }
}
```

```java
// api/src/main/java/com/api/todos/application/usecase/todo/CreateTodoUseCase.java
package com.api.todos.application.usecase.todo;

import com.api.todos.application.command.todo.CreateTodoCommand;
import com.api.todos.application.dto.TodoResult;
import com.api.todos.domain.model.Todo;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.domain.repository.UserRepository;

/**
 * TODO作成ユースケース（Application層）
 */
public class CreateTodoUseCase {
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    
    public CreateTodoUseCase(TodoRepository todoRepository, UserRepository userRepository) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
    }
    
    public TodoResult execute(CreateTodoCommand command) {
        // 1. ユーザー存在確認
        userRepository.findById(command.getUserId())
            .orElseThrow(() -> new UserNotFoundException());
        
        // 2. Todo作成
        Todo todo = Todo.create(
            command.getTitle(),
            command.getDescriptions(),
            command.getUserId()
        );
        
        // 3. 保存
        Todo saved = todoRepository.save(todo);
        
        return TodoResult.from(saved);
    }
}
```

---

### 4. Infrastructure層でRepository Interfaceを実装

**目的**: Infrastructure層でDomain層のRepository Interfaceを実装します。

**実装例**:
```java
// api/src/main/java/com/api/todos/infrastructure/persistence/repository/TodoRepositoryImpl.java
package com.api.todos.infrastructure.persistence.repository;

import com.api.todos.domain.model.Todo;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.infrastructure.persistence.entity.TodoJpaEntity;
import com.api.todos.infrastructure.persistence.mapper.TodoMapper;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * TodoRepositoryの実装（Infrastructure層）
 * 
 * Domain層で定義されたインターフェースを実装
 * Spring Data JPAを使用してデータアクセス
 */
@Repository
public class TodoRepositoryImpl implements TodoRepository {
    private final TodoJpaRepository jpaRepository;
    private final TodoMapper todoMapper;
    
    public TodoRepositoryImpl(TodoJpaRepository jpaRepository, TodoMapper todoMapper) {
        this.jpaRepository = jpaRepository;
        this.todoMapper = todoMapper;
    }
    
    @Override
    public Optional<Todo> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(todoMapper::toDomainModel);
    }
    
    @Override
    public List<Todo> findByUserId(UUID userId) {
        return jpaRepository.findByUserIdAndDeletedFalse(userId).stream()
            .map(todoMapper::toDomainModel)
            .toList();
    }
    
    @Override
    public Optional<Todo> findByIdAndUserId(UUID id, UUID userId) {
        return jpaRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
            .map(todoMapper::toDomainModel);
    }
    
    @Override
    public List<Todo> searchByTitle(UUID userId, String titleKeyword) {
        return jpaRepository.findByUserIdAndTitleContainingAndDeletedFalse(userId, titleKeyword).stream()
            .map(todoMapper::toDomainModel)
            .toList();
    }
    
    @Override
    public List<Todo> findByUserIdAndCompleted(UUID userId, boolean completed) {
        return jpaRepository.findByUserIdAndCompletedAndDeletedFalse(userId, completed).stream()
            .map(todoMapper::toDomainModel)
            .toList();
    }
    
    @Override
    public Todo save(Todo todo) {
        TodoJpaEntity entity = todoMapper.toJpaEntity(todo);
        TodoJpaEntity saved = jpaRepository.save(entity);
        return todoMapper.toDomainModel(saved);
    }
    
    @Override
    public void delete(UUID id) {
        // 論理削除の実装
        jpaRepository.findById(id).ifPresent(entity -> {
            entity.setDeleted(true);
            jpaRepository.save(entity);
        });
    }
    
    @Override
    public void deleteByUserId(UUID userId) {
        List<TodoJpaEntity> todos = jpaRepository.findByUserId(userId);
        todos.forEach(entity -> entity.setDeleted(true));
        jpaRepository.saveAll(todos);
    }
    
    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }
    
    @Override
    public boolean existsByIdAndUserId(UUID id, UUID userId) {
        return jpaRepository.existsByIdAndUserIdAndDeletedFalse(id, userId);
    }
    
    @Override
    public long countByUserId(UUID userId) {
        return jpaRepository.countByUserIdAndDeletedFalse(userId);
    }
    
    @Override
    public long countByUserIdAndCompleted(UUID userId, boolean completed) {
        return jpaRepository.countByUserIdAndCompletedAndDeletedFalse(userId, completed);
    }
}
```

---

## 🔄 データフロー - Repository Interfaceの役割

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
│  - Repository Interface使用（Domain層のインターフェース）      │
└────────────────────────────────────────────────────────────────┘
                            ↓
┌────────────────────────────────────────────────────────────────┐
│ Domain層                                                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Repository Interface ← ★ このパッケージ                  │  │
│  │  - findById(UUID id): Optional<Todo>                     │  │
│  │  - save(Todo todo): Todo                                 │  │
│  │  - delete(UUID id): void                                 │  │
│  └──────────────────────────────────────────────────────────┘  │
│                            ↑ implements                         │
└────────────────────────────────────────────────────────────────┘
                            ↑
┌────────────────────────────────────────────────────────────────┐
│ Infrastructure層 (Repository実装)                               │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ TodoRepositoryImpl                                       │  │
│  │  - Spring Data JPA使用                                   │  │
│  │  - JPA Entity ⇔ Domain Entity 変換                       │  │
│  │  - データベースアクセス                                  │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
                            ↓
┌────────────────────────────────────────────────────────────────┐
│ Database (PostgreSQL)                                           │
└────────────────────────────────────────────────────────────────┘
```

---

## 🧪 テスト戦略

### Repository Interface テスト（モックを使用）

**目的**: Application層のUseCaseをテストする際、Repository InterfaceをMockします。

**実装例**:
```java
package com.api.todos.application.usecase.todo;

import com.api.todos.application.dto.TodoResult;
import com.api.todos.domain.model.Todo;
import com.api.todos.domain.repository.TodoRepository;
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
 * GetTodoUseCase テスト
 * Repository InterfaceをMockして単体テスト
 */
@ExtendWith(MockitoExtension.class)
class GetTodoUseCaseTest {
    
    @Mock
    private TodoRepository todoRepository;  // ✅ Repository InterfaceをMock
    
    private GetTodoUseCase useCase;
    
    @BeforeEach
    void setUp() {
        useCase = new GetTodoUseCase(todoRepository);
    }
    
    @Test
    void TODOが存在する場合は正常に取得できること() {
        // Arrange
        UUID todoId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Todo todo = Todo.create("Test TODO", "Description", userId);
        
        // Mock設定
        when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
        
        // Act
        TodoResult result = useCase.execute(todoId);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test TODO");
        
        // Repository呼び出し確認
        verify(todoRepository).findById(todoId);
    }
    
    @Test
    void TODOが存在しない場合は例外をスローすること() {
        // Arrange
        UUID todoId = UUID.randomUUID();
        
        // Mock設定
        when(todoRepository.findById(todoId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(todoId))
            .isInstanceOf(TodoNotFoundException.class)
            .hasMessageContaining("TODO not found");
        
        verify(todoRepository).findById(todoId);
    }
}
```

```java
package com.api.todos.application.usecase.todo;

import com.api.todos.application.command.todo.CreateTodoCommand;
import com.api.todos.application.dto.TodoResult;
import com.api.todos.domain.model.Todo;
import com.api.todos.domain.model.User;
import com.api.todos.domain.model.UserRole;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.domain.repository.UserRepository;
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
 */
@ExtendWith(MockitoExtension.class)
class CreateTodoUseCaseTest {
    
    @Mock
    private TodoRepository todoRepository;
    
    @Mock
    private UserRepository userRepository;
    
    private CreateTodoUseCase useCase;
    
    @BeforeEach
    void setUp() {
        useCase = new CreateTodoUseCase(todoRepository, userRepository);
    }
    
    @Test
    void 正常にTODOを作成できること() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User user = User.create("testuser", "test@example.com", "Test", "User", UserRole.USER);
        CreateTodoCommand command = new CreateTodoCommand("Test TODO", "Description", userId);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        TodoResult result = useCase.execute(command);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test TODO");
        
        // Repository呼び出し確認
        verify(userRepository).findById(userId);
        
        // saveメソッドの引数を検証
        ArgumentCaptor<Todo> todoCaptor = ArgumentCaptor.forClass(Todo.class);
        verify(todoRepository).save(todoCaptor.capture());
        
        Todo savedTodo = todoCaptor.getValue();
        assertThat(savedTodo.getTitle()).isEqualTo("Test TODO");
        assertThat(savedTodo.getUserId()).isEqualTo(userId);
    }
    
    @Test
    void ユーザーが存在しない場合は例外をスローすること() {
        // Arrange
        UUID userId = UUID.randomUUID();
        CreateTodoCommand command = new CreateTodoCommand("Test TODO", "Description", userId);
        
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(UserNotFoundException.class);
        
        verify(userRepository).findById(userId);
        verify(todoRepository, never()).save(any());
    }
}
```

---

## ✅ 実装チェックリスト

### Repository Interface実装時

- [ ] **Pure Javaのinterface**として定義（実装を含まない）
- [ ] **Domain Entity**を型として使用（JPA Entityは使用しない）
- [ ] **Springアノテーション**を使用していない
- [ ] **Spring Data JPA**を継承していない
- [ ] **JavaDocコメント**を記述
- [ ] **ビジネス要件に基づいたメソッド**のみ定義

### メソッド設計

- [ ] メソッド名が**ビジネスの意図を明確に表現**している
- [ ] 引数・戻り値は**Domain Entity**を使用
- [ ] 戻り値で`Optional<T>`を適切に使用（存在しない可能性がある場合）
- [ ] 例外をスローする場合は**JavaDocに記載**

### 対応する他のコンポーネント

- [ ] **Domain層**: Entity（ビジネスロジック実装）が存在
- [ ] **Application層**: UseCase（Repository Interface使用）が存在
- [ ] **Infrastructure層**: Repository実装クラスが存在
- [ ] **Infrastructure層**: Mapper（Domain Entity ⇔ JPA Entity変換）が存在
- [ ] **テスト**: UseCaseテスト（Repository Mock使用）が実装済み

### 依存性逆転の原則チェック

- [ ] Repository InterfaceはDomain層に配置
- [ ] Repository実装はInfrastructure層に配置
- [ ] Application層はRepository Interfaceに依存（実装に依存しない）
- [ ] Infrastructure層がDomain層に依存（逆ではない）

---

## 📚 参考資料

### プロジェクト内ドキュメント
- **[AGENTS.md](../../../../../AGENTS.md)** - AIエージェント向けコンテキストドキュメント
- **[Domain層 README](../README.md)** - Domain層全体の概要
- **[Domain層 model README](../model/README.md)** - Entity実装パターン
- **[Infrastructure層 persistence README](../../infrastructure/persistence/README.md)** - Repository実装パターン

### 外部参考資料
- [Clean Architecture (Robert C. Martin)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Domain-Driven Design (Eric Evans)](https://www.domainlanguage.com/ddd/)
- [Dependency Inversion Principle](https://en.wikipedia.org/wiki/Dependency_inversion_principle)

---

## 🎯 まとめ

Repository Interfaceは、**データアクセスを抽象化し、依存性逆転の原則を実現する**ための重要なコンポーネントです。

### 重要ポイント

1. **Pure Java（最重要）**
   - Spring、JPA等のアノテーション使用禁止
   - Pure Javaのinterfaceのみ定義
   - 実装を含まない

2. **依存性逆転の原則**
   - Domain層でRepository Interface定義
   - Infrastructure層でRepository実装
   - Application層はInterfaceに依存

3. **Domain Entityを使用**
   - 引数・戻り値はDomain Entity
   - JPA Entity等のInfrastructure層の型は使用しない
   - 依存方向: 外側→内側のみ

4. **ビジネス要件に基づいた設計**
   - 不要なメソッドを定義しない
   - メソッド名でビジネスの意図を表現
   - JavaDocで仕様を明確化

### 禁止事項

- ❌ Repository InterfaceでSpring Data JPAを継承
- ❌ Repository InterfaceでJPA Entityを使用
- ❌ Repository Interfaceに実装を含める

このREADMEを参考に、**Pure Javaで定義された高品質なRepository Interface**を構築してください。Repository Interfaceは、クリーンアーキテクチャの核心である依存性逆転の原則を実現し、Domain層をInfrastructure層から独立させます。

---

**作成日**: 2026年1月5日  
**対象バージョン**: Spring Boot TODO API v0.0.1-SNAPSHOT  
**対象パッケージ**: `com.api.todos.domain.repository`
