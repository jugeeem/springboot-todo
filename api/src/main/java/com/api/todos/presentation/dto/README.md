# dto パッケージ - Data Transfer Object（Presentation層）

Spring Boot TODO APIのData Transfer Object（DTO）パッケージです。このパッケージは **Presentation層専用のデータ転送オブジェクト** を定義し、外部との境界（REST API）でのデータ交換を担当します。

## 🎯 DTOの責務と役割

DTOは以下の明確な責務を持ちます：

### 1. 外部との境界を定義

- **HTTPリクエスト**: クライアントから送信されるJSON → リクエストDTO
- **HTTPレスポンス**: レスポンスDTO → クライアントへ返却されるJSON

### 2. 層間の分離

```
Client（JSON）
    ↕ Presentation層のDTO
Controller
    ↕ Application層のCommand/Result
UseCase（Pure Java）
```

**重要**: Presentation層のDTOとApplication層のCommand/Resultは **別のオブジェクト** です。

### 3. バリデーション

- Bean Validationアノテーション（`@NotNull`, `@Size`, `@Email`等）の適用
- HTTPリクエストの妥当性検証

## 📁 パッケージ構成

```
dto/
├── common/                    # 共通DTO
│   ├── ErrorResponse.java    # エラーレスポンス
│   ├── PageResponse.java     # ページネーションレスポンス
│   └── ValidationError.java  # バリデーションエラー詳細
├── auth/                      # 認証関連DTO
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── RegisterRequest.java
│   └── RegisterResponse.java
├── user/                      # ユーザー関連DTO
│   ├── CreateUserRequest.java
│   ├── UpdateUserRequest.java
│   └── UserResponse.java
└── todo/                      # TODO関連DTO
    ├── CreateTodoRequest.java
    ├── UpdateTodoRequest.java
    └── TodoResponse.java
```

## 🚨 DTOの絶対禁止事項

### ❌ 1. ドメインモデルをDTOとして使用

```java
// ❌ 絶対禁止: ドメインモデルをレスポンスとして使用
@GetMapping("/{id}")
public ResponseEntity<Todo> getTodo(@PathVariable UUID id) {
    Todo todo = service.execute(id);  // ドメインモデル
    return ResponseEntity.ok(todo);   // ❌ 直接返却
}

// ✅ 正しい実装: DTOに変換して返却
@GetMapping("/{id}")
public ResponseEntity<TodoResponse> getTodo(@PathVariable UUID id) {
    TodoResult result = service.execute(id);        // Application層のResult
    TodoResponse response = TodoResponse.from(result);  // ✅ DTOに変換
    return ResponseEntity.ok(response);
}
```

### ❌ 2. JPA EntityをDTOとして使用

```java
// ❌ 絶対禁止: JPA EntityをDTOとして使用
@GetMapping("/{id}")
public ResponseEntity<TodoJpaEntity> getTodo(@PathVariable UUID id) {
    TodoJpaEntity entity = repository.findById(id);
    return ResponseEntity.ok(entity);  // ❌ JPA Entityを直接返却
}

// ✅ 正しい実装: DTOに変換
@GetMapping("/{id}")
public ResponseEntity<TodoResponse> getTodo(@PathVariable UUID id) {
    TodoResult result = service.execute(id);
    TodoResponse response = TodoResponse.from(result);  // ✅ DTOに変換
    return ResponseEntity.ok(response);
}
```

### ❌ 3. Presentation層のDTOをUseCaseに直接渡す

```java
// ❌ 絶対禁止: Presentation層のDTOをそのままUseCaseに渡す
@PostMapping
public ResponseEntity<TodoResponse> createTodo(
    @RequestBody CreateTodoRequest request  // Presentation層のDTO
) {
    // ❌ DTOをそのままServiceに渡す
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

### ❌ 4. Application層のCommand/ResultをPresentation層で直接使用

```java
// ❌ 絶対禁止: Application層のResultを直接レスポンスとして使用
@GetMapping("/{id}")
public ResponseEntity<TodoResult> getTodo(@PathVariable UUID id) {
    TodoResult result = service.execute(id);  // Application層のResult
    return ResponseEntity.ok(result);         // ❌ Resultを直接返却
}

