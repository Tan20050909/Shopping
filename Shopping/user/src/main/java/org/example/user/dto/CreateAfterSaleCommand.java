package org.example.user.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAfterSaleCommand(
        @NotNull Long orderId,
        @NotNull Long orderItemId,
        @NotNull Integer afterSaleType,
        @NotBlank String applyReason,
        String applyEvidence,
        @NotNull @DecimalMin("0.01") BigDecimal applyAmount
) {
}
