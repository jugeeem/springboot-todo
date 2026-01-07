package com.api.todos.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ユーザーエンティティ（Domain層） Pure Javaで実装 - フレームワーク依存なし
 *
 * <h5>【処理概要】</h5>
 * <p>
 * このクラスは、ユーザーに関するビジネスルールと属性を表現します。<br>
 * ユーザーの作成、更新、削除などの操作を提供します。<br>
 * </p>
 *
 * @author GenkiHashioka
 */
public class User {

    /**
     * ユーザーID
     */
    private final UUID id;
    /**
     * ユーザー名
     */
    private String username;
    /**
     * 名前
     */
    private String firstName;
    /**
     * 名前（ふりがな）
     */
    private String firstNameRuby;
    /**
     * 姓
     */
    private String lastName;
    /**
     * 姓（ふりがな）
     */
    private String lastNameRuby;
    /**
     * ハッシュ化されたパスワード
     */
    private String passwordHash;
    /**
     * ユーザーの権限レベル
     */
    private UserRole role;
    /**
     * 作成日時
     */
    private final LocalDateTime createdAt;
    /**
     * 更新日時
     */
    private LocalDateTime updatedAt;
    /**
     * 論理削除フラグ
     */
    private boolean deleted;

    /**
     * 定数定義（エラーメッセージ作成用）
     */
    private static final String ERROR_FIELD_FIRSTNAME = "firstName";
    private static final String ERROR_FIELD_FIRSTNAMERUBY = "firstNameRuby";
    private static final String ERROR_FIELD_LASTNAME = "lastName";
    private static final String ERROR_FIELD_LASTNAMERUBY = "lastNameRuby";

    private static final String ERROR_MSG_REQUIRED = "%sは必須です";
    private static final String ERROR_MSG_NAME_LENGTH = "%sは50文字以内である必要があります";
    private static final String ERROR_MSG_DELETED_USER = "削除済みのユーザーは%sできません";

    /**
     * プライベートコンストラクタ。 外部からの直接インスタンス化を防止し、ファクトリーメソッドを通じてオブジェクトを生成させるために使用。
     *
     * @param id ユーザーID
     * @param username ユーザー名
     * @param firstName 名前
     * @param firstNameRuby 名前（ふりがな）
     * @param lastName 姓
     * @param lastNameRuby 姓（ふりがな）
     * @param passwordHash ハッシュ化されたパスワード
     * @param role ユーザーの権限レベル
     * @param createdAt 作成日時
     * @param updatedAt 更新日時
     * @param deleted 論理削除フラグ
     */
    private User(UUID id, String username, String firstName, String firstNameRuby, String lastName, String lastNameRuby, String passwordHash, UserRole role, LocalDateTime createdAt, LocalDateTime updatedAt, boolean deleted) {
        // バリデーション
        validateId(id);
        validateUsername(username);
        validateName(firstName, ERROR_FIELD_FIRSTNAME);
        validateName(firstNameRuby, ERROR_FIELD_FIRSTNAMERUBY);
        validateName(lastName, ERROR_FIELD_LASTNAME);
        validateName(lastNameRuby, ERROR_FIELD_LASTNAMERUBY);
        validatePasswordHash(passwordHash);
        validateRole(role);
        varidateCreatedAt(createdAt);
        varidateUpdatedAt(updatedAt);

        // フィールドの初期化
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.firstNameRuby = firstNameRuby;
        this.lastName = lastName;
        this.lastNameRuby = lastNameRuby;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
    }

    /**
     * 新規ユーザーの作成。 新規ユーザー作成時に使用するファクトリーメソッドです。
     *
     * @param username ユーザー名
     * @param firstName 名前
     * @param firstNameRuby 名前（ふりがな）
     * @param lastName 姓
     * @param lastNameRuby 姓（ふりがな）
     * @param passwordHash ハッシュ化されたパスワード
     * @param role ユーザーの権限レベル
     * @return 新規作成されたUserオブジェクト
     */
    public static User create(String username, String firstName, String firstNameRuby, String lastName, String lastNameRuby, String passwordHash, UserRole role) {
        // 引数を検証し、新しいUUIDと現在日時を生成してUserオブジェクトを返す。
        return new User(
                UUID.randomUUID(),
                username,
                firstName,
                firstNameRuby,
                lastName,
                lastNameRuby,
                passwordHash,
                role,
                LocalDateTime.now(),
                LocalDateTime.now(),
                false
        );
    }

