# repository パッケージ - リポジトリ実装層

Infrastructure層のpersistence/repositoryパッケージです。このパッケージは **データアクセスとリポジトリパターンの実装** を担当します。

## 🎯 repositoryパッケージの役割

### 責務

1. **Spring Data JPA Repository**: JPA Entityの基本的なCRUD操作とカスタムクエリ定義
2. **Repository実装クラス**: Domain層のRepositoryインターフェースを実装
3. **依存性逆転の実現**: Domain層でインターフェース定義、Infrastructure層で実装

### クリーンアーキテクチャにおける位置づけ

```
┌─────────────────────────────────────────────────┐
│  Presentation層                                  │
│  - Controllers                                   │
├─────────────────────────────────────────────────┤
│  Infrastructure層 - persistence/repository/ ← ここ│
│  - Spring Data JPA Repository                   │
│  - Repository実装（Domain Interfaceを実装）     │
├─────────────────────────────────────────────────┤
│  Application層                                   │
│  - Use Cases (Pure Java)                        │
├─────────────────────────────────────────────────┤
│  Domain層                                        │
│  - Repository Interface（抽象定義）             │
└─────────────────────────────────────────────────┘
```

**依存性逆転の原則（Dependency Inversion Principle）**：

```
Domain層: TodoRepository interface を定義
            ↑
            | implements
            |
Infrastructure層: TodoRepositoryImpl class を実装
```

**重要な設計原則**：
- **Domain層が外部に依存しない**：Domain層はインターフェースのみ定義（Pure Java）
- **Infrastructure層がDomain層に依存**：Domain層のインターフェースを実装
- **ビジネスロジックはDomain層**：Repository実装はデータアクセスのみ

## 📁 パッケージ構成

```
repository/
├── TodoJpaRepository.java          # Spring Data JPA Repository（TODO）
├── TodoRepositoryImpl.java         # TodoRepository実装
├── UserJpaRepository.java          # Spring Data JPA Repository（User）
└── UserRepositoryImpl.java         # UserRepository実装
```

## 🚨 絶対禁止事項

### ❌ 1. Repository実装でビジネスロジックを実装

```java
// ❌ 絶対禁止: Repository実装でビジネスルールを実装
package com.api.todos.infrastructure.persistence.repository;

@Repository
public class TodoRepositoryImpl implements TodoRepository {
    
    @Override
    public Todo save(Todo todo) {
        // ❌ ビジネスルール検証（Repository実装で実装してはいけない）
        if (todo.getTitle() == null || todo.getTitle().isEmpty()) {
            throw new IllegalArgumentException("タイトルは必須です");
        }
        
        if (todo.getTitle().length() > 32) {
            throw new IllegalArgumentException("タイトルは32文字以内です");
        }
        
        // ❌ ビジネスルール計算（Repository実装で実装してはいけない）
        if (todo.isCompleted() && todo.getCompletedAt() == null) {
            todo.setCompletedAt(LocalDateTime.now());
        }
        
        TodoJpaEntity entity = mapper.toJpaEntity(todo);
        TodoJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomainModel(saved);
    }
}

// ✅ 正しい実装: Repository実装はデータアクセスのみ
package com.api.todos.infrastructure.persistence.repository;

@Repository
public class TodoRepositoryImpl implements TodoRepository {
    
    @Override
    public Todo save(Todo todo) {
        // ✅ ビジネスルールは含めない（Domain層で実装済み）
        TodoJpaEntity entity = mapper.toJpaEntity(todo);
        TodoJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomainModel(saved);
    }
}

// ビジネスルールはDomain層のEntityで実装
package com.api.todos.domain.model;

public class Todo {
    public Todo(String title, String descriptions, UUID userId) {
        // ✅ ビジネスルール: タイトル必須チェック
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("タイトルは必須です");
        }
        
        // ✅ ビジネスルール: タイトル長さチェック
        if (title.length() > 32) {
            throw new IllegalArgumentException("タイトルは32文字以内です");
        }
        
        this.title = title;
        this.descriptions = descriptions;
        this.userId = userId;
        this.completed = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.deleted = false;
    }
    
    // ✅ ビジネスルール: TODO完了
    public void markAsCompleted() {
        if (this.completed) {
            throw new IllegalStateException("既に完了済みのTODOです");
        }
        this.completed = true;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
```

### ❌ 2. UseCaseでJPA Repositoryを直接使用

