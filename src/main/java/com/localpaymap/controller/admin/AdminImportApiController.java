package com.localpaymap.controller.admin;

import com.localpaymap.dto.SyncLogResponse;
import com.localpaymap.service.StoreImportService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/api/stores/import")
public class AdminImportApiController {

    private final StoreImportService storeImportService;

    public AdminImportApiController(StoreImportService storeImportService) {
        this.storeImportService = storeImportService;
    }

    @PostMapping
    public SyncLogResponse importFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("regionName") String regionName,
            @RequestParam("currencyName") String currencyName)
            throws IOException {
        String json = new String(file.getBytes(), StandardCharsets.UTF_8);
        return SyncLogResponse.from(storeImportService.importJson(json, regionName, currencyName));
    }
}
