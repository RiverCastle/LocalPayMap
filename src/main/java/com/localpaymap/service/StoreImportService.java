package com.localpaymap.service;

import com.localpaymap.client.MerchantImportItem;
import com.localpaymap.client.MerchantImportPayload;
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
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectMapper;

/**
 * 지자체 지역화폐 앱 API를 그대로 덤프한 raw JSON 파일을 관리자가 업로드하면 파싱해서 DB에 반영한다.
 * (공공데이터포털에 없는 지역/데이터를 관리자가 직접 확보해 채워 넣는 경로)
 * source=OPEN_API, MANUAL 레코드는 건드리지 않고 source=IMPORTED 레코드만 seq 기준으로 upsert한다.
 */
@Service
public class StoreImportService {

    private static final Logger log = LoggerFactory.getLogger(StoreImportService.class);

    private final StoreRepository storeRepository;
    private final CurrencyTypeResolver currencyTypeResolver;
    private final SyncLogRepository syncLogRepository;
    private final ObjectMapper objectMapper;

    public StoreImportService(
            StoreRepository storeRepository,
            CurrencyTypeResolver currencyTypeResolver,
            SyncLogRepository syncLogRepository,
            ObjectMapper objectMapper) {
        this.storeRepository = storeRepository;
        this.currencyTypeResolver = currencyTypeResolver;
        this.syncLogRepository = syncLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SyncLog importJson(String json, String regionName, String currencyName) {
        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        try {
            CurrencyType currencyType = currencyTypeResolver.resolve(regionName, currencyName);

            // 파일이 페이지별 응답을 이어붙인 형태(연속된 JSON 도큐먼트)일 수 있어 스트리밍으로 여러 개를 순회한다.
            MappingIterator<MerchantImportPayload> pages =
                    objectMapper.readerFor(MerchantImportPayload.class).readValues(json);
            while (pages.hasNextValue()) {
                MerchantImportPayload payload = pages.nextValue();
                List<MerchantImportItem> items =
                        payload.getData() != null && payload.getData().getMerchants() != null
                                ? payload.getData().getMerchants()
                                : List.of();
                for (MerchantImportItem item : items) {
                    UpsertResult result = upsert(item, currencyType);
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
                    "파일 가져오기 완료 (" + currencyName + ", skipped=" + skipped + ")");
            return syncLogRepository.save(syncLog);
        } catch (Exception e) {
            log.error("가맹점 파일 가져오기 실패", e);
            SyncLog syncLog = new SyncLog(LocalDateTime.now(), SyncStatus.FAIL, inserted, updated, e.getMessage());
            return syncLogRepository.save(syncLog);
        }
    }

    private UpsertResult upsert(MerchantImportItem item, CurrencyType currencyType) {
        if (item.getSeq() == null || item.getLatitude() == null || item.getLongitude() == null || isBlank(item.getSimpleNm())) {
            return UpsertResult.SKIPPED;
        }
        String externalId = String.valueOf(item.getSeq());

        return storeRepository
                .findBySourceAndExternalId(StoreSource.IMPORTED, externalId)
                .map(existing -> {
                    Store updated = new Store(
                            item.getSimpleNm(),
                            item.getAddr(),
                            null,
                            item.getLatitude(),
                            item.getLongitude(),
                            item.phoneOrNull(),
                            item.getBizTypeNm(),
                            currencyType,
                            BizStatus.OPEN,
                            StoreSource.IMPORTED,
                            externalId);
                    existing.updateFrom(updated);
                    return UpsertResult.UPDATED;
                })
                .orElseGet(() -> {
                    Store store = new Store(
                            item.getSimpleNm(),
                            item.getAddr(),
                            null,
                            item.getLatitude(),
                            item.getLongitude(),
                            item.phoneOrNull(),
                            item.getBizTypeNm(),
                            currencyType,
                            BizStatus.OPEN,
                            StoreSource.IMPORTED,
                            externalId);
                    storeRepository.save(store);
                    return UpsertResult.INSERTED;
                });
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