```java
// ❌ 絶対禁止: UseCaseでSpring Data JPA Repositoryを直接使用
package com.api.todos.application.usecase.todo;

import com.api.todos.infrastructure.persistence.repository.TodoJpaRepository;  // ❌ Infrastructure層に依存

public class CreateTodoUseCase {
    private final TodoJpaRepository jpaRepository;  // ❌ JPA Repositoryを直接使用
    
    public CreateTodoUseCase(TodoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    public TodoResult execute(CreateTodoCommand command) {
        // ❌ JPA Entityを直接操作（Domain Modelを使うべき）
        TodoJpaEntity entity = new TodoJpaEntity();
        entity.setTitle(command.getTitle());
        entity.setDescriptions(command.getDescriptions());
        entity.setUserId(command.getUserId());
        
        TodoJpaEntity saved = jpaRepository.save(entity);
        return TodoResult.from(saved);
    }
}

// ✅ 正しい実装: UseCaseはDomain層のRepositoryインターフェースを使用
package com.api.todos.application.usecase.todo;

import com.api.todos.domain.model.Todo;  // ✅ Domain Model
import com.api.todos.domain.repository.TodoRepository;  // ✅ Domain層のインターフェース

public class CreateTodoUseCase {
    private final TodoRepository todoRepository;  // ✅ Domain層のインターフェース
    private final UserRepository userRepository;
    
    public CreateTodoUseCase(TodoRepository todoRepository, UserRepository userRepository) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
    }
    
    public TodoResult execute(CreateTodoCommand command) {
        // ユーザーの存在確認
        userRepository.findById(command.getUserId())
                .orElseThrow(() -> new UserNotFoundException("ユーザーが見つかりません"));
        
        // ✅ Domain Modelを使用
        Todo todo = new Todo(
            command.getTitle(),
            command.getDescriptions(),
            command.getUserId()
        );
        
        // ✅ Domain層のRepositoryインターフェース経由で保存
        Todo saved = todoRepository.save(todo);
        
        // Resultに変換して返却
        return TodoResult.from(saved);
    }
}
```

### ❌ 3. SQLインジェクション脆弱性のあるクエリ

```java
// ❌ 絶対禁止: 文字列連結によるクエリ構築（SQLインジェクション脆弱性）
package com.api.todos.infrastructure.persistence.repository;

@Repository
public interface TodoJpaRepository extends JpaRepository<TodoJpaEntity, UUID> {
    
    // ❌ 文字列連結（SQLインジェクション脆弱性）
    @Query("SELECT t FROM TodoJpaEntity t WHERE t.userId = '" + userId + "'")
    List<TodoJpaEntity> findByUserId(UUID userId);
    
    // ❌ 動的クエリを文字列連結で構築（脆弱性）
    @Query("SELECT t FROM TodoJpaEntity t WHERE t.title LIKE '%" + searchText + "%'")
    List<TodoJpaEntity> searchByTitle(String searchText);
}

// ✅ 正しい実装: パラメータバインディング（SQLインジェクション対策）
package com.api.todos.infrastructure.persistence.repository;

@Repository
public interface TodoJpaRepository extends JpaRepository<TodoJpaEntity, UUID> {
    
    // ✅ @Paramでパラメータバインディング
    @Query("SELECT t FROM TodoJpaEntity t WHERE t.userId = :userId AND t.deleted = false")
    List<TodoJpaEntity> findByUserId(@Param("userId") UUID userId);
    
    // ✅ LIKE検索も@Paramでパラメータバインディング
    @Query("SELECT t FROM TodoJpaEntity t WHERE t.title LIKE :titlePattern AND t.deleted = false")
    List<TodoJpaEntity> searchByTitle(@Param("titlePattern") String titlePattern);
}
```

## ✅ 正しい実装パターン

### 1. Spring Data JPA Repository

