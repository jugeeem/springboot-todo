# Infrastructure層 - config パッケージ

## 📋 パッケージ概要

**場所**: `api/src/main/java/com/api/todos/infrastructure/config/`

**目的**: アプリケーション全体の設定・DI（依存性注入）管理を行う設定クラスを集約するパッケージです。特に、**Application層のPure JavaクラスをSpringのDIコンテナに登録する**という重要な役割を担っています。

**主要コンポーネント**:
- **UseCaseConfig**: Application層のUseCaseをBean登録（最重要）
- **SecurityConfig**: セキュリティ設定（Spring Security、JWT等）
- **DatabaseConfig**: データベース接続・トランザクション設定
- **その他の設定クラス**: CORS、JSON設定、外部API連携等

---

## 🏛️ クリーンアーキテクチャにおける位置づけ

```
┌─────────────────────────────────────────────────┐
│  Presentation層 (Controllers, REST API)         │
├─────────────────────────────────────────────────┤
│  Infrastructure層 (Persistence, Security)       │
│  ┌───────────────────────────────────────────┐  │
│  │  config/ ← ★ このパッケージ               │  │
│  │  - UseCaseConfig (UseCase Bean登録)       │  │
│  │  - SecurityConfig (認証・認可設定)        │  │
│  │  - DatabaseConfig (DB設定)                │  │
│  └───────────────────────────────────────────┘  │
├─────────────────────────────────────────────────┤
│  Application層 (UseCases - Pure Java)           │
│  ↑ configパッケージがDIコンテナに登録          │
├─────────────────────────────────────────────────┤
│  Domain層 (Entities, Repository Interfaces)     │
└─────────────────────────────────────────────────┘
```

### 依存関係の方向

```
Infrastructure層 (config/)
    ↓ 依存（Bean登録）
Application層 (UseCases - Pure Java)
    ↓ 依存（ビジネスロジック実行）
Domain層 (Repository Interfaces)
```

**重要**: configパッケージは、**Application層のPure Javaクラス（UseCase）をSpringのDIコンテナに登録する橋渡し役**です。UseCaseはSpringに依存しないPure Javaですが、Infrastructure層のconfigクラスがこれらをBean登録することで、Springアプリケーション内で利用可能にします。

---

## 🎯 責務（Responsibilities）

### ✅ このパッケージが行うべきこと

1. **UseCase Bean登録**（最重要）
   - Application層のUseCaseをSpring DIコンテナに登録
   - UseCaseのコンストラクタに必要な依存性（Repository等）を注入

2. **セキュリティ設定**
   - Spring Securityの設定
   - JWT認証・認可の設定
   - パスワードエンコーダー（BCrypt）の設定

3. **データベース設定**
   - データソース設定
   - トランザクションマネージャー設定
   - JPA設定

4. **アプリケーション全体設定**
   - CORS設定
   - JSON Serializerのカスタマイズ
   - 外部API連携設定

### ❌ このパッケージが行ってはいけないこと

1. **ビジネスロジックの実装**
   - 設定クラス内にビジネスルールを記述しない
   - あくまで「Bean登録」「設定」に徹する

2. **データアクセスロジックの実装**
   - Repository実装はpersistence/repository/に配置
   - config内でSQL実行やデータ操作をしない

3. **Controllerロジックの実装**
   - REST APIのエンドポイント処理はPresentation層に配置
   - config内でHTTPリクエスト処理をしない

---

## 🚨 禁止パターン（Anti-Patterns）

### ❌ 1. UseCase内で@Serviceアノテーションを使用

**問題**: Application層のUseCaseにSpringアノテーションを付与すると、Pure Javaではなくなり、フレームワーク依存が発生します。

```java
// ❌ 絶対禁止: UseCase内で@Serviceアノテーションを使用
package com.api.todos.application.usecase.todo;

import org.springframework.stereotype.Service;

@Service // ❌ Application層でSpring依存は禁止
public class CreateTodoUseCase {
    // UseCaseはPure Javaであるべき
}
```

