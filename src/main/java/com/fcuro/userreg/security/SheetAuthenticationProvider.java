package com.fcuro.userreg.security;

import com.fcuro.userreg.domain.User;
import com.fcuro.userreg.repository.SheetAccessException;
import com.fcuro.userreg.repository.UserSheetRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * ログインフォームで入力された「スプレッドシートID・メールアドレス・パスワード」を用いて認証する。
 *
 * 対象スプレッドシートが固定ではなくリクエストごとに変わるため、標準のUserDetailsServiceベースの
 * DaoAuthenticationProviderは使わず、AuthenticationProviderを直接実装している。
 */
@Component
public class SheetAuthenticationProvider implements AuthenticationProvider {

    private final UserSheetRepository userSheetRepository;
    private final PasswordEncoder passwordEncoder;

    public SheetAuthenticationProvider(UserSheetRepository userSheetRepository, PasswordEncoder passwordEncoder) {
        this.userSheetRepository = userSheetRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String rawPassword = String.valueOf(authentication.getCredentials());

        String spreadsheetId = extractSpreadsheetId(authentication);
        if (!StringUtils.hasText(spreadsheetId)) {
            throw new BadCredentialsException("スプレッドシートIDを入力してください");
        }
        String sheetName = extractSheetName(authentication);
        if (!StringUtils.hasText(sheetName)) {
            throw new BadCredentialsException("シート名を入力してください");
        }

        Optional<User> found;
        try {
            found = userSheetRepository.findByEmail(spreadsheetId, sheetName, email);
        } catch (SheetAccessException e) {
            throw new AuthenticationServiceException(
                    "スプレッドシートにアクセスできませんでした。スプレッドシートID・シート名と共有設定をご確認ください。", e);
        }
        User user = found.orElseThrow(() -> new BadCredentialsException("メールアドレスまたはパスワードが正しくありません"));

        if (user.isRetired()) {
            throw new DisabledException("このアカウントは無効化されています");
        }
        if (!StringUtils.hasText(user.getTargetId())) {
            throw new DisabledException("targetIdが設定されていないため、このアカウントはログインできません");
        }
        if (!user.isAdmin()) {
            throw new DisabledException("管理者ユーザ(adminFlag=TRUE)ではないため、このアカウントはログインできません");
        }
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BadCredentialsException("メールアドレスまたはパスワードが正しくありません");
        }

        SheetUserPrincipal principal = new SheetUserPrincipal(user, spreadsheetId, sheetName);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private String extractSpreadsheetId(Authentication authentication) {
        Object details = authentication.getDetails();
        if (details instanceof SheetAuthenticationDetails sheetDetails) {
            return sheetDetails.getSpreadsheetId();
        }
        return null;
    }

    private String extractSheetName(Authentication authentication) {
        Object details = authentication.getDetails();
        if (details instanceof SheetAuthenticationDetails sheetDetails) {
            return sheetDetails.getSheetName();
        }
        return null;
    }
}
