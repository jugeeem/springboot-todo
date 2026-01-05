# com.api.todos パッケージ

Spring Boot TODO API のメインパッケージです。このパッケージは **純粋なクリーンアーキテクチャ（厳密版）** に基づいて設計されています。

## 📐 アーキテクチャ概要

このプロジェクトは、Robert C. Martin（Uncle Bob）が提唱するクリーンアーキテクチャの原則を厳格に遵守しています。

### 依存関係の方向（The Dependency Rule）

```
外側の層 → 内側の層 への依存のみ許可
内側の層 → 外側の層 への依存は絶対禁止
```

```
┌─────────────────────────────────────────────────┐
│  Presentation層 (presentation/)                  │ 最外層
│  - REST Controllers, DTOs                       │
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

## 📁 パッケージ構成

### 1. domain/ - Domain層（最内層・Pure Java）

**責務**: ビジネスルール・ドメインロジックの集約

```
domain/
├── model/           # エンティティ・値オブジェクト
├── repository/      # リポジトリインターフェース（実装はInfrastructure層）
└── service/         # ドメインサービス
```

**依存関係**:
- ✅ 許可: **いかなる外部ライブラリにも依存しない** (Pure Java)
- ❌ 禁止: Spring、JPA、データベース、外部フレームワークへの依存

**特徴**:
- フレームワークに依存しない純粋なJavaコード
- ビジネスロジックはドメインモデル内に実装
- `@Entity`, `@Service`, `@Component`等のアノテーションは使用しない

### 2. application/ - Application層（Pure Java）

**責務**: ユースケースの実装、ドメインオブジェクトのオーケストレーション

```
application/
├── command/         # Commandオブジェクト（入力）
│   ├── auth/
│   └── todo/
├── dto/             # Resultオブジェクト（出力）
└── usecase/         # ユースケース（Pure Java）
    ├── auth/
    └── todo/
```

**依存関係**:
- ✅ 許可: Domain層のみ
- ❌ 禁止: Infrastructure層、Presentation層への依存
- ❌ 禁止: Spring、JPA等のフレームワークアノテーション（`@Service`, `@Transactional`）

**特徴**:
- UseCaseはPure Javaで実装（`@Service`アノテーションなし）
- Presentation層のDTOは使用せず、Application層専用の`Command`/`Result`を使用
- トランザクション管理はInfrastructure層に委譲

### 3. infrastructure/ - Infrastructure層（Spring依存OK）

**責務**: フレームワーク固有の実装、データベースアクセス、外部サービス連携

```
infrastructure/
├── config/                    # 設定クラス
│   └── UseCaseConfig.java    # UseCaseのDI設定
├── persistence/               # データベースアクセス
│   ├── entity/               # JPA Entity（ドメインモデルと分離）
│   └── repository/           # Repository実装
├── security/                  # セキュリティインフラ（JWT等）
└── service/                   # トランザクション管理ラッパー
```

**依存関係**:
- ✅ 許可: Domain層、Application層への依存
- ✅ 許可: Spring、JPA、PostgreSQL等のフレームワーク依存
- ❌ 禁止: Presentation層への依存

**特徴**:
- JPA EntityはDomain層のエンティティと分離
- UseCaseをラップする`@Service`クラスで`@Transactional`を管理
- `UseCaseConfig`でPure JavaのUseCaseをSpring DIコンテナに登録

### 4. presentation/ - Presentation層（最外層）

**責務**: REST APIエンドポイントの公開、HTTPリクエスト・レスポンスのハンドリング

```
presentation/
├── dto/             # Data Transfer Object
│   └── common/     # 共通DTO
└── rest/            # REST Controller
```

**依存関係**:
- ✅ 許可: Domain層、Application層、Infrastructure層への依存
- ✅ 許可: Spring Web、Jackson等のフレームワーク依存

**特徴**:
- Presentation層のDTOとApplication層のCommand/Resultを相互変換
- Infrastructure層のサービス（トランザクション管理ラッパー）を呼び出し
- ドメインモデルを直接レスポンスとして返さない

## 🔄 データフロー

典型的なAPI呼び出しのフロー:

```
1. Client → Presentation層のDTO (CreateTodoRequest)
   ↓
2. Controller: DTO → Application層のCommand変換 (CreateTodoCommand)
   ↓
3. Infrastructure層のService呼び出し (@Transactional)
   ↓
4. Application層のUseCase実行 (Pure Java)
   ↓
5. Domain層のビジネスロジック実行
   ↓
6. Domain層のRepositoryインターフェース経由でデータ永続化
   ↓
7. Application層のResult返却 (TodoResult)
   ↓
8. Controller: Result → Presentation層のDTO変換 (TodoResponse)
   ↓