**なぜダメか**:
- Application層がSpring Frameworkに依存してしまう
- クリーンアーキテクチャの「内側の層はフレームワークに依存しない」原則に違反
- テストが困難になる（Springコンテキスト必須）

**正しい実装**:
```java
// ✅ 正しい実装: UseCaseはPure Java
package com.api.todos.application.usecase.todo;

// アノテーションなし - Pure Java
public class CreateTodoUseCase {
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;

    // コンストラクタインジェクション（Spring非依存）
    public CreateTodoUseCase(
        TodoRepository todoRepository,
        UserRepository userRepository
    ) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
    }

    public TodoResult execute(CreateTodoCommand command) {
        // ビジネスロジック実装
    }
}
```

```java
// ✅ 正しい実装: Infrastructure層のUseCaseConfigでBean登録
package com.api.todos.infrastructure.config;

import com.api.todos.application.usecase.todo.CreateTodoUseCase;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.domain.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {
    
    @Bean
    public CreateTodoUseCase createTodoUseCase(
        TodoRepository todoRepository,
        UserRepository userRepository
    ) {
        // Pure JavaのUseCaseをBean登録
        return new CreateTodoUseCase(todoRepository, userRepository);
    }
}
```

---

### ❌ 2. 設定クラス内にビジネスロジックを実装

**問題**: configクラスは「設定」に徹するべきで、ビジネスロジックを含めてはいけません。

```java
// ❌ 絶対禁止: 設定クラス内にビジネスロジックを実装
package com.api.todos.infrastructure.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class TodoConfig {
    
    // ❌ ビジネスロジックを設定クラスに記述
    public boolean validateTodoTitle(String title) {
        if (title == null || title.isEmpty()) {
            return false;
        }
        return title.length() <= 32;
    }
    
    // ❌ データアクセスロジックを設定クラスに記述
    public List<Todo> findActiveTodos(UUID userId) {
        // データベースアクセス処理
    }
}
```

**なぜダメか**:
- 設定クラスの責務を超えている
- ビジネスロジックはDomain層、データアクセスはInfrastructure層のRepository実装に配置すべき
- テスタビリティが低下

**正しい実装**:
```java
// ✅ 正しい実装: 設定クラスは「Bean登録」「設定」のみ
package com.api.todos.infrastructure.config;

import com.api.todos.application.usecase.todo.CreateTodoUseCase;
import com.api.todos.application.usecase.todo.GetTodoListUseCase;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.domain.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {
    
    // ✅ Bean登録のみ（ビジネスロジックなし）
    @Bean
    public CreateTodoUseCase createTodoUseCase(
        TodoRepository todoRepository,
        UserRepository userRepository
    ) {
        return new CreateTodoUseCase(todoRepository, userRepository);
    }
    
    @Bean
    public GetTodoListUseCase getTodoListUseCase(
        TodoRepository todoRepository
    ) {
        return new GetTodoListUseCase(todoRepository);
    }
}
```

---

### ❌ 3. UseCaseConfigでRepository実装を直接注入

**問題**: UseCaseはDomain層のRepositoryインターフェースに依存すべきで、Infrastructure層の実装クラスに直接依存してはいけません。

```java
// ❌ 絶対禁止: Repository実装クラスを直接注入
package com.api.todos.infrastructure.config;

import com.api.todos.application.usecase.todo.CreateTodoUseCase;
import com.api.todos.infrastructure.persistence.repository.TodoRepositoryImpl;
import com.api.todos.infrastructure.persistence.repository.UserRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {
    
    @Bean
    public CreateTodoUseCase createTodoUseCase(
        TodoRepositoryImpl todoRepository,    // ❌ 実装クラスを直接注入
        UserRepositoryImpl userRepository     // ❌ 実装クラスを直接注入
    ) {
        return new CreateTodoUseCase(todoRepository, userRepository);
    }
}
```

**なぜダメか**:
- 依存性逆転の原則（DIP）に違反
- UseCaseがInfrastructure層の実装に依存してしまう
- Repository実装を変更した場合、UseCaseConfigも変更が必要になる

