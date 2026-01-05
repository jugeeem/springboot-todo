# rest パッケージ - REST Controller

Spring Boot TODO APIのREST Controllerパッケージです。このパッケージは **Presentation層の中核** を担い、RESTful APIエンドポイントを実装します。

## 🎯 REST Controllerの責務

REST Controllerは以下の明確な責務のみを持ちます：

1. **HTTPリクエストの受信**
   - `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`
   - リクエストパラメータ、パスパラメータ、ボディの受け取り
   - リクエストヘッダーの取得

2. **Presentation層のDTO ⇔ Application層のCommand/Resultの変換**
   - リクエストDTO → Command（入力変換）
   - Result → レスポンスDTO（出力変換）

3. **Infrastructure層のService呼び出し**
   - トランザクション管理ラッパー（`@Service`）を経由
   - **UseCaseを直接呼び出してはいけない**

4. **HTTPレスポンスの返却**
   - 適切なHTTPステータスコード
   - レスポンスヘッダーの設定

5. **バリデーション・認証の適用**
   - `@Valid`によるバリデーション
   - `@PreAuthorize`によるロール制御

## 🚨 REST Controllerで絶対にしてはいけないこと

### ❌ 1. UseCaseを直接呼び出す

```java
// ❌ 絶対禁止: UseCaseを直接注入・呼び出し
@RestController
public class TodoController {
    private final CreateTodoUseCase useCase;  // ❌ UseCaseを直接注入
    
    public TodoController(CreateTodoUseCase useCase) {
        this.useCase = useCase;
    }
    
    @PostMapping
    public ResponseEntity<TodoResponse> createTodo(...) {
        // ❌ トランザクション管理がない状態でUseCaseを呼び出し
        TodoResult result = useCase.execute(command);
        // ...
    }
}

// ✅ 正しい実装: Infrastructure層のServiceを経由
@RestController
public class TodoController {
    private final CreateTodoService service;  // ✅ Infrastructure層のService
    
    public TodoController(CreateTodoService service) {
        this.service = service;
    }
    
    @PostMapping
    public ResponseEntity<TodoResponse> createTodo(...) {
        // ✅ トランザクション管理されたServiceを呼び出し
        TodoResult result = service.execute(command);
        // ...
    }
}
```

### ❌ 2. ドメインモデルを直接レスポンスとして返却

```java
// ❌ 絶対禁止: ドメインモデルを直接返却
@GetMapping("/{id}")
public ResponseEntity<Todo> getTodo(@PathVariable UUID id) {
    Todo todo = service.execute(id);
    return ResponseEntity.ok(todo);  // ❌ ドメインモデルを直接返却
}

// ✅ 正しい実装: DTOに変換して返却
@GetMapping("/{id}")
public ResponseEntity<TodoResponse> getTodo(@PathVariable UUID id) {
    GetTodoQuery query = new GetTodoQuery(id);
    TodoResult result = getTodoService.execute(query);
    TodoResponse response = TodoResponse.from(result);  // ✅ DTOに変換
    return ResponseEntity.ok(response);
}
```

### ❌ 3. ビジネスロジックをControllerに記述

```java
// ❌ 絶対禁止: Controllerでビジネスロジック
@PostMapping
public ResponseEntity<TodoResponse> createTodo(@RequestBody CreateTodoRequest request) {
    // ❌ Controllerでバリデーション処理
    if (request.getTitle() == null || request.getTitle().isEmpty()) {
        throw new ValidationException("タイトルは必須です");
    }
    
    // ❌ Controllerでビジネスルール実行
    if (request.getTitle().length() > 100) {
        throw new BusinessException("タイトルが長すぎます");
    }
    
    // ❌ Controllerでデータ操作
    Todo todo = new Todo();
    todo.setTitle(request.getTitle());
    todoRepository.save(todo);
    
    // ...
}

// ✅ 正しい実装: 全てUseCaseに委譲
@PostMapping
public ResponseEntity<TodoResponse> createTodo(
    @Valid @RequestBody CreateTodoRequest request,  // ✅ @Validでバリデーション
    @RequestHeader("x-user-id") UUID userId
) {
    CreateTodoCommand command = new CreateTodoCommand(
        request.getTitle(),
        request.getDescriptions(),
        userId
    );
    
    // ✅ ビジネスロジックは全てUseCaseで実行
    TodoResult result = createTodoService.execute(command);
    
    TodoResponse response = TodoResponse.from(result);
    return ResponseEntity.ok(response);
}
```

### ❌ 4. Repository（JPA Repository）を直接操作

