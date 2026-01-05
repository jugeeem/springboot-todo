# service パッケージ - トランザクション管理ラッパー

Infrastructure層のserviceパッケージです。このパッケージは **Pure JavaのUseCaseをラップし、Springのトランザクション管理を適用** する責務を担います。

## 🎯 serviceパッケージの役割

### なぜラッパーが必要なのか？

このプロジェクトは **厳密なクリーンアーキテクチャ** を採用しています：

```
【クリーンアーキテクチャの原則】

Application層 (UseCase) = Pure Java
  ↓
  ✅ フレームワーク依存なし
  ✅ @Service、@Transactionalアノテーションなし
  ✅ 単体テストが容易

Infrastructure層 (Service) = Spring依存
  ↓
  ✅ @Serviceアノテーション付与
  ✅ @Transactionalアノテーション付与
  ✅ トランザクション境界を定義
```

**UseCaseに直接`@Service`を付与しない理由**：

1. **Pure Javaを保つため**: Application層はフレームワークに依存しない
2. **テスタビリティ**: Pure JavaのUseCaseは単体テストが容易
3. **再利用性**: 異なるフレームワークでも再利用可能
4. **責務の分離**: トランザクション管理はInfrastructure層の責務

## 📁 パッケージ構成

```
service/
├── auth/                             # 認証関連サービス
│   ├── InitializePasswordService.java     # パスワード初期化（@Transactional）
│   └── GenerateJwtTokenService.java       # JWTトークン生成（@Transactional）
└── todo/                             # TODO関連サービス
    ├── CreateTodoService.java            # TODO作成（@Transactional）
    ├── GetTodoService.java               # TODO取得（ReadOnly）
    ├── GetTodosService.java              # TODO一覧取得（ReadOnly）
    ├── UpdateTodoService.java            # TODO更新（@Transactional）
    └── DeleteTodoService.java            # TODO削除（@Transactional）
```

## 🚨 絶対禁止事項

### ❌ 1. UseCaseに直接@Serviceを付与

```java
// ❌ 絶対禁止: Application層のUseCaseに@Serviceを付与
package com.api.todos.application.usecase.todo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service  // ❌ Application層でSpringアノテーション
@Transactional  // ❌ Application層でSpringアノテーション
public class CreateTodoUseCase {
    // Pure Javaであるべき
}

// ✅ 正しい実装: UseCaseはPure Java
package com.api.todos.application.usecase.todo;

// アノテーションなし（Pure Java）
public class CreateTodoUseCase {
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    
    public CreateTodoUseCase(TodoRepository todoRepository, UserRepository userRepository) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
    }
    
    public TodoResult execute(CreateTodoCommand command) {
        // ビジネスロジック実装
    }
}

// ✅ 正しい実装: Infrastructure層でラッパーサービス
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

### ❌ 2. Presentation層のDTOを使用

```java
// ❌ 絶対禁止: Presentation層のDTOをServiceで受け取る
package com.api.todos.infrastructure.service;

import com.api.todos.presentation.dto.CreateTodoRequest;  // ❌ Presentation層に依存

@Service
public class CreateTodoService {
    public TodoResult execute(CreateTodoRequest request) {  // ❌ Presentation層のDTO
        // ...
    }
}

// ✅ 正しい実装: Application層のCommandを使用
package com.api.todos.infrastructure.service;

import com.api.todos.application.command.todo.CreateTodoCommand;  // ✅ Application層に依存

@Service
public class CreateTodoService {
    public TodoResult execute(CreateTodoCommand command) {  // ✅ Application層のCommand
        // ...
    }
}
```

### ❌ 3. Serviceで直接ビジネスロジックを実装

```java
// ❌ 絶対禁止: Serviceでビジネスロジックを実装
@Service
public class CreateTodoService {
    private final TodoRepository todoRepository;
    