// ✅ 正しい実装: Presentation層のDTOに変換
@GetMapping("/{id}")
public ResponseEntity<TodoResponse> getTodo(@PathVariable UUID id) {
    TodoResult result = service.execute(id);        // Application層のResult
    TodoResponse response = TodoResponse.from(result);  // ✅ DTOに変換
    return ResponseEntity.ok(response);
}
```

## ✅ 正しいDTOの実装パターン

### 1. リクエストDTO（入力）

```java
package com.api.todos.presentation.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.*;

/**
 * TODO作成リクエストDTO（Presentation層専用）
 * 
 * 【責務】
 * - HTTPリクエストボディのJSONをマッピング
 * - Bean Validationによる入力検証
 * 
 * 【注意】
 * - このDTOをUseCaseに直接渡してはいけない
 * - Application層のCommandに変換する必要がある
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTodoRequest {
    
    @NotBlank(message = "タイトルは必須です")
    @Size(max = 32, message = "タイトルは32文字以内で入力してください")
    private String title;
    
    @Size(max = 128, message = "説明は128文字以内で入力してください")
    private String descriptions;
}
```

### 2. レスポンスDTO（出力）

```java
package com.api.todos.presentation.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.api.todos.application.dto.TodoResult;
import java.util.UUID;
import java.time.LocalDateTime;

/**
 * TODOレスポンスDTO（Presentation層専用）
 * 
 * 【責務】
 * - Application層のResultをHTTPレスポンス用のJSONに変換
 * - クライアントに返却するデータ構造の定義
 * 
 * 【注意】
 * - Application層のResultから変換するためのファクトリメソッド（from）を提供
 * - ドメインモデルやJPA Entityから直接変換してはいけない
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
     * Application層のResultからPresentation層のDTOに変換
     * 
     * ★ ここで層の境界を明確にする
     * 
     * @param result Application層のTodoResult
     * @return Presentation層のTodoResponse
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

### 3. 共通DTO - エラーレスポンス

```java
package com.api.todos.presentation.dto.common;

import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * エラーレスポンスDTO
 * 
 * 全てのAPIエラーで統一されたフォーマットを返却
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private String message;
    private String errorCode;
    private LocalDateTime timestamp;
    private List<ValidationError> validationErrors;
    
    /**
     * 単純なエラーメッセージのみのコンストラクタ
     */
    public ErrorResponse(String message) {
        this.message = message;
        this.errorCode = "GENERAL_ERROR";
        this.timestamp = LocalDateTime.now();
        this.validationErrors = null;
    }
    
    /**
     * エラーコード付きコンストラクタ
     */
    public ErrorResponse(String message, String errorCode) {
        this.message = message;
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
        this.validationErrors = null;
    }
    
    /**
     * バリデーションエラー詳細付きコンストラクタ
     */
    public ErrorResponse(String message, String errorCode, LocalDateTime timestamp) {
        this.message = message;
        this.errorCode = errorCode;
        this.timestamp = timestamp;
        this.validationErrors = null;
    }
}

/**
 * バリデーションエラー詳細
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
class ValidationError {
    private String field;
    private String message;
    private Object rejectedValue;
}
```

### 4. 共通DTO - ページネーションレスポンス

```java
package com.api.todos.presentation.dto.common;

import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * ページネーションレスポンスDTO
 * 
 * リスト取得APIで統一されたページネーション情報を返却
 * 
 * @param <T> レスポンスDTO（TodoResponse, UserResponse等）
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean first;
    private boolean last;
    
    /**
     * ページ情報を自動計算するコンストラクタ
     */
    public PageResponse(List<T> content, long totalElements, int totalPages, int currentPage) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.pageSize = content.size();
        this.first = (currentPage == 0);
        this.last = (currentPage == totalPages - 1);
    }
}
```

## 🔄 DTOの変換パターン

### パターン1: リクエストDTO → Command

```java
// Controllerでの変換
@PostMapping
public ResponseEntity<TodoResponse> createTodo(
    @Valid @RequestBody CreateTodoRequest request,  // Presentation層のDTO
    @RequestHeader("x-user-id") UUID userId
) {
    // ステップ1: Presentation層のDTO → Application層のCommandに変換
    CreateTodoCommand command = new CreateTodoCommand(
        request.getTitle(),
        request.getDescriptions(),
        userId
    );
    
    // ステップ2: Commandを使用してServiceを呼び出し
    TodoResult result = createTodoService.execute(command);
    
    // ステップ3: Result → DTOに変換（後述）
    TodoResponse response = TodoResponse.from(result);
    
    return ResponseEntity.ok(response);
}
```

### パターン2: Result → レスポンスDTO

```java
// TodoResponse内でのファクトリメソッド
public static TodoResponse from(TodoResult result) {
    TodoResponse response = new TodoResponse();
    response.setId(result.getId());
    response.setTitle(result.getTitle());
    response.setDescriptions(result.getDescriptions());
    response.setCompleted(result.isCompleted());
    response.setUserId(result.getUserId());
    response.setCreatedAt(result.getCreatedAt());
    response.setUpdatedAt(result.getUpdatedAt());
    response.setDeleted(result.isDeleted());
    return response;
}

