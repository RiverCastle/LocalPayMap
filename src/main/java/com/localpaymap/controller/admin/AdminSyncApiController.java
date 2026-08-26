package com.localpaymap.controller.admin;

import com.localpaymap.dto.SyncLogResponse;
import com.localpaymap.repository.SyncLogRepository;
import com.localpaymap.service.OpenDataSyncService;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/sync")
public class AdminSyncApiController {

    private final OpenDataSyncService openDataSyncService;
    private final SyncLogRepository syncLogRepository;

    public AdminSyncApiController(OpenDataSyncService openDataSyncService, SyncLogRepository syncLogRepository) {
        this.openDataSyncService = openDataSyncService;
        this.syncLogRepository = syncLogRepository;
    }

    @PostMapping("/open-data")
    public SyncLogResponse triggerSync() {
        return SyncLogResponse.from(openDataSyncService.sync());
    }

    @GetMapping("/logs")
    public List<SyncLogResponse> logs() {
        return syncLogRepository.findAllByOrderByExecutedAtDesc(PageRequest.of(0, 20)).stream()
                .map(SyncLogResponse::from)
                .toList();
    }
}