    @Transactional
    public TodoResult execute(CreateTodoCommand command) {
        // ❌ ビジネスロジックをServiceに実装
        if (command.getTitle() == null || command.getTitle().isEmpty()) {
            throw new IllegalArgumentException("タイトルは必須です");
        }
        
        Todo todo = new Todo(command.getTitle(), command.getDescriptions(), command.getUserId());
        Todo saved = todoRepository.save(todo);
        return TodoResult.from(saved);
    }
}

// ✅ 正しい実装: ServiceはUseCaseを呼び出すだけ
@Service
public class CreateTodoService {
    private final CreateTodoUseCase useCase;  // ✅ UseCaseに委譲
    
    public CreateTodoService(CreateTodoUseCase useCase) {
        this.useCase = useCase;
    }
    
    @Transactional
    public TodoResult execute(CreateTodoCommand command) {
        return useCase.execute(command);  // ✅ UseCaseを呼び出すだけ
    }
}
```

## ✅ 正しい実装パターン

### 1. 基本パターン（書き込み操作）

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
 * - ビジネスロジックは実装しない（UseCaseに委譲）
 */
@Service
public class CreateTodoService {
    
    private final CreateTodoUseCase useCase;
    
    /**
     * コンストラクタインジェクション
     * 
     * @param useCase TODO作成UseCase（UseCaseConfigでBean登録済み）
     */
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

### 2. 読み取り専用パターン（readOnly=true）

```java
package com.api.todos.infrastructure.service;

import com.api.todos.application.dto.TodoResult;
import com.api.todos.application.usecase.todo.GetTodoUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/**
 * TODO取得サービス（読み取り専用）
 * 
 * 【ポイント】
 * - 読み取り専用の場合は @Transactional(readOnly = true) を指定
 * - パフォーマンス最適化（読み取り専用モード）
 */
@Service
public class GetTodoService {
    
    private final GetTodoUseCase useCase;
    
    public GetTodoService(GetTodoUseCase useCase) {
        this.useCase = useCase;
    }
    
    /**
     * TODO取得処理（読み取り専用トランザクション）
     * 
     * @param id TODO ID
     * @return Application層のResult
     */
    @Transactional(readOnly = true)
    public TodoResult execute(UUID id) {
        return useCase.execute(id);
    }
}
```

### 3. リスト取得パターン

```java
package com.api.todos.infrastructure.service;

import com.api.todos.application.dto.TodoResult;
import com.api.todos.application.usecase.todo.GetTodosUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

/**
 * TODO一覧取得サービス（読み取り専用）
 */
@Service
public class GetTodosService {
    
    private final GetTodosUseCase useCase;
    
    public GetTodosService(GetTodosUseCase useCase) {
        this.useCase = useCase;
    }
    
    /**
     * TODO一覧取得処理（読み取り専用トランザクション）
     * 
     * @param userId ユーザーID
     * @return Application層のResult一覧
     */
    @Transactional(readOnly = true)
    public List<TodoResult> execute(UUID userId) {
        return useCase.execute(userId);
    }
}
```

### 4. 更新パターン

```java
package com.api.todos.infrastructure.service;

import com.api.todos.application.command.todo.UpdateTodoCommand;
import com.api.todos.application.dto.TodoResult;
import com.api.todos.application.usecase.todo.UpdateTodoUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TODO更新サービス
 */
@Service
public class UpdateTodoService {
    
    private final UpdateTodoUseCase useCase;
    
    public UpdateTodoService(UpdateTodoUseCase useCase) {
        this.useCase = useCase;
    }
    
    /**
     * TODO更新処理（トランザクション管理あり）
     * 
     * @param command Application層のCommand
     * @return Application層のResult
     */
    @Transactional
    public TodoResult execute(UpdateTodoCommand command) {
        return useCase.execute(command);
    }
}
```

### 5. 削除パターン

```java
package com.api.todos.infrastructure.service;

import com.api.todos.application.usecase.todo.DeleteTodoUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/**
 * TODO削除サービス
 * 
 * 【実装方針】
 * - 論理削除（deleted=trueに更新）
 * - 物理削除ではない
 */
@Service
public class DeleteTodoService {
    
    private final DeleteTodoUseCase useCase;
    
