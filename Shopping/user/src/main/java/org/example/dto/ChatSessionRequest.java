package org.example.dto;

import jakarta.validation.constraints.NotNull;

public record ChatSessionRequest(
        @NotNull Long merchantId,
        Long goodsId
) {
}
