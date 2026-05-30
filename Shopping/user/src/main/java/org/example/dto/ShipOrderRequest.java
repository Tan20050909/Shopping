package org.example.dto;

import jakarta.validation.constraints.NotBlank;

public record ShipOrderRequest(
        @NotBlank(message = "物流公司不能为空")
        String expressCompany,
        @NotBlank(message = "物流单号不能为空")
        String expressNo,
        String remark
) {
}
