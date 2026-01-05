package com.api.todos.presentation.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 共通APIレスポンスDTO（Presentation層）
 *
 * <p>全てのAPIエンドポイントで使用される統一されたレスポンス形式
 *
 * <p>レスポンス構造:
 *
 * <pre>{@code
 * {
 *   "success": true/false,
 *   "message": "Request successful",
 *   "data": { ... } or null
 * }
 * }</pre>
 *
 * @param <T> レスポンスデータの型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** リクエスト成功/失敗フラグ */
    private final boolean success;

    /** レスポンスメッセージ */
    private final String message;

    /** レスポンスデータ（任意） */
    private final T data;

    /**
     * コンストラクタ（全項目指定）
     *
     * @param success リクエスト成功/失敗フラグ
     * @param message レスポンスメッセージ
     * @param data レスポンスデータ
     */
    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功レスポンスを生成（データあり）
     *
     * @param <T> レスポンスデータの型
     * @param data レスポンスデータ
     * @return 成功レスポンス
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Request successful", data);
    }

    /**
     * 成功レスポンスを生成（カスタムメッセージ、データあり）
     *
     * @param <T> レスポンスデータの型
     * @param message レスポンスメッセージ
     * @param data レスポンスデータ
     * @return 成功レスポンス
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /**
     * 成功レスポンスを生成（データなし）
     *
     * @param message レスポンスメッセージ
     * @return 成功レスポンス
     */
    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null);
    }

    /**
     * エラーレスポンスを生成
     *
     * @param message エラーメッセージ
     * @return エラーレスポンス
     */
    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, message, null);
    }

    /**
     * エラーレスポンスを生成（データあり）
     *
     * @param <T> レスポンスデータの型
     * @param message エラーメッセージ
     * @param data エラーデータ
     * @return エラーレスポンス
     */
    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data);
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