    public DeleteTodoService(DeleteTodoUseCase useCase) {
        this.useCase = useCase;
    }
    
    /**
     * TODO削除処理（トランザクション管理あり）
     * 
     * @param id TODO ID
     */
    @Transactional
    public void execute(UUID id) {
        useCase.execute(id);
    }
}
```

### 6. 認証関連パターン

```java
package com.api.todos.infrastructure.service;

import com.api.todos.application.command.auth.InitializePasswordCommand;
import com.api.todos.application.usecase.auth.InitializePasswordUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * パスワード初期化サービス
 */
@Service
public class InitializePasswordService {
    
    private final InitializePasswordUseCase useCase;
    
    public InitializePasswordService(InitializePasswordUseCase useCase) {
        this.useCase = useCase;
    }
    
    /**
     * パスワード初期化処理（トランザクション管理あり）
     * 
     * @param command Application層のCommand
     */
    @Transactional
    public void execute(InitializePasswordCommand command) {
        useCase.execute(command);
    }
}
```

```java
package com.api.todos.infrastructure.service;

import com.api.todos.application.command.auth.GenerateJwtTokenCommand;
import com.api.todos.application.dto.AuthResult;
import com.api.todos.application.usecase.auth.GenerateJwtTokenUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * JWTトークン生成サービス
 */
@Service
public class GenerateJwtTokenService {
    
    private final GenerateJwtTokenUseCase useCase;
    
    public GenerateJwtTokenService(GenerateJwtTokenUseCase useCase) {
        this.useCase = useCase;
    }
    
    /**
     * JWTトークン生成処理（トランザクション管理あり）
     * 
     * @param command Application層のCommand
     * @return Application層のResult（JWTトークン含む）
     */
    @Transactional(readOnly = true)
    public AuthResult execute(GenerateJwtTokenCommand command) {
        return useCase.execute(command);
    }
}
```

## 🔄 データフローにおけるServiceの役割

```
【全体のデータフロー】

1. Client → HTTPリクエスト
   ↓
2. Presentation層: Controller
   - リクエストDTO受信
   - DTO → Command変換
   ↓
3. Infrastructure層: Service ★ ここ
   - @Transactional開始
   - UseCaseを呼び出す
   ↓
4. Application層: UseCase（Pure Java）
   - ビジネスロジック実行
   - ドメインオブジェクトの操作
   ↓
5. Domain層: Entity, Repository
   - ドメインロジック実行
   - リポジトリ呼び出し
   ↓
6. Infrastructure層: Repository実装
   - JPA Entityで永続化
   - Domain Model ⇔ JPA Entity 変換
   ↓
7. Infrastructure層: Service
   - @Transactionalコミット
   - Result返却
   ↓
8. Presentation層: Controller
   - Result → DTO変換
   - HTTPレスポンス返却
   ↓
9. Client ← HTTPレスポンス
```

## 🎯 @Transactionalの使い分け

### 書き込み操作（INSERT/UPDATE/DELETE）

```java
@Transactional  // デフォルト設定
public TodoResult execute(CreateTodoCommand command) {
    return useCase.execute(command);
}
```

**設定内容**：
- propagation = `REQUIRED`（デフォルト）
- readOnly = `false`（デフォルト）
- timeout = デフォルト
- rollbackFor = `RuntimeException`と`Error`（デフォルト）

### 読み取り専用操作（SELECT）

```java
@Transactional(readOnly = true)  // 読み取り専用最適化
public TodoResult execute(UUID id) {
    return useCase.execute(id);
}
```

**メリット**：
- パフォーマンス最適化（読み取り専用モード）
- データベースへの書き込み防止
- リソース使用量の削減

### カスタム設定が必要な場合

```java
@Transactional(
    readOnly = false,
    timeout = 30,  // 30秒タイムアウト
    rollbackFor = Exception.class  // すべての例外でロールバック
)
public TodoResult execute(CreateTodoCommand command) {
    return useCase.execute(command);
}
```

## 📊 依存関係まとめ

Infrastructure層のServiceが依存できるもの：

- ✅ **Application層**: UseCase, Command, Query, Result
- ✅ **Spring Framework**: `@Service`, `@Transactional`等
- ✅ **Lombokは使用しない**: Serviceクラスではコンストラクタインジェクションを明示的に記述

Infrastructure層のServiceが依存してはいけないもの：

- ❌ **Presentation層**: Controller, Request/Response DTO
- ❌ **Domain層**: Entity, Repository Interface（UseCaseが依存する）
- ❌ **Infrastructure層の他の実装**: Repository実装、JPA Entity（UseCaseが依存する）

## 🧪 テスト戦略

### Service統合テスト

```java
package com.api.todos.infrastructure.service;

