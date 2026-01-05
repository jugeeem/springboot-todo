# Application層（アプリケーション層）

## 📋 層の概要

**場所**: `api/src/main/java/com/api/todos/application/`

**目的**: **アプリケーション固有のビジネスフローをオーケストレーション**する層です。Domain層のビジネスルール（Entity、Value Object、Domain Service）を組み合わせて、ユースケースを実現します。Application層は **Pure Java** で実装され、フレームワークに一切依存しません。

**重要原則**: Application層は **如何なる理由があろうとフレームワークに依存してはいけません**。Spring、JPA等のアノテーションは **絶対に使用禁止** です。

---

## 🏛️ クリーンアーキテクチャにおける位置づけ

```
┌─────────────────────────────────────────────────┐
│  Frameworks & Drivers (Presentation層)          │ 最外層
│  - Controllers, REST API, Web Framework         │
│     ↓ Request DTO受取                           │
│     ↓ Request DTO → Command変換                 │
│     ↑ Result受取                                │
│     ↑ Result → Response DTO変換                 │
├─────────────────────────────────────────────────┤
│  Interface Adapters (Infrastructure層)          │
│  - Repositories実装, 外部サービス連携           │
│     ↓ Command渡す                               │
│     ↓ @Service + @Transactional (ラッパー)      │
│     ↑ Result返却                                │
├─────────────────────────────────────────────────┤
│  Application Business Rules (Application層)     │ ★ この層
│  - Use Cases（Pure Java）                       │
│  - Command/Result（Pure Java）                  │
│  - ドメインオブジェクトのオーケストレーション    │
│     ↓ Domain層のRepository Interface呼び出し    │
│     ↓ Domain層のEntity/Service呼び出し          │
│     ↑ Domain層から取得したEntityをResultに変換  │
├─────────────────────────────────────────────────┤
│  Enterprise Business Rules (Domain層)           │ 最内層
│  - Entities, Value Objects, Domain Services     │
│  - Repository Interfaces                        │
└─────────────────────────────────────────────────┘
```

### 依存関係の方向（The Dependency Rule）

```
Application層は:
✅ Domain層のみに依存できる
❌ Infrastructure層、Presentation層に依存してはいけない
❌ Spring、JPA等のフレームワークに依存してはいけない
```

---

## 📁 パッケージ構成

```
application/
├── command/                 # Commandオブジェクト（入力）
│   ├── auth/               # 認証関連Command
│   │   └── InitializePasswordCommand.java
│   └── todo/               # TODO関連Command
│       ├── CreateTodoCommand.java
│       ├── UpdateTodoCommand.java
│       └── TodoQueryCommand.java
│
├── dto/                    # Resultオブジェクト（出力）
│   ├── TodoResult.java
│   └── UserResult.java
│
└── usecase/                # UseCase（Pure Java）
    ├── auth/               # 認証関連UseCase
    │   └── InitializePasswordUseCase.java
    └── todo/               # TODO関連UseCase
        ├── CreateTodoUseCase.java
        ├── UpdateTodoUseCase.java
        ├── CompleteTodoUseCase.java
        └── GetTodoListUseCase.java
```

### 各パッケージの役割

1. **[command/](./command/README.md)** - Commandオブジェクト（UseCaseの入力）
   - Presentation層のDTOから変換される
   - Application層専用の入力オブジェクト
   - Pure Java、不変オブジェクト（finalフィールド、setterなし）
   
2. **[dto/](./dto/README.md)** - Resultオブジェクト（UseCaseの出力）
   - Domain EntityからResultオブジェクトに変換
   - Application層専用の出力オブジェクト
   - Pure Java、不変オブジェクト（finalフィールド、setterなし）
   
3. **[usecase/](./usecase/README.md)** - UseCase（アプリケーション固有のビジネスフロー）
   - Commandを受け取り、Resultを返却
   - Domain層のEntity/Service/Repositoryをオーケストレーション
   - Pure Java（@Service、@Transactionalアノテーション使用禁止）

---

## 🎯 Application層の責務（Responsibilities）

### ✅ このレイヤーが行うべきこと

1. **アプリケーション固有のビジネスフローのオーケストレーション**
   - ユースケースの実行順序の定義
   - Domain層のEntity、Value Object、Domain Serviceの組み合わせ
   - トランザクション境界の定義（UseCaseの粒度）

2. **Domain層とPresentation層の橋渡し**
   - Presentation層のDTOをCommandに変換（Controllerの責務）
   - Domain EntityをResultに変換（UseCaseの責務）
   - Presentation層とDomain層の独立性を保つ

