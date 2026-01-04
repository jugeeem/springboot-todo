# ユーザー API ドキュメント

このドキュメントは、ユーザー管理に関連するAPIエンドポイントを説明します。

---

## 目次

- [認証について](#認証について)
- [権限レベルについて](#権限レベルについて)
- [ユーザー一覧取得](#ユーザー一覧取得)
- [新規ユーザー作成](#新規ユーザー作成)
- [ユーザー詳細取得](#ユーザー詳細取得)
- [ユーザー情報更新](#ユーザー情報更新)
- [ユーザー削除](#ユーザー削除)
- [現在ユーザー情報取得](#現在ユーザー情報取得)
- [現在ユーザー情報更新](#現在ユーザー情報更新)
- [パスワード変更](#パスワード変更)
- [現在ユーザーのTODO一覧取得](#現在ユーザーのtodo一覧取得)
- [現在ユーザーのTODO統計取得](#現在ユーザーのtodo統計取得)
- [特定ユーザーのTODO一覧取得](#特定ユーザーのtodo一覧取得)

---

## 認証について

ほぼすべてのユーザー APIエンドポイントは認証が必要です。リクエスト時には有効なJWTトークンを`Authorization`ヘッダーまたはCookieに含める必要があります。

ミドルウェアにより以下のヘッダーが自動的に設定されます：
- `x-user-id`: 認証されたユーザーのID
- `x-user-role`: 認証されたユーザーの権限レベル

---

## 権限レベルについて

| ロール | 値 | 説明 |
|---|---|---|
| ADMIN | 1 | 管理者：全ユーザーへのフルアクセス権限 |
| MANAGER | 2 | マネージャー：管理者に準じた権限 |
| USER | 3以上 | 一般ユーザー：自分の情報のみアクセス可能 |

---

## ユーザー一覧取得

管理者またはマネージャーのみが全ユーザー一覧を取得できます。
削除ステータスのフィルタリング、複数フィールドでの検索、カスタムソート機能をサポートします。

### エンドポイント

```
GET /api/users
```

### リクエストヘッダー

| ヘッダー | 必須 | 説明 |
|---|---|---|
| `x-user-id` | ✓ | 認証されたユーザーID（ミドルウェアで自動設定） |
| `x-user-role` | ✓ | 認証されたユーザーの権限レベル（ミドルウェアで自動設定） |

### クエリパラメータ

| パラメータ | 型 | 必須 | デフォルト | 説明 | 制約 |
|---|---|---|---|---|---|
| `page` | number | - | 1 | ページ番号 | 1以上 |
| `perPage` | number | - | 20 | 1ページあたりの件数 | 1〜100 |
| `deleted` | string | - | - | 削除ステータスフィルタ | 'true': 削除済みのみ、'false': アクティブのみ、省略: 全て |
| `id` | string | - | - | ID検索（完全一致） | - |
| `username` | string | - | - | ユーザー名検索（部分一致、大文字小文字区別なし） | - |
| `firstName` | string | - | - | 名前検索（部分一致、大文字小文字区別なし） | - |
| `firstNameRuby` | string | - | - | 名前ふりがな検索（部分一致、大文字小文字区別なし） | - |
| `lastName` | string | - | - | 姓検索（部分一致、大文字小文字区別なし） | - |
| `lastNameRuby` | string | - | - | 姓ふりがな検索（部分一致、大文字小文字区別なし） | - |
| `role` | number | - | - | 権限レベル検索（完全一致） | - |
| `sortBy` | string | - | created_at | ソート対象フィールド | id, username, first_name, first_name_ruby, last_name, last_name_ruby, role, created_at |
| `sortOrder` | string | - | asc | ソート順序 | asc（昇順）, desc（降順） |

### レスポンス

**成功時 (200 OK)**

```json
{
  "success": true,
  "message": "ユーザー一覧を取得しました",
  "data": {
    "data": [
      {
        "id": "user-1",
        "username": "john_doe",
        "firstName": "John",
        "lastName": "Doe",
        "role": 1,
        "createdAt": "2025-07-24T10:00:00.000Z",
        "updatedAt": "2025-07-24T10:00:00.000Z"
      }
    ],
    "pagination": {
      "currentPage": 1,
      "totalPages": 5,
      "totalUsers": 100,
      "perPage": 20
    }
  }
}
```

### エラーレスポンス

**バリデーションエラー (400 Bad Request)**

```json
{
  "success": false,
  "message": "クエリパラメータが正しくありません"
}
```

**認証エラー (401 Unauthorized)**

```json
{
  "success": false,
  "message": "Authentication required"
}
```

**権限エラー (403 Forbidden)**

```json
{
  "success": false,
  "message": "管理者権限が必要です"
}
```

**サーバーエラー (500 Internal Server Error)**

```json
{
  "success": false,
  "message": "ユーザー一覧の取得に失敗しました"
}
```

### リクエスト例

**基本的な取得**

```bash
curl -X GET "http://localhost:3000/api/users?page=2&perPage=10" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**アクティブユーザーのみ取得**

```bash
curl -X GET "http://localhost:3000/api/users?deleted=false" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**削除済みユーザーのみ取得**

```bash
curl -X GET "http://localhost:3000/api/users?deleted=true" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**ユーザー名で検索**

```bash
curl -X GET "http://localhost:3000/api/users?username=john" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**名前で検索してソート**

```bash
curl -X GET "http://localhost:3000/api/users?firstName=太郎&sortBy=first_name&sortOrder=asc" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**権限レベルでフィルタリング**

```bash
curl -X GET "http://localhost:3000/api/users?role=1&sortBy=username&sortOrder=asc" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**複数条件での検索**

```bash
curl -X GET "http://localhost:3000/api/users?deleted=false&role=1&sortBy=created_at&sortOrder=desc" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 注意事項

- 管理者（ADMIN=1）またはマネージャー（MANAGER=2）のみアクセス可能
- ユーザー情報にパスワードハッシュは含まれません
- 検索は部分一致で大文字小文字を区別しません（ILIKE検索）
- デフォルトでは削除ステータスに関わらず全てのユーザーを取得します
- `deleted`パラメータを指定することで、アクティブまたは削除済みユーザーのみをフィルタリングできます
- ソートのデフォルトは`created_at`の昇順です
- ページネーションはフィルタリング・ソート後に適用されます

---

## 新規ユーザー作成

管理者またはマネージャーが新規ユーザーを作成します。

### エンドポイント

```
POST /api/users
```

### リクエストヘッダー

| ヘッダー | 必須 | 説明 |
|---|---|---|
| `x-user-id` | ✓ | 認証されたユーザーID（ミドルウェアで自動設定） |
| `x-user-role` | ✓ | 認証されたユーザーの権限レベル（ミドルウェアで自動設定） |
| `Content-Type` | ✓ | application/json |

### リクエストボディ

| フィールド | 型 | 必須 | 説明 | 制約 |
|---|---|---|---|---|
| `username` | string | ✓ | ユーザー名 | 1〜50文字 |
| `password` | string | ✓ | パスワード | 6文字以上 |
| `firstName` | string | - | 名前 | 50文字以下 |
| `firstNameRuby` | string | - | 名前のふりがな | 50文字以下 |
| `lastName` | string | - | 姓 | 50文字以下 |
| `lastNameRuby` | string | - | 姓のふりがな | 50文字以下 |
| `role` | number | - | ユーザーロール | - |

### レスポンス

**成功時 (200 OK)**

```json
{
  "success": true,
  "message": "新規ユーザーを作成しました",
  "data": {
    "id": "user-123",
    "username": "new_employee",
    "firstName": "太郎",
    "lastName": "田中",
    "role": 1,
    "createdAt": "2025-07-24T10:00:00.000Z",
    "updatedAt": "2025-07-24T10:00:00.000Z"
  }
}
```

### エラーレスポンス

**バリデーションエラー (400 Bad Request)**

```json
{
  "success": false,
  "message": "入力データが正しくありません"
}
```

**認証エラー (401 Unauthorized)**

```json
{
  "success": false,
  "message": "Authentication required"
}
```

**権限エラー (403 Forbidden)**

```json
{
  "success": false,
  "message": "管理者権限が必要です"
}
```

**ユーザー名重複エラー (409 Conflict)**

```json
{
  "success": false,
  "message": "このユーザー名は既に使用されています"
}
```

**サーバーエラー (500 Internal Server Error)**

```json
{
  "success": false,
  "message": "ユーザー作成に失敗しました"
}
```

### リクエスト例

```bash
curl -X POST http://localhost:3000/api/users \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "username": "new_employee",
    "firstName": "太郎",
    "lastName": "田中",
    "password": "securePassword123",
    "role": 1
  }'
```

### 注意事項

- 管理者（ADMIN=1）またはマネージャー（MANAGER=2）のみアクセス可能
- パスワードは自動的にハッシュ化されて保存されます
- ユーザー名は一意である必要があります
- レスポンスにパスワードハッシュは含まれません

---

## ユーザー詳細取得

指定されたIDのユーザー情報を取得します。

### エンドポイント

```
GET /api/users/{id}
```

### パラメータ

| パラメータ | 型 | 必須 | 説明 |
|---|---|---|---|
| `id` | string | ✓ | ユーザーID（URLパラメータ） |

### リクエストヘッダー

| ヘッダー | 必須 | 説明 |
|---|---|---|
| `x-user-id` | ✓ | 認証されたユーザーID（ミドルウェアで自動設定） |
| `x-user-role` | ✓ | 認証されたユーザーの権限レベル（ミドルウェアで自動設定） |

### レスポンス

**成功時 (200 OK)**

```json
{
  "success": true,
  "message": "ユーザー情報を取得しました",
  "data": {
    "id": "user-123",
    "username": "john_doe",
    "firstName": "John",
    "lastName": "Doe",
    "role": 1,
    "createdAt": "2025-07-24T10:00:00.000Z",
    "updatedAt": "2025-07-24T10:00:00.000Z"
  }
}
```

### エラーレスポンス

**認証エラー (401 Unauthorized)**

```json
{
  "success": false,
  "message": "Authentication required"
}
```

**権限エラー (403 Forbidden)**

```json
{
  "success": false,
  "message": "このユーザー情報にアクセスする権限がありません"
}
```

**ユーザー未存在エラー (404 Not Found)**

```json
{
  "success": false,
  "message": "ユーザーが見つかりません"
}
```

**サーバーエラー (500 Internal Server Error)**

```json
{
  "success": false,
  "message": "ユーザー情報の取得に失敗しました"
}
```

### リクエスト例

```bash
curl -X GET http://localhost:3000/api/users/user-123 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 注意事項

- 管理者またはマネージャーは全ユーザーアクセス可能
- 一般ユーザーは自分の情報のみアクセス可能
- パスワードハッシュは除外してレスポンス

---

## ユーザー情報更新

指定されたIDのユーザー情報を部分更新します。

### エンドポイント

```
PATCH /api/users/{id}
```

### パラメータ

| パラメータ | 型 | 必須 | 説明 |
|---|---|---|---|
| `id` | string | ✓ | ユーザーID（URLパラメータ） |

### リクエストヘッダー

| ヘッダー | 必須 | 説明 |
|---|---|---|
| `x-user-id` | ✓ | 認証されたユーザーID（ミドルウェアで自動設定） |
| `x-user-role` | ✓ | 認証されたユーザーの権限レベル（ミドルウェアで自動設定） |
| `Content-Type` | ✓ | application/json |

### リクエストボディ

**管理者・マネージャーの場合**

| フィールド | 型 | 必須 | 説明 | 制約 |
|---|---|---|---|---|
| `username` | string | - | ユーザー名 | 1〜50文字 |
| `password` | string | - | パスワード | 6文字以上 |
| `firstName` | string | - | 名前 | 50文字以下 |
| `firstNameRuby` | string | - | 名前のふりがな | 50文字以下 |
| `lastName` | string | - | 姓 | 50文字以下 |
| `lastNameRuby` | string | - | 姓のふりがな | 50文字以下 |
| `role` | number | - | ユーザーロール | - |

**一般ユーザーの場合**

| フィールド | 型 | 必須 | 説明 | 制約 |
|---|---|---|---|---|
| `firstName` | string | - | 名前 | 50文字以下 |
| `firstNameRuby` | string | - | 名前のふりがな | 50文字以下 |
| `lastName` | string | - | 姓 | 50文字以下 |
| `lastNameRuby` | string | - | 姓のふりがな | 50文字以下 |

### レスポンス

**成功時 (200 OK)**

```json
{
  "success": true,
  "message": "ユーザー情報を更新しました",
  "data": {
    "id": "user-123",
    "username": "john_doe",
    "firstName": "新しい名前",
    "lastName": "新しい姓",
    "role": 1,
    "createdAt": "2025-07-24T10:00:00.000Z",
    "updatedAt": "2025-07-24T16:00:00.000Z"
  }
}
```

### エラーレスポンス

**バリデーションエラー (400 Bad Request)**

```json
{
  "success": false,
  "message": "入力データが正しくありません"
}
```

**認証エラー (401 Unauthorized)**

```json
{
  "success": false,
  "message": "Authentication required"
}
```

**権限エラー (403 Forbidden)**

```json
{
  "success": false,
  "message": "このユーザー情報を更新する権限がありません"
}
```

**ユーザー未存在エラー (404 Not Found)**

```json
{
  "success": false,
  "message": "ユーザーが見つかりません"
}
```

**ユーザー名重複エラー (409 Conflict)**

```json
{
  "success": false,
  "message": "このユーザー名は既に使用されています"
}
```

**サーバーエラー (500 Internal Server Error)**

```json
{
  "success": false,
  "message": "ユーザー情報の更新に失敗しました"
}
```

### リクエスト例

```bash
curl -X PATCH http://localhost:3000/api/users/user-123 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "新しい名前",
    "lastName": "新しい姓"
  }'
```

### 注意事項

- 管理者またはマネージャーは全ユーザー更新可能
- 一般ユーザーは自分の情報のみ更新可能
- パスワード更新時は自動ハッシュ化
- ユーザー名重複チェック実施

---

## ユーザー削除

指定されたIDのユーザーを物理削除します。

### エンドポイント

```
DELETE /api/users/{id}
```

### パラメータ

| パラメータ | 型 | 必須 | 説明 |
|---|---|---|---|
| `id` | string | ✓ | ユーザーID（URLパラメータ） |

### リクエストヘッダー

| ヘッダー | 必須 | 説明 |
|---|---|---|
| `x-user-id` | ✓ | 認証されたユーザーID（ミドルウェアで自動設定） |
| `x-user-role` | ✓ | 認証されたユーザーの権限レベル（ミドルウェアで自動設定） |

### レスポンス

**成功時 (200 OK)**

```json
{
  "success": true,
  "message": "ユーザーを削除しました",
  "data": {
    "deleted": true,
    "userId": "user-123"
  }
}
```

### エラーレスポンス

**認証エラー (401 Unauthorized)**

```json
{
  "success": false,
  "message": "Authentication required"
}
```

**権限エラー (403 Forbidden)**

```json
{
  "success": false,
  "message": "管理者またはマネージャー権限が必要です"
}
```

**自己削除エラー (403 Forbidden)**

```json
{
  "success": false,
  "message": "自分自身を削除することはできません"
}
```

**ユーザー未存在エラー (404 Not Found)**

```json
{
  "success": false,
  "message": "削除対象のユーザーが見つかりません"
}
```

**サーバーエラー (500 Internal Server Error)**

```json
{
  "success": false,
  "message": "ユーザーの削除に失敗しました"
}
```

### リクエスト例

```bash
curl -X DELETE http://localhost:3000/api/users/user-123 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 注意事項

- 管理者またはマネージャー権限が必要
- **物理削除のため、この操作は元に戻せません**
- データベースから完全にレコードが削除されます
- 関連するToDoデータもCASCADE設定により自動的に削除されます
- 自分自身の削除は禁止

---

## 現在ユーザー情報取得

認証済みユーザーの自分自身の情報を取得します。

### エンドポイント

```
GET /api/users/me
```

### リクエストヘッダー

| ヘッダー | 必須 | 説明 |
|---|---|---|
| `x-user-id` | ✓ | 認証されたユーザーID（ミドルウェアで自動設定） |

### レスポンス

**成功時 (200 OK)**

```json
{
  "success": true,
  "message": "ユーザー情報を取得しました",
  "data": {
    "id": "user-123",
    "username": "john_doe",
    "firstName": "John",
    "lastName": "Doe",
    "role": 1,
    "createdAt": "2025-07-24T10:00:00.000Z",
    "updatedAt": "2025-07-24T10:00:00.000Z"
  }
}
```

### エラーレスポンス

**認証エラー (401 Unauthorized)**

```json
{
  "success": false,
  "message": "Authentication required"
}
```

**ユーザー未存在エラー (401 Unauthorized)**

```json
{
  "success": false,
  "message": "ユーザー情報が見つかりません"
}
```

**サーバーエラー (500 Internal Server Error)**

```json
{
  "success": false,
  "message": "ユーザー情報の取得に失敗しました"
}
```

### リクエスト例

```bash
curl -X GET http://localhost:3000/api/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 注意事項

- JWTトークンから自動的にユーザーIDを抽出
- パスワードハッシュは自動的に除外
- ユーザーダッシュボードやプロフィール表示に使用

---

## 現在ユーザー情報更新

認証済みユーザーが自分自身の情報を更新します。

### エンドポイント

```
PATCH /api/users/me
```

### リクエストヘッダー

| ヘッダー | 必須 | 説明 |
|---|---|---|
| `x-user-id` | ✓ | 認証されたユーザーID（ミドルウェアで自動設定） |
| `x-user-role` | ✓ | 認証されたユーザーの権限レベル（ミドルウェアで自動設定） |
| `Content-Type` | ✓ | application/json |

### リクエストボディ

| フィールド | 型 | 必須 | 説明 | 制約 |
|---|---|---|---|---|
| `firstName` | string | - | 名前 | 50文字以下 |
| `firstNameRuby` | string | - | 名前のふりがな | 50文字以下 |
| `lastName` | string | - | 姓 | 50文字以下 |
| `lastNameRuby` | string | - | 姓のふりがな | 50文字以下 |

### レスポンス

**成功時 (200 OK)**

```json
{
  "success": true,
  "message": "プロフィールを更新しました",
  "data": {
    "id": "user-123",
    "username": "john_doe",
    "firstName": "新しい名前",
    "firstNameRuby": "あたらしいなまえ",
    "lastName": "新しい姓",
    "lastNameRuby": "あたらしいせい",
    "role": 1,
    "createdAt": "2025-07-24T10:00:00.000Z",
    "updatedAt": "2025-07-24T16:00:00.000Z"
  }
}
```

### エラーレスポンス

**バリデーションエラー (400 Bad Request)**

```json
{
  "success": false,
  "message": "入力データが正しくありません"
}
```

**認証エラー (401 Unauthorized)**

```json
{
  "success": false,
  "message": "Authentication required"
}
```

**ユーザー未存在エラー (401 Unauthorized)**

```json
{
  "success": false,
  "message": "ユーザー情報が見つかりません"
}
```

**サーバーエラー (500 Internal Server Error)**

```json
{
  "success": false,
  "message": "プロフィールの更新に失敗しました"
}
```

### リクエスト例

```bash
curl -X PATCH http://localhost:3000/api/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "新しい名前",
    "lastName": "新しい姓",
    "firstNameRuby": "あたらしいなまえ",
    "lastNameRuby": "あたらしいせい"
  }'
```

### 注意事項

- ユーザーは自分の情報のみ更新可能
- ユーザー名とパスワード変更は別のエンドポイントで処理
- 一般ユーザーは role フィールドを変更不可
- 管理者は `/api/users/[id]` エンドポイントを使用

---

## パスワード変更

認証済みユーザーが自分自身のパスワードを変更します。

### エンドポイント

```
PUT /api/users/me/password
```

### リクエストヘッダー

| ヘッダー | 必須 | 説明 |
|---|---|---|
| `x-user-id` | ✓ | 認証されたユーザーID（ミドルウェアで自動設定） |
| `Content-Type` | ✓ | application/json |

### リクエストボディ

| フィールド | 型 | 必須 | 説明 | 制約 |
|---|---|---|---|---|
| `currentPassword` | string | ✓ | 現在のパスワード | - |
| `newPassword` | string | ✓ | 新しいパスワード | 6文字以上 |
| `confirmPassword` | string | ✓ | 新しいパスワード（確認用） | newPasswordと一致 |

### レスポンス

**成功時 (200 OK)**

```json
{
  "success": true,
  "message": "パスワードを変更しました",
  "data": null
}
```

### エラーレスポンス

**バリデーションエラー (400 Bad Request)**

```json
{
  "success": false,
  "message": "入力データが正しくありません: Password must be at least 6 characters"
}
```

**現在のパスワードエラー (400 Bad Request)**

```json
{
  "success": false,
  "message": "現在のパスワードが間違っています"
}
```

**認証エラー (401 Unauthorized)**

```json
{
  "success": false,
  "message": "Authentication required"
}
```

**ユーザー未存在エラー (401 Unauthorized)**

```json
{
  "success": false,
  "message": "ユーザー情報が見つかりません"
}
```

**サーバーエラー (500 Internal Server Error)**

```json
{
  "success": false,
  "message": "パスワード変更処理中にエラーが発生しました"
}
```

### リクエスト例

```bash
curl -X PUT http://localhost:3000/api/users/me/password \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "currentSecurePassword",
    "newPassword": "newSecurePassword123",
    "confirmPassword": "newSecurePassword123"
  }'
```

### 注意事項

- 現在のパスワード検証必須
- パスワード確認による入力ミス防止
- bcryptによる安全なパスワードハッシュ化
- パスワード変更後、既存セッションは継続

---

## 現在ユーザーのTODO一覧取得

ログインユーザーが作成したTODO一覧を取得します。

### エンドポイント

```
GET /api/users/me/todos
```

### リクエストヘッダー

| ヘッダー | 必須 | 説明 |
|---|---|---|
| `x-user-id` | ✓ | 認証されたユーザーID（ミドルウェアで自動設定） |

### レスポンス

**成功時 (200 OK)**

```json
{
  "success": true,
  "message": "Request successful",
  "data": [
    {
      "id": 1,
      "title": "Todo タイトル",
      "descriptions": "Todo 説明",
      "userId": "user-123",
      "createdAt": "2025-07-30T00:00:00.000Z",
      "updatedAt": "2025-07-30T00:00:00.000Z"
    }
  ]
}
```

### エラーレスポンス

**認証エラー (401 Unauthorized)**

```json
{
  "success": false,
  "message": "Authentication required"
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
curl -X GET http://localhost:3000/api/users/me/todos \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 注意事項

- プロフィール画面でのTODO表示に使用
- 自分のTODOのみ取得可能

---

## 現在ユーザーのTODO統計取得

ログインユーザーのTODO統計情報を取得します。

### エンドポイント

```
GET /api/users/me/todos/stats
```

### リクエストヘッダー

| ヘッダー | 必須 | 説明 |
|---|---|---|
| `x-user-id` | ✓ | 認証されたユーザーID（ミドルウェアで自動設定） |

### レスポンス

**成功時 (200 OK)**

```json
{
  "success": true,
  "message": "Request successful",
  "data": {
    "totalTodos": 10,
    "completedTodos": 6,
    "pendingTodos": 4,
    "completionRate": 60
  }
}
```

### エラーレスポンス

**認証エラー (401 Unauthorized)**

```json
{
  "success": false,
  "message": "Authentication required"
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
curl -X GET http://localhost:3000/api/users/me/todos/stats \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 注意事項

- プロフィール画面での統計表示に使用
- 総数、完了数、進行中数、完了率を含む

---

## 特定ユーザーのTODO一覧取得

指定されたユーザーIDに紐づくTODO一覧を取得します。

### エンドポイント

```
GET /api/users/{id}/todos
```

### パラメータ

| パラメータ | 型 | 必須 | 説明 |
|---|---|---|---|
| `id` | string | ✓ | ユーザーID（URLパラメータ） |

### リクエストヘッダー

| ヘッダー | 必須 | 説明 |
|---|---|---|
| `x-user-id` | ✓ | 認証されたユーザーID（ミドルウェアで自動設定） |
| `x-user-role` | ✓ | 認証されたユーザーの権限レベル（ミドルウェアで自動設定） |

### レスポンス

**成功時 (200 OK)**

```json
{
  "success": true,
  "message": "Request successful",
  "data": [
    {
      "id": 1,
      "title": "Todo タイトル",
      "descriptions": "Todo 説明",
      "userId": "user-123",
      "createdAt": "2025-07-30T00:00:00.000Z",
      "updatedAt": "2025-07-30T00:00:00.000Z"
    }
  ]
}
```

### エラーレスポンス

**認証エラー (401 Unauthorized)**

```json
{
  "success": false,
  "message": "Authentication required"
}
```

**権限エラー (403 Forbidden)**

```json
{
  "success": false,
  "message": "Access denied"
}
```

**ユーザー未存在エラー (404 Not Found)**

```json
{
  "success": false,
  "message": "User not found"
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
curl -X GET http://localhost:3000/api/users/user-123/todos \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 注意事項

- 管理者は全ユーザーのTODOにアクセス可能
- 一般ユーザーは自分のTODOのみアクセス可能
- ユーザー詳細画面でのTODO表示に使用

---

## 共通エラーコード

| ステータスコード | 説明 |
|---|---|
| 200 | リクエスト成功 |
| 400 | 不正なリクエスト（バリデーションエラー） |
| 401 | 認証エラー（認証情報が無効または期限切れ） |
| 403 | アクセス拒否（権限がない） |
| 404 | リソースが見つからない |
| 409 | 競合エラー（ユーザー名重複など） |
| 500 | サーバー内部エラー |

---

## データモデル

### User オブジェクト

| フィールド | 型 | 説明 |
|---|---|---|
| `id` | string | ユーザーID |
| `username` | string | ユーザー名（最大50文字） |
| `firstName` | string \| undefined | 名前（最大50文字、任意） |
| `firstNameRuby` | string \| undefined | 名前のふりがな（最大50文字、任意） |
| `lastName` | string \| undefined | 姓（最大50文字、任意） |
| `lastNameRuby` | string \| undefined | 姓のふりがな（最大50文字、任意） |
| `role` | number | ユーザーロール（1: ADMIN, 2: MANAGER, 3以上: USER） |
| `createdAt` | string | 作成日時（ISO8601形式） |
| `updatedAt` | string | 更新日時（ISO8601形式） |

---

## セキュリティに関する注意事項

- すべてのユーザー APIエンドポイントはHTTPS経由でアクセスすることを推奨します
- JWTトークンは安全に保管し、第三者と共有しないでください
- パスワードは安全にハッシュ化されて保存されます
- 認証トークンの有効期限が切れた場合は、再度ログインが必要です
- 管理者権限の悪用を防ぐため、アクセスログの監視を推奨します
