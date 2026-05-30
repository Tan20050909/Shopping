package org.example.user.dto;

import jakarta.validation.constraints.Min;

public record UpdateCartItemRequest(
        @Min(1) Integer num,
        Boolean selected
) {
}
