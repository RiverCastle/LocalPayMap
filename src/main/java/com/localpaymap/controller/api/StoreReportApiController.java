package com.localpaymap.controller.api;

import com.localpaymap.domain.ReportType;
import com.localpaymap.dto.MyReportResponse;
import com.localpaymap.dto.ReportSummaryResponse;
import com.localpaymap.service.StoreReportService;
import java.util.List;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인한 사용자의 가맹점 제보. 제보 현황 조회는 공개 가맹점 API 응답에 포함된다. */
@RestController
@RequestMapping("/api/me")
public class StoreReportApiController {

    private final StoreReportService service;

    public StoreReportApiController(StoreReportService service) {
        this.service = service;
    }

    @GetMapping("/reports")
    public List<MyReportResponse> mine(JwtAuthenticationToken auth) {
        return service.listMine(userId(auth));
    }

    @PutMapping("/stores/{storeId}/reports/{type}")
    public ReportSummaryResponse add(
            JwtAuthenticationToken auth, @PathVariable Long storeId, @PathVariable ReportType type) {
        return service.add(userId(auth), storeId, type);
    }

    @DeleteMapping("/stores/{storeId}/reports/{type}")
    public ReportSummaryResponse cancel(
            JwtAuthenticationToken auth, @PathVariable Long storeId, @PathVariable ReportType type) {
        return service.cancel(userId(auth), storeId, type);
    }

    private static Long userId(JwtAuthenticationToken auth) {
        return Long.valueOf(auth.getToken().getSubject());
    }
}
