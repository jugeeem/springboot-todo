# persistence パッケージ - データベース永続化層

Infrastructure層のpersistenceパッケージです。このパッケージは **データベースアクセスとデータ永続化** を担当します。

## 🎯 persistenceパッケージの役割

### 責務

1. **JPA Entity**: データベーステーブルとのマッピング（永続化専用）
2. **Spring Data JPA Repository**: JPA Entityの基本的なCRUD操作
3. **Repository実装**: Domain層のRepositoryインターフェースの実装
4. **Mapper**: Domain Model ⇔ JPA Entity の相互変換

### クリーンアーキテクチャにおける位置づけ

```
┌─────────────────────────────────────────────────┐
│  Presentation層                                  │
│  - Controllers                                   │
├─────────────────────────────────────────────────┤
│  Infrastructure層 - persistence/ ← ここ          │
│  - JPA Entity, Repository実装, Mapper          │
├─────────────────────────────────────────────────┤
│  Application層                                   │
│  - Use Cases (Pure Java)                        │
├─────────────────────────────────────────────────┤
│  Domain層                                        │
│  - Entity (Pure Java), Repository Interface    │
└─────────────────────────────────────────────────┘
```

**重要な設計原則**：
- **Domain層のEntityとJPA Entityは分離**
- **依存性逆転の原則**：Domain層でインターフェース定義、Infrastructure層で実装
- JPA EntityはInfrastructure層に配置（JPA依存OK）
- Domain EntityはPure Java（フレームワーク依存NG）

## 📁 パッケージ構成

```
persistence/
├── entity/                          # JPA Entity（永続化専用）
│   ├── TodoJpaEntity.java          # TODO JPA Entity
│   └── UserJpaEntity.java          # User JPA Entity
├── repository/                      # Repository実装
│   ├── TodoJpaRepository.java      # Spring Data JPA Repository（TODO）
│   ├── TodoRepositoryImpl.java     # TodoRepository実装
│   ├── UserJpaRepository.java      # Spring Data JPA Repository（User）
│   └── UserRepositoryImpl.java     # UserRepository実装
└── mapper/                          # Domain Model ⇔ JPA Entity 変換
    ├── TodoMapper.java             # Todo変換ロジック
    └── UserMapper.java             # User変換ロジック
```

## 🚨 絶対禁止事項

### ❌ 1. Domain層のEntityにJPAアノテーションを付与

```java
// ❌ 絶対禁止: Domain層のEntityでJPAアノテーションを使用
package com.api.todos.domain.model;

import jakarta.persistence.Entity;  // ❌ Domain層でJPA依存
import jakarta.persistence.Table;

@Entity  // ❌ Domain層でJPAアノテーション
@Table(name = "todos")
public class Todo {
    // Domain層はPure Javaであるべき
}

// ✅ 正しい実装: Domain層はPure Java
package com.api.todos.domain.model;

// フレームワーク依存なし（Pure Java）
public class Todo {
    private final UUID id;
    private String title;
    private String descriptions;
    private boolean completed;
    private final UUID userId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;
    
    // ビジネスルール実装
    public void markAsCompleted() {
        if (this.completed) {
            throw new IllegalStateException("既に完了済みのTODOです");
        }
        this.completed = true;
        this.updatedAt = LocalDateTime.now();
    }
}
```

### ❌ 2. JPA EntityをDomain Modelとして直接使用

```java
// ❌ 絶対禁止: JPA EntityをDomain Modelとして使用
@Service
public class CreateTodoService {
    @Transactional
    public TodoJpaEntity execute(CreateTodoCommand command) {
        // ❌ JPA EntityをDomain Modelとして扱う
        TodoJpaEntity entity = new TodoJpaEntity();
        entity.setTitle(command.getTitle());
        return jpaRepository.save(entity);
    }
}

// ✅ 正しい実装: Domain ModelとJPA Entityを分離
@Service
public class CreateTodoService {
    @Transactional
    public TodoResult execute(CreateTodoCommand command) {
        // ✅ Domain Modelで処理
        Todo todo = new Todo(command.getTitle(), command.getDescriptions(), command.getUserId());
        
        // Repository経由で保存（内部でJPA Entityに変換）
        Todo saved = todoRepository.save(todo);
        
        // Resultに変換して返却
        return TodoResult.from(saved);
    }
}
```

