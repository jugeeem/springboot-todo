package com.api.todos.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * セキュリティ設定クラス
 *
 * <p>Spring Security の設定を行います。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * セキュリティフィルターチェーンの設定
     *
     * @param http HttpSecurityオブジェクト
     * @return SecurityFilterChain
     * @throws Exception セキュリティ設定エラー
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // ルートと /health をセキュリティから除外
                .requestMatchers("/", "/health", "/actuator/health").permitAll()
                // その他のリクエストは認証が必要
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> { }); // Basic認証を有効化

        return http.build();
    }
}