3. **Pure Javaでの実装**
   - フレームワークに一切依存しない
   - Spring、JPA等のアノテーション使用禁止
   - ビジネスロジックの再利用可能性を確保

4. **ドメインオブジェクトの取得と保存の管理**
   - Repository Interface経由でのDomain Entity取得
   - ビジネスロジック実行後のDomain Entity保存
   - 複数のRepositoryをまたがる処理のオーケストレーション

### ❌ このレイヤーが行ってはいけないこと

1. **ビジネスルールの実装**
   - → ビジネスルールはDomain層のEntity/Serviceで実装
   - UseCaseはビジネスルールを呼び出すだけ

2. **Presentation層のDTOへの依存**
   - → Application層専用のCommand/Resultを使用
   - Presentation層のDTOは使用禁止

3. **Infrastructure層の実装への依存**
   - → Domain層のRepository Interfaceに依存
   - JPA Repository等の実装クラスは使用禁止

4. **トランザクション管理の実装**
   - → トランザクション管理はInfrastructure層の責務
   - @Transactionalアノテーションは使用禁止

5. **フレームワーク依存**
   - → Spring、JPA等のアノテーション使用禁止
   - Pure Javaで実装

---

## 🚨 絶対原則：Pure Java（フレームワーク依存禁止）

Application層は **Pure Java** で実装しなければなりません。以下のアノテーション・ライブラリは **絶対に使用禁止** です：

### ❌ 禁止されるアノテーション・ライブラリ

```java
// ❌ Spring Framework
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// ❌ JPA/Hibernate
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;

// ❌ Lombok（Application層では非推奨）
import lombok.Getter;
import lombok.Setter;
import lombok.Data;
import lombok.Builder;

// ❌ Bean Validation（Commandでは非推奨）
import jakarta.validation.constraints.NotNull;
```

**重要**: トランザクション管理（@Transactional）は **Infrastructure層のServiceラッパー** で実施します。

### ✅ 許可されるもの

```java
// ✅ Java標準ライブラリ
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// ✅ Domain層の依存
import com.api.todos.domain.model.Todo;
import com.api.todos.domain.model.User;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.domain.service.TodoDomainService;

// ✅ Pure Javaのコンストラクタ・メソッド
public class CreateTodoUseCase {
    // Pure Javaのフィールド
    private final TodoRepository todoRepository;
    
    // Pure Javaのコンストラクタ
    public CreateTodoUseCase(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }
    
    // Pure Javaのメソッド
    public TodoResult execute(CreateTodoCommand command) {
        // Pure Javaの実装
    }
}
```

---

## 🔄 データフローの全体像

### 1. リクエストからレスポンスまでの流れ

```
【Client】
    ↓ HTTPリクエスト
┌─────────────────────────────────────────────────┐
│ 【Presentation層】                               │
│ Controller                                      │
│   ↓ 1. Request DTO受取                          │
│   ↓ 2. Request DTO → Command変換                │
│   ↓ 3. Infrastructure層のService呼び出し         │
└─────────────────────────────────────────────────┘
    ↓ Command
┌─────────────────────────────────────────────────┐
│ 【Infrastructure層】                             │
│ Service (@Service + @Transactional)             │
│   ↓ 4. トランザクション開始                      │
│   ↓ 5. UseCase呼び出し（Commandそのまま渡す）    │
└─────────────────────────────────────────────────┘
    ↓ Command
┌─────────────────────────────────────────────────┐
│ 【Application層】★この層                        │
│ UseCase (Pure Java)                             │
│   ↓ 6. Commandからデータ取得                     │
│   ↓ 7. Repository Interface経由でEntity取得     │
│   ↓ 8. Domain Entity/Serviceのビジネスロジック実行│
│   ↓ 9. Repository Interface経由でEntity保存     │
│   ↓ 10. Domain Entity → Result変換              │
│   ↑ 11. Result返却                              │
└─────────────────────────────────────────────────┘
    ↑ Result
┌─────────────────────────────────────────────────┐
│ 【Infrastructure層】                             │
│ Service                                         │
│   ↑ 12. トランザクションコミット                 │
│   ↑ 13. Resultそのまま返却                      │
└─────────────────────────────────────────────────┘
    ↑ Result
┌─────────────────────────────────────────────────┐
│ 【Presentation層】                               │
│ Controller                                      │
│   ↑ 14. Result受取                              │
│   ↑ 15. Result → Response DTO変換               │
│   ↑ 16. HTTPレスポンス返却                      │
└─────────────────────────────────────────────────┘
    ↑ HTTPレスポンス
【Client】
```

### 2. データ変換の流れ

