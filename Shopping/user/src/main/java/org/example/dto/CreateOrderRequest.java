package org.example.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotNull Long addrId,
        Long userCouponId,
        List<Long> userCouponIds,
        List<Long> cartIds,
        boolean fromCart,
        List<OrderSkuRequest> items
) {
}