```java
package com.api.todos.infrastructure.persistence.repository;

import com.api.todos.infrastructure.persistence.entity.TodoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository（TODO）
 *
 * <p>【責務】
 *
 * <ul>
 *   <li>JPA Entityの基本的なCRUD操作
 *   <li>Spring Data JPAの機能を活用したクエリ定義
 *   <li>カスタムクエリの実装（@Query）
 * </ul>
 *
 * <p>【設計原則】
 *
 * <ul>
 *   <li>SQLインジェクション対策: パラメータバインディング（@Param）必須
 *   <li>論理削除を考慮したクエリ（deleted = false）
 *   <li>メソッド命名規則に従ったクエリ自動生成（Spring Data JPA機能）
 * </ul>
 *
 * <p>【禁止事項】
 *
 * <ul>
 *   <li>文字列連結によるクエリ構築（SQLインジェクション脆弱性）
 *   <li>ビジネスロジックの実装（Domain層の責務）
 * </ul>
 */
@Repository
public interface TodoJpaRepository extends JpaRepository<TodoJpaEntity, UUID> {

    /**
     * ユーザーIDでTODOを検索（削除済み除外）
     *
     * <p>【SQLインジェクション対策】 文字列連結ではなく、@Paramでパラメータバインディング
     *
     * @param userId ユーザーID
     * @return TODO一覧
     */
    @Query("SELECT t FROM TodoJpaEntity t WHERE t.userId = :userId AND t.deleted = false")
    List<TodoJpaEntity> findByUserId(@Param("userId") UUID userId);

    /**
     * IDとユーザーIDでTODOを検索（削除済み除外）
     *
     * <p>【用途】 ユーザーが自分のTODOのみアクセスできるように制御
     *
     * @param id TODO ID
     * @param userId ユーザーID
     * @return TODO（存在しない場合はOptional.empty()）
     */
    @Query(
            "SELECT t FROM TodoJpaEntity t WHERE t.id = :id AND t.userId = :userId AND t.deleted"
                    + " = false")
    Optional<TodoJpaEntity> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    /**
     * 完了状態でフィルタリング（削除済み除外）
     *
     * <p>【メソッド命名規則】 Spring Data JPAのメソッド命名規則に従い、クエリを自動生成
     *
     * <ul>
     *   <li>findBy: SELECT
     *   <li>UserId: WHERE user_id = ?
     *   <li>And: AND
     *   <li>Completed: WHERE completed = ?
     *   <li>AndDeletedFalse: AND deleted = false
     * </ul>
     *
     * @param userId ユーザーID
     * @param completed 完了状態
     * @return TODO一覧
     */
    List<TodoJpaEntity> findByUserIdAndCompletedAndDeletedFalse(UUID userId, boolean completed);

    /**
     * ユーザーIDとタイトルで検索（削除済み除外、LIKE検索）
     *
     * <p>【LIKE検索】 titlePatternには '%検索文字列%' を渡す
     *
     * <p>例: searchByTitle(userId, "%買い物%")
     *
     * @param userId ユーザーID
     * @param titlePattern タイトル検索パターン（%を含む）
     * @return TODO一覧
     */
    @Query(
            "SELECT t FROM TodoJpaEntity t WHERE t.userId = :userId AND t.title LIKE :titlePattern"
                    + " AND t.deleted = false ORDER BY t.createdAt DESC")
    List<TodoJpaEntity> searchByTitle(
            @Param("userId") UUID userId, @Param("titlePattern") String titlePattern);

    /**
     * 作成日時の範囲でフィルタリング（削除済み除外）
     *
     * <p>【用途】 期間指定でTODOを検索
     *
     * @param userId ユーザーID
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @return TODO一覧
     */
    @Query(
            "SELECT t FROM TodoJpaEntity t WHERE t.userId = :userId AND t.createdAt BETWEEN"
                    + " :startDate AND :endDate AND t.deleted = false ORDER BY t.createdAt DESC")
    List<TodoJpaEntity> findByUserIdAndCreatedAtBetween(
            @Param("userId") UUID userId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * 削除済みを含むすべてのTODOを検索
     *
     * <p>【用途】 管理者機能やゴミ箱機能で使用
     *
     * @param userId ユーザーID
     * @return TODO一覧（削除済み含む）
     */
    @Query("SELECT t FROM TodoJpaEntity t WHERE t.userId = :userId ORDER BY t.createdAt DESC")
    List<TodoJpaEntity> findAllByUserId(@Param("userId") UUID userId);
}
```

### 2. Repository実装クラス（Domain層インターフェースの実装）