```java
// ❌ 絶対禁止: ControllerでRepositoryを直接操作
@RestController
public class TodoController {
    private final TodoJpaRepository todoJpaRepository;  // ❌ JPA Repositoryを注入
    
    @GetMapping("/{id}")
    public ResponseEntity<TodoResponse> getTodo(@PathVariable UUID id) {
        // ❌ Controllerで直接DB操作
        TodoJpaEntity entity = todoJpaRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("TODO not found"));
        
        TodoResponse response = new TodoResponse();
        response.setId(entity.getId());
        response.setTitle(entity.getTitle());
        // ...
        
        return ResponseEntity.ok(response);
    }
}
```

### ❌ 5. Presentation層のDTOをUseCaseに直接渡す

```java
// ❌ 絶対禁止: Presentation層のDTOをそのままUseCaseに渡す
@PostMapping
public ResponseEntity<TodoResponse> createTodo(
    @RequestBody CreateTodoRequest request  // Presentation層のDTO
) {
    // ❌ Presentation層のDTOをそのまま渡す
    TodoResult result = createTodoService.execute(request);
    // ...
}

// ✅ 正しい実装: Application層のCommandに変換
@PostMapping
public ResponseEntity<TodoResponse> createTodo(
    @RequestBody CreateTodoRequest request,  // Presentation層のDTO
    @RequestHeader("x-user-id") UUID userId
) {
    // ✅ Application層のCommandに変換
    CreateTodoCommand command = new CreateTodoCommand(
        request.getTitle(),
        request.getDescriptions(),
        userId
    );
    
    TodoResult result = createTodoService.execute(command);
    // ...
}
```

## ✅ 正しいREST Controllerの実装パターン

### 標準的なControllerテンプレート

```java
package com.api.todos.presentation.rest;

import com.api.todos.application.command.todo.CreateTodoCommand;
import com.api.todos.application.dto.TodoResult;
import com.api.todos.infrastructure.service.CreateTodoService;
import com.api.todos.presentation.dto.CreateTodoRequest;
import com.api.todos.presentation.dto.TodoResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.UUID;

/**
 * TODOコントローラー
 * 
 * 【クリーンアーキテクチャの原則】
 * 1. HTTPリクエストの受信
 * 2. Presentation層のDTO → Application層のCommandに変換
 * 3. Infrastructure層のService経由でUseCase実行（トランザクション管理）
 * 4. Application層のResult → Presentation層のDTOに変換
 * 5. HTTPレスポンスの返却
 * 
 * 【禁止事項】
 * - UseCaseを直接呼び出さない
 * - ドメインモデルを直接レスポンスしない
 * - ビジネスロジックを記述しない
 * - Repositoryを直接操作しない
 */
@RestController
@RequestMapping("/api/todos")
public class TodoController {
    // ✅ Infrastructure層のService（トランザクション管理ラッパー）を注入
    private final CreateTodoService createTodoService;
    private final GetTodoService getTodoService;
    private final UpdateTodoService updateTodoService;
    private final DeleteTodoService deleteTodoService;
    
    public TodoController(
        CreateTodoService createTodoService,
        GetTodoService getTodoService,
        UpdateTodoService updateTodoService,
        DeleteTodoService deleteTodoService
    ) {
        this.createTodoService = createTodoService;
        this.getTodoService = getTodoService;
        this.updateTodoService = updateTodoService;
        this.deleteTodoService = deleteTodoService;
    }
    
    /**
     * TODO作成
     * POST /api/todos
     */
    @PostMapping
    public ResponseEntity<TodoResponse> createTodo(
        @Valid @RequestBody CreateTodoRequest request,  // バリデーション適用
        @RequestHeader("x-user-id") UUID userId          // 認証情報取得
    ) {
        // ステップ1: Presentation層のDTO → Application層のCommandに変換
        CreateTodoCommand command = new CreateTodoCommand(
            request.getTitle(),
            request.getDescriptions(),
            userId
        );
        
        // ステップ2: Infrastructure層のService経由でUseCase実行
        TodoResult result = createTodoService.execute(command);
        
        // ステップ3: Application層のResult → Presentation層のDTOに変換
        TodoResponse response = TodoResponse.from(result);
        
        // ステップ4: HTTPレスポンスとして返却
        return ResponseEntity
            .status(HttpStatus.CREATED)  // 201 Created
            .body(response);
    }
    
    /**
     * TODO取得
     * GET /api/todos/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TodoResponse> getTodo(
        @PathVariable UUID id,
        @RequestHeader("x-user-id") UUID userId
    ) {
        GetTodoQuery query = new GetTodoQuery(id, userId);
        TodoResult result = getTodoService.execute(query);
        TodoResponse response = TodoResponse.from(result);
        
        return ResponseEntity.ok(response);  // 200 OK
    }
    
    /**
     * TODO更新
     * PUT /api/todos/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<TodoResponse> updateTodo(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateTodoRequest request,
        @RequestHeader("x-user-id") UUID userId
    ) {
        UpdateTodoCommand command = new UpdateTodoCommand(
            id,
            request.getTitle(),
            request.getDescriptions(),
            request.isCompleted(),
            userId
        );
        
        TodoResult result = updateTodoService.execute(command);
        TodoResponse response = TodoResponse.from(result);
        
        return ResponseEntity.ok(response);  // 200 OK
    }
    
    /**
     * TODO削除
     * DELETE /api/todos/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodo(
        @PathVariable UUID id,
        @RequestHeader("x-user-id") UUID userId
    ) {
        DeleteTodoCommand command = new DeleteTodoCommand(id, userId);
        deleteTodoService.execute(command);
        
        return ResponseEntity.noContent().build();  // 204 No Content
    }
}
```

