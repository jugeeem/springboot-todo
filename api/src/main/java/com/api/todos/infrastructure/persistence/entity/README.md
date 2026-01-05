# entity パッケージ - JPA Entity層

Infrastructure層のpersistence/entityパッケージです。このパッケージは **データベーステーブルとJPAエンティティのマッピング** を担当します。

## 🎯 entityパッケージの役割

### 責務

1. **データベーステーブルとのマッピング**: JPA/Hibernateアノテーションでマッピング
2. **永続化専用エンティティ**: データベースへの保存・取得のみに使用
3. **テーブル構造の定義**: カラム名、制約、インデックス等の定義

### クリーンアーキテクチャにおける位置づけ

```
┌─────────────────────────────────────────────────┐
│  Presentation層                                  │
│  - Controllers                                   │
├─────────────────────────────────────────────────┤
│  Infrastructure層 - persistence/entity/ ← ここ   │
│  - JPA Entity（永続化専用）                     │
│  - データベーステーブルとのマッピング           │
├─────────────────────────────────────────────────┤
│  Application層                                   │
│  - Use Cases (Pure Java)                        │
├─────────────────────────────────────────────────┤
│  Domain層                                        │
│  - Entity (Pure Java, ビジネスロジック含む)    │
└─────────────────────────────────────────────────┘
```

**Domain層のEntity vs Infrastructure層のJPA Entity**：

| 項目 | Domain Entity | JPA Entity |
|------|--------------|------------|
| 場所 | `domain/model/` | `infrastructure/persistence/entity/` |
| 目的 | ビジネスロジック実装 | データベース永続化 |
| 依存関係 | Pure Java（フレームワーク依存なし） | JPA、Hibernate依存OK |
| アノテーション | なし | `@Entity`, `@Table`, `@Column`等 |
| Lombok | 使用しない（推奨） | 使用OK |
| ビジネスルール | 含む | 含めない |

**重要な設計原則**：
- **Domain EntityとJPA Entityは別クラス**として定義
- **JPA EntityはInfrastructure層に配置**（Domain層には配置しない）
- **ビジネスロジックはDomain Entityに実装**（JPA Entityには実装しない）
- **MapperでDomain Entity ⇔ JPA Entity変換**

## 📁 パッケージ構成

```
entity/
├── TodoJpaEntity.java          # TODO JPA Entity
└── UserJpaEntity.java          # User JPA Entity
```

## 🚨 絶対禁止事項

### ❌ 1. Domain層のEntityにJPAアノテーションを付与

```java
// ❌ 絶対禁止: Domain層のEntityでJPAアノテーションを使用
package com.api.todos.domain.model;

import jakarta.persistence.Entity;  // ❌ Domain層でJPA依存
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

@Entity  // ❌ Domain層でJPAアノテーション
@Table(name = "todos")
public class Todo {
    @Id  // ❌ Domain層でJPAアノテーション
    private UUID id;
    
    @Column(name = "title")  // ❌ Domain層でJPAアノテーション
    private String title;
    
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
    
    // コンストラクタ
    public Todo(String title, String descriptions, UUID userId) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.descriptions = descriptions;
        this.userId = userId;
        this.completed = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.deleted = false;
    }
    
    // ✅ ビジネスルールはDomain Entityに実装
    public void markAsCompleted() {
        if (this.completed) {
            throw new IllegalStateException("既に完了済みのTODOです");
        }
        this.completed = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    // getter, equals, hashCode
}

// ✅ 正しい実装: JPA EntityはInfrastructure層に配置
package com.api.todos.infrastructure.persistence.entity;

import jakarta.persistence.*;  // ✅ Infrastructure層でJPA依存OK

@Entity
@Table(name = "todos", schema = "public")
public class TodoJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, length = 32)
    private String title;
    
    // 永続化専用（ビジネスロジックなし）
}
```

### ❌ 2. JPA Entityをドメインモデルとして直接使用