```java
package com.api.todos.infrastructure.persistence.repository;

import com.api.todos.domain.model.Todo;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.infrastructure.persistence.entity.TodoJpaEntity;
import com.api.todos.infrastructure.persistence.mapper.TodoMapper;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TodoRepositoryの実装
 *
 * <p>【責務】
 *
 * <ul>
 *   <li>Domain層のTodoRepositoryインターフェースを実装
 *   <li>JPA Entityとの永続化操作
 *   <li>Domain Model ⇔ JPA Entity の変換（Mapperを使用）
 * </ul>
 *
 * <p>【依存性逆転の原則（Dependency Inversion Principle）】
 *
 * <ul>
 *   <li>Domain層: TodoRepositoryインターフェース定義
 *   <li>Infrastructure層: TodoRepositoryImpl実装（この部分）
 *   <li>依存方向: Infrastructure層 → Domain層（逆転）
 * </ul>
 *
 * <p>【設計原則】
 *
 * <ul>
 *   <li>ビジネスロジックは含めない（Domain層で実装）
 *   <li>データアクセスのみに専念
 *   <li>Mapperで変換ロジックを分離
 *   <li>トランザクション管理はInfrastructure層のServiceで実施
 * </ul>
 *
 * <p>【禁止事項】
 *
 * <ul>
 *   <li>ビジネスルールの実装
 *   <li>バリデーション処理
 *   <li>複雑なロジックの実装
 * </ul>
 */
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
        return jpaRepository.findById(id).map(mapper::toDomainModel);
    }

    @Override
    public List<Todo> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Todo> findByIdAndUserId(UUID id, UUID userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(mapper::toDomainModel);
    }

    @Override
    public List<Todo> findByUserIdAndCompleted(UUID userId, boolean completed) {
        return jpaRepository.findByUserIdAndCompletedAndDeletedFalse(userId, completed).stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<Todo> searchByTitle(UUID userId, String titlePattern) {
        // LIKE検索のパターン作成（%を追加）
        String pattern = "%" + titlePattern + "%";
        return jpaRepository.searchByTitle(userId, pattern).stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public Todo save(Todo todo) {
        // Domain Model → JPA Entity 変換
        TodoJpaEntity entity = mapper.toJpaEntity(todo);

        // JPA Repositoryで保存
        TodoJpaEntity saved = jpaRepository.save(entity);

        // JPA Entity → Domain Model 変換
        return mapper.toDomainModel(saved);
    }

    @Override
    public void delete(UUID id) {
        // 論理削除（deleted フラグを true に更新）
        jpaRepository.findById(id).ifPresent(entity -> {
            entity.setDeleted(true);
            entity.setUpdatedAt(java.time.LocalDateTime.now());
            jpaRepository.save(entity);
        });
    }

    @Override
    public void deleteByIdAndUserId(UUID id, UUID userId) {
        // 論理削除（ユーザーIDでフィルタリング）
        jpaRepository.findByIdAndUserId(id, userId).ifPresent(entity -> {
            entity.setDeleted(true);
            entity.setUpdatedAt(java.time.LocalDateTime.now());
            jpaRepository.save(entity);
        });
    }

    @Override
    public void physicalDelete(UUID id) {
        // 物理削除（本当にデータベースから削除）
        // 注意: 通常は論理削除を使用し、物理削除は管理者機能等でのみ使用
        jpaRepository.deleteById(id);
    }

    @Override
    public List<Todo> findAll(UUID userId) {
        // 削除済みを含むすべてのTODOを取得
        return jpaRepository.findAllByUserId(userId).stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }
}
```

### 3. UserRepositoryの実装例

```java
package com.api.todos.infrastructure.persistence.repository;

import com.api.todos.domain.model.User;
import com.api.todos.domain.repository.UserRepository;
import com.api.todos.infrastructure.persistence.entity.UserJpaEntity;
import com.api.todos.infrastructure.persistence.mapper.UserMapper;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UserRepositoryの実装
 *
 * <p>【責務】
 *
 * <ul>
 *   <li>Domain層のUserRepositoryインターフェースを実装
 *   <li>ユーザーデータの永続化操作
 *   <li>認証・認可で使用するユーザー検索
 * </ul>
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserMapper mapper;

    public UserRepositoryImpl(UserJpaRepository jpaRepository, UserMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainModel);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(mapper::toDomainModel);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(mapper::toDomainModel);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public List<User> findAll() {
        return jpaRepository.findByDeletedFalse().stream()
                .map(mapper::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = mapper.toJpaEntity(user);
        UserJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomainModel(saved);
    }

    @Override
    public void delete(UUID id) {
        // 論理削除
        jpaRepository.findById(id).ifPresent(entity -> {
            entity.setDeleted(true);
            entity.setUpdatedAt(java.time.LocalDateTime.now());
            jpaRepository.save(entity);
        });
    }
}
```

### 4. Spring Data JPA Repository（User）

