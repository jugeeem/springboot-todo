package com.api.todos.presentation.dto.common;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * エラー詳細DTO（Presentation層）
 *
 * <p>エラーレスポンスに含める詳細情報
 *
 * <p>バリデーションエラーなど、詳細なエラー情報を返す場合に使用
 *
 * <pre>{@code
 * {
 *   "success": false,
 *   "message": "Validation error",
 *   "data": {
 *     "timestamp": "2025-07-24T10:00:00",
 *     "path": "/api/users",
 *     "errors": {
 *       "username": "Username is required",
 *       "password": "Password must be at least 6 characters"
 *     }
 *   }
 * }
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDetails {

    /** エラー発生時刻 */
    private final LocalDateTime timestamp;

    /** リクエストパス */
    private final String path;

    /** エラー詳細マップ（フィールド名 → エラーメッセージ） */
    private final Map<String, String> errors;

    /**
     * コンストラクタ
     *
     * @param path リクエストパス
     * @param errors エラー詳細マップ
     */
    public ErrorDetails(String path, Map<String, String> errors) {
        this.timestamp = LocalDateTime.now();
        this.path = path;
        this.errors = errors;
    }

    /**
     * エラー詳細を生成
     *
     * @param path リクエストパス
     * @param errors エラー詳細マップ
     * @return エラー詳細
     */
    public static ErrorDetails of(String path, Map<String, String> errors) {
        return new ErrorDetails(path, errors);
    }

    // Getters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