### ❌ 3. Repository実装でビジネスロジックを実装

```java
// ❌ 絶対禁止: Repository実装でビジネスロジックを実装
@Repository
public class TodoRepositoryImpl implements TodoRepository {
    @Override
    public Todo save(Todo todo) {
        // ❌ Repository実装でビジネスルール判定
        if (todo.getTitle() == null || todo.getTitle().isEmpty()) {
            throw new IllegalArgumentException("タイトルは必須です");
        }
        
        TodoJpaEntity entity = toJpaEntity(todo);
        TodoJpaEntity saved = jpaRepository.save(entity);
        return toDomainModel(saved);
    }
}

// ✅ 正しい実装: Repository実装はデータアクセスのみ
@Repository
public class TodoRepositoryImpl implements TodoRepository {
    @Override
    public Todo save(Todo todo) {
        // ✅ ビジネスルールは含めない（Domain層で実装済み）
        TodoJpaEntity entity = toJpaEntity(todo);
        TodoJpaEntity saved = jpaRepository.save(entity);
        return toDomainModel(saved);
    }
}

// ビジネスルールはDomain層のEntityで実装
public class Todo {
    public Todo(String title, String descriptions, UUID userId) {
        // ✅ ビジネスルール: タイトル必須チェック
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("タイトルは必須です");
        }
        this.title = title;
        // ...
    }
}
```

## ✅ 正しい実装パターン

### 1. JPA Entity（永続化専用）

```java
package com.api.todos.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;
import java.time.LocalDateTime;

/**
 * JPA用TODOエンティティ（永続化専用）
 *
 * <p>【重要】
 *
 * <ul>
 *   <li>Domain層のTodoエンティティとは分離
 *   <li>データベーステーブルとのマッピングのみを担当
 *   <li>ビジネスロジックは含めない
 *   <li>Lombokの使用OK（Infrastructure層）
 * </ul>
 *
 * <p>【データベース設計】
 *
 * <pre>
 * テーブル名: todos
 * スキーマ: public
 * 主キー: id (UUID)
 * 論理削除: deleted (boolean)
 * </pre>
 */
@Entity
@Table(name = "todos", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean deleted;

    /**
     * エンティティ作成時に自動実行
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    /**
     * エンティティ更新時に自動実行
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### 2. Spring Data JPA Repository

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
 * <p>【セキュリティ原則】
 *
 * <ul>
 *   <li>SQLインジェクション対策: パラメータバインディング（@Param）
 *   <li>文字列連結によるクエリ構築は禁止
 * </ul>
 */
@Repository
public interface TodoJpaRepository extends JpaRepository<TodoJpaEntity, UUID> {

    /**
     * ユーザーIDでTODOを検索（削除済み除外）
     *
     * <p>【重要】 文字列連結ではなく、パラメータバインディングを使用（SQLインジェクション対策）
     *
     * @param userId ユーザーID
     * @return TODO一覧
     */
    @Query("SELECT t FROM TodoJpaEntity t WHERE t.userId = :userId AND t.deleted = false")
    List<TodoJpaEntity> findByUserId(@Param("userId") UUID userId);

    /**
     * IDとユーザーIDでTODOを検索（削除済み除外）
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
     * <p>メソッド名からクエリを自動生成（Spring Data JPAの機能）
     *
     * @param userId ユーザーID
     * @param completed 完了状態
     * @return TODO一覧
     */
    List<TodoJpaEntity> findByUserIdAndCompletedAndDeletedFalse(UUID userId, boolean completed);

    /**
     * ユーザーIDとタイトルで検索（削除済み除外、LIKE検索）
     *
     * @param userId ユーザーID
     * @param titlePattern タイトル検索パターン（%を含む）
     * @return TODO一覧
     */
    @Query(
            "SELECT t FROM TodoJpaEntity t WHERE t.userId = :userId AND t.title LIKE :titlePattern"
                    + " AND t.deleted = false")
    List<TodoJpaEntity> searchByTitle(
            @Param("userId") UUID userId, @Param("titlePattern") String titlePattern);
}
```