```java
// ❌ 絶対禁止: JPA EntityをDomain Modelとして使用
package com.api.todos.application.usecase.todo;

import com.api.todos.infrastructure.persistence.entity.TodoJpaEntity;  // ❌ Infrastructure層に依存

public class CreateTodoUseCase {
    private final TodoJpaRepository jpaRepository;
    
    public TodoResult execute(CreateTodoCommand command) {
        // ❌ JPA EntityをDomain Modelとして扱う
        TodoJpaEntity entity = new TodoJpaEntity();
        entity.setTitle(command.getTitle());
        entity.setDescriptions(command.getDescriptions());
        entity.setUserId(command.getUserId());
        
        // ❌ ビジネスロジックをUseCaseで実装（本来はDomain Entityの責務）
        if (entity.getTitle() == null || entity.getTitle().isEmpty()) {
            throw new IllegalArgumentException("タイトルは必須です");
        }
        
        TodoJpaEntity saved = jpaRepository.save(entity);
        return TodoResult.from(saved);
    }
}

// ✅ 正しい実装: UseCaseはDomain Modelを使用
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
        
        // ✅ Domain Modelを使用（ビジネスルールはコンストラクタで検証）
        Todo todo = new Todo(
            command.getTitle(),
            command.getDescriptions(),
            command.getUserId()
        );
        
        // ✅ Domain層のRepositoryインターフェース経由で保存
        // （内部でMapperがDomain Model → JPA Entityに変換）
        Todo saved = todoRepository.save(todo);
        
        // Resultに変換して返却
        return TodoResult.from(saved);
    }
}
```

### ❌ 3. JPA Entityにビジネスロジックを実装

```java
// ❌ 絶対禁止: JPA Entityでビジネスロジックを実装
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
    
    @Column(nullable = false)
    private boolean completed;
    
    // ❌ JPA Entityでビジネスルールを実装（Domain Entityの責務）
    public void markAsCompleted() {
        if (this.completed) {
            throw new IllegalStateException("既に完了済みのTODOです");
        }
        this.completed = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    // ❌ JPA Entityでビジネスルール検証（Domain Entityの責務）
    public void setTitle(String title) {
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("タイトルは必須です");
        }
        if (title.length() > 32) {
            throw new IllegalArgumentException("タイトルは32文字以内です");
        }
        this.title = title;
    }
}

// ✅ 正しい実装: JPA Entityは永続化のみ
package com.api.todos.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;
import java.time.LocalDateTime;

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
    
    @Column(nullable = false)
    private boolean completed;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // ✅ ビジネスロジックは含めない（永続化のみ）
    // ✅ @PrePersist/@PreUpdateで自動タイムスタンプ更新はOK
    
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

// ✅ ビジネスルールはDomain Entityに実装
package com.api.todos.domain.model;

public class Todo {
    private final UUID id;
    private String title;
    private boolean completed;
    
    // ✅ ビジネスルール: TODO完了
    public void markAsCompleted() {
        if (this.completed) {
            throw new IllegalStateException("既に完了済みのTODOです");
        }
        this.completed = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    // ✅ ビジネスルール: タイトル検証はコンストラクタで実施
    public Todo(String title, String descriptions, UUID userId) {
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("タイトルは必須です");
        }
        if (title.length() > 32) {
            throw new IllegalArgumentException("タイトルは32文字以内です");
        }
        this.title = title;
        // ...
    }
}
```

## ✅ 正しい実装パターン

### 1. TodoJpaEntity（完全な実装例）

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
 *   <li>ビジネスロジックは含めない（Domain層の責務）
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
 * インデックス: user_id, created_at
 * </pre>
 *
 * <p>【対応するDomain Entity】
 *
 * <ul>
 *   <li>com.api.todos.domain.model.Todo（Pure Java、ビジネスロジック含む）
 * </ul>
 *
 * <p>【Mapperによる変換】
 *
 * <ul>
 *   <li>Domain Model ⇔ JPA Entity の変換はTodoMapperで実施
 * </ul>
 */