```java
package com.api.todos.infrastructure.persistence.repository;

import com.api.todos.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository（User）
 *
 * <p>【責務】
 *
 * <ul>
 *   <li>ユーザーデータの基本的なCRUD操作
 *   <li>ユーザー名・メールアドレスでの検索
 *   <li>認証・認可で使用するユーザー検索
 * </ul>
 */
@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    /**
     * ユーザー名で検索（削除済み除外）
     *
     * <p>【用途】 ログイン認証
     *
     * @param username ユーザー名
     * @return ユーザー（存在しない場合はOptional.empty()）
     */
    @Query("SELECT u FROM UserJpaEntity u WHERE u.username = :username AND u.deleted = false")
    Optional<UserJpaEntity> findByUsername(@Param("username") String username);

    /**
     * メールアドレスで検索（削除済み除外）
     *
     * <p>【用途】 ユーザー登録時の重複チェック、パスワードリセット
     *
     * @param email メールアドレス
     * @return ユーザー（存在しない場合はOptional.empty()）
     */
    @Query("SELECT u FROM UserJpaEntity u WHERE u.email = :email AND u.deleted = false")
    Optional<UserJpaEntity> findByEmail(@Param("email") String email);

    /**
     * ユーザー名の存在確認（削除済み除外）
     *
     * <p>【用途】 ユーザー登録時の重複チェック
     *
     * @param username ユーザー名
     * @return 存在する場合true
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM UserJpaEntity u WHERE"
            + " u.username = :username AND u.deleted = false")
    boolean existsByUsername(@Param("username") String username);

    /**
     * メールアドレスの存在確認（削除済み除外）
     *
     * <p>【用途】 ユーザー登録時の重複チェック
     *
     * @param email メールアドレス
     * @return 存在する場合true
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM UserJpaEntity u WHERE"
            + " u.email = :email AND u.deleted = false")
    boolean existsByEmail(@Param("email") String email);

    /**
     * 削除されていないユーザーをすべて取得
     *
     * <p>【用途】 ユーザー一覧表示（管理者機能）
     *
     * @return ユーザー一覧
     */
    @Query("SELECT u FROM UserJpaEntity u WHERE u.deleted = false ORDER BY u.createdAt DESC")
    List<UserJpaEntity> findByDeletedFalse();

    /**
     * ロールでフィルタリング（削除済み除外）
     *
     * <p>【用途】 ロール別のユーザー一覧表示
     *
     * @param role ロール（0: ADMIN, 1: MANAGER, 2: USER）
     * @return ユーザー一覧
     */
    @Query("SELECT u FROM UserJpaEntity u WHERE u.role = :role AND u.deleted = false ORDER BY"
            + " u.createdAt DESC")
    List<UserJpaEntity> findByRole(@Param("role") int role);
}
```

## 🔄 データフローにおけるrepositoryパッケージの役割

```
【データ保存フロー】

1. Presentation層: Controller
   - リクエストDTO受信
   ↓
2. Infrastructure層: Service
   - @Transactional開始
   ↓
3. Application層: UseCase（Pure Java）
   - Domain Model作成・操作
   ↓
4. Domain層: Repository Interface呼び出し
   ↓
5. Infrastructure層: Repository実装 ★ repository/ ここ
   ┌─────────────────────────────────┐
   │ TodoRepositoryImpl              │
   │ - Domain Model受け取り          │
   │ - Mapperで変換                  │
   │   (Domain Model → JPA Entity)   │
   │ - Spring Data JPA Repositoryで保存│
   │ - Mapperで変換                  │
   │   (JPA Entity → Domain Model)   │
   │ - Domain Model返却              │
   └─────────────────────────────────┘
   ↓
6. データベース: PostgreSQL
   - データ永続化
   ↓
7. Infrastructure層: Service
   - @Transactionalコミット
```

## 🔐 セキュリティベストプラクティス

### 1. SQLインジェクション対策

```java
// ✅ 正しい実装: パラメータバインディング
@Query("SELECT t FROM TodoJpaEntity t WHERE t.userId = :userId AND t.title LIKE :titlePattern")
List<TodoJpaEntity> searchByTitle(@Param("userId") UUID userId, @Param("titlePattern") String titlePattern);

// ❌ 絶対禁止: 文字列連結
@Query("SELECT t FROM TodoJpaEntity t WHERE t.userId = '" + userId + "' AND t.title LIKE '%" + searchText + "%'")
List<TodoJpaEntity> searchByTitle(UUID userId, String searchText);
```

### 2. 論理削除の実装

```java
// ✅ 論理削除を実装（物理削除は避ける）
@Override
public void delete(UUID id) {
    jpaRepository.findById(id).ifPresent(entity -> {
        entity.setDeleted(true);  // 論理削除フラグ
        entity.setUpdatedAt(LocalDateTime.now());
        jpaRepository.save(entity);
    });
}

// 検索時は削除済みを除外
@Query("SELECT t FROM TodoJpaEntity t WHERE t.userId = :userId AND t.deleted = false")
List<TodoJpaEntity> findByUserId(@Param("userId") UUID userId);
```

### 3. ユーザーIDによるアクセス制御

```java
// ✅ ユーザーが自分のデータのみアクセスできるように制御
@Query("SELECT t FROM TodoJpaEntity t WHERE t.id = :id AND t.userId = :userId AND t.deleted = false")
Optional<TodoJpaEntity> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

// UseCaseでの使用例
public TodoResult execute(GetTodoQuery query) {
    Todo todo = todoRepository.findByIdAndUserId(query.getTodoId(), query.getUserId())
            .orElseThrow(() -> new TodoNotFoundException("TODOが見つかりません"));
    return TodoResult.from(todo);
}
```

### 4. パスワードの取り扱い

```java
// ✅ パスワードはハッシュ化されたものをデータベースに保存
@Override
public User save(User user) {
    // ハッシュ化はDomain層またはApplication層で実施済み
    // Repository実装はデータアクセスのみ
    UserJpaEntity entity = mapper.toJpaEntity(user);
    UserJpaEntity saved = jpaRepository.save(entity);
    return mapper.toDomainModel(saved);
}

// ❌ Repository実装でパスワードをハッシュ化しない
// ビジネスルール（パスワードハッシュ化）はDomain層の責務
```

