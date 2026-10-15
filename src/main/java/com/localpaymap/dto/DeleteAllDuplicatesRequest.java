package com.localpaymap.dto;

import jakarta.validation.constraints.NotNull;

/** @param expectedDeleteCount 사용자가 확인한 삭제 예정 건수. 실행 시점의 실제 건수와 다르면 삭제하지 않는다. */
public record DeleteAllDuplicatesRequest(@NotNull Long expectedDeleteCount) {}
