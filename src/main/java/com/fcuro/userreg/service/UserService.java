package com.fcuro.userreg.service;

import com.fcuro.userreg.domain.User;
import com.fcuro.userreg.repository.UserSheetRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class UserService {

    private static final ZoneId TOKYO_ZONE = ZoneId.of("Asia/Tokyo");

    private final UserSheetRepository userSheetRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserSheetRepository userSheetRepository, PasswordEncoder passwordEncoder) {
        this.userSheetRepository = userSheetRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(String spreadsheetId, String sheetName, String email, String rawPassword, String targetId,
                          boolean admin) {
        if (userSheetRepository.existsByEmail(spreadsheetId, sheetName, email)) {
            throw new EmailAlreadyExistsException("このメールアドレスは既に登録されています: " + email);
        }

        // allowIdはUserSheetRepository.save()側でスプレッドシートの既存データ件数から連番採番する
        User user = new User(
                null,
                email,
                passwordEncoder.encode(rawPassword),
                targetId,
                false,
                admin,
                currentTimestamp()
        );
        userSheetRepository.save(spreadsheetId, sheetName, user);
    }

    public List<User> listUsers(String spreadsheetId, String sheetName) {
        return userSheetRepository.findAll(spreadsheetId, sheetName);
    }

    public void updateRetiredFlag(String spreadsheetId, String sheetName, String allowId, boolean retired) {
        boolean updated = userSheetRepository.updateRetiredFlag(
                spreadsheetId, sheetName, allowId, retired, currentTimestamp());
        if (!updated) {
            throw new UserNotFoundException("指定されたユーザーが見つかりません: allowId=" + allowId);
        }
    }

    public void updateAdminFlag(String spreadsheetId, String sheetName, String allowId, boolean admin) {
        boolean updated = userSheetRepository.updateAdminFlag(
                spreadsheetId, sheetName, allowId, admin, currentTimestamp());
        if (!updated) {
            throw new UserNotFoundException("指定されたユーザーが見つかりません: allowId=" + allowId);
        }
    }

    /** 日本時間(Asia/Tokyo)のタイムスタンプ文字列を返す。 */
    private String currentTimestamp() {
        return ZonedDateTime.now(TOKYO_ZONE).toOffsetDateTime().toString();
    }
}
