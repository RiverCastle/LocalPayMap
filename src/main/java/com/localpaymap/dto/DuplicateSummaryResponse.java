package com.localpaymap.dto;

/**
 * 일괄 삭제 전 확인용 요약.
 *
 * @param totalStores 현재 전체 가맹점 수
 * @param duplicateGroups 중복 그룹 수
 * @param duplicateStores 중복 그룹에 속한 가맹점 수(그룹당 유지될 1건 포함)
 * @param deleteCount 삭제될 가맹점 수 (그룹당 가장 먼저 등록된 1건만 유지)
 * @param remainingStores 삭제 후 남는 전체 가맹점 수
 */
public record DuplicateSummaryResponse(
        long totalStores, long duplicateGroups, long duplicateStores, long deleteCount, long remainingStores) {}
