package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateProfileRequest(
        @NotBlank String nickname,
        String avatar,
        String realName,
        Integer gender,
        @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "生日格式应为 yyyy-MM-dd") String birthday
) {
}