// または、Application層のResultをそのままマッピング
public TodoResponse(TodoResult result) {
    this.id = result.getId();
    this.title = result.getTitle();
    this.descriptions = result.getDescriptions();
    this.completed = result.isCompleted();
    this.userId = result.getUserId();
    this.createdAt = result.getCreatedAt();
    this.updatedAt = result.getUpdatedAt();
    this.deleted = result.isDeleted();
}
```

### パターン3: リスト変換

```java
// Controller内でのリスト変換
@GetMapping
public ResponseEntity<PageResponse<TodoResponse>> getTodos(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestHeader("x-user-id") UUID userId
) {
    GetTodosQuery query = new GetTodosQuery(userId, page, size);
    TodoListResult result = getTodosService.execute(query);
    
    // Application層のResultリスト → Presentation層のDTOリストに変換
    List<TodoResponse> todos = result.getTodos().stream()
        .map(TodoResponse::from)  // メソッド参照で変換
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

## 🔍 バリデーション

### Bean Validationアノテーション一覧

| アノテーション | 用途 | 例 |
|---|---|---|
| `@NotNull` | null不可 | `@NotNull private String title;` |
| `@NotBlank` | null・空文字・空白不可 | `@NotBlank private String username;` |
| `@NotEmpty` | null・空コレクション不可 | `@NotEmpty private List<String> tags;` |
| `@Size` | サイズ制限 | `@Size(min=1, max=32) private String title;` |
| `@Min` | 最小値 | `@Min(0) private int age;` |
| `@Max` | 最大値 | `@Max(150) private int age;` |
| `@Email` | メールアドレス形式 | `@Email private String email;` |
| `@Pattern` | 正規表現パターン | `@Pattern(regexp="^[0-9]{3}-[0-9]{4}$")` |
| `@Past` | 過去の日付 | `@Past private LocalDate birthDate;` |
| `@Future` | 未来の日付 | `@Future private LocalDate deadline;` |

### バリデーション実装例

```java
@Getter
@Setter
@NoArgsConstructor
public class CreateUserRequest {
    
    @NotBlank(message = "ユーザー名は必須です")
    @Size(min = 3, max = 20, message = "ユーザー名は3〜20文字で入力してください")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "ユーザー名は英数字とアンダースコアのみ使用できます")
    private String username;
    
    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    private String email;
    
    @NotBlank(message = "パスワードは必須です")
    @Size(min = 8, max = 100, message = "パスワードは8〜100文字で入力してください")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
        message = "パスワードは大文字・小文字・数字・特殊文字を含む必要があります"
    )
    private String password;
    
    @NotBlank(message = "名前は必須です")
    @Size(max = 50, message = "名前は50文字以内で入力してください")
    private String firstName;
    
    @NotBlank(message = "苗字は必須です")
    @Size(max = 50, message = "苗字は50文字以内で入力してください")
    private String lastName;
}
```

### Controllerでのバリデーション適用

```java
@PostMapping
public ResponseEntity<UserResponse> createUser(
    @Valid @RequestBody CreateUserRequest request  // @Valid でバリデーション実行
) {
    // バリデーションエラーがある場合、自動的に400 Bad Requestが返される
    // エラー内容は@RestControllerAdviceで統一的に処理
    
    CreateUserCommand command = new CreateUserCommand(/*...*/);
    UserResult result = createUserService.execute(command);
    UserResponse response = UserResponse.from(result);
    
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

## 📊 Lombokの使用

Presentation層のDTOでは **Lombok使用を推奨** します：

```java
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter                    // すべてのフィールドにgetterを生成
@Setter                    // すべてのフィールドにsetterを生成
@NoArgsConstructor         // デフォルトコンストラクタ生成
@AllArgsConstructor        // 全フィールドを引数に取るコンストラクタ生成
public class TodoResponse {
    private UUID id;
    private String title;
    private String descriptions;
    // ...
}
```

### Lombok使用の注意点

- **Domain層**: Lombok使用は極力避ける（Pure Java優先）
- **Application層**: Command/ResultもPure Javaを優先
- **Presentation層**: DTOはLombok使用OK（ボイラープレート削減）
- **Infrastructure層**: JPA EntityはLombok使用OK

## 📋 実装チェックリスト

新しいDTOを作成する際は、以下を確認してください：

### 設計
- [ ] リクエストDTOとレスポンスDTOを分離した
- [ ] Application層のCommand/Resultとは別のクラスとして定義した
- [ ] 共通DTOは`dto/common/`に配置した

### リクエストDTO
- [ ] Lombokアノテーションを適用（`@Getter`, `@Setter`, `@NoArgsConstructor`）
- [ ] Bean Validationアノテーションを適用
- [ ] 適切なエラーメッセージを設定

### レスポンスDTO
- [ ] Lombokアノテーションを適用
- [ ] `from(Result result)`メソッドを実装（Application層のResultから変換）
- [ ] JSON出力に不要なフィールドを含めていない（パスワード等）

### 変換
- [ ] Controller内でDTO → Commandの変換を実装
- [ ] Controller内でResult → DTOの変換を実装
- [ ] ドメインモデルやJPA Entityから直接変換していない

### 禁止事項の確認
- [ ] ドメインモデルをDTOとして使用していない
- [ ] JPA EntityをDTOとして使用していない
- [ ] Presentation層のDTOをUseCaseに直接渡していない
- [ ] Application層のCommand/ResultをHTTPレスポンスに直接使用していない

### ドキュメント
- [ ] Javadocコメントを記述した
- [ ] APIドキュメント（`.docs/api/`）を更新した

## 🔗 関連ドキュメント

- **[presentation README](../README.md)** - Presentation層全体の概要
- **[rest README](../rest/README.md)** - REST Controller実装パターン
- **[com.api.todos README](../../README.md)** - パッケージ全体の概要
- **[AGENTS.md](../../../../../../../../AGENTS.md)** - AIエージェント向けコンテキストドキュメント
- **[AUTH.md](../../../../../../../../.docs/api/AUTH.md)** - 認証API仕様
- **[USERS.md](../../../../../../../../.docs/api/USERS.md)** - ユーザー管理API仕様
- **[TODOS.md](../../../../../../../../.docs/api/TODOS.md)** - TODO管理API仕様

## 🎯 DTO設計のベストプラクティス

### 1. 単一責任の原則

各DTOは1つの目的のみを持つべきです：

```java
// ✅ 良い例: 作成用と更新用で分ける
public class CreateTodoRequest { /* ... */ }
public class UpdateTodoRequest { /* ... */ }

// ❌ 悪い例: 1つのDTOで全ての操作を担当
public class TodoRequest {
    private UUID id;  // 更新時のみ使用
    // ...
}
```

### 2. 不変性の活用（レスポンスDTO）

レスポンスDTOは変更不要なため、immutableにすることを検討：

```java
@Getter
@AllArgsConstructor
public class TodoResponse {
    private final UUID id;
    private final String title;
    // ...
}
```

### 3. セキュリティ - 機密情報の除外

レスポンスDTOにはパスワード等の機密情報を含めない：

```java
// ✅ 良い例: パスワードを含めない
@Getter
public class UserResponse {
    private UUID id;
    private String username;
    private String email;
    // パスワードは含めない
}

// ❌ 悪い例: パスワードを含める
@Getter
public class UserResponse {
    private UUID id;
    private String username;
    private String hashedPassword;  // ❌ 機密情報
}
```

---

**作成日**: 2025年1月5日  
**対象バージョン**: Spring Boot TODO API v0.0.1-SNAPSHOT  

このドキュメントは、Presentation層のDTOの設計原則と実装パターンを説明するものです。**Presentation層のDTOとApplication層のCommand/Resultは明確に分離**し、層の境界を意識した実装を行ってください。DTOは単なるデータ転送の入れ物であり、ビジネスロジックを含めてはいけません。