import com.api.todos.application.command.todo.CreateTodoCommand;
import com.api.todos.application.dto.TodoResult;
import com.api.todos.domain.model.User;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CreateTodoServiceの統合テスト
 * 
 * 【テスト方針】
 * - @SpringBootTestで実行（DIコンテナ起動）
 * - @Transactionalでテストデータをロールバック
 * - 実際のDBアクセスを伴う統合テスト
 */
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
        // Given: テストユーザー作成
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
        var saved = todoRepository.findById(result.getId());
        assertTrue(saved.isPresent());
    }
    
    @Test
    void execute_存在しないユーザーの場合は例外をスロー() {
        // Given: 存在しないユーザーID
        UUID nonExistentUserId = UUID.randomUUID();
        CreateTodoCommand command = new CreateTodoCommand(
            "Test TODO",
            "Description",
            nonExistentUserId
        );
        
        // When & Then: 例外をスロー
        assertThrows(
            UserNotFoundException.class,
            () -> service.execute(command)
        );
    }
    
    @Test
    void execute_トランザクションがロールバックされる() {
        // Given: テストユーザー作成
        User user = createTestUser();
        userRepository.save(user);
        
        // 不正なコマンド（タイトルがnull）
        CreateTodoCommand command = new CreateTodoCommand(
            null,  // 不正
            "Description",
            user.getId()
        );
        
        // When: 例外発生
        assertThrows(
            IllegalArgumentException.class,
            () -> service.execute(command)
        );
        
        // Then: トランザクションがロールバックされ、DBには保存されていない
        var todos = todoRepository.findByUserId(user.getId());
        assertTrue(todos.isEmpty());
    }
    
    private User createTestUser() {
        return new User(
            UUID.randomUUID(),
            "testuser",
            "test@example.com",
            "hashedPassword",
            "Test",
            "User",
            1,
            true,
            null,
            null,
            false
        );
    }
}
```

## 📋 実装チェックリスト

新しいServiceを追加する際は、以下を確認してください：

### 基本実装
- [ ] `@Service`アノテーションを付与
- [ ] `@Transactional`アノテーションを付与（読み取り専用の場合は`readOnly=true`）
- [ ] UseCaseをコンストラクタインジェクション
- [ ] `execute`メソッドでUseCaseを呼び出すだけ
- [ ] Application層のCommand/Resultを使用

### 禁止事項の確認
- [ ] Presentation層のDTOを使用していない
- [ ] ビジネスロジックを実装していない（UseCaseに委譲）
- [ ] Domain層やInfrastructure層の実装詳細に直接依存していない
- [ ] UseCaseに直接`@Service`を付与していない

### トランザクション設定
- [ ] 書き込み操作は`@Transactional`（デフォルト設定）
- [ ] 読み取り専用操作は`@Transactional(readOnly = true)`
- [ ] タイムアウトが必要な場合は`timeout`を指定
- [ ] カスタムロールバック条件が必要な場合は`rollbackFor`を指定

### 命名規則
- [ ] クラス名: `<動詞><名詞>Service`（例: `CreateTodoService`）
- [ ] メソッド名: `execute`（統一）
- [ ] パッケージ: `com.api.todos.infrastructure.service.<機能>`

### ドキュメント
- [ ] Javadocで責務を明記
- [ ] トランザクション境界を説明
- [ ] パラメータと戻り値の型を記述

## 🎯 Serviceクラス設計のベストプラクティス

### 1. 単一責任の原則（SRP）

```java
// ✅ 良い例: 1つのUseCaseに対して1つのService
@Service
public class CreateTodoService {
    private final CreateTodoUseCase useCase;
    