**正しい実装**:
```java
// ✅ 正しい実装: Domain層のインターフェースを注入
package com.api.todos.infrastructure.config;

import com.api.todos.application.usecase.todo.CreateTodoUseCase;
import com.api.todos.domain.repository.TodoRepository;  // ✅ インターフェース
import com.api.todos.domain.repository.UserRepository;  // ✅ インターフェース
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {
    
    @Bean
    public CreateTodoUseCase createTodoUseCase(
        TodoRepository todoRepository,   // ✅ インターフェースを注入
        UserRepository userRepository    // ✅ インターフェースを注入
    ) {
        // SpringのDIコンテナが自動的に実装クラス（TodoRepositoryImpl等）を注入
        return new CreateTodoUseCase(todoRepository, userRepository);
    }
}
```

**Spring DIの仕組み**:
```
1. TodoRepositoryImpl が @Repository アノテーションで登録される
2. TodoRepositoryImpl は TodoRepository インターフェースを実装
3. Spring DIコンテナが「TodoRepository型が必要」と認識
4. 自動的にTodoRepositoryImplインスタンスを注入
```

---

## ✅ 正しい実装パターン

### 1. UseCaseConfig - UseCase Bean登録（最重要）

**目的**: Application層のPure JavaなUseCaseをSpring DIコンテナに登録します。

**実装例**:
```java
// api/src/main/java/com/api/todos/infrastructure/config/UseCaseConfig.java
package com.api.todos.infrastructure.config;

import com.api.todos.application.usecase.auth.GenerateJwtTokenUseCase;
import com.api.todos.application.usecase.auth.InitializePasswordUseCase;
import com.api.todos.application.usecase.todo.CreateTodoUseCase;
import com.api.todos.application.usecase.todo.GetTodoListUseCase;
import com.api.todos.application.usecase.todo.UpdateTodoUseCase;
import com.api.todos.application.usecase.todo.DeleteTodoUseCase;
import com.api.todos.domain.repository.TodoRepository;
import com.api.todos.domain.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * UseCaseのDI設定クラス
 * 
 * Application層のPure JavaなUseCaseをSpring DIコンテナに登録する。
 * UseCaseはSpringに依存しないが、このConfigクラスがBean登録を担当する。
 */
@Configuration
public class UseCaseConfig {
    
    // ========================================
    // 認証関連UseCase
    // ========================================
    
    /**
     * パスワード初期化UseCase Bean登録
     */
    @Bean
    public InitializePasswordUseCase initializePasswordUseCase(
        UserRepository userRepository
    ) {
        return new InitializePasswordUseCase(userRepository);
    }
    
    /**
     * JWTトークン生成UseCase Bean登録
     */
    @Bean
    public GenerateJwtTokenUseCase generateJwtTokenUseCase(
        UserRepository userRepository
    ) {
        return new GenerateJwtTokenUseCase(userRepository);
    }
    
    // ========================================
    // TODO関連UseCase
    // ========================================
    
    /**
     * TODO作成UseCase Bean登録
     */
    @Bean
    public CreateTodoUseCase createTodoUseCase(
        TodoRepository todoRepository,
        UserRepository userRepository
    ) {
        return new CreateTodoUseCase(todoRepository, userRepository);
    }
    
    /**
     * TODO一覧取得UseCase Bean登録
     */
    @Bean
    public GetTodoListUseCase getTodoListUseCase(
        TodoRepository todoRepository
    ) {
        return new GetTodoListUseCase(todoRepository);
    }
    
    /**
     * TODO更新UseCase Bean登録
     */
    @Bean
    public UpdateTodoUseCase updateTodoUseCase(
        TodoRepository todoRepository
    ) {
        return new UpdateTodoUseCase(todoRepository);
    }
    
    /**
     * TODO削除UseCase Bean登録
     */
    @Bean
    public DeleteTodoUseCase deleteTodoUseCase(
        TodoRepository todoRepository
    ) {
        return new DeleteTodoUseCase(todoRepository);
    }
}
```