@Entity
@Table(name = "todos", schema = "public", indexes = {
    @Index(name = "idx_todos_user_id", columnList = "user_id"),
    @Index(name = "idx_todos_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TodoJpaEntity {

    /**
     * TODO ID（主キー）
     * UUIDで自動生成
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * タイトル（必須、最大32文字）
     */
    @Column(nullable = false, length = 32)
    private String title;

    /**
     * 説明（最大128文字）
     */
    @Column(length = 128)
    private String descriptions;

    /**
     * 完了状態（デフォルト: false）
     */
    @Column(nullable = false)
    private boolean completed;

    /**
     * ユーザーID（外部キー）
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * 作成日時（自動設定、更新不可）
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新日時（自動更新）
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 論理削除フラグ（デフォルト: false）
     */
    @Column(nullable = false)
    private boolean deleted;

    /**
     * エンティティ作成時に自動実行
     * 作成日時・更新日時を自動設定
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
     * 更新日時を自動更新
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### 2. UserJpaEntity（完全な実装例）

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
 * JPA用ユーザーエンティティ（永続化専用）
 *
 * <p>【重要】
 *
 * <ul>
 *   <li>Domain層のUserエンティティとは分離
 *   <li>認証・認可で使用するユーザー情報を永続化
 *   <li>ビジネスロジックは含めない（Domain層の責務）
 * </ul>
 *
 * <p>【データベース設計】
 *
 * <pre>
 * テーブル名: users
 * スキーマ: public
 * 主キー: id (UUID)
 * 一意制約: username, email
 * 論理削除: deleted (boolean)
 * インデックス: username, email
 * </pre>
 *
 * <p>【ロール定義】
 *
 * <ul>
 *   <li>0: ADMIN（管理者）
 *   <li>1: MANAGER（マネージャー）
 *   <li>2: USER（一般ユーザー）
 * </ul>
 *
 * <p>【セキュリティ】
 *
 * <ul>
 *   <li>hashedPassword: BCryptでハッシュ化されたパスワード
 *   <li>passwordInitialized: 初期パスワード変更済みフラグ
 * </ul>
 */
@Entity
@Table(name = "users", schema = "public", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_users_email", columnNames = "email")
    },
    indexes = {
        @Index(name = "idx_users_username", columnList = "username"),
        @Index(name = "idx_users_email", columnList = "email")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserJpaEntity {

    /**
     * ユーザーID（主キー）
     * UUIDで自動生成
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * ユーザー名（必須、一意、最大16文字）
     */
    @Column(nullable = false, unique = true, length = 16)
    private String username;

    /**
     * メールアドレス（必須、一意、最大128文字）
     */
    @Column(nullable = false, unique = true, length = 128)
    private String email;

    /**
     * ハッシュ化されたパスワード（必須、最大256文字）
     * BCryptでハッシュ化されたパスワードを格納
     */
    @Column(name = "hashed_password", nullable = false, length = 256)
    private String hashedPassword;

    /**
     * 名（最大32文字）
     */
    @Column(name = "first_name", length = 32)
    private String firstName;

    /**
     * 姓（最大32文字）
     */
    @Column(name = "last_name", length = 32)
    private String lastName;

    /**
     * ロール（必須）
     * 0: ADMIN, 1: MANAGER, 2: USER
     */
    @Column(nullable = false)
    private int role;

    /**
     * パスワード初期化済みフラグ（デフォルト: false）
     * 初期パスワードから変更済みかどうか
     */
    @Column(name = "password_initialized", nullable = false)
    private boolean passwordInitialized;

    /**
     * 作成日時（自動設定、更新不可）
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新日時（自動更新）
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 論理削除フラグ（デフォルト: false）
     */
    @Column(nullable = false)
    private boolean deleted;

    /**
     * エンティティ作成時に自動実行
     * 作成日時・更新日時を自動設定
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
     * 更新日時を自動更新
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### 3. @PrePersist / @PreUpdateの活用

```java
/**
 * @PrePersist / @PreUpdateを活用した自動タイムスタンプ更新
 *
 * <p>【メリット】
 *
 * <ul>
 *   <li>作成日時・更新日時を自動設定（手動設定不要）
 *   <li>更新漏れを防止
 *   <li>一貫性のあるタイムスタンプ管理
 * </ul>
 *
 * <p>【注意事項】
 *
 * <ul>
 *   <li>@PrePersist: INSERT前に実行
 *   <li>@PreUpdate: UPDATE前に実行
 *   <li>メソッド名は任意（protected推奨）
 * </ul>
 */
@Entity
@Table(name = "todos")
public class TodoJpaEntity {
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * INSERT前に自動実行
     * 作成日時・更新日時を設定
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
     * UPDATE前に自動実行
     * 更新日時のみ更新（作成日時は不変）
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### 4. 論理削除の実装パターン

```java
/**
 * 論理削除パターン
 *
 * <p>【論理削除とは】
 *
 * <ul>
 *   <li>物理削除（DELETEクエリ）ではなく、deletedフラグをtrueに更新
 *   <li>データは実際には削除されず、論理的に削除済みとして扱う
 *   <li>監査証跡、復元機能、データ保護の観点で推奨
 * </ul>
 *
 * <p>【実装方法】
 *
 * <ul>
 *   <li>deletedカラム（boolean）を追加
 *   <li>検索クエリで `deleted = false` を条件に追加
 *   <li>削除時はUPDATEクエリで `deleted = true` に更新
 * </ul>
 */
@Entity
@Table(name = "todos")
@Getter
@Setter
@NoArgsConstructor
public class TodoJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, length = 32)
    private String title;
    
    /**
     * 論理削除フラグ
     * true: 削除済み（論理削除）
     * false: 有効（デフォルト）
     */
    @Column(nullable = false)
    private boolean deleted = false;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

// Repository実装での論理削除
@Repository
public class TodoRepositoryImpl implements TodoRepository {
    @Override
    public void delete(UUID id) {
        jpaRepository.findById(id).ifPresent(entity -> {
            // 物理削除ではなく、論理削除フラグを更新
            entity.setDeleted(true);
            entity.setUpdatedAt(LocalDateTime.now());
            jpaRepository.save(entity);
        });
    }
}

// Spring Data JPA Repositoryでの検索（削除済み除外）
@Repository
public interface TodoJpaRepository extends JpaRepository<TodoJpaEntity, UUID> {
    /**
     * ユーザーIDで検索（削除済み除外）
     */
    @Query("SELECT t FROM TodoJpaEntity t WHERE t.userId = :userId AND t.deleted = false")
    List<TodoJpaEntity> findByUserId(@Param("userId") UUID userId);
}
```

## 🔄 データフローにおけるentityパッケージの役割

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
5. Infrastructure層: Repository実装
   - Domain Model受け取り
   ↓
6. Infrastructure層: Mapper
   - Domain Model → JPA Entity変換 ★ entity/ ここ
   ↓
7. Infrastructure層: Spring Data JPA Repository
   - JPA Entity保存
   ↓
8. データベース: PostgreSQL
   - JPA Entity → テーブル永続化 ★ entity/ ここ
   ↓
9. Infrastructure層: Mapper
   - JPA Entity → Domain Model変換 ★ entity/ ここ
   ↓
10. Infrastructure層: Service
   - @Transactionalコミット
```

**JPA Entityの役割**：
1. **Domain Model → JPA Entity**: Mapperで変換（保存前）
2. **JPA Entity → Database**: JPAでテーブル永続化
3. **Database → JPA Entity**: JPAでテーブル取得
4. **JPA Entity → Domain Model**: Mapperで変換（取得後）

## 🔐 セキュリティベストプラクティス

### 1. パスワードの取り扱い

```java
/**
 * パスワードは必ずハッシュ化して保存
 *
 * <p>【重要】
 *
 * <ul>
 *   <li>平文パスワードは絶対に保存しない
 *   <li>BCryptでハッシュ化されたパスワードを保存
 *   <li>ハッシュ化はDomain層またはApplication層で実施
 *   <li>JPA EntityにはハッシュパスワードのみSetterで渡す
 * </ul>
 */
@Entity
@Table(name = "users")
public class UserJpaEntity {
    /**
     * ハッシュ化されたパスワード（平文パスワードではない）
     */
    @Column(name = "hashed_password", nullable = false, length = 256)
    private String hashedPassword;  // ✅ BCryptでハッシュ化済み
    
    // ❌ 平文パスワードのカラムは作成しない
    // @Column(name = "password")
    // private String password;  // 絶対禁止
}

// ハッシュ化はInfrastructure層のSecurityで実施
@Component
public class PasswordEncoderConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

// Application層のUseCaseでハッシュ化してから保存
public class RegisterUserUseCase {
    private final PasswordEncoder passwordEncoder;
    
    public UserResult execute(RegisterUserCommand command) {
        // ✅ パスワードをハッシュ化
        String hashedPassword = passwordEncoder.encode(command.getPassword());
        
        // ✅ ハッシュ化されたパスワードをDomain Modelに渡す
        User user = new User(
            command.getUsername(),
            command.getEmail(),
            hashedPassword  // ハッシュ化済み
        );
        
        // Repository経由で保存（JPA Entityにもハッシュ化済みが渡される）
        User saved = userRepository.save(user);
        return UserResult.from(saved);
    }
}
```

### 2. 機密情報のログ出力除外

```java
/**
 * 機密情報をログに出力しない
 *
 * <p>【注意事項】
 *
 * <ul>
 *   <li>toString()メソッドでパスワードを含めない
 *   <li>Lombokの@ToString(exclude = {"hashedPassword"})を使用
 *   <li>ログ出力時にパスワードフィールドを除外
 * </ul>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"hashedPassword"})  // ✅ パスワードをtoString()から除外
public class UserJpaEntity {
    @Id
    private UUID id;
    
    @Column(nullable = false)
    private String username;
    
    /**
     * ハッシュ化されたパスワード
     * toString()から除外される
     */
    @Column(name = "hashed_password", nullable = false)
    private String hashedPassword;
}

// ログ出力例
log.info("User: {}", userEntity);  // パスワードは出力されない
```

### 3. 一意制約とインデックス

```java
/**
 * 一意制約・インデックスの設定
 *
 * <p>【セキュリティ・パフォーマンス】
 *
 * <ul>
 *   <li>username, emailに一意制約（重複登録防止）
 *   <li>usernameにインデックス（ログイン認証の高速化）
 *   <li>emailにインデックス（メール検索の高速化）
 * </ul>
 */
@Entity
@Table(name = "users", 
    // ✅ 一意制約（重複登録防止）
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_users_email", columnNames = "email")
    },
    // ✅ インデックス（検索パフォーマンス向上）
    indexes = {
        @Index(name = "idx_users_username", columnList = "username"),
        @Index(name = "idx_users_email", columnList = "email")
    }
)
public class UserJpaEntity {
    @Column(nullable = false, unique = true, length = 16)
    private String username;
    
    @Column(nullable = false, unique = true, length = 128)
    private String email;
}
```

## 🧪 テスト戦略

### JPA Entityのテスト

```java
package com.api.todos.infrastructure.persistence.entity;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TodoJpaEntityのテスト
 *
 * <p>【テスト方針】
 *
 * <ul>
 *   <li>@PrePersist / @PreUpdateの動作確認
 *   <li>Lombokアノテーションの動作確認
 *   <li>フィールドの初期値確認
 * </ul>
 */
class TodoJpaEntityTest {

    @Test
    void onCreate_作成日時と更新日時が自動設定される() {
        // Given
        TodoJpaEntity entity = new TodoJpaEntity();
        entity.setTitle("Test TODO");
        entity.setUserId(UUID.randomUUID());
        
        // When: @PrePersist実行
        entity.onCreate();
        
        // Then: 作成日時・更新日時が設定される
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    void onCreate_既に作成日時が設定されている場合は上書きしない() {
        // Given
        TodoJpaEntity entity = new TodoJpaEntity();
        LocalDateTime fixedTime = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        entity.setCreatedAt(fixedTime);
        
        // When: @PrePersist実行
        entity.onCreate();
        
        // Then: 既存の作成日時は上書きされない
        assertEquals(fixedTime, entity.getCreatedAt());
    }

    @Test
    void onUpdate_更新日時のみが更新される() {
        // Given
        TodoJpaEntity entity = new TodoJpaEntity();
        LocalDateTime createdTime = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        entity.setCreatedAt(createdTime);
        entity.setUpdatedAt(createdTime);
        
        // When: @PreUpdate実行（時間をずらす）
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // ignore
        }
        entity.onUpdate();
        
        // Then: 更新日時のみ更新される（作成日時は不変）
        assertEquals(createdTime, entity.getCreatedAt());
        assertTrue(entity.getUpdatedAt().isAfter(createdTime));
    }

    @Test
    void getter_Setter_Lombokが正常に機能する() {
        // Given
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String title = "Test Title";
        
        // When
        TodoJpaEntity entity = new TodoJpaEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setTitle(title);
        entity.setCompleted(true);
        entity.setDeleted(false);
        
        // Then
        assertEquals(id, entity.getId());
        assertEquals(userId, entity.getUserId());
        assertEquals(title, entity.getTitle());
        assertTrue(entity.isCompleted());
        assertFalse(entity.isDeleted());
    }

    @Test
    void allArgsConstructor_すべてのフィールドを設定できる() {
        // Given
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        // When
        TodoJpaEntity entity = new TodoJpaEntity(
            id,
            "Test Title",
            "Test Description",
            false,
            userId,
            now,
            now,
            false
        );
        
        // Then
        assertEquals(id, entity.getId());
        assertEquals("Test Title", entity.getTitle());
        assertEquals("Test Description", entity.getDescriptions());
        assertFalse(entity.isCompleted());
        assertEquals(userId, entity.getUserId());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
        assertFalse(entity.isDeleted());
    }
}
```

### Repository統合テストでのJPA Entity検証

```java
@DataJpaTest
class TodoJpaRepositoryTest {

    @Autowired
    private TodoJpaRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void save_JPA_Entityが正しく保存される() {
        // Given: JPA Entity作成
        TodoJpaEntity entity = new TodoJpaEntity();
        entity.setTitle("Test TODO");
        entity.setDescriptions("Test Description");
        entity.setCompleted(false);
        entity.setUserId(UUID.randomUUID());
        entity.setDeleted(false);
        
        // When: 保存
        TodoJpaEntity saved = repository.save(entity);
        entityManager.flush();
        
        // Then: 保存成功
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());  // @PrePersistで自動設定
        assertNotNull(saved.getUpdatedAt());
        
        // DBに保存されているか確認
        TodoJpaEntity found = entityManager.find(TodoJpaEntity.class, saved.getId());
        assertNotNull(found);
        assertEquals("Test TODO", found.getTitle());
    }

    @Test
    void update_更新日時が自動更新される() throws InterruptedException {
        // Given: エンティティ保存
        TodoJpaEntity entity = new TodoJpaEntity();
        entity.setTitle("Original Title");
        entity.setUserId(UUID.randomUUID());
        entity.setDeleted(false);
        TodoJpaEntity saved = repository.save(entity);
        entityManager.flush();
        
        LocalDateTime originalUpdatedAt = saved.getUpdatedAt();
        
        // When: タイトル更新
        Thread.sleep(10);  // 時間をずらす
        saved.setTitle("Updated Title");
        repository.save(saved);
        entityManager.flush();
        
        // Then: 更新日時が更新される
        entityManager.clear();
        TodoJpaEntity updated = entityManager.find(TodoJpaEntity.class, saved.getId());
        assertEquals("Updated Title", updated.getTitle());
        assertTrue(updated.getUpdatedAt().isAfter(originalUpdatedAt));
    }
}
```

## 📋 実装チェックリスト

新しいJPA Entityを追加する際は、以下を確認してください：

### JPA Entity実装
- [ ] `@Entity`と`@Table`アノテーションを付与
- [ ] Domain層のEntityとは別クラスとして定義（Infrastructure層に配置）
- [ ] スキーマ名を明示的に指定（`schema = "public"`）
- [ ] テーブル名を明示的に指定（`name = "table_name"`）
- [ ] 主キーに`@Id`と`@GeneratedValue(strategy = GenerationType.UUID)`を設定
- [ ] カラム名とマッピング（`@Column`）を正しく設定
- [ ] `nullable`, `length`, `unique`等の制約を適切に設定
- [ ] 一意制約が必要なフィールドに`@UniqueConstraint`を設定
- [ ] 検索で使用するフィールドに`@Index`を設定
- [ ] `@PrePersist`と`@PreUpdate`で作成日時・更新日時を自動設定
- [ ] 論理削除フラグ（`deleted`）を追加
- [ ] Lombokを使用してボイラープレートコード削減
- [ ] 機密情報（パスワード等）を`@ToString(exclude = {...})`で除外

### 禁止事項の確認
- [ ] Domain層のEntityにJPAアノテーションを付与していない
- [ ] JPA Entityをドメインモデルとして直接使用していない
- [ ] JPA Entityにビジネスロジックを実装していない
- [ ] 平文パスワードのカラムを作成していない
- [ ] 物理削除ではなく論理削除を実装している

### 対応するコンポーネント
- [ ] Domain層のEntityが存在する（Pure Java）
- [ ] Mapperが存在する（Domain Model ⇔ JPA Entity変換）
- [ ] Spring Data JPA Repositoryが存在する
- [ ] Repository実装が存在する（Domain層のインターフェース実装）

### テスト実装
- [ ] `@PrePersist` / `@PreUpdate`の動作確認テスト
- [ ] Lombokアノテーションの動作確認テスト
- [ ] フィールドの初期値確認テスト
- [ ] Repository統合テストでJPA Entityの保存・取得・更新を確認

## 🔗 関連ドキュメント

- **[persistence README](../README.md)** - persistence層全体の概要
- **[domain/model README](../../../domain/model/README.md)** - Domain層のEntity（Pure Java）
- **[persistence/mapper README](../mapper/README.md)** - Domain Model ⇔ JPA Entity 変換
- **[persistence/repository README](../repository/README.md)** - Repository実装
- **[AGENTS.md](../../../../../../../../../AGENTS.md)** - AIエージェント向けコンテキストドキュメント

## 💡 まとめ

Infrastructure層のpersistence/entityパッケージは **データベーステーブルとJPAエンティティのマッピング** を担当します：

### ✅ entityパッケージの責務

1. **データベーステーブルとのマッピング**: JPA/Hibernateアノテーション
2. **永続化専用エンティティ**: データベースへの保存・取得のみ
3. **テーブル構造の定義**: カラム名、制約、インデックス等

### ❌ entityパッケージでやってはいけないこと

1. **Domain層のEntityにJPAアノテーションを付与**（Pure Javaを保つ）
2. **JPA Entityをドメインモデルとして直接使用**（必ず分離）
3. **JPA Entityにビジネスロジックを実装**（Domain層の責務）

### 🎯 設計の意図

この設計により：

- **Domain層をフレームワークから独立**させる（Pure Java）
- **Domain EntityとJPA Entityを分離**してそれぞれの責務を明確化
- **データベーススキーマの変更がビジネスロジックに影響しない**
- **Mapperで変換ロジックを集約**し、保守性を向上

---

**作成日**: 2025年1月5日  
**対象バージョン**: Spring Boot TODO API v0.0.1-SNAPSHOT  

このドキュメントは、Infrastructure層のpersistence/entityパッケージの責務と実装パターンを説明するものです。**Domain EntityとJPA Entityを厳格に分離し、それぞれの責務を明確にする** という設計原則を徹底してください。
