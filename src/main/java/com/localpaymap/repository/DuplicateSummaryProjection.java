package com.localpaymap.repository;

/** 중복 그룹 수와 그룹에 속한 가맹점 총 수 집계 결과 매핑용. */
public interface DuplicateSummaryProjection {
    Long getGroupCount();

    Long getDuplicateStoreCount();
}
