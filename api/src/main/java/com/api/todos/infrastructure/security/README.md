# security パッケージ - セキュリティインフラストラクチャ

Infrastructure層のsecurityパッケージです。このパッケージは **認証・認可・セキュリティ関連のインフラストラクチャ実装** を担当します。

## 🎯 securityパッケージの役割

### 責務

1. **JWT認証**: トークンの生成・検証・解析
2. **パスワード管理**: ハッシュ化・検証
3. **認証フィルター**: HTTPリクエストの認証処理
4. **Spring Security設定**: セキュリティポリシーの実装

### クリーンアーキテクチャにおける位置づけ

```
┌─────────────────────────────────────────────────┐
│  Presentation層                                  │
│  - Controllers (認証・認可の適用)                │
├─────────────────────────────────────────────────┤
│  Infrastructure層 - security/ ← ここ             │
│  - JWT Provider, Password Encoder, Filter       │
├─────────────────────────────────────────────────┤
│  Application層                                   │
│  - Use Cases (ビジネスロジック)                  │
├─────────────────────────────────────────────────┤
│  Domain層                                        │
│  - User Entity (Pure Java)                      │
└─────────────────────────────────────────────────┘
```

**重要な設計方針**：
- セキュリティインフラはInfrastructure層で実装
- Domain層のUserエンティティは純粋なJava（認証ロジックなし）
- Spring Securityへの依存はInfrastructure層に限定

## 📁 パッケージ構成

```
security/
├── JwtTokenProvider.java         # JWTトークンの生成・検証
├── PasswordEncoderConfig.java    # BCryptPasswordEncoderのBean定義
├── SecurityConfig.java           # Spring Security設定
└── filter/
    └── JwtAuthenticationFilter.java  # JWT認証フィルター
```

## 🚨 絶対禁止事項

### ❌ 1. Domain層にセキュリティロジックを実装

```java
// ❌ 絶対禁止: Domain層のUserエンティティにSpring Securityの依存
package com.api.todos.domain.model;

import org.springframework.security.core.userdetails.UserDetails;  // ❌ Spring Security依存

public class User implements UserDetails {  // ❌ Domain層でフレームワーク依存
    // ...
}

// ✅ 正しい実装: Domain層はPure Java
package com.api.todos.domain.model;

public class User {  // ✅ Pure Java
    private final UUID id;
    private String username;
    private String hashedPassword;  // ハッシュ化済みパスワードを保持するだけ
    
    // ビジネスルール: パスワード初期化確認
    public boolean isPasswordInitialized() {
        return this.passwordInitialized;
    }
}
```

### ❌ 2. Application層でJWT操作を実装

```java
// ❌ 絶対禁止: Application層のUseCaseでJWT生成
package com.api.todos.application.usecase.auth;

import io.jsonwebtoken.Jwts;  // ❌ Application層でJWT依存

public class LoginUseCase {
    public AuthResult execute(LoginCommand command) {
        // ❌ UseCaseでJWT生成ロジックを実装
        String token = Jwts.builder()
            .setSubject(userId.toString())
            .signWith(key)
            .compact();
        
        return new AuthResult(token);
    }
}

// ✅ 正しい実装: Infrastructure層のJwtTokenProviderに委譲
package com.api.todos.application.usecase.auth;

public class GenerateJwtTokenUseCase {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;  // ✅ Infrastructure層のコンポーネント
    
    public AuthResult execute(GenerateJwtTokenCommand command) {
        // ユーザー情報取得
        User user = userRepository.findById(command.getUserId())
            .orElseThrow(() -> new UserNotFoundException("ユーザーが見つかりません"));
        
        // Infrastructure層のJwtTokenProviderに委譲
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        
        return new AuthResult(token, user.getId(), user.getUsername());
    }
}
```

### ❌ 3. パスワードを平文で扱う

