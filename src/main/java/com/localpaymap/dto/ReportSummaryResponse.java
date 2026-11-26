package com.localpaymap.dto;

import com.localpaymap.domain.ReportType;
import java.time.Instant;
import java.util.Map;

/** 가맹점 하나의 제보 현황. counts에는 제보가 있는 종류만 들어간다. */
public record ReportSummaryResponse(Map<ReportType, Long> counts, long total, Instant lastReportedAt) {

    public static ReportSummaryResponse empty() {
        return new ReportSummaryResponse(Map.of(), 0, null);
    }
}
