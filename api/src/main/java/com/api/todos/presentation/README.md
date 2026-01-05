# presentation パッケージ - Presentation層（最外層）

Spring Boot TODO APIのPresentation層（プレゼンテーション層）です。このパッケージは **クリーンアーキテクチャの最外層** に位置し、REST APIエンドポイントとHTTP通信を担当します。

## 📐 Presentation層の位置づけ

```
┌─────────────────────────────────────────────────┐
│  Presentation層 (presentation/) ← ここ          │ 最外層
│  - REST Controllers, DTOs, Exception Handlers  │
├─────────────────────────────────────────────────┤
│  Infrastructure層 (infrastructure/)             │
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

Presentation層は以下の責務を持ちます：

1. **REST APIエンドポイントの公開**
   - HTTPメソッド（GET/POST/PUT/DELETE）の定義
   - URLルーティング
   - リクエストパラメータ・ボディの受け取り

2. **HTTPリクエスト・レスポンスのハンドリング**
   - HTTPステータスコードの設定
   - レスポンスヘッダーの制御
   - エラーレスポンスの返却

3. **DTOの変換**
   - Presentation層のDTO → Application層のCommand/Query
   - Application層のResult → Presentation層のDTO

4. **バリデーション**
   - リクエストパラメータの検証
   - Bean Validation（`@Valid`, `@NotNull`等）の適用

5. **認証・認可の適用**
   - JWTトークンの検証
   - ロールベースのアクセス制御

## 📁 パッケージ構成

```
presentation/
├── dto/                  # Data Transfer Object（Presentation層専用）
│   ├── common/          # 共通DTO（ページネーション、エラーレスポンス等）
│   ├── auth/            # 認証関連のDTO
│   ├── user/            # ユーザー関連のDTO
│   └── todo/            # TODO関連のDTO
└── rest/                 # REST Controller
    ├── AuthController.java       # 認証API
    ├── UserController.java       # ユーザー管理API
    └── TodoController.java       # TODO管理API
```

## 🔄 データフロー（Presentation層の役割）

```
1. Client → HTTPリクエスト
   ↓
2. Controller: リクエストを受信
   ↓
3. Controller: Presentation層のDTOに変換（自動マッピング）
   ↓
4. Controller: Application層のCommandに変換 ★ ここが重要
   ↓
5. Controller: Infrastructure層のServiceを呼び出し（@Transactional）
   ↓
6. Controller: Application層のResultを受け取る
   ↓
7. Controller: Presentation層のDTOに変換 ★ ここが重要
   ↓
8. Controller: HTTPレスポンスとして返却
   ↓
9. HTTPレスポンス → Client
```

## 📝 実装ルール

### ✅ Presentation層で実施すべきこと

#### 1. DTO ⇔ Command/Result の変換

**Presentation層のDTOとApplication層のCommand/Resultは明確に分離する**

```java
@PostMapping
public ResponseEntity<TodoResponse> createTodo(
    @RequestBody CreateTodoRequest request,  // Presentation層のDTO
    @RequestHeader("x-user-id") UUID userId
) {
    // ✅ 1. Presentation層のDTO → Application層のCommand変換
    CreateTodoCommand command = new CreateTodoCommand(
        request.getTitle(),
        request.getDescriptions(),
        userId
    );
    
    // ✅ 2. Infrastructure層のサービス経由でUseCase実行
    TodoResult result = createTodoService.execute(command);
    
    // ✅ 3. Application層のResult → Presentation層のDTO変換
    TodoResponse response = TodoResponse.from(result);
    
    return ResponseEntity.ok(response);
}
```

#### 2. Infrastructure層のService（トランザクション管理ラッパー）を呼び出す

**UseCaseを直接呼び出さず、Infrastructure層のServiceを経由する**

```java
@RestController
@RequestMapping("/api/todos")
public class TodoController {
    // ✅ Infrastructure層のServiceを注入
    private final CreateTodoService createTodoService;
    
    public TodoController(CreateTodoService createTodoService) {
        this.createTodoService = createTodoService;
    }
    