**重要ポイント**:
1. **@Configuration**: このクラスが設定クラスであることを宣言
2. **@Bean**: 各メソッドが返すオブジェクトをSpring DIコンテナに登録
3. **コンストラクタインジェクション**: UseCaseのコンストラクタに必要な依存性（Repository）を注入
4. **インターフェース依存**: `TodoRepository`, `UserRepository`はDomain層のインターフェース
5. **Pure Java維持**: UseCase自体はSpringに依存しない

---

### 2. SecurityConfig - セキュリティ設定

**目的**: Spring Securityの設定、JWT認証・認可、パスワードエンコーダー設定を行います。

**実装例**:
```java
// api/src/main/java/com/api/todos/infrastructure/config/SecurityConfig.java
package com.api.todos.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * セキュリティ設定クラス
 * 
 * Spring Securityの設定、JWT認証・認可、パスワードエンコーダー設定を行う。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    /**
     * パスワードエンコーダー（BCrypt）
     * 
     * BCryptを使用したパスワードハッシュ化を提供する。
     * ストレングス（コスト）は10（デフォルト）を使用。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * セキュリティフィルターチェーン設定
     * 
     * HTTP Security設定を行う。
     * - JWT認証を使用するため、セッションはSTATELESS
     * - CSRF保護は無効（REST APIのため）
     * - 認証不要のエンドポイント（/api/auth/**）を許可
     * - その他のエンドポイントは認証必須
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF保護無効（JWT認証のため）
            .csrf(csrf -> csrf.disable())
            
            // セッション管理（STATELESS - JWTのため）
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 認可設定
            .authorizeHttpRequests(auth -> auth
                // 認証不要エンドポイント
                .requestMatchers("/api/auth/**").permitAll()
                // その他のエンドポイントは認証必須
                .anyRequest().authenticated()
            );
        
        return http.build();
    }
}
```

**重要ポイント**:
1. **BCryptPasswordEncoder**: パスワードハッシュ化にBCryptを使用（ストレングス10）
2. **STATELESS Session**: JWT認証のため、サーバー側でセッションを保持しない
3. **CSRF無効化**: REST APIでJWT認証を使用するため、CSRF保護は不要
4. **認可設定**: `/api/auth/**`は認証不要、その他は認証必須

---

### 3. DatabaseConfig - データベース設定

**目的**: データソース、トランザクションマネージャー、JPA設定を行います。

**実装例**:
```java
// api/src/main/java/com/api/todos/infrastructure/config/DatabaseConfig.java
package com.api.todos.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * データベース設定クラス
 * 
 * JPA Repository、トランザクション管理を有効化する。
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "com.api.todos.infrastructure.persistence.repository"
)
@EnableTransactionManagement
public class DatabaseConfig {
    
    // データソース設定はapplication.propertiesで管理
    // spring.datasource.url=jdbc:postgresql://localhost:5432/todos_db
    // spring.datasource.username=todos_user
    // spring.datasource.password=todos_password
    // spring.jpa.hibernate.ddl-auto=validate
    // spring.jpa.show-sql=true
}
```

**重要ポイント**:
1. **@EnableJpaRepositories**: JPA Repositoryを有効化（basePackages指定）
2. **@EnableTransactionManagement**: トランザクション管理を有効化
3. **データソース設定**: `application.properties`で管理（ハードコードしない）

---

### 4. CorsConfig - CORS設定

**目的**: フロントエンドからのクロスオリジンリクエストを許可します。

**実装例**:
```java
// api/src/main/java/com/api/todos/infrastructure/config/CorsConfig.java
package com.api.todos.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * CORS設定クラス
 * 
 * フロントエンドからのクロスオリジンリクエストを許可する。
 */
@Configuration
public class CorsConfig {
    
    /**
     * CORSフィルター設定
     * 
     * 開発環境用の設定（本番環境では適切に制限すること）
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 許可するオリジン（開発環境: localhost:3000等）
        config.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:8080"
        ));
        
        // 許可するHTTPメソッド
        config.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        
        // 許可するヘッダー
        config.setAllowedHeaders(Arrays.asList("*"));
        
        // 認証情報を含むリクエストを許可
        config.setAllowCredentials(true);
        
        // Preflight リクエストのキャッシュ時間（秒）
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        
        return new CorsFilter(source);
    }
}
```

