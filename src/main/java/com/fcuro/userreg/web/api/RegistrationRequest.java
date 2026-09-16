package com.fcuro.userreg.web.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @NotBlank String spreadsheetId,
        @NotBlank String sheetName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        String targetId
) {
}
