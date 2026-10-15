package com.localpaymap.repository;

/** 중복 가맹점 그룹(지역화폐+가맹점명+도로명주소가 같은 묶음) 집계 결과 매핑용. */
public interface DuplicateGroupProjection {
    String getName();

    String getRoadAddress();

    Long getCurrencyTypeId();

    Long getCnt();
}