**重要ポイント**:
1. **AllowedOrigins**: フロントエンドのURL（本番環境では厳密に制限）
2. **AllowedMethods**: 許可するHTTPメソッド
3. **AllowCredentials**: Cookie、Authorization headerを許可
4. **MaxAge**: Preflightリクエストのキャッシュ時間

**セキュリティ注意**:
- 本番環境では`setAllowedOrigins`を厳密に制限する
- `setAllowedHeaders(Arrays.asList("*"))`は開発用、本番では具体的に指定

---

### 5. JacksonConfig - JSON設定

**目的**: JSON SerializationのカスタマイズをRESTful API全体に適用します。

**実装例**:
```java
// api/src/main/java/com/api/todos/infrastructure/config/JacksonConfig.java
package com.api.todos.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson（JSON）設定クラス
 * 
 * JSON Serialization/Deserializationの設定をカスタマイズする。
 */
@Configuration
public class JacksonConfig {
    
    /**
     * ObjectMapper Bean登録
     * 
     * - JavaTimeModuleを登録（LocalDateTime等のシリアライズ対応）
     * - snake_case命名規則を使用
     * - タイムスタンプをISO-8601形式で出力
     */
    @Bean
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
            // Java 8日時API対応
            .modules(new JavaTimeModule())
            
            // タイムスタンプをISO-8601形式で出力（数値ではなく文字列）
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            
            // snake_case命名規則（オプション - プロジェクトポリシーに従う）
            // .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            
            .build();
    }
}
```

**重要ポイント**:
1. **JavaTimeModule**: `LocalDateTime`, `LocalDate`等のJava 8日時APIをサポート
2. **WRITE_DATES_AS_TIMESTAMPS無効**: タイムスタンプを`"2024-01-05T10:30:00"`形式で出力
3. **命名規則**: snake_case/camelCaseはプロジェクトポリシーに従う

---

## 🔄 データフロー - Bean登録とDI

```
┌────────────────────────────────────────────────────────────────┐
│ Spring起動時                                                    │
├────────────────────────────────────────────────────────────────┤
│ 1. Infrastructure層のRepository実装をBean登録                  │
│    - TodoRepositoryImpl (@Repository)                          │
│    - UserRepositoryImpl (@Repository)                          │
│                                                                │
│ 2. UseCaseConfig実行                                           │
│    - createTodoUseCase(@Bean)メソッド実行                      │
│    - SpringがTodoRepository実装（TodoRepositoryImpl）を自動注入 │
│    - CreateTodoUseCaseインスタンスをBean登録                   │
│                                                                │
│ 3. Infrastructure層のService登録                               │
│    - CreateTodoService (@Service)                              │
│    - @Autowiredでcreatetod UseCaseを注入                      │
│                                                                │
│ 4. Presentation層のController登録                              │
│    - TodoController (@RestController)                          │
│    - @AutowiredでCreateTodoServiceを注入                       │
└────────────────────────────────────────────────────────────────┘
```

```
┌────────────────────────────────────────────────────────────────┐
│ 実行時のデータフロー                                            │
├────────────────────────────────────────────────────────────────┤
│ 1. Client → Controller (Presentation層)                        │
│    POST /api/todos { "title": "タスク", "descriptions": "" }  │
│                                                                │
│ 2. Controller → Service (Infrastructure層)                     │
│    CreateTodoService.execute(command)                          │
│    ※ @Transactional適用                                        │
│                                                                │
│ 3. Service → UseCase (Application層)                           │
│    CreateTodoUseCase.execute(command)                          │
│    ※ Pure Javaで実装                                           │
│                                                                │
│ 4. UseCase → Repository (Domain層インターフェース)              │
│    todoRepository.save(todo)                                   │
│                                                                │
│ 5. Repository実装 → Database (Infrastructure層)                │
│    TodoRepositoryImpl → TodoJpaRepository → PostgreSQL         │
│                                                                │
│ 6. 結果を逆順に返却                                             │
│    Database → Repository → UseCase → Service → Controller      │
└────────────────────────────────────────────────────────────────┘
```

