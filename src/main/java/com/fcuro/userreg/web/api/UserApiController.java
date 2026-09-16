package com.fcuro.userreg.web.api;

import com.fcuro.userreg.service.EmailAlreadyExistsException;
import com.fcuro.userreg.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserApiController {

    private final UserService userService;

    public UserApiController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegistrationRequest request) {
        try {
            // このAPIは未認証でも呼び出せるため、adminFlagは常にfalseで登録する(自己申告での昇格を防ぐため)
            userService.register(request.spreadsheetId(), request.sheetName(), request.email(), request.password(),
                    request.targetId(), false);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "登録が完了しました"));
        } catch (EmailAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me(Authentication authentication) {
        return ResponseEntity.ok(Map.of("email", authentication.getName()));
    }
}