### 3. Repository実装（Domain層のインターフェース実装）

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
 * <p>【依存性逆転の原則】
 *
 * <ul>
 *   <li>Domain層: TodoRepositoryインターフェース定義
 *   <li>Infrastructure層: TodoRepositoryImpl実装（この部分）
 *   <li>Domain層 ← Infrastructure層の依存方向
 * </ul>
 *
 * <p>【設計原則】
 *
 * <ul>
 *   <li>ビジネスロジックは含めない（Domain層で実装）
 *   <li>データアクセスのみに専念
 *   <li>Mapperで変換ロジックを分離
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
    public Todo save(Todo todo) {
        TodoJpaEntity entity = mapper.toJpaEntity(todo);
        TodoJpaEntity saved = jpaRepository.save(entity);
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

    @Override
    public void deleteByIdAndUserId(UUID id, UUID userId) {
        // 論理削除（ユーザーIDでフィルタリング）
        jpaRepository.findByIdAndUserId(id, userId).ifPresent(entity -> {
            entity.setDeleted(true);
            entity.setUpdatedAt(java.time.LocalDateTime.now());
            jpaRepository.save(entity);
        });
    }
}
```

### 4. Mapper（Domain Model ⇔ JPA Entity 変換）

```java
package com.api.todos.infrastructure.persistence.mapper;

import com.api.todos.domain.model.Todo;
import com.api.todos.infrastructure.persistence.entity.TodoJpaEntity;
import org.springframework.stereotype.Component;

/**
 * TodoMapper（Domain Model ⇔ JPA Entity 変換）
 *
 * <p>【責務】
 *
 * <ul>
 *   <li>Domain Model → JPA Entity の変換
 *   <li>JPA Entity → Domain Model の変換
 *   <li>変換ロジックの集約
 * </ul>
 *
 * <p>【設計原則】
 *
 * <ul>
 *   <li>変換ロジックをMapperに集約し、Repository実装をシンプルに保つ
 *   <li>Domain ModelとJPA Entityの構造が変更された場合、Mapperのみ修正
 *   <li>Nullチェック等の防御的プログラミング
 * </ul>
 */
@Component
public class TodoMapper {

    /**
     * JPA Entity → Domain Model 変換
     *
     * @param entity JPA Entity
     * @return Domain Model
     */
    public Todo toDomainModel(TodoJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Todo(
                entity.getId(),
                entity.getTitle(),
                entity.getDescriptions(),
                entity.isCompleted(),
                entity.getUserId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.isDeleted());
    }

    /**
     * Domain Model → JPA Entity 変換
     *
     * @param todo Domain Model
     * @return JPA Entity
     */
    public TodoJpaEntity toJpaEntity(Todo todo) {
        if (todo == null) {
            return null;
        }

        TodoJpaEntity entity = new TodoJpaEntity();
        entity.setId(todo.getId());
        entity.setTitle(todo.getTitle());
        entity.setDescriptions(todo.getDescriptions());
        entity.setCompleted(todo.isCompleted());
        entity.setUserId(todo.getUserId());
        entity.setCreatedAt(todo.getCreatedAt());
        entity.setUpdatedAt(todo.getUpdatedAt());
        entity.setDeleted(todo.isDeleted());

        return entity;
    }
}
```

## 🔄 データフローにおけるpersistenceパッケージの役割

```
【データ保存フロー】

1. Presentation層: Controller
   - リクエストDTO受信
   ↓
2. Infrastructure層: Service
   - @Transactional開始
   ↓
3. Application層: UseCase
   - Domain Model作成・操作
   ↓
4. Domain層: Repository Interface呼び出し
   ↓
5. Infrastructure層: Repository実装 ★ persistence/ ここ
   - Domain Model → JPA Entity変換（Mapper）
   - JPA Repositoryで保存
   - JPA Entity → Domain Model変換（Mapper）
   ↓
6. データベース: PostgreSQL
   - データ永続化
   ↓
7. Infrastructure層: Service
   - @Transactionalコミット
