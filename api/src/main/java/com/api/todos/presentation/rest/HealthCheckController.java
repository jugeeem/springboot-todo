package com.api.todos.presentation.rest;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ヘルスチェックコントローラー
 *
 * <p>APIの稼働状態を確認するためのエンドポイントを提供します。
 */
@RestController
public class HealthCheckController {

    /**
     * ルートエンドポイント
     *
     * <p>APIが稼働していることを確認するシンプルなエンドポイントです。
     *
     * @return ウェルカムメッセージ
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, String>> root() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Welcome to Spring Boot TODO API");
        response.put("status", "running");

        return ResponseEntity.ok(response);
    }

    /**
     * ヘルスチェックエンドポイント
     *
     * <p>APIが正常に稼働しているかを確認します。
     *
     * @return ヘルスチェック結果
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now(ZoneId.of("Asia/Tokyo")).toString());
        response.put("service", "ShiftApp API");
        response.put("version", "v0.1.1-SNAPSHOT");

        return ResponseEntity.ok(response);
    }

    /**
     * シンプルなヘルスチェックエンドポイント
     *
     * <p>最小限の情報でAPIの稼働状態を確認します。 ロードバランサーやモニタリングツールからの定期的な確認に使用します。
     *
     * @return ステータスのみを含むレスポンス
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");

        return ResponseEntity.ok(response);
    }
}