    /**
     * 既存ユーザーの再構築。 既存のユーザーデータからUserオブジェクトを再構築するためのファクトリーメソッドです。
     *
     * @param id ユーザーID
     * @param username ユーザー名
     * @param firstName 名前
     * @param firstNameRuby 名前（ふりがな）
     * @param lastName 姓
     * @param lastNameRuby 姓（ふりがな）
     * @param passwordHash ハッシュ化されたパスワード
     * @param role ユーザーの権限レベル
     * @param createdAt 作成日時
     * @param updatedAt 更新日時
     * @param deleted 論理削除フラグ
     * @return 再構築されたUserオブジェクト
     */
    public static User reconstruct(UUID id, String username, String firstName, String firstNameRuby, String lastName, String lastNameRuby, String passwordHash, UserRole role, LocalDateTime createdAt, LocalDateTime updatedAt, boolean deleted) {
        // 引数を検証し、Userオブジェクトを返す。
        return new User(
                id,
                username,
                firstName,
                firstNameRuby,
                lastName,
                lastNameRuby,
                passwordHash,
                role,
                createdAt,
                updatedAt,
                deleted
        );
    }

    /**
     * ビジネスルール: ユーザー情報の更新
     *
     * @param firstName 名前
     * @param firstNameRuby 名前（ふりがな）
     * @param lastName 姓
     * @param lastNameRuby 姓（ふりがな）
     */
    public void updateProfile(
            String firstName,
            String firstNameRuby,
            String lastName,
            String lastNameRuby) {
        // 削除済みのユーザーは更新できない
        if (this.deleted) {
            throw new IllegalStateException("削除済みのユーザーは更新できません");
        }
        // 名前とふりがなのバリデーション
        validateName(firstName, ERROR_FIELD_FIRSTNAME);
        validateName(firstNameRuby, ERROR_FIELD_FIRSTNAMERUBY);
        validateName(lastName, ERROR_FIELD_LASTNAME);
        validateName(lastNameRuby, ERROR_FIELD_LASTNAMERUBY);

        // プロフィール情報の更新
        this.firstName = firstName;
        this.firstNameRuby = firstNameRuby;
        this.lastName = lastName;
        this.lastNameRuby = lastNameRuby;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * ビジネスルール: パスワードの変更
     *
     * @param newPasswordHash 新しいハッシュ化されたパスワード
     */
    public void changePassword(String newPasswordHash) {
        // 削除済みのユーザーはパスワード変更できない
        if (this.deleted) {
            throw new IllegalStateException(String.format(ERROR_MSG_DELETED_USER, "パスワード変更"));
        }
        // パスワードハッシュのバリデーション
        validatePasswordHash(newPasswordHash);

        // パスワードの更新
        this.passwordHash = newPasswordHash;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * ビジネスルール: ロールの変更
     *
     * @param newRole 新しいロール
     */
    public void changeRole(UserRole newRole) {
        // 削除済みのユーザーはロール変更できない
        if (this.deleted) {
            throw new IllegalStateException(String.format(ERROR_MSG_DELETED_USER, "ロール変更"));
        }
        // ロールのバリデーション
        validateRole(newRole);

        // ロールの更新
        this.role = newRole;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * ビジネスルール: ユーザーの削除（論理削除）
     */
    public void delete() {
        // 既に削除済みの場合は例外をスロー
        if (this.deleted) {
            throw new IllegalStateException("既に削除済みのユーザーです");
        }

        // 論理削除の実行
        this.deleted = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 管理者権限を持っているかどうかを判定する
     */
    public boolean isAdmin() {
        return this.role.hasAdminPrivilege();
    }

    /**
     * マネージャー権限を持つかをチェックする。
     */
    public boolean isManager() {
        return this.role.hasManagerPrivilege();
    }

    /**
     * userIDのバリデーション。 userIDがnullの場合、IllegalArgumentExceptionをスローします。
     *
     * @param id ユーザーID
     */
    private void validateId(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException(String.format(ERROR_MSG_REQUIRED, "ID"));
        }
    }

    /**
     * ユーザー名のバリデーション。
     * ユーザー名がnullまたは空白の場合、IllegalArgumentExceptionをスローします。また、ユーザー名が50文字を超える場合も例外をスローします。
     *
     * @param username ユーザー名
     */
    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(String.format(ERROR_MSG_REQUIRED, "ユーザー名"));
        }
        if (username.length() > 50) {
            throw new IllegalArgumentException("ユーザー名は50文字以内である必要があります");
        }
    }

    /**
     * 名前のバリデーション。 名前が50文字を超える場合、IllegalArgumentExceptionをスローします。
     *
     * @param name 名前
     * @param fieldName フィールド名（エラーメッセージ用）
     */
    private void validateName(String name, String fieldName) {
        if (name != null && name.length() > 50) {
            throw new IllegalArgumentException(String.format(ERROR_MSG_NAME_LENGTH, fieldName));
        }
    }

    /**
     * パスワードハッシュのバリデーション。
     * パスワードハッシュがnullまたは空白の場合、IllegalArgumentExceptionをスローします。
     *
     * @param passwordHash ハッシュ化されたパスワード
     */
    private void validatePasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException(String.format(ERROR_MSG_REQUIRED, "パスワードハッシュ"));
        }
        if (passwordHash.length() != 60) {
            throw new IllegalArgumentException(
                    "パスワードハッシュの長さが不正です（BCryptは60文字）"
            );
        }
    }

    /**
     * ロールのバリデーション。 ロールがnullの場合、IllegalArgumentExceptionをスローします。
     *
     * @param role ユーザーの権限レベル
     */
    private void validateRole(UserRole role) {
        if (role == null) {
            throw new IllegalArgumentException(String.format(ERROR_MSG_REQUIRED, "ロール"));
        }
    }

    /**
     * 作成日時のバリデーション。 作成日時がnullの場合、IllegalArgumentExceptionをスローします。
     *
     * @param createdAt 作成日時
     */
    private void varidateCreatedAt(LocalDateTime createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException(String.format(ERROR_MSG_REQUIRED, "作成日時"));
        }
    }

    /**
     * 更新日時のバリデーション。 更新日時がnullの場合、IllegalArgumentExceptionをスローします。
     *
     * @param updatedAt 更新日時
     */
    private void varidateUpdatedAt(LocalDateTime updatedAt) {
        if (updatedAt == null) {
            throw new IllegalArgumentException(String.format(ERROR_MSG_REQUIRED, "更新日時"));
        }
    }

    /**
     * ユーザーIDを取得。
     *
     * @return ユーザーID
     */
    public UUID getId() {
        return id;
    }

    /**
     * ユーザー名を取得。
     *
     * @return ユーザー名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 名前を取得。
     *
     * @return 名前
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * 名前（ふりがな）を取得。
     *
     * @return 名前（ふりがな）
     */
    public String getFirstNameRuby() {
        return firstNameRuby;
    }

    /**
     * 姓を取得。
     *
     * @return 姓
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * 姓（ふりがな）を取得。
     *
     * @return 姓（ふりがな）
     */
    public String getLastNameRuby() {
        return lastNameRuby;
    }

    /**
     * ハッシュ化されたパスワードを取得。
     *
     * @return ハッシュ化されたパスワード
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * ユーザーの権限レベルを取得。
     *
     * @return ユーザーの権限レベル
     */
    public UserRole getRole() {
        return role;
    }

    /**
     * 作成日時を取得。
     *
     * @return 作成日時
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 更新日時を取得。
     *
     * @return 更新日時
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 論理削除フラグを取得。
     *
     * @return 論理削除フラグ
     */
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        User user = (User) obj;
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "User{"
                + "id=" + id
                + ", username='" + username + '\''
                + ", firstName='" + firstName + '\''
                + ", lastName='" + lastName + '\''
                + ", role=" + role
                + ", deleted=" + deleted
                + '}';
    }
}