    @PostMapping
    public ResponseEntity<TodoResponse> createTodo(...) {
        // ✅ Infrastructure層のServiceを呼び出し（トランザクション管理あり）
        TodoResult result = createTodoService.execute(command);
        
        // ...
    }
}
```

#### 3. 適切なHTTPステータスコードの返却

```java
// 成功時
return ResponseEntity.ok(response);                    // 200 OK
return ResponseEntity.status(HttpStatus.CREATED)       // 201 Created
    .body(response);

// エラー時（例外ハンドラーで処理）
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)                  // 404 Not Found
        .body(new ErrorResponse(ex.getMessage()));
}
```

#### 4. バリデーションの適用

```java
@PostMapping
public ResponseEntity<TodoResponse> createTodo(
    @Valid @RequestBody CreateTodoRequest request,  // @Valid でバリデーション
    @RequestHeader("x-user-id") UUID userId
) {
    // ...
}

// DTOクラス
@Getter
@Setter
@NoArgsConstructor
public class CreateTodoRequest {
    @NotBlank(message = "タイトルは必須です")
    @Size(max = 32, message = "タイトルは32文字以内で入力してください")
    private String title;
    
    @Size(max = 128, message = "説明は128文字以内で入力してください")
    private String descriptions;
}
```

### ❌ Presentation層で絶対にしてはいけないこと

#### 1. ドメインモデルを直接レスポンスとして返却

```java
// ❌ 絶対禁止: ドメインモデルを直接返却
@GetMapping("/{id}")
public ResponseEntity<Todo> getTodo(@PathVariable UUID id) {
    Todo todo = getTodoUseCase.execute(id);
    return ResponseEntity.ok(todo);  // ❌ ドメインモデルを直接返却
}

// ✅ 正しい実装: DTOに変換して返却
@GetMapping("/{id}")
public ResponseEntity<TodoResponse> getTodo(@PathVariable UUID id) {
    TodoResult result = getTodoService.execute(id);
    TodoResponse response = TodoResponse.from(result);  // ✅ DTOに変換
    return ResponseEntity.ok(response);
}
```

#### 2. UseCaseを直接呼び出す

```java
// ❌ 絶対禁止: UseCaseを直接注入・呼び出し
@RestController
public class TodoController {
    private final CreateTodoUseCase useCase;  // ❌ UseCaseを直接注入
    
    @PostMapping
    public ResponseEntity<TodoResponse> createTodo(...) {
        TodoResult result = useCase.execute(command);  // ❌ トランザクション管理がない
        // ...
    }
}

// ✅ 正しい実装: Infrastructure層のServiceを経由
@RestController
public class TodoController {
    private final CreateTodoService service;  // ✅ Infrastructure層のServiceを注入
    
    @PostMapping
    public ResponseEntity<TodoResponse> createTodo(...) {
        TodoResult result = service.execute(command);  // ✅ トランザクション管理あり
        // ...
    }
}
```

#### 3. ビジネスロジックをControllerに記述

```java
// ❌ 絶対禁止: Controllerにビジネスロジック
@PostMapping
public ResponseEntity<TodoResponse> createTodo(...) {
    // ❌ Controllerでビジネスロジックを実行してはいけない
    if (request.getTitle().length() > 100) {
        throw new BusinessException("タイトルが長すぎます");
    }
    
    // ❌ Controllerでデータ操作をしてはいけない
    Todo todo = new Todo();
    todo.setTitle(request.getTitle());
    todoRepository.save(todo);
    
    // ...
}

// ✅ 正しい実装: ビジネスロジックはUseCaseに委譲
@PostMapping
public ResponseEntity<TodoResponse> createTodo(...) {
    CreateTodoCommand command = new CreateTodoCommand(/*...*/);
    TodoResult result = createTodoService.execute(command);  // ✅ UseCaseに委譲
    TodoResponse response = TodoResponse.from(result);
    return ResponseEntity.ok(response);
}
```

#### 4. Repository（JPA Repository）を直接呼び出す

```java
// ❌ 絶対禁止: Controllerでリポジトリを直接操作
@RestController
public class TodoController {
    private final TodoJpaRepository todoJpaRepository;  // ❌ JPA Repositoryを直接注入
    
