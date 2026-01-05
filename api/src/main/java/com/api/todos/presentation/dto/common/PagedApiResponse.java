package com.api.todos.presentation.dto.common;

import java.util.List;

/**
 * ページネーション付きAPIレスポンスDTO（Presentation層）
 *
 * <p>ページネーション機能を持つAPIエンドポイントで使用
 *
 * <p>レスポンス構造:
 *
 * <pre>{@code
 * {
 *   "success": true,
 *   "message": "Request successful",
 *   "data": {
 *     "data": [...],
 *     "total": 50,
 *     "page": 1,
 *     "perPage": 20,
 *     "totalPages": 3
 *   }
 * }
 * }</pre>
 *
 * @param <T> データアイテムの型
 */
public class PagedApiResponse<T> extends ApiResponse<PagedData<T>> {

    /**
     * コンストラクタ（内部使用）
     *
     * @param success リクエスト成功/失敗フラグ
     * @param message レスポンスメッセージ
     * @param pagedData ページネーションデータ
     */
    private PagedApiResponse(boolean success, String message, PagedData<T> pagedData) {
        super(success, message, pagedData);
    }

    /**
     * 成功レスポンスを生成（ページネーション付き）
     *
     * @param <T> データアイテムの型
     * @param data データアイテムのリスト
     * @param total 総件数
     * @param page 現在のページ番号
     * @param perPage 1ページあたりの件数
     * @return ページネーション付き成功レスポンス
     */
    public static <T> PagedApiResponse<T> success(List<T> data, long total, int page, int perPage) {
        PagedData<T> pagedData = PagedData.of(data, total, page, perPage);
        return new PagedApiResponse<>(true, "Request successful", pagedData);
    }

    /**
     * 成功レスポンスを生成（カスタムメッセージ、ページネーション付き）
     *
     * @param <T> データアイテムの型
     * @param message レスポンスメッセージ
     * @param data データアイテムのリスト
     * @param total 総件数
     * @param page 現在のページ番号
     * @param perPage 1ページあたりの件数
     * @return ページネーション付き成功レスポンス
     */
    public static <T> PagedApiResponse<T> success(
            String message, List<T> data, long total, int page, int perPage) {
        PagedData<T> pagedData = PagedData.of(data, total, page, perPage);
        return new PagedApiResponse<>(true, message, pagedData);
    }
}
