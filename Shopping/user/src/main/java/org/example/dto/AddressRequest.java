package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddressRequest(
        @NotBlank String consignee,
        @NotBlank @Pattern(regexp = "\\d{11}", message = "必须是 11 位手机号") String phone,
        @NotBlank String province,
        String provinceCode,
        @NotBlank String city,
        String cityCode,
        @NotBlank String district,
        String districtCode,
        @NotBlank String detailAddr,
        String postalCode,
        String remark,
        boolean defaultAddress
) {
}