```

## 📊 依存関係まとめ

persistenceパッケージが依存できるもの：

- ✅ **Domain層**: Entity, Repository Interface
- ✅ **Spring Data JPA**: `JpaRepository`, `@Entity`, `@Table`, `@Query`等
- ✅ **PostgreSQL**: JDBCドライバー
- ✅ **Lombok**: `@Getter`, `@Setter`, `@NoArgsConstructor`等

persistenceパッケージが依存してはいけないもの：

- ❌ **Application層**: UseCase, Command, Result
- ❌ **Presentation層**: Controller, DTO
- ❌ **Infrastructure層の他のパッケージ**: Service, Security等

## 🔐 セキュリティベストプラクティス

### 1. SQLインジェクション対策

```java
// ✅ 正しい実装: パラメータバインディング
@Query("SELECT t FROM TodoJpaEntity t WHERE t.userId = :userId")
List<TodoJpaEntity> findByUserId(@Param("userId") UUID userId);

// ❌ 絶対禁止: 文字列連結
@Query("SELECT t FROM TodoJpaEntity t WHERE t.userId = '" + userId + "'")  // 脆弱性
List<TodoJpaEntity> findByUserId(UUID userId);
```

### 2. 論理削除の実装

```java
// ✅ 物理削除ではなく、論理削除を実装
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

### 3. @PrePersist / @PreUpdateの活用

```java
@Entity
public class TodoJpaEntity {
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

## 🧪 テスト戦略

### Repository統合テスト

```java
package com.api.todos.infrastructure.persistence.repository;

import com.api.todos.domain.model.Todo;
import com.api.todos.infrastructure.persistence.entity.TodoJpaEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

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
        TodoJpaEntity entity = new TodoJpaEntity();
        entity.setTitle("Test TODO");
        entity.setDescriptions("Test Description");
        entity.setCompleted(false);
        entity.setUserId(UUID.randomUUID());
        entity.setDeleted(false);
        TodoJpaEntity saved = entityManager.persistAndFlush(entity);

        // When: 検索
        Optional<Todo> result = repository.findById(saved.getId());

        // Then: 取得成功
        assertTrue(result.isPresent());
        assertEquals("Test TODO", result.get().getTitle());
    }

    @Test
    void findByUserId_ユーザーのTODO一覧を取得できる() {
        // Given: テストデータ投入
        UUID userId = UUID.randomUUID();
        
        TodoJpaEntity entity1 = createTestEntity("TODO 1", userId, false);
        TodoJpaEntity entity2 = createTestEntity("TODO 2", userId, false);
        entityManager.persistAndFlush(entity1);
        entityManager.persistAndFlush(entity2);

        // When: 検索
        List<Todo> results = repository.findByUserId(userId);

        // Then: 2件取得
        assertEquals(2, results.size());
    }

    @Test
    void save_新しいTODOを保存できる() {
        // Given: ドメインモデル作成
        UUID userId = UUID.randomUUID();
        Todo todo = new Todo(
            null,
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

        // DBに保存されているか確認
        TodoJpaEntity entity = entityManager.find(TodoJpaEntity.class, saved.getId());
        assertNotNull(entity);
        assertEquals("New TODO", entity.getTitle());
    }

    @Test
    void delete_論理削除が実行される() {
        // Given: テストデータ投入
        TodoJpaEntity entity = createTestEntity("Test TODO", UUID.randomUUID(), false);
        TodoJpaEntity saved = entityManager.persistAndFlush(entity);

        // When: 削除
        repository.delete(saved.getId());
        entityManager.flush();

        // Then: 論理削除フラグがtrueになっている
        TodoJpaEntity deleted = entityManager.find(TodoJpaEntity.class, saved.getId());
        assertTrue(deleted.isDeleted());
    }

    private TodoJpaEntity createTestEntity(String title, UUID userId, boolean deleted) {
        TodoJpaEntity entity = new TodoJpaEntity();
        entity.setTitle(title);
        entity.setDescriptions("Test Description");
        entity.setCompleted(false);
        entity.setUserId(userId);
        entity.setDeleted(deleted);
        return entity;
    }
}
```

### Mapperのテスト

```java
@ExtendWith(MockitoExtension.class)
class TodoMapperTest {

    private TodoMapper mapper = new TodoMapper();

    @Test
    void toDomainModel_JPA EntityをDomain Modelに変換できる() {
        // Given
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        TodoJpaEntity entity = new TodoJpaEntity(
            id, "Test", "Description", false, userId, now, now, false
        );

        // When
        Todo result = mapper.toDomainModel(entity);

        // Then
        assertEquals(id, result.getId());
        assertEquals("Test", result.getTitle());
        assertEquals("Description", result.getDescriptions());
    }