## 🧪 テスト戦略

### Repository統合テスト

```java
package com.api.todos.infrastructure.persistence.repository;

import com.api.todos.domain.model.Todo;
import com.api.todos.infrastructure.persistence.entity.TodoJpaEntity;
import com.api.todos.infrastructure.persistence.mapper.TodoMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import java.util.UUID;
import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TodoRepositoryImplの統合テスト
 *
 * <p>【テスト方針】
 *
 * <ul>
 *   <li>@DataJpaTestでJPA関連のみテスト（軽量）
 *   <li>TestEntityManagerでテストデータ投入
 *   <li>実際のDBアクセスを伴う統合テスト
 *   <li>Repository実装の動作検証
 * </ul>
 */
@DataJpaTest
@Import({TodoRepositoryImpl.class, TodoMapper.class})
class TodoRepositoryImplTest {

    @Autowired
    private TodoRepositoryImpl repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findById_存在するTODOを取得できる() {
        // Given: テストデータ投入
        UUID userId = UUID.randomUUID();
        TodoJpaEntity entity = createTestEntity("Test TODO", userId, false);
        TodoJpaEntity saved = entityManager.persistAndFlush(entity);

        // When: 検索
        Optional<Todo> result = repository.findById(saved.getId());

        // Then: 取得成功
        assertTrue(result.isPresent());
        assertEquals("Test TODO", result.get().getTitle());
        assertEquals(userId, result.get().getUserId());
    }

    @Test
    void findById_存在しないIDで検索するとOptional_emptyが返る() {
        // Given: 存在しないID
        UUID nonExistentId = UUID.randomUUID();

        // When: 検索
        Optional<Todo> result = repository.findById(nonExistentId);

        // Then: Optional.empty()
        assertFalse(result.isPresent());
    }

    @Test
    void findByUserId_ユーザーのTODO一覧を取得できる() {
        // Given: テストデータ投入
        UUID userId = UUID.randomUUID();

        TodoJpaEntity entity1 = createTestEntity("TODO 1", userId, false);
        TodoJpaEntity entity2 = createTestEntity("TODO 2", userId, false);
        TodoJpaEntity entity3 = createTestEntity("TODO 3", userId, true);  // 削除済み

        entityManager.persistAndFlush(entity1);
        entityManager.persistAndFlush(entity2);
        entityManager.persistAndFlush(entity3);

        // When: 検索
        List<Todo> results = repository.findByUserId(userId);

        // Then: 削除済みを除く2件取得
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(t -> t.getTitle().equals("TODO 1")));
        assertTrue(results.stream().anyMatch(t -> t.getTitle().equals("TODO 2")));
        assertFalse(results.stream().anyMatch(t -> t.getTitle().equals("TODO 3")));  // 削除済みは除外
    }

    @Test
    void findByIdAndUserId_自分のTODOを取得できる() {
        // Given: テストデータ投入
        UUID userId = UUID.randomUUID();
        TodoJpaEntity entity = createTestEntity("My TODO", userId, false);
        TodoJpaEntity saved = entityManager.persistAndFlush(entity);

        // When: 検索
        Optional<Todo> result = repository.findByIdAndUserId(saved.getId(), userId);

        // Then: 取得成功
        assertTrue(result.isPresent());
        assertEquals("My TODO", result.get().getTitle());
    }

    @Test
    void findByIdAndUserId_他人のTODOは取得できない() {
        // Given: テストデータ投入
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        TodoJpaEntity entity = createTestEntity("Other's TODO", userId, false);
        TodoJpaEntity saved = entityManager.persistAndFlush(entity);

        // When: 他人のIDで検索
        Optional<Todo> result = repository.findByIdAndUserId(saved.getId(), otherUserId);

        // Then: Optional.empty()
        assertFalse(result.isPresent());
    }

    @Test
    void save_新しいTODOを保存できる() {
        // Given: Domain Model作成
        UUID userId = UUID.randomUUID();
        Todo todo = new Todo(
                null,  // IDはnull（新規作成）
                "New TODO",
                "New Description",
                false,
                userId,
                null,
                null,
                false
        );

        // When: 保存
        Todo saved = repository.save(todo);

        // Then: 保存成功
        assertNotNull(saved.getId());
        assertEquals("New TODO", saved.getTitle());
        assertEquals("New Description", saved.getDescriptions());
        assertFalse(saved.isCompleted());
        assertFalse(saved.isDeleted());

        // DBに保存されているか確認
        TodoJpaEntity entity = entityManager.find(TodoJpaEntity.class, saved.getId());
        assertNotNull(entity);
        assertEquals("New TODO", entity.getTitle());
    }

    @Test
    void save_既存のTODOを更新できる() {
        // Given: テストデータ投入
        UUID userId = UUID.randomUUID();
        TodoJpaEntity entity = createTestEntity("Old Title", userId, false);
        TodoJpaEntity saved = entityManager.persistAndFlush(entity);

        // Domain Model取得
        Todo todo = repository.findById(saved.getId()).orElseThrow();

        // タイトルを変更（Domain Modelで）
        Todo updatedTodo = new Todo(
                todo.getId(),
                "New Title",  // タイトル変更
                todo.getDescriptions(),
                todo.isCompleted(),
                todo.getUserId(),
                todo.getCreatedAt(),
                LocalDateTime.now(),
                todo.isDeleted()
        );

        // When: 更新
        Todo result = repository.save(updatedTodo);

        // Then: 更新成功
        assertEquals("New Title", result.getTitle());

        // DBで確認
        entityManager.clear();
        TodoJpaEntity updated = entityManager.find(TodoJpaEntity.class, saved.getId());
        assertEquals("New Title", updated.getTitle());
    }

    @Test
    void delete_論理削除が実行される() {
        // Given: テストデータ投入
        UUID userId = UUID.randomUUID();
        TodoJpaEntity entity = createTestEntity("Test TODO", userId, false);
        TodoJpaEntity saved = entityManager.persistAndFlush(entity);

        // When: 削除
        repository.delete(saved.getId());
        entityManager.flush();
        entityManager.clear();

        // Then: 論理削除フラグがtrueになっている
        TodoJpaEntity deleted = entityManager.find(TodoJpaEntity.class, saved.getId());
        assertTrue(deleted.isDeleted());

        // findByIdでは取得できない（削除済み除外）
        Optional<Todo> result = repository.findById(saved.getId());
        assertFalse(result.isPresent());
    }

    @Test
    void deleteByIdAndUserId_自分のTODOのみ削除できる() {
        // Given: テストデータ投入
        UUID userId = UUID.randomUUID();
        TodoJpaEntity entity = createTestEntity("My TODO", userId, false);
        TodoJpaEntity saved = entityManager.persistAndFlush(entity);

        // When: 削除
        repository.deleteByIdAndUserId(saved.getId(), userId);
        entityManager.flush();
        entityManager.clear();

        // Then: 論理削除フラグがtrueになっている
        TodoJpaEntity deleted = entityManager.find(TodoJpaEntity.class, saved.getId());
        assertTrue(deleted.isDeleted());
    }

    @Test
    void deleteByIdAndUserId_他人のTODOは削除できない() {
        // Given: テストデータ投入
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        TodoJpaEntity entity = createTestEntity("Other's TODO", userId, false);
        TodoJpaEntity saved = entityManager.persistAndFlush(entity);

        // When: 他人のIDで削除試行
        repository.deleteByIdAndUserId(saved.getId(), otherUserId);
        entityManager.flush();
        entityManager.clear();

        // Then: 削除されていない
        TodoJpaEntity notDeleted = entityManager.find(TodoJpaEntity.class, saved.getId());
        assertFalse(notDeleted.isDeleted());
    }

    @Test
    void searchByTitle_タイトルで部分一致検索できる() {
        // Given: テストデータ投入
        UUID userId = UUID.randomUUID();
        entityManager.persistAndFlush(createTestEntity("買い物リスト", userId, false));
        entityManager.persistAndFlush(createTestEntity("買い物メモ", userId, false));
        entityManager.persistAndFlush(createTestEntity("仕事のタスク", userId, false));

        // When: "買い物"で検索
        List<Todo> results = repository.searchByTitle(userId, "買い物");

        // Then: 2件取得
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(t -> t.getTitle().contains("買い物")));
    }

    private TodoJpaEntity createTestEntity(String title, UUID userId, boolean deleted) {
        TodoJpaEntity entity = new TodoJpaEntity();
        entity.setTitle(title);
        entity.setDescriptions("Test Description");
        entity.setCompleted(false);
        entity.setUserId(userId);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setDeleted(deleted);
        return entity;
    }
}
```

