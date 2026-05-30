package org.example.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewCommand(
        @NotNull Long orderId,
        @NotNull Long orderItemId,
        @NotNull @Min(1) @Max(5) Integer goodsScore,
        @NotNull @Min(1) @Max(5) Integer serviceScore,
        @NotNull @Min(1) @Max(5) Integer logisticsScore,
        @NotBlank @Size(max = 500) String commentContent,
        String commentPic,
        Boolean anonymous
) {
}
