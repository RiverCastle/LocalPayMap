package com.localpaymap.service;

import com.localpaymap.controller.api.ApiException;
import com.localpaymap.domain.ReportType;
import com.localpaymap.domain.StoreReport;
import com.localpaymap.dto.MyReportResponse;
import com.localpaymap.dto.ReportSummaryResponse;
import com.localpaymap.dto.StoreResponse;
import com.localpaymap.repository.ReportCountProjection;
import com.localpaymap.repository.StoreReportRepository;
import com.localpaymap.repository.StoreRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreReportService {

    private final StoreReportRepository reportRepository;
    private final StoreRepository storeRepository;

    public StoreReportService(StoreReportRepository reportRepository, StoreRepository storeRepository) {
        this.reportRepository = reportRepository;
        this.storeRepository = storeRepository;
    }

    /**
     * 이미 남긴 제보를 다시 보내도 오류 없이 성공(멱등)한다.
     * 동시 요청이 유니크 제약에 걸려도 이후 조회가 가능하도록 이 메서드는 하나의 트랜잭션으로 묶지 않는다.
     */
    public ReportSummaryResponse add(Long userId, Long storeId, ReportType type) {
        requireStore(storeId);
        if (!reportRepository.existsByUserIdAndStoreIdAndType(userId, storeId, type)) {
            try {
                reportRepository.saveAndFlush(new StoreReport(storeId, userId, type));
            } catch (DataIntegrityViolationException e) {
                // 같은 제보가 방금 저장된 것이면 성공으로 본다. 그 외의 위반은 숨기지 않는다.
                if (!reportRepository.existsByUserIdAndStoreIdAndType(userId, storeId, type)) {
                    throw e;
                }
            }
        }
        return summaryOf(storeId);
    }

    @Transactional
    public ReportSummaryResponse cancel(Long userId, Long storeId, ReportType type) {
        requireStore(storeId);
        reportRepository.deleteMine(userId, storeId, type);
        return summaryOf(storeId);
    }

    @Transactional(readOnly = true)
    public List<MyReportResponse> listMine(Long userId) {
        return reportRepository.findMine(userId).stream().map(MyReportResponse::from).toList();
    }

    /** 가맹점 응답에 제보 현황을 붙인다. 가맹점 수와 무관하게 조회는 한 번이다. */
    public List<StoreResponse> attachReports(List<StoreResponse> stores) {
        Map<Long, ReportSummaryResponse> summaries =
                summaries(stores.stream().map(StoreResponse::id).toList());
        return stores.stream()
                .map(s -> s.withReports(summaries.getOrDefault(s.id(), ReportSummaryResponse.empty())))
                .toList();
    }

    private ReportSummaryResponse summaryOf(Long storeId) {
        return summaries(List.of(storeId)).getOrDefault(storeId, ReportSummaryResponse.empty());
    }

    private Map<Long, ReportSummaryResponse> summaries(Collection<Long> storeIds) {
        Map<Long, ReportSummaryResponse> result = new HashMap<>();
        if (storeIds.isEmpty()) {
            return result;
        }
        for (ReportCountProjection row : reportRepository.countByStoreIds(storeIds)) {
            result.merge(row.getStoreId(), toSummary(row), StoreReportService::combine);
        }
        return result;
    }

    private static ReportSummaryResponse toSummary(ReportCountProjection row) {
        return new ReportSummaryResponse(Map.of(row.getType(), row.getCnt()), row.getCnt(), row.getLastAt());
    }

    private static ReportSummaryResponse combine(ReportSummaryResponse a, ReportSummaryResponse b) {
        Map<ReportType, Long> counts = new EnumMap<>(ReportType.class);
        counts.putAll(a.counts());
        counts.putAll(b.counts());
        Instant last = a.lastReportedAt().isAfter(b.lastReportedAt()) ? a.lastReportedAt() : b.lastReportedAt();
        return new ReportSummaryResponse(counts, a.total() + b.total(), last);
    }

    private void requireStore(Long storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 가맹점입니다.");
        }
    }
}