---

## 🔒 セキュリティ考慮事項

### 1. パスワードエンコーダー（BCrypt）

**実装**:
```java
@Bean
public PasswordEncoder passwordEncoder() {
    // BCryptストレングス10（デフォルト）
    return new BCryptPasswordEncoder();
}
```

**使用例**:
```java
// Domain/Application層でパスワードハッシュ化
public class InitializePasswordUseCase {
    private final PasswordEncoder passwordEncoder;
    
    public InitializePasswordUseCase(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    public UserResult execute(InitializePasswordCommand command) {
        // 平文パスワードをハッシュ化
        String hashedPassword = passwordEncoder.encode(command.getPassword());
        
        // ハッシュ化されたパスワードのみを保存
        User user = new User(
            command.getUserId(),
            command.getUsername(),
            hashedPassword  // ✅ ハッシュ化済み
        );
        
        return userRepository.save(user);
    }
}
```

**重要**:
- **平文パスワードは絶対に保存しない**
- **BCryptストレングスは10以上を推奨**（デフォルト10）
- **パスワードハッシュは256文字以上のカラムに保存**

---

### 2. JWT認証設定

**SessionCreationPolicy.STATELESS**:
```java
.sessionManagement(session -> 
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```

- サーバー側でセッションを保持しない
- JWTトークンのみで認証状態を管理
- スケーラビリティ向上（サーバーレス対応）

---

### 3. CSRF保護無効化（JWT認証時）

**設定**:
```java
.csrf(csrf -> csrf.disable())
```

**理由**:
- JWT認証ではCSRF攻撃のリスクが低い
- JWTトークンはHTTPヘッダー（Authorization: Bearer <token>）で送信
- JavaScriptから直接アクセス不可（HttpOnly Cookie使用時は別途考慮）

**注意**:
- Cookie based認証の場合はCSRF保護を有効にする
- JWTをlocalStorageに保存する場合はXSS対策を徹底

---

### 4. CORS設定（本番環境）

**開発環境**:
```java
config.setAllowedOrigins(Arrays.asList(
    "http://localhost:3000",
    "http://localhost:8080"
));
```

**本番環境**:
```java
config.setAllowedOrigins(Arrays.asList(
    "https://yourdomain.com",
    "https://www.yourdomain.com"
));
```

**重要**:
- `"*"`（全て許可）は本番環境では **絶対に使用しない**
- 信頼できるオリジンのみを許可
- `setAllowCredentials(true)`使用時は`"*"`不可

---

## 🧪 テスト戦略

### 1. UseCaseConfig テスト

**目的**: UseCaseが正しくBean登録されているかテストします。

**実装例**:
```java
package com.api.todos.infrastructure.config;

import com.api.todos.application.usecase.todo.CreateTodoUseCase;
import com.api.todos.application.usecase.todo.GetTodoListUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UseCaseConfig テスト
 * 
 * UseCaseが正しくSpring DIコンテナに登録されているかテストする。
 */
@SpringBootTest
class UseCaseConfigTest {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Test
    void createTodoUseCaseがBean登録されていること() {
        // UseCaseがSpringコンテナに登録されているか確認
        CreateTodoUseCase useCase = applicationContext.getBean(CreateTodoUseCase.class);
        
        assertThat(useCase).isNotNull();
    }
    
    @Test
    void getTodoListUseCaseがBean登録されていること() {
        GetTodoListUseCase useCase = applicationContext.getBean(GetTodoListUseCase.class);
        
        assertThat(useCase).isNotNull();
    }
    
    @Test
    void 全てのUseCaseがBean登録されていること() {
        // 各UseCaseのBean登録を確認
        assertThat(applicationContext.getBean(CreateTodoUseCase.class)).isNotNull();
        assertThat(applicationContext.getBean(GetTodoListUseCase.class)).isNotNull();
        // 他のUseCaseも同様にテスト
    }
}
```

