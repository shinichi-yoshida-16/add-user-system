package com.fcuro.userreg.security;

import com.fcuro.userreg.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * ログイン中のユーザ情報。認証時に入力されたスプレッドシートIDをセッション内で
 * 保持するために、ログインセッションのAuthentication#getPrincipal()として保持する。
 */
public class SheetUserPrincipal implements UserDetails {

    private final User user;
    private final String spreadsheetId;
    private final String sheetName;

    public SheetUserPrincipal(User user, String spreadsheetId, String sheetName) {
        this.user = user;
        this.spreadsheetId = spreadsheetId;
        this.sheetName = sheetName;
    }

    public String getAllowId() {
        return user.getAllowId();
    }

    public String getTargetId() {
        return user.getTargetId();
    }

    public String getSpreadsheetId() {
        return spreadsheetId;
    }

    public String getSheetName() {
        return sheetName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !user.isRetired();
    }
}
