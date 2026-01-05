package com.api.todos.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/** TODOエンティティ（Domain層） Pure Javaで実装 - フレームワーク依存なし */
public class Todo {
    private final UUID id;
    private String title;
    private String descriptions;
    private boolean completed;
    private final UUID userId;
    private final LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private boolean deleted;

    /** 既存TODOの再構築（リポジトリから取得時） */
    public Todo(
            UUID id,
            String title,
            String descriptions,
            boolean completed,
            UUID userId,
            LocalDateTime createdAt,
            String createdBy,
            LocalDateTime updatedAt,
            String updatedBy,
            boolean deleted) {
        this.id = id;
        this.title = title;
        this.descriptions = descriptions;
        this.completed = completed;
        this.userId = userId;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.deleted = deleted;
    }

    /** 新規TODO作成 */
    public Todo(String title, String descriptions, UUID userId, String createdBy) {
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("タイトルは必須です");
        }
        if (title.length() > 32) {
            throw new IllegalArgumentException("タイトルは32文字以内で入力してください");
        }
        if (descriptions != null && descriptions.length() > 128) {
            throw new IllegalArgumentException("説明は128文字以内で入力してください");
        }
        if (userId == null) {
            throw new IllegalArgumentException("ユーザーIDは必須です");
        }

        this.id = UUID.randomUUID();
        this.title = title;
        this.descriptions = descriptions;
        this.completed = false;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
        this.createdBy = createdBy;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = createdBy;
        this.deleted = false;
    }

    /** ビジネスルール: TODO完了状態の変更 */
    public void markAsCompleted(String updatedBy) {
        if (this.deleted) {
            throw new IllegalStateException("削除済みのTODOは完了にできません");
        }
        if (this.completed) {
            throw new IllegalStateException("既に完了済みのTODOです");
        }
        this.completed = true;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
    }

    /** ビジネスルール: TODO未完了状態に戻す */
    public void markAsIncomplete(String updatedBy) {
        if (this.deleted) {
            throw new IllegalStateException("削除済みのTODOは未完了に戻せません");
        }
        if (!this.completed) {
            throw new IllegalStateException("未完了のTODOです");
        }
        this.completed = false;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
    }

    /** ビジネスルール: TODO内容の更新 */
    public void update(String title, String descriptions, String updatedBy) {
        if (this.deleted) {
            throw new IllegalStateException("削除済みのTODOは更新できません");
        }
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("タイトルは必須です");
        }
        if (title.length() > 32) {
            throw new IllegalArgumentException("タイトルは32文字以内で入力してください");
        }
        if (descriptions != null && descriptions.length() > 128) {
            throw new IllegalArgumentException("説明は128文字以内で入力してください");
        }

        this.title = title;
        this.descriptions = descriptions;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
    }

    /** ビジネスルール: TODO削除（論理削除） */
    public void delete(String updatedBy) {
        if (this.deleted) {
            throw new IllegalStateException("既に削除済みのTODOです");
        }
        this.deleted = true;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescriptions() {
        return descriptions;
    }

    public boolean isCompleted() {
        return completed;
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Todo todo = (Todo) o;
        return id != null && id.equals(todo.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