    @Transactional
    public TodoResult execute(CreateTodoCommand command) {
        return useCase.execute(command);
    }
}

// ❌ 悪い例: 複数のUseCaseを1つのServiceで管理
@Service
public class TodoService {
    private final CreateTodoUseCase createUseCase;
    private final UpdateTodoUseCase updateUseCase;
    private final DeleteTodoUseCase deleteUseCase;
    
    @Transactional
    public TodoResult create(CreateTodoCommand command) { /* ... */ }
    
    @Transactional
    public TodoResult update(UpdateTodoCommand command) { /* ... */ }
    
    @Transactional
    public void delete(UUID id) { /* ... */ }
}
```

### 2. コンストラクタインジェクション（必須）

```java
// ✅ 良い例: コンストラクタインジェクション
@Service
public class CreateTodoService {
    private final CreateTodoUseCase useCase;
    
    public CreateTodoService(CreateTodoUseCase useCase) {
        this.useCase = useCase;
    }
}

// ❌ 悪い例: フィールドインジェクション
@Service
public class CreateTodoService {
    @Autowired  // ❌ フィールドインジェクションは避ける
    private CreateTodoUseCase useCase;
}
```

### 3. トランザクション境界の明確化

```java
// ✅ 良い例: トランザクション境界が明確
@Service
public class CreateTodoService {
    @Transactional  // ここでトランザクション開始
    public TodoResult execute(CreateTodoCommand command) {
        return useCase.execute(command);
    }  // ここでトランザクション終了（コミット）
}

// ❌ 悪い例: トランザクション境界が不明確
@Service
public class CreateTodoService {
    // @Transactionalなし → トランザクション管理されない
    public TodoResult execute(CreateTodoCommand command) {
        return useCase.execute(command);
    }
}
```

## 🔗 関連ドキュメント

- **[infrastructure README](../README.md)** - Infrastructure層全体の概要
- **[config README](../config/README.md)** - UseCaseConfig等の設定
- **[application README](../../application/README.md)** - Application層のUseCase/Command/Result
- **[presentation/rest README](../../presentation/rest/README.md)** - ControllerからServiceの呼び出し
- **[AGENTS.md](../../../../../../../../AGENTS.md)** - AIエージェント向けコンテキストドキュメント

## 💡 まとめ

Infrastructure層のserviceパッケージは **トランザクション管理ラッパー** の役割を担います：

### ✅ Serviceの責務

1. **Pure JavaのUseCaseをラップ**
2. **@Serviceアノテーションを付与**（DI管理）
3. **@Transactionalアノテーションを付与**（トランザクション管理）
4. **トランザクション境界を定義**
5. **UseCaseを呼び出すだけ**（ビジネスロジックは実装しない）

### ❌ Serviceでやってはいけないこと

1. **ビジネスロジックの実装**（UseCaseに委譲）
2. **Presentation層のDTOの使用**（Application層のCommand/Result）
3. **複数のUseCaseを1つのServiceで管理**（単一責任の原則）
4. **UseCaseに直接@Serviceを付与**（Pure Javaを保つ）

### 🎯 設計の意図

この設計により：

- **Application層をPure Javaに保つ**（フレームワーク独立）
- **テスタビリティの向上**（UseCaseの単体テストが容易）
- **再利用性の向上**（異なるフレームワークでも再利用可能）
- **責務の明確化**（トランザクション管理はInfrastructure層）

---

**作成日**: 2025年1月5日  
**対象バージョン**: Spring Boot TODO API v0.0.1-SNAPSHOT  

このドキュメントは、Infrastructure層のserviceパッケージの責務と実装パターンを説明するものです。**Pure JavaのUseCaseをラップし、Springのトランザクション管理を適用する** という役割を徹底し、Application層のPure Java性を保ちながら、適切なトランザクション境界を定義してください。
