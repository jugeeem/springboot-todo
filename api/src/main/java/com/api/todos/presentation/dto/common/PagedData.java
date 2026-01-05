package com.api.todos.presentation.dto.common;

import java.util.List;

/**
 * ページネーションデータDTO（Presentation層）
 *
 * <p>ページネーション付きのデータとメタデータを含むレスポンス
 *
 * <p>構造:
 *
 * <pre>{@code
 * {
 *   "data": [...],
 *   "total": 50,
 *   "page": 1,
 *   "perPage": 20,
 *   "totalPages": 3
 * }
 * }</pre>
 *
 * @param <T> データアイテムの型
 */
public class PagedData<T> {

    /** データアイテムのリスト */
    private final List<T> data;

    /** 総件数（フィルタ条件適用後） */
    private final long total;

    /** 現在のページ番号 */
    private final int page;

    /** 1ページあたりの件数 */
    private final int perPage;

    /** 総ページ数 */
    private final int totalPages;

    /**
     * コンストラクタ
     *
     * @param data データアイテムのリスト
     * @param total 総件数
     * @param page 現在のページ番号
     * @param perPage 1ページあたりの件数
     */
    public PagedData(List<T> data, long total, int page, int perPage) {
        this.data = data;
        this.total = total;
        this.page = page;
        this.perPage = perPage;
        this.totalPages = (int) Math.ceil((double) total / perPage);
    }

    /**
     * ページネーションデータを生成
     *
     * @param <T> データアイテムの型
     * @param data データアイテムのリスト
     * @param total 総件数
     * @param page 現在のページ番号
     * @param perPage 1ページあたりの件数
     * @return ページネーションデータ
     */
    public static <T> PagedData<T> of(List<T> data, long total, int page, int perPage) {
        return new PagedData<>(data, total, page, perPage);
    }

    // Getters
    public List<T> getData() {
        return data;
    }

    public long getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getPerPage() {
        return perPage;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