### リスト取得（ページネーション対応）

```java
/**
 * TODO一覧取得（ページネーション対応）
 * GET /api/todos?page=0&size=20
 */
@GetMapping
public ResponseEntity<PageResponse<TodoResponse>> getTodos(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestHeader("x-user-id") UUID userId
) {
    GetTodosQuery query = new GetTodosQuery(userId, page, size);
    TodoListResult result = getTodosService.execute(query);
    
    List<TodoResponse> todos = result.getTodos().stream()
        .map(TodoResponse::from)
        .collect(Collectors.toList());
    
    PageResponse<TodoResponse> response = new PageResponse<>(
        todos,
        result.getTotalElements(),
        result.getTotalPages(),
        result.getCurrentPage()
    );
    
    return ResponseEntity.ok(response);
}
```

## 📊 HTTPステータスコードの使い分け

REST Controllerでは適切なHTTPステータスコードを返却する必要があります：

### 成功系

| ステータスコード | 用途 | 実装例 |
|---|---|---|
| `200 OK` | 正常な取得・更新 | `ResponseEntity.ok(response)` |
| `201 Created` | 新規作成成功 | `ResponseEntity.status(HttpStatus.CREATED).body(response)` |
| `204 No Content` | 削除成功（レスポンスボディなし） | `ResponseEntity.noContent().build()` |

### エラー系

エラーレスポンスは `@RestControllerAdvice` で統一的に処理：

```java
package com.api.todos.presentation.exception;

import com.api.todos.presentation.dto.common.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * リソース未検出エラー（404 Not Found）
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            ex.getMessage(),
            "RESOURCE_NOT_FOUND",
            LocalDateTime.now()
        );
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)  // 404
            .body(error);
    }
    
    /**
     * バリデーションエラー（400 Bad Request）
     */
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
            .status(HttpStatus.BAD_REQUEST)  // 400
            .body(error);
    }
    
    /**
     * 認証エラー（401 Unauthorized）
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        ErrorResponse error = new ErrorResponse(
            ex.getMessage(),
            "UNAUTHORIZED",
            LocalDateTime.now()
        );
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)  // 401
            .body(error);
    }
    
    /**
     * 権限不足エラー（403 Forbidden）
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
        ErrorResponse error = new ErrorResponse(
            ex.getMessage(),
            "FORBIDDEN",
            LocalDateTime.now()
        );
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)  // 403
            .body(error);
    }
    
    /**
     * 予期しないエラー（500 Internal Server Error）
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception ex) {
        ErrorResponse error = new ErrorResponse(
            "予期しないエラーが発生しました",
            "INTERNAL_SERVER_ERROR",
            LocalDateTime.now()
        );
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)  // 500
            .body(error);
    }
}
```

## 🔐 セキュリティ・認証

### JWTトークン認証

```java
@RestController
@RequestMapping("/api/todos")
public class TodoController {
    
    @GetMapping
    @PreAuthorize("hasRole('USER')")  // ロールベースのアクセス制御
    public ResponseEntity<List<TodoResponse>> getTodos(
        @RequestHeader("Authorization") String token  // JWTトークン
    ) {
        // トークンからユーザーIDを取得（Security Contextから）
        UUID userId = SecurityContextHolder.getContext()
            .getAuthentication()
            .getName();
        
        // ...
    }
}
```

### ロールベースアクセス制御

```java
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")  // ADMIN権限のみ
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        // ...
    }
    
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")  // ADMINまたはMANAGER
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        // ...
    }
}
```

## 🧪 テスト戦略

REST Controllerのテストは `@WebMvcTest` を使用：

