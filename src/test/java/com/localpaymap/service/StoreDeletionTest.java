package com.localpaymap.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.localpaymap.domain.BizStatus;
import com.localpaymap.domain.ReportType;
import com.localpaymap.domain.Store;
import com.localpaymap.domain.StoreSource;
import com.localpaymap.dto.FavoriteGroupRequest;
import com.localpaymap.repository.FavoriteItemRepository;
import com.localpaymap.repository.StoreReportRepository;
import com.localpaymap.repository.StoreRepository;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** 가맹점을 삭제해도 즐겨찾기·제보가 허공에 남거나 사라지지 않는지 확인한다. */
@SpringBootTest
@ActiveProfiles("local")
class StoreDeletionTest {

    /** 즐겨찾기/제보 서비스는 사용자 존재를 확인하지 않으므로 다른 테스트와 겹치지 않는 id를 쓴다. */
    private static final AtomicLong USER_IDS = new AtomicLong(900_000);

    @Autowired StoreService storeService;
    @Autowired FavoriteGroupService favoriteGroupService;
    @Autowired StoreReportService storeReportService;
    @Autowired StoreRepository storeRepository;
    @Autowired FavoriteItemRepository favoriteItemRepository;
    @Autowired StoreReportRepository storeReportRepository;

    private Store newStore(String name) {
        return storeRepository.save(new Store(
                name, name + " 도로명", null, 36.0, 129.0, null, "한식", null, BizStatus.OPEN, StoreSource.MANUAL, null));
    }

    private Long newGroup(Long userId, String name) {
        return favoriteGroupService.create(userId, new FavoriteGroupRequest(name, null)).id();
    }

    private List<Long> storeIdsIn(Long userId, Long groupId) {
        return favoriteGroupService.list(userId).stream()
                .filter(g -> g.id().equals(groupId))
                .findFirst()
                .orElseThrow()
                .storeIds();
    }

    @Test
    void deletingStoreRemovesItsFavoritesAndReports() {
        Long user = USER_IDS.incrementAndGet();
        Store gone = newStore("단일 삭제 식당");
        Store other = newStore("남는 식당");
        Long group = newGroup(user, "A");
        favoriteGroupService.addStore(user, group, gone.getId());
        favoriteGroupService.addStore(user, group, other.getId());
        storeReportService.add(user, gone.getId(), ReportType.CLOSED);

        storeService.delete(gone.getId());

        assertThat(storeIdsIn(user, group)).containsExactly(other.getId());
        assertThat(favoriteItemRepository.findByStoreId(gone.getId())).isEmpty();
        assertThat(storeReportRepository.findByStoreId(gone.getId())).isEmpty();
    }

    @Test
    void deletingSelectedDuplicatesMovesFavoritesAndReportsToRemainingStore() {
        Long user = USER_IDS.incrementAndGet();
        Store keep = newStore("선택 중복 식당");
        Store dup1 = newStore("선택 중복 식당");
        Store dup2 = newStore("선택 중복 식당");
        Long movedOnly = newGroup(user, "옮겨지는 그룹");
        Long alreadyHasKeeper = newGroup(user, "이미 담긴 그룹");
        favoriteGroupService.addStore(user, movedOnly, dup1.getId());
        favoriteGroupService.addStore(user, movedOnly, dup2.getId());
        favoriteGroupService.addStore(user, alreadyHasKeeper, keep.getId());
        favoriteGroupService.addStore(user, alreadyHasKeeper, dup1.getId());
        storeReportService.add(user, dup1.getId(), ReportType.PAYMENT_REFUSED);
        storeReportService.add(user, dup2.getId(), ReportType.PAYMENT_REFUSED);
        storeReportService.add(user, dup2.getId(), ReportType.WRONG_INFO);

        storeService.deleteAll(List.of(dup1.getId(), dup2.getId()));

        assertThat(storeIdsIn(user, movedOnly)).containsExactly(keep.getId());
        assertThat(storeIdsIn(user, alreadyHasKeeper)).containsExactly(keep.getId());
        // 같은 사용자의 같은 제보는 한 건으로 합쳐진다.
        assertThat(storeReportRepository.findByStoreId(keep.getId()))
                .extracting(r -> r.getType())
                .containsExactlyInAnyOrder(ReportType.PAYMENT_REFUSED, ReportType.WRONG_INFO);
        assertThat(favoriteItemRepository.findByStoreId(dup1.getId())).isEmpty();
        assertThat(storeReportRepository.findByStoreId(dup2.getId())).isEmpty();
    }

    @Test
    void deletingAllDuplicatesMovesFavoritesToOldestStore() {
        Long user = USER_IDS.incrementAndGet();
        Store oldest = newStore("일괄 중복 식당");
        Store dup = newStore("일괄 중복 식당");
        Long group = newGroup(user, "A");
        favoriteGroupService.addStore(user, group, dup.getId());

        storeService.deleteAllDuplicates(storeService.summarizeDuplicates().deleteCount());

        assertThat(storeRepository.existsById(dup.getId())).isFalse();
        assertThat(storeIdsIn(user, group)).containsExactly(oldest.getId());
    }

    @Test
    void favoritesLeftOverFromEarlierDeletionsAreNotCounted() {
        Long user = USER_IDS.incrementAndGet();
        Store store = newStore("정리 전에 지워진 식당");
        Long group = newGroup(user, "A");
        favoriteGroupService.addStore(user, group, store.getId());

        // 정리 로직이 생기기 전처럼 가맹점만 지운다.
        storeRepository.deleteById(store.getId());

        assertThat(storeIdsIn(user, group)).isEmpty();
        assertThat(favoriteGroupService.listStores(user, group)).isEmpty();
        assertThat(favoriteItemRepository.countByGroupId(group)).isZero();
    }
}
