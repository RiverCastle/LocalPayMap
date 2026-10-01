package com.localpaymap.repository;

/** 업종별 가맹점 수 집계 결과 매핑용. */
public interface CategoryCountProjection {
    String getCategory();

    Long getCnt();
}
