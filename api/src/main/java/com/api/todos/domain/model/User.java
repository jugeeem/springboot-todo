package com.api.todos.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/** ユーザーエンティティ（Domain層） Pure Javaで実装 - フレームワーク依存なし */
public class User {
    private final UUID id;
    private String username;
    private String firstName;
    private String firstNameRuby;
    private String lastName;
    private String lastNameRuby;
    private int role;
    private String passwordHash;
    private final LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private boolean deleted;

    /** 既存ユーザーの再構築（リポジトリから取得時） */
    public User(
            UUID id,
            String username,
            String firstName,
            String firstNameRuby,
            String lastName,
            String lastNameRuby,
            int role,
            String passwordHash,
            LocalDateTime createdAt,
            String createdBy,
            LocalDateTime updatedAt,
            String updatedBy,
            boolean deleted) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.firstNameRuby = firstNameRuby;
        this.lastName = lastName;
        this.lastNameRuby = lastNameRuby;
        this.role = role;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.deleted = deleted;
    }

    /** 新規ユーザーの作成 */
    public User(
            String username,
            String firstName,
            String firstNameRuby,
            String lastName,
            String lastNameRuby,
            int role,
            String passwordHash,
            String createdBy) {
        this.id = UUID.randomUUID();
        this.username = username;
        this.firstName = firstName;
        this.firstNameRuby = firstNameRuby;
        this.lastName = lastName;
        this.lastNameRuby = lastNameRuby;
        this.role = role;
        this.passwordHash = passwordHash;
        this.createdAt = LocalDateTime.now();
        this.createdBy = createdBy;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = createdBy;
        this.deleted = false;
    }

    /** ビジネスルール: ユーザー情報の更新 */
    public void updateProfile(
            String firstName,
            String firstNameRuby,
            String lastName,
            String lastNameRuby,
            String updatedBy) {
        if (this.deleted) {
            throw new IllegalStateException("削除済みのユーザーは更新できません");
        }
        this.firstName = firstName;
        this.firstNameRuby = firstNameRuby;
        this.lastName = lastName;
        this.lastNameRuby = lastNameRuby;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
    }

    /** ビジネスルール: パスワードの変更 */
    public void changePassword(String newPasswordHash, String updatedBy) {
        if (this.deleted) {
            throw new IllegalStateException("削除済みのユーザーはパスワード変更できません");
        }
        if (newPasswordHash == null || newPasswordHash.isEmpty()) {
            throw new IllegalArgumentException("パスワードハッシュは必須です");
        }
        this.passwordHash = newPasswordHash;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
    }

    /** ビジネスルール: ロールの変更 */
    public void changeRole(int newRole, String updatedBy) {
        if (this.deleted) {
            throw new IllegalStateException("削除済みのユーザーはロール変更できません");
        }
        this.role = newRole;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
    }

    /** ビジネスルール: ユーザーの削除（論理削除） */
    public void delete(String updatedBy) {
        if (this.deleted) {
            throw new IllegalStateException("既に削除済みのユーザーです");
        }
        this.deleted = true;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
    }

    /** ビジネスルール: パスワード検証 */
    public boolean verifyPassword(String hashedPassword) {
        return this.passwordHash.equals(hashedPassword);
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getFirstNameRuby() {
        return firstNameRuby;
    }

    public String getLastName() {
        return lastName;
    }

    public String getLastNameRuby() {
        return lastNameRuby;
    }

    public int getRole() {
        return role;
    }

    public String getPasswordHash() {
        return passwordHash;
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
        User user = (User) o;
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
