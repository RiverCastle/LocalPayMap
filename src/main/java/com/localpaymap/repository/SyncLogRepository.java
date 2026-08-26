package com.localpaymap.repository;

import com.localpaymap.domain.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {
    java.util.List<SyncLog> findAllByOrderByExecutedAtDesc(Pageable pageable);
}
