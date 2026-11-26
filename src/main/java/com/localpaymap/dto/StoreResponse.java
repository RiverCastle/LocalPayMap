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
        BizStatus bizStatus,
        /** 제보 현황. 제보를 함께 조회하지 않는 응답(관리자 화면 등)에서는 null이다. */
        ReportSummaryResponse reports) {

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
                store.getBizStatus(),
                null);
    }

    public StoreResponse withReports(ReportSummaryResponse reports) {
        return new StoreResponse(
                id,
                name,
                roadAddress,
                jibunAddress,
                lat,
                lng,
                phone,
                category,
                currencyTypeId,
                currencyName,
                bizStatus,
                reports);
    }
}