```
Request DTO (Presentation層)
    ↓ Controller: DTO → Command変換
Command (Application層 - 入力)
    ↓ UseCase: Commandからデータ取得
Domain Entity (Domain層)
    ↓ UseCase: Entity → Result変換
Result (Application層 - 出力)
    ↓ Controller: Result → Response DTO変換
Response DTO (Presentation層)
```

---

## 🚨 重要な設計パターン

### 1. トランザクション管理の分離

**原則**: Application層のUseCaseはPure Javaで実装し、トランザクション管理はInfrastructure層のServiceラッパーで実施します。

```java
// ❌ 絶対禁止: Application層でトランザクション管理
package com.api.todos.application.usecase.todo;

import org.springframework.stereotype.Service;  // ❌ Spring依存
import org.springframework.transaction.annotation.Transactional;  // ❌ Spring依存

@Service  // ❌ Application層でSpringアノテーション使用禁止
public class CreateTodoUseCase {
    
    @Transactional  // ❌ Application層でトランザクション管理禁止
    public TodoResult execute(CreateTodoCommand command) {
        // ビジネスロジック
    }
}
```

```java
// ✅ 正しい実装: Application層（Pure Java UseCase）
package com.api.todos.application.usecase.todo;

import com.api.todos.application.command.todo.CreateTodoCommand;
import com.api.todos.application.dto.TodoResult;
import com.api.todos.domain.repository.TodoRepository;

/**
 * TODO作成ユースケース（Pure Java）
 * 
 * 注意: @Service、@Transactionalアノテーションは使用しない
 */
public class CreateTodoUseCase {
    
    private final TodoRepository todoRepository;
    
    // Pure Javaのコンストラクタ
    public CreateTodoUseCase(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }
    
    // Pure Javaのメソッド（@Transactionalなし）
    public TodoResult execute(CreateTodoCommand command) {
        // ビジネスフロー実装
        Todo todo = Todo.create(
            command.getTitle(),
            command.getDescriptions(),
            command.getUserId()
        );
        
        Todo savedTodo = todoRepository.save(todo);
        
        return TodoResult.from(savedTodo);
    }
}
```

```java
// ✅ 正しい実装: Infrastructure層（トランザクション管理ラッパー）
package com.api.todos.infrastructure.service;

import com.api.todos.application.command.todo.CreateTodoCommand;
import com.api.todos.application.dto.TodoResult;
import com.api.todos.application.usecase.todo.CreateTodoUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TODO作成サービス（Infrastructure層のトランザクション管理ラッパー）
 * 
 * Pure JavaのUseCaseをラップし、Springのトランザクション管理を適用
 */
@Service
public class CreateTodoService {
    
    private final CreateTodoUseCase useCase;
    
    public CreateTodoService(CreateTodoUseCase useCase) {
        this.useCase = useCase;
    }
    
    @Transactional
    public TodoResult execute(CreateTodoCommand command) {
        return useCase.execute(command);
    }
}
```

```java
// ✅ 正しい実装: Infrastructure層（UseCase設定）
package com.api.todos.infrastructure.config;

import com.api.todos.application.usecase.todo.CreateTodoUseCase;
import com.api.todos.domain.repository.TodoRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application層のUseCaseをDI管理する設定クラス
 * Pure JavaのUseCaseをSpring DIコンテナに登録
 */
@Configuration
public class UseCaseConfig {
    
    @Bean
    public CreateTodoUseCase createTodoUseCase(TodoRepository todoRepository) {
        return new CreateTodoUseCase(todoRepository);
    }
}
```

---

## ✅ 実装パターン例

### 1. 基本的なCRUD UseCase

```java
// CreateTodoUseCase.java（作成）
public class CreateTodoUseCase {
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    
    public TodoResult execute(CreateTodoCommand command) {
        // 1. ユーザー存在確認
        userRepository.findById(command.getUserId())
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        // 2. Domain Entity作成
        Todo todo = Todo.create(
            command.getTitle(),
            command.getDescriptions(),
            command.getUserId()
        );
        
        // 3. 保存
        Todo savedTodo = todoRepository.save(todo);
        
        // 4. Result変換
        return TodoResult.from(savedTodo);
    }
}
```

```java
// GetTodoUseCase.java（取得）
public class GetTodoUseCase {
    private final TodoRepository todoRepository;
    
    public TodoResult execute(UUID todoId) {
        // 1. Repository経由でEntity取得
        Todo todo = todoRepository.findById(todoId)
            .orElseThrow(() -> new TodoNotFoundException("TODO not found"));
        
        // 2. Result変換
        return TodoResult.from(todo);
    }
}
```

