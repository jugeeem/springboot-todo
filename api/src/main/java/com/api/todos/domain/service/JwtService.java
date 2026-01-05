package com.api.todos.domain.service;

import java.util.UUID;

/** JWTサービスインターフェース（Domain層） Pure Javaインターフェース - フレームワーク依存なし Infrastructure層で実装される（依存性逆転の原則） */
public interface JwtService {

    /**
     * JWTトークンを生成する
     *
     * @param userId ユーザーID
     * @param username ユーザー名
     * @param role ユーザーロール
     * @return 生成されたJWTトークン
     */
    String generateToken(UUID userId, String username, int role);

    /**
     * JWTトークンを検証し、ユーザーIDを取得する
     *
     * @param token JWTトークン
     * @return ユーザーID（検証失敗時はnull）
     */
    UUID validateTokenAndGetUserId(String token);

    /**
     * JWTトークンからユーザー名を取得する
     *
     * @param token JWTトークン
     * @return ユーザー名（検証失敗時はnull）
     */
    String getUsernameFromToken(String token);

    /**
     * JWTトークンからロールを取得する
     *
     * @param token JWTトークン
     * @return ロール（検証失敗時はnull）
     */
    Integer getRoleFromToken(String token);

    /**
     * JWTトークンが有効かどうかを検証する
     *
     * @param token JWTトークン
     * @return 有効な場合true
     */
    boolean isTokenValid(String token);
}