---

### 2. SecurityConfig テスト

**目的**: セキュリティ設定が正しく動作するかテストします。

**実装例**:
```java
package com.api.todos.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SecurityConfig テスト
 * 
 * 認証・認可設定が正しく動作するかテストする。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void 認証なしで_api_auth_エンドポイントにアクセス可能() throws Exception {
        mockMvc.perform(post("/api/auth/login"))
            .andExpect(status().isOk());  // または4xx（リクエストボディ不正時）
    }
    
    @Test
    void 認証なしで_api_todos_エンドポイントにアクセス不可() throws Exception {
        mockMvc.perform(get("/api/todos"))
            .andExpect(status().isUnauthorized());  // 401 Unauthorized
    }
    
    @Test
    void JWTトークンありで_api_todos_エンドポイントにアクセス可能() throws Exception {
        String jwtToken = "valid_jwt_token";  // テスト用JWTトークン生成
        
        mockMvc.perform(get("/api/todos")
            .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk());
    }
}
```

---

### 3. PasswordEncoder テスト

**目的**: BCryptパスワードエンコーダーが正しく動作するかテストします。

**実装例**:
```java
package com.api.todos.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PasswordEncoder テスト
 * 
 * BCryptパスワードエンコーダーの動作確認。
 */
@SpringBootTest
class PasswordEncoderTest {
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Test
    void パスワードをハッシュ化できること() {
        String rawPassword = "password123";
        
        String hashedPassword = passwordEncoder.encode(rawPassword);
        
        // ハッシュ化されたパスワードは元のパスワードと異なる
        assertThat(hashedPassword).isNotEqualTo(rawPassword);
        // BCryptハッシュは60文字
        assertThat(hashedPassword).hasSize(60);
    }
    
    @Test
    void ハッシュ化されたパスワードを検証できること() {
        String rawPassword = "password123";
        String hashedPassword = passwordEncoder.encode(rawPassword);
        
        // 正しいパスワード
        boolean matches = passwordEncoder.matches(rawPassword, hashedPassword);
        assertThat(matches).isTrue();
        
        // 間違ったパスワード
        boolean notMatches = passwordEncoder.matches("wrongPassword", hashedPassword);
        assertThat(notMatches).isFalse();
    }
    
    @Test
    void 同じパスワードでも異なるハッシュが生成されること() {
        String rawPassword = "password123";
        
        String hash1 = passwordEncoder.encode(rawPassword);
        String hash2 = passwordEncoder.encode(rawPassword);
        
        // BCryptはソルト付きなので、同じパスワードでも異なるハッシュ
        assertThat(hash1).isNotEqualTo(hash2);
        
        // しかし両方とも検証成功
        assertThat(passwordEncoder.matches(rawPassword, hash1)).isTrue();
        assertThat(passwordEncoder.matches(rawPassword, hash2)).isTrue();
    }
}
```

---

## ✅ 実装チェックリスト

### UseCaseConfig実装時

- [ ] `@Configuration`アノテーションを付与
- [ ] 各UseCaseを`@Bean`メソッドで登録
- [ ] UseCaseのコンストラクタに必要な依存性（Repository）を注入
- [ ] **Domain層のインターフェース**を注入（実装クラスではない）
- [ ] UseCaseはPure Javaで実装（@Serviceアノテーションなし）
- [ ] ビジネスロジックをConfig内に記述していないか確認

### SecurityConfig実装時

