package com.localpaymap.dto;

import com.localpaymap.domain.ReportType;
import com.localpaymap.domain.StoreReport;

public record MyReportResponse(Long storeId, ReportType type) {

    public static MyReportResponse from(StoreReport report) {
        return new MyReportResponse(report.getStoreId(), report.getType());
    }
}
