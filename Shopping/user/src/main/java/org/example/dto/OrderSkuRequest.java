package org.example.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderSkuRequest(
        @NotNull Long skuId,
        @Min(1) int num
) {
}
