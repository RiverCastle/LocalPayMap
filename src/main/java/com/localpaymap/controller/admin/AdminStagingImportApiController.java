package com.localpaymap.controller.admin;

import com.localpaymap.dto.SyncLogResponse;
import com.localpaymap.service.StagingStoreImportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/stores/import-suwon-staging")
public class AdminStagingImportApiController {

    private final StagingStoreImportService stagingStoreImportService;

    public AdminStagingImportApiController(StagingStoreImportService stagingStoreImportService) {
        this.stagingStoreImportService = stagingStoreImportService;
    }

    @PostMapping
    public SyncLogResponse importSuwon() {
        return SyncLogResponse.from(stagingStoreImportService.importSuwon());
    }
}
