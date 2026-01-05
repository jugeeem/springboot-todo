# infrastructure パッケージ - Infrastructure層（インフラストラクチャ層）

Spring Boot TODO APIのInfrastructure層（インフラストラクチャ層）です。このパッケージは **フレームワーク固有の実装とインフラ関連の責務** を担当します。

## 📐 Infrastructure層の位置づけ

```
┌─────────────────────────────────────────────────┐
│  Presentation層 (presentation/)                  │ 最外層
│  - REST Controllers, DTOs, Exception Handlers  │
├─────────────────────────────────────────────────┤
│  Infrastructure層 (infrastructure/) ← ここ      │
│  - JPA Entities, Repository実装, Config        │
├─────────────────────────────────────────────────┤
│  Application層 (application/)                   │
│  - Use Cases, Commands, Results (Pure Java)    │
├─────────────────────────────────────────────────┤
│  Domain層 (domain/)                              │ 最内層
│  - Entities, Repositories Interface (Pure Java)│
└─────────────────────────────────────────────────┘
```

## 🎯 責務

Infrastructure層は以下の責務を持ちます：

### 1. データベースアクセスの実装

- **JPA Entity**: データベーステーブルとのマッピング（Domain層のEntityとは分離）
- **JPA Repository**: Spring Data JPAのRepository定義
- **Repository実装**: Domain層のRepositoryインターフェースの具象クラス
- **Mapper**: Domain Model ⇔ JPA Entityの相互変換

### 2. UseCaseのDI設定

- **UseCaseConfig**: Pure JavaのUseCaseをSpring DIコンテナに登録
- Bean定義による依存性注入の設定

### 3. トランザクション管理

- **Service（トランザクション管理ラッパー）**: UseCaseをラップし`@Transactional`を適用
- データベーストランザクションの境界を明確にする

### 4. フレームワーク固有の実装

- Spring Securityの設定
- JWT認証・暗号化の実装
- 外部サービス連携
- その他インフラ関連の設定

## 📁 パッケージ構成

```
infrastructure/
├── config/                          # 設定クラス
│   ├── UseCaseConfig.java          # UseCaseのBean登録（DI設定）
│   ├── SecurityConfig.java         # Spring Security設定
│   └── DatabaseConfig.java         # データベース設定
├── persistence/                     # データベースアクセス
│   ├── entity/                     # JPA Entity（永続化専用）
│   │   ├── TodoJpaEntity.java
│   │   └── UserJpaEntity.java
│   ├── repository/                 # Repository実装
│   │   ├── TodoJpaRepository.java       # Spring Data JPA Repository
│   │   ├── TodoRepositoryImpl.java      # Domain層のインターフェース実装
│   │   ├── UserJpaRepository.java
│   │   └── UserRepositoryImpl.java
│   └── mapper/                     # Domain Model ⇔ JPA Entity 変換
│       ├── TodoMapper.java
│       └── UserMapper.java
├── security/                        # セキュリティインフラ
│   ├── JwtTokenProvider.java       # JWTトークン生成・検証
│   ├── PasswordEncoder.java        # パスワードハッシュ化
│   └── AuthenticationFilter.java  # 認証フィルター
└── service/                         # トランザクション管理ラッパー
    ├── auth/
    │   ├── InitializePasswordService.java
    │   └── GenerateJwtTokenService.java
    └── todo/
        ├── CreateTodoService.java
        ├── GetTodoService.java
        ├── UpdateTodoService.java
        └── DeleteTodoService.java
```

## 🚨 Infrastructure層の絶対禁止事項

### ❌ 1. Presentation層への依存

```java
// ❌ 絶対禁止: Infrastructure層でPresentation層のDTOを使用
package com.api.todos.infrastructure.service;

import com.api.todos.presentation.dto.CreateTodoRequest;  // ❌ Presentation層に依存

@Service
public class CreateTodoService {
    public TodoResult execute(CreateTodoRequest request) {  // ❌ Presentation層のDTOを受け取る
        // ...
    }
}

// ✅ 正しい実装: Application層のCommandを使用
package com.api.todos.infrastructure.service;

import com.api.todos.application.command.todo.CreateTodoCommand;  // ✅ Application層に依存

@Service
public class CreateTodoService {
    public TodoResult execute(CreateTodoCommand command) {  // ✅ Application層のCommandを受け取る
        // ...
    }
}
```

