package com.localpaymap.repository;

import com.localpaymap.domain.ReportType;
import java.time.Instant;

public interface ReportCountProjection {

    Long getStoreId();

    ReportType getType();

    long getCnt();

    Instant getLastAt();
}
