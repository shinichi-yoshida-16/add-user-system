package com.fcuro.userreg.domain;

/**
 * Googleスプレッドシートの1行に対応するユーザ情報。
 * シート列: A=allowId, B=email, C=password, D=targetId, E=retiredFlag, F=adminFlag, G=updatedAt
 *
 * allowIdはシートごとに列見出し(ヘッダー名)が異なりうる内部管理用の識別子で、
 * ユーザが認識・入力するものではない。システムが登録時に自動採番する。
 * targetIdは別システムが通知メールの紐づけに使うための任意項目で、本システムでは
 * 読み書きするのみで、その値自体は利用しない。
 * adminFlagがTRUEのユーザのみ本システムへのログインが可能。
 */
public class User {

    private final String allowId;
    private final String email;
    private final String password;
    private final String targetId;
    private final boolean retired;
    private final boolean admin;
    private final String updatedAt;

    public User(String allowId, String email, String password, String targetId,
                boolean retired, boolean admin, String updatedAt) {
        this.allowId = allowId;
        this.email = email;
        this.password = password;
        this.targetId = targetId;
        this.retired = retired;
        this.admin = admin;
        this.updatedAt = updatedAt;
    }

    public String getAllowId() {
        return allowId;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getTargetId() {
        return targetId;
    }

    public boolean isRetired() {
        return retired;
    }

    public boolean isAdmin() {
        return admin;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
