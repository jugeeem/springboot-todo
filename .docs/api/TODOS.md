# TODO API ドキュメント

このドキュメントは、TODOアイテムの管理に関連するAPIエンドポイントを説明します。

---

## 目次

- [TODO一覧取得](#todo一覧取得)
- [TODO作成](#todo作成)
- [TODO詳細取得](#todo詳細取得)
- [TODO更新](#todo更新)
- [TODO削除](#todo削除)

---

## 認証について

すべてのTODO APIエンドポイントは認証が必要です。リクエスト時には有効なJWTトークンを`Authorization`ヘッダーに含める必要があります。

```
Authorization: Bearer <your-jwt-token>
```

認証されたユーザーは、自分が所有するTODOのみアクセス・操作が可能です。

---

## TODO一覧取得

認証されたユーザーが所有するTODOアイテムを取得します。ページネーション、フィルタリング、ソート機能をサポートします。

### エンドポイント

```
GET /api/todos
```

### リクエストヘッダー

| ヘッダー | 必須 | 説明 |
|---|---|---|
| `Authorization` | ✓ | JWTトークン（例: "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."） |

### クエリパラメータ

| パラメータ | 型 | 必須 | デフォルト | 説明 | 制約 |
|---|---|---|---|---|---|
| `page` | number | - | 1 | ページ番号 | 1以上 |
| `perPage` | number | - | 20 | 1ページあたりの件数 | 1〜100 |
| `completedFilter` | string | - | all | 完了状態フィルタ | `all`: 全件、`completed`: 完了済みのみ、`incomplete`: 未完了のみ |
| `sortBy` | string | - | createdAt | ソート基準 | `createdAt`, `updatedAt`, `title` |
| `sortOrder` | string | - | asc | ソート順序 | `asc`: 昇順、`desc`: 降順 |

### レスポンス

**成功時 (200 OK)**

```json
{
  "success": true,
  "message": "Request successful",
  "data": {
    "data": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "title": "買い物リスト作成",
        "descriptions": "今週の食材を買う",
        "completed": false,
        "userId": "user-123",
        "createdAt": "2025-07-24T10:00:00.000Z",
        "createdBy": "user-123",
        "updatedAt": "2025-07-24T10:00:00.000Z",
        "updatedBy": "user-123",
        "deleted": false
      },
      {
        "id": "660e8400-e29b-41d4-a716-446655440001",
        "title": "プロジェクト資料作成",
        "descriptions": null,
        "completed": false,
        "userId": "user-123",
        "createdAt": "2025-07-24T11:00:00.000Z",
        "createdBy": "user-123",
        "updatedAt": "2025-07-24T11:00:00.000Z",
        "updatedBy": "user-123",
        "deleted": false
      }
    ],
    "total": 50,
    "page": 1,
    "perPage": 20,
    "totalPages": 3
  }
}
```

### エラーレスポンス

**認証エラー (401 Unauthorized)**

```json
{
  "success": false,
  "message": "Unauthorized"
}
```

**サーバーエラー (500 Internal Server Error)**

```json
{
  "success": false,
  "message": "Internal server error"
}
```

### リクエスト例

**デフォルト（ページネーション、全件取得、作成日時昇順）**

```bash
curl -X GET http://localhost:3000/api/todos \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**ページネーション指定**

```bash
curl -X GET "http://localhost:3000/api/todos?page=2&perPage=10" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**作成日時の降順でソート**

```bash
curl -X GET "http://localhost:3000/api/todos?sortBy=createdAt&sortOrder=desc" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**タイトルの昇順でソート**

```bash
curl -X GET "http://localhost:3000/api/todos?sortBy=title&sortOrder=asc" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**複合クエリ（2ページ目、20件、作成日時降順）**

```bash
curl -X GET "http://localhost:3000/api/todos?page=2&perPage=20&sortBy=createdAt&sortOrder=desc" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**未完了タスクのみ取得**

```bash
curl -X GET "http://localhost:3000/api/todos?completedFilter=incomplete" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**完了済みタスクのみ取得**

```bash
curl -X GET "http://localhost:3000/api/todos?completedFilter=completed" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**未完了タスクのみ削除済みを除外して取得**

### 注意事項

- デフォルトでは全件（削除済み含む、完了/未完了両方）を取得します
- 完了状態フィルタ（`completedFilter`）でステータス絞り込みが可能です
- ページネーションはデフォルトで20件/ページです
- ソートはデフォルトで作成日時の昇順です
- `perPage`の最大値は100です
- `page`は1以上の整数である必要があります

---

## TODO作成

認証されたユーザーの新しいTODOアイテムを作成します。

### エンドポイント

```
POST /api/todos
```

### リクエストヘッダー

| ヘッダー | 必須 | 説明 |
|---|---|---|
| `Authorization` | ✓ | JWTトークン（例: "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."） |
| `Content-Type` | ✓ | application/json |

### リクエストボディ

| フィールド | 型 | 必須 | 説明 | 制約 |
|---|---|---|---|---|
| `title` | string | ✓ | TODOタイトル | 1〜32文字 |
| `descriptions` | string | - | TODO説明 | 128文字以下 |
| `completed` | boolean | - | 完了状態 | デフォルト: false |

### レスポンス

**成功時 (200 OK)**

```json
{
  "success": true,
  "message": "Todo created successfully",
  "data": {
    "id": "770e8400-e29b-41d4-a716-446655440002",
    "title": "プレゼンテーション準備",
    "descriptions": "来週の会議用のスライドを作成する",
    "completed": false,
    "userId": "user-123",
    "createdAt": "2025-07-24T12:00:00.000Z",
    "createdBy": "user-123",
    "updatedAt": "2025-07-24T12:00:00.000Z",
    "updatedBy": "user-123",
    "deleted": false
  }
}
```

### エラーレスポンス

**バリデーションエラー (400 Bad Request)**

```json
{
  "success": false,
  "message": "Invalid input: Title is required"
}
```

**認証エラー (401 Unauthorized)**

```json
{
  "success": false,
  "message": "Unauthorized"
}
```

**サーバーエラー (500 Internal Server Error)**

```json
{
  "success": false,
  "message": "Internal server error"
}
```

### リクエスト例

**最小構成**

```bash
curl -X POST http://localhost:3000/api/todos \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "title": "新しいタスク"
  }'
```

**全項目**

```bash
curl -X POST http://localhost:3000/api/todos \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "title": "プレゼンテーション準備",
    "descriptions": "来週の会議用のスライドを作成する",
    "completed": false
  }'
```

### 注意事項

- 作成されたTODOは自動的に認証ユーザーに関連付けられます
- `userId`フィールドはリクエストボディに含める必要はありません

---

## TODO詳細取得

指定されたIDのTODOアイテムの詳細を取得します。

### エンドポイント

```
GET /api/todos/{id}
```

### パラメータ

| パラメータ | 型 | 必須 | 説明 |
|---|---|---|---|
| `id` | string | ✓ | TODO ID（URLパラメータ） |

### リクエストヘッダー

| ヘッダー | 必須 | 説明 |
|---|---|---|
| `Authorization` | ✓ | JWTトークン（例: "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."） |

### レスポンス

**成功時 (200 OK)**

```json
{
  "success": true,
  "message": "Request successful",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "title": "買い物リスト作成",
    "descriptions": "今週の食材を買う",
    "completed": false,
    "userId": "user-123",
    "createdAt": "2025-07-24T10:00:00.000Z",
    "createdBy": "user-123",
    "updatedAt": "2025-07-24T10:00:00.000Z",
    "updatedBy": "user-123",
    "deleted": false
  }
}
```

### エラーレスポンス

**認証エラー (401 Unauthorized)**

```json
{
  "success": false,
  "message": "Unauthorized"
}
```

**アクセス拒否 (403 Forbidden)**

```json
{
  "success": false,
  "message": "Access denied"
}
```

**存在しないTODO (404 Not Found)**

```json
{
  "success": false,
  "message": "Todo not found"
}
```

**サーバーエラー (500 Internal Server Error)**

```json
{
  "success": false,
  "message": "Internal server error"
}
```

### リクエスト例

```bash
curl -X GET http://localhost:3000/api/todos/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 注意事項

- 認証されたユーザーが所有するTODOのみアクセス可能です
- 他のユーザーのTODOにアクセスしようとすると403エラーが返されます

---

## TODO更新

指定されたIDのTODOアイテムを更新します。

### エンドポイント

```
PUT /api/todos/{id}
```

### パラメータ

| パラメータ | 型 | 必須 | 説明 |
|---|---|---|---|
| `id` | string | ✓ | TODO ID（URLパラメータ） |

### リクエストヘッダー

| ヘッダー | 必須 | 説明 |
|---|---|---|
| `Authorization` | ✓ | JWTトークン（例: "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."） |
| `Content-Type` | ✓ | application/json |

### リクエストボディ

| フィールド | 型 | 必須 | 説明 | 制約 |
|---|---|---|---|---|
| `title` | string | - | TODOタイトル | 1〜32文字 |
| `descriptions` | string | - | TODO説明 | 128文字以下 |
| `completed` | boolean | - | 完了状態 | true/false |

### レスポンス

**成功時 (200 OK)**

```json
{
  "success": true,
  "message": "Todo updated successfully",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "title": "完了済み：買い物リスト作成",
    "descriptions": "食材購入完了、冷蔵庫に保管済み",
    "completed": true,
    "userId": "user-123",
    "createdAt": "2025-07-24T10:00:00.000Z",
    "createdBy": "user-123",
    "updatedAt": "2025-07-24T15:30:00.000Z",
    "updatedBy": "user-123",
    "deleted": false
  }
}
```

### エラーレスポンス

**バリデーションエラー (400 Bad Request)**

```json
{
  "success": false,
  "message": "Invalid input: Title must be less than 32 characters"
}
```

**認証エラー (401 Unauthorized)**

```json
{
  "success": false,
  "message": "Unauthorized"
}
```

**アクセス拒否 (403 Forbidden)**

```json
{
  "success": false,
  "message": "Access denied"
}
```

**存在しないTODO (404 Not Found)**

```json
{
  "success": false,
  "message": "Todo not found"
}
```

**サーバーエラー (500 Internal Server Error)**

```json
{
  "success": false,
  "message": "Internal server error"
}
```

### リクエスト例

**タイトルのみ更新**

```bash
curl -X PUT http://localhost:3000/api/todos/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "title": "更新されたタイトル"
  }'
```

**両方の項目を更新**

```bash
curl -X PUT http://localhost:3000/api/todos/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "title": "完了済み：買い物リスト作成",
    "descriptions": "食材購入完了、冷蔵庫に保管済み",
    "completed": true
  }'
```

### 注意事項

- リクエストボディに含まれたフィールドのみが更新されます（部分更新）
- 認証されたユーザーが所有するTODOのみ更新可能です
- `updatedAt`フィールドは自動的に更新されます

---

## TODO削除

指定されたIDのTODOアイテムを削除します。

### エンドポイント

```
DELETE /api/todos/{id}
```

### パラメータ

| パラメータ | 型 | 必須 | 説明 |
|---|---|---|---|
| `id` | string | ✓ | TODO ID（URLパラメータ） |

### リクエストヘッダー

| ヘッダー | 必須 | 説明 |
|---|---|---|
| `Authorization` | ✓ | JWTトークン（例: "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."） |

### レスポンス

**成功時 (200 OK)**

```json
{
  "success": true,
  "message": "Todo deleted successfully",
  "data": null
}
```

### エラーレスポンス

**認証エラー (401 Unauthorized)**

```json
{
  "success": false,
  "message": "Unauthorized"
}
```

**アクセス拒否 (403 Forbidden)**

```json
{
  "success": false,
  "message": "Access denied"
}
```

**存在しないTODO (404 Not Found)**

```json
{
  "success": false,
  "message": "Todo not found"
}
```

**サーバーエラー (500 Internal Server Error)**

```json
{
  "success": false,
  "message": "Internal server error"
}
```

### リクエスト例

```bash
curl -X DELETE http://localhost:3000/api/todos/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 注意事項

- 削除は論理削除（`deleted`フラグをtrueに設定）で行われます
- 論理削除されたTODOは削除後も存在し、`deleted: true`の状態で保持されます
- 認証されたユーザーが所有するTODOのみ削除可能です
- 削除後、`data`フィールドは`null`を返します

---

## 共通エラーコード

| ステータスコード | 説明 |
|---|---|
| 200 | リクエスト成功 |
| 400 | 不正なリクエスト（バリデーションエラー） |
| 401 | 認証エラー（認証情報が無効または期限切れ） |
| 403 | アクセス拒否（権限がない） |
| 404 | リソースが見つからない |
| 500 | サーバー内部エラー |

---

## データモデル

### TODO オブジェクト

| フィールド | 型 | 説明 |
|---|---|---|
| `id` | string | TODO ID（UUID形式） |
| `title` | string | TODOタイトル（最大32文字） |
| `descriptions` | string \| null | TODO説明（最大128文字、任意） |
| `completed` | boolean | 完了状態（true: 完了、false: 未完了） |
| `userId` | string | 所有者のユーザーID（UUID形式） |
| `createdAt` | string | 作成日時（ISO8601形式） |
| `createdBy` | string | 作成者のユーザーID |
| `updatedAt` | string | 更新日時（ISO8601形式） |
| `updatedBy` | string | 更新者のユーザーID |
| `deleted` | boolean | 論理削除フラグ（true: 削除済み、false: 有効） |

### ページネーションレスポンス

TODO一覧取得のレスポンスには、以下のページネーション情報が含まれます。

| フィールド | 型 | 説明 |
|---|---|---|
| `data` | Todo[] | TODOアイテムの配列 |
| `total` | number | 総件数（フィルタ条件適用後） |
| `page` | number | 現在のページ番号 |
| `perPage` | number | 1ページあたりの件数 |
| `totalPages` | number | 総ページ数 |

---

## セキュリティに関する注意事項

- すべてのTODO APIエンドポイントはHTTPS経由でアクセスすることを推奨します
- JWTトークンは安全に保管し、第三者と共有しないでください
- 各ユーザーは自分が所有するTODOのみアクセス・操作が可能です
- 認証トークンの有効期限が切れた場合は、再度ログインが必要です