```java
// UpdateTodoUseCase.java（更新）
public class UpdateTodoUseCase {
    private final TodoRepository todoRepository;
    
    public TodoResult execute(UpdateTodoCommand command) {
        // 1. Repository経由でEntity取得
        Todo todo = todoRepository.findById(command.getTodoId())
            .orElseThrow(() -> new TodoNotFoundException("TODO not found"));
        
        // 2. Domain Entityのビジネスロジック実行
        if (command.shouldUpdateTitle()) {
            todo.updateTitle(command.getTitle());
        }
        if (command.shouldUpdateDescriptions()) {
            todo.updateDescriptions(command.getDescriptions());
        }
        
        // 3. 保存
        Todo updatedTodo = todoRepository.save(todo);
        
        // 4. Result変換
        return TodoResult.from(updatedTodo);
    }
}
```

### 2. Domain Serviceを使用するUseCase

```java
// UpdateTodoUseCase.java（アクセス制御付き）
public class UpdateTodoUseCase {
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    private final TodoDomainService todoDomainService;
    
    public TodoResult execute(UpdateTodoCommand command) {
        // 1. Repository経由でEntity取得
        Todo todo = todoRepository.findById(command.getTodoId())
            .orElseThrow(() -> new TodoNotFoundException("TODO not found"));
        
        User user = userRepository.findById(command.getUserId())
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        // 2. Domain Serviceでアクセス制御
        if (!todoDomainService.isOwner(todo, user)) {
            throw new AccessDeniedException("You are not the owner of this TODO");
        }
        
        // 3. ビジネスロジック実行
        todo.updateTitle(command.getTitle());
        todo.updateDescriptions(command.getDescriptions());
        
        // 4. 保存
        Todo updatedTodo = todoRepository.save(todo);
        
        // 5. Result変換
        return TodoResult.from(updatedTodo);
    }
}
```

### 3. リスト取得UseCase（Domain Serviceでフィルタリング）

```java
// GetTodoListUseCase.java
public class GetTodoListUseCase {
    private final TodoRepository todoRepository;
    private final TodoDomainService todoDomainService;
    
    public List<TodoResult> execute(UUID userId) {
        // 1. Repository経由でEntity一覧取得
        List<Todo> todos = todoRepository.findByUserId(userId);
        
        // 2. Result変換
        return todos.stream()
            .map(TodoResult::from)
            .collect(Collectors.toList());
    }
    
    public List<TodoResult> getIncompleteTodos(UUID userId) {
        List<Todo> todos = todoRepository.findByUserId(userId);
        
        // Domain Serviceでフィルタリング
        List<Todo> incompleteTodos = todoDomainService.filterIncompleteTodos(todos);
        
        return incompleteTodos.stream()
            .map(TodoResult::from)
            .collect(Collectors.toList());
    }
}
```

---

## 🧪 テスト戦略

### Application層のテスト方針

Application層のUseCaseは、**Repository InterfaceをモックしたPure Javaユニットテスト**で実施します。

```java
package com.api.todos.application.usecase.todo;

import com.api.todos.application.command.todo.CreateTodoCommand;
import com.api.todos.application.dto.TodoResult;
import com.api.todos.domain.model.Todo;
import com.api.todos.domain.model.User;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CreateTodoUseCase テスト
 * 
 * Repository InterfaceをモックしたPure Javaユニットテスト
 * Springコンテキスト不要、高速実行
 */
@ExtendWith(MockitoExtension.class)
class CreateTodoUseCaseTest {
    
    @Mock
    private TodoRepository todoRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private CreateTodoUseCase useCase;
    
    @Test
    void 有効なCommandでTODOを作成できること() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = User.create("testuser", "test@example.com", "John", "Doe", UserRole.USER);
        CreateTodoCommand command = new CreateTodoCommand("Test TODO", "Description", userId);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        TodoResult result = useCase.execute(command);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test TODO");
        assertThat(result.getDescriptions()).isEqualTo("Description");
        assertThat(result.getUserId()).isEqualTo(userId);
        
        verify(userRepository).findById(userId);
        verify(todoRepository).save(any(Todo.class));
    }
    
    @Test
    void ユーザーが存在しない場合は例外をスローすること() {
        // Given
        UUID userId = UUID.randomUUID();
        CreateTodoCommand command = new CreateTodoCommand("Test TODO", "Description", userId);
        
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessageContaining("User not found");
        
        verify(userRepository).findById(userId);
        verify(todoRepository, never()).save(any());
    }
}
```

---

## ✅ 実装チェックリスト

### UseCase実装時