9. Presentation層のDTO → Client
```

## 🚨 絶対禁止事項

以下のパターンは **絶対に実装してはいけません**:

### ❌ 1. Domain層でフレームワークアノテーションを使用

```java
// ❌ 絶対禁止
package com.api.todos.domain.model;

import org.springframework.stereotype.Component;
import jakarta.persistence.Entity;

@Component  // ❌ Domain層でSpring依存は禁止
@Entity     // ❌ Domain層でJPA依存は禁止
public class Todo {
    // ...
}
```

### ❌ 2. Application層でフレームワークアノテーションを使用

```java
// ❌ 絶対禁止
package com.api.todos.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service        // ❌ Application層でSpring依存は禁止
public class CreateTodoUseCase {
    @Transactional  // ❌ トランザクション管理はInfrastructure層の責務
    public TodoResult execute(CreateTodoCommand command) {
        // ...
    }
}
```

### ❌ 3. ドメインモデルを直接レスポンスとして返却

```java
// ❌ 絶対禁止
@GetMapping("/{id}")
public ResponseEntity<Todo> getTodo(@PathVariable UUID id) {
    Todo todo = getTodoUseCase.execute(id);
    return ResponseEntity.ok(todo); // ❌ ドメインモデルを直接返却
}

// ✅ 正しい実装
@GetMapping("/{id}")
public ResponseEntity<TodoResponse> getTodo(@PathVariable UUID id) {
    TodoResult result = getTodoUseCase.execute(id);
    TodoResponse response = TodoResponse.from(result);
    return ResponseEntity.ok(response); // ✅ DTOに変換して返却
}
```

### ❌ 4. Infrastructure層の実装詳細をApplication層で参照

```java
// ❌ 絶対禁止
package com.api.todos.application.usecase;

import com.api.todos.infrastructure.persistence.repository.TodoJpaRepository;

public class CreateTodoUseCase {
    private final TodoJpaRepository jpaRepository; // ❌ Infrastructure層の実装に依存
    
    // ✅ 正しくはDomain層のTodoRepositoryインターフェースに依存すべき
}
```

## 📝 実装ガイドライン

### 新しいユースケースを追加する場合

1. **Domain層**: Entity → Repository Interface
2. **Application層**: Command → UseCase (Pure Java) → Result
3. **Infrastructure層**: JPA Entity → Repository実装 → UseCaseConfig登録 → Service（トランザクション管理ラッパー）
4. **Presentation層**: Request DTO → Controller → Response DTO

### 各層での実装のポイント

#### Domain層
- Pure Javaで実装
- ビジネスルールはドメインモデル内に実装
- Repositoryはインターフェースのみ定義

#### Application層
- Pure Javaで実装（`@Service`なし）
- Commandで入力を受け取る
- Resultで出力を返す
- トランザクション管理は行わない

#### Infrastructure層
- JPA EntityはDomain層のエンティティと分離
- Repository実装でDomain層のインターフェースを実装
- UseCaseConfigでUseCaseをBean登録
- ServiceクラスでUseCaseをラップし、`@Transactional`を適用

#### Presentation層
- DTOとCommand/Resultの変換を行う
- Infrastructure層のService（トランザクション管理ラッパー）を呼び出す
- ドメインモデルを直接返さない

## 🧪 テスト戦略

### 層別テスト方針

- **Domain層**: Pure Javaのユニットテスト（モック不要）
- **Application層**: UseCase単体テスト（Repositoryをモック）
- **Infrastructure層**: Repository統合テスト（実DBまたはTestcontainers）
- **Presentation層**: Controller統合テスト（`@WebMvcTest`）

## 🔗 関連ドキュメント

- **[AGENTS.md](../../../../../../../AGENTS.md)** - AIエージェント向けコンテキストドキュメント（詳細な設計原則）
- **[README.md](../../../../../../../README.md)** - プロジェクト全体概要
- **[AUTH.md](../../../../../../../.docs/api/AUTH.md)** - 認証API仕様
- **[USERS.md](../../../../../../../.docs/api/USERS.md)** - ユーザー管理API仕様
- **[TODOS.md](../../../../../../../.docs/api/TODOS.md)** - TODO管理API仕様

## 📞 質問・サポート

実装に関する質問や不明点がある場合は、AGENTS.mdの以下のセクションを参照してください:

- **アーキテクチャ原則**: 「純粋なクリーンアーキテクチャ」セクション
- **実装パターン**: 「実装例」セクション
- **禁止事項**: 「絶対禁止事項」セクション

---

**作成日**: 2025年1月5日  
**対象バージョン**: Spring Boot TODO API v0.0.1-SNAPSHOT  

このドキュメントは、`com.api.todos`パッケージの構造と設計原則を説明するものです。**純粋なクリーンアーキテクチャの原則を厳格に遵守**し、一貫性のある実装を行ってください。