### ❌ 2. JPA EntityをDomain Modelとして扱う

```java
// ❌ 絶対禁止: JPA EntityをDomain Modelとして使用
package com.api.todos.domain.model;

import jakarta.persistence.Entity;  // ❌ Domain層でJPA依存

@Entity  // ❌ Domain層でJPAアノテーション
public class Todo {
    // ...
}

// ✅ 正しい実装: Domain ModelとJPA Entityを分離
// Domain層: Pure Javaのエンティティ
package com.api.todos.domain.model;

public class Todo {  // ✅ Pure Java（フレームワーク依存なし）
    private final UUID id;
    private String title;
    // ...
}

// Infrastructure層: JPA Entity
package com.api.todos.infrastructure.persistence.entity;

import jakarta.persistence.Entity;  // ✅ Infrastructure層でJPA依存

@Entity
@Table(name = "todos")
public class TodoJpaEntity {  // ✅ JPA専用のエンティティ
    @Id
    private UUID id;
    private String title;
    // ...
}
```

### ❌ 3. UseCaseに直接@Serviceを付与

```java
// ❌ 絶対禁止: UseCaseに@Serviceアノテーションを付与
package com.api.todos.application.usecase.todo;

import org.springframework.stereotype.Service;

@Service  // ❌ Application層でSpringアノテーション
public class CreateTodoUseCase {
    // ...
}

// ✅ 正しい実装: UseCaseはPure Java、ラッパーサービスで@Service
// Application層: Pure JavaのUseCase
package com.api.todos.application.usecase.todo;

public class CreateTodoUseCase {  // ✅ Pure Java（アノテーションなし）
    private final TodoRepository todoRepository;
    
    public CreateTodoUseCase(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }
    
    public TodoResult execute(CreateTodoCommand command) {
        // ...
    }
}

// Infrastructure層: トランザクション管理ラッパー
package com.api.todos.infrastructure.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service  // ✅ Infrastructure層で@Service
public class CreateTodoService {
    private final CreateTodoUseCase useCase;
    
    public CreateTodoService(CreateTodoUseCase useCase) {
        this.useCase = useCase;
    }
    
    @Transactional  // ✅ Infrastructure層で@Transactional
    public TodoResult execute(CreateTodoCommand command) {
        return useCase.execute(command);
    }
}
```

## ✅ 正しいInfrastructure層の実装パターン

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
 * 【重要】
 * - Domain層のTodoエンティティとは分離
 * - データベーステーブルとのマッピングのみを担当
 * - ビジネスロジックは含めない
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
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
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
 * Spring Data JPA Repository
 * 
 * 【責務】
 * - JPA Entityの永続化操作
 * - Spring Data JPAの機能を活用したクエリ定義
 */
@Repository
public interface TodoJpaRepository extends JpaRepository<TodoJpaEntity, UUID> {
    
    /**
     * ユーザーIDでTODOを検索（削除済み除外）
     */
    @Query("SELECT t FROM TodoJpaEntity t WHERE t.userId = :userId AND t.deleted = false")
    List<TodoJpaEntity> findByUserId(@Param("userId") UUID userId);
    
    /**
     * IDとユーザーIDでTODOを検索（削除済み除外）
     */
    @Query("SELECT t FROM TodoJpaEntity t WHERE t.id = :id AND t.userId = :userId AND t.deleted = false")
    Optional<TodoJpaEntity> findByIdAndUserId(
        @Param("id") UUID id,
        @Param("userId") UUID userId
    );
    
    /**
     * 完了状態でフィルタリング
     */
    List<TodoJpaEntity> findByUserIdAndCompletedAndDeletedFalse(UUID userId, boolean completed);
}
```

### 3. Repository実装（Domain層のインターフェース実装）

```java
package com.api.todos.infrastructure.persistence.repository;

