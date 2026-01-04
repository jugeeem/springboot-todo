-- UUIDの拡張を有効化
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- usersテーブルの作成
CREATE TABLE users (
	id			UUID		NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY

	,username		VARCHAR		NOT NULL UNIQUE
	,first_name		VARCHAR
	,first_name_ruby	VARCHAR
	,last_name		VARCHAR
	,last_name_ruby		VARCHAR
	,role			INTEGER		NOT NULL DEFAULT 8
	,password_hash		VARCHAR		NOT NULL

	,created_at		TIMESTAMPTZ	NOT NULL DEFAULT CURRENT_TIMESTAMP
	,created_by		VARCHAR		NOT NULL DEFAULT 'system'
	,updated_at		TIMESTAMPTZ	NOT NULL DEFAULT CURRENT_TIMESTAMP
	,updated_by		VARCHAR		NOT NULL DEFAULT 'system'
	,deleted		BOOLEAN		NOT NULL DEFAULT FALSE
);

-- インデックスの作成
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_deleted ON users(deleted);


-- todosテーブルの作成
CREATE TABLE todos (
	id		UUID		NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY

	,title		VARCHAR(32)	NOT NULL
	,descriptions	VARCHAR(128)
	,completed	BOOLEAN		NOT NULL DEFAULT FALSE

	,created_at	TIMESTAMPTZ	NOT NULL DEFAULT CURRENT_TIMESTAMP
	,created_by	VARCHAR		NOT NULL DEFAULT 'system'
	,updated_at	TIMESTAMPTZ	NOT NULL DEFAULT CURRENT_TIMESTAMP
	,updated_by	VARCHAR		NOT NULL DEFAULT 'system'
	,deleted	BOOLEAN		NOT NULL DEFAULT FALSE

	,user_id	UUID		NOT NULL REFERENCES users(id) ON DELETE CASCADE
);

-- インデックスの作成
CREATE INDEX idx_todos_user_id ON todos(user_id);
CREATE INDEX idx_todos_deleted ON todos(deleted);
