package com.localpaymap.service;

import com.localpaymap.domain.FavoriteItem;
import com.localpaymap.domain.Store;
import com.localpaymap.domain.StoreReport;
import com.localpaymap.repository.FavoriteItemRepository;
import com.localpaymap.repository.StoreReportRepository;
import com.localpaymap.repository.StoreRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가맹점을 삭제하기 직전에, 그 가맹점을 가리키는 사용자 데이터를 정리한다. 가맹점을 지우는 모든 경로에서 먼저 호출해야 한다.
 * 같은 가맹점(지역화폐·가맹점명·도로명주소가 동일)이 남으면 그쪽으로 옮기고, 남는 것이 없으면 함께 삭제한다.
 */
@Component
public class StoreReferenceCleaner {

    private static final int CHUNK_SIZE = 1000;

    private final StoreRepository storeRepository;
    private final FavoriteItemRepository favoriteItemRepository;
    private final StoreReportRepository storeReportRepository;

    public StoreReferenceCleaner(
            StoreRepository storeRepository,
            FavoriteItemRepository favoriteItemRepository,
            StoreReportRepository storeReportRepository) {
        this.storeRepository = storeRepository;
        this.favoriteItemRepository = favoriteItemRepository;
        this.storeReportRepository = storeReportRepository;
    }

    /** 삭제와 한 트랜잭션으로 묶여야 하므로 호출측 트랜잭션이 없으면 실패한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void release(Collection<Long> deletingStoreIds) {
        Set<Long> deleting = new HashSet<>(deletingStoreIds);
        List<Long> ids = new ArrayList<>(deleting);

        // 대량 삭제에서도 사용자 데이터가 실제로 걸려 있는 가맹점만 하나씩 처리한다.
        Set<Long> referenced = new LinkedHashSet<>();
        for (int from = 0; from < ids.size(); from += CHUNK_SIZE) {
            List<Long> chunk = ids.subList(from, Math.min(from + CHUNK_SIZE, ids.size()));
            referenced.addAll(favoriteItemRepository.findReferencedStoreIds(chunk));
            referenced.addAll(storeReportRepository.findReferencedStoreIds(chunk));
        }

        for (Store store : storeRepository.findAllById(referenced)) {
            Optional<Long> keeperId = findKeeperId(store, deleting);
            moveFavorites(store.getId(), keeperId);
            moveReports(store.getId(), keeperId);
        }
    }

    private Optional<Long> findKeeperId(Store store, Set<Long> deleting) {
        if (store.getRoadAddress() == null) {
            return Optional.empty();
        }
        Long currencyTypeId = store.getCurrencyType() != null ? store.getCurrencyType().getId() : null;
        return storeRepository.findDuplicateMembers(store.getName(), store.getRoadAddress(), currencyTypeId).stream()
                .map(Store::getId)
                .filter(id -> !deleting.contains(id))
                .filter(Objects::nonNull)
                .findFirst();
    }

    private void moveFavorites(Long fromStoreId, Optional<Long> keeperId) {
        for (FavoriteItem item : favoriteItemRepository.findByStoreId(fromStoreId)) {
            // 남는 가맹점이 이미 같은 그룹에 담겨 있으면 중복이 되므로 옮기지 않고 지운다.
            if (keeperId.isEmpty()
                    || favoriteItemRepository.existsByGroupIdAndStoreId(item.getGroupId(), keeperId.get())) {
                favoriteItemRepository.delete(item);
            } else {
                item.moveTo(keeperId.get());
            }
        }
    }

    private void moveReports(Long fromStoreId, Optional<Long> keeperId) {
        for (StoreReport report : storeReportRepository.findByStoreId(fromStoreId)) {
            // 같은 사용자의 같은 제보가 남는 가맹점에 이미 있으면 한 건으로 합친다.
            if (keeperId.isEmpty()
                    || storeReportRepository.existsByUserIdAndStoreIdAndType(
                            report.getUserId(), keeperId.get(), report.getType())) {
                storeReportRepository.delete(report);
            } else {
                report.moveTo(keeperId.get());
            }
        }
    }
}