```java
// ❌ 絶対禁止: パスワードを平文で保存・比較
public class CreateUserUseCase {
    public UserResult execute(CreateUserCommand command) {
        User user = new User(
            UUID.randomUUID(),
            command.getUsername(),
            command.getPassword()  // ❌ 平文パスワードをそのまま保存
        );
        return userRepository.save(user);
    }
}

// ✅ 正しい実装: パスワードをハッシュ化して保存
public class CreateUserUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;  // ✅ Infrastructure層のコンポーネント
    
    public UserResult execute(CreateUserCommand command) {
        // パスワードをハッシュ化
        String hashedPassword = passwordEncoder.encode(command.getPassword());
        
        User user = new User(
            UUID.randomUUID(),
            command.getUsername(),
            hashedPassword  // ✅ ハッシュ化済みパスワード
        );
        return userRepository.save(user);
    }
}
```

## ✅ 正しい実装パターン

### 1. JwtTokenProvider（JWT管理）

```java
package com.api.todos.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

/**
 * JWTトークンの生成・検証を担当するコンポーネント
 *
 * <p>【責務】
 *
 * <ul>
 *   <li>JWTトークンの生成
 *   <li>JWTトークンの検証（署名・有効期限）
 *   <li>トークンからユーザー情報の抽出
 * </ul>
 *
 * <p>【設計原則】
 *
 * <ul>
 *   <li>Infrastructure層でJWT操作を集約
 *   <li>Application層やDomain層はJWTに依存しない
 *   <li>トークン有効期限や署名鍵はapplication.propertiesで設定
 * </ul>
 */
@Component
public class JwtTokenProvider {

    private final Key key;
    private final long validityInMilliseconds;

    /**
     * コンストラクタ
     *
     * @param secret JWT署名用の秘密鍵（application.propertiesから取得）
     * @param validityInMilliseconds トークン有効期限（ミリ秒）
     */
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration:3600000}") long validityInMilliseconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.validityInMilliseconds = validityInMilliseconds;
    }

    /**
     * JWTトークンを生成する
     *
     * @param userId ユーザーID
     * @param username ユーザー名
     * @return 生成されたJWTトークン
     */
    public String generateToken(UUID userId, String username) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("username", username)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key)
                .compact();
    }

    /**
     * トークンからユーザーIDを取得する
     *
     * @param token JWTトークン
     * @return ユーザーID
     * @throws JwtException トークンが無効な場合
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = extractClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * トークンからユーザー名を取得する
     *
     * @param token JWTトークン
     * @return ユーザー名
     * @throws JwtException トークンが無効な場合
     */
    public String getUsernameFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims.get("username", String.class);
    }

    /**
     * トークンの有効性を検証する
     *
     * @param token JWTトークン
     * @return トークンが有効な場合true、無効な場合false
     */
    public boolean validateToken(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // トークンが無効（署名エラー、期限切れ等）
            return false;
        }
    }

    /**
     * トークンからクレームを抽出する
     *
     * @param token JWTトークン
     * @return クレーム
     * @throws JwtException トークンが無効な場合
     */
    private Claims extractClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }
}
```

### 2. PasswordEncoderConfig（パスワードハッシュ化設定）

```java
package com.api.todos.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * パスワードエンコーダー設定
 *
 * <p>【責務】
 *
 * <ul>
 *   <li>BCryptPasswordEncoderのBean定義
 *   <li>パスワードのハッシュ化・検証機能の提供
 * </ul>
 *
 * <p>【セキュリティ原則】
 *
 * <ul>
 *   <li>パスワードは必ずBCryptでハッシュ化
 *   <li>平文パスワードは保存しない
 *   <li>ソルト（Salt）は自動生成
 * </ul>
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * BCryptPasswordEncoderのBean定義
     *
     * <p>BCryptの特徴：
     *
     * <ul>
     *   <li>ソルトの自動生成
     *   <li>コスト係数による計算量調整（デフォルト: 10）
     *   <li>レインボーテーブル攻撃への耐性
     * </ul>
     *
     * @return PasswordEncoderインスタンス
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 3. SecurityConfig（Spring Security設定）

```java
package com.api.todos.infrastructure.security;