### Spring Data JPA Repositoryのテスト

```java
@DataJpaTest
class TodoJpaRepositoryTest {

    @Autowired
    private TodoJpaRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByUserId_SQLインジェクション対策が機能する() {
        // Given: 悪意のあるユーザーIDを想定
        UUID userId = UUID.randomUUID();

        // When: 検索（パラメータバインディングで安全に実行される）
        List<TodoJpaEntity> results = repository.findByUserId(userId);

        // Then: SQLインジェクションは発生しない
        assertNotNull(results);
    }

    @Test
    void findByUserIdAndCompletedAndDeletedFalse_メソッド命名規則でクエリ生成() {
        // Given: テストデータ
        UUID userId = UUID.randomUUID();
        TodoJpaEntity completed = createEntity("Completed", userId, true, false);
        TodoJpaEntity incomplete = createEntity("Incomplete", userId, false, false);
        TodoJpaEntity deleted = createEntity("Deleted", userId, false, true);

        entityManager.persistAndFlush(completed);
        entityManager.persistAndFlush(incomplete);
        entityManager.persistAndFlush(deleted);

        // When: 未完了のみ検索
        List<TodoJpaEntity> results = repository.findByUserIdAndCompletedAndDeletedFalse(userId, false);

        // Then: 未完了かつ削除されていないもののみ
        assertEquals(1, results.size());
        assertEquals("Incomplete", results.get(0).getTitle());
    }

    private TodoJpaEntity createEntity(String title, UUID userId, boolean completed, boolean deleted) {
        TodoJpaEntity entity = new TodoJpaEntity();
        entity.setTitle(title);
        entity.setDescriptions("Test");
        entity.setCompleted(completed);
        entity.setUserId(userId);
        entity.setDeleted(deleted);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }
}
```

