package com.localpaymap.dto;

import com.localpaymap.domain.CurrencyType;

public record CurrencyTypeResponse(Long id, String regionName, String currencyName, String code) {

    public static CurrencyTypeResponse from(CurrencyType currencyType) {
        return new CurrencyTypeResponse(
                currencyType.getId(),
                currencyType.getRegionName(),
                currencyType.getCurrencyName(),
                currencyType.getCode());
    }
}
