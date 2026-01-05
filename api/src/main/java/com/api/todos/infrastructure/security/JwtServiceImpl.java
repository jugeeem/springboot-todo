package com.api.todos.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import com.api.todos.domain.service.JwtService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** JWT サービス実装（Infrastructure層） Domain層のJwtServiceインターフェースを実装 JJWTライブラリを使用したJWT生成・検証処理 */
@Service
public class JwtServiceImpl implements JwtService {

    private final SecretKey secretKey;
    private final long jwtExpirationMs;

    /**
     * コンストラクタ
     *
     * @param secret JWT署名用シークレットキー（application.propertiesから注入）
     * @param jwtExpirationMs JWT有効期限（ミリ秒）
     */
    public JwtServiceImpl(
            @Value("${jwt.secret:default-secret-key-change-this-in-production-minimum-256-bits}")
                    String secret,
            @Value("${jwt.expiration-ms:86400000}") long jwtExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = jwtExpirationMs;
    }

    @Override
    public String generateToken(UUID userId, String username, int role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    @Override
    public UUID validateTokenAndGetUserId(String token) {
        try {
            Claims claims = extractClaims(token);
            String userIdString = claims.getSubject();
            return UUID.fromString(userIdString);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.get("username", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Integer getRoleFromToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.get("role", Integer.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * JWTトークンからClaimsを抽出する（内部メソッド）
     *
     * @param token JWTトークン
     * @return Claims
     * @throws JwtException トークンが無効な場合
     */
    private Claims extractClaims(String token) throws JwtException {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }
}
