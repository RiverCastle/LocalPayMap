package com.localpaymap.repository;

/** 격자 클러스터링 네이티브 쿼리 결과 매핑용 (컬럼 별칭 avgLat/avgLng/cnt 기준). */
public interface StoreClusterProjection {
    Double getAvgLat();

    Double getAvgLng();

    Long getCnt();
}