    @GetMapping("/{id}")
    public ResponseEntity<TodoResponse> getTodo(@PathVariable UUID id) {
        TodoJpaEntity entity = todoJpaRepository.findById(id)  // ❌ 直接DB操作
            .orElseThrow();
        // ...
    }
}
```

## 📋 DTOの実装パターン

### リクエストDTO（入力）

```java
// Presentation層のリクエストDTO
package com.api.todos.presentation.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;

@Getter
@Setter
@NoArgsConstructor
public class CreateTodoRequest {
    @NotBlank(message = "タイトルは必須です")
    @Size(max = 32, message = "タイトルは32文字以内で入力してください")
    private String title;
    
    @Size(max = 128, message = "説明は128文字以内で入力してください")
    private String descriptions;
}
```

### レスポンスDTO（出力）

```java
// Presentation層のレスポンスDTO
package com.api.todos.presentation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import com.api.todos.application.dto.TodoResult;
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
    
    /**
     * Application層のResultオブジェクトから変換
     * ★ ここでApplication層とPresentation層の境界を明確にする
     */
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

### 共通DTO（エラーレスポンス）

```java
// 共通エラーレスポンスDTO
package com.api.todos.presentation.dto.common;

import lombok.Getter;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private String message;
    private String errorCode;
    private LocalDateTime timestamp;
    
    public ErrorResponse(String message) {
        this.message = message;
        this.errorCode = "GENERAL_ERROR";
        this.timestamp = LocalDateTime.now();
    }
}
```

## 🎛️ Controllerの実装パターン

### 標準的なControllerの実装

```java
package com.api.todos.presentation.rest;

import com.api.todos.application.command.todo.CreateTodoCommand;
import com.api.todos.application.dto.TodoResult;
import com.api.todos.infrastructure.service.CreateTodoService;
import com.api.todos.presentation.dto.CreateTodoRequest;
import com.api.todos.presentation.dto.TodoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.UUID;

/**
 * TODOコントローラー
 * 
 * 【責務】
 * 1. HTTPリクエストの受信
 * 2. Presentation層のDTO → Application層のCommandに変換
 * 3. Infrastructure層のServiceを呼び出し（トランザクション管理）
 * 4. Application層のResult → Presentation層のDTOに変換
 * 5. HTTPレスポンスの返却
 */
@RestController
@RequestMapping("/api/todos")
public class TodoController {
    private final CreateTodoService createTodoService;
    
    public TodoController(CreateTodoService createTodoService) {
        this.createTodoService = createTodoService;
    }
    
    /**
     * TODO作成エンドポイント
     * 
     * @param request Presentation層のリクエストDTO
     * @param userId リクエストヘッダーから取得したユーザーID
     * @return Presentation層のレスポンスDTO
     */
    @PostMapping
    public ResponseEntity<TodoResponse> createTodo(
        @Valid @RequestBody CreateTodoRequest request,
        @RequestHeader("x-user-id") UUID userId
    ) {
        // 1. Presentation層のDTO → Application層のCommandに変換
        CreateTodoCommand command = new CreateTodoCommand(
            request.getTitle(),
            request.getDescriptions(),
            userId
        );
        
        // 2. Infrastructure層のService経由でUseCase実行
        TodoResult result = createTodoService.execute(command);
        
        // 3. Application層のResult → Presentation層のDTOに変換
        TodoResponse response = TodoResponse.from(result);
        
        // 4. HTTPレスポンスとして返却
        return ResponseEntity.ok(response);
    }
}
```

## 🚨 例外ハンドリング

Presentation層では統一された例外ハンドリングを実装します：

```java
package com.api.todos.presentation.exception;

import com.api.todos.presentation.dto.common.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            ex.getMessage(),
            "RESOURCE_NOT_FOUND",
            LocalDateTime.now()
        );
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getDefaultMessage())
            .collect(Collectors.joining(", "));
            
        ErrorResponse error = new ErrorResponse(
            message,
            "VALIDATION_ERROR",
            LocalDateTime.now()
        );
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }
}
```

