package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank @Pattern(regexp = "\\d{11}", message = "必须是 11 位手机号") String phone,
        @NotBlank String password
) {
}
