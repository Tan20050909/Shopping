package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LiveCommentRequest(
        @NotBlank @Size(max = 300) String content,
        Integer commentType
) {
}
