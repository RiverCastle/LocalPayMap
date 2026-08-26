package com.localpaymap.dto;

import com.localpaymap.domain.BizStatus;
import com.localpaymap.domain.Store;

public record StoreResponse(
        Long id,
        String name,
        String roadAddress,
        String jibunAddress,
        Double lat,
        Double lng,
        String phone,
        String category,
        Long currencyTypeId,
        String currencyName,
        BizStatus bizStatus) {

    public static StoreResponse from(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getName(),
                store.getRoadAddress(),
                store.getJibunAddress(),
                store.getLat(),
                store.getLng(),
                store.getPhone(),
                store.getCategory(),
                store.getCurrencyType() != null ? store.getCurrencyType().getId() : null,
                store.getCurrencyType() != null ? store.getCurrencyType().getCurrencyName() : null,
                store.getBizStatus());
    }
}