## 🔐 セキュリティ

Presentation層では認証・認可を適用します：

```java
@RestController
@RequestMapping("/api/todos")
public class TodoController {
    
    @GetMapping
    @PreAuthorize("hasRole('USER')")  // ロールベースのアクセス制御
    public ResponseEntity<List<TodoResponse>> getTodos(
        @RequestHeader("Authorization") String token  // JWTトークン
    ) {
        // ...
    }
}
```

## 📊 依存関係まとめ

Presentation層が依存できるもの：

- ✅ **Application層**: Command, Query, Result
- ✅ **Infrastructure層**: Service（トランザクション管理ラッパー）
- ✅ **Spring Web**: `@RestController`, `@RequestMapping`, `ResponseEntity`等
- ✅ **Bean Validation**: `@Valid`, `@NotNull`, `@Size`等
- ✅ **Lombok**: `@Getter`, `@Setter`, `@NoArgsConstructor`等

Presentation層が依存してはいけないもの：

- ❌ **Domain層**: Entity, Repository（直接参照禁止）
- ❌ **Application層**: UseCase（直接呼び出し禁止）
- ❌ **Infrastructure層**: JPA Entity, JPA Repository（直接操作禁止）

## 🧪 テスト戦略

Presentation層のテスト方針：

```java
@WebMvcTest(TodoController.class)
class TodoControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CreateTodoService createTodoService;  // Infrastructure層のServiceをモック
    
    @Test
    void createTodo_正常系_201Createdを返却() throws Exception {
        // Given
        CreateTodoRequest request = new CreateTodoRequest();
        request.setTitle("Test TODO");
        request.setDescriptions("Test description");
        
        TodoResult result = new TodoResult(/*...*/);
        when(createTodoService.execute(any())).thenReturn(result);
        
        // When & Then
        mockMvc.perform(post("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-user-id", UUID.randomUUID().toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Test TODO"));
    }
    
    @Test
    void createTodo_バリデーションエラー_400BadRequestを返却() throws Exception {
        // Given
        CreateTodoRequest request = new CreateTodoRequest();
        request.setTitle("");  // バリデーションエラー
        
        // When & Then
        mockMvc.perform(post("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-user-id", UUID.randomUUID().toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
```

## 🔗 関連ドキュメント

- **[com.api.todos README](../README.md)** - パッケージ全体の概要
- **[AGENTS.md](../../../../../../../AGENTS.md)** - AIエージェント向けコンテキストドキュメント
- **[AUTH.md](../../../../../../../.docs/api/AUTH.md)** - 認証API仕様
- **[USERS.md](../../../../../../../.docs/api/USERS.md)** - ユーザー管理API仕様
- **[TODOS.md](../../../../../../../.docs/api/TODOS.md)** - TODO管理API仕様

## ✅ 実装チェックリスト

新しいエンドポイントを追加する際は、以下を確認してください：

- [ ] Presentation層のリクエストDTOを定義した
- [ ] Presentation層のレスポンスDTOを定義した
- [ ] DTO → Command/Query への変換を実装した
- [ ] Result → DTO への変換を実装した
- [ ] Infrastructure層のServiceを呼び出している（UseCaseを直接呼び出していない）
- [ ] 適切なHTTPステータスコードを返却している
- [ ] バリデーションを適用した（必要に応じて）
- [ ] 例外ハンドリングを実装した
- [ ] Controller統合テストを作成した
- [ ] ドメインモデルを直接レスポンスしていない

---

**作成日**: 2025年1月5日  
**対象バージョン**: Spring Boot TODO API v0.0.1-SNAPSHOT  

このドキュメントは、Presentation層の責務と実装パターンを説明するものです。**クリーンアーキテクチャの原則を厳格に遵守**し、DTO変換とInfrastructure層のService経由でのUseCase実行を徹底してください。
