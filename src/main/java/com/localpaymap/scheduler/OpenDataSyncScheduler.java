package com.localpaymap.scheduler;

import com.localpaymap.service.OpenDataSyncService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OpenDataSyncScheduler {

    private final OpenDataSyncService openDataSyncService;

    public OpenDataSyncScheduler(OpenDataSyncService openDataSyncService) {
        this.openDataSyncService = openDataSyncService;
    }

    @Scheduled(cron = "${open-data.sync-cron:0 0 4 * * *}")
    public void syncDaily() {
        openDataSyncService.sync();
    }
}
