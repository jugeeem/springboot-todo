# データベースリレーション図

## ERダイアグラム

```mermaid
erDiagram
    users ||--o{ todos : "has many"
    
    users {
        UUID id PK "プライマリーキー"
        VARCHAR username UK "ユーザー名(一意)"
        VARCHAR first_name "名"
        VARCHAR first_name_ruby "名(フリガナ)"
        VARCHAR last_name "姓"
        VARCHAR last_name_ruby "姓(フリガナ)"
        INTEGER role "ロール(デフォルト:8)"
        VARCHAR password_hash "パスワードハッシュ"
        TIMESTAMPTZ created_at "作成日時"
        VARCHAR created_by "作成者"
        TIMESTAMPTZ updated_at "更新日時"
        VARCHAR updated_by "更新者"
        BOOLEAN deleted "削除フラグ"
    }
    
    todos {
        UUID id PK "プライマリーキー"
        VARCHAR title "タイトル(最大32文字)"
        VARCHAR descriptions "説明(最大128文字)"
        BOOLEAN completed "完了フラグ(デフォルト:false)"
        TIMESTAMPTZ created_at "作成日時"
        VARCHAR created_by "作成者"
        TIMESTAMPTZ updated_at "更新日時"
        VARCHAR updated_by "更新者"
        BOOLEAN deleted "削除フラグ"
        UUID user_id FK "ユーザーID"
    }
```

## テーブル概要

### users テーブル
ユーザー情報を管理するテーブル。

**主要カラム:**
- `id`: UUID型のプライマリーキー(自動生成)
- `username`: 一意のユーザー名
- `role`: ユーザーロール(デフォルト: 8)
- `password_hash`: ハッシュ化されたパスワード

**インデックス:**
- `idx_users_username`: username列にインデックス
- `idx_users_deleted`: deleted列にインデックス

### todos テーブル
TODO項目を管理するテーブル。

**主要カラム:**
- `id`: UUID型のプライマリーキー(自動生成)
- `title`: TODOのタイトル(最大32文字)
- `descriptions`: TODOの説明(最大128文字)
- `completed`: 完了状態(デフォルト: false)
- `user_id`: ユーザーへの外部キー

**インデックス:**
- `idx_todos_user_id`: user_id列にインデックス
- `idx_todos_deleted`: deleted列にインデックス

## リレーション

### users → todos (1対多)
- 1人のユーザーは複数のTODOを持つことができます
- `todos.user_id` が `users.id` を参照します
- `ON DELETE CASCADE`: ユーザーが削除されると、関連するTODOも自動的に削除されます

## 共通カラム

両テーブルには以下の監査用カラムが共通して含まれています:
- `created_at`: レコード作成日時(デフォルト: 現在時刻)
- `created_by`: レコード作成者(デフォルト: 'system')
- `updated_at`: レコード更新日時(デフォルト: 現在時刻)
- `updated_by`: レコード更新者(デフォルト: 'system')
- `deleted`: 論理削除フラグ(デフォルト: false)