import com.api.todos.infrastructure.security.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security設定
 *
 * <p>【責務】
 *
 * <ul>
 *   <li>HTTPセキュリティポリシーの定義
 *   <li>認証・認可ルールの設定
 *   <li>JWTフィルターの適用
 *   <li>CORS設定
 * </ul>
 *
 * <p>【セキュリティポリシー】
 *
 * <ul>
 *   <li>ステートレス（セッションレス）認証
 *   <li>JWT Bearer Token認証
 *   <li>CSRF保護無効（JWTを使用するため）
 *   <li>エンドポイント毎のアクセス制御
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * セキュリティフィルターチェーンの設定
     *
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception 設定エラー
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF保護を無効化（JWT認証のため不要）
                .csrf(csrf -> csrf.disable())

                // ステートレスセッション管理（JWTを使用）
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 認証・認可ルール
                .authorizeHttpRequests(
                        authorize ->
                                authorize
                                        // 認証不要のエンドポイント
                                        .requestMatchers("/api/auth/**")
                                        .permitAll()

                                        // 管理者専用エンドポイント
                                        .requestMatchers("/api/admin/**")
                                        .hasRole("ADMIN")

                                        // その他のエンドポイントは認証必須
                                        .anyRequest()
                                        .authenticated())

                // JWTフィルターを追加
                .addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

### 4. JwtAuthenticationFilter（JWT認証フィルター）

```java
package com.api.todos.infrastructure.security.filter;

import com.api.todos.infrastructure.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT認証フィルター
 *
 * <p>【責務】
 *
 * <ul>
 *   <li>HTTPリクエストからJWTトークンを抽出
 *   <li>トークンの検証
 *   <li>認証情報をSecurityContextに設定
 * </ul>
 *
 * <p>【処理フロー】
 *
 * <ol>
 *   <li>AuthorizationヘッダーからBearerトークンを抽出
 *   <li>JwtTokenProviderでトークン検証
 *   <li>トークンが有効な場合、ユーザー情報を抽出
 *   <li>認証情報をSecurityContextに設定
 *   <li>次のフィルターに処理を委譲
 * </ol>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Authorizationヘッダーからトークンを抽出
            String token = extractTokenFromRequest(request);

            // トークンが存在し、有効な場合
            if (token != null && jwtTokenProvider.validateToken(token)) {
                // トークンからユーザー情報を取得
                UUID userId = jwtTokenProvider.getUserIdFromToken(token);
                String username = jwtTokenProvider.getUsernameFromToken(token);

                // 認証情報を作成
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId, // principal: ユーザーID
                                null, // credentials: パスワード（不要）
                                null // authorities: 権限（必要に応じて設定）
                                );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // SecurityContextに認証情報を設定
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // トークン検証エラー（ログ出力のみ、処理は継続）
            logger.error("JWT authentication failed", e);
        }

        // 次のフィルターに処理を委譲
        filterChain.doFilter(request, response);
    }

    /**
     * リクエストからJWTトークンを抽出する
     *
     * @param request HTTPリクエスト
     * @return JWTトークン（存在しない場合はnull）
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
```

## 🔐 セキュリティベストプラクティス

### 1. パスワード管理

