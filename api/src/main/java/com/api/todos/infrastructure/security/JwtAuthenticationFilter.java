package com.api.todos.infrastructure.security;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.api.todos.domain.service.JwtService;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT認証フィルター（Infrastructure層）
 *
 * <p>リクエストヘッダー「Authorization: Bearer {token}」からJWTトークンを取得し、 検証してSecurityContextに認証情報を設定する
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Authorization ヘッダーからJWTトークンを取得
            String token = extractTokenFromRequest(request);

            if (token != null && jwtService.isTokenValid(token)) {
                // トークンが有効な場合、認証情報をSecurityContextに設定
                UUID userId = jwtService.validateTokenAndGetUserId(token);
                String username = jwtService.getUsernameFromToken(token);
                Integer role = jwtService.getRoleFromToken(token);

                if (userId != null && username != null && role != null) {
                    // ロールを権限として設定
                    SimpleGrantedAuthority authority =
                            new SimpleGrantedAuthority("ROLE_" + mapRoleToString(role));

                    // 認証トークンを作成
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userId, // Principal（主体）としてUserIdを設定
                                    null, // Credentials（資格情報）はnull
                                    Collections.singletonList(authority));

                    // リクエスト詳細を設定
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    // SecurityContextに認証情報を設定
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            // トークン検証エラーの場合、ログ出力（本番環境ではロギングフレームワークを使用）
            logger.error("JWT認証エラー: " + e.getMessage());
        }

        // 次のフィルターへ
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

    /**
     * ロール番号を文字列にマッピング
     *
     * @param role ロール番号
     * @return ロール文字列
     */
    private String mapRoleToString(int role) {
        return switch (role) {
            case 0 -> "ADMIN"; // 管理者
            case 4 -> "MANAGER"; // マネージャー
            case 8 -> "USER"; // 一般ユーザー
            default -> "USER";
        };
    }
}