```java
package com.api.todos.presentation.rest;

import com.api.todos.application.dto.TodoResult;
import com.api.todos.infrastructure.service.CreateTodoService;
import com.api.todos.presentation.dto.CreateTodoRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TodoController.class)
class TodoControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CreateTodoService createTodoService;  // Infrastructure層のServiceをモック
    
    @Test
    void createTodo_正常系_201Createdを返却() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        TodoResult result = new TodoResult(
            UUID.randomUUID(),
            "Test TODO",
            "Test description",
            false,
            userId,
            LocalDateTime.now(),
            LocalDateTime.now(),
            false
        );
        when(createTodoService.execute(any())).thenReturn(result);
        
        // When & Then
        mockMvc.perform(post("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-user-id", userId.toString())
                .content("""
                    {
                        "title": "Test TODO",
                        "descriptions": "Test description"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.title").value("Test TODO"))
            .andExpect(jsonPath("$.completed").value(false));
    }
    
    @Test
    void createTodo_バリデーションエラー_400BadRequestを返却() throws Exception {
        // Given: タイトルが空のリクエスト
        UUID userId = UUID.randomUUID();
        
        // When & Then
        mockMvc.perform(post("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-user-id", userId.toString())
                .content("""
                    {
                        "title": "",
                        "descriptions": "Test description"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void getTodo_正常系_200OKを返却() throws Exception {
        // Given
        UUID todoId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TodoResult result = new TodoResult(/* ... */);
        when(getTodoService.execute(any())).thenReturn(result);
        
        // When & Then
        mockMvc.perform(get("/api/todos/{id}", todoId)
                .header("x-user-id", userId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(todoId.toString()));
    }
    
    @Test
    void getTodo_リソース未検出_404NotFoundを返却() throws Exception {
        // Given
        UUID todoId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(getTodoService.execute(any()))
            .thenThrow(new ResourceNotFoundException("TODO not found"));
        
        // When & Then
        mockMvc.perform(get("/api/todos/{id}", todoId)
                .header("x-user-id", userId.toString()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }
}
```

## 📋 実装チェックリスト

新しいエンドポイントを追加する際は、以下を確認してください：

### 設計・アーキテクチャ
- [ ] Presentation層のリクエストDTOを定義した
- [ ] Presentation層のレスポンスDTOを定義した
- [ ] Application層のCommand/Queryオブジェクトを定義した
- [ ] Infrastructure層のServiceを実装した（またはある）

### Controller実装
- [ ] `@RestController`アノテーションを付与
- [ ] `@RequestMapping`でベースパスを指定
- [ ] 適切なHTTPメソッドアノテーション（`@GetMapping`等）
- [ ] Infrastructure層のServiceを注入（UseCaseを直接注入していない）
- [ ] リクエストDTO → Commandへの変換を実装
- [ ] Result → レスポンスDTOへの変換を実装
- [ ] 適切なHTTPステータスコードを返却

### バリデーション・セキュリティ
- [ ] `@Valid`によるバリデーションを適用
- [ ] 認証ヘッダー（`Authorization`等）を適用
- [ ] ロールベース制御（`@PreAuthorize`）を適用（必要に応じて）

### 禁止事項の確認
- [ ] UseCaseを直接呼び出していない
- [ ] ドメインモデルを直接レスポンスしていない
- [ ] Controllerにビジネスロジックを記述していない
- [ ] Repositoryを直接操作していない
- [ ] Presentation層のDTOをUseCaseに直接渡していない

### テスト
- [ ] 正常系のテストを作成した（200/201のケース）
- [ ] バリデーションエラーのテストを作成した（400のケース）
- [ ] リソース未検出のテストを作成した（404のケース）
- [ ] 権限エラーのテストを作成した（401/403のケース、必要に応じて）

### ドキュメント
- [ ] APIドキュメント（`.docs/api/`）を更新した
- [ ] Javadocコメントを記述した

## 🔗 関連ドキュメント

- **[presentation README](../README.md)** - Presentation層全体の概要
- **[com.api.todos README](../../README.md)** - パッケージ全体の概要
- **[AGENTS.md](../../../../../../../../AGENTS.md)** - AIエージェント向けコンテキストドキュメント
- **[AUTH.md](../../../../../../../../.docs/api/AUTH.md)** - 認証API仕様
- **[USERS.md](../../../../../../../../.docs/api/USERS.md)** - ユーザー管理API仕様
- **[TODOS.md](../../../../../../../../.docs/api/TODOS.md)** - TODO管理API仕様

## 📖 参考実装

プロジェクト内の既存Controllerを参考にしてください：

- **AuthController** - 認証関連のエンドポイント実装
- **UserController** - ユーザー管理のエンドポイント実装
- **TodoController** - TODO管理のエンドポイント実装

---

**作成日**: 2025年1月5日  
**対象バージョン**: Spring Boot TODO API v0.0.1-SNAPSHOT  

このドキュメントは、REST Controllerの実装パターンと禁止事項を説明するものです。**クリーンアーキテクチャの原則を厳格に遵守**し、DTO変換とInfrastructure層のService経由でのUseCase実行を徹底してください。Controllerは単なる「薄いレイヤー」であり、ビジネスロジックを一切含まないことが重要です。
