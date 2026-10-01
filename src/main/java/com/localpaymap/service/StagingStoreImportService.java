package com.localpaymap.service;

import com.localpaymap.domain.BizStatus;
import com.localpaymap.domain.CurrencyType;
import com.localpaymap.domain.Store;
import com.localpaymap.domain.StoreSource;
import com.localpaymap.domain.SyncLog;
import com.localpaymap.domain.SyncStatus;
import com.localpaymap.repository.StoreRepository;
import com.localpaymap.repository.SyncLogRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 경기데이터드림 원본 적재 테이블(gg_mnyfacltstus_suwon)의 가맹점을 store/currency_type으로 옮겨 담는다.
 * source=IMPORTED, externalId=가맹점번호(frcs_no) 기준으로 upsert하며, 계속사업자 + 유효 좌표인 행만 대상이다.
 * 이미 옮겨졌으나 원본에서 휴/폐업으로 바뀐 가맹점은 지도에 남지 않도록 삭제한다.
 */
@Service
public class StagingStoreImportService {

    private static final Logger log = LoggerFactory.getLogger(StagingStoreImportService.class);

    private static final String REGION_NAME = "경기도 수원시";
    private static final String CURRENCY_NAME = "수원페이";
    private static final String ACTIVE_BIZ_STATE = "계속사업자";

    private static final String SELECT_SQL =
            "select frcs_no, cmpnm_nm, indutype_nm, lotno_addr, roadnm_addr, lat, lng, biz_state "
                    + "from gg_mnyfacltstus_suwon";

    private final JdbcTemplate jdbcTemplate;
    private final StoreRepository storeRepository;
    private final CurrencyTypeResolver currencyTypeResolver;
    private final SyncLogRepository syncLogRepository;

    public StagingStoreImportService(
            JdbcTemplate jdbcTemplate,
            StoreRepository storeRepository,
            CurrencyTypeResolver currencyTypeResolver,
            SyncLogRepository syncLogRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.storeRepository = storeRepository;
        this.currencyTypeResolver = currencyTypeResolver;
        this.syncLogRepository = syncLogRepository;
    }

    @Transactional
    public SyncLog importSuwon() {
        int inserted = 0;
        int updated = 0;
        int removed = 0;
        int skipped = 0;
        try {
            CurrencyType currencyType = currencyTypeResolver.resolve(REGION_NAME, CURRENCY_NAME);

            Map<String, Store> existing = new HashMap<>();
            for (Store store : storeRepository.findAll()) {
                if (store.getSource() == StoreSource.IMPORTED
                        && store.getExternalId() != null
                        && store.getCurrencyType() != null
                        && store.getCurrencyType().getId().equals(currencyType.getId())) {
                    existing.put(store.getExternalId(), store);
                }
            }

            List<Store> toInsert = new ArrayList<>();
            List<Store> toDelete = new ArrayList<>();

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(SELECT_SQL);
            for (Map<String, Object> row : rows) {
                String frcsNo = (String) row.get("frcs_no");
                String name = (String) row.get("cmpnm_nm");
                Number lat = (Number) row.get("lat");
                Number lng = (Number) row.get("lng");
                boolean active = ACTIVE_BIZ_STATE.equals(row.get("biz_state"));
                Store current = existing.get(frcsNo);

                if (!active) {
                    if (current != null) {
                        toDelete.add(current);
                        removed++;
                    } else {
                        skipped++;
                    }
                    continue;
                }
                if (frcsNo == null || name == null || name.isBlank() || lat == null || lng == null
                        || lat.doubleValue() == 0 || lng.doubleValue() == 0) {
                    skipped++;
                    continue;
                }

                Store incoming = new Store(
                        name,
                        (String) row.get("roadnm_addr"),
                        (String) row.get("lotno_addr"),
                        lat.doubleValue(),
                        lng.doubleValue(),
                        null,
                        (String) row.get("indutype_nm"),
                        currencyType,
                        BizStatus.OPEN,
                        StoreSource.IMPORTED,
                        frcsNo);
                if (current != null) {
                    current.updateFrom(incoming);
                    updated++;
                } else {
                    toInsert.add(incoming);
                    inserted++;
                }
            }

            storeRepository.saveAll(toInsert);
            storeRepository.deleteAll(toDelete);

            return syncLogRepository.save(new SyncLog(
                    LocalDateTime.now(),
                    SyncStatus.SUCCESS,
                    inserted,
                    updated,
                    "수원 원본 테이블 반영 완료 (제외=" + skipped + ", 삭제=" + removed + ")"));
        } catch (Exception e) {
            log.error("수원 원본 테이블 → store 반영 실패", e);
            // 롤백되도록 예외를 던진다. ApiExceptionHandler가 메시지를 400으로 내려준다.
            throw new IllegalArgumentException("수원 원본 테이블 반영 실패: " + e.getMessage(), e);
        }
    }
}
