package com.fcuro.userreg.service;

import com.fcuro.userreg.domain.User;
import com.fcuro.userreg.repository.UserSheetRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private static final String SPREADSHEET_ID = "sheet-123";
    private static final String SHEET_NAME = "Users";

    private final UserSheetRepository repository = mock(UserSheetRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserService userService = new UserService(repository, passwordEncoder);

    @Test
    void 新規メールアドレスは登録できる() {
        when(repository.existsByEmail(SPREADSHEET_ID, SHEET_NAME, "new@example.com")).thenReturn(false);

        userService.register(SPREADSHEET_ID, SHEET_NAME, "new@example.com", "password123", "target-1", false);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(eq(SPREADSHEET_ID), eq(SHEET_NAME), captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getAllowId()).isNull();
        assertThat(saved.getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.getPassword())).isTrue();
        assertThat(saved.getTargetId()).isEqualTo("target-1");
        assertThat(saved.isRetired()).isFalse();
        assertThat(saved.isAdmin()).isFalse();
    }

    @Test
    void 既に登録済みのメールアドレスは例外になる() {
        when(repository.existsByEmail(SPREADSHEET_ID, SHEET_NAME, "exists@example.com")).thenReturn(true);

        assertThatThrownBy(() ->
                userService.register(SPREADSHEET_ID, SHEET_NAME, "exists@example.com", "password123", null, false))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void adminFlagを指定して管理者ユーザとして登録できる() {
        when(repository.existsByEmail(SPREADSHEET_ID, SHEET_NAME, "admin@example.com")).thenReturn(false);

        userService.register(SPREADSHEET_ID, SHEET_NAME, "admin@example.com", "password123", "target-1", true);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(eq(SPREADSHEET_ID), eq(SHEET_NAME), captor.capture());
        assertThat(captor.getValue().isAdmin()).isTrue();
    }
}