    @Test
    void toJpaEntity_Domain ModelをJPA Entityに変換できる() {
        // Given
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        Todo todo = new Todo(
            id, "Test", "Description", false, userId, now, now, false
        );

        // When
        TodoJpaEntity result = mapper.toJpaEntity(todo);

        // Then
        assertEquals(id, result.getId());
        assertEquals("Test", result.getTitle());
        assertEquals("Description", result.getDescriptions());
    }
}
```

## 📋 実装チェックリスト

新しいpersistenceコンポーネントを追加する際は、以下を確認してください：

### JPA Entity実装
- [ ] `@Entity`と`@Table`アノテーションを付与
- [ ] Domain層のEntityとは別クラスとして定義
- [ ] カラム名とマッピング（`@Column`）を正しく設定
- [ ] 主キーに`@Id`と`@GeneratedValue`を設定
- [ ] `@PrePersist`と`@PreUpdate`で作成日時・更新日時を自動設定
- [ ] Lombokを使用してボイラープレートコード削減
- [ ] ビジネスロジックは含めない（永続化のみ）

### Spring Data JPA Repository
- [ ] `JpaRepository`を継承
- [ ] カスタムクエリは`@Query`アノテーションで定義
- [ ] SQLインジェクション対策（`@Param`でパラメータバインディング）
- [ ] 論理削除を考慮したクエリ（`deleted = false`）

### Repository実装
- [ ] Domain層のRepositoryインターフェースを実装
- [ ] Spring Data JPA Repositoryを注入
- [ ] Mapperを注入して変換処理を委譲
- [ ] ビジネスロジックは実装しない（Domain層で実装）
- [ ] `@Repository`アノテーションを付与

### Mapper実装
- [ ] `@Component`アノテーションを付与
- [ ] `toDomainModel`メソッド実装（JPA Entity → Domain Model）
- [ ] `toJpaEntity`メソッド実装（Domain Model → JPA Entity）
- [ ] Nullチェック実装
- [ ] 全フィールドを適切にマッピング

### 禁止事項の確認
- [ ] Domain層のEntityにJPAアノテーションを付与していない
- [ ] JPA EntityをDomain Modelとして直接使用していない
- [ ] Repository実装でビジネスロジックを実装していない
- [ ] SQLインジェクション脆弱性がない

## 🔗 関連ドキュメント

- **[infrastructure README](../README.md)** - Infrastructure層全体の概要
- **[domain README](../../domain/README.md)** - Domain層のEntity/Repository Interface
- **[config README](../config/README.md)** - DatabaseConfig等の設定
- **[AGENTS.md](../../../../../../../../AGENTS.md)** - AIエージェント向けコンテキストドキュメント

## 💡 まとめ

Infrastructure層のpersistenceパッケージは **データベース永続化とデータアクセス** を担当します：

### ✅ persistenceパッケージの責務

1. **JPA Entity**: データベーステーブルとのマッピング（永続化専用）
2. **Spring Data JPA Repository**: 基本的なCRUD操作とカスタムクエリ
3. **Repository実装**: Domain層のインターフェース実装（依存性逆転）
4. **Mapper**: Domain Model ⇔ JPA Entityの相互変換

### ❌ persistenceパッケージでやってはいけないこと

1. **Domain層のEntityにJPAアノテーションを付与**（Pure Javaを保つ）
2. **JPA EntityをDomain Modelとして直接使用**（必ず分離）
3. **Repository実装でビジネスロジックを実装**（Domain層の責務）
4. **SQLインジェクション脆弱性**（パラメータバインディング必須）

### 🎯 設計の意図

この設計により：

- **Domain層をフレームワークから独立**させる（Pure Java）
- **依存性逆転の原則**を実現（Domain層でインターフェース定義）
- **データベーススキーマの変更がドメインロジックに影響しない**
- **テスタビリティの向上**（Repository実装を個別にテスト可能）

---

**作成日**: 2025年1月5日  
**対象バージョン**: Spring Boot TODO API v0.0.1-SNAPSHOT  

このドキュメントは、Infrastructure層のpersistenceパッケージの責務と実装パターンを説明するものです。**Domain ModelとJPA Entityを厳格に分離し、依存性逆転の原則を徹底する** という設計原則を守り、クリーンアーキテクチャの純粋性を保ってください。