```java
// ✅ パスワードのハッシュ化（ユーザー登録時）
@Service
public class CreateUserService {
    private final CreateUserUseCase useCase;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional
    public UserResult execute(CreateUserCommand command) {
        // パスワードをハッシュ化
        String hashedPassword = passwordEncoder.encode(command.getPassword());
        
        // ハッシュ化済みパスワードでCommandを再構築
        CreateUserCommand secureCommand = new CreateUserCommand(
            command.getUsername(),
            command.getEmail(),
            hashedPassword,
            command.getFirstName(),
            command.getLastName()
        );
        
        return useCase.execute(secureCommand);
    }
}

// ✅ パスワード検証（ログイン時）
@Service
public class AuthenticateUserService {
    private final AuthenticateUserUseCase useCase;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional(readOnly = true)
    public AuthResult execute(LoginCommand command) {
        // ユーザー取得
        User user = userRepository.findByUsername(command.getUsername())
            .orElseThrow(() -> new AuthenticationException("認証に失敗しました"));
        
        // パスワード検証
        if (!passwordEncoder.matches(command.getPassword(), user.getHashedPassword())) {
            throw new AuthenticationException("認証に失敗しました");
        }
        
        return useCase.execute(new AuthenticateUserCommand(user.getId()));
    }
}
```

### 2. JWT管理

```java
// ✅ JWTトークン生成（ログイン成功後）
@Service
public class GenerateJwtTokenService {
    private final GenerateJwtTokenUseCase useCase;
    private final JwtTokenProvider jwtTokenProvider;
    
    @Transactional(readOnly = true)
    public AuthResult execute(GenerateJwtTokenCommand command) {
        // ユーザー情報取得
        User user = userRepository.findById(command.getUserId())
            .orElseThrow(() -> new UserNotFoundException("ユーザーが見つかりません"));
        
        // JWTトークン生成
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        
        return new AuthResult(token, user.getId(), user.getUsername());
    }
}

// ✅ JWTトークン検証（API呼び出し時）
// → JwtAuthenticationFilterで自動実行
// → SecurityContextにユーザー情報が設定される
// → Controllerで認証済みユーザー情報を取得可能
@GetMapping("/me")
public ResponseEntity<UserResponse> getCurrentUser() {
    UUID userId = (UUID) SecurityContextHolder.getContext()
        .getAuthentication().getPrincipal();
    
    // ...
}
```

### 3. 機密情報の管理

```properties
# application.properties

# JWT設定（本番環境では環境変数から読み込む）
jwt.secret=${JWT_SECRET:your-secret-key-change-this-in-production}
jwt.expiration=3600000

# データベース接続（本番環境では環境変数から読み込む）
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/tododb}
spring.datasource.username=${DB_USERNAME:todouser}
spring.datasource.password=${DB_PASSWORD:todopass}
```

## 🧪 テスト戦略

### JwtTokenProviderのテスト

```java
@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {
    
    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "test-secret-key-for-jwt-signing-must-be-long-enough";
    private final long validity = 3600000L; // 1時間
    
    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secret, validity);
    }
    
    @Test
    void generateToken_正常にトークンを生成できる() {
        // Given
        UUID userId = UUID.randomUUID();
        String username = "testuser";
        
        // When
        String token = jwtTokenProvider.generateToken(userId, username);
        
        // Then
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token));
        assertEquals(username, jwtTokenProvider.getUsernameFromToken(token));
    }
    
    @Test
    void validateToken_無効なトークンは検証に失敗する() {
        // Given
        String invalidToken = "invalid.token.here";
        
        // When & Then
        assertFalse(jwtTokenProvider.validateToken(invalidToken));
    }
}
```

### PasswordEncoderのテスト

```java
@SpringBootTest
class PasswordEncoderTest {
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Test
    void encode_パスワードをハッシュ化できる() {
        // Given
        String rawPassword = "password123";
        
        // When
        String hashedPassword = passwordEncoder.encode(rawPassword);
        
        // Then
        assertNotNull(hashedPassword);
        assertNotEquals(rawPassword, hashedPassword);
    }
    
    @Test
    void matches_正しいパスワードは検証に成功する() {
        // Given
        String rawPassword = "password123";
        String hashedPassword = passwordEncoder.encode(rawPassword);
        
        // When & Then
        assertTrue(passwordEncoder.matches(rawPassword, hashedPassword));
    }
    
    @Test
    void matches_間違ったパスワードは検証に失敗する() {
        // Given
        String rawPassword = "password123";
        String wrongPassword = "wrongpassword";
        String hashedPassword = passwordEncoder.encode(rawPassword);
        
        // When & Then
        assertFalse(passwordEncoder.matches(wrongPassword, hashedPassword));
    }
}
```

