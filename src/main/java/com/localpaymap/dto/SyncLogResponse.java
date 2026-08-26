package com.localpaymap.dto;

import com.localpaymap.domain.SyncLog;
import com.localpaymap.domain.SyncStatus;
import java.time.LocalDateTime;

public record SyncLogResponse(
        Long id, LocalDateTime executedAt, SyncStatus status, int insertedCount, int updatedCount, String message) {

    public static SyncLogResponse from(SyncLog log) {
        return new SyncLogResponse(
                log.getId(), log.getExecutedAt(), log.getStatus(), log.getInsertedCount(), log.getUpdatedCount(), log.getMessage());
    }
}