- [ ] `@Configuration`と`@EnableWebSecurity`を付与
- [ ] `passwordEncoder()`メソッドでBCryptを設定
- [ ] `securityFilterChain()`でHTTP Security設定
- [ ] JWT認証の場合は`SessionCreationPolicy.STATELESS`
- [ ] 認証不要エンドポイント（/api/auth/**）を許可
- [ ] CSRF保護の有効/無効を適切に設定

### DatabaseConfig実装時

- [ ] `@Configuration`アノテーションを付与
- [ ] `@EnableJpaRepositories`でbasePackages指定
- [ ] `@EnableTransactionManagement`を有効化
- [ ] データソース設定は`application.properties`で管理

### CorsConfig実装時

- [ ] `@Configuration`アノテーションを付与
- [ ] `corsFilter()`メソッドでCorsFilterを設定
- [ ] 本番環境では`allowedOrigins`を厳密に制限
- [ ] `allowCredentials`を適切に設定

### 禁止事項チェック

- [ ] Application層のUseCase内で`@Service`アノテーションを使用していない
- [ ] Config内にビジネスロジックを記述していない
- [ ] Repository実装クラスを直接注入していない（インターフェース注入）
- [ ] 平文パスワードを保存していない
- [ ] 本番環境でCORS `"*"`を使用していない

### 対応する他のコンポーネント

- [ ] **Application層**: UseCase（Pure Java）が実装済み
- [ ] **Infrastructure層**: Repository実装（@Repository）が実装済み
- [ ] **Infrastructure層**: Service（@Service, @Transactional）が実装済み
- [ ] **Presentation層**: Controller（@RestController）が実装済み
- [ ] **application.properties**: データソース設定、JPA設定が記述済み

### テスト実装

- [ ] UseCaseConfigテスト: Bean登録確認
- [ ] SecurityConfigテスト: 認証・認可動作確認
- [ ] PasswordEncoderテスト: BCryptハッシュ化・検証確認
- [ ] CorsConfigテスト: CORS設定動作確認（オプション）

---

## 📚 参考資料

### プロジェクト内ドキュメント
- **[AGENTS.md](../../../../../../AGENTS.md)** - AIエージェント向けコンテキストドキュメント
- **[Application層 README](../../application/README.md)** - UseCase実装パターン
- **[Infrastructure層 persistence README](../persistence/README.md)** - Repository実装パターン
- **[Presentation層 REST README](../../presentation/rest/README.md)** - Controller実装パターン

### 外部参考資料
- [Spring Configuration Documentation](https://docs.spring.io/spring-framework/reference/core/beans/java.html)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [Spring Boot Configuration Properties](https://docs.spring.io/spring-boot/reference/features/external-config.html)
- [BCrypt Password Encoder](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html#authentication-password-storage-bcrypt)

---

## 🎯 まとめ

Infrastructure層の`config`パッケージは、**Application層のPure JavaなUseCaseをSpring DIコンテナに登録する橋渡し役**として、クリーンアーキテクチャを実現する上で極めて重要な役割を果たします。

### 重要ポイント

1. **UseCaseConfig**（最重要）
   - Application層のUseCaseをBean登録
   - UseCaseはSpringに依存しないPure Java
   - Domain層のインターフェースを注入（実装クラスではない）

2. **SecurityConfig**
   - BCryptでパスワードハッシュ化
   - JWT認証の場合はSTATELESS Session
   - CSRF保護は認証方式に応じて設定

3. **その他の設定**
   - DatabaseConfig: JPA Repository、トランザクション管理
   - CorsConfig: クロスオリジンリクエスト許可（本番環境では厳格に）
   - JacksonConfig: JSON Serialization設定

### 禁止事項

- ❌ Application層のUseCase内で`@Service`アノテーションを使用
- ❌ Config内にビジネスロジックを記述
- ❌ Repository実装クラスを直接注入（インターフェース注入すべき）
- ❌ 平文パスワードの保存
- ❌ 本番環境でCORS `"*"`を使用

このREADMEを参考に、クリーンアーキテクチャの原則を遵守した設定クラスを実装してください。

---

**作成日**: 2026年1月5日  
**対象バージョン**: Spring Boot TODO API v0.0.1-SNAPSHOT  
**対象パッケージ**: `com.api.todos.infrastructure.config`
