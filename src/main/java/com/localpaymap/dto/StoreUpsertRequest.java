package com.localpaymap.dto;

import com.localpaymap.domain.BizStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StoreUpsertRequest(
        @NotBlank String name,
        @NotBlank String roadAddress,
        String jibunAddress,
        String phone,
        String category,
        @NotNull Long currencyTypeId,
        @NotNull BizStatus bizStatus) {}