## 📋 実装チェックリスト

新しいセキュリティコンポーネントを追加する際は、以下を確認してください：

### JWT認証
- [ ] `JwtTokenProvider`でトークン生成・検証を実装
- [ ] トークン有効期限を設定（application.properties）
- [ ] 署名鍵を環境変数から読み込む（本番環境）
- [ ] トークンからユーザー情報を正しく抽出

### パスワード管理
- [ ] パスワードは必ずBCryptでハッシュ化
- [ ] 平文パスワードを保存していない
- [ ] パスワード検証は`passwordEncoder.matches()`を使用
- [ ] パスワードをログ出力していない

### 認証フィルター
- [ ] `OncePerRequestFilter`を継承
- [ ] Authorizationヘッダーからトークン抽出
- [ ] トークン検証エラーを適切に処理
- [ ] `SecurityContext`に認証情報を設定

### Spring Security設定
- [ ] ステートレスセッション管理（`SessionCreationPolicy.STATELESS`）
- [ ] CSRF保護無効（JWT認証のため）
- [ ] エンドポイント毎のアクセス制御
- [ ] JWTフィルターを適切な位置に追加

### セキュリティ原則
- [ ] Domain層はセキュリティフレームワークに依存しない
- [ ] Application層はJWT操作を直接実装しない
- [ ] 機密情報（秘密鍵、パスワード）をコードに含めない
- [ ] SQLインジェクション対策（パラメータバインディング）

## 🔗 関連ドキュメント

- **[infrastructure README](../README.md)** - Infrastructure層全体の概要
- **[config README](../config/README.md)** - 設定クラス全般
- **[application README](../../application/README.md)** - Application層のUseCase
- **[presentation/rest README](../../presentation/rest/README.md)** - ControllerでのJWT認証適用
- **[AGENTS.md](../../../../../../../../AGENTS.md)** - AIエージェント向けコンテキストドキュメント

## 💡 まとめ

Infrastructure層のsecurityパッケージは **認証・認可・セキュリティインフラの実装** を担当します：

### ✅ securityパッケージの責務

1. **JWT認証**: トークン生成・検証・抽出
2. **パスワード管理**: BCryptによるハッシュ化・検証
3. **認証フィルター**: HTTPリクエストの認証処理
4. **Spring Security設定**: セキュリティポリシーの定義

### ❌ securityパッケージでやってはいけないこと

1. **Domain層でセキュリティロジックを実装**（Pure Javaを保つ）
2. **Application層でJWT操作を実装**（Infrastructure層に委譲）
3. **パスワードを平文で扱う**（必ずBCryptでハッシュ化）
4. **機密情報をコードに含める**（環境変数から読み込む）

### 🎯 設計の意図

この設計により：

- **Domain層とApplication層をセキュリティフレームワークから独立**させる
- **セキュリティロジックをInfrastructure層に集約**し、保守性を向上
- **JWT認証とパスワードハッシュ化のベストプラクティス**を実現
- **テスタビリティの向上**（セキュリティコンポーネントを個別にテスト可能）

---

**作成日**: 2025年1月5日  
**対象バージョン**: Spring Boot TODO API v0.0.1-SNAPSHOT  

このドキュメントは、Infrastructure層のsecurityパッケージの責務と実装パターンを説明するものです。**JWT認証とパスワードハッシュ化をInfrastructure層で実装し、Domain層とApplication層をセキュリティフレームワークから独立させる** という原則を徹底してください。