## 📋 実装チェックリスト

新しいRepositoryを追加する際は、以下を確認してください：

### Spring Data JPA Repository
- [ ] `JpaRepository<Entity, ID>`を継承
- [ ] カスタムクエリは`@Query`アノテーションで定義
- [ ] SQLインジェクション対策（`@Param`でパラメータバインディング）
- [ ] 論理削除を考慮したクエリ（`deleted = false`）
- [ ] メソッド命名規則に従ったクエリ自動生成（Spring Data JPA機能）
- [ ] `@Repository`アノテーションを付与

### Repository実装クラス
- [ ] Domain層のRepositoryインターフェースを実装
- [ ] Spring Data JPA Repositoryを注入
- [ ] Mapperを注入して変換処理を委譲
- [ ] ビジネスロジックは実装しない（Domain層で実装）
- [ ] データアクセスのみに専念
- [ ] `@Repository`アノテーションを付与
- [ ] 論理削除の実装（物理削除は避ける）

### テスト実装
- [ ] `@DataJpaTest`で統合テスト
- [ ] `TestEntityManager`でテストデータ投入
- [ ] 正常系・異常系の両方をテスト
- [ ] SQLインジェクション対策のテスト
- [ ] 論理削除の動作確認
- [ ] ユーザーIDによるアクセス制御の確認

### 禁止事項の確認
- [ ] Repository実装でビジネスロジックを実装していない
- [ ] UseCaseでJPA Repositoryを直接使用していない
- [ ] 文字列連結によるクエリ構築をしていない
- [ ] Domain層のEntityにJPAアノテーションを付与していない

## 🔗 関連ドキュメント

- **[persistence README](../README.md)** - persistence層全体の概要
- **[domain/repository README](../../../domain/repository/README.md)** - Domain層のRepositoryインターフェース
- **[persistence/entity README](../entity/README.md)** - JPA Entity
- **[persistence/mapper README](../mapper/README.md)** - Domain Model ⇔ JPA Entity 変換
- **[AGENTS.md](../../../../../../../../../AGENTS.md)** - AIエージェント向けコンテキストドキュメント

## 💡 まとめ

Infrastructure層のpersistence/repositoryパッケージは **データアクセスとリポジトリパターンの実装** を担当します：

### ✅ repositoryパッケージの責務

1. **Spring Data JPA Repository**: JPA Entityの基本的なCRUD操作とカスタムクエリ定義
2. **Repository実装クラス**: Domain層のRepositoryインターフェースを実装
3. **依存性逆転の実現**: Domain層でインターフェース定義、Infrastructure層で実装

### ❌ repositoryパッケージでやってはいけないこと

1. **Repository実装でビジネスロジックを実装**（Domain層の責務）
2. **UseCaseでJPA Repositoryを直接使用**（依存性逆転の原則違反）
3. **文字列連結によるクエリ構築**（SQLインジェクション脆弱性）

### 🎯 設計の意図

この設計により：

- **依存性逆転の原則**を実現（Domain層がInfrastructure層に依存しない）
- **Domain層をフレームワークから独立**させる（Pure Java）
- **テスタビリティの向上**（Repository実装を個別にテスト可能）
- **ビジネスロジックとデータアクセスの分離**（関心の分離）
- **データベーススキーマの変更がドメインロジックに影響しない**

---

**作成日**: 2025年1月5日  
**対象バージョン**: Spring Boot TODO API v0.0.1-SNAPSHOT  

このドキュメントは、Infrastructure層のpersistence/repositoryパッケージの責務と実装パターンを説明するものです。**依存性逆転の原則を厳格に守り、Domain層をフレームワークから独立させる** という設計原則を徹底してください。
