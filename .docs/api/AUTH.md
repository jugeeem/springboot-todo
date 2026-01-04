# 認証 API ドキュメント

このドキュメントは、ユーザー認証に関連するAPIエンドポイントを説明します。

---

## 目次

- [ユーザー登録](#ユーザー登録)
- [ユーザーログイン](#ユーザーログイン)
- [ユーザーログアウト](#ユーザーログアウト)

---

## ユーザー登録

新しいユーザーアカウントを作成します。

### エンドポイント

```
POST /api/auth/register
```

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
  "message": "Registration successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 2,
      "username": "taro_yamada",
      "firstName": "太郎",
      "firstNameRuby": "たろう",
      "lastName": "山田",
      "lastNameRuby": "やまだ",
      "role": 1
    }
  }
}
```

### エラーレスポンス

**バリデーションエラー (400 Bad Request)**

```json
{
  "success": false,
  "message": "Invalid input: Username is required, Password must be at least 6 characters"
}
```

**ユーザー名重複エラー (400 Bad Request)**

```json
{
  "success": false,
  "message": "Username already exists"
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
curl -X POST http://localhost:3000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jane_doe",
    "password": "securePassword123"
  }'
```

**全項目**

```bash
curl -X POST http://localhost:3000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "taro_yamada",
    "password": "mySecretPassword456",
    "firstName": "太郎",
    "firstNameRuby": "たろう",
    "lastName": "山田",
    "lastNameRuby": "やまだ",
    "role": 1
  }'
```

### 注意事項

- ユーザー名は一意である必要があります
- 登録成功時、認証情報がCookieに自動的に設定されます
- パスワードは安全にハッシュ化されて保存されます

---

## ユーザーログイン

ユーザー名とパスワードを使用してユーザー認証を行い、成功時にはJWTトークンを返却します。

### エンドポイント

```
POST /api/auth/login
```

### リクエストボディ

| フィールド | 型 | 必須 | 説明 | 制約 |
|---|---|---|---|---|
| `username` | string | ✓ | ユーザー名 | 1文字以上 |
| `password` | string | ✓ | パスワード | 1文字以上 |

### レスポンス

**成功時 (200 OK)**

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "username": "john_doe",
      "firstName": "John",
      "lastName": "Doe",
      "role": 1
    }
  }
}
```

### エラーレスポンス

**バリデーションエラー (400 Bad Request)**

```json
{
  "success": false,
  "message": "Invalid input: Username is required"
}
```

**認証エラー (401 Unauthorized)**

```json
{
  "success": false,
  "message": "Invalid username or password"
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
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "securePassword123"
  }'
```

### 注意事項

- ログイン成功時、認証情報がCookieに自動的に設定されます
- JWTトークンには有効期限があります
- パスワードは平文で送信されますが、HTTPS通信を使用することを推奨します

---

## ユーザーログアウト

ユーザーのログアウト処理を行い、認証Cookieを削除します。

### エンドポイント

```
POST /api/auth/logout
```

### リクエストボディ

リクエストボディは不要です。

### レスポンス

**成功時 (200 OK)**

```json
{
  "success": true,
  "message": "Logout successful"
}
```

### エラーレスポンス

**サーバーエラー (500 Internal Server Error)**

```json
{
  "success": false,
  "message": "Logout failed"
}
```

### リクエスト例

```bash
curl -X POST http://localhost:3000/api/auth/logout
```

### 注意事項

- ログアウト処理により、すべての認証Cookieが削除されます
- ログアウト後は、保護されたAPIエンドポイントへのアクセスができなくなります
- クライアント側でもトークンを適切に削除することを推奨します

---

## 共通エラーコード

| ステータスコード | 説明 |
|---|---|
| 200 | リクエスト成功 |
| 400 | 不正なリクエスト（バリデーションエラー） |
| 401 | 認証エラー（認証情報が無効または期限切れ） |
| 500 | サーバー内部エラー |

---

## セキュリティに関する注意事項

- すべての認証エンドポイントはHTTPS経由でアクセスすることを推奨します
- パスワードは安全にハッシュ化されて保存されます
- JWTトークンは安全に保管し、第三者と共有しないでください
- Cookie は HttpOnly 属性が設定されており、XSS攻撃から保護されています