import com.api.todos.domain.model.Todo;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.infrastructure.persistence.entity.TodoJpaEntity;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TodoRepositoryの実装
 * 
 * 【責務】
 * - Domain層のTodoRepositoryインターフェースを実装
 * - JPA Entityとの永続化操作
 * - Domain Model ⇔ JPA Entity の変換
 * 
 * 【依存性逆転の原則】
 * - Domain層でインターフェース定義
 * - Infrastructure層で実装
 */
@Repository
public class TodoRepositoryImpl implements TodoRepository {
    
    private final TodoJpaRepository jpaRepository;
    
    public TodoRepositoryImpl(TodoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public Optional<Todo> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(this::toDomainModel);
    }
    
    @Override
    public List<Todo> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
            .map(this::toDomainModel)
            .collect(Collectors.toList());
    }
    
    @Override
    public Todo save(Todo todo) {
        TodoJpaEntity entity = toJpaEntity(todo);
        TodoJpaEntity saved = jpaRepository.save(entity);
        return toDomainModel(saved);
    }
    
    @Override
    public void delete(UUID id) {
        // 論理削除
        jpaRepository.findById(id).ifPresent(entity -> {
            entity.setDeleted(true);
            jpaRepository.save(entity);
        });
    }
    
    /**
     * JPA Entity → Domain Model 変換
     */
    private Todo toDomainModel(TodoJpaEntity entity) {
        return new Todo(
            entity.getId(),
            entity.getTitle(),
            entity.getDescriptions(),
            entity.isCompleted(),
            entity.getUserId(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.isDeleted()
        );
    }
    
    /**
     * Domain Model → JPA Entity 変換
     */
    private TodoJpaEntity toJpaEntity(Todo todo) {
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

### 4. UseCaseConfig（DI設定）

```java
package com.api.todos.infrastructure.config;

import com.api.todos.application.usecase.todo.*;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.domain.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application層のUseCaseをDI管理する設定クラス
 * 
 * 【責務】
 * - Pure JavaのUseCaseをSpring DIコンテナに登録
 * - UseCaseの依存関係を解決
 * 
 * 【重要】
 * - UseCaseはPure Javaなので@Serviceアノテーションは使わない
 * - Infrastructure層でBean登録してDI管理
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
    
    @Bean
    public GetTodoUseCase getTodoUseCase(
        TodoRepository todoRepository
    ) {
        return new GetTodoUseCase(todoRepository);
    }
    
    @Bean
    public GetTodosUseCase getTodosUseCase(
        TodoRepository todoRepository
    ) {
        return new GetTodosUseCase(todoRepository);
    }
    
    @Bean
    public UpdateTodoUseCase updateTodoUseCase(
        TodoRepository todoRepository
    ) {
        return new UpdateTodoUseCase(todoRepository);
    }
    
    @Bean
    public DeleteTodoUseCase deleteTodoUseCase(
        TodoRepository todoRepository
    ) {
        return new DeleteTodoUseCase(todoRepository);
    }
}
```

### 5. Service（トランザクション管理ラッパー）

```java
package com.api.todos.infrastructure.service;

import com.api.todos.application.command.todo.CreateTodoCommand;
import com.api.todos.application.dto.TodoResult;
import com.api.todos.application.usecase.todo.CreateTodoUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TODO作成サービス（Infrastructure層のトランザクション管理ラッパー）
 * 
 * 【責務】
 * - Pure JavaのUseCaseをラップ
 * - Springのトランザクション管理を適用
 * - データベーストランザクションの境界を定義
 * 
 * 【重要】
 * - @Serviceと@Transactionalはこのラッパークラスで管理
 * - UseCaseには直接付与しない（Pure Javaを保つため）
 */
@Service
public class CreateTodoService {
    
    private final CreateTodoUseCase useCase;
    
    public CreateTodoService(CreateTodoUseCase useCase) {
        this.useCase = useCase;
    }
    
    /**
     * TODO作成処理（トランザクション管理あり）
     * 
     * @param command Application層のCommand
     * @return Application層のResult
     */
    @Transactional
    public TodoResult execute(CreateTodoCommand command) {
        return useCase.execute(command);
    }
}
```

## 🔄 データフローにおけるInfrastructure層の役割

```
【全体のデータフロー】

1. Client → HTTPリクエスト
   ↓
2. Presentation層: リクエストDTO受信
   ↓
3. Presentation層: DTO → Command変換
   ↓
4. Infrastructure層: Service呼び出し ★ @Transactional開始
   ↓
5. Application層: UseCase実行（Pure Java）
   ↓
6. Domain層: ビジネスロジック実行
   ↓
7. Domain層: Repositoryインターフェース呼び出し
   ↓
8. Infrastructure層: Repository実装 ★ JPA Entityで永続化
   ↓
9. Infrastructure層: Domain Model ⇔ JPA Entity 変換
   ↓
10. Infrastructure層: Service終了 ★ @Transactionalコミット
   ↓
11. Application層: Result返却
   ↓
12. Presentation層: Result → DTO変換
   ↓
13. Client ← HTTPレスポンス
```

## 📊 依存関係まとめ

Infrastructure層が依存できるもの：

- ✅ **Domain層**: Entity, Repository Interface, Domain Service
- ✅ **Application層**: UseCase, Command, Query, Result
- ✅ **Spring Framework**: `@Service`, `@Configuration`, `@Transactional`等
- ✅ **Spring Data JPA**: `JpaRepository`, `@Entity`, `@Table`等
- ✅ **PostgreSQL**: JDBCドライバー
- ✅ **Lombok**: `@Getter`, `@Setter`等

Infrastructure層が依存してはいけないもの：

- ❌ **Presentation層**: Controller, Request/Response DTO, Exception Handler

## 🔐 セキュリティインフラの実装

### JWT Token Provider

```java
package com.api.todos.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

/**
 * JWTトークンの生成・検証
 */
@Component
public class JwtTokenProvider {
    
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final long validityInMilliseconds = 3600000; // 1時間
    
    /**
     * JWTトークン生成
     */
    public String generateToken(UUID userId, String username) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);
        
        return Jwts.builder()
            .setSubject(userId.toString())
            .claim("username", username)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(key)
            .compact();
    }
    
    /**
     * トークンからユーザーIDを取得
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
        
        return UUID.fromString(claims.getSubject());
    }
    
    /**
     * トークンの有効性検証
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

## 🧪 テスト戦略

Infrastructure層のテスト方針：

### Repository統合テスト

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TodoRepositoryImplTest {
    
    @Autowired
    private TodoJpaRepository jpaRepository;
    
    private TodoRepositoryImpl repository;
    
    @BeforeEach
    void setUp() {
        repository = new TodoRepositoryImpl(jpaRepository);
    }
    
    @Test
    void findById_存在するTODOを取得できる() {
        // Given: テストデータ投入
        TodoJpaEntity entity = new TodoJpaEntity();
        entity.setTitle("Test TODO");
        entity.setUserId(UUID.randomUUID());
        TodoJpaEntity saved = jpaRepository.save(entity);
        
        // When: 検索
        Optional<Todo> result = repository.findById(saved.getId());
        
        // Then: 取得できる
        assertTrue(result.isPresent());
        assertEquals("Test TODO", result.get().getTitle());
    }
    
    @Test
    void save_新しいTODOを保存できる() {
        // Given: ドメインモデル作成
        UUID userId = UUID.randomUUID();
        Todo todo = new Todo("Test TODO", "Description", userId);
        
        // When: 保存
        Todo saved = repository.save(todo);
        
        // Then: 保存成功
        assertNotNull(saved.getId());
        assertEquals("Test TODO", saved.getTitle());
    }
}
```

### Service統合テスト

```java
@SpringBootTest
@Transactional
class CreateTodoServiceTest {
    
    @Autowired
    private CreateTodoService service;
    
    @Autowired
    private TodoRepository todoRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void execute_正常にTODOを作成できる() {
        // Given: ユーザー作成
        User user = createTestUser();
        userRepository.save(user);
        
        CreateTodoCommand command = new CreateTodoCommand(
            "Test TODO",
            "Description",
            user.getId()
        );
        
        // When: サービス実行（トランザクション管理あり）
        TodoResult result = service.execute(command);
        
        // Then: 作成成功
        assertNotNull(result.getId());
        assertEquals("Test TODO", result.getTitle());
        
        // DBに保存されているか確認
        Optional<Todo> saved = todoRepository.findById(result.getId());
        assertTrue(saved.isPresent());
    }
}
```

## 📋 実装チェックリスト

新しいInfrastructure層のコンポーネントを追加する際は、以下を確認してください：

### JPA Entity実装
- [ ] `@Entity`と`@Table`アノテーションを付与
- [ ] Domain層のEntityとは別クラスとして定義
- [ ] ビジネスロジックは含めない（永続化のみ）
- [ ] カラム名とマッピング（`@Column`）を正しく設定
- [ ] 作成日時・更新日時の自動設定（`@PrePersist`, `@PreUpdate`）

### Repository実装
- [ ] Domain層のRepositoryインターフェースを実装
- [ ] Spring Data JPAのRepositoryを注入
- [ ] Domain Model ⇔ JPA Entityの変換メソッドを実装
- [ ] SQLインジェクション対策（パラメータバインディング）

### UseCaseConfig
- [ ] `@Configuration`アノテーションを付与
- [ ] UseCaseの`@Bean`メソッドを定義
- [ ] 依存関係を正しく注入

### Service（トランザクション管理ラッパー）
- [ ] `@Service`アノテーションを付与
- [ ] `@Transactional`アノテーションを付与
- [ ] UseCaseを注入（コンストラクタインジェクション）
- [ ] Application層のCommand/Resultを使用
- [ ] Presentation層のDTOは使用しない

### 禁止事項の確認
- [ ] Presentation層に依存していない
- [ ] JPA EntityをDomain Modelとして使用していない
- [ ] UseCaseに直接`@Service`を付与していない
- [ ] Application層でInfrastructure層の実装詳細を参照していない

### セキュリティ
- [ ] SQLインジェクション対策（パラメータバインディング）
- [ ] パスワードのハッシュ化（BCrypt）
- [ ] JWTトークンの適切な生成・検証

## 🔗 関連ドキュメント

- **[com.api.todos README](../README.md)** - パッケージ全体の概要
- **[domain README](../domain/README.md)** - Domain層の実装パターン
- **[application README](../application/README.md)** - Application層の実装パターン
- **[presentation README](../presentation/README.md)** - Presentation層の実装パターン
- **[AGENTS.md](../../../../../../../AGENTS.md)** - AIエージェント向けコンテキストドキュメント

## 🎯 Infrastructure層設計のベストプラクティス

### 1. 依存性逆転の原則（Dependency Inversion Principle）

```java
// ✅ 良い例: Domain層でインターフェース定義、Infrastructure層で実装
// Domain層
package com.api.todos.domain.repository;
public interface TodoRepository {
    Optional<Todo> findById(UUID id);
    // ...
}

// Infrastructure層
package com.api.todos.infrastructure.persistence.repository;
@Repository
public class TodoRepositoryImpl implements TodoRepository {
    // 実装
}
```

### 2. トランザクション境界の明確化

```java
// ✅ 良い例: Service層でトランザクション境界を定義
@Service
public class CreateTodoService {
    @Transactional  // ここでトランザクション開始
    public TodoResult execute(CreateTodoCommand command) {
        return useCase.execute(command);
    }  // ここでトランザクション終了（コミット）
}
```

### 3. Domain ModelとJPA Entityの分離

Domain層のエンティティとJPA Entityは **必ず分離** します。これにより：

- Domain層をフレームワークから独立させる
- データベーススキーマの変更がドメインロジックに影響しない
- テストが容易になる

---

**作成日**: 2025年1月5日  
**対象バージョン**: Spring Boot TODO API v0.0.1-SNAPSHOT  

このドキュメントは、Infrastructure層の責務と実装パターンを説明するものです。**依存性逆転の原則を厳格に遵守**し、Domain層のインターフェースを実装し、UseCaseのDI設定とトランザクション管理を適切に行ってください。Infrastructure層はフレームワーク依存を許容しますが、Presentation層への依存は禁止です。
