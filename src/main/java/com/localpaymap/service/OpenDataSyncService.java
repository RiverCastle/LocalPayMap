package com.localpaymap.service;

import com.localpaymap.client.OpenDataApiClient;
import com.localpaymap.client.OpenDataStoreItem;
import com.localpaymap.domain.BizStatus;
import com.localpaymap.domain.CurrencyType;
import com.localpaymap.domain.Store;
import com.localpaymap.domain.StoreSource;
import com.localpaymap.domain.SyncLog;
import com.localpaymap.domain.SyncStatus;
import com.localpaymap.repository.StoreRepository;
import com.localpaymap.repository.SyncLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공공데이터포털 "전국지역화폐가맹점표준데이터"를 주기적으로 내려받아 DB에 반영한다.
 * source=MANUAL(관리자 수기 등록) 레코드는 절대 덮어쓰지 않는다.
 */
@Service
public class OpenDataSyncService {

    private static final Logger log = LoggerFactory.getLogger(OpenDataSyncService.class);
    private static final int PAGE_SIZE = 1000;

    private final OpenDataApiClient openDataApiClient;
    private final StoreRepository storeRepository;
    private final CurrencyTypeResolver currencyTypeResolver;
    private final SyncLogRepository syncLogRepository;

    public OpenDataSyncService(
            OpenDataApiClient openDataApiClient,
            StoreRepository storeRepository,
            CurrencyTypeResolver currencyTypeResolver,
            SyncLogRepository syncLogRepository) {
        this.openDataApiClient = openDataApiClient;
        this.storeRepository = storeRepository;
        this.currencyTypeResolver = currencyTypeResolver;
        this.syncLogRepository = syncLogRepository;
    }

    @Transactional
    public SyncLog sync() {
        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        try {
            int totalCount = openDataApiClient.fetchTotalCount(1);
            int totalPages = totalCount == 0 ? 1 : (int) Math.ceil((double) totalCount / PAGE_SIZE);

            for (int page = 1; page <= totalPages; page++) {
                List<OpenDataStoreItem> items = openDataApiClient.fetchPage(page, PAGE_SIZE);
                for (OpenDataStoreItem item : items) {
                    UpsertResult result = upsert(item);
                    switch (result) {
                        case INSERTED -> inserted++;
                        case UPDATED -> updated++;
                        case SKIPPED -> skipped++;
                    }
                }
            }

            SyncLog syncLog = new SyncLog(
                    LocalDateTime.now(),
                    SyncStatus.SUCCESS,
                    inserted,
                    updated,
                    "동기화 완료 (skipped=" + skipped + ")");
            return syncLogRepository.save(syncLog);
        } catch (Exception e) {
            log.error("공공데이터 동기화 실패", e);
            SyncLog syncLog =
                    new SyncLog(LocalDateTime.now(), SyncStatus.FAIL, inserted, updated, e.getMessage());
            return syncLogRepository.save(syncLog);
        }
    }

    private UpsertResult upsert(OpenDataStoreItem item) {
        if (isBlank(item.getBizrno()) || isBlank(item.getLat()) || isBlank(item.getLot())) {
            return UpsertResult.SKIPPED;
        }
        double lat;
        double lng;
        try {
            lat = Double.parseDouble(item.getLat());
            lng = Double.parseDouble(item.getLot());
        } catch (NumberFormatException e) {
            return UpsertResult.SKIPPED;
        }

        CurrencyType currencyType = resolveCurrencyType(item);
        BizStatus bizStatus = "폐업".equals(item.getTrdStateNm()) ? BizStatus.CLOSED : BizStatus.OPEN;

        return storeRepository
                .findBySourceAndExternalId(StoreSource.OPEN_API, item.getBizrno())
                .map(existing -> {
                    Store updated = new Store(
                            item.getBizplcNm(),
                            item.getRdnwhlAddr(),
                            item.getLnmAddr(),
                            lat,
                            lng,
                            item.getTelNo(),
                            item.getInduty(),
                            currencyType,
                            bizStatus,
                            StoreSource.OPEN_API,
                            item.getBizrno());
                    existing.updateFrom(updated);
                    return UpsertResult.UPDATED;
                })
                .orElseGet(() -> {
                    Store store = new Store(
                            item.getBizplcNm(),
                            item.getRdnwhlAddr(),
                            item.getLnmAddr(),
                            lat,
                            lng,
                            item.getTelNo(),
                            item.getInduty(),
                            currencyType,
                            bizStatus,
                            StoreSource.OPEN_API,
                            item.getBizrno());
                    storeRepository.save(store);
                    return UpsertResult.INSERTED;
                });
    }

    private CurrencyType resolveCurrencyType(OpenDataStoreItem item) {
        String region = (item.getCtpvNm() != null ? item.getCtpvNm() : "") + " "
                + (item.getSggNm() != null ? item.getSggNm() : "");
        return currencyTypeResolver.resolve(region, item.getBillNm());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private enum UpsertResult {
        INSERTED,
        UPDATED,
        SKIPPED
    }
}