- [ ] **Pure Java**で実装（@Service、@Transactionalアノテーション使用禁止）
- [ ] **Domain層のRepository Interface**に依存（Infrastructure層の実装には依存しない）
- [ ] **Application層のCommand/Result**を使用（Presentation層のDTOは使用しない）
- [ ] **Domain Entityのビジネスロジックメソッド**を呼び出す（UseCaseでビジネスルール実装しない）
- [ ] **Domain Service**を適切に使用（複雑なビジネスロジック、アクセス制御等）
- [ ] **コンストラクタインジェクション**で依存性を受け取る
- [ ] **JavaDoc**でビジネスフローを記述
- [ ] **例外ハンドリング**を実装（Domain層の例外をApplication層で処理）

### Command/Result実装時

- [ ] **Pure Java**で実装（Spring/Lombokアノテーション使用禁止）
- [ ] **不変オブジェクト**として実装（finalフィールド、setterなし）
- [ ] **コンストラクタで検証**（Command）またはFactoryメソッド（Result）
- [ ] **Getterのみ提供**（setterは提供しない）
- [ ] **equals/hashCode**を実装
- [ ] **toString**でデバッグ情報を提供

### Infrastructure層連携

- [ ] **UseCaseConfig**でUseCaseインスタンスをBean登録（Infrastructure層）
- [ ] **Serviceラッパー**でトランザクション管理を実施（Infrastructure層）
- [ ] **Controller**でDTO⇔Command/Result変換を実施（Presentation層）

---

## 📚 パッケージ別詳細ドキュメント

### Application層のサブパッケージ

1. **[command/](./command/README.md)** - Commandオブジェクト（UseCaseの入力）
   - Pure Javaの不変オブジェクト
   - Presentation層のDTOから変換
   - コンストラクタでの入力検証
   
2. **[dto/](./dto/README.md)** - Resultオブジェクト（UseCaseの出力）
   - Pure Javaの不変オブジェクト
   - Domain EntityからResultに変換
   - Presentation層のDTOとは分離
   
3. **[usecase/](./usecase/README.md)** - UseCase（アプリケーション固有のビジネスフロー）
   - Pure Java（@Service、@Transactional使用禁止）
   - Domain層のオーケストレーション
   - Commandを受け取り、Resultを返却

---

## 🔗 関連ドキュメント

### プロジェクト内ドキュメント
- **[AGENTS.md](../../../../AGENTS.md)** - AIエージェント向けコンテキストドキュメント
- **[Domain層 README](../domain/README.md)** - Domain層全体の概要
- **[Infrastructure層 README](../infrastructure/README.md)** - Infrastructure層全体の概要
- **[Presentation層 README](../presentation/README.md)** - Presentation層全体の概要

### 外部参考資料
- [Clean Architecture (Robert C. Martin)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Effective Java (Joshua Bloch)](https://www.oreilly.com/library/view/effective-java/9780134686097/)

---

## 🎯 まとめ

Application層は、**アプリケーション固有のビジネスフローをPure Javaでオーケストレーション**する層です。

### 重要ポイント

1. **Pure Java（最重要）**
   - Spring、JPAアノテーション使用禁止
   - フレームワークに一切依存しない
   - @Service、@Transactionalは使用禁止

2. **トランザクション管理の分離**
   - UseCaseはPure Javaで実装
   - トランザクション管理はInfrastructure層のServiceラッパー
   - UseCaseConfigでUseCaseをBean登録

3. **Command/Result パターン**
   - Commandはプリミティブなデータのみ保持
   - ResultはDomain Entityから変換
   - Presentation層のDTOとは分離

4. **Domain層のオーケストレーション**
   - Repository InterfaceでEntity取得
   - Domain EntityのビジネスロジックメソッドOK
   - Domain Serviceで複雑なロジック実行
   - Repository InterfaceでEntity保存

5. **責務の明確化**
   - ビジネスルールはDomain層（Entity/Service）
   - ビジネスフローはApplication層（UseCase）
   - トランザクション管理はInfrastructure層（Serviceラッパー）
   - DTO変換はPresentation層（Controller）

### 禁止事項

- ❌ Application層で@Service、@Transactionalアノテーション使用
- ❌ Presentation層のDTOをUseCaseに渡す
- ❌ Infrastructure層の実装に依存
- ❌ UseCaseでビジネスルールを実装

このREADMEを参考に、**Pure Javaで実装された高品質なApplication層**を構築してください。Application層は、Domain層とPresentation層の橋渡しを行い、アプリケーション固有のビジネスフローを実現する重要な層です。

---

**作成日**: 2026年1月5日  
**対象バージョン**: Spring Boot TODO API v0.0.1-SNAPSHOT  
**対象パッケージ**: `com.api.todos.application`
